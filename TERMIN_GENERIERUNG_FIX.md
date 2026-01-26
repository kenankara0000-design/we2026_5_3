# Fix: Termine werden jetzt für die Zukunft generiert

## ✅ Problem behoben

### Was war das Problem?

**Szenario:**
- Kunde erstellt mit:
  - Abholdatum: heute
  - Lieferdatum: morgen
  - `wiederholen = false` (einmaliger Termin)

**Aktuelles Verhalten (VORHER):**
- ✅ Heute: Abholdatum wird angezeigt mit A-Button
- ❌ Morgen: Kein Termin wird angezeigt, obwohl Lieferdatum morgen ist

**Ursache:**
- `customerFaelligAm()` berücksichtigte nur das Abholungsdatum für nicht-wiederholende Kunden
- Das Auslieferungsdatum wurde ignoriert
- `getFaelligAm()` in `Customer.kt` gibt nur `abholungDatum` zurück, nicht `auslieferungDatum`

---

## ✅ Lösung implementiert

### Änderung in `TourPlannerViewModel.kt`:

**Vorher (FALSCH):**
```kotlin
// Für Kunden ohne Liste: Normale Logik
val faelligAm = c.getFaelligAm()  // ❌ Gibt nur Abholungsdatum zurück
return faelligAm
```

**Nachher (KORREKT):**
```kotlin
if (!c.wiederholen) {
    // Einmaliger Termin: Berücksichtige sowohl Abholungs- als auch Auslieferungsdatum
    val abholungStart = getStartOfDay(c.abholungDatum)
    val auslieferungStart = getStartOfDay(c.auslieferungDatum)
    val abDatumStart = getStartOfDay(abDatum)
    
    // Prüfe ob abDatum genau auf Abholungstag liegt
    if (abDatumStart == abholungStart && !abholungGeloescht) {
        return c.abholungDatum
    }
    
    // Prüfe ob abDatum genau auf Auslieferungstag liegt
    if (abDatumStart == auslieferungStart && !auslieferungGeloescht) {
        return c.auslieferungDatum
    }
    
    // Wenn abDatum zwischen beiden liegt: Gib das nächste fällige Datum zurück
    // ... weitere Logik
}
```

---

## ✅ Was jetzt funktioniert:

### 1. Einmalige Termine mit verschiedenen Daten ✅
- ✅ Abholung heute, Auslieferung morgen:
  - Heute: Abholungstermin wird angezeigt
  - Morgen: Auslieferungstermin wird angezeigt

### 2. Beide Daten werden berücksichtigt ✅
- ✅ `customerFaelligAm()` prüft sowohl Abholungs- als auch Auslieferungsdatum
- ✅ Gibt das passende Datum zurück, basierend auf dem angezeigten Tag

### 3. Gelöschte Termine werden berücksichtigt ✅
- ✅ Wenn Abholung gelöscht wurde, wird nur Auslieferung angezeigt
- ✅ Wenn Auslieferung gelöscht wurde, wird nur Abholung angezeigt
- ✅ Wenn beide gelöscht wurden, wird kein Termin angezeigt

### 4. Verschobene Termine ✅
- ✅ Verschobene Termine werden korrekt berücksichtigt
- ✅ Gelöschte verschobene Termine werden ignoriert

---

## 📋 Test-Szenarien

### Szenario 1: Abholung heute, Auslieferung morgen
- **Kunde:** Abholdatum heute, Auslieferungsdatum morgen, `wiederholen = false`
- **Heute:** ✅ Abholungstermin wird angezeigt mit A-Button
- **Morgen:** ✅ Auslieferungstermin wird angezeigt mit L-Button
- **Andere Tage:** ✅ Keine Termine

### Szenario 2: Nur Abholung
- **Kunde:** Abholdatum heute, kein Auslieferungsdatum, `wiederholen = false`
- **Heute:** ✅ Abholungstermin wird angezeigt mit A-Button
- **Andere Tage:** ✅ Keine Termine

### Szenario 3: Nur Auslieferung
- **Kunde:** Kein Abholdatum, Auslieferungsdatum morgen, `wiederholen = false`
- **Morgen:** ✅ Auslieferungstermin wird angezeigt mit L-Button
- **Andere Tage:** ✅ Keine Termine

### Szenario 4: Abholung gelöscht
- **Kunde:** Abholdatum heute (gelöscht), Auslieferungsdatum morgen, `wiederholen = false`
- **Heute:** ✅ Kein Termin (Abholung gelöscht)
- **Morgen:** ✅ Auslieferungstermin wird angezeigt mit L-Button

---

## 🔧 Technische Details

### Logik für nicht-wiederholende Kunden:

1. **Prüfe verschobene Termine:**
   - Wenn `verschobenAufDatum > 0`: Verwende verschobenes Datum
   - Prüfe ob verschobenes Datum gelöscht wurde

2. **Prüfe Abholungs- und Auslieferungsdatum:**
   - Wenn `abDatum` genau auf Abholungstag liegt: Gib Abholungsdatum zurück
   - Wenn `abDatum` genau auf Auslieferungstag liegt: Gib Auslieferungsdatum zurück
   - Wenn `abDatum` zwischen beiden liegt: Gib das nächste fällige Datum zurück
   - Wenn `abDatum` vor beiden liegt: Gib Abholungsdatum zurück (nächstes fälliges)
   - Wenn `abDatum` nach beiden liegt: Gib 0 zurück (keine weiteren Termine)

3. **Berücksichtige gelöschte Termine:**
   - Wenn Termin gelöscht wurde, ignoriere ihn
   - Gib das nächste nicht-gelöschte Datum zurück

---

## ✅ Zusammenfassung

### Vorher:
- ❌ `customerFaelligAm()` berücksichtigte nur Abholungsdatum
- ❌ Auslieferungsdatum wurde ignoriert
- ❌ Termine wurden nicht für die Zukunft generiert (nur Abholungstag)

### Nachher:
- ✅ `customerFaelligAm()` berücksichtigt beide Daten (Abholung und Auslieferung)
- ✅ Termine werden für beide Tage angezeigt
- ✅ Gelöschte Termine werden korrekt berücksichtigt
- ✅ Verschobene Termine funktionieren korrekt

---

**Ende des Berichts**
