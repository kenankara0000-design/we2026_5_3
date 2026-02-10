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

### Cursor springt bei Kunde-Suche in Erfassung zurück

- **Behoben Feb 2026:** Beim Tippen ins „Kunde suchen“-Feld (Erfassung starten, Kundenpreise) sprang der Cursor an den Anfang. Ursache: Die Such-Query wurde erst asynchron im ViewModel aktualisiert. Fix: Sofortige synchrone Aktualisierung von `customerSearchQuery`, Filterung der Kundenliste weiterhin asynchron. Betroffene ViewModels: `WaschenErfassungViewModel`, `KundenpreiseViewModel`.

### Mehrere A-Tage: nur der erste wurde für Intervalle genutzt

- **Behoben Feb 2026:** `TerminAusKundeUtils.erstelleIntervalleAusKunde` erstellt jetzt ein Intervall **pro** A-Wochentag. Kunden mit mehreren A-Tagen (z. B. Mo + Mi) erhalten alle Termine in der 365-Tage-Berechnung. Rückwärtskompatibel: Ein A-Tag = ein Intervall wie zuvor.

### SevDesk Preise-Import: „chain validation failed“

- **Behoben/Bekannt Feb 2026:** Tritt nur im **Emulator** auf; auf echten Geräten funktioniert der SevDesk-Import normal. Kein App-Fix nötig (SSL-Phänomen des Emulators).

### Erledigte-Liste bei vergangenem Datum leer (z. B. gestern)

- **Behoben Feb 2026:** Zwei Ursachen: (1) `TourDataProcessor` ruft bei Vergangenheit `sammleErledigteInListen()` auf. (2) **Entscheidend:** `TerminCache.getTermineInRange()` nutzte immer `getTermine365()` (Termine nur ab heute). Bei viewDate = gestern lieferte das keine Termine → `hatKundeTerminAmDatum` false → `listenMitKunden` blieb leer → Erledigte-Sheet leer. Fix: In `TerminCache.getTermineInRange()` wird bei `startDatum` in der Vergangenheit nun direkt `TerminBerechnungUtils.berechneAlleTermineFuerKunde(…, startDatum, tageVoraus)` aufgerufen, sodass Listen für gestern befüllt werden und das Erledigt-Sheet die Listen-Kunden anzeigt.
