# Bekannte Fehler (Bug-Punkte)

**Single Source of Truth** für alle dokumentierten Bugs der App we2026_5.  
Neue bekannte Fehler hier eintragen; behobene Fehler mit Datum/Hinweis entfernen oder als „behoben“ markieren.

Keine Änderung am Verhalten (Bug-Fix) ohne ausdrückliche Freigabe (vgl. `.cursor/rules/nicht-kaputt-machen.mdc`).

---

## Offen

### Erfassung-Menü: „Kamera / Foto“ wirkt „kaputt“ (öffnet nicht sofort Kamera/Formular)

- **Symptom:** In `Erfassung` (Menü) führt „Kamera / Foto“ nicht direkt wie im Kundendetail-Tab „Belege“ in den Kamera/Formular-Flow; es ist zuerst eine Kundenauswahl nötig, erst danach öffnet sich das Formular inkl. Kamera/Foto.
- **Ursache:** `WaschenErfassungActivity` kann das Wäscheliste-Formular nur für einen ausgewählten Kunden öffnen. Das Intent-Flag `OPEN_FORMULAR_WITH_CAMERA` wird erst verarbeitet, sobald der UI-State `ErfassungenListe(customer)` erreicht ist.
- **Relevante Stellen:** `ErfassungMenuActivity.kt` (Extra `OPEN_FORMULAR_WITH_CAMERA`), `WaschenErfassungActivity.kt` (`openFormularWithCameraWhenReady` + `LaunchedEffect`), `WaschenErfassungViewModel.openFormularWithCamera()`.

### Erfassung: Android-Zurück beendet Screen statt 1x zurück zur Kunden-Ansicht

- **Symptom:** Nach Kundenauswahl → „Neue Erfassung“ → Art wählen (Formular/Manuell): Beim Drücken der Android-Zurück-Taste landet man wieder „unter Erfassung“ (Activity wird beendet) statt erst zurück zur Kunden-Ansicht (Belege-Liste).
- **Ursache:** System-Back wurde nicht auf die Compose-State-Navigation gemappt; dadurch griff das Default-Back-Verhalten der Activity.
- **Fix:** `BackHandler` in `WaschenErfassungActivity.kt` hinzugefügt, der identisch zum TopBar-Back durch die UI-States navigiert.
- **Relevante Stellen:** `WaschenErfassungActivity.kt` (BackHandler/handleBack), `WaschenErfassungScreen` Back-Callback.

---

## Behoben

### Tour-Listen: Erledigung funktioniert nicht; Wochentag nicht sichtbar; Termine nicht auf Kunden übernommen

- **Behoben Feb 2026:** Erledigung für Tour-Listen-Kunden, Wochentag-Anzeige beim Bearbeiten der Liste sowie Übernahme der Listen-Termine auf die Kunden (termineVonListe) wurden umgesetzt. Tour-Listen-Kunden können im Tourenplaner erledigt werden; Wochentag A wird angezeigt und Termine werden korrekt auf die Kunden übernommen.

### (historisch – ursprünglicher Eintrag)
- **Symptom:** (1) Tour-Listen-Kunden konnten nicht erledigt werden (A/L-Buttons blockiert oder „nur heute“). (2) Beim Bearbeiten der Liste wurde kein Wochentag als ausgewählt angezeigt. (3) Die Termine der Tour-Liste erschienen nicht bei den Tour-Kunden.
- **Relevante Stellen:** `TourPlannerDateUtils`, `TourPlannerActivity` Erledigung-Sheet, `ListeBearbeitenScreen` / `ListeBearbeitenListenTermineSection`, `ListeBearbeitenCallbacks.syncTermineVonListeToKunden`, `addListenTerminFromWochentag`.

### Bereits erledigte Termine erschienen wieder als nicht erledigt

- **Behoben Feb 2026:** Nach dem erneuten Öffnen der App oder nach Neuladen wurden Termine, die bereits als erledigt markiert waren, wieder als nicht erledigt angezeigt. Ursache: Beim Lesen aus der Firebase Realtime DB werden Zahlen teils als Double geliefert; die Standard-Deserialisierung (`getValue(Customer::class.java)`) setzte die Long-Felder `abholungErledigtAm`, `auslieferungErledigtAm` usw. dann auf 0. Fix: In `CustomerSnapshotParser` werden die Erledigungsfelder (abholungErfolgt, auslieferungErfolgt, abholungErledigtAm, auslieferungErledigtAm, abholungZeitstempel, auslieferungZeitstempel, keinerWäscheErfolgt, keinerWäscheErledigtAm, faelligAmDatum) explizit mit `optionalLong`/`optionalBoolean` gelesen und in das Customer-Objekt übernommen – Long-Werte kommen so korrekt an (Number→Long).

### Ausnahme-Termine: A+L konnten nicht erstellt werden

- **Behoben Feb 2026:** Im Ausnahme-Termin-Dialog (Einmalig – Ausnahme) fehlte die Option „Abholung + Auslieferung (A+L)“. Nur „Nur Abholung“ und „Nur Auslieferung“ waren wählbar; die Methode `saveAusnahmeAbholungMitLieferung` existierte, wurde aber nie aufgerufen. Fix: Dialog auf Listenauswahl (setItems) umgestellt mit dritter Option „Abholung + Auslieferung (A+L)“, die `addAusnahmeAbholungMitLieferung` nutzt. Relevante Stelle: `AusnahmeTerminActivity.kt`.

### Ausnahme-Termine: Kalender war an A/L-Wochentage gebunden

- **Behoben Feb 2026:** Der Kalender in AusnahmeTerminActivity zeigte A/L-Wochentage farblich hervorgehoben, wodurch der Eindruck entstand, Ausnahme-Termine seien nur an diesen Tagen möglich. Ausnahme-Termine sollen aber an jedem Datum und für jeden Typ (A, L, A+L) erstellt werden können. Fix: `aWochentage` und `lWochentage` werden nun als leere Listen übergeben – der Kalender ist neutral, jeder Tag gleich darstellbar.

### Cursor springt bei Kunde-Suche in Erfassung zurück

- **Behoben Feb 2026:** Beim Tippen ins „Kunde suchen“-Feld (Erfassung starten, Kundenpreise) sprang der Cursor an den Anfang. Ursache: Die Such-Query wurde erst asynchron im ViewModel aktualisiert. Fix: Sofortige synchrone Aktualisierung von `customerSearchQuery`, Filterung der Kundenliste weiterhin asynchron. Betroffene ViewModels: `WaschenErfassungViewModel`, `KundenpreiseViewModel`.

### Mehrere A-Tage: nur der erste wurde für Intervalle genutzt

- **Behoben Feb 2026:** `TerminAusKundeUtils.erstelleIntervalleAusKunde` erstellt jetzt ein Intervall **pro** A-Wochentag. Kunden mit mehreren A-Tagen (z. B. Mo + Mi) erhalten alle Termine in der 365-Tage-Berechnung. Rückwärtskompatibel: Ein A-Tag = ein Intervall wie zuvor.

### SevDesk Preise-Import: „chain validation failed“

- **Behoben/Bekannt Feb 2026:** Tritt nur im **Emulator** auf; auf echten Geräten funktioniert der SevDesk-Import normal. Kein App-Fix nötig (SSL-Phänomen des Emulators).

### Erledigte-Liste bei vergangenem Datum leer (z. B. gestern)

- **Behoben Feb 2026:** Zwei Ursachen: (1) `TourDataProcessor` ruft bei Vergangenheit `sammleErledigteInListen()` auf. (2) **Entscheidend:** `TerminCache.getTermineInRange()` nutzte immer `getTermine365()` (Termine nur ab heute). Bei viewDate = gestern lieferte das keine Termine → `hatKundeTerminAmDatum` false → `listenMitKunden` blieb leer → Erledigte-Sheet leer. Fix: In `TerminCache.getTermineInRange()` wird bei `startDatum` in der Vergangenheit nun direkt `TerminBerechnungUtils.berechneAlleTermineFuerKunde(…, startDatum, tageVoraus)` aufgerufen, sodass Listen für gestern befüllt werden und das Erledigt-Sheet die Listen-Kunden anzeigt.

### Ausnahme-Termine: 01.01.70 statt nur A/L, normale Badges statt A-A/A-L gelb

- **Behoben Feb 2026:** Ausnahme-Termine (nur A oder nur L) wurden in Termine-Tab und Alle-Termine-Block falsch angezeigt: (1) Bei nur L zeigte sich „A 01.01.70“ (epoch), bei nur A „L 01.01.70“. (2) Ausnahme-Termine sahen aus wie normale A/L (blau/grün) statt A-A/A-L (gelb). Fix: In `ErledigungTabTerminContent` und `AlleTermineBlock` werden 0-Daten nicht mehr angezeigt; Ausnahme-Termine (Pair mit einem 0-Wert) werden mit A-A/A-L und Farbe `status_ausnahme` dargestellt.

### Kunden-Termine: „mögliche“ Termine nicht als solche erkennbar (sollten ausgegraut sein)

- **Behoben Feb 2026:** Nur für **unregelmäßige** Kunden (`kundenTyp=UNREGELMAESSIG`): Im „Alle Termine“-Block (Kundendetail → Termine & Tour) und im Aktionen-Sheet (TourPlanner → Tab „Termin“) werden „mögliche“ (nur berechnete) Termine **neutral grau** dargestellt (ohne A/L-Farben), bis der Termin tatsächlich als Kunden-/Ausnahme-Termin angelegt ist. Relevante Stellen: `ui/detail/AlleTermineBlock.kt`, `ui/detail/CustomerDetailTermineTab.kt`, `ui/tourplanner/ErledigungTabTerminContent.kt`.

### „+ Termin“: Unregelmäßig zeigte „Einmalig – Kunden-Termin“ nicht an

- **Behoben Feb 2026:** Bei unregelmäßigen Kunden (`kundenTyp=UNREGELMAESSIG`) soll das „+ Termin“-Sheet nur **Einmalig – Kunden-Termin**, **Einmalig – Ausnahme** und **Urlaub** anbieten. Vorher fehlte „Einmalig – Kunden-Termin“ (es wurden nur Ausnahme/Urlaub angezeigt). Fix: Allowed-Optionen im `NeuerTerminArtSheet` angepasst. Relevante Stelle: `ui/detail/NeuerTerminArtSheet.kt`.

### Einmalig – Kunden-Termin: Bestätigungsdialog zeigte „Ausnahme“

- **Behoben Feb 2026:** Beim Anlegen eines „Einmalig – Kunden-Termins“ (A+L) wurde im Bestätigungsdialog fälschlich „Ausnahme-Termin … anlegen?“ angezeigt, obwohl korrekt ein Kunden-Termin gespeichert wurde. Fix: Eigener Dialogtext + Titel für Einmalig-Kunde im `AusnahmeTerminActivity`. Relevante Stellen: `AusnahmeTerminActivity.kt`, `strings.xml` (`dialog_kunden_termin_bestaetigen`).
