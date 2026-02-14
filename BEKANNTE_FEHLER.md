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

### Tourenplaner: Drag-Drop funktionierte nur für erste 2 Termine

- **Behoben [2026-02-14]:** Ursache war eine fehlerhafte Custom-Drag-Drop-Implementierung, die auf `LazyListItemInfo` basierte und bei gemischten Item-Typen (Header + Kunden) nicht korrekt funktionierte. Fix: Integration der `org.burnoutcrew.reorderable`-Bibliothek mit separater flacher Kundenliste im Bearbeiten-Modus. Toter Code (TourPlannerDragDrop.kt, ungenutzte Drag-Parameter) entfernt.
- **Relevante Stellen:** TourPlannerScreen.kt, TourPlannerCustomerRow.kt.
