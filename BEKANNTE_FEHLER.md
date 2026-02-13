# Bekannte Fehler (Bug-Punkte)

**Single Source of Truth** für alle dokumentierten Bugs der App we2026_5.  
Neue bekannte Fehler hier eintragen; behobene Fehler mit Datum/Hinweis entfernen oder als „behoben“ markieren.

Keine Änderung am Verhalten (Bug-Fix) ohne ausdrückliche Freigabe (vgl. `.cursor/rules/nicht-kaputt-machen.mdc`).

---

## Offen

### Standardpreisliste: Löschen ohne Bestätigung (historisch)

- **Hinweis:** Die „Preisliste Tour / Privat“ wurde in **Standardpreisliste** umbenannt (`StandardPreislisteScreen.kt`). Im neuen Screen wird beim Löschen ein Bestätigungsdialog angezeigt (`ConfirmDialog`). Falls in einer älteren Version noch ohne Dialog gelöscht wurde, betrifft das die alte TourPreisliste; die aktuelle Standardpreisliste hat einen Lösch-Dialog.
- **Relevante Stelle:** `ui/wasch/StandardPreislisteScreen.kt` (Delete → ConfirmDialog, dann `onRemoveStandardPreis`).

### Erfassung-Menü: „Kamera / Foto“ wirkt „kaputt“ (öffnet nicht sofort Kamera/Formular)

- **Symptom:** In `Erfassung` (Menü) führt „Kamera / Foto“ nicht direkt wie im Kundendetail-Tab „Belege“ in den Kamera/Formular-Flow; es ist zuerst eine Kundenauswahl nötig, erst danach öffnet sich das Formular inkl. Kamera/Foto.
- **Ursache:** `WaschenErfassungActivity` kann das Wäscheliste-Formular nur für einen ausgewählten Kunden öffnen. Das Intent-Flag `OPEN_FORMULAR_WITH_CAMERA` wird erst verarbeitet, sobald der UI-State `ErfassungenListe(customer)` erreicht ist.
- **Relevante Stellen:** `ErfassungMenuActivity.kt` (Extra `OPEN_FORMULAR_WITH_CAMERA`), `WaschenErfassungActivity.kt` (`openFormularWithCameraWhenReady` + `LaunchedEffect`), `WaschenErfassungViewModel.openFormularWithCamera()`.

### Erfassung: Android-Zurück beendet Screen statt 1x zurück zur Kunden-Ansicht

- **Symptom:** Nach Kundenauswahl → „Neue Erfassung“ → Art wählen (Formular/Manuell): Beim Drücken der Android-Zurück-Taste landet man wieder „unter Erfassung“ (Activity wird beendet) statt erst zurück zur Kunden-Ansicht (Belege-Liste).
- **Ursache:** System-Back wurde nicht auf die Compose-State-Navigation gemappt; dadurch griff das Default-Back-Verhalten der Activity.
- **Fix:** `BackHandler` in `WaschenErfassungActivity.kt` hinzugefügt, der identisch zum TopBar-Back durch die UI-States navigiert.
- **Relevante Stellen:** `WaschenErfassungActivity.kt` (BackHandler/handleBack), `WaschenErfassungScreen` Back-Callback.

### Tourenplaner: Überfällige Listenkunden aus Listen ohne Wochentag erscheinen nicht in der Sektion „Überfällig“

- **Symptom:** Kunde (z. B. Lutze Rötha) mit Fälligkeit 12.02. wird am 12.02. als überfällig angezeigt, am 13.02. (heute, keine Termine für heute) aber nicht in der Sektion „Überfällig“. Erwartung: Überfällige sollen am Fälligkeitstag und am heutigen Tag immer erscheinen, bis erledigt.
- **Ursache:** Die zentrale Sektion „Überfällig“ oben im Tourenplaner wird nur aus (1) Kunden ohne Liste (Gewerblich/Privat ohne listeId) und (2) Kunden aus **Wochentagslisten** (Listen mit wochentag 0..6) befüllt. **Listenkunden aus Listen ohne Wochentag** (wochentag z. B. -1) werden dort nie eingetragen; sie erscheinen nur in ihrer Listen-Karte. Wenn am angezeigten Tag keine anderen Fälligen in der Liste sind, wirkt die Überfällig-Anzeige „weg“.
- **Hinweis:** Hat nichts mit Zeitzone zu tun (Berlin ist überall gesetzt). Die Einzel-Logik „sollUeberfaelligAnzeigen“ (Fälligkeitstag + heutiger Tag) ist korrekt; die Zuordnung zur Sektion „Überfällig“ berücksichtigt Kunden aus Listen ohne Wochentag nicht.
- **Relevante Stellen:** `tourplanner/TourDataProcessor.kt` (Befüllung `alleUeberfaelligeKunden` nur über overdueGewerblich + Wochentagslisten; Listen ohne Wochentag werden ausgelassen).

### Tourenplaner: Kunde mit A erledigt, L offen/überfällig erscheint nicht als überfällig

- **Symptom:** Erledigung A ist korrekt (z. B. Abholung 12.02 erledigt). Am heutigen Tag (13.02) wird der Kunde trotzdem nicht als überfällig angezeigt. Regel „Erledigung darf nur am Datum Heute“: Ein Termin von gestern kann man heute nicht mehr erledigen – dadurch entsteht ein Konflikt, wenn der Kunde dann auch nicht mehr in der Überfällig-Liste erscheint.
- **Ursache:** Die Überfällig-Prüfung behandelt den Kunden als „erledigt“, sobald **entweder** A **oder** L erledigt ist. Wenn A erledigt ist, wird der Kunde nie in die Sektion „Überfällig“ aufgenommen – auch wenn L noch offen oder überfällig ist. Er erscheint also nicht mehr, obwohl noch etwas offen ist; ein Nachholen der Erledigung am Folgetag ist durch die Regel „nur am Datum Heute“ ohnehin nicht vorgesehen.
- **Erwartung:** Überfällig anzeigen, sobald **irgendein** überfälliger Termin (A oder L) noch nicht erledigt ist. Kunde mit A erledigt, L überfällig → weiterhin als überfällig anzeigen (für L).
- **Relevante Stelle:** `tourplanner/TourDataFilter.kt` (`istKundeUeberfaellig`: Abbruch bei `abholungErfolgt || auslieferungErfolgt`; sollte pro Termin-Typ prüfen, ob noch ein überfälliger A oder L offen ist).

---

*Behobene Fehler werden hier entfernt (oder mit Behoben-Datum versehen und dann aus der Liste genommen).*
