# Analyse: Verschieben-Funktion für einen Termin

## Ablauf („Ein Termin verschieben“)

### 1. UI – Auslösung

- **Karten-Ansicht (Adapter):** Klick auf Button „V“ (Verschieben) → `CustomerViewHolderBinder` setzt `btnVerschieben.setOnClickListener` und ruft `dialogHelper.showVerschiebenDialog(customer)` auf.
- **TourPlanner (Compose):** Erledigungs-Sheet zeigt „Verschieben“-Button, wenn `state.showVerschieben`; Klick ruft `onVerschieben(customer)` → Coordinator öffnet `sheetDialogHelper.showVerschiebenDialog(customer)`.

### 2. Dialog – Datum und Modus

- **CustomerDialogHelper.showVerschiebenDialog(customer):**
  - Öffnet Datumspicker (`DialogBaseHelper.showDatePickerDialog`).
  - Nach Auswahl eines Datums erscheint AlertDialog:
    - **„Nur diesen Termin“** (Positive) → `onVerschieben?.invoke(customer, newDate, false)`.
    - **„Alle restlichen Termine“** (Neutral) → `onVerschieben?.invoke(customer, newDate, true)`.
  - `newDate` = gewähltes Datum (Zeitstempel vom Picker).

### 3. Handler – Einzelverschiebung (alleVerschieben = false)

- **TourPlannerCallbackHandler.handleVerschieben(customer, newDate, false):**
  1. **Originaldatum:** `originalDatum = customer.getFaelligAm().takeIf { it > 0 } ?: TerminBerechnungUtils.getStartOfDay(viewDate.timeInMillis)`  
     – also das nächste Fälligkeitsdatum des Kunden oder, falls 0, der Start des angezeigten Tages.
  2. **Abbruch:** Wenn `originalDatum <= 0L` → Toast mit Fehlermeldung, Abbruch.
  3. **Neuer Eintrag:** `VerschobenerTermin(originalDatum, newDate, intervallId = null, typ = TerminTyp.ABHOLUNG)`.
  4. **Liste aktualisieren:** Bestehende `customer.verschobeneTermine`; alle Einträge, deren `originalDatum` (Tagesstart) gleich dem aktuellen `originalStart` ist, werden entfernt; der neue Eintrag wird angehängt.
  5. **Serialisierung:** `serializeVerschobeneTermine(newList)` erzeugt eine Map mit Keys `"0"`, `"1"`, …; pro Eintrag nur `originalDatum` und `verschobenAufDatum` (Long).
  6. **Firebase:** `repository.updateCustomer(customer.id, mapOf("verschobeneTermine" to serialized))`.
  7. **Erfolg:** Toast „Termin verschoben“, nach 2 s `reloadCurrentView()`.

### 4. Wo die Verschiebung wirkt (Anzeige & Logik)

- **TerminBerechnungUtils** (berechneTermineFuerIntervall / berechneAlleTermineFuerKunde):  
  Nutzt `verschobeneTermine`; für jedes berechnete Datum wird `TerminFilterUtils.istTerminVerschoben(terminDatum, verschobeneTermine, intervallId)` aufgerufen. Bei Treffer wird `verschobenAufDatum` statt des ursprünglichen Datums verwendet → verschobene Termine erscheinen am neuen Datum.
- **TerminFilterUtils.istTerminVerschoben:**  
  Sucht in `verschobeneTermine` nach Eintrag mit gleichem Tagesstart (`originalDatum`) und optional passendem `intervallId`; gibt den Eintrag oder null zurück.
- **Customer.getFaelligAm():**  
  Baut Termine über `TerminBerechnungUtils` (inkl. `verschobeneTermine`) und liefert das nächste fällige Datum → nach Verschieben zeigt „Nächste Tour“ etc. das neue Datum.
- **TourPlannerDateUtils** (calculateAbholungDatum / calculateAuslieferungDatum):  
  Prüft `customer.verschobeneTermine` auf Einträge, deren `verschobenAufDatum` am angezeigten Tag liegt; wenn ja, wird dieses Datum für Abholung/Auslieferung am View-Tag verwendet.
- **TourDataFilter** (customerFaelligAm):  
  Nutzt `TerminFilterUtils.istTerminVerschoben` für Abholungsdatum und `verschobeneTermine`, um fälliges Datum unter Berücksichtigung von Verschiebungen zu ermitteln.
- **CustomerButtonVisibilityHelper:**  
  V-Button sichtbar, wenn an dem Tag ein Termin (A/L) oder ein verschobener Termin (original oder verschoben am Tag) existiert; nutzt `customer.verschobeneTermine`.

## Datenmodell

- **Customer.verschobeneTermine:** `List<VerschobenerTermin>`.
- **VerschobenerTermin:**  
  `originalDatum`, `verschobenAufDatum` (Long, Default 0L), `intervallId` (String?, null), `typ` (TerminTyp, default ABHOLUNG).  
  In Firebase wird aktuell nur `originalDatum` und `verschobenAufDatum` geschrieben (Map mit Index-Keys `"0"`, `"1"`, …).

## Mögliche Schwachstellen / Randfälle

1. **Originaldatum bei „nur diesen Termin“:**  
   Es wird immer `getFaelligAm()` genutzt (oder View-Tag-Start). Bei Kunden mit mehreren Terminen pro Tag (z. B. A und L) ist „dieser Termin“ nicht eindeutig – es wird effektiv das nächste Fälligkeitsdatum verschoben. Wenn der Nutzer z. B. nur die Auslieferung verschieben will, ist das aktuell nicht abgebildet (es wird immer ein Eintrag mit `typ = ABHOLUNG` angelegt).
2. **Typ wird nicht persistiert:**  
   In Firebase steht nur `originalDatum` und `verschobenAufDatum`; `typ` und `intervallId` kommen aus den Defaults (ABHOLUNG, null). Die Logik in `TerminFilterUtils.istTerminVerschoben` filtert nach `intervallId`; da wir null schreiben, passt das. Für reine Abholungs-/Auslieferungs-Unterscheidung bei mehreren Terminen am Tag wäre Persistenz von `typ` nötig (derzeit nicht der Fall).
3. **View-Datum vs. getFaelligAm():**  
   Wenn `getFaelligAm() == 0` (z. B. Kunde ohne Intervalle oder nur vergangene Termine), wird der Start des angezeigten Tages (`viewDate`) als `originalDatum` genommen. Das ist sinnvoll, wenn der Nutzer an diesem Tag einen Termin sieht und verschiebt; sonst könnte der verschobene Eintrag einen „fiktiven“ Originaltag haben.
4. **Ersetzen nach Tagesstart:**  
   Alle bisherigen Einträge mit gleichem **Tagesstart** wie das aktuelle `originalDatum` werden entfernt und durch einen neuen ersetzt. Pro Tag (originalDatum) gibt es damit höchstens einen Verschiebe-Eintrag – ausreichend, solange nur ein Termin pro Tag verschoben wird.

## Kurzfassung

- **Ein Termin verschieben:** Nutzer wählt neues Datum und „Nur diesen Termin“ → Handler ermittelt das zu verschiebende Originaldatum (`getFaelligAm()` oder View-Tag), legt einen `VerschobenerTermin` an, aktualisiert die Liste `verschobeneTermine` (ersetzt Einträge mit gleichem Original-Tag) und speichert nur `originalDatum`/`verschobenAufDatum` als Map in Firebase. Anzeige und Fälligkeit nutzen überall `verschobeneTermine` bzw. `istTerminVerschoben`; die Verschiebung wirkt damit in Tourenplan, Karten und Terminberechnung.
