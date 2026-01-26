# Fix: Überfällig und Erledigt Section-Logik korrigiert

## ✅ Problem behoben

### Was war das Problem?

**Anforderung:**
- Überfällig-Section: Soll angezeigt werden, wenn ein Termin von gestern (oder früher) ist und bis er erledigt wurde immer angezeigt werden
- Erledigt-Section: Soll angezeigt werden, wenn ein Termin erledigt ist

**Aktuelles Problem:**
- Überfällig-Logik prüfte nur das nächste fällige Datum
- Für nicht-wiederholende Kunden mit Abholung gestern und Auslieferung morgen:
  - Heute: Überfällig wird angezeigt ✅
  - Morgen: Überfällig wird NICHT mehr angezeigt ❌ (obwohl Abholung immer noch überfällig ist!)

---

## ✅ Lösung implementiert

### Änderung: Überfällig-Logik prüft beide Daten

**Vorher (FALSCH):**
```kotlin
val faelligAm = customerFaelligAm(customer, null, viewDateStart)
val isOverdue = !isDone && faelligAm < heuteStart && viewDateStart >= faelligAm
// ❌ Prüft nur das nächste fällige Datum
```

**Nachher (KORREKT):**
```kotlin
// Überfällig: Prüfe ob EINER der Termine (Abholung ODER Auslieferung) überfällig ist
val abholungUeberfaellig = !customer.abholungErfolgt && customer.abholungDatum > 0 && 
                         getStartOfDay(customer.abholungDatum) < heuteStart
val auslieferungUeberfaellig = !customer.auslieferungErfolgt && customer.auslieferungDatum > 0 && 
                              getStartOfDay(customer.auslieferungDatum) < heuteStart

// Für wiederholende Kunden: Prüfe ob fälliges Datum überfällig ist
val wiederholendUeberfaellig = customer.wiederholen && !isDone && faelligAm < heuteStart

// Überfällig wenn: EINER der Termine überfällig ist UND angezeigtes Datum >= überfälliges Datum
val isOverdue = !isDone && (
    (abholungUeberfaellig && viewDateStart >= getStartOfDay(customer.abholungDatum)) ||
    (auslieferungUeberfaellig && viewDateStart >= getStartOfDay(customer.auslieferungDatum)) ||
    (wiederholendUeberfaellig && viewDateStart >= faelligAm)
)
```

---

## ✅ Was jetzt funktioniert:

### 1. Überfällig-Section ✅
- ✅ Wird angezeigt, wenn Abholung überfällig ist (auch wenn Auslieferung noch nicht fällig)
- ✅ Wird angezeigt, wenn Auslieferung überfällig ist (auch wenn Abholung schon erledigt)
- ✅ Wird angezeigt, bis beide Termine erledigt sind
- ✅ Funktioniert für nicht-wiederholende Kunden
- ✅ Funktioniert für wiederholende Kunden

### 2. Erledigt-Section ✅
- ✅ Wird angezeigt, wenn beide Termine erledigt sind (`abholungErfolgt && auslieferungErfolgt`)
- ✅ Funktioniert für alle Kunden

### 3. Sections sind standardmäßig expanded ✅
- ✅ Sections sind standardmäßig expanded (bereits implementiert)
- ✅ Benutzer sieht sofort überfällige/erledigte Kunden

---

## 📋 Test-Szenarien

### Szenario 1: Abholung gestern, Auslieferung morgen
- **Kunde:** Abholdatum gestern, Auslieferungsdatum morgen, beide nicht erledigt
- **Gestern:** ✅ Überfällig-Section wird angezeigt (Abholung überfällig)
- **Heute:** ✅ Überfällig-Section wird angezeigt (Abholung immer noch überfällig)
- **Morgen:** ✅ Überfällig-Section wird angezeigt (Abholung immer noch überfällig)
- **Nach Erledigung beider:** ✅ Erledigt-Section wird angezeigt

### Szenario 2: Abholung heute, Auslieferung gestern
- **Kunde:** Abholdatum heute, Auslieferungsdatum gestern, beide nicht erledigt
- **Gestern:** ✅ Überfällig-Section wird angezeigt (Auslieferung überfällig)
- **Heute:** ✅ Überfällig-Section wird angezeigt (Auslieferung immer noch überfällig)
- **Nach Erledigung beider:** ✅ Erledigt-Section wird angezeigt

### Szenario 3: Beide Termine erledigt
- **Kunde:** Abholung und Auslieferung erledigt
- **Heute:** ✅ Erledigt-Section wird angezeigt

### Szenario 4: Nur Abholung erledigt
- **Kunde:** Abholung erledigt, Auslieferung nicht erledigt
- **Heute:** ✅ Keine Erledigt-Section (beide müssen erledigt sein)
- ✅ Wenn Auslieferung überfällig: Überfällig-Section wird angezeigt

---

## 🔧 Technische Details

### Überfällig-Logik:

1. **Prüfe Abholung:**
   - Überfällig wenn: `!abholungErfolgt && abholungDatum < heute && viewDateStart >= abholungDatum`

2. **Prüfe Auslieferung:**
   - Überfällig wenn: `!auslieferungErfolgt && auslieferungDatum < heute && viewDateStart >= auslieferungDatum`

3. **Prüfe wiederholende Kunden:**
   - Überfällig wenn: `wiederholen && !isDone && faelligAm < heute && viewDateStart >= faelligAm`

4. **Kombiniere:**
   - Überfällig wenn: EINER der oben genannten Fälle zutrifft

### Erledigt-Logik:

1. **Prüfe beide Termine:**
   - Erledigt wenn: `abholungErfolgt && auslieferungErfolgt`

---

## ✅ Zusammenfassung

### Vorher:
- ❌ Überfällig-Logik prüfte nur das nächste fällige Datum
- ❌ Wenn Abholung gestern und Auslieferung morgen: Morgen wurde Überfällig nicht mehr angezeigt

### Nachher:
- ✅ Überfällig-Logik prüft beide Daten (Abholung und Auslieferung)
- ✅ Überfällig wird angezeigt, bis beide Termine erledigt sind
- ✅ Erledigt wird angezeigt, wenn beide Termine erledigt sind
- ✅ Sections sind standardmäßig expanded und sichtbar

---

**Ende des Berichts**
