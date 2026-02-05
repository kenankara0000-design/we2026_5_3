# Refactoring-Analyse und Plan – we2026_5

**Stand:** Februar 2026  
**Basis:** Analyse der App (nicht des bestehenden REFACTORING_PLAN.md).

---

## Ausgangslage (Ist-Zustand)

- App vollständig auf Compose; alle Screens nutzen Jetpack Compose.
- Tour-Planer-Logik bereits teilweise aufgeteilt: `TourDataProcessor`, `WochentagslistenProcessor`, `TourListenProcessor`, `TourPlannerCallbackHandler`, `TourPlannerCoordinator`.
- Mehrere Dateien haben **über 350 Zeilen**; die größten liegen bei **~860** und **~790** Zeilen.
- **Wochentag-/A-L-Anzeige** und **Kundenart-Labels** sind in mehreren Screens **dupliziert**.
- **berechneNaechstenWochentag** existiert doppelt (`TerminAusKundeUtils`, `TerminRegelManager`).
- Paket-/Dateinamen nutzen teils noch „Liste“ (listebearbeiten, ListeErstellen, KundenListen); fachlich ist „Tour“ etabliert.

---

## Phase 1: Dateigrößen reduzieren (Priorität: hoch)

Ziel: Große Dateien aufteilen, ohne Design/Logik zu ändern. Orientierung: ab ~300–400 Zeilen Aufteilung prüfen; ab ~500+ konsequent aufteilen.

| # | Datei | Zeilen | Maßnahme |
|---|-------|--------|----------|
| 1.1 | **TourPlannerScreen.kt** | ~862 | Sub-Composables in eigene Dateien: z. B. `TourPlannerSectionHeader.kt`, `TourPlannerListeHeader.kt`, `TourPlannerCustomerRow.kt`, `TourPlannerErledigtRow.kt`. Data Classes (`ErledigungSheetArgs`, `CustomerOverviewPayload`) ggf. in `TourPlannerModels.kt` oder in bestehende tourplanner-Modelle. |
| 1.2 | **CustomerDetailScreen.kt** | ~790 | Sub-Composables auslagern: z. B. `CustomerDetailWochentagChipRow.kt` (oder in gemeinsame Wochentag-UI), `CustomerDetailStatusSection.kt`, `CustomerDetailRegelNameRow.kt`, `CustomerDetailIntervallRow.kt`. Gemeinsame UI-Konstanten (SectionSpacing etc.) in eine gemeinsame Theme/Constants-Datei oder in die neue Wochentag-UI. |
| 1.3 | **CustomerManagerScreen.kt** | ~455 | Kleinere Blöcke auslagern: z. B. Kundenkarten-Liste als eigenes Composable in `CustomerManagerCard.kt` oder in `ui/customermanager/`; `formatALWochentag` in gemeinsamen Wochentag-Helper (siehe Phase 2). |
| 1.4 | **TourPlannerCallbackHandler.kt** | ~435 | Nach Verantwortung aufteilen: z. B. `TourPlannerErledigungHandler` (Abholung, Auslieferung, KW, Rückgängig) und `TourPlannerVerschiebenUrlaubHandler` (Verschieben, Urlaub), oder pro Aktion kleine Use-Case-Klassen; gemeinsame Hilfsfunktionen (z. B. `serializeVerschobeneTermine`, `executeErledigung`) in eine gemeinsame Datei. |
| 1.5 | **ListeBearbeitenScreen.kt** | ~390 | Sub-Composables auslagern: `ListeBearbeitenAlWochentagText`, `IntervallRow`, `KundeInListeItem` in eigene Dateien unter `ui/listebearbeiten/` oder gemeinsame UI (Wochentag siehe Phase 2). |
| 1.6 | **TourDataProcessor.kt** | ~380 | Bereits entlastet durch WochentagslistenProcessor/TourListenProcessor. Optional: lange `processTourData`-Blöcke (filteredGewerblich, Items bauen) in benannte private Methoden oder kleine Helper-Klassen in gleichem Paket. |
| 1.7 | **AddCustomerScreen.kt** | ~372 | Große UI-Blöcke (z. B. Typ-Auswahl, Adresse, Tour/Wochentag) als eigene Composables in `ui/addcustomer/` (z. B. `AddCustomerTourSection.kt`) auslagern. |
| 1.8 | **KundenListenScreen.kt** | ~352 | Falls viele private Composables: auslagern in `ui/kundenlisten/` (z. B. Listen-Card, Filter-Chip-Bereich). |

**Reihenfolge empfohlen:** 1.1 → 1.2 (größte Gewinne), dann 1.4 (CallbackHandler stark gebündelt), danach 1.3, 1.5, 1.7, 1.8; 1.6 nur bei Bedarf.

---

## Phase 2: Code-Duplikation beseitigen (Priorität: hoch)

| # | Duplikat | Vorkommen | Maßnahme |
|---|----------|-----------|----------|
| 2.1 | **Wochentag-Anzeige (A/L)** | TourPlannerScreen (`AlWochentagText`), CustomerDetailScreen (`WochentagChipRow`), ListeBearbeitenScreen (`AlWochentagText`), CustomerManagerScreen (`formatALWochentag`), TerminRegelErstellenScreen (`WochentagListenMenue`), AddCustomerScreen (Chips) | Gemeinsame UI: z. B. `ui/common/WochentagUi.kt` mit Composables: `AlWochentagText(customer, color)`, `WochentagChipRow(selected, weekdays, onSelect, …)`, `formatALWochentag(customer, getString)`. String-Ressourcen für Mo–So zentral nutzen. |
| 2.2 | **Kundenart-Label (G/P/T)** | TourPlannerScreen `getKundenArtLabel(customer)` | In `CustomerTypeButtonHelper.kt` oder neue `ui/common/KundenArtLabel.kt` / `domain/KundenArtLabel.kt` als wiederverwendbare Funktion; in TourPlannerScreen nur noch aufrufen. |
| 2.3 | **berechneNaechstenWochentag** | `TerminAusKundeUtils` (private), `TerminRegelManager` (private) | Eine gemeinsame Implementierung: z. B. in `TerminBerechnungUtils` oder neue `util/WochentagBerechnung.kt`; beide Stellen nutzen diese. Signatur: `fun naechsterWochentagAb(startDatum: Long, wochentag: Int): Long`. |

**Reihenfolge:** 2.3 zuerst (reine Util, wenig UI), dann 2.1 (größter Duplikationsabbau), dann 2.2.

---

## Phase 3: Paket- und Namensstruktur (Priorität: niedrig)

- **Liste vs. Tour:** Pakete/Dateien wie `listebearbeiten`, `ListeBearbeitenScreen`, `ListeErstellenScreen`, `KundenListen*` sind fachlich „Tour“. Umbenennung (z. B. `TourBearbeitenScreen`, `TourErstellenScreen`, `TourListenScreen`) ist optional und aufwändig (Git-History, Verweise). Empfehlung: nur bei neuer Funktionalität konsequent „Tour“ benennen; große Umbenennung als separates Vorhaben.
- **Doppelte Struktur:** `liste/ListeBearbeitenCallbacks.kt` vs. `ui/listebearbeiten/` – inhaltlich OK; bei Aufräumen könnte `ListeBearbeitenCallbacks` näher an `ui/listebearbeiten/` oder an `tourplanner/` rücken, wenn es nur Tour-Erledigungslogik betrifft.

---

## Phase 4: Sonstiges (Priorität: niedrig)

| # | Thema | Maßnahme |
|---|--------|----------|
| 4.1 | **Deprecated weiter abbauen** | `Customer.getFaelligAm()`, `listenWochentag` etc. überall durch empfohlene APIs ersetzen und Deprecated entfernen. |
| 4.2 | **TerminRegelManager.kt (~309 Z.)** | Nach Phase 2.3 (berechneNaechstenWochentag auslagern) prüfen, ob weitere Teile (z. B. Fälligkeitslogik) in kleine Utils/Use-Cases ausgelagert werden können. |
| 4.3 | **Activities** | Dünne Wrapper (TourPlannerActivity, etc.) sind in Ordnung; kein Zwang zum Aufteilen. |

---

## Abhängigkeiten und Reihenfolge

```
Phase 2.3 (berechneNaechstenWochentag) → unabhängig, zuerst möglich
Phase 2.1 (Wochentag-UI) → reduziert Duplikate in Phase 1.2, 1.3, 1.5, 1.7
Phase 1.1, 1.2 → können parallel zu Phase 2 geplant werden; bei 1.2 Nutzung von 2.1 einplanen
Phase 1.4 → unabhängig von 2.x
Phase 3, 4 → jederzeit parallel oder nach 1/2
```

**Empfohlene Gesamtreihenfolge:**

1. **Phase 2.3** – `berechneNaechstenWochentag` zentralisieren  
2. **Phase 2.1** – Wochentag-UI in `ui/common/WochentagUi.kt` (oder ähnlich)  
3. **Phase 1.1** – TourPlannerScreen aufteilen  
4. **Phase 1.2** – CustomerDetailScreen aufteilen (mit Nutzung von 2.1)  
5. **Phase 1.4** – TourPlannerCallbackHandler aufteilen  
6. **Phase 2.2** – getKundenArtLabel zentral  
7. **Phase 1.3, 1.5, 1.7, 1.8** – weitere große Screens nach Bedarf  
8. **Phase 1.6, 3, 4** – optional

---

## Kurzüberblick: Dateien > 300 Zeilen (Stand Analyse)

| Zeilen | Datei |
|--------|--------|
| 862 | TourPlannerScreen.kt |
| 790 | CustomerDetailScreen.kt |
| 455 | CustomerManagerScreen.kt |
| 435 | TourPlannerCallbackHandler.kt |
| 390 | ListeBearbeitenScreen.kt |
| 380 | TourDataProcessor.kt |
| 372 | AddCustomerScreen.kt |
| 352 | KundenListenScreen.kt |
| 338 | TerminRegelManagerScreen.kt |
| 309 | TerminRegelManager.kt |
| 301 | CustomerDetailActivity.kt |
| 300 | TourDataFilter.kt |

Alle weiteren Dateien liegen unter 300 Zeilen.
