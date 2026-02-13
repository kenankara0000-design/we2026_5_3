# Analyse 02: Code-Architektur

**Status:** ✅ Erledigt (2026-02-13)  
**Quelle:** Extrahiert aus `2026-02-13_21-02.plan.md` (Abschnitte 2, 4, 5, 6, 7, 8, 11, 12, 14)

---

## 1. Activities & Navigation

### 24 Activities – Inkonsistentes Setup

| Problem | Betroffene Activities | Empfehlung |
|---------|----------------------|------------|
| **Kein MaterialTheme-Wrapper** | `MainActivity`, `PreiseActivity`, `ErfassungMenuActivity`, `SettingsActivity`, `DataImportActivity`, `TourPlannerActivity`, `CustomerManagerActivity` (~38%) | Einheitlich `MaterialTheme { }` |
| **window.decorView.setBackgroundColor()** | `MainActivity:66`, `AddCustomerActivity:36` | Entfernen – über Compose-Theme lösen |
| **Gemischte Navigation** (AppNavigation vs. direkter Intent) | 62% nutzen direkte Intents | Alle auf `AppNavigation` umstellen |
| **Kein BackHandler** | ~22 von 24 Activities | Für komplexe State-Machines ergänzen |
| **Intent-Extra-Boilerplate** | 5+ Activities | Extension `Intent.requireStringExtra(key)` |
| **@Suppress("UNUSED_EXPRESSION")** | `StatisticsActivity:37`, `ListeErstellenActivity:20` | Koin-Initialisierung korrekt lösen |

---

## 2. ViewModels & State Management

### 2.1 Gemischte Patterns (LiveData vs. StateFlow)

| ViewModel | Pattern | Empfehlung |
|-----------|---------|------------|
| `MainViewModel` | `LiveData` + `observeAsState` | → `StateFlow` + `collectAsState` |
| `TourPlannerViewModel` | `StateFlow` intern, `LiveData` exponiert | → `StateFlow` durchgängig |
| `AddCustomerViewModel` | Nur `LiveData` | → `StateFlow` |
| `CustomerManagerViewModel` | Gemischt | → `StateFlow` durchgängig |
| `MapViewModel` | `LiveData` | → `StateFlow` |
| `StatisticsViewModel` | `LiveData` | → `StateFlow` |
| `ListeErstellenViewModel` | `LiveData` | → `StateFlow` |
| `CustomerDetailViewModel` | `StateFlow` | ✅ OK |
| `WaschenErfassungViewModel` | `StateFlow` | ✅ OK |
| `BelegeViewModel` | `StateFlow` | ✅ OK |
| `KundenListenViewModel` | `StateFlow` | ✅ OK |
| `ListeBearbeitenViewModel` | `StateFlow` | ✅ OK |

**~7 ViewModels (29%)** nutzen noch LiveData.

### 2.2 Error-Handling – inkonsistent

| ViewModel | Error-Pattern |
|-----------|-------------|
| `CustomerDetailViewModel` | `_errorMessage: StateFlow<String?>` |
| `CustomerManagerViewModel` | `_error: LiveData<String?>` |
| `WaschenErfassungViewModel` | Error in `UiState` sealed class |
| `ListeBearbeitenViewModel` | `when (result)` mit eigenem Handling |

### 2.3 Stille Fehler

| Stelle | Problem |
|--------|---------|
| `CustomerDetailViewModel.saveCustomer()` | `_customerId.value ?: return` – kein Feedback |
| `ListeBearbeitenViewModel.loadDaten()` | `listeId ?: return` – kein Feedback |

---

## 3. Repositories & Datenzugriff

| Problem | Stellen | Empfehlung |
|---------|---------|------------|
| **Rückgabetyp-Mix:** `Boolean` vs. `Result<T>` | `CustomerRepository` | Alle auf `Result<T>` |
| **Retry-Logik inkonsistent** | Nur `CustomerRepository` nutzt `FirebaseRetryHelper` | Überall einsetzen |
| **Parsing inkonsistent** | `CustomerSnapshotParser` vs. inline Parsing | Eigene Parser-Klassen |
| **AppErrorMapper inkonsistent** | Nur in 1 Stelle genutzt | Überall verwenden |

---

## 4. Datenmodelle

- **Nullable-Felder:** Konsistent ✅
- **Default-Werte:** Sinnvoll ✅
- **Fehlende Validierung:** `Customer.tageAzuL` (0–365), `Customer.intervallTage` (1–365)

---

## 5. Business-Logik – Zu große Dateien

| Datei | Zeilen | Empfehlung |
|-------|--------|------------|
| `TourDataProcessor.kt` | 482 | Aufteilen: `TourErledigtDetector`, `TourOverdueDetector` |
| `TerminBerechnungUtils.kt` | 502 | Aufteilen: `TerminCalculationUtils`, `TerminDateUtils` |
| `ListeBearbeitenCallbacks.kt` | 364 | Optional aufteilen |
| `SevDeskApi.kt` | 466 | OK – API-Klassen sind groß |

---

## 6. Doppelter Code (Duplikate)

### Hohe Priorität

| Duplikat | Stellen | Lösung |
|----------|---------|--------|
| **Result-Handling** | `CustomerDetailVM:197`, `CustomerManagerVM:188`, `ListeBearbeitenVM:84` | Extension `handleResult()` |
| **Kunden-Suche** | `WaschenErfassungVM:180-200`, `BelegeVM:140-151` | `CustomerSearchHelper` |
| **Preis-Laden** | `WaschenErfassungVM:238-247`, `BelegeVM:211-220` | `PriceLoadingUtils` |
| **Erfassung-Flow** | `WaschenErfassungVM:207-216`, `BelegeVM:168-177` | `ErfassungFlowCollector` |

### Mittlere Priorität

| Duplikat | Stellen | Lösung |
|----------|---------|--------|
| **Loading-State** | 6+ ViewModels | BaseViewModel |
| **Überfällig-Erkennung** | 3+ Stellen | Konsolidieren in `TerminFilterUtils` |
| **Erledigt-Erkennung** | TourDataProcessor, TourListenProcessorImpl | `ErledigtDetector` |
| **getStartOfDay()** | 2+ Stellen | Einmal in `AppTimeZone` |

---

## 7. Architektur-Konflikte

| Konflikt | Beschreibung |
|----------|-------------|
| LiveData vs. StateFlow | 29% vs. 71% – gemischt |
| AppNavigation vs. Intents | 21% vs. 62% – nicht durchgesetzt |
| MaterialTheme: mit vs. ohne | 62% vs. 38% |
| DetailUiConstants | Nur 3 Screens nutzen es |

---

## 8. Toter/Alter Code

| Datei/Code | Problem | Empfehlung |
|-----------|---------|------------|
| 7 Migrations-Dateien | Laufen bei jedem Start | Prüfen → archivieren |
| `FirebaseSyncManager.kt` | Alle Methoden No-Ops | Entfernen oder `@Deprecated` |
| `purple_500` in colors.xml | Alte Primärfarbe | Prüfen und entfernen |
| `TabSwipeFrameLayout.kt` | Custom View in Compose-Welt | Prüfen, ggf. entfernen |
| View-basierte Dialoge | `TourPlannerDialogHelper` | Zu Compose migrieren |
| 5 Layout-XMLs | View-basiert | Referenzen prüfen |
| `AppLogger.kt:28` | `ENABLED = true` hardcoded | `BuildConfig.DEBUG` verwenden |

---

## 9. Strings

- `strings.xml` hat 866 Zeilen – gut ✅
- Hardcodiert: `CustomerTypeButtonHelper:19-24`, `DialogBaseHelper:21-22` → String-Ressourcen
- Potenziell ungenutzt: `tour_preis_*` Strings

---

*Keine Umsetzung ohne ausdrückliche Freigabe.*
