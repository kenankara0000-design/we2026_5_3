# Refactoring-Plan (neu) – we2026_5

**Erstellt:** Februar 2026  
**Basis:** Aktuelle Analyse der App-Dateien (Kotlin, Struktur, Abhängigkeiten).

---

## 1. Ausgangslage (Ist-Zustand)

- **~107 Kotlin-Dateien** im App-Modul; UI weitgehend **Jetpack Compose**, Einstieg über **Activities**.
- **Paketstruktur:** `ui/` nach Features (addcustomer, detail, tourplanner, listebearbeiten, kundenlisten, wasch, …), `data/repository/`, `tourplanner/`, `util/`, `sevdesk/`, `liste/`, `adapter/`, `detail/`, `customermanager/`, `di/`, `work/`.
- **Größte Dateien (Zeilen):**
  - TourPlannerScreen.kt **615**
  - CustomerDetailScreen.kt **575**
  - CustomerRepository.kt **409**
  - CustomerManagerScreen.kt **421**
  - WaschenErfassungScreen.kt **358**
  - CustomerStammdatenForm.kt **354**
  - MainScreen.kt **315**
  - ErledigungSheetContent.kt **299**
  - ListeBearbeitenScreen.kt **119** (nach Refactoring A.9)
  - KundenListenScreen.kt **290**
  - TourDataProcessor.kt **376**
  - TerminBerechnungUtils.kt **306**
  - CustomerDetailActivity.kt **308**
  - TourPlannerErledigungHandler.kt **266**
  - weitere 20+ Dateien zwischen 100 und 250 Zeilen.
- **Deprecated-API-Nutzung:** `Customer.getFaelligAm()` wird noch verwendet in: CustomerDetailScreen, StatisticsViewModel (4×), TourPlannerVerschiebenUrlaubHandler. `listenWochentag` wird in AddCustomerActivity beim Anlegen gesetzt.
- **Customer/KundenListe:** Mehrere `@Deprecated`-Felder (intervallTage, abholungDatum, listenWochentag, getFaelligAm() etc.); Migration auf `intervalle` / TerminBerechnungUtils teilweise umgesetzt.
- **RecyclerView-Adapter:** IntervallAdapter, IntervallViewAdapter, ListeIntervallAdapter, ListeIntervallViewAdapter existieren; **keine direkten Referenzen** in anderen Kotlin-Dateien gefunden – Intervall-UI läuft in Compose (ListeBearbeitenIntervallRow, CustomerDetailIntervallRow). Prüfung nötig, ob Adapter + item_intervall.xml noch eingebunden sind (z. B. über AndroidView oder alte Activity).
- **Naming:** Durchgängig Mischung „Liste“ vs. „Tour“ (ListeBearbeiten*, KundenListen*, ListeErstellen*; fachlich Touren/Listen).
- **Tests:** Vorhanden u. a. CustomerRepositoryTest, TourDataProcessorTest, CustomerManagerViewModelTest, TerminBerechnungUtilsTest, TerminRegelManagerTest, ValidationHelperTest.

---

## 2. Ziele

- Wartbarkeit verbessern (Dateigrößen, klare Verantwortung).
- Deprecated-APIs schrittweise ablösen und entfernen.
- Toten oder doppelten Code identifizieren und ggf. entfernen.
- Keine funktionalen Änderungen; bestehende Logik nicht brechen.

## 2.1 Wichtige Regel vor dem Löschen von Code

**Bevor irgendwelcher Code gelöscht wird**, muss **bestätigt** werden, dass die Löschung **keine Auswirkung** auf **Funktionen** und **Design** hat:

- Prüfen: Wird die Klasse/Datei/Funktion irgendwo referenziert? (Kotlin, XML-Layouts, AndroidManifest, Gradle, Koin/DI.)
- Prüfen: Wird ein Layout (z. B. `item_*.xml`) nur von diesem Code genutzt? Wenn ja, gehört die Nutzungsprüfung dazu.
- Erst wenn **keine Referenz** gefunden wurde (oder nur interne, ungenutzte), darf gelöscht werden – und optional mit einem kurzen Vermerk im Plan/Commit dokumentieren.
- Bei Unsicherheit: **nicht löschen**, im Plan als „ungeklärt“ vermerken und Nutzer/Team fragen.

---

## 3. Phase A: Große Dateien verkleinern (Priorität: hoch)

| # | Datei | Zeilen | Empfohlene Maßnahme |
|---|--------|--------|----------------------|
| A.1 | **TourPlannerScreen.kt** | ~~615~~ **262** | **Erledigt (Feb 2026):** Sub-Composables ausgelagert: TourPlannerOverviewDialog, TourPlannerTopBar, TourPlannerStateViews, TourPlannerErledigtSheet. Ziel unter 450 erreicht. |
| A.2 | **CustomerDetailScreen.kt** | ~~575~~ **373** | **Erledigt (Feb 2026):** Sub-Composables ausgelagert: CustomerDetailTopBar, CustomerDetailStateViews, CustomerDetailActionsRow, CustomerDetailNaechsterTermin, CustomerDetailKundenTypSection, CustomerDetailTerminRegelCard, CustomerDetailFotosSection. Ziel unter 450 erreicht. |
| A.3 | **CustomerRepository.kt** | ~~409~~ **233** | **Erledigt (Feb 2026):** Parsing/Serialisierung in CustomerSnapshotParser.kt ausgelagert; Repository nur noch Orchestrierung + Firebase-Calls + awaitWithTimeout-Helper. Ziel unter 250 erreicht. |
| A.4 | **CustomerManagerScreen.kt** | ~~421~~ **164** | **Erledigt (Feb 2026):** Sub-Composables ausgelagert: CustomerManagerTopBar, CustomerManagerSearchAndFilter, CustomerManagerStateViews, CustomerManagerBulkBar. Ziel unter 350 erreicht. |
| A.5 | **WaschenErfassungScreen.kt** | ~~358~~ **112** | **Erledigt (Feb 2026):** Sub-Composables ausgelagert: WaschenErfassungTopBar, WaschenErfassungKundeSuchenContent, WaschenErfassungErfassungenListeContent, WaschenErfassungDetailContent, WaschenErfassungErfassenContent. |
| A.6 | **CustomerStammdatenForm.kt** | 354 | Optional: „Weitere Angaben“-Block (ExpandableSection-Inhalt) in eigenes Composable. |
| A.7 | **MainScreen.kt** | 315 | Nur bei Bedarf: Karten-/Listen-Bereich oder Aktionen-Bereich auslagern. |
| A.8 | **ErledigungSheetContent.kt** | ~~299~~ **139** | **Erledigt (Feb 2026):** Inhalt in ErledigungSheetKopf, ErledigungTabErledigungContent, ErledigungTabTerminContent, ErledigungTabDetailsContent ausgelagert. |
| A.9 | **ListeBearbeitenScreen.kt** | ~~292~~ **119** | **Erledigt (Feb 2026):** Sub-Composables ausgelagert: ListeBearbeitenTopBar, ListeBearbeitenStateViews, ListeBearbeitenMetadatenBlock, ListeBearbeitenIntervallSection, ListeBearbeitenKundenSection. |
| A.10 | **KundenListenScreen.kt** | 290 | Optional: Filter-Chips, Listen-Karte in eigene Dateien. |

**Reihenfolge:** A.1–A.5, A.8, A.9 erledigt. Als Nächstes nach Aufwand A.6, A.7, A.10.

---

## 4. Phase B: Deprecated-APIs ablösen (Priorität: hoch)

| # | Thema | Vorkommen | Maßnahme |
|---|--------|-----------|----------|
| B.1 | **Customer.getFaelligAm()** | **Erledigt (Feb 2026):** TerminBerechnungUtils.naechstesFaelligAmDatum(customer) hinzugefügt; alle Aufrufer (CustomerDetailScreen, StatisticsViewModel 4×, TourPlannerVerschiebenUrlaubHandler) umgestellt; Customer.getFaelligAm() delegiert an Util. |
| B.2 | **listenWochentag / wochentag** | AddCustomerActivity (listenWochentag = -1), Customer (Felder deprecated) | Sicherstellen, dass nirgends mehr fachlich darauf zugegriffen wird; nur Default beim Anlegen ist unkritisch. Optional: Parameter beim Anlegen entfernen, sobald DB-Migration keine alten Werte mehr erwartet. |
| B.3 | **Weitere @Deprecated in Customer/KundenListe** | intervallTage, abholungDatum, auslieferungDatum, … | Prüfen: Alle Leser auf intervalle / neue Felder umgestellt? Dann Deprecated-Felder nur noch für DB-Lesen (Migration) behalten und klar dokumentieren; keine neuen Aufrufer. |

**Reihenfolge:** B.1 zuerst (sichtbar in UI/Statistik), dann B.2/B.3.

---

## 5. Phase C: Toten / doppelten Code prüfen (Priorität: mittel)

| # | Thema | Maßnahme |
|---|--------|----------|
| C.1 | **RecyclerView-Adapter** | **Erledigt (Feb 2026):** Prüfung durchgeführt – die vier Adapter werden nirgends instanziiert; `ItemIntervallBinding`/`item_intervall.xml` nur in diesen Adaptern verwendet; Intervall-UI läuft in Compose (CustomerDetailIntervallRow, ListeBearbeitenIntervallRow). **Bestätigung: Keine Auswirkung auf Funktion/Design.** Adapter-Dateien und item_intervall.xml entfernt. |
| C.2 | **Duplikate isIntervallFaelligAm / isIntervallFaelligInZukunft** | TourDataFilter, TourDataProcessor, TourPlannerDateUtils haben ähnliche Logik. Prüfen: Eine gemeinsame Implementierung in TourPlannerDateUtils oder TerminBerechnungUtils, Rest delegiert. |
| C.3 | **Doppelte CustomerRepositoryTest** | Es existieren `CustomerRepositoryTest.kt` und `data/repository/CustomerRepositoryTest.kt`. Prüfen: Eine davon umbenennen oder zusammenführen, damit keine Verwechslung entsteht. |

---

## 6. Phase D: Konsistenz und Struktur (Priorität: niedrig)

| # | Thema | Maßnahme |
|---|--------|----------|
| D.1 | **Liste vs. Tour (Namen)** | Paket- und Dateinamen (ListeBearbeiten*, KundenListen*, ListeErstellen*) fachlich „Tour/Listen“. Keine große Umbenennung vorschlagen; nur: Bei **neuen** Dateien/Funktionen konsequent „Tour“ oder „Kundenliste“ verwenden; in Doku festhalten. |
| D.2 | **CustomerDetailActivity** | Mit ~308 Zeilen relativ groß für eine Activity. Optional: Photo-Launcher-/Permission-Logik in CustomerPhotoManager belassen; Activity nur noch setContent + ViewModel-Bindung. |
| D.3 | **util-Paket** | Viele Dateien (TerminBerechnungUtils, TerminFilterUtils, TerminRegelManager, WochentagBerechnung, …). Optional: Unterpakete z. B. `util/termin/`, `util/migration/` für bessere Orientierung; kein Muss. |

---

## 7. Phase E: Testbarkeit und Architektur (Priorität: niedrig)

| # | Thema | Maßnahme |
|---|--------|----------|
| E.1 | **Repository-Interface** | CustomerRepositoryInterface bereits vorhanden; andere Repositories (KundenListeRepository, TourPlanRepository, ErfassungRepository) haben keine Interfaces. Nur bei Bedarf (z. B. für Tests oder Austausch der Implementierung): Interfaces einführen. |
| E.2 | **Unit-Tests** | Bestehende Tests beibehalten und bei Refactorings anpassen. Optional: Tests für TourPlannerErledigungHandler, TourPlannerVerschiebenUrlaubHandler, TerminFilterUtils ergänzen. |

---

## 8. Abhängigkeiten und Reihenfolge

- **Phase A** (Dateien verkleinern) und **Phase B** (Deprecated) können parallel angegangen werden; B.1 entlastet CustomerDetailScreen/Statistics, A.2 bleibt lesbarer.
- **Phase C** (toter Code) vor größeren Umbenennungen (D.1), damit keine toten Adapter übrig bleiben.
- **Phase D und E** jederzeit optional.

**Empfohlene Reihenfolge:**

1. **C.1** – Adapter-Nutzung prüfen (schnell, klärt toten Code). ✓ Erledigt.
2. **B.1** – getFaelligAm() ersetzen. ✓ Erledigt.
3. **A.1** – TourPlannerScreen verkleinern. ✓ Erledigt (Feb 2026).
4. **A.2** – CustomerDetailScreen verkleinern. ✓ Erledigt (Feb 2026).
5. **A.3** – CustomerRepository Parsing auslagern. ✓ Erledigt (Feb 2026).
6. **B.2/B.3** – restliche Deprecated-Nutzung prüfen.
7. **C.2, C.3** – Duplikate und doppelte Tests.
8. **A.4** – CustomerManagerScreen verkleinern. ✓ Erledigt (Feb 2026).
9. **A.5** – WaschenErfassungScreen verkleinern. ✓ Erledigt (Feb 2026).
10. **A.8** – ErledigungSheetContent aufteilen. ✓ Erledigt (Feb 2026).
11. **A.9** – ListeBearbeitenScreen verkleinern. ✓ Erledigt (Feb 2026).
12. **A.6, A.7, A.10, D, E** – nach Bedarf.

---

## 9. Kurzreferenz: Dateien > 250 Zeilen (Stand Analyse)

| Zeilen | Datei |
|--------|--------|
| 262 | ui/tourplanner/TourPlannerScreen.kt |
| 373 | ui/detail/CustomerDetailScreen.kt |
| 233 | data/repository/CustomerRepository.kt |
| 164 | ui/customermanager/CustomerManagerScreen.kt |
| 376 | tourplanner/TourDataProcessor.kt |
| 112 | ui/wasch/WaschenErfassungScreen.kt |
| 354 | ui/addcustomer/CustomerStammdatenForm.kt |
| 315 | ui/main/MainScreen.kt |
| 308 | CustomerDetailActivity.kt |
| 306 | util/TerminBerechnungUtils.kt |
| 139 | ui/tourplanner/ErledigungSheetContent.kt |
| 119 | ui/listebearbeiten/ListeBearbeitenScreen.kt |
| 290 | ui/kundenlisten/KundenListenScreen.kt |
| 266 | tourplanner/TourPlannerErledigungHandler.kt |
| 253 | ui/liste/ListeErstellenScreen.kt |
| 249 | ui/statistics/StatisticsScreen.kt |
| 228 | adapter/CustomerButtonVisibilityHelper.kt |
| 222 | ui/common/WochentagUi.kt |
| 214 | ui/detail/CustomerDetailViewModel.kt |
| 212 | tourplanner/TourDataFilter.kt |
| 212 | TourPlannerActivity.kt |
| 211 | liste/ListeBearbeitenCallbacks.kt |
| 193 | ValidationHelper.kt |
| 189 | data/repository/KundenListeRepository.kt |
| 188 | detail/CustomerPhotoManager.kt |
| 188 | ImageUtils.kt |
| 181 | ui/customermanager/CustomerManagerViewModel.kt |
| 175 | customermanager/CustomerExportHelper.kt |
| 173 | ui/addcustomer/AddCustomerComponents.kt |
| 172 | ui/detail/CustomerDetailStatusSection.kt |
| 161 | ui/urlaub/UrlaubScreen.kt |
| 156 | ui/addcustomer/AddCustomerViewModel.kt |
| 146 | util/DialogBaseHelper.kt |
| 146 | ui/statistics/StatisticsViewModel.kt |
| 143 | tourplanner/TourDataCategorizer.kt |

---

**Ende des Plans.** Bei jeder Änderung: Build + manueller Kurztest der betroffenen Funktion (Termin-Planer, Kunden-Detail, Statistiken, Listen-Bearbeitung).
