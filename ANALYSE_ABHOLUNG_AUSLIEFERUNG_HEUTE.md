# Analyse: A/L/KW-Buttons – „nur am Heute erledigt“ obwohl heute

## Symptom
- Beim Drücken von **A** (Abholung), **L** (Auslieferung) oder **KW** (Keine Wäsche) erscheint die Meldung, dass Termine nur am Tag „Heute“ erledigt werden können.
- Die Erledigung wird **nicht** gespeichert, obwohl der Nutzer den Tag „Heute“ anzeigt und es tatsächlich heute ist.

---

## Ablauf (zwei Prüfungen)

### 1. Prüfung im Binder (CustomerViewHolderBinder)
- **Wo:** `setupClickListeners` – vor dem Aufruf von `onAbholung` / `onAuslieferung` / `onKw`.
- **Bedingung:** `istHeute = (viewDateStart == heuteStart)`
  - `viewDateStart` = Start des **angezeigten** Tages im Tourenplaner (`displayedDateMillis`).
  - `heuteStart` = Start des **aktuellen** Tages (`System.currentTimeMillis()`).
- **Meldung bei Fehler:** *„Termine können nur am Tag Heute erledigt werden.“*
- **Folge:** Wenn hier `istHeute == false`, wird die Meldung gezeigt und der Handler **gar nicht** aufgerufen.

Wenn der Nutzer also **heute** anzeigt und tippt, ist `istHeute == true` und der Handler wird aufgerufen. Die Meldung, die der Nutzer sieht, kommt daher sehr wahrscheinlich von der **zweiten** Prüfung.

### 2. Prüfung im Handler (TourPlannerCallbackHandler)
- **Wo:** `handleAbholung` / `handleAuslieferung` / `handleKw`.
- **Bedingung:**  
  - A: `istTerminHeuteFaellig(customer, ABHOLUNG, heuteStart)`  
  - L: `istTerminHeuteFaellig(customer, AUSLIEFERUNG, heuteStart)`  
  - KW: mindestens einer von A/L „heute fällig“.
- **Meldung bei Fehler:**  
  - A: *„Abholung kann nur erledigt werden, wenn das Datum heute ist.“*  
  - L: *„Auslieferung kann nur erledigt werden, wenn das Datum heute ist.“*  
  - KW: *„KW (Keine Wäsche) nur an Abholungs- oder Auslieferungstag.“*
- **Folge:** Wenn `istTerminHeuteFaellig` hier `false` liefert, wird die Meldung gezeigt, **ohne** Firebase-Update. Daher wird nichts als erledigt gespeichert.

**Fazit:** Das Verhalten (Meldung + keine Speicherung) passt dazu, dass die **zweite** Prüfung (`istTerminHeuteFaellig`) fälschlich `false` zurückgibt, obwohl ein Termin „heute“ fällig ist.

---

## Funktion `istTerminHeuteFaellig`

**Datei:** `TourPlannerCallbackHandler.kt`

**Logik (vereinfacht):**
1. Termine für den Kunden laden:  
   `berechneAlleTermineFuerKunde(customer, liste, startDatum = heuteStart - 1 Tag, tageVoraus = 2)`  
   → Suchfenster: **gestern** bis **morgen** (2 Tage).
2. **Normal fällig:** Es gibt einen Termin des Typs (A oder L) mit `getStartOfDay(it.datum) == heuteStart` → `true`.
3. **Überfällig:** Falls kein solcher Termin, wird geprüft, ob ein überfälliger Termin „heute“ angezeigt werden soll (`sollUeberfaelligAnzeigen`) → ggf. `true`.
4. Sonst → `false`.

Mögliche Ursachen für falsches `false`:

1. **Suchfenster zu klein**  
   Nur `heuteStart - 1 Tag` bis `heuteStart + 2 Tage`. Je nach Zeitzone, Rundung oder Intervall-Logik könnte „heute“ knapp außerhalb des Fensters liegen oder der erste Treffer fehlen.

2. **Termin-Berechnung**  
   Wenn `berechneAlleTermineFuerKunde` / `berechneWiederholendeTermine` für den Kunden (z. B. wochentagsbasiert, Intervall 7 Tage) an der Grenze des 2-Tage-Fensters keinen Termin mit `datum == heuteStart` erzeugt, findet Schritt 2 keinen Treffer.

3. **Zeitzone / Tagesgrenze**  
   `getStartOfDay` nutzt `Calendar.getInstance()` (Standard-Zeitzone). Wenn irgendwo mit UTC oder anderer Zeitzone gearbeitet wird, kann „heute“ in einer Logik 00:00 UTC und in der anderen 00:00 Lokalzeit sein → Vergleich schlägt fehl.

4. **Überfällig-Pfad**  
   Wenn der Termin nur als „überfällig“ zählt und `sollUeberfaelligAnzeigen` für `anzeigeDatum = heuteStart` und `aktuellesDatum = heuteStart` nicht greift, liefert die Überfällig-Prüfung kein `true`.

---

## Empfohlene Änderungen

1. **Suchfenster in `istTerminHeuteFaellig` vergrößern**  
   Statt 1 Tag zurück und 2 Tage voraus z. B.:
   - `startDatum = heuteStart - 7 Tage`
   - `tageVoraus = 14`  
   So ist „heute“ sicher im Fenster, auch bei Randfällen (Intervall, Wochentag, erste Berechnung).

2. **Zusätzliches Logging (optional)**  
   In `istTerminHeuteFaellig` kurz loggen:
   - `heuteStart` (z. B. als formatiertes Datum),
   - Anzahl und Typ der gefundenen Termine,
   - ob ein Termin mit `getStartOfDay(it.datum) == heuteStart` gefunden wurde.  
   Das hilft, ob die Ursache beim Fenster, der Termin-Berechnung oder dem Datumsvergleich liegt.

3. **Keine inhaltliche Abschwächung der Prüfung**  
   Die Prüfung „nur am Heute erledigt“ soll bestehen bleiben; nur die **Ermittlung**, ob ein Termin „heute“ fällig ist, soll robuster werden (größeres Fenster + gleiche Logik).

---

## Kurzfassung
- Die Meldung kommt aus dem **TourPlannerCallbackHandler**, weil `istTerminHeuteFaellig` `false` liefert.
- Dadurch wird das Firebase-Update nicht ausgeführt → keine Erledigung trotz „Heute“.
- Ursache liegt sehr wahrscheinlich in der **Termin-Suche** (zu kleines Fenster oder Randfall in der Termin-Berechnung), nicht in der ersten Prüfung im Binder.
- **Konkrete Maßnahme:** In `istTerminHeuteFaellig` das Suchfenster auf z. B. 7 Tage zurück und 14 Tage voraus erweitern und ggf. Logging ergänzen.
