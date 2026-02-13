# Referenz: Analysen und Berichte (konsolidiert)

**Stand:** Feb 2026  

Dies ist die **eine Datei** mit dem vollständigen, noch relevanten Inhalt aus den ehemaligen Analysen-, Berichts- und Plan-Dateien.

- **Manifest (Scope, Technik, Ziele):** PROJECT_MANIFEST.md  
- **Ideen und Pläne für die Zukunft:** ZUKUNFTSPLAENE.md  

Die ursprünglichen Einzeldateien können nach Prüfung entfernt werden; der Inhalt steht hier.

---

# 1. Architektur: Schwere Activities – Coordinator & ViewModel

**Stand:** Januar 2026  
**Ziel:** Activity nur noch Binding, ViewModel beobachten, Coordinator/Intents; keine Geschäftslogik, keine vielen Helper-Instanzen direkt in der Activity.

## 1.1 Regeln für die Activity

**Activity darf**
- **Binding setzen:** `binding = XBinding.inflate(layoutInflater); setContentView(binding.root)`
- **Klicks an ViewModel/Coordinator weiterreichen:** z. B. `binding.btnX.setOnClickListener { viewModel.doX() }` oder `coordinator.onX()`
- **Observables beobachten:** `viewModel.state.observe(this) { ... }` bzw. `lifecycleScope.launch { viewModel.flow.collect { ... } }`
- **Intents/Fragments/Dialoge anstoßen:** `startActivity(...)`, `supportFragmentManager`, Launcher registrieren

**Activity darf nicht**
- **Repository direkt aufrufen** (außer ggf. für reine DI-Weitergabe an Coordinator)
- **Termin-/Erledigungslogik berechnen** (gehört ins ViewModel oder Use-Case)
- **Viele if/else für Geschäftslogik** enthalten
- **Viele Helper-Instanzen selbst verwalten** – ein Coordinator pro Screen kapselt sie

## 1.2 Umgesetzt: CustomerDetail

| Schicht | Verantwortung |
|--------|----------------|
| **CustomerDetailViewModel** | State (currentCustomer, deleted, isLoading, errorMessage); Geschäftslogik: setCustomerId, saveCustomer(updates), deleteCustomer(); Datenquelle: Repository getCustomerFlow(customerId). |
| **CustomerDetailCoordinator** | Kapselt UISetup, Callbacks, EditManager, PhotoManager; setupUi(); updateCustomer(customer) bei ViewModel-Update; Launcher-Results: onTakePictureResult, onPickImageResult, onCameraPermissionResult. |
| **CustomerDetailActivity** | Binding; Launcher registrieren (Result an coordinatorRef weiterreichen); viewModel.setCustomerId(id); coordinator.setupUi(); ViewModel beobachten (currentCustomer → coordinator.updateCustomer, deleted → setResult + finish, errorMessage → Toast); onDestroy: coordinatorRef = null. |

**Abhängigkeiten:** ViewModel braucht CustomerRepository. Coordinator braucht Activity, Binding, ViewModel, CustomerRepository, TerminRegelRepository (im Code: TerminRegelManager / regelNameByRegelId aktuell emptyMap), customerId, Launcher (3x), onProgressVisibilityChanged.

## 1.3 Umgesetzt: TourPlanner

- **TourPlannerCoordinator** kapselt: TourPlannerDateUtils, TourPlannerDialogHelper, TourPlannerCallbackHandler, CustomerDialogHelper (Sheet); viewDate (Calendar), sync mit ViewModel.selectedTimestamp; deleteTermin(), resetTourCycle(), reloadCurrentView().
- **TourPlannerActivity** nur noch: Coordinator erstellen, setContent mit TourPlannerScreen; ViewModel/NetworkMonitor beobachten; Compose-State halten; alle Erledigungs-/Dialog-Aufrufe an coordinator weiterreichen.
- **Datum:** Im ViewModel (selectedTimestamp); viewDate im Coordinator für Helper. – Erledigt (Februar 2026).

## 1.4 Dokumentation pro Activity (Übersicht)

- **CustomerDetail:** ViewModel = State + load/save/delete; Coordinator = UISetup, Callbacks, EditManager, PhotoManager; Activity = Binding, Launcher, Observer.
- **TourPlanner:** ViewModel = selectedTimestamp, tourItems, loadTourData; Coordinator = dateUtils, dialogHelper, callbackHandler, sheetDialogHelper; Activity = setContent, Observer, Compose-State.

---

# 2. Bericht: Zustand der App (Februar 2026)

**Zweck:** Stand der App, Code-Verbesserungen, mögliche Fehler, Termin-Logik-Vereinheitlichung.

## 2.1 Gesamtzustand

- Manifest klar; Architektur Coordinator + ViewModel; Termin-Berechnung zentral in **TerminBerechnungUtils**, Cache in **TerminCache**, Filter in **TourDataFilter**. Keine TODO/FIXME in Kotlin (grep).

## 2.2 Code-Verbesserungen (ohne Verhalten zu brechen)

| Thema | Befund | Vorschlag |
|-------|--------|-----------|
| **Deprecated Felder** | Customer: abholungDatum, auslieferungDatum, wiederholen, intervallTage, letzterTermin, wochentag, listenWochentag @Exclude/@Deprecated. | Beibehalten bis Migration abgeschlossen; keine Löschung ohne Prüfung aller Referenzen. |
| **Customer.getFaelligAm()** | @Deprecated, delegiert an TerminBerechnungUtils.naechstesFaelligAmDatum. | Aufrufer auf naechstesFaelligAmDatum/effectiveFaelligAmDatum umstellen, dann getFaelligAm() entfernen. |
| **Doppelte Intervall-Logik** | TourDataFilter und TourPlannerDateUtils: isIntervallFaelligAm für **ListeIntervall** doppelt. | Eine gemeinsame Implementierung (z. B. TerminBerechnungUtils oder IntervallBerechnungUtils), beide Stellen darauf zugreifen. |
| **AgentDebugLog** | In TerminBerechnungUtils und CustomerDetailTermineTab. | Für Produktion nur in Debug-Build oder entfernen. |

## 2.3 Mögliche Fehler / Lücken

- **Fälligkeit nur aus berechneten Terminen:** TourDataFilter.customerFaelligAm() und TourPlannerDateUtils.getFaelligAmDatum* nutzen nur **berechneAlleTermineFuerKunde** (Intervalle, Wochentag, listenTermine) – **nicht** kundenTermine und **nicht** ausnahmeTermine. Kunden nur mit kundenTermine/ausnahmeTermine haben dort kein Fälligkeitsdatum.
- **TerminCache.getTermine365** liefert nur berechnete Termine; getTerminePairs365 merged für Anzeige. Wer nur getTermine365/getTermineInRange für „wann fällig?“ nutzt, sieht keine Kunden-/Ausnahme-Termine.

**Empfehlung:** Eine einheitliche Quelle für alle Termine (inkl. kundenTermine + ausnahmeTermine) – z. B. `berechneAlleTermineFuerKundeVollstaendig` oder Parameter `includeKundenUndAusnahmeTermine` – und alle Fälligkeits-/Erledigungsstellen darauf umstellen.

## 2.4 Termin-Berechnung vereinheitlichen

- **Option A:** Neue Funktion `berechneAlleTermineFuerKundeVollstaendig(...)` die berechnete + kundenTermine + ausnahmeTermine zu einer Liste zusammenführt; alle Fälligkeits-/„Termin am Datum“-Stellen nutzen nur noch diese.
- **Option B:** berechneAlleTermineFuerKunde um `includeKundenUndAusnahmeTermine: Boolean = false` erweitern.

## 2.5 Termin- und Intervall-Arten (Überblick)

| Art | Wo | Daten |
|-----|-----|-------|
| Regelmäßig (Intervall) | Customer.intervalle (CustomerIntervall) | wiederholen, intervallTage, regelTyp (WEEKLY, …) |
| Monatlich (n-ter Wochentag) | CustomerIntervall regelTyp MONTHLY_WEEKDAY | monthWeekOfMonth, monthWeekday, tageAzuL |
| Wochentag (ohne Intervall) | effectiveAbholungWochentage | Termine an diesen Wochentagen, L = A + tageAzuL |
| Unregelmäßig / Ad-hoc | kundenTyp UNREGELMAESSIG/AUF_ABRUF | kundenTermine oder ausnahmeTermine |
| Kunden-Termine | Customer.kundenTermine | datum, typ "A"/"L" |
| Ausnahme-Termine | Customer.ausnahmeTermine | datum, typ "A"/"L" |
| Listen-Termine | KundenListe.listenTermine | A/L für die ganze Liste |
| Wochentagslisten | KundenListe wochentag 0..6 | wochentagA, tageAzuL |
| Listen ohne Wochentag (Listenkunden) | KundenListe wochentag z. B. -1 | listenTermine, wochentagA, tageAzuL |

AusnahmeTermin und KundenTermin sind strukturell identisch (datum, typ "A"/"L"); Vereinheitlichung z. B. Einzeltermin(datum, typ, quelle: AUSNAHME | KUNDE) möglich.

## 2.6 Kurzfassung

- App-Stand stabil; Deprecated abräumen, doppelte Intervall-Logik zusammenführen, Debug-Log optional.
- Fälligkeit/Erledigung: kundenTermine/ausnahmeTermine an einer „vollständigen“ Termin-Quelle einbeziehen.

---

# 3. Compose-Migration – Protokoll

**Regel:** Neu bauen nur in Compose; bestehende Screens schrittweise migriert.

## 3.1 Strategie

1. Compose einbinden – Build vorbereiten. 2. Ersten kleinen Screen wählen. 3. Ein Screen nach dem anderen. 4. Neue Features von vornherein in Compose.

## 3.2 Phasen (Stand: alle erledigt)

- **Phase 0:** Compose-BOM, buildFeatures, Compose Compiler (Gradle-Plugin); Build läuft.
- **Phase 1:** StatisticsActivity vollständig in Compose.
- **Phase 2:** ListeErstellen, TerminRegelErstellen, MapView [x]; LoginActivity [ ] (ohne sichtbare UI).
- **Phase 3:** TerminRegelManager, KundenListen, ListeBearbeiten, AddCustomer [x].
- **Phase 4:** MainActivity, CustomerManager, CustomerDetail, TourPlanner, Erledigung-BottomSheet [x].

**Wann erledigt:** Screen läuft vollständig in Compose (kein XML-Layout mehr), getestet. Neue UI wird nur noch in Compose gebaut.

*Letzte Aktualisierung: Alle genannten Screens in Compose; LoginActivity ohne sichtbare UI.*

---

# 4. Referenz: Kurzstand (Compose, Architektur, Performance, Termin, OCR)

- **Compose:** Phase 0–4 abgeschlossen; alle Screens in Compose (außer Login ohne UI). Siehe Abschn. 3.
- **Architektur:** Activity nur Binding, ViewModel beobachten, Coordinator; CustomerDetail und TourPlanner umgesetzt. Hinweis: Im Code TerminRegelManager (object), kein TerminRegelRepository in Koin; regelNameByRegelId = emptyMap().
- **Performance & Offline:** Prio 1 (Doppelladung, Listener, Offline, Sync-Hinweis) erledigt. Prio 2 (Paging) offen. Prio 3 Thumbnails erledigt. Prio 4 dauerhaft abgesichert. Prio 5 Dateigröße offen.
- **TourPlanner 3-Tage:** Start auf Heute, 3-Tage-Fenster + Überfällig (60 Tage), +1-Tag-Vorberechnung, MainViewModel getCustomersForTour – Phasen 1–4 erledigt; Phase 5 (Paging) optional.
- **Termin-Logik:** TerminBerechnungUtils, TerminCache, TourDataFilter, TourDataProcessor. Lücke: Fälligkeit berücksichtigt nicht überall kundenTermine/ausnahmeTermine (BEKANNTE_FEHLER).
- **Größte Dateien (ca.):** TerminBerechnungUtils ~480, SevDeskApi ~451, TourDataProcessor ~409, WaschenErfassungViewModel ~422, CustomerRepository/CustomerStammdatenForm ~383, TourPlannerScreen ~380. Faustregel: 200–400 Zeilen gut, bis ~600 überschaubar.
- **Wäscheliste/OCR:** ML Kit (On-Device), Bereich „Wäscheliste“–„Vielen Dank“, Felder Name/Adresse/Telefon/Artikeltabelle. Handschrift schlechter als Druck. Cloud-Optionen siehe Abschnitt 8.

---

# 5. Performance & Offline – Prioritäten und Stand

## 5.1 Prioritätenliste

**Prio 1:** Keine Doppelladung, Listener nur bei Bedarf, Firebase Offline aktiv, Offline-/Sync-Hinweis in der UI → **alle erledigt (Feb 2026)**.  
**Prio 2:** Paging im Kundenmanager, Suche/Filter mit Paging → **offen** (bei Bedarf vor ~500 Kunden).  
**Prio 3:** Fotos als Thumbnails → **erledigt**.  
**Prio 4:** TourPlanner-Berechnung im Hintergrund, Firebase-Persistence → **dauerhaft abgesichert**.  
**Prio 5:** Dateigröße/Aufteilung großer Screens → **offen** (auf Nutzer-Bestätigung warten).

## 5.2 Stand Erledigt/Offen

| Prio | Thema | Status |
|------|--------|--------|
| 1.1–1.4 | Doppelladung, Listener, Offline, Sync-Hinweis | ✅ Erledigt |
| 2.1–2.2 | Paging, Suche/Filter mit Paging | ⏳ Offen |
| 3.1 | Thumbnails | ✅ Erledigt |
| 4.1–4.2 | TourPlanner-Berechnung, Persistence | ✅ Erledigt |
| 5.1 | Dateigröße/Aufteilung | ⏳ Offen |

**Phase-1-Abschluss (Prio 1):** Statistik/Badge nutzen Tour-Kunden; Flows nur bei sichtbarem Hauptbildschirm; Persistence in Application aktiv; Offline-/Sync-Hinweis auf Hauptbildschirm, TourPlanner- und Kundenmanager-TopBar.  
**Release-Checkliste:** Nach Änderungen an App-Start/Firebase-Init „Persistence aktiv?“ prüfen.

## 5.3 Phasen (Auszug)

- **Phase 2 (Prio 2):** Paging im Kundenmanager (Seitengröße 50–100), danach Suche/Filter mit Paging. Firebase Realtime DB hat kein klassisches Offset – clientseitig paginieren oder Ausschnitt-Konzept.
- **Phase 3:** Thumbnails – erledigt (Kundenmodell Thumbnail-URLs, Upload erzeugt Thumb, Listen/Vorschau laden Thumbnails).
- **Phase 4:** Daueraufgabe; Persistence aktiv lassen.
- **Phase 5:** Große Dateien aufteilen (TopBar, Listenbereich, Detailbereich), keine Verhaltensänderung.

---

# 6. TourPlanner Performance – 3-Tage-Fenster

**Ziel:** Start immer auf Heute, nur Daten heute−1/heute/heute+1 (+ Überfällig z. B. 60 Tage), flüssiger Tagwechsel.

## 6.1 Prioritätsliste (Status)

| Prio | Aufgabe | Status |
|------|---------|--------|
| 1 | Start immer auf Heute | ✅ Erledigt |
| 2–6 | TourDataProcessor, TourDataFilter, TourListenProcessorImpl, TourPlannerStatusBadge, TourPlannerDateUtils: 3-Tage-Fenster + Überfällig | ✅ Erledigt |
| 7 | +1-Tag-Vorberechnung (Cache, preload) | ✅ Erledigt |
| 8 | MainViewModel: getCustomersForTour + reduziertes Fenster | ✅ Erledigt |

## 6.2 Phasen (Kurz)

- **Phase 1:** Standard-Datum auf heute; last_view_date optional.
- **Phase 2:** Fenster von 365/730 auf 3 Tage (viewDateStart−1 bis viewDateStart+2); Überfällig eigenes Vergangenheitsfenster (z. B. 60 Tage). Betroffen: TourDataProcessor, TourDataFilter, TourListenProcessorImpl, TourPlannerStatusBadge, TourPlannerDateUtils.
- **Phase 3:** Cache für vorberechnete Tour-Daten; bei heute morgen vorberechnen; bei Tagwechsel zuerst Cache.
- **Phase 4:** Tour-Count von getAllCustomersFlow auf getCustomersForTourFlow; getFälligCount mit reduziertem Fenster.
- **Phase 5 (optional):** Paging bei >500 Kunden (Manifest).

**Offene Punkte:** last_view_date entfallen oder als Einstellung? Überfällig maximale Vergangenheitstiefe (60/90 Tage)?

---

# 7. Plan-Umsetzung: Zentralisierung (2026-02-12)

**Status:** Phasen 1–4 abgeschlossen.

## 7.1 Umgesetzt

- **ComposeDialogHelper:** ConfirmDialog, InfoDialog, CustomDialog, DialogState (SettingsScreen, SevDeskImportScreen, Listen- und Privat-Kundenpreise-Screen).
- **AppNavigation:** Typ-sichere Intents für alle Activities; Keys (CUSTOMER_ID, TOUR_ID, LISTE_ID, BELEG_MONTH_KEY, …). MainActivity, SettingsActivity, CustomerDetailActivity exemplarisch refactored.
- **Result<T>:** Erweitert um Loading, onLoading(), isLoading().
- **FirebaseRetryHelper:** executeWithRetry, setValueWithRetry, updateChildrenWithRetry, removeValueWithRetry. CustomerRepository updateCustomerResult/deleteCustomerResult.
- **FirebaseConstants:** CUSTOMERS, CUSTOMERS_FOR_TOUR, TOUR_PLAENE, KUNDEN_LISTEN, ARTICLES, KUNDEN_PREISE, WASCH_ERFASSUNGEN; Customer-Felder (NAME, ADRESSE, PLZ, …). CustomerRepository nutzt sie.
- **Colors:** status_offline_yellow; MainScreen colorResource. Weitere Duplikate identifiziert (SevDeskImportScreen, ListeBearbeitenScreen, StatisticsScreen, ListeErstellenScreen).
- **AppLogger:** e(), w(), i(), d(), v(), logException(); Tag-Prefix WE2026/. CustomerRepository umgestellt. Duplikate: printStackTrace, Log.e/d in Repositories/TourPlannerErledigungHandler.
- **BaseViewModel:** isLoading, errorMessage; executeWithLoading, executeWithErrorHandling, executeWithLoadingAndErrorHandling, showError, clearError. Empfohlen für UrlaubViewModel, WaschenErfassungViewModel, CustomerDetailViewModel.

## 7.2 Nächste Schritte (aus Plan, in ZUKUNFTSPLAENE.md)

- Repositories auf FirebaseRetryHelper umstellen (1/9), Activities auf AppNavigation (3/24).
- Color(ContextCompat.getColor) → colorResource(); Log.e/printStackTrace → AppLogger.
- ViewModels von BaseViewModel erben; optional DialogBaseHelper konsolidieren.
- Crashlytics in AppLogger; Result Loading in Repositories; Navigation-Testing.

---

# 8. Wäscheliste-Formular: OCR – Optionen & Cloud-Dienste

Stand: Februar 2026.

## 8.1 Aktuell in der App (On-Device)

- **Engine:** ML Kit Text Recognition (Latin, gebündelt) – `com.google.mlkit:text-recognition:16.0.1`
- **Bereich:** Nur zwischen „Wäscheliste“ und „Vielen Dank für Ihren Auftrag!“; Kopf/Foot ignoriert.
- **Felder:** Name, Adresse, Telefon (Labels NAME/ADRESSE/TELEFON), Artikeltabelle mit Mengen. Sonstiges nur manuell.
- **Mengen:** Häkchen ignoriert; erste Zahl 1–999 als Menge (nicht Maximum).
- **Einschränkung:** Handschrift deutlich schlechter als Druck.

## 8.2 Verbesserung ohne Technik-Wechsel

- Foto: Gleichmäßiges Licht, scharf, Formularbereich im Bild. Formular: „Bitte Druckbuchstaben, Kugelschreiber/Filzstift“. OCR als Hilfe – Erkanntes prüfen, Rest manuell.

## 8.3 Technische Optionen

| Option | Aufwand |
|--------|--------|
| Andere On-Device-Engine (z. B. Tesseract) | Mittel; Handschrift bleibt schwierig |
| Cloud-OCR (Google, AWS, Azure, OCR.space) | Mittel bis hoch (API, Datenschutz); bessere Handschrift |
| Eigenes Modell | Sehr hoch |
| Bildvorverarbeitung (Kontrast, Begradigung) | Gering |

## 8.4 Cloud-Dienste (Kosten/Free)

- **Google Vision API:** 1.000 Einheiten/Monat free, danach ca. 1,50 $/1.000. Handschrift unterstützt.
- **Google Document AI:** Kein dauerhaftes Free; ca. 1,50 $/1.000 Seiten; Handschrift besser.
- **AWS Textract:** Free Tier oft 1.000 Seiten/Monat (erste 3 Monate); danach pro Seite. Handschrift, Formulare, Tabellen.
- **Azure Document Intelligence:** Oft 500 Seiten/Monat Free; danach pro Seite. Handschrift, OCR, Layout, Formulare.
- **OCR.space:** Free API mit Limits; Engine 3 mit Handschrift-OCR. Datenschutz/Server-Standort prüfen.

*Preise auf offiziellen Seiten prüfen.*

## 8.5 Datenschutz (DSGVO)

- Cloud: Daten verlassen das Gerät. Für personenbezogene Kundendaten EU/DSGVO-konforme Optionen oder anonymisierte/Test-Daten. ML Kit On-Device: keine Weitergabe.

## 8.6 Relevante Dateien im Projekt

- Parser: `wasch/WaeschelisteOcrParser.kt`
- Formular-State & Merge: `wasch/WaeschelisteFormularState.kt`
- ViewModel: `ui/wasch/WaschenErfassungViewModel.kt`
- UI Formular: `ui/wasch/WaeschelisteFormularContent.kt`

---

# 9. Tiefenanalyse & Gesamtanalyse (konsolidierte Referenz)

*Inhalt ehemals in BERICHT_TIEFENANALYSE_APP_2026.md und GESAMTANALYSE_APP_2026_02.md (Dateien gelöscht); Zukunfts-Ideen in ZUKUNFTSPLAENE.md.*

## 9.1 Abgleich mit Code (Stand Feb 2026)

**Erledigt:** App-Name vereinheitlicht (TourPlaner 2026); Login-Feedback (Fehlermeldung + Retry); Coordinator + ViewModel für CustomerDetail und TourPlanner; Compose-Migration abgeschlossen; Koin durchgängig; Result + AppErrorMapper an zentralen Stellen.

**Teilweise:** Fehlerbehandlung (Result/AppErrorMapper nicht überall); getFaelligAm() – keine Aufrufer, Entfernung möglich; contentDescription – nicht überall systematisch geprüft.

**Offen:** Vollständige Termin-Quelle (kundenTermine/ausnahmeTermine in Fälligkeit); offene Bugs siehe BEKANNTE_FEHLER.md; doppelte Intervall-Logik; TerminRegel-Konsistenz (Doku: TerminRegelManager/emptyMap); AgentDebugLog nur in Debug; LoadState kaum genutzt.

## 9.2 App-Struktur (Pakete, Dateien)

- **Pakete:** ui/ (main, customermanager, detail, tourplanner, listebearbeiten, wasch, sevdesk, statistics, mapview, urlaub, kundenlisten, liste, addcustomer, common), data/repository/, util/, tourplanner/, sevdesk/. Activities: 18+ (Login, Main, AddCustomer, CustomerManager, CustomerDetail, TourPlanner, KundenListen, ListeErstellen, ListeBearbeiten, …).
- **Größte Dateien (Wartbarkeit):** TerminBerechnungUtils ~480, SevDeskApi ~451, TourDataProcessor ~409, WaschenErfassungViewModel ~422, CustomerRepository/CustomerStammdatenForm ~383, TourPlannerScreen ~380, CustomerDetailActivity ~368, … Faustregel: 200–400 Zeilen gut, bis ~600 überschaubar.

## 9.3 Architektur: Stärken und Schwächen

**Stärken:** Coordinator + ViewModel für schwere Screens (CustomerDetail, TourPlanner); zentrale Terminlogik (TerminBerechnungUtils, TerminFilterUtils, TerminCache); Compose-Migration abgeschlossen; Koin durchgängig; Repository-Interface mit Fake für Tests; Offline-First (Persistence, NetworkMonitor); Tests für Kernlogik.

**Schwächen:** TerminRegel in Doku als Repository, im Code TerminRegelManager/emptyMap; Fälligkeit nicht überall inkl. kundenTermine/ausnahmeTermine; doppelte isIntervallFaelligAm (ListeIntervall); Deprecated-Felder noch vorhanden; Fehlerbehandlung uneinheitlich; vereinzelt hardcodierte Strings.

## 9.4 Bekannte Fehler

**Aktuelle offene Bugs:** Siehe **BEKANNTE_FEHLER.md**.

Offene technische Punkte (ohne Fix-Freigabe): Fälligkeit berücksichtigt nicht überall kundenTermine/ausnahmeTermine (customerFaelligAm, naechstesFaelligAmDatum, getFaelligAmDatum*); doppelte isIntervallFaelligAm (ListeIntervall) in TourDataFilter.kt und TourPlannerDateUtils.kt.

## 9.5 Verbesserungsvorschläge (priorisiert)

**Hohe Priorität:** Vollständige Termin-Quelle; offene Bugs nach Freigabe (siehe BEKANNTE_FEHLER.md); einheitliche Fehlerbehandlung (LoadState/Result); Strings in strings.xml, contentDescription.

**Mittlere Priorität:** Doppelte Intervall-Logik zusammenführen; Deprecated abräumen (getFaelligAm(), ggf. Felder); TerminRegel-Doku anpassen; AgentDebugLog nur Debug; SevDeskApi bei Wachstum aufteilen.

**Niedrige Priorität:** CustomerAdapter-/Callback-Bündelung; Barrierefreiheit (48dp, Kontrast); Dark Theme prüfen; MapView bei vielen Adressen (Filter „nur heute“ oder Aufteilung).

## 9.6 Code-Vorschläge (konkret)

- **Termin-Quelle:** Funktion alleTermineFuerKunde(customer, liste, from, to) die berechneAlleTermineFuerKunde + kundenTermine + ausnahmeTermine merged; alle Fälligkeitsstellen darauf umstellen.
- **ListeIntervall-Fälligkeit:** Eine isListeIntervallFaelligAm in TerminBerechnungUtils (oder IntervallBerechnungUtils); TourDataFilter und TourPlannerDateUtils nutzen sie.
- **Fehlerbehandlung:** AppErrorMapper überall nutzen; Texte in strings.xml; ViewModels optional LoadState&lt;T&gt;.

## 9.7 Design/UX (aus Tiefenanalyse)

- **Design-Manifest:** DESIGN_MANIFEST.md (Material 3, Farbsystem, Typografie). Themes: Theme.We2026_5, values-night. Komponenten: Buttons (glossy A/L/KW/V/U), CardViews, TabLayouts, Bottom-Sheet, Status-Badges.
- **UX-Stärken:** Tourenplaner Sektionen Überfällig/Heute/Erledigt; Erledigung semantische Farben; Suche mit Debounce; Offline-/Sync-Hinweise.
- **Verbesserungspotenzial:** App-Name einheitlich (erledigt); Statistik-Labels in strings.xml; Barrierefreiheit (TalkBack, 48dp, Kontrast); Dark Theme durchgängig; Kundenkarte entlasten (z. B. Telefon nur im Sheet).

## 9.8 Beste nächste Schritte / Beste Ideen

- **Schnell (ohne Verhalten):** Doku TerminRegelRepository → TerminRegelManager/emptyMap; getFaelligAm() entfernen; AgentDebugLog nur in Debug.
- **Stabilität:** Vollständige Termin-Quelle; offene Bugs siehe BEKANNTE_FEHLER.md (nach Freigabe beheben).
- **Wartbarkeit:** ListeIntervall-Fälligkeit eine Stelle; AppErrorMapper in strings.xml; LoadState wo sinnvoll.
- **UX:** Barrierefreiheit prüfen; optional regelNameByRegelId aus Regeldaten füllen.
- **Zukunft (nur mit Freigabe):** Paging (>500 Kunden), Benachrichtigungen, Export/PDF, echte Karte, Rollen, Kalender-Export, Onboarding (siehe ZUKUNFTSPLAENE.md).

---

**Bugs:** BEKANNTE_FEHLER.md  
**Regeln:** .cursorrules, .cursor/rules/

*Letzte Aktualisierung: Feb 2026*
