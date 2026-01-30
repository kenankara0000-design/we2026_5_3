# Fehleranalyse – we2026_5
**Datum:** 30. Januar 2026

---

## Linter-Status
- **Keine** Linter-Fehler im Projekt.

---

## Gefundene Punkte

### 1. **runBlocking – ungünstig im UI-/Callback-Kontext** (Priorität: Mittel)

**TourPlannerDateUtils.kt**
- Zeilen 36, 122, 265: `runBlocking { listeRepository.getListeById(...) }`
- **Problem:** Blockiert den aufrufenden Thread (oft Main). Wird u.a. aus Adapter-Callbacks (`getAbholungDatum`, `getAuslieferungDatum`, `getNaechstesTourDatum`) aufgerufen → Risiko für ANR oder Ruckler.
- **Empfehlung:** Listen aus ViewModel (z.B. `getListen()`) synchron bereitstellen und hier nur noch aus dem Speicher lesen; oder Aufrufer auf Coroutines umstellen (suspend/withContext), sodass kein `runBlocking` nötig ist.

**TourPlannerCallbackHandler.kt**
- Zeile 82: `getTermineFuerKunde` nutzt `runBlocking { listeRepository.getListeById(...); berechneAlleTermineFuerKunde(...) }` – wird beim Binden der Liste auf dem Main-Thread ausgeführt.
- Zeile 445: `istTerminHeuteFaellig` nutzt `runBlocking { listeRepository.getListeById(customer.listeId) }` – wird aus `handleAbholung`/`handleAuslieferung`/`handleKw` (innerhalb `launch`) aufgerufen; blockiert dort den Coroutine-Dispatcher.
- **Empfehlung:** Listen per Lambda vom ViewModel holen (z.B. `getListen()`) oder innerhalb der bestehenden Coroutine `withContext(Dispatchers.IO) { listeRepository.getListeById(...) }` verwenden und `runBlocking` entfernen.

### 2. **Ungenutzter Import** (behoben)
- **CustomerEditManager.kt:** Import `kotlinx.coroutines.runBlocking` wurde nicht verwendet.
- **Änderung:** Import entfernt.

### 3. **Doppel-Null-Assertion (`!!`)** (Priorität: Niedrig)

- **CustomerViewHolderBinder.kt (79, 133):** `displayedDateMillis!!` steht jeweils in `if (displayedDateMillis != null) { ... }`. Wenn `displayedDateMillis` ein `val` ist, wäre Smart-Cast möglich und `!!` überflüssig; bei `var` ist `!!` eine gängige, aber harte Null-Assertion.
- **TourPlannerDateUtils.kt (42, 128):** `liste!!` nach `if (liste != null)` – gleiche Situation, Smart-Cast oft möglich.
- **TourPlannerDateUtils.kt (277):** `liste!!.geloeschteTermine` – `liste` ist hier aus einer vorherigen null-Prüfung bekannt; `!!` vermeidbar, z.B. durch `liste?.geloeschteTermine ?: emptyList()` oder lokale Variable nach null-Check.
- **TerminRegelErstellenActivity.kt (322):** `regelId!!` – Aufruf nur, wenn `regelId != null` (Branch „Regel bearbeiten“); semantisch in Ordnung, `!!` könnte durch `regelId?.let { ... }` ersetzt werden.
- **TourDataCategorizer.kt (39, 42, 93, 140):** `naechstesDatum!!` in Bedingungen der Form `(naechstesDatum == null || x < naechstesDatum!!)` – durch Short-Circuit wird `!!` nur bei nicht-null ausgeführt; sicher, aber z.B. `naechstesDatum?.let { x < it } == true` wäre ohne `!!` lesbar.

**Empfehlung:** Kein akuter Bug; bei Refactorings `!!` schrittweise durch Smart-Cast oder sichere Aufrufe (`.let`, `?.`) ersetzen.

### 4. **TODO (nur Information)**
- **TerminRegelErstellenActivity.kt (421):** „Wenn Listen auch Regel-IDs speichern, hier auch Listen prüfen“ – inhaltliche Erweiterung, kein Fehler.

---

## Kurzfassung

| Thema                         | Priorität | Status / Empfehlung                    |
|------------------------------|-----------|----------------------------------------|
| runBlocking in DateUtils/CallbackHandler | Mittel    | Ersetzen durch getListen() oder withContext(IO) |
| Ungenutzter Import (CustomerEditManager) | Niedrig   | Behoben (Import entfernt)              |
| Doppel-Null-Assertion (`!!`)  | Niedrig   | Dokumentiert; bei Gelegenheit entschärfen |

---

## Nächste Schritte (optional)

1. **runBlocking entfernen:** ViewModel um `getListen(): List<KundenListe>` ergänzen, an `TourPlannerDateUtils` und `TourPlannerCallbackHandler` übergeben; in beiden Klassen alle `runBlocking { listeRepository.getListeById(...) }` durch Nutzung von `getListen().find { it.id == customer.listeId }` bzw. in Handlern durch `withContext(Dispatchers.IO) { listeRepository.getListeById(...) }` ersetzen.
2. **Build & manueller Test:** Nach Änderungen „Rebuild Project“ und einmal Tourenplaner sowie Regel erstellen/bearbeiten durchspielen.
