# Analyse 07: Offline-Verhalten

**Status:** ✅ Erledigt (2026-02-13)  
**Priorität:** 🟡 Mittel

---

## Grundlage

- **Firebase Realtime DB Persistence:** Lokal gespeichert, Schreibvorgänge werden gepuffert
- **NetworkMonitor:** `isOnline` (LiveData), `isSyncing` – überwacht Netzwerkstatus
- **WorkManager:** Foto-Uploads (ImageUploadWorker, StorageUploadManager) – laufen bei Verbindung

---

## Ergebnisse pro Bereich

### 1. Tourenplaner (Laden, Erledigen)

| Aspekt | Status | Details |
|--------|--------|---------|
| Offline laden | ✅ | Firebase Persistence liefert gecachte Daten |
| Offline erledigen (A/L/KW) | ✅ | Schreibvorgänge über Firebase werden gepuffert |
| Hinweis in UI | ✅ | `TourPlannerTopBar`: Gelber Offline-Banner mit Icon + „Offline" |
| Sync-Status | ⚠️ | Kein Hinweis auf ausstehende/gepufferte Änderungen |
| Hardcodierte Farbe | ⚠️ | `Color(0xFFFFEB3B)` statt `colorResource` |

**Code:** `TourPlannerTopBar.kt` Z.147–168, `TourPlannerActivity.kt` Z.75–76 (NetworkMonitor)

---

### 2. Kundenliste (Laden, Suchen)

| Aspekt | Status | Details |
|--------|--------|---------|
| Offline laden | ✅ | Firebase Persistence |
| Offline suchen/filtern | ✅ | Lokale Filterung |
| Hinweis in UI | ✅ | Offline-Badge in TopAppBar |

**Code:** `CustomerManagerActivity.kt` Z.74, `CustomerManagerTopBar.kt`

---

### 3. Kundendetail (Lesen, Speichern)

| Aspekt | Status | Details |
|--------|--------|---------|
| Offline lesen | ✅ | Firebase Persistence |
| Offline speichern | ✅ | `awaitWithTimeout` → `true` bei Timeout (lokal gepuffert) |
| Hinweis in UI | ❌ | **Kein Offline-Hinweis im Kundendetail** |
| Sync-Status | ❌ | Kein Hinweis, ob Änderungen synchronisiert sind |

**Code:** `CustomerRepository.kt` Z.221–234, `FirebaseRetryHelper`

---

### 4. Neuer Kunde anlegen

| Aspekt | Status | Details |
|--------|--------|---------|
| Offline anlegen | ✅ | Firebase Persistence puffert |
| Hinweis in UI | ❌ | Kein Offline-Hinweis |
| Feedback | ⚠️ | Toast bei Fehler, aber kein Offline-spezifischer Hinweis |

---

### 5. Erfassung (Neue Erfassung)

| Aspekt | Status | Details |
|--------|--------|---------|
| Offline erfassen | ✅ | Firebase Persistence puffert |
| Hinweis in UI | ❌ | Kein Offline-Hinweis |

---

### 6. Foto-Upload

| Aspekt | Status | Details |
|--------|--------|---------|
| Offline fotografieren | ✅ | Foto lokal gespeichert |
| Upload bei Verbindung | ✅ | WorkManager (ImageUploadWorker) |
| Hinweis in UI | ⚠️ | Upload-Fortschritt im Kundendetail (LinearProgressIndicator), aber kein Offline-Hinweis |
| Pending-Uploads sichtbar? | ❌ | Kein Hinweis auf ausstehende Uploads |

---

### 7. SevDesk-Import

| Aspekt | Status | Details |
|--------|--------|---------|
| Offline möglich? | ❌ | Benötigt Netzwerk (API-Calls zu my.sevdesk.de) |
| Hinweis in UI | ❌ | **Kein Offline-Hinweis** – Import schlägt still fehl oder zeigt generischen Fehler |
| Empfehlung | Button deaktivieren oder Hinweis zeigen wenn offline |

---

### 8. Listen erstellen/bearbeiten

| Aspekt | Status | Details |
|--------|--------|---------|
| Offline möglich | ✅ | Firebase Persistence |
| Hinweis in UI | ❌ | Kein Offline-Hinweis |

---

### 9. Preise bearbeiten

| Aspekt | Status | Details |
|--------|--------|---------|
| Offline möglich | ✅ | Firebase Persistence |
| Hinweis in UI | ❌ | Kein Offline-Hinweis |

---

### 10. NetworkMonitor / Sync-Anzeige

| Aspekt | Status | Details |
|--------|--------|---------|
| NetworkMonitor existiert | ✅ | `NetworkMonitor.kt` mit `isOnline` (LiveData) |
| `isSyncing` existiert | ✅ | Aber nur in wenigen Screens genutzt |
| Genutzt in | ⚠️ | Nur TourPlanner, CustomerManager, MainScreen |
| **Nicht genutzt in** | ❌ | Kundendetail, Erfassung, Listen, Preise, Urlaub, SevDesk, Statistiken, etc. |

---

## Zusammenfassung

### Offline-Hinweis vorhanden

| Screen | Offline-Badge | Sync-Badge |
|--------|--------------|------------|
| MainScreen | ✅ | ✅ |
| TourPlannerScreen | ✅ | ❌ |
| CustomerManagerScreen | ✅ | ❌ |

### Offline-Hinweis fehlt

| Screen | Benötigt Netzwerk? | Risiko |
|--------|-------------------|--------|
| CustomerDetailScreen | Nein (Firebase) | Nutzer weiß nicht ob gespeichert/gesynced |
| WaschenErfassungScreen | Nein (Firebase) | Nutzer unsicher ob Erfassung angekommen |
| KundenListenScreen | Nein (Firebase) | Gering |
| ListeBearbeitenScreen | Nein (Firebase) | Gering |
| PreiseScreens | Nein (Firebase) | Gering |
| UrlaubScreen | Nein (Firebase) | Gering |
| SevDeskImportScreen | **Ja** | **Hoch** – Import schlägt fehl ohne Hinweis |
| StatisticsScreen | Nein (Firebase) | Gering |
| AddCustomerScreen | Nein (Firebase) | Nutzer unsicher ob Kunde angelegt |

### Kernprobleme

1. **Nur 3 von 24 Screens zeigen Offline-Status** – alle anderen nutzen NetworkMonitor nicht
2. **Kein Sync-Status**: Nutzer sieht nie, ob ausstehende Änderungen synchronisiert wurden
3. **SevDesk-Import**: Einziger Screen der zwingend Netzwerk braucht – kein Offline-Hinweis
4. **Ausstehende Foto-Uploads**: Nicht sichtbar (WorkManager läuft im Hintergrund)

---

*Keine Umsetzung ohne ausdrückliche Freigabe.*
