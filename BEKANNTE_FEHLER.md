# Bekannte Fehler (Bug-Punkte)

**Single Source of Truth** für alle dokumentierten Bugs der App we2026_5.  
Neue bekannte Fehler hier eintragen; behobene Fehler mit Datum/Hinweis entfernen oder als „behoben“ markieren.

Keine Änderung am Verhalten (Bug-Fix) ohne ausdrückliche Freigabe (vgl. `.cursor/rules/nicht-kaputt-machen.mdc`).

---

## Offen

### Tour-Listen: Erledigung funktioniert nicht; Wochentag nicht sichtbar; Termine nicht auf Kunden übernommen

- **Symptom:** (1) Tour-Listen-Kunden können nicht erledigt werden (A/L-Buttons blockiert oder „nur heute“). (2) Beim Bearbeiten der Liste wird kein Wochentag als ausgewählt angezeigt. (3) Die Termine der Tour-Liste erscheinen nicht bei den Tour-Kunden (Tourenplaner zeigt keine fälligen Termine / Erledigung nicht möglich).
- **Ursache (kurz):**
  - **Erledigung:** Die Erledigungslogik (Abholung/Auslieferung „heute fällig?“) nutzt nur Kundendaten, **ohne** die Tour-Liste zu übergeben. Termine kommen dann nur aus `termineVonListe` am Kunden. Wenn `termineVonListe` leer ist, gibt es keinen heutigen Termin → Erledigung wird verweigert.
  - **Wochentag:** Beim Bearbeiten einer Tour-Liste wird „Wochentag A“ nur angezeigt, wenn die Liste als Tour-Liste geführt wird (`wochentag` nicht 0..6). Der **ausgewählte** Wochentag kommt aus `wochentagA`. Ist `wochentagA` nie gesetzt (null/-1), zeigt die Chip-Zeile keinen ausgewählten Tag – und „Termin anlegen“ (aus Wochentag) kann keine Termine erzeugen.
  - **Übernahme auf Kunden:** Die Listen-Termine werden auf die Kunden übertragen (`termineVonListe`), wenn: ein Listen-Termin **hinzugefügt** oder **entfernt** wird; ein Kunde **zur Liste hinzugefügt** wird (dann mit den aktuellen listenTermine); oder beim **Öffnen** „Liste bearbeiten“ (Backfill für Kunden mit leerem `termineVonListe`). Wenn zuerst Kunden zur Liste kamen und **danach** erst „Wochentag A“ gesetzt und Termine angelegt wurden: Ohne gesetzten Wochentag A kann man keine Termine anlegen → kein Sync → Kunden behalten leere `termineVonListe` → Erledigung findet „keinen Termin heute“.
- **Relevante Stellen:** `TourPlannerDateUtils` (calculateAbholungDatum/calculateAuslieferungDatum mit `liste = null`); `TourPlannerActivity` Erledigung-Sheet: `getTermineFuerKunde` ohne Liste; `ListeBearbeitenScreen` / `ListeBearbeitenListenTermineSection` (Anzeige nur bei Tour-Liste, Auswahl `wochentagA`); `ListeBearbeitenCallbacks.syncTermineVonListeToKunden` / Backfill in `ListeBearbeitenViewModel`; `addListenTerminFromWochentag` bricht ab, wenn `wochentagA` nicht in 0..6.

---

## Behoben

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
