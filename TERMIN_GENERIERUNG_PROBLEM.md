# Problem: Termine werden nicht für die Zukunft generiert

## 🔍 Gefundenes Problem

### Szenario:
- Kunde erstellt mit:
  - Abholdatum: heute
  - Lieferdatum: morgen
  - `wiederholen = false` (einmaliger Termin)

### Aktuelles Verhalten:
- ✅ Heute: Abholdatum wird angezeigt mit A-Button
- ❌ Morgen: Kein Termin wird angezeigt, obwohl Lieferdatum morgen ist

---

## 🔍 Ursache

### Problem 1: `customerFaelligAm()` berücksichtigt nur Abholungsdatum ❌

**In `TourPlannerViewModel.kt` Zeile 268:**
```kotlin
// Für Kunden ohne Liste: Normale Logik
val faelligAm = c.getFaelligAm()  // ❌ Gibt nur Abholungsdatum zurück!
```

**In `Customer.kt` Zeile 38-41:**
```kotlin
fun getFaelligAm(): Long {
    if (!wiederholen) {
        // Einmaliger Termin: Abholungsdatum verwenden
        return if (verschobenAufDatum > 0) verschobenAufDatum else abholungDatum
        // ❌ Berücksichtigt NICHT auslieferungDatum!
    }
}
```

**Problem:**
- Für nicht-wiederholende Kunden gibt `getFaelligAm()` nur das Abholungsdatum zurück
- Das Auslieferungsdatum wird ignoriert
- Wenn Auslieferung an einem anderen Tag ist, wird dieser Termin nicht angezeigt

---

### Problem 2: Filter-Logik prüft beide Daten, aber `customerFaelligAm` nicht ❌

**In `TourPlannerViewModel.kt` Zeile 149-156:**
```kotlin
if (!customer.wiederholen) {
    // Einmaliger Termin: Prüfe ob Abholungsdatum an diesem Tag liegt
    val abholungAm = getStartOfDay(customer.abholungDatum)
    val auslieferungAm = getStartOfDay(customer.auslieferungDatum)
    if (abholungAm != viewDateStart && auslieferungAm != viewDateStart) return@filter false
    // ✅ Prüft beide Daten
}
```

**Aber dann:**
```kotlin
val faelligAm = customerFaelligAm(customer, null, viewDateStart)
// ❌ Gibt nur Abholungsdatum zurück, nicht Auslieferungsdatum!
```

**Problem:**
- Die Filter-Logik prüft korrekt beide Daten (Abholung und Auslieferung)
- Aber `customerFaelligAm()` gibt nur Abholungsdatum zurück
- Die nachfolgende Logik verwendet `faelligAm` für Berechnungen, die dann falsch sind

---

## 🔧 Lösung

### Lösung 1: `customerFaelligAm()` muss beide Daten berücksichtigen

**Für nicht-wiederholende Kunden:**
- Prüfe sowohl Abholungs- als auch Auslieferungsdatum
- Gib das nächste fällige Datum zurück (das näher am `abDatum` liegt)
- Wenn `abDatum` zwischen Abholung und Auslieferung liegt, gib das passende zurück

**Beispiel:**
- Abholung: heute (Tag 0)
- Auslieferung: morgen (Tag 1)
- Wenn `abDatum = heute`: Gib Abholungsdatum zurück
- Wenn `abDatum = morgen`: Gib Auslieferungsdatum zurück

---

### Lösung 2: Bessere Logik für einmalige Termine

**Option A: Nächstes fälliges Datum zurückgeben**
```kotlin
if (!wiederholen) {
    val abholungStart = getStartOfDay(abholungDatum)
    val auslieferungStart = getStartOfDay(auslieferungDatum)
    val abDatumStart = getStartOfDay(abDatum)
    
    // Wenn abDatum vor beiden liegt: Gib Abholungsdatum zurück
    if (abDatumStart < abholungStart && abDatumStart < auslieferungStart) {
        return minOf(abholungDatum, auslieferungDatum)
    }
    
    // Wenn abDatum zwischen beiden liegt: Gib das passende zurück
    if (abDatumStart >= abholungStart && abDatumStart < auslieferungStart) {
        return abholungDatum
    }
    if (abDatumStart >= auslieferungStart && abDatumStart < abholungStart) {
        return auslieferungDatum
    }
    
    // Wenn abDatum nach beiden liegt: Gib das spätere zurück
    return maxOf(abholungDatum, auslieferungDatum)
}
```

**Option B: Prüfe ob abDatum auf einem der Termine liegt**
```kotlin
if (!wiederholen) {
    val abholungStart = getStartOfDay(abholungDatum)
    val auslieferungStart = getStartOfDay(auslieferungDatum)
    val abDatumStart = getStartOfDay(abDatum)
    
    // Wenn abDatum genau auf Abholungstag liegt
    if (abDatumStart == abholungStart) return abholungDatum
    
    // Wenn abDatum genau auf Auslieferungstag liegt
    if (abDatumStart == auslieferungStart) return auslieferungDatum
    
    // Wenn abDatum zwischen beiden liegt: Gib das nächste zurück
    if (abDatumStart > abholungStart && abDatumStart < auslieferungStart) {
        return auslieferungDatum  // Nächstes fälliges Datum
    }
    if (abDatumStart > auslieferungStart && abDatumStart < abholungStart) {
        return abholungDatum  // Nächstes fälliges Datum
    }
    
    // Wenn abDatum vor beiden liegt: Gib Abholungsdatum zurück
    if (abDatumStart < abholungStart && abDatumStart < auslieferungStart) {
        return abholungDatum
    }
    
    // Wenn abDatum nach beiden liegt: Gib das spätere zurück
    return maxOf(abholungDatum, auslieferungDatum)
}
```

---

## 📋 Zusammenfassung

### Was funktioniert NICHT:
- ❌ `customerFaelligAm()` berücksichtigt nur Abholungsdatum für nicht-wiederholende Kunden
- ❌ Auslieferungsdatum wird ignoriert
- ❌ Termine werden nicht für die Zukunft generiert (nur Abholungstag wird angezeigt)

### Was behoben werden muss:
- ✅ `customerFaelligAm()` muss beide Daten (Abholung und Auslieferung) berücksichtigen
- ✅ Für nicht-wiederholende Kunden: Gib das nächste fällige Datum zurück
- ✅ Termine müssen für beide Tage (Abholung und Auslieferung) angezeigt werden

---

**Ende des Berichts**
