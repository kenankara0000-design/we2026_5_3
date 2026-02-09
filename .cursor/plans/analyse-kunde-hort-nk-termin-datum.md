# Analyse: Kunde „Hort Nk.“ – Termin-Datum ändert sich nicht nach Speichern

**Nur Analyse, keine Code-Änderungen.**

---

## Beobachtung

Beim Kunden „Hort Nk.“ wird das Termin-Datum in der App geändert und gespeichert, aber die Anzeige (bzw. die berechneten Termine) bleibt beim alten Datum.

---

## Mögliche Ursachen (priorisiert)

### 1. Firebase liefert Long-Felder als Double – Intervall-Daten gehen verloren

**Wo:** `CustomerSnapshotParser.parseIntervalleWithErstelltAm()`  
**Problem:** Die Realtime Database liefert große Zahlen (z. B. Timestamps) oft als **Double**. Beim direkten Deserialisieren mit `entry.getValue(CustomerIntervall::class.java)` können `abholungDatum` und `auslieferungDatum` (Typ `Long`) falsch gemappt werden (z. B. 0 oder abgeschnitten).  
**Aktuell:** Nur `erstelltAm` wird explizit mit `optionalLong(entry, "erstelltAm")` sicher gelesen. `abholungDatum` und `auslieferungDatum` kommen ausschließlich aus der automatischen Deserialisierung.  
**Folge:** Nach dem Speichern kommt vom Listener ein Kunde mit Interval len, deren A/L-Daten 0 oder falsch sind → Termin-Berechnung nutzt dann andere Quellen oder liefert „kein Termin“ / altes Verhalten.

---

### 2. Intervall-`erstelltAm` wird beim Speichern ignoriert (Startdatum)

**Wo:** `TerminAusKundeUtils.erstelleIntervallAusKunde()`  
**Problem:** Beim Erzeugen des „Haupt-Intervalls“ aus dem Formular wird `erstelltAm` des Intervalls **immer** auf `System.currentTimeMillis()` gesetzt (Zeile 65), nicht auf das vom Nutzer gewählte **Startdatum** (`startDatum`).  
**Folge:** Das Startdatum des Kunden (`customer.erstelltAm`) wird korrekt gespeichert; die ersten Termine (A/L) werden aus `startDatum` berechnet und sind korrekt. Aber das Intervall-Feld `erstelltAm` ist „jetzt“. Wo die Termin-Logik `intervall.erstelltAm` nutzt (z. B. Mindest-Start für Termine, MONTHLY_WEEKDAY), kann das zu abweichendem oder „altem“ Verhalten führen.

---

### 3. Welches „Datum“ wird geändert?

- **Startdatum (Erstellungsdatum)** im Tab „Termine & Tour“ → fließt in `erstelltAm` und in die Berechnung von `mainFromForm` (erstes Intervall).  
- **Konkretes A- oder L-Datum** eines Intervalls → wird über den Datumspicker (`onDatumSelected` → `IntervallManager.showDatumPickerForCustomer`) in `editIntervalle` geändert und beim Speichern in `intervalleToSave` übernommen.

Wenn nur das **Startdatum** geändert wird: Es wird in `updates` geschrieben und in `startDatumA` für `erstelleIntervallAusKunde` genutzt. Das neu berechnete Intervall hat dann neue `abholungDatum`/`auslieferungDatum`, wird aber nur gespeichert, wenn  
`stateForSave.kundenTyp == KundenTyp.REGELMAESSIG && stateForSave.abholungWochentage.isNotEmpty()`.  
Sonst wird `editIntervalle` unverändert gespeichert – dann würde eine reine Startdatum-Änderung die Intervalle nicht anfassen.

---

### 4. Aktualisierung der UI nach Speichern

**Ablauf:** Nach `updateCustomer`/`updateChildren` in Firebase löst die Realtime DB den `ValueEventListener` aus → `getCustomerFlow(id)` emittiert erneut → `currentCustomer` im ViewModel wird aktualisiert → die Detail-UI zeigt den neuen Kunden.  
**Fazit:** Die UI sollte sich nach erfolgreichem Speichern von selbst aktualisieren, **sofern** die aus Firebase gelesenen Daten (insbesondere `intervalle` mit korrekten Long-Werten) stimmen. Wenn die Anzeige sich nicht ändert, passt sehr wahrscheinlich das **gelesene** Datenmodell nicht (siehe Punkt 1).

---

### 5. Tour-Planner / „customers_for_tour“

Nach dem Update wird bei tourenrelevanten Änderungen `syncTourCustomer(it)` aufgerufen; die Tour-Daten werden also aus dem aktualisierten Kunden neu abgeleitet. Wenn der Kunde aus Firebase aber falsche Intervalle hat (Punkt 1), ist auch der Tour-Index falsch.

---

## Empfohlene Prüfungen (ohne Code hier umzusetzen)

1. **Firebase-Konsolen-Daten für „Hort Nk.“:**  
   Unter `customers/<id>/intervalle/0/` prüfen, ob nach dem Speichern `abholungDatum`, `auslieferungDatum` und `erstelltAm` als Zahlen mit den erwarteten Werten stehen.

2. **Parsing absichern:**  
   In `parseIntervalleWithErstelltAm` für jedes Intervall auch `abholungDatum` und `auslieferungDatum` per `optionalLong(entry, "abholungDatum")` bzw. `optionalLong(entry, "auslieferungDatum")` lesen und das geparste Intervall damit überschreiben (analog zu `erstelltAm`), damit Double-Werte von Firebase zuverlässig als Long ankommen.

3. **Startdatum im Intervall:**  
   In `TerminAusKundeUtils.erstelleIntervallAusKunde` das Intervall-`erstelltAm` aus dem übergebenen `startDatum` (Tagesanfang) setzen statt aus `System.currentTimeMillis()`, damit gespeichertes Startdatum und Intervall-Start konsistent sind.

4. **Kunden-Typ und Wochentage:**  
   Für „Hort Nk.“ prüfen: Ist `kundenTyp == REGELMAESSIG` und sind Abhol-Wochentage gesetzt? Nur dann wird `mainFromForm` neu aus dem Formular gebaut und mit den neuen Datumsangaben gespeichert; andernfalls wird nur `editIntervalle` geschrieben.

---

## Kurzfassung

Am wahrscheinlichsten ist, dass **beim Lesen aus Firebase die Long-Felder der Intervalle** (`abholungDatum`, `auslieferungDatum`) durch Double-Deserialisierung verloren gehen oder falsch ankommen. Zweiter Punkt: Das **Intervall-`erstelltAm`** wird beim Erzeugen aus dem Formular nicht aus dem Nutzer-Startdatum gesetzt. Dritter Punkt: Sicherstellen, dass bei der geänderten Datumsangabe tatsächlich der Pfad „regelmäßig + Wochentage“ genutzt wird und damit `mainFromForm` mit den neuen Daten gespeichert wird.
