# Fehlerbericht: "Nächste Tour: 1.1.1970"

## Problembeschreibung

In der Anwendung wird für Kunden (z.B. "Kenan") das Datum **"Nächste Tour: 1.1.1970"** angezeigt. Dieses Datum entspricht dem Unix-Epoch (1. Januar 1970, 00:00:00 UTC) und tritt auf, wenn ein `Long`-Wert `0` ist.

## Fehlerursache

**Datei:** `CustomerAdapter.kt` (Zeilen 259-277)

**Problem:** Die Berechnung des "Nächste Tour"-Datums im `CustomerAdapter` verwendet **nicht** die neue Intervall-Struktur und berücksichtigt **nicht** die `getFaelligAm()` Funktion aus `Customer.kt`.

### Aktueller Code (FEHLERHAFT):

```kotlin
val naechsteTour = if (customer.verschobenAufDatum > 0) {
    customer.verschobenAufDatum
} else if (customer.listeId.isNotEmpty()) {
    if (customer.abholungDatum > 0) {
        customer.abholungDatum
    } else {
        System.currentTimeMillis()
    }
} else {
    // Für Kunden ohne Liste: Normale Berechnung
    if (customer.wiederholen && customer.letzterTermin > 0) {
        customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
    } else {
        customer.abholungDatum  // ← PROBLEM: Kann 0 sein!
    }
}
```

### Warum führt das zu "1.1.1970"?

1. **Neue Kunden-Struktur:** Kunden, die mit der neuen `intervalle`-Struktur erstellt wurden, haben die alten Felder (`abholungDatum`, `letzterTermin`, etc.) auf `0` gesetzt.

2. **Fehlende Berücksichtigung:** Der `CustomerAdapter` prüft nicht, ob `customer.intervalle.isNotEmpty()` ist und verwendet stattdessen die alten, leeren Felder.

3. **Ergebnis:** Wenn `customer.abholungDatum == 0` und `customer.letzterTermin == 0`, dann wird `naechsteTour = 0`, was zu "1.1.1970" formatiert wird.

## Lösung

Der `CustomerAdapter` sollte die bereits vorhandene `getFaelligAm()` Funktion aus `Customer.kt` verwenden, die:
- ✅ Die neue `intervalle`-Struktur berücksichtigt
- ✅ Die alte Struktur als Fallback verwendet
- ✅ `TerminBerechnungUtils` für korrekte Berechnungen nutzt
- ✅ Gelöschte und verschobene Termine berücksichtigt

### Korrigierter Code:

```kotlin
// Nächstes Tour-Datum berechnen und anzeigen
val naechsteTour = customer.getFaelligAm()
```

## Betroffene Dateien

1. **`CustomerAdapter.kt`** (Zeile 259-277)
   - Muss angepasst werden, um `customer.getFaelligAm()` zu verwenden

## Zusätzliche Überlegungen

- Die `getFaelligAm()` Funktion kann auch `0L` zurückgeben, wenn kein Termin gefunden wird
- In diesem Fall sollte ein Fallback-Wert angezeigt werden (z.B. "Kein Termin" oder das aktuelle Datum)
- Die Formatierung sollte auch prüfen, ob `naechsteTour > 0` ist, bevor sie formatiert wird

## Empfohlene Änderung

```kotlin
// Nächstes Tour-Datum berechnen und anzeigen
val naechsteTour = customer.getFaelligAm()

if (naechsteTour > 0) {
    val cal = Calendar.getInstance()
    cal.timeInMillis = naechsteTour
    val dateStr = "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}.${cal.get(Calendar.YEAR)}"
    holder.binding.tvNextTour.text = "Nächste Tour: $dateStr"
    holder.binding.tvNextTour.visibility = View.VISIBLE
} else {
    holder.binding.tvNextTour.text = "Nächste Tour: Kein Termin"
    holder.binding.tvNextTour.visibility = View.VISIBLE
}
```

## Priorität

**HOCH** - Dies ist ein sichtbarer Fehler, der die Benutzererfahrung erheblich beeinträchtigt.
