# Analyse 08: Fehlerbehandlung (UX)

**Status:** ✅ Erledigt (2026-02-13)  
**Priorität:** 🟡 Mittel

---

## Ergebnisse pro Bereich

### 1. Tourenplaner (Laden fehlschlägt)

| Aspekt | Status | Details |
|--------|--------|---------|
| Fehlermeldung klar? | ✅ | `TourPlannerErrorView` mit Text + „Erneut versuchen" |
| Stille Fehler? | ⚠️ | `getSheetState == null` → „Aktionen" tut nichts, kein Feedback |
| Retry? | ✅ | `onRetry` → `coordinator.reloadCurrentView()` |
| Loading-State? | ❌ | `_isLoading` existiert aber wird **nie auf `true` gesetzt** – LoadingView wird nie angezeigt |
| Error-State? | ✅ | `viewModel.setError(message)` durch Coordinator |

**Code:** `TourPlannerStateViews.kt` Z.32–63, `TourPlannerViewModel.kt` Z.134–135

---

### 2. Kunde speichern (fehlschlägt)

| Aspekt | Status | Details |
|--------|--------|---------|
| Fehlermeldung klar? | ⚠️ | Toast mit `error_message_generic` – generisch |
| Stille Fehler? | ⚠️ | `saveCustomer` mit `_customerId.value ?: return` – kein Feedback wenn ID fehlt |
| Retry? | ❌ | Kein Retry-Button; Nutzer muss nochmal speichern |
| Loading-State? | ✅ | `CustomerDetailLoadingView` bei `isLoading` |
| Validierung? | ⚠️ | Nur Name als Pflichtfeld; kein Format-Check (Tel, PLZ) |

**Code:** `CustomerDetailViewModel.kt` Z.166–206, `CustomerDetailActivity.kt` Z.316–321

---

### 3. Erfassung speichern

| Aspekt | Status | Details |
|--------|--------|---------|
| Fehlermeldung klar? | ✅ | Inline-Fehlermeldung (roter Text) über Speichern-Button |
| Stille Fehler? | ❌ | Kein bekannter stiller Fehler |
| Retry? | ❌ | Kein Retry; Nutzer kann erneut Speichern drücken |
| Loading-State? | ✅ | `isSaving` → Button-Text „…", Button disabled |
| Validierung? | ✅ | Min. 1 Position mit Menge > 0 |

**Code:** `WaschenErfassungViewModel.kt` Z.326–331, `WaschenErfassungErfassenContent.kt` Z.100–112

---

### 4. Foto-Upload fehlschlägt

| Aspekt | Status | Details |
|--------|--------|---------|
| Fehlermeldung klar? | ⚠️ | WorkManager-Status nicht direkt in UI sichtbar |
| Stille Fehler? | ⚠️ | Upload-Fehler sind für den Nutzer nicht offensichtlich |
| Retry? | ✅ | WorkManager hat Retry-Policy |
| Loading-State? | ✅ | `LinearProgressIndicator` im Stammdaten-Tab während Upload |

---

### 5. Firebase-Verbindungsfehler

| Aspekt | Status | Details |
|--------|--------|---------|
| Fehlermeldung klar? | ⚠️ | Generische Fehlermeldungen in den meisten Screens |
| Stille Fehler? | ⚠️ | Firebase Persistence puffert – Fehler werden „verschluckt" |
| Retry? | ✅ | `FirebaseRetryHelper` mit max. 3 Versuche |
| Sync-Feedback? | ❌ | Kein Hinweis ob Daten synchronisiert sind |

---

### 6. SevDesk-Import fehlschlägt

| Aspekt | Status | Details |
|--------|--------|---------|
| Fehlermeldung klar? | ✅ | `error` als roter Text im Screen |
| Stille Fehler? | ❌ | Fehler werden angezeigt |
| Retry? | ⚠️ | Nutzer kann Button erneut drücken, kein expliziter Retry |
| Loading-State? | ✅ | `LinearProgressIndicator` + Button-Text „…" bei `isBusy` |

---

### 7. Ungültige Eingaben (Formular)

| Screen | Validierung | Details |
|--------|-------------|---------|
| AddCustomer | ✅ Name | `validation_name_missing`; Duplikat-Check mit Dialog |
| CustomerDetail | ✅ Name | `validationNameMissing` |
| ListeErstellen | ✅ Name, Wochentag | `validation_list_name_missing`, `validation_list_wochentag` |
| WaschenErfassung | ✅ Positionen | Min. 1 Position mit Menge > 0 |
| Kundenpreise | ✅ Preis | Netto/Brutto-Validierung im Dialog |
| **Telefon-Format** | ❌ | Keine Validierung |
| **PLZ-Format** | ❌ | Keine Validierung |
| **E-Mail-Format** | ❌ | Keine Validierung |

---

### 8. Kunde löschen

| Aspekt | Status | Details |
|--------|--------|---------|
| Bestätigungs-Dialog? | ✅ | AlertDialog vor Löschung |
| Fehlermeldung? | ✅ | Toast bei Fehler |
| Stille Fehler? | ❌ | |

---

### 9. Liste löschen

| Aspekt | Status | Details |
|--------|--------|---------|
| Bestätigungs-Dialog? | Nicht gefunden | Prüfung nötig |
| Fehlermeldung? | ⚠️ | Toast |

---

### 10. Preis speichern

| Aspekt | Status | Details |
|--------|--------|---------|
| Fehlermeldung klar? | ✅ | Validierung im Dialog (roter Text) |
| Stille Fehler? | ❌ | |
| Loading-State? | ✅ | `isSaving` im Dialog |

---

## Querschnittsbefunde

### Loading-States

| Screen | Loading vorhanden? | Funktioniert? |
|--------|-------------------|---------------|
| TourPlannerScreen | Definiert | ❌ **`_isLoading` wird nie `true`** |
| CustomerDetailScreen | ✅ | ✅ |
| CustomerManagerScreen | Definiert | ❌ **`_isLoading` wird nie `true`; `loadCustomers()` ist leer** |
| MainScreen | ❌ | Kein Loading-State definiert |
| WaschenErfassungScreen | ✅ | ✅ |
| KundenListenScreen | ✅ | ✅ (Text „Laden...") |
| StatisticsScreen | ✅ | ✅ |
| LoginScreen | ✅ | ✅ |
| MapViewScreen | ✅ | ✅ |
| TerminAnlegenUnregelmaessig | ✅ | ✅ |
| SevDeskImportScreen | ✅ | ✅ |
| ListeBearbeitenScreen | ✅ | ✅ |
| **UrlaubScreen** | ⚠️ | Nur bei `customer == null` |
| **KundenpreiseScreen** | ❌ | Kein Loading-Indikator |
| **AusnahmeTerminActivity** | ❌ | Kein Loading bei Kunden-Laden |
| **ArtikelVerwaltungScreen** | ❌ | Kein Loading |
| **BelegeActivity** | ❌ | Kein Loading |

### Error-States

| Screen | Error UI | Art |
|--------|----------|-----|
| TourPlannerScreen | ✅ ErrorView + Retry | Eigene View |
| CustomerDetailScreen | ⚠️ Toast | Generisch |
| CustomerManagerScreen | ⚠️ Snackbar | Keine Retry-View |
| KundenListenScreen | ✅ Emoji + Retry | Eigene View |
| StatisticsScreen | ✅ Roter Text | Inline |
| SevDeskImportScreen | ✅ Roter Text | Inline |
| LoginScreen | ✅ Text + Retry | Eigene View |
| MapViewScreen | ✅ Text | Eigene View |
| TerminAnlegenUnregelmaessig | ✅ Roter Text | Inline |
| **AddCustomerScreen** | Toast | Generisch |
| **WaschenErfassungScreen** | Inline roter Text | OK |
| **UrlaubScreen** | Toast | Generisch |
| **KundenpreiseScreen** | ❌ | Kein Error-State |
| **AusnahmeTerminActivity** | Toast | Generisch |
| **ArtikelVerwaltungScreen** | ❌ | Kein Error-State |
| **BelegeActivity** | ❌ | Kein Error-State |

### Stille Fehler (zusammengefasst)

| Stelle | Problem | Auswirkung |
|--------|---------|------------|
| TourPlanner: `getSheetState == null` | „Aktionen" reagiert nicht | Nutzer tippt, nichts passiert |
| CustomerDetail: `_customerId.value ?: return` | Speichern bricht ab | Kein Feedback |
| Firebase Persistence | Fehler werden gepuffert | Nutzer denkt alles ist gespeichert |
| Foto-Upload-Fehler | WorkManager im Hintergrund | Nutzer sieht nicht ob Upload geklappt hat |

---

## Empfehlungen (ohne Umsetzung)

1. **TourPlanner / CustomerManager:** `_isLoading` korrekt setzen (auf `true` während initialem Laden)
2. **Konsistente Error-Anzeige:** Statt Mix aus Toast, Snackbar, Inline, eigene View → einheitliche `AppErrorView` Composable
3. **Stille Fehler eliminieren:** Feedback bei `getSheetState == null`, `_customerId ?: return`
4. **Sync-Feedback:** Nach erfolgreichem Sync kurze Bestätigung
5. **Loading für alle Screens:** Einheitliche `AppLoadingView`

---

*Keine Umsetzung ohne ausdrückliche Freigabe.*
