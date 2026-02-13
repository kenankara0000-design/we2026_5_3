# Analyse 09: Performance

**Status:** ✅ Erledigt (2026-02-13)  
**Priorität:** 🟢 Niedrig

---

## Ergebnisse pro Bereich

### 1. App-Start (Migrations)

| Aspekt | Status | Details |
|--------|--------|---------|
| Migrations blockieren UI? | ✅ Nein | `lifecycleScope.launch(Dispatchers.IO)` – läuft auf IO-Thread |
| Anzahl | 8 Migrationen | Laufen parallel im Scope |
| Wartezeit vor UI? | ✅ Nein | `setContent` wird sofort aufgerufen |
| Risiko | ⚠️ Gering | Bei erster Installation und großer DB könnten Migrationen zusammen IO belasten |

**Code:** `MainActivity.kt` Z.54–63

---

### 2. Tourenplaner (Daten laden)

| Aspekt | Status | Details |
|--------|--------|---------|
| Datenmenge | Alle Tour-Kunden + alle Listen | 2 Firebase-Flows: `customersForTour`, `alleListen` |
| Paging | ❌ Nein | Alle Kunden werden geladen |
| Filterung | ✅ | `TourDataProcessor.processTourData` filtert auf 3-Tage-Fenster + Überfällig |
| Firebase-Listener | 2 aktive | `customersForTourRef`, `listenRef` |
| LazyColumn | ✅ | Mit `key` (`c-${id}`, `h-${sectionType}-$index`) |
| Recomposition | ⚠️ | `displayItems` als `mutableStateOf` – Updates triggern Recomposition der gesamten Liste; kein `derivedStateOf` |
| `getStatusBadgeText` | ⚠️ | Pro Kunde in Activity aufgerufen – bei großer Liste häufig |

**Code:** `TourPlannerViewModel.kt` Z.52–60, `TourPlannerScreen.kt` Z.274–332

---

### 3. Kundenliste (500+ Kunden)

| Aspekt | Status | Details |
|--------|--------|---------|
| Datenmenge | Alle Kunden | `getAllCustomersFlow()` – kein Paging |
| Filter-Performance | ⚠️ | `combine` mit 7 Flow-Quellen, mehrere Filter + Suche auf gesamter Liste |
| LazyColumn | ✅ | `items(customers, key = { it.id })` |
| Recomposition | ✅ | `key = { it.id }` unterstützt stabile IDs |
| Suche | ✅ | 300ms Debounce |
| Risiko bei 500+ | ⚠️ | Filter + Suche auf der gesamten Liste könnte UI kurz verzögern |

**Code:** `CustomerManagerViewModel.kt` Z.54–115, `CustomerManagerScreen.kt` Z.149–168

---

### 4. Kundendetail (Laden)

| Aspekt | Status | Details |
|--------|--------|---------|
| Firebase-Listener | 3 aktive | Customer, 2× Erfassung (offen + erledigt) |
| Zusätzliche Berechnungen | 2 abgeleitete | `tourListenName`, `terminePairs365` |
| Recomposition | ✅ | `formState` via `remember(customer?.id, isInEditMode)` |
| Risiko | ✅ Gering | Einzelner Kunde, begrenzte Daten |

**Code:** `CustomerDetailViewModel.kt` Z.59–120

---

### 5. Erfassung (Artikel-Liste)

| Aspekt | Status | Details |
|--------|--------|---------|
| Artikel-Suche | ✅ | `searchResults.take(8)` – max. 8 Treffer in Column |
| Kunden-Suche | ✅ | Cache beim ersten Aufbau, dann lokal gefiltert |
| LazyColumn | ✅ | Kunden-Suche, Belegliste, Alle Belege |
| Beleg-Detail | ⚠️ | `forEach` in Column statt LazyColumn |
| Risiko | ✅ Gering | Begrenzte Artikelanzahl |

---

### 6. Foto-Upload (Hintergrund)

| Aspekt | Status | Details |
|--------|--------|---------|
| WorkManager | ✅ | `ImageUploadWorker` mit Retry-Policy |
| Blockiert UI? | ✅ Nein | Hintergrund-Thread |
| Risiko | ✅ Gering | |

---

### 7. SevDesk-Import (Netzwerk)

| Aspekt | Status | Details |
|--------|--------|---------|
| Progress-Anzeige | ✅ | `LinearProgressIndicator` |
| Blockiert UI? | ✅ Nein | Async |
| Timeout | ⚠️ | Nicht explizit geprüft |

---

### 8. Compose Recompositions

| Screen | Problem | Details |
|--------|---------|---------|
| TourPlannerScreen | ⚠️ | `displayItems` als `mutableStateOf` → gesamte Liste recomposed |
| TourPlannerScreen | ⚠️ | Kein `derivedStateOf` für abgeleitete Werte |
| MainScreen | ✅ | Flow-basiert mit `observeAsState` |
| CustomerManagerScreen | ✅ | `key = { it.id }` |
| CustomerDetailScreen | ✅ | `remember(customer?.id, isInEditMode)` |

---

### 9. Firebase-Listener (offene)

| Screen | Listener |
|--------|----------|
| TourPlannerScreen | 2 (customersForTour, alleListen) |
| CustomerDetailScreen | 3 (customer, erfassungenOffen, erfassungenErledigt) |
| CustomerManagerScreen | 1 (allCustomers) |
| MainScreen | 1+ (allCustomers + Slot-Berechnung) |
| WaschenErfassungScreen | 1+ (je nach State) |
| KundenListenScreen | 1 (alleListen) |

**Risiko:** Bei mehreren gleichzeitig offenen Activities (z. B. TourPlanner → CustomerDetail) könnten 5+ Listener aktiv sein. Firebase Realtime DB handhabt das in der Regel gut, aber bei schlechter Verbindung könnte Sync langsamer werden.

**Listener-Lifecycle:** Flows werden beim Verlassen der Activity beendet (viewModelScope / lifecycleScope).

---

### 10. Speicherverbrauch

| Aspekt | Status | Details |
|--------|--------|---------|
| Alle Kunden im Speicher | ⚠️ | Bei 500+ Kunden potentiell relevant |
| Foto-Thumbnails | ⚠️ | Coil/Image-Loading – RAM-Nutzung abhängig von Caching |
| Firebase Persistence Größe | ✅ | Firebase verwaltet Disk-Cache automatisch |

---

## forEach statt LazyColumn (Problembereiche)

| Screen | Methode | Max. Einträge | Risiko |
|--------|---------|---------------|--------|
| ListeBearbeitenScreen | `forEach` in Column | Viele Kunden möglich | ⚠️ Mittel |
| UrlaubScreen | `forEachIndexed` | Meist wenige | ✅ Gering |
| BelegDetail | `forEach` in Column | Viele Erfassungen möglich | ⚠️ Mittel |
| AlleTermineBlock | `forEach` mit max. 6 | Begrenzt | ✅ Gering |

---

## Zusammenfassung

### Kein sofortiger Handlungsbedarf

Die App läuft für den aktuellen Scope (< 500 Kunden) performant. Firebase Persistence, Flow-basiertes Loading und LazyColumn werden größtenteils korrekt eingesetzt.

### Beobachten / Langfristig

| Thema | Priorität | Details |
|-------|-----------|---------|
| Paging bei 500+ Kunden | 🟡 | CustomerManager + TourPlanner |
| `derivedStateOf` im TourPlanner | 🟡 | `displayItems` Recomposition optimieren |
| `forEach` → LazyColumn | 🟢 | ListeBearbeiten, BelegDetail |
| Filter-Performance bei 500+ | 🟡 | CustomerManager: 7 Flows + Suche |
| Loading-States korrekt setzen | 🔴 | TourPlanner + CustomerManager: `_isLoading` nie `true` |

---

*Keine Umsetzung ohne ausdrückliche Freigabe.*
