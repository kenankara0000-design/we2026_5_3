# Analyse: Überfällig und Erledigt Section-Logik

## 🔍 Aktuelle Logik

### Überfällig-Section (Zeile 200):
```kotlin
val isOverdue = !isDone && faelligAm < heuteStart && viewDateStart >= faelligAm
```

**Bedeutung:**
- Überfällig wenn:
  - `!isDone`: Nicht erledigt
  - `faelligAm < heuteStart`: Fällig vor heute
  - `viewDateStart >= faelligAm`: Angezeigtes Datum >= fälliges Datum

**Problem:**
- ✅ Logik sieht korrekt aus
- ⚠️ Aber: Sections werden nur für "Gewerblich-Kunden ohne Liste" erstellt
- ⚠️ Listen-Kunden werden nicht in Sections angezeigt

### Erledigt-Section (Zeile 197, 203):
```kotlin
val isDone = customer.abholungErfolgt && customer.auslieferungErfolgt
if (isDone) -> doneGewerblich.add(customer)
```

**Bedeutung:**
- Erledigt wenn: Abholung UND Auslieferung erledigt

**Problem:**
- ✅ Logik sieht korrekt aus
- ⚠️ Aber: Sections werden nur für "Gewerblich-Kunden ohne Liste" erstellt

---

## 🔍 Gefundene Probleme

### Problem 1: Sections nur für Gewerblich-Kunden ohne Liste ❌

**Aktuell:**
- Sections werden nur für `filteredGewerblich` erstellt (Zeile 196-207)
- Listen-Kunden werden direkt unter Listen-Headern angezeigt (Zeile 181-189)
- Listen-Kunden werden NICHT in Sections angezeigt

**Problem:**
- Wenn ein Listen-Kunde überfällig ist, wird er nicht in "ÜBERFÄLLIG" Section angezeigt
- Wenn ein Listen-Kunde erledigt ist, wird er nicht in "ERLEDIGT" Section angezeigt

---

### Problem 2: Überfällig-Logik könnte für nicht-wiederholende Kunden problematisch sein ⚠️

**Nach meiner Änderung:**
- `customerFaelligAm()` berücksichtigt jetzt beide Daten (Abholung und Auslieferung)
- Für nicht-wiederholende Kunden: Gibt das passende Datum zurück

**Mögliches Problem:**
- Wenn Abholung heute und Auslieferung morgen ist:
  - Heute: `faelligAm` = Abholungsdatum (heute)
  - `isOverdue` = `false` (weil `faelligAm < heuteStart` = `false`)
  - ✅ Korrekt

- Wenn Abholung gestern und Auslieferung morgen ist:
  - Heute: `faelligAm` = Abholungsdatum (gestern)
  - `isOverdue` = `true` (weil `faelligAm < heuteStart` = `true`)
  - ✅ Korrekt

- Morgen: `faelligAm` = Auslieferungsdatum (morgen)
  - `isOverdue` = `false` (weil `faelligAm < heuteStart` = `false`)
  - ⚠️ Problem: Überfällige Abholung wird nicht mehr angezeigt!

**Lösung:**
- Überfällig sollte prüfen, ob EINER der Termine (Abholung ODER Auslieferung) überfällig ist
- Nicht nur das nächste fällige Datum

---

## 🔧 Lösung

### Lösung 1: Überfällig-Logik für beide Daten prüfen

**Für nicht-wiederholende Kunden:**
- Prüfe ob Abholung überfällig ist
- Prüfe ob Auslieferung überfällig ist
- Wenn EINER überfällig ist, zeige in "ÜBERFÄLLIG" Section

**Ende des Berichts**
