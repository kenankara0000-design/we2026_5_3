# Analyse-Plan Refactoring – Ausführung

**Erstellt:** Februar 2026  
**Basis:** REFACTORING_ANALYSE_PLAN.md

Konkrete Schritte in empfohlener Reihenfolge. Mit [ ] abhaken.

---

## Schritt 1: Phase 2.3 – berechneNaechstenWochentag zentralisieren

- [x] **1.1** Neue Datei `app/.../util/WochentagBerechnung.kt` (oder in `TerminBerechnungUtils.kt`) anlegen.
  - Öffentliche Funktion: `fun naechsterWochentagAb(startDatum: Long, wochentag: Int): Long`
  - Logik aus `TerminAusKundeUtils.kt` (Zeilen 18–36) übernehmen; `isValidWeekday()` ggf. mit aufnehmen.
- [x] **1.2** `TerminAusKundeUtils.kt`: private `berechneNaechstenWochentag` entfernen, Aufrufe auf zentrale Util umstellen.
- [x] **1.3** `TerminRegelManager.kt`: private `berechneNaechstenWochentag` (ab Zeile 310) entfernen, alle Aufrufe auf zentrale Util umstellen.
- [ ] **1.4** Build + kurzer Test (Termin-Erstellung, Regel-Logik).

**Betroffene Dateien:**  
`util/TerminAusKundeUtils.kt`, `util/TerminRegelManager.kt`, neu: `util/WochentagBerechnung.kt` oder Erweiterung `TerminBerechnungUtils.kt`

---

## Schritt 2: Phase 2.1 – Wochentag-UI gemeinsam

- [x] **2.1** Ordner/Datei anlegen: `ui/common/WochentagUi.kt` (ggf. Paket `ui.common` anlegen).
- [x] **2.2** Composable `AlWochentagText(customer, color)` aus `TourPlannerScreen.kt` (ab ~693) bzw. `ListeBearbeitenScreen.kt` (ab ~57) zusammenführen → eine gemeinsame Implementierung in `WochentagUi.kt`.
- [x] **2.3** Composable `WochentagChipRow(...)` aus `CustomerDetailScreen.kt` (ab ~77) nach `WochentagUi.kt` verschieben; Parameter vereinheitlichen.
- [x] **2.4** Funktion `formatALWochentag(customer, getString)` aus `CustomerManagerScreen.kt` (ab ~70) nach `WochentagUi.kt` (oder gemeinsamen Helper).
- [x] **2.5** `WochentagListenMenue` aus `TerminRegelErstellenScreen.kt` (ab ~305) prüfen: entweder in `WochentagUi.kt` aufnehmen oder klar dokumentieren, warum getrennt.
- [x] **2.6** Alle Screens umstellen:  
  `TourPlannerScreen.kt`, `CustomerDetailScreen.kt`, `ListeBearbeitenScreen.kt`, `CustomerManagerScreen.kt`, `TerminRegelErstellenScreen.kt`, `AddCustomerScreen.kt` (Chips) → Import aus `WochentagUi.kt`.
- [x] **2.7** String-Ressourcen Mo–So zentral: Doppelung `termin_regel_weekday_sample` entfernt; `getWochentagFullResIds()` ergänzt; hardcodierte Wochentage in ListeErstellenViewModel, KundenListenViewModel, TerminRegelInfoText, TerminRegelManagerActivity durch String-Ressourcen ersetzt.

**Betroffene Dateien:**  
Neu: `ui/common/WochentagUi.kt`;  
`ui/tourplanner/TourPlannerScreen.kt`, `ui/detail/CustomerDetailScreen.kt`, `ui/listebearbeiten/ListeBearbeitenScreen.kt`, `ui/customermanager/CustomerManagerScreen.kt`, `ui/terminregel/TerminRegelErstellenScreen.kt`, `ui/addcustomer/AddCustomerScreen.kt`

---

## Schritt 3: Phase 1.1 – TourPlannerScreen aufteilen

- [x] **3.1** Data Classes auslagern: `ErledigungSheetArgs`, `CustomerOverviewPayload` → z.B. `ui/tourplanner/TourPlannerModels.kt` oder in bestehende tourplanner-Modelle.
- [x] **3.2** Sub-Composables in eigene Dateien unter `ui/tourplanner/`:
  - [x] `TourPlannerSectionHeader.kt`
  - [x] `TourPlannerListeHeader.kt`
  - [x] `TourPlannerCustomerRow.kt`
  - [x] `TourPlannerErledigtRow.kt`
- [x] **3.3** `TourPlannerScreen.kt` auf Imports und Aufrufe der ausgelagerten Composables reduzieren.
- [ ] **3.4** Build + UI-Test Tour-Planer.

**Ziel:** TourPlannerScreen.kt unter ~400 Zeilen.

---

## Schritt 4: Phase 1.2 – CustomerDetailScreen aufteilen

- [x] **4.1** Wochentag-UI aus Schritt 2 nutzen (bereits in Schritt 2) (keine lokale `WochentagChipRow` mehr).
- [x] **4.2** Sub-Composables auslagern unter `ui/detail/`:
  - [x] Wochentag-UI aus WochentagUi
  - [x] `CustomerDetailStatusSection.kt`
  - [x] `CustomerDetailRegelNameRow.kt`
  - [x] `CustomerDetailIntervallRow.kt`
- [x] **4.3** Gemeinsame UI-Konstanten (SectionSpacing etc.) in `DetailUiConstants.kt`; genutzt in CustomerDetailScreen, CustomerDetailStatusSection, CustomerDetailRegelNameRow, CustomerDetailIntervallRow.
- [ ] **4.4** Build + UI-Test Kunden-Detail.

**Ziel:** CustomerDetailScreen.kt unter ~400 Zeilen.

---

## Schritt 5: Phase 1.4 – TourPlannerCallbackHandler aufteilen

- [x] **5.1** Gemeinsame Hilfsfunktionen extrahieren (`TourPlannerCallbackHelpers.serializeVerschobeneTermine`) (z.B. `serializeVerschobeneTermine`, `executeErledigung`) → neue Datei z.B. `tourplanner/TourPlannerCallbackHelpers.kt` oder in bestehende Util.
- [x] **5.2** Nach Verantwortung aufteilen:
  - [x] `TourPlannerErledigungHandler` (Abholung, Auslieferung, KW, Rückgängig) – neue Datei.
  - [x] `TourPlannerVerschiebenUrlaubHandler` (Verschieben, Urlaub) – neue Datei.
- [x] **5.3** `TourPlannerCallbackHandler.kt` als Fassade oder Coordinator, der die neuen Handler nutzt.
- [ ] **5.4** Build + Test Erledigung/Verschieben/Urlaub.

**Betroffene Dateien:**  
`tourplanner/TourPlannerCallbackHandler.kt`, neu: Handler- und Helper-Dateien im Paket `tourplanner/`

---

## Schritt 6: Phase 2.2 – getKundenArtLabel zentral

- [x] **6.1** `getKundenArtLabel(customer)` aus `TourPlannerScreen.kt` in `CustomerTypeButtonHelper.kt` oder neue `ui/common/KundenArtLabel.kt` / `domain/KundenArtLabel.kt` verschieben.
- [x] **6.2** TourPlannerScreen nur noch Aufruf der zentralen Funktion.
- [ ] **6.3** Build.

**Betroffene Dateien:**  
`ui/tourplanner/TourPlannerScreen.kt`, `ui/CustomerTypeButtonHelper.kt` oder neu `ui/common/KundenArtLabel.kt`

---

## Schritt 7: Weitere große Screens (Phase 1.3, 1.5, 1.7, 1.8)

- [x] **7.1** CustomerManagerScreen.kt (~455 Z.): Kundenkarten-Liste in `CustomerManagerCard.kt`; `formatALWochentag` bereits in Schritt 2 ersetzt.
- [x] **7.2** ListeBearbeitenScreen.kt (~390 Z.): `IntervallRow`, `KundeInListeItem` in `ListeBearbeitenIntervallRow.kt`, `ListeBearbeitenKundeInListeItem.kt` (Wochentag aus Schritt 2).
- [x] **7.3** AddCustomerScreen.kt (~372 Z.): RadioOption, IntervallSchnellauswahl, WeekdaySelector in `AddCustomerComponents.kt`.
- [x] **7.4** KundenListenScreen.kt (~352 Z.): ListenItem in `KundenListenListenItem.kt`.

**Reihenfolge:** 7.1 → 7.2 → 7.3 → 7.4 (oder nach Priorität).

---

## Schritt 8: Optional – Phase 1.6, 3, 4

- [x] **8.1** TourDataProcessor.kt: Hilfsmethoden `warUeberfaelligUndErledigtAmDatum`, `istTerminUeberfaellig`, `hatUeberfaelligeAbholung`, `hatUeberfaelligeAuslieferung`, `berechneAlleTermineFuerKunde` extrahiert.
- [ ] **8.2** Phase 3 (Paket/Dateinamen Liste vs. Tour): nur bei neuer Funktionalität „Tour“ nutzen; große Umbenennung separates Vorhaben.
- [ ] **8.3** Phase 4: Deprecated abbauen (`Customer.getFaelligAm()`, `listenWochentag` etc.); TerminRegelManager nach 2.3 prüfen.

---

## Kurzreferenz: Dateien > 300 Zeilen

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

---

## Abhängigkeiten (Überblick)

- Schritt 1 unabhängig → zuerst.
- Schritt 2 reduziert Duplikate in Schritten 3, 4, 7.
- Schritte 3 und 4 können nach 2; bei 4 Nutzung von 2 einplanen.
- Schritt 5 unabhängig von 2.x.
- Schritte 6–8 beliebig nach 1–5.
