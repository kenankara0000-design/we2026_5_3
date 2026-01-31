# Compose-Migration – Protokoll (Anker-Datei)

**Regel:** Alles, was wir **neu bauen**, wird in **Jetpack Compose** umgesetzt.  
**Bestehende Screens:** werden schrittweise migriert; hier wird jeder Schritt als erledigt markiert.

---

## Strategie: Ohne Risiko anfangen

1. **Compose erst einbinden** – keine Screen-Umstellung, nur Build vorbereiten.
2. **Ersten Screen wählen, der klein und isoliert ist** – wenig Abhängigkeiten, ViewModel vorhanden.
3. **Ein Screen nach dem anderen** – nie mehrere schwere Screens gleichzeitig umstellen.
4. **Neue Features von vornherein in Compose** – keine neuen XML-Layouts mehr für neue UI.

---

## Phase 0: Compose einbinden

| Schritt | Beschreibung | Status |
|--------|--------------|--------|
| 0.1 | In `app/build.gradle.kts`: Compose-BOM und Compose-Dependencies hinzufügen (compose-bom, ui, material3, activity-compose, lifecycle-viewmodel-compose). | [x] |
| 0.2 | In `android { buildFeatures { compose = true } }` setzen. Ab Kotlin 2.0: Compose Compiler als Gradle-Plugin (`org.jetbrains.kotlin.plugin.compose`), kein `composeOptions` mehr. | [x] |
| 0.3 | Projekt bauen und starten – bestehende App läuft unverändert (kein Screen umgestellt). Lokal: `./gradlew assembleDebug` bzw. Run in Android Studio. | [x] |

---

## Phase 1: Erster Screen (minimales Risiko)

**Empfehlung:** **StatisticsActivity** – klein, ein Layout, ein ViewModel (`StatisticsViewModel`), klare UI (Karten mit Zahlen). Kein RecyclerView, keine komplexen Adapter.

| Schritt | Beschreibung | Status |
|--------|--------------|--------|
| 1.1 | StatisticsActivity: Activity auf `setContent { ... }` umstellen; UI komplett in Compose (Statistik-Karten, Loading, Error). ViewModel weiter nutzen (state.observeAsState()). | [x] |
| 1.2 | Altes `activity_statistics.xml` und ViewBinding in StatisticsActivity entfernen. | [x] |
| 1.3 | StatisticsScreen in der App testen (Daten, Zurück, Fehlerfall). | [x] **Fix:** NoSuchMethodError in CircularProgressIndicator behoben (durch Text „Laden…“ ersetzt). Getestet – funktioniert. |

---

## Phase 2: Weitere einfache Screens

Reihenfolge nach Risiko (einfach → komplex).

| Screen | Begründung | Status |
|--------|------------|--------|
| LoginActivity | Sehr wenig UI (aktuell ggf. nur Splash/Redirect). Wenn sichtbare Login-UI kommt → in Compose. | [ ] |
| ListeErstellenActivity | Relativ überschaubar; Formular. | [x] |
| TerminRegelErstellenActivity | Formular-Screen. | [x] |
| MapViewActivity | Heute nur Trampolin zu Maps; wenn In-App-Karte kommt → Compose. | [x] |

---

## Phase 3: Mittlere Komplexität

| Screen | Begründung | Status |
|--------|------------|--------|
| TerminRegelManagerActivity | Liste + Aktionen. | [x] |
| KundenListenActivity | Listen-UI. | [x] |
| ListeBearbeitenActivity | Bearbeitung + Listen. | [x] |
| AddCustomerActivity | Formular mit mehr Feldern. | [x] |

---

## Phase 4: Schwere Screens (spät angehen)

Viele Helper, RecyclerViews, Fragments – erst wenn Phase 1–3 sicher laufen.

| Screen / Teil | Begründung | Status |
|---------------|------------|--------|
| MainActivity | Zentrale Navigation; kann zuletzt oder als Compose-Insel (nur Inhalt) migriert werden. | [x] |
| CustomerManagerActivity | Tabs, Suche, RecyclerView, Adapter. | [x] |
| CustomerDetailActivity | Detail, Fotos, Intervalle, viele Callbacks. | [x] |
| TourPlannerActivity | Tourenplaner, Bottom-Sheet, Gesten, Kartenliste. | [x] |
| ErledigungBottomSheetDialogFragment | Bottom-Sheet → kann als Compose-BottomSheet neu gebaut werden. | [x] |

---

## Wann etwas als „erledigt“ markieren

- **Phase 0:** Build läuft, Compose ist nutzbar; keine Regression in der App.
- **Phase 1–4:** Der betreffende Screen läuft **vollständig in Compose** (kein XML-Layout mehr für diesen Screen), getestet (Daten, Navigation, Fehler).

---

## Neue Features (ab sofort)

Jede **neue** UI (neuer Screen, neues Dialog, neues Bottom-Sheet, neuer Abschnitt in einer Liste) wird **in Compose** gebaut. Das steht auch in der Projekt-Regel (`.cursor/rules`). In dieser Datei werden keine Einzel-Features aufgelistet; die Regel gilt global.

---

*Letzte Aktualisierung: Kunden-Übersicht-Dialog im Tourenplaner auf Compose umgestellt (AlertDialog in TourPlannerScreen mit overviewCustomer/overviewRegelNamen). dialog_customer_overview.xml entfernt. Keine weiteren Screens zur Migration offen (LoginActivity ohne sichtbare UI).*
