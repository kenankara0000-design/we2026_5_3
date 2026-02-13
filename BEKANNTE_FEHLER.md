# Bekannte Fehler (Bug-Punkte)

**Single Source of Truth** für alle dokumentierten Bugs der App we2026_5.  
Neue bekannte Fehler hier eintragen; behobene Fehler mit Datum/Hinweis entfernen oder als „behoben“ markieren.

Keine Änderung am Verhalten (Bug-Fix) ohne ausdrückliche Freigabe (vgl. `.cursor/rules/nicht-kaputt-machen.mdc`).

---

## Offen

### Listen- und Privat-Kundenpreise: Löschen ohne Bestätigung (nur historischer Hinweis, kein aktueller Bug)

- **Hinweis:** In der App heißt der Eintrag **„Listen- und Privat-Kundenpreise“**. Beim Löschen wird dort ein Bestätigungsdialog angezeigt. Dieser Eintrag dient nur der Dokumentation: Falls in einer älteren Version ohne Dialog gelöscht wurde, betraf das die alte Preisliste; die heutige hat den Lösch-Dialog.
- **Relevante Stelle:** Screen für Listen- und Privat-Kundenpreise (Delete → ConfirmDialog).

### Erfassung-Menü: „Kamera / Foto“ wirkt „kaputt“ (öffnet nicht sofort Kamera/Formular)

- **Symptom:** Im Erfassung-Menü führt „Kamera / Foto“ nicht direkt in den Kamera/Formular-Flow wie im Kundendetail-Tab „Belege“; es ist zuerst eine Kundenauswahl nötig, danach öffnet sich das Formular inkl. Kamera/Foto.
- **Ursache:** Die WaschenErfassungActivity kann das Wäscheliste-Formular nur für einen bereits ausgewählten Kunden öffnen. Das Intent-Flag für „Kamera sofort“ wird erst verarbeitet, sobald der Zustand „ErfassungenListe (Kunde)“ erreicht ist.
- **Relevante Stellen:** ErfassungMenuActivity, WaschenErfassungActivity, WaschenErfassungViewModel.

---

## Behoben

### Belege: Android-Zurück vom Beleg-Detail ging ins Erfassungs-Menü (Behoben 2026-02-13)

- **War:** Beleg öffnen → Zurück drücken → landete im Erfassungs-Menü statt einmal zurück in die Belege-Liste.
- **Ursache:** BelegeActivity hatte keinen BackHandler; System-Zurück beendete die Activity.
- **Fix:** BackHandler in BelegeActivity (analog WaschenErfassungActivity) führt bei Zurück die gleiche Logik aus wie die TopBar (BelegDetail → backFromBelegDetail(), BelegListe → backToAlleBelege() usw., nur bei AlleBelege → finish()).

### Erfassung: Android-Zurück beendete Screen (Behoben)

- **War:** Nach Kundenauswahl → „Neue Erfassung“ → Art wählen: Android-Zurück beendete die Activity statt einmal zurück zur Kunden-Ansicht zu gehen.
- **Fix:** BackHandler in WaschenErfassungActivity navigiert nun mit handleBack durch die UI-States wie das TopBar-Back.

### Tourenplaner: Überfällige Listenkunden aus Listen ohne Wochentag (Behoben 2026-02-13)

- Überfällige Kunden aus Listen ohne Wochentag werden nun ebenfalls in die Sektion „Überfällig“ aufgenommen (TourDataProcessor).

### Tourenplaner: Kunde mit A erledigt, L offen/überfällig (Behoben 2026-02-13)

- Überfällig wird pro Termin-Typ (A/L) geprüft: Kunde erscheint weiter in „Überfällig“, solange mindestens ein überfälliger A- oder L-Termin noch nicht erledigt ist (TourDataFilter, istKundeUeberfaellig).

---

*Behobene Fehler können aus der Liste genommen werden, sobald nicht mehr benötigt.*
