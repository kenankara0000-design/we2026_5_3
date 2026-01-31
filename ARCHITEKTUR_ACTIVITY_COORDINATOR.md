# Architektur: Schwere Activities – Coordinator & ViewModel

**Stand:** Januar 2026  
**Ziel:** Activity nur noch Binding, ViewModel beobachten, Coordinator/Intents; keine Geschäftslogik, keine vielen Helper-Instanzen direkt in der Activity.

---

## 1. Regeln für die Activity

### Activity darf
- **Binding setzen:** `binding = XBinding.inflate(layoutInflater); setContentView(binding.root)`
- **Klicks an ViewModel/Coordinator weiterreichen:** z. B. `binding.btnX.setOnClickListener { viewModel.doX() }` oder `coordinator.onX()`
- **Observables beobachten:** `viewModel.state.observe(this) { ... }` bzw. `lifecycleScope.launch { viewModel.flow.collect { ... } }`
- **Intents/Fragments/Dialoge anstoßen:** `startActivity(...)`, `supportFragmentManager`, Launcher registrieren

### Activity darf nicht
- **Repository direkt aufrufen** (außer ggf. für reine DI-Weitergabe an Coordinator)
- **Termin-/Erledigungslogik berechnen** (gehört ins ViewModel oder Use-Case)
- **Viele if/else für Geschäftslogik** enthalten
- **Viele Helper-Instanzen selbst verwalten** – ein Coordinator pro Screen kapselt sie

---

## 2. Umgesetzt: CustomerDetail

### 2.1 Verantwortung

| Schicht | Verantwortung |
|--------|----------------|
| **CustomerDetailViewModel** | State (currentCustomer, deleted, isLoading, errorMessage); Geschäftslogik: setCustomerId, saveCustomer(updates), deleteCustomer(); Datenquelle: Repository getCustomerFlow(customerId). |
| **CustomerDetailCoordinator** | Kapselt UISetup, Callbacks, EditManager, PhotoManager; setupUi(); updateCustomer(customer) bei ViewModel-Update; Launcher-Results: onTakePictureResult, onPickImageResult, onCameraPermissionResult. |
| **CustomerDetailActivity** | Binding; Launcher registrieren (Result an coordinatorRef weiterreichen); viewModel.setCustomerId(id); coordinator.setupUi(); ViewModel beobachten (currentCustomer → coordinator.updateCustomer, deleted → setResult + finish, errorMessage → Toast); onDestroy: coordinatorRef = null. |

### 2.2 Abhängigkeiten

- **ViewModel** braucht: CustomerRepository (getCustomerFlow, updateCustomer, deleteCustomer).
- **Coordinator** braucht: Activity, Binding, ViewModel, CustomerRepository, TerminRegelRepository, customerId, Launcher (3x), onProgressVisibilityChanged.
- **Activity** braucht: ViewModel (Koin), Repository/RegelRepository (nur für Coordinator), Coordinator.

### 2.3 Schnittstellen (optional für Tests)

- **Save-Updates:** CustomerEditManager erhält `onSaveUpdates: (Map<String, Any>, (Boolean) -> Unit) -> Unit` (von ViewModel.saveCustomer angeboten).
- **Delete:** CustomerDetailCallbacks erhält `onDeleteRequested: () -> Unit` (von ViewModel.deleteCustomer angeboten).
- **Kunde aktuell:** Callbacks stellen `currentCustomerForEdit` bereit; Coordinator nutzt es für EditManager.

---

## 3. Geplant: TourPlanner

### 3.1 Ziel

- **TourPlannerCoordinator** kapselt: TourPlannerUISetup, TourPlannerCallbackHandler, TourPlannerDateUtils, TourPlannerDialogHelper, TourPlannerGestureHandler, CustomerDialogHelper (Sheet); Adapter-Erstellung und -Callbacks.
- **TourPlannerActivity** nur noch: Binding, ViewModel beobachten (selectedTimestamp, tourItems, isLoading, error), Coordinator aufrufen (setupUi(), onDateChanged(ts), onTourItems(items, dateMillis), reloadCurrentView(), showErledigungSheet); Implementierung von ErledigungSheetCallbacks mit Delegation an Coordinator.
- **Datum:** Bereits im ViewModel (selectedTimestamp); viewDate nur noch im Coordinator für Helper, die ein Calendar brauchen.

### 3.2 Nächste Schritte

1. TourPlannerCoordinator anlegen: Konstruktor(activity, binding, viewModel, repository, listeRepository, regelRepository).
2. Helper im Coordinator erstellen und in setupUi() verdrahten; Adapter im Coordinator halten, RecyclerView in setupUi() setzen.
3. API des Coordinators: setupUi(), onDateChanged(ts: Long?), onTourItems(items, dateMillis), reloadCurrentView(), showCustomerOverviewDialog(customer), showErledigungSheet(customer, state), handleAbholung(customer), handleAuslieferung(customer), handleKw(customer), handleRueckgaengig(customer), getViewDateMillis(): Long.
4. Activity: ErledigungSheetCallbacks implementieren und an Coordinator delegieren; alle Klicks und Observer auf ViewModel/Coordinator umstellen; viewDate und alle Helper-Referenzen entfernen.

---

## 4. Dokumentation pro Activity (Übersicht)

### CustomerDetail (umgesetzt)
- **ViewModel:** CustomerDetailViewModel – State + load/save/delete.
- **Coordinator:** CustomerDetailCoordinator – UISetup, Callbacks, EditManager, PhotoManager; setupUi(), updateCustomer(), onTakePictureResult, onPickImageResult, onCameraPermissionResult.
- **Activity:** CustomerDetailActivity – Binding, Launcher, ViewModel-Observer, Coordinator; kein Listener, kein loadCustomer() in Activity.

### TourPlanner (geplant)
- **ViewModel:** TourPlannerViewModel – selectedTimestamp, tourItems, toggleSection, prevDay, nextDay, goToToday, loadTourData; bereits vorhanden.
- **Coordinator:** TourPlannerCoordinator (noch zu erstellen) – alle Helper, setupUi(), onDateChanged, onTourItems, reloadCurrentView, Sheet/Dialog-Weiterleitung.
- **Activity:** TourPlannerActivity – schlank: Binding, Observer, Coordinator-API, ErledigungSheetCallbacks-Delegation.

---

*Dieses Dokument dient als Referenz für die schrittweise Entlastung schwerer Activities und für zukünftige Refactorings.*
