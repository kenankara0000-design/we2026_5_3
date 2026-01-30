# Refactoring-Analyse – we2026_5 (Tour-Planer)
**Datum:** 30. Januar 2026  
**Ziel:** Weitere Refactoring-Möglichkeiten nach bereits durchgeführten Aufräumarbeiten  
**Status (30.01.2026):** Die unten beschriebenen Refactorings 1–3, 5 und 6 wurden umgesetzt.

---

## 1. Ausgangslage

Das Projekt wurde am 26.01.2026 bereits stark refaktoriert:
- **CustomerAdapter** → ViewHolder-Binder, ButtonVisibility, CompletionHints, DialogHelper, ItemHelper, Callbacks
- **TourPlannerActivity** → UISetup, GestureHandler, **TourPlannerCallbackHandler**
- **TourPlannerViewModel** → **TourDataProcessor**, TourDataCategorizer, TourDataFilter
- **ListeBearbeitenActivity** → ListeBearbeitenUIManager, ListeBearbeitenCallbacks
- **CustomerDetailActivity** → CustomerPhotoManager, CustomerEditManager, CustomerDetailCallbacks, CustomerDetailUISetup

Trotzdem gibt es noch **große Dateien**, **Duplikate** und **verbesserungswürdige Muster**.

---

## 2. Aktuelle große Dateien (nach Zeilen)

| Datei | Zeilen | Bewertung |
|-------|--------|-----------|
| **TourPlannerCallbackHandler.kt** | 527 | Noch zu groß, viel Duplikat-Logik |
| **TerminRegelErstellenActivity.kt** | 504 | Monolithische Activity |
| **TourDataProcessor.kt** | 478 | Komplex, aber bereits ausgelagert |
| **TourPlannerActivity.kt** | 429 | Akzeptabel nach Refactoring |
| **CustomerAdapter.kt** | 389 | Akzeptabel |
| **CustomerViewHolderBinder.kt** | 386 | Akzeptabel |
| **CustomerButtonVisibilityHelper.kt** | 353 | Akzeptabel |
| **TourDataFilter.kt** | 300 | Akzeptabel |
| **TerminRegelManagerActivity.kt** | 394 | Mittel |

---

## 3. Refactoring-Vorschläge (Priorität)

### 3.1 TourPlannerCallbackHandler.kt (Priorität: Hoch)

**Problem:**
- 527 Zeilen, viele ähnliche Handler (`handleAbholung`, `handleAuslieferung`, `handleKw`).
- **runBlocking** in `getTermineFuerKunde` und in `handleRueckgaengig` (listeRepository) – blockiert den Main-Thread bzw. ist in Coroutine-Umgebung ungeeignet.
- Viele **hardcodierte Strings** (Toast, Fehlermeldungen).
- Gemeinsame Muster: „heute prüfen → Updates bauen → Firebase mit Retry → Toast → clearButtons + reload“.

**Vorschlag:**
1. **Gemeinsame Erledigungs-Logik extrahieren**
   - Eine interne Funktion z. B. `executeErledigung(typ: TerminTyp, customer, updatesBuilder, successMessage)` oder pro Typ kleine Klassen/Strategy, die nur die spezifischen Felder füllen; der Ablauf (Prüfung heute → Updates → Retry → Toast → Reload) ist dann einmal implementiert.
2. **runBlocking ersetzen**
   - `getTermineFuerKunde`: Repository-Aufruf als `suspend` (z. B. `getListeById` suspend machen oder in ViewModel/Scope mit `async`/`withContext` laden) und dem Adapter per suspend-Lambda oder vorab geladene Daten übergeben.
   - In `handleRueckgaengig`: `runBlocking { listeRepository.getListeById(...) }` durch Aufruf in bestehendem `CoroutineScope(Dispatchers.Main).launch` und darin `withContext(Dispatchers.IO) { listeRepository.getListeById(...) }` (falls keine suspend-API) oder direkten suspend-Aufruf.
3. **Strings auslagern**
   - Alle Toast- und Fehlermeldungen in `res/values/strings.xml` (oder passende Namensräume) auslagern und im Handler nur noch Ressourcen-IDs verwenden.

**Geschätzter Aufwand:** Mittel (2–3 Stunden)

---

### 3.2 TerminRegelErstellenActivity.kt (Priorität: Hoch)

**Problem:**
- 504 Zeilen, alles in einer Activity: UI-Setup, Klick-Logik, DatePicker, Wochentag-Picker, Validierung, Laden, Speichern, Löschen.
- Viele **Toast**-Meldungen und **hardcodierte Strings**.
- Kein ViewModel – Logik liegt direkt in der Activity.

**Vorschlag:**
1. **ViewModel einführen**
   - `TerminRegelErstellenViewModel`: State für Regel (Name, Beschreibung, Wiederholen, Intervall, wochentagBasiert, startDatum, abholung/auslieferung Wochentag/Datum), `loadRegel(id)`, `saveRegel()`, `deleteRegel()`, Validierung. Activity bindet nur UI und ruft ViewModel auf.
2. **UI-Helper für Picker**
   - Z. B. `TerminRegelDatePickerHelper` oder Erweiterung von `DateFormatter`/util: „Startdatum“, „Abholung“, „Auslieferung“ mit gleichem DatePicker-Setup und Format `"%02d.%02d.%04d"` zentral.
   - Wochentag-Picker-Dialog (AlertDialog mit `wochentage`) einmal kapseln, von Activity und ggf. anderen Stellen nutzbar.
3. **Strings**
   - Alle festen Texte (Titel, Buttons, Toasts, Validierungsmeldungen) in `strings.xml`.

**Geschätzter Aufwand:** Mittel (2–3 Stunden)

---

### 3.3 getStartOfDay / Datumslogik vereinheitlichen (Priorität: Mittel)

**Problem:**
- **TerminBerechnungUtils** (zentral für Termin-Berechnung).
- **TourPlannerDateUtils** und **TourDataCategorizer** haben jeweils eigene `getStartOfDay`.
- **TerminRegelManager** hat private `getStartOfDay` (Duplikat-Implementierung).
- **CustomerAdapter** hat private `getStartOfDay` (Duplikat).

**Vorschlag:**
1. **Eine Quelle für „Tagesanfang“**
   - Entweder nur `TerminBerechnungUtils.getStartOfDay` überall verwenden (und ggf. als Erweiterung/Konvention dokumentieren) oder eine kleine `DateUtils`/`CalendarUtils` nur für `getStartOfDay(ts: Long)` die von allen genutzt wird.
2. **TerminRegelManager** und **CustomerAdapter**
   - Statt eigener Implementierung: `TerminBerechnungUtils.getStartOfDay(ts)` aufrufen (oder die gewählte zentrale Util). So bleibt Zeitzone/Logik an einer Stelle.

**Geschätzter Aufwand:** Niedrig (ca. 1 Stunde)

---

### 3.4 Toast- und Fehlermeldungen zentralisieren (Priorität: Mittel)

**Problem:**
- Viele Stellen zeigen Toast mit hardcodierten deutschen Sätzen (TourPlannerCallbackHandler, TerminRegelErstellenActivity, TerminRegelManagerActivity, ListeBearbeitenCallbacks, CustomerDetailCallbacks, …).
- `strings.xml` enthält nur wenige Einträge; der Großteil der UI-Texte liegt im Code.

**Vorschlag:**
1. **strings.xml ausbauen**
   - Alle nutzer-sichtbaren Texte (Buttons, Toasts, Dialoge, Fehlermeldungen, Validierungen) in `res/values/strings.xml` (oder thematisch in mehreren string-Dateien) auslagern.
2. **Optional: ToastHelper**
   - Eine kleine Util oder Extension `Context.showToast(@StringRes resId)` / `showToast(message: String)` für einheitliche Länge (z. B. LENGTH_SHORT für Erfolg, LENGTH_LONG für Fehler), um Duplikate wie `Toast.makeText(..., LENGTH_SHORT).show()` zu reduzieren.

**Geschätzter Aufwand:** Niedrig–Mittel (1–2 Stunden, je nach Umfang)

---

### 3.5 TourDataProcessor.kt weiter aufteilen (Priorität: Niedrig)

**Problem:**
- 478 Zeilen, viele verschachtelte Bedingungen und Listen-/Einzelkunden-Logik.
- Schon gut in Processor/Categorizer/Filter getrennt, aber `processTourData` ist weiterhin lang und schwer im Kopf zu behalten.

**Vorschlag:**
1. **Lesbarkeit durch benannte Hilfsfunktionen**
   - Z. B. `listenMitKundenBerechnen(...)`, `kundenOhneListeMitTerminen(...)`, `erledigteKundenFiltern(...)` – die öffentliche API bleibt `processTourData`, intern werden klare Schritte aufgerufen.
2. **Optional**
   - Sehr lange Blöcke (z. B. „erledigte Kunden in Listen“) in eine eigene private Funktion oder eine kleine innere Klasse/Klasse im gleichen Modul auslagern.

**Geschätzter Aufwand:** Niedrig (1–2 Stunden)

---

### 3.6 runBlocking in der Codebase reduzieren (Priorität: Mittel)

**Problem:**
- **TourPlannerCallbackHandler**: `runBlocking { ... listeRepository.getListeById ... }` und `runBlocking { TerminBerechnungUtils.berechneAlleTermineFuerKunde(...) }`.
- **TourPlannerDateUtils**: mehrfach `runBlocking` für Repository-Aufrufe.
- **CustomerEditManager**: `runBlocking` verwendet.

**Vorschlag:**
1. **Repository-Layer**
   - Wo möglich: suspend-Funktionen für Einzelabfragen anbieten (z. B. `suspend fun getListeById(id: String): KundenListe?`), neben bestehenden Flow-APIs.
2. **Aufrufer**
   - Statt `runBlocking` in UI/Callback-Handlern: gleichen Code in `launch`/`viewModelScope` mit `withContext(Dispatchers.IO) { ... }` oder direkten suspend-Aufruf ausführen; Callback-Handler bekommt ggf. einen `CoroutineScope` oder nutzt einen von der Activity/ViewModel bereitgestellten.
3. **TerminBerechnungUtils**
   - Falls `berechneAlleTermineFuerKunde` nur synchrone Aufrufe braucht: Aufrufer soll die suspend-Wrapper nutzen und die Berechnung auf dem IO-Dispatcher ausführen, ohne `runBlocking`.

**Geschätzter Aufwand:** Mittel (2–3 Stunden, abhängig von Repository-API)

---

## 4. Bereits erledigte / offene Punkte aus früheren Analysen

- **ANALYSE_ABHOLUNG_AUSLIEFERUNG_HEUTE.md:**  
  Die dort empfohlene Anpassung (größeres Suchfenster in `istTerminHeuteFaellig`) ist ein **Bugfix/Robustheit**, kein Refactoring – kann unabhängig umgesetzt werden.

- **Ungenutzte Imports/Drawables** (APP_ANALYSE_2026_01_26_V2):  
  Falls noch nicht bereinigt: ungenutzte Imports und Drawables entfernen (kleiner Aufwand).

- **DateFormatter:**  
  Bereits vorhanden; vereinzelt wird noch direkt `SimpleDateFormat` verwendet (z. B. CustomerExportHelper, TourPlannerActivity) – optional in DateFormatter auslagern.

---

## 5. Empfohlene Reihenfolge

1. **getStartOfDay vereinheitlichen** (schnell, geringes Risiko, weniger Duplikat).
2. **Strings auslagern** (Toast + Fehlermeldungen) – parallel oder direkt vor den großen Refactorings.
3. **TourPlannerCallbackHandler** (runBlocking entfernen, gemeinsame Erledigungs-Logik, dann Strings).
4. **TerminRegelErstellenActivity** (ViewModel + Picker-Helper + Strings).
5. **runBlocking** in TourPlannerDateUtils und CustomerEditManager ersetzen (evtl. mit Punkt 3 abstimmen).
6. **TourDataProcessor** lesbarer machen (benannte Hilfsfunktionen).
7. Optional: **ToastHelper** und restliche **SimpleDateFormat**-Nutzung in DateFormatter konsolidieren.

---

## 6. Kurzfassung

| Thema | Priorität | Aufwand | Nutzen |
|-------|-----------|---------|--------|
| TourPlannerCallbackHandler vereinfachen & runBlocking entfernen | Hoch | Mittel | Wartbarkeit, keine Blockierung |
| TerminRegelErstellenActivity mit ViewModel & Helper | Hoch | Mittel | Testbarkeit, klare Trennung |
| getStartOfDay zentral nutzen | Mittel | Niedrig | Einheitlichkeit, weniger Fehler |
| Strings (Toast/Fehler) in strings.xml | Mittel | Niedrig–Mittel | i18n, Konsistenz |
| runBlocking in Repo/Utils ersetzen | Mittel | Mittel | Saubere Coroutine-Nutzung |
| TourDataProcessor lesbarer machen | Niedrig | Niedrig | Wartbarkeit |

Mit diesen Schritten bleibt das Verhalten der App erhalten, die Struktur wird klarer und zukünftige Erweiterungen (z. B. weitere Sprachen, Tests, neue Termin-Typen) fallen leichter.
