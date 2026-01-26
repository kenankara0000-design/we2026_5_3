# Analyse: Intervall und A/L Button-Aktivierung

## 🔍 Gefundene Probleme

### Problem 1: Button-Aktivierung prüft nicht das angezeigte Datum ❌

**Aktuelle Logik in `CustomerAdapter.kt` (Zeilen 385-431):**

```kotlin
// A-Button: Prüft nur ob Datum vorhanden ist, NICHT ob es heute fällig ist
val hatAbholungDatum = if (customer.listeId.isNotEmpty()) {
    true  // ❌ Immer true für Listen-Kunden
} else {
    customer.abholungDatum > 0 || (customer.wiederholen && customer.letzterTermin > 0)
}
val aButtonAktiv = hatAbholungDatum && !customer.abholungErfolgt
```

**Problem:**
- ❌ Prüft nur, ob ein Abholungsdatum vorhanden ist
- ❌ Prüft NICHT, ob es am angezeigten Tag fällig ist
- ❌ Callbacks `getAbholungDatum` und `getAuslieferungDatum` werden definiert, aber NICHT verwendet!

---

### Problem 2: A und L an verschiedenen Tagen werden nicht korrekt behandelt ❌

**Szenario:**
- Abholung (A) am Montag
- Auslieferung (L) am Dienstag

**Aktuelles Verhalten:**
- ❌ Beide Buttons werden an beiden Tagen angezeigt
- ❌ A-Button sollte nur am Montag sichtbar sein
- ❌ L-Button sollte nur am Dienstag sichtbar sein

**Erwartetes Verhalten:**
- ✅ A-Button nur am Montag anzeigen
- ✅ L-Button nur am Dienstag anzeigen
- ✅ Nur der Button, der am angezeigten Tag fällig ist, sollte aktiv sein

---

### Problem 3: Intervalle mit verschiedenen Daten werden nicht korrekt behandelt ❌

**Szenario:**
- Intervall hat `abholungDatum` am Montag
- Intervall hat `auslieferungDatum` am Dienstag
- Wiederholendes Intervall (z.B. wöchentlich)

**Aktuelles Verhalten:**
- ❌ Beide Buttons werden immer angezeigt, wenn Kunde in Liste ist
- ❌ Prüft nicht, ob am angezeigten Tag ein Abholungs- oder Auslieferungstermin fällig ist

**Erwartetes Verhalten:**
- ✅ A-Button nur anzeigen, wenn am angezeigten Tag ein Abholungstermin fällig ist
- ✅ L-Button nur anzeigen, wenn am angezeigten Tag ein Auslieferungstermin fällig ist
- ✅ Intervalle korrekt berücksichtigen (wiederholende Intervalle)

---

## 🔧 Lösung

### Schritt 1: Callbacks verwenden für Datum-Prüfung

Die Callbacks `getAbholungDatum` und `getAuslieferungDatum` werden bereits definiert und in `TourPlannerActivity` gesetzt, aber NICHT verwendet!

**Aktuell:**
```kotlin
// Callbacks werden definiert, aber nicht verwendet
var getAbholungDatum: ((Customer) -> Long)? = null
var getAuslieferungDatum: ((Customer) -> Long)? = null
```

**Sollte sein:**
```kotlin
// Prüfe ob am angezeigten Tag ein Abholungstermin fällig ist
val abholungDatumHeute = getAbholungDatum?.invoke(customer) ?: 0L
val hatAbholungHeute = abholungDatumHeute > 0

// Prüfe ob am angezeigten Tag ein Auslieferungstermin fällig ist
val auslieferungDatumHeute = getAuslieferungDatum?.invoke(customer) ?: 0L
val hatAuslieferungHeute = auslieferungDatumHeute > 0
```

---

### Schritt 2: Button-Sichtbarkeit basierend auf fälligem Datum

**Aktuell:**
```kotlin
// Button wird angezeigt wenn Datum vorhanden ist (egal welcher Tag)
holder.binding.btnAbholung.visibility = if (hatAbholungDatum && !isDone) View.VISIBLE else View.GONE
```

**Sollte sein:**
```kotlin
// Button wird nur angezeigt wenn am angezeigten Tag ein Abholungstermin fällig ist
holder.binding.btnAbholung.visibility = if (hatAbholungHeute && !isDone) View.VISIBLE else View.GONE
```

---

### Schritt 3: Intervalle korrekt berücksichtigen

Die Funktionen `calculateAbholungDatum` und `calculateAuslieferungDatum` in `TourPlannerActivity` berechnen bereits korrekt:
- ✅ Prüfen ob am angezeigten Tag ein Abholungs-/Auslieferungstermin fällig ist
- ✅ Berücksichtigen wiederholende Intervalle
- ✅ Berücksichtigen verschiedene Daten für A und L

**Problem:** Diese Funktionen werden nicht verwendet!

---

## 📋 Zusammenfassung

### Was funktioniert:
- ✅ Datum-Berechnung für Intervalle ist korrekt implementiert
- ✅ Callbacks werden definiert und gesetzt
- ✅ Funktionen `calculateAbholungDatum` und `calculateAuslieferungDatum` sind korrekt

### Was funktioniert NICHT:
- ❌ Button-Aktivierung verwendet die Callbacks nicht
- ❌ Buttons werden angezeigt, auch wenn kein Termin am angezeigten Tag fällig ist
- ❌ A und L an verschiedenen Tagen werden nicht korrekt behandelt
- ❌ Intervalle werden nicht für Button-Aktivierung berücksichtigt

---

## 🎯 Lösung

Die Callbacks `getAbholungDatum` und `getAuslieferungDatum` müssen verwendet werden, um zu prüfen, ob am angezeigten Tag ein Termin fällig ist. Nur dann sollten die Buttons angezeigt werden.

**Ende des Berichts**
