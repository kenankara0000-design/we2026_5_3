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

---

*Behobene Fehler werden hier entfernt (oder mit Behoben-Datum versehen und dann aus der Liste genommen).*
