# Gesamtanalyse: TourPlaner 2026 (we2026_5)

**Stand:** Februar 2026  
**Zweck:** Tiefe, gründliche Analyse der gesamten App-Struktur mit Verbesserungsvorschlägen, Code-Empfehlungen und Zukunftsplanung.

---

## 0. Abgleich mit Code (Stand Feb 2026)

*Nach Vergleich der Analyse mit dem aktuellen Code: Was ist bereits erledigt, was offen.*

### 0.1 Erledigt

- **[x] App-Name vereinheitlichen:** `strings.xml` enthält `app_name` = „TourPlaner 2026“, `main_title` und `app_title_tourplaner` ebenfalls; Manifest nutzt `@string/app_name`. Einheitlicher Anzeigename.
- **[x] Login-Feedback:** LoginActivity zeigt bei Fehler `error_login_failed` aus strings.xml, State `loginError`, und Nutzer kann erneut „Anonym weiter“ klicken (Retry). Button „Erneut versuchen“ (login_retry) blendet Fehler aus. Entspricht ZUKUNFTSPLAENE „Meldung + Retry“.
- **[x] Coordinator + ViewModel für schwere Screens:** Wie in Analyse beschrieben umgesetzt (CustomerDetail, TourPlanner).
- **[x] Compose-Migration:** Alle Screens in Compose; COMPOSE_MIGRATION_PROTOKOLL bestätigt.
- **[x] Koin durchgängig:** Repositories und ViewModels in AppModule gebunden.
- **[x] Result + AppErrorMapper genutzt:** CustomerRepository (updateCustomer, deleteCustomer) liefert Result<T>, nutzt AppErrorMapper.toSaveMessage/toDeleteMessage. CustomerDetailViewModel, SevDeskImportViewModel, UrlaubViewModel, TourPlannerCoordinator verarbeiten Result. StatisticsViewModel nutzt AppErrorMapper.toLoadMessage.

### 0.2 Teilweise erledigt

- **[~] Fehlerbehandlung:** Result und AppErrorMapper sind an mehreren zentralen Stellen im Einsatz; nicht in allen ViewModels/Screens einheitlich. AppErrorMapper verwendet noch feste deutsche Texte (nicht strings.xml).
- **[~] Deprecated getFaelligAm():** Methode existiert nur in Customer.kt (delegiert an naechstesFaelligAmDatum). **Keine anderen Aufrufer im Code gefunden** – Entfernung der Methode nach kurzer Prüfung möglich.
- **[~] contentDescription:** In mehreren UI-Dateien vorhanden (z. B. MainScreen, TourPlannerTopBar, CustomerManagerTopBar); systematische Vollständigkeit und 48dp Touch-Targets nicht geprüft.

### 0.3 Offen (unverändert)

- **Vollständige Termin-Quelle:** Keine Funktion `alleTermineFuerKunde`/`berechneAlleTermineFuerKundeVollstaendig`; kundenTermine/ausnahmeTermine nicht in allen Fälligkeitsstellen einbezogen.
- **Bekannte Bugs:** Mehrere A-Tage (TerminAusKundeUtils Zeile 50: `effectiveAbholungWochentage.firstOrNull()`); Erledigte bei Vergangenheit (TourDataProcessor: `if (!istVergangenheit)` um Befüllung von tourListenErledigt).
- **Doppelte Intervall-Logik:** TourDataFilter.isIntervallFaelligAm (Zeile 119) und TourPlannerDateUtils.isIntervallFaelligAm (Zeile 68) – zwei getrennte Implementierungen für ListeIntervall; TourDataProcessor delegiert an Filter.
- **TerminRegel-Konsistenz:** ARCHITEKTUR_ACTIVITY_COORDINATOR.md nennt „TerminRegelRepository“ für CustomerDetailCoordinator; im Code gibt es kein TerminRegelRepository in Koin, CustomerDetailActivity übergibt `regelNameByRegelId = emptyMap()`. Dokumentation anpassen oder Repository einführen.
- **AgentDebugLog:** Wird in TourPlannerActivity, TourPlannerViewModel, TerminBerechnungUtils, CustomerDetailTermineTab, TourPlannerCoordinator, FirebaseConfig genutzt; **nicht** an BuildConfig.DEBUG gekoppelt – läuft auch in Release.
- **LoadState:** Sealed class existiert (util/LoadState.kt); wird in der App kaum genutzt (keine ViewModels mit LoadState<T> gefunden).

### 0.4 Beste nächste Schritte (Empfehlung)

1. **Architektur-Doku anpassen:** In ARCHITEKTUR_ACTIVITY_COORDINATOR.md „TerminRegelRepository“ durch „TerminRegelManager / regelNameByRegelId (aktuell emptyMap)“ ersetzen – klärt Inkonsistenz ohne Code-Änderung.
2. **Customer.getFaelligAm() entfernen:** Da keine Aufrufer außer der Definition; naechstesFaelligAmDatum direkt nutzen; Methode und ggf. @Deprecated-Kommentar entfernen (nach Prüfung mit „nicht kaputt machen“).
3. **AgentDebugLog nur in Debug:** Alle Aufrufe in `if (BuildConfig.DEBUG) { AgentDebugLog.log(...) }` wrappen oder AgentDebugLog.log intern prüfen – weniger Log-Ausgaben in Release.
4. **Eine gemeinsame isListeIntervallFaelligAm:** In TerminBerechnungUtils (oder IntervallBerechnungUtils) implementieren; TourDataFilter und TourPlannerDateUtils darauf umstellen – doppelte Logik entfällt.
5. **Vollständige Termin-Quelle:** Wie in Abschnitt 5.1 beschrieben umsetzen – behebt Fälligkeitslücke für kundenTermine/ausnahmeTermine.
6. **Bugs nach Freigabe:** Mehrere A-Tage und Erledigte bei Vergangenheit wie in BEKANNTE_FEHLER.md und Abschnitt 5.2/5.3 angehen.

---

## 1. App-Struktur im Überblick

### 1.1 Projektidentität (aus PROJECT_MANIFEST.md)

- **Name:** TourPlaner 2026 (intern we2026_5)
- **Zweck:** Touren- und Kundenverwaltung für Wäscherei/Textildienstleistung
- **Zielgruppe:** Fahrer, Disponenten, Büro
- **Scope:** Bis 500 Kunden; ab ~500 Kunden Paging/Lazy-Loading empfohlen

### 1.2 Technischer Stack

| Schicht | Technologie |
|--------|-------------|
| UI | Jetpack Compose (Migration abgeschlossen), ViewBinding noch aktiv, vereinzelt XML-Layouts (Dialoge, Items) |
| State/Navigation | ViewModel (Koin), Coordinator für schwere Screens (CustomerDetail, TourPlanner) |
| Daten | Firebase Realtime DB, Firebase Storage (Fotos), Firebase Auth (anonym) |
| DI | Koin (appModule: Repositories, ViewModels) |
| Build | Kotlin 1.x, SDK 34, minSdk 24, Java 17 |

### 1.3 Paket- und Dateistruktur

- **~151 Kotlin-Dateien** unter `app/src/main/java/com/example/we2026_5/`
- **Activities:** 18+ (Login, Main, AddCustomer, CustomerManager, CustomerDetail, TourPlanner, KundenListen, ListeErstellen, ListeBearbeiten, TerminAnlegenUnregelmaessig, Urlaub, WaschenErfassung, ArtikelVerwaltung, SevDeskImport, ErfassungMenu, Belege, Kundenpreise, Settings, Statistics, MapView, AusnahmeTermin)
- **UI:** `ui/` mit Subpaketen pro Feature (main, customermanager, detail, tourplanner, listebearbeiten, wasch, sevdesk, statistics, mapview, urlaub, kundenlisten, liste, addcustomer, common)
- **Daten:** `data/repository/` – CustomerRepository, KundenListeRepository, TourPlanRepository, TourOrderRepository, ArticleRepository, ErfassungRepository, KundenPreiseRepository; CustomerSnapshotParser, Interfaces
- **Domain/Util:** Modelle im Root (Customer, KundenListe, CustomerIntervall, …), `util/` (TerminBerechnungUtils, TerminFilterUtils, Migrations, LoadState, Result, AppErrorMapper, …), `tourplanner/` (TourDataProcessor, TourDataFilter, TerminCache, Coordinator, Helper)
- **SevDesk:** `sevdesk/` (SevDeskApi, SevDeskImport, SevDeskPrefs, SevDeskKundenpreiseImport, …)

### 1.4 Größte Dateien (Wartbarkeit)

| Datei | Zeilen | Hinweis |
|-------|--------|--------|
| SevDeskApi.kt | 451 | API-Client; evtl. in Endpoint-Module aufteilen |
| TourDataProcessor.kt | 409 | Kernlogik Tour; bereits gut in Kategorizer/Filter getrennt |
| CustomerRepository.kt | 383 | Groß; SnapshotParser ausgelagert |
| CustomerStammdatenForm.kt | 383 | Formular; evtl. Sektionen in eigene Composables |
| TourPlannerScreen.kt | 380 | Screen; TopBar/StateViews bereits ausgelagert |
| CustomerDetailActivity.kt | 368 | Coordinator-Pattern umgesetzt |
| CustomerDetailViewModel.kt | 340 | State + Save/Delete; OK |
| TourPlannerActivity.kt | 330 | Coordinator entlastet Activity |
| CustomerDetailScreen.kt | 330 | Tabs/Sektionen; weiter aufteilen möglich |
| ListeBearbeitenCallbacks.kt | 316 | Viele Callbacks; Bündelung sinnvoll |
| MainScreen.kt | 306 | Hauptmenü; überschaubar |
| CustomerDetailTermineTourForm.kt | 299 | Formular; könnte in kleinere Composables |
| TourPlannerViewModel.kt | 286 | OK |
| TourPlannerErledigungHandler.kt | 289 | Erledigungslogik; gut kapsuliert |
| WaschenErfassungViewModel.kt | 422 | Erfassungslogik; evtl. Use-Cases extrahieren |
| TerminBerechnungUtils.kt | 480 | Zentrale Terminlogik; sehr wichtig, gut dokumentiert |

Faustregel aus den Regeln: 200–400 Zeilen gut, bis ~600 überschaubar; 1000+ problematisch. Keine Datei überschreitet 500 Zeilen stark; SevDeskApi und TerminBerechnungUtils sind die Spitzenreiter.

---

## 2. Architektur-Bewertung

### 2.1 Stärken

- **Coordinator + ViewModel für schwere Screens:** CustomerDetail und TourPlanner folgen ARCHITEKTUR_ACTIVITY_COORDINATOR.md; Activity nur Binding, Launcher, Observer; Logik in ViewModel/Coordinator.
- **Zentrale Terminlogik:** TerminBerechnungUtils, TerminFilterUtils, TerminCache; TourDataProcessor/TourDataFilter/TourDataCategorizer klar getrennt.
- **Compose-Migration abgeschlossen:** Alle Screens laut COMPOSE_MIGRATION_PROTOKOLL in Compose; keine neuen XML-Screens.
- **Koin durchgängig:** Repositories und ViewModels zentral in AppModule; ViewModel mit Parametern (customerId, Context) wo nötig.
- **Repository-Interface:** CustomerRepositoryInterface mit FakeCustomerRepository für Tests.
- **Offline-First:** Firebase Persistence, NetworkMonitor, Sync-Hinweise in UI.
- **Tests vorhanden:** TerminBerechnungUtils, TourDataProcessor, CustomerRepository (inkl. Fake), CustomerManagerViewModel, ValidationHelper, TerminRegelManager.

### 2.2 Schwächen / technische Schulden

- **TerminRegelRepository in Architektur erwähnt, nicht in Koin:** ARCHITEKTUR_ACTIVITY_COORDINATOR.md nennt TerminRegelRepository für CustomerDetailCoordinator; in AppModule existiert nur TerminRegelManager (object). Entweder Dokumentation anpassen oder Repository einführen und injizieren.
- **Fälligkeit nicht überall vollständig:** BERICHT_APP_ZUSTAND_2026_02.md: customerFaelligAm / getFaelligAmDatum* nutzen nur berechneAlleTermineFuerKunde (Intervalle, Wochentag, Listen) – **nicht** kundenTermine und ausnahmeTermine. Kunden nur mit Einzel-/Ausnahme-Terminen können falsche oder keine Fälligkeit liefern.
- **Doppelte Intervall-Logik:** TourDataFilter und TourPlannerDateUtils haben jeweils isIntervallFaelligAm für ListeIntervall; eine gemeinsame Stelle (z. B. TerminBerechnungUtils oder IntervallBerechnungUtils) würde Duplikate vermeiden.
- **Deprecated-Felder:** Customer (abholungDatum, auslieferungDatum, wiederholen, intervallTage, …) und Customer.getFaelligAm() noch vorhanden; Aufrufer auf naechstesFaelligAmDatum/effectiveFaelligAmDatum umstellen, dann entfernen.
- **Fehlerbehandlung uneinheitlich:** LoadState/Result vorhanden, aber nicht überall genutzt; Fehler teils Toast, teils UI, teils Log; kein zentraler Firebase → User-Text-Mapper.
- **Hardcodierte Strings:** Teilweise noch im Code (laut Tiefenanalyse); vollständige Auslagerung in strings.xml für Lokalisierung und Konsistenz empfohlen.

---

## 3. Bekannte Fehler (BEKANNTE_FEHLER.md)

- **Mehrere A-Tage:** Nur der erste A-Wochentag wird für Intervalle genutzt; bei mehreren A-Tagen (z. B. Mo + Mi) kann der Termin für den zweiten Tag fehlen. Relevante Stellen: TerminAusKundeUtils, TerminBerechnungUtils.
- **Erledigte-Liste bei Vergangenheit leer:** Bei vergangenem Datum wird tourListenErledigt nicht befüllt (TourDataProcessor, if (!istVergangenheit)); Erledigt-Sheet wirkt leer, obwohl Kunden erledigt waren.

Beide ohne Verhaltensänderung (Fix) ohne ausdrückliche Freigabe.

---

## 4. Verbesserungsvorschläge (vom jetzigen Zustand)

### 4.1 Hohe Priorität (Stabilität, Korrektheit)

1. **Vollständige Termin-Quelle für Fälligkeit**  
   Eine einzige Quelle für „alle Termine inkl. kundenTermine + ausnahmeTermine“ einführen (Option A oder B aus BERICHT_APP_ZUSTAND_2026_02.md):
   - **Option A:** Neue Funktion z. B. `berechneAlleTermineFuerKundeVollstaendig(...)` die berechnete Termine + kundenTermine + ausnahmeTermine merged. Alle Fälligkeits- und „Termin am Datum“-Stellen nutzen nur noch diese (ggf. TerminCache darauf aufsetzen).
   - **Option B:** TerminBerechnungUtils.berechneAlleTermineFuerKunde um optionalen Merge erweitern (`includeKundenUndAusnahmeTermine: Boolean = false`).

2. **Bekannte Bugs adressieren** (nach Freigabe)  
   - Mehrere A-Tage: Alle effectiveAbholungWochentage in der Intervall-/Fälligkeitslogik berücksichtigen.  
   - Erledigte bei Vergangenheit: tourListenErredigt auch für Vergangenheit befüllen (Logik in TourDataProcessor anpassen).

3. **Einheitliche Fehlerbehandlung**  
   Gemeinsames Konzept (LoadState/Result) durchgängig nutzen; Repository gibt Fehler zurück, ViewModel mappt auf UI; optional zentraler AppErrorMapper für Firebase → strings.xml.

4. **Restliche Strings in strings.xml**  
   Alle sichtbaren Texte auslagern; contentDescription für Icons/Buttons prüfen.

### 4.2 Mittlere Priorität (Wartbarkeit, Konsistenz)

5. **Doppelte Intervall-Logik zusammenführen**  
   Eine gemeinsame Implementierung für ListeIntervall-Fälligkeit (z. B. in TerminBerechnungUtils oder IntervallBerechnungUtils), TourDataFilter und TourPlannerDateUtils nutzen sie.

6. **Deprecated sauber abräumen**  
   Alle Aufrufer von Customer.getFaelligAm() auf naechstesFaelligAmDatum/effectiveFaelligAmDatum umstellen, dann getFaelligAm() und ggf. weitere deprecated Felder entfernen (nach Prüfung aller Referenzen gemäß Regel „nicht kaputt machen“).

7. **TerminRegel-Konsistenz**  
   Entweder TerminRegelRepository in Koin aufnehmen und im CustomerDetailCoordinator nutzen oder Architektur-Dokumentation anpassen (TerminRegelManager als einzige Quelle).

8. **AgentDebugLog**  
   Nur in Debug-Build aktivieren oder entfernen, wenn nicht mehr benötigt.

9. **SevDeskApi.kt**  
   Bei weiterem Wachstum in Module/Endpoints aufteilen (z. B. Contacts, Parts, PartContactPrice).

### 4.3 Niedrige Priorität (Nice-to-have)

10. **CustomerAdapter-/Callback-Bündelung**  
    Ein Interface oder eine Datenklasse für alle Erledigungs-/Termin-Callbacks; Lesbarkeit und Testbarkeit erhöhen.

11. **App-Name vereinheitlichen**  
    Ein Name (z. B. „TourPlaner 2026“) in Manifest und Hauptbildschirm; „we2026_5“ nur intern.  
    → **Erledigt:** strings.xml + Manifest = „TourPlaner 2026“.

12. **Login-Feedback**  
    Bei Fehler der anonymen Anmeldung Meldung + Retry statt sofort finish() (bereits in ZUKUNFTSPLAENE.md).  
    → **Erledigt:** LoginActivity zeigt Fehlermeldung und Nutzer kann „Anonym weiter“ erneut ausführen bzw. Fehler mit „Erneut versuchen“ ausblenden.

13. **Barrierefreiheit**  
    contentDescription, 48dp Touch-Targets, Kontrast prüfen; Dark Theme durchgängig testen.

---

## 5. Code-Vorschläge (konkret)

### 5.1 Termin-Quelle vereinheitlichen

- In **TerminBerechnungUtils** (oder neuer Datei `TerminQuelleUtils.kt`):
  - Funktion `alleTermineFuerKunde(customer, liste, fromMillis, toMillis): List<TerminInfo>` die:
    - berechneAlleTermineFuerKunde nutzt,
    - kundenTermine und ausnahmeTermine filtert (im Bereich fromMillis–toMillis),
    - zu einer sortierten Liste zusammenführt.
  - Alle Stellen, die „nächstes Fälligkeitsdatum“ oder „fällig am Datum“ brauchen (TourDataFilter.customerFaelligAm, TourPlannerDateUtils.getFaelligAmDatum*, naechstesFaelligAmDatum), auf diese Quelle umstellen.

### 5.2 TourDataProcessor – Erledigte bei Vergangenheit

- Im Block für `tourListenErledigt`: Die Bedingung `if (!istVergangenheit)` so anpassen, dass auch bei Vergangenheit die erledigten Kunden aus Listen in tourListenErredigt übernommen werden (z. B. gleiche Logik wie für „Heute“, nur mit Erledigungsfilter für das viewDate).

### 5.3 Mehrere A-Tage

- In **TerminAusKundeUtils** (und ggf. TerminBerechnungUtils): Statt `effectiveAbholungWochentage.firstOrNull()` alle A-Wochentage in die Berechnung einbeziehen (z. B. für jedes A-Datum den nächsten Termin berechnen und in die 365-Tage-Liste aufnehmen).

### 5.4 Fehlerbehandlung

- **AppErrorMapper** erweitern und überall nutzen, wo Firebase-Fehler in User-Text umgesetzt werden.
- ViewModels: State als `sealed class LoadState<out T>` (Loading, Success(T), Error(message)); UI reagiert einheitlich (Loading-Spinner, Content, Error-Snackbar/Toast).

### 5.5 ListeIntervall-Fälligkeit

- In **TerminBerechnungUtils** (oder IntervallBerechnungUtils) eine Funktion `isListeIntervallFaelligAm(intervall, datum, liste): Boolean` implementieren.
- **TourDataFilter** und **TourPlannerDateUtils** diese Funktion nutzen statt eigener Duplikate.

---

## 6. Funktionen und Zukunftsplanung

### 6.1 Bereits im Manifest/Backlog (ohne Freigabe nicht umsetzen)

- Login-Feedback (Meldung + Retry)
- History-Log für Kunden (wer, wann, was)
- Optionale Features: Echte Karten-UI, Tour-Reihenfolge (Drag & Drop), Benachrichtigungen, Paging im Kundenmanager
- Benutzer und Rollen (Admin, Wäscherei, Fahrer) mit rollenbasierter Sicht/Bearbeitung
- Kalender-Export (iCal/CalDAV)
- Onboarding bei leerer App

### 6.2 Aus IDEEN_ZUKUNFT.md

- Benachrichtigungen: „Morgen X Kunden fällig“, Erinnerung pausierte Kunden
- Berichte & Export: Monatliche Berichte (PDF/CSV), Export Buchhaltung, Tour-Last-Anzeige
- Technik & UX: Paging/Lazy-Loading (>500 Kunden), Accessibility, Dark Mode
- Sonstiges: Vertragsende/Kündigungsdatum mit Auto-Pause, Bulk-Import (CSV/Excel), Adress-Validierung (Geocoding), Mehrsprachigkeit

### 6.3 Aus BERICHT_TIEFENANALYSE (Ideen)

- Echte Karten-Integration (In-App mit Markern/Route statt nur Maps-Intent)
- Tour-Reihenfolge speichern und an Karten-App übergeben
- Filter im Tourenplaner (Kundenart, Liste, Region/PLZ)
- Erledigungsquote pro Zeitraum (Woche/Monat)
- Mehrere Nutzer/Geräte (E-Mail/Google Auth, Berechtigungen)
- Auftrags-/Mengenverwaltung, Rechnungs-/Abrechnungsrelevanz, Kunden-Kommunikation (E-Mail/SMS), Check-in am Kunden (Zeiterfassung)
- Termin-Regeln erweitern (Feiertage, Schließtage, „jeden 2. Montag“)
- Listen-Vorlagen, Echtzeit-Sync-Status, Crashlytics/Analytics konsequent nutzen

### 6.4 Drucker/Belege (PROJECT_MANIFEST)

- Klebestreifen/Etiketten mit Kunden-Alias und Endpreis; ESC/POS + Bluetooth; 58 mm/40 mm; Library einbinden.

### 6.5 Priorisierung für die Zukunft (Empfehlung)

1. **Zuerst:** Offene Bugs und Termin-Quelle (Fälligkeit vollständig) – Stabilität.
2. **Dann:** Fehlerbehandlung, Strings, Deprecated-Abbau – Wartbarkeit.
3. **Danach:** Login-Feedback, App-Name, Barrierefreiheit – UX.
4. **Später:** Paging (>500 Kunden), Benachrichtigungen, Export, echte Karte – Features aus Backlog/IDEEN_ZUKUNFT.

---

## 7. Kurzfassung

- **Struktur:** Klar getrennte Pakete (ui/, data/repository, util/, tourplanner/, sevdesk/); Compose-Migration abgeschlossen; Coordinator+ViewModel für schwere Screens.
- **Stärken:** Zentrale Terminlogik, Koin, Offline-First, Tests für Kernlogik.
- **Schwächen:** Fälligkeit berücksichtigt nicht überall kundenTermine/ausnahmeTermine; zwei dokumentierte Bugs (mehrere A-Tage, Erledigte bei Vergangenheit); doppelte Intervall-Logik; uneinheitliche Fehlerbehandlung; vereinzelt hardcodierte Strings.
- **Verbesserungen:** Vollständige Termin-Quelle, Bug-Fixes (nach Freigabe), einheitliche Fehlerbehandlung, Strings auslagern, Intervall-Logik zusammenführen, Deprecated abbauen.
- **Zukunft:** Backlog und IDEEN_ZUKUNFT/ZUKUNFTSPLAENE.md nicht ohne Freigabe umsetzen; priorisiert: Stabilität → Wartbarkeit → UX → neue Features (Paging, Benachrichtigungen, Export, Karte, Rollen).

---

## 8. Beste Ideen (Zusammenfassung)

- **Schnelle Gewinne (ohne Verhalten zu ändern):** Architektur-Doku (TerminRegelRepository → Realität) anpassen; getFaelligAm() entfernen (keine Aufrufer); AgentDebugLog nur in Debug aktivieren.
- **Stabilität:** Eine „vollständige“ Termin-Quelle (inkl. kundenTermine + ausnahmeTermine) einführen und alle Fälligkeits-/Erledigungsstellen darauf umstellen; danach die zwei bekannten Bugs (mehrere A-Tage, Erledigte bei Vergangenheit) mit Freigabe beheben.
- **Wartbarkeit:** ListeIntervall-Fälligkeit an einer Stelle (TerminBerechnungUtils oder IntervallBerechnungUtils), TourDataFilter und TourPlannerDateUtils nutzen sie; AppErrorMapper-Texte in strings.xml auslagern; LoadState dort nutzen, wo noch Loading/Error ad hoc gehandhabt wird.
- **UX:** Barrierefreiheit systematisch prüfen (contentDescription, 48dp, Kontrast); optional regelNameByRegelId aus echten Termin-Regeln befüllen (falls Regeln irgendwo geladen werden), damit Kundendetail Regel-Namen anzeigen kann.
- **Zukunft (nur mit Freigabe):** Paging im Kundenmanager (>500 Kunden), Benachrichtigungen, Export/PDF, echte Karten-UI, Rollen (Admin/Wäscherei/Fahrer), Kalender-Export, Onboarding.

---

## 9. Profi-Bericht: Code-Zeilenanalyse (tiefe Auswertung)

*Grundlage: Durchsicht der App-Kotlin-Dateien (Modelle, Util, Data, Tourplanner, Activities, UI). Jeder relevante Bereich wurde zeilenorientiert ausgewertet.*

### 9.1 Erkenntnisse pro Bereich (Zeilen-/Datei-Analyse)

**Modelle (Customer.kt, KundenListe.kt, CustomerIntervall.kt, …)**  
- Customer: Viele Felder, klare Trennung neuer Struktur (intervalle, defaultAbholungWochentage) vs. deprecated (abholungDatum, auslieferungDatum, …). `displayName`, `effectiveAbholungWochentage`/`effectiveAuslieferungWochentage` korrekt @Exclude. Zeile 126: privates Feld `faelligAm` nur Dummy für Firebase – konsistent. Zeile 148–151: getFaelligAm() delegiert an TerminBerechnungUtils; keine anderen Aufrufer → Entfernung unkritisch.  
- KundenTermin und AusnahmeTermin: identische Struktur (datum, typ "A"/"L"); Vereinheitlichung zu einem Typ mit „quelle“ möglich, aber nicht zwingend.  
- KundenListe: wochentagA, tageAzuL, listenTermine gut dokumentiert; deprecated abholungWochentag/auslieferungWochentag noch vorhanden.

**Util (TerminBerechnungUtils, TerminFilterUtils, TerminAusKundeUtils, AppErrorMapper, Result, LoadState)**  
- TerminBerechnungUtils: getStartOfDay, berechneTermineFuerIntervall, berechneAlleTermineFuerKunde, hatTerminAmDatum, naechstesFaelligAmDatum, effectiveFaelligAmDatum – alles an einer Stelle. **Lücke:** berechneAlleTermineFuerKunde liefert nur Intervalle + Wochentag + listenTermine, **keine** kundenTermine/ausnahmeTermine. naechstesFaelligAmDatum (Zeile 424–458) nutzt nur diese Quelle → Kunden nur mit Einzel-/Ausnahme-Terminen bekommen 0L.  
- TerminAusKundeUtils Zeile 50: `effectiveAbholungWochentage.firstOrNull()` – nur ein A-Tag wird genutzt → **Bug „Mehrere A-Tage“**.  
- TerminFilterUtils: istTerminVerschoben, istTerminGeloescht, istUeberfaellig, getEffectiveUrlaubEintraege, istTerminInUrlaubEintraege – sauber getrennt.  
- AppErrorMapper: Alle Texte hardcodiert (z. B. „Ein Fehler ist aufgetreten“, „Verbindungsfehler…“). Sollte auf strings.xml (z. B. error_generic, error_connection) umgestellt werden, sofern Context/Resources verfügbar oder über Application.  
- LoadState: nur Definition, in der App kaum verwendet.

**Data/Repository (CustomerRepository, …)**  
- CustomerRepository: getAllCustomersFlow, getCustomersForTourFlow, getCustomerFlow, updateCustomerResult, deleteCustomerResult nutzen Result + AppErrorMapper. saveCustomer/updateCustomer/deleteCustomer liefern nur Boolean – zwei APIs parallel (Result vs. Boolean). Parsing auf Dispatchers.Default; awaitWithTimeout für Offline. tourRelevantKeys und withoutDeprecatedCustomerFields konsistent. addAusnahmeTermin, addKundenAbholungMitLieferung, removeKundenTermine vorhanden.  
- TerminCache: getTermine365 ruft berechneAlleTermineFuerKunde (ohne kundenTermine/ausnahmeTermine im Cache); getTerminePairs365 merged explizit kundenTermine + ausnahmeTermine – nur für Anzeige, nicht für Fälligkeit.

**Tourplanner (TourDataProcessor, TourDataFilter, TourPlannerDateUtils)**  
- TourDataProcessor Zeile 197–203: `istVergangenheit = viewDateStart < heuteStart`; Zeile 203: `if (!istVergangenheit)` um den gesamten Block, der tourListenErledigt befüllt. Bei Vergangenheit bleibt tourListenErledigt leer → **Bug „Erledigte bei Vergangenheit“**.  
- TourDataFilter.customerFaelligAm (Zeile 25–36): nutzt termincache.getTermineInRange (berechneAlleTermineFuerKunde), also **keine** kundenTermine/ausnahmeTermine. hatKundeTerminAmDatum (Zeile 40–70) prüft korrekt zuerst ausnahmeTermine, dann kundenTermine, dann Cache.  
- TourDataFilter.isIntervallFaelligAm (Zeile 119–164) und TourPlannerDateUtils.isIntervallFaelligAm (Zeile 68–88): **zwei verschiedene Implementierungen** für ListeIntervall (eine prüft A+L getrennt, eine nur „intervallStart == datumStart“ bei !wiederholen). TourDataProcessor Zeile 349–350 delegiert an filter – aber TourPlannerDateUtils wird woanders genutzt → Duplikat.

**Application / DI**  
- FirebaseConfig: AgentDebugLog.setLogFile im onCreate – Log-Datei wird in Release geschrieben. AppCompatDelegate.MODE_NIGHT_NO global – Dark Mode deaktiviert. Persistence und Koin korrekt.  
- AppModule: Alle Repositories und ViewModels gebunden; kein TerminRegelRepository; SevDeskImportViewModel mit Context-Parameter.

**Activities / Login**  
- LoginActivity: Fehlermeldung und Retry-/Weiter-Button vorhanden; State loginError, isLoading.  
- CustomerDetailActivity: regelNameByRegelId = emptyMap() an CustomerDetailScreen übergeben – Regel-Namen werden nirgends geladen.

### 9.2 Fehler (konkret)

| Nr. | Beschreibung | Datei / Stelle |
|-----|--------------|----------------|
| 1 | Mehrere A-Tage: Nur erster Wochentag wird für Intervall-Erstellung genutzt. | TerminAusKundeUtils.kt Zeile 50: `effectiveAbholungWochentage.firstOrNull()`. |
| 2 | Erledigte-Liste bei vergangenem Datum leer (Listen-Kunden fehlen im Erledigt-Sheet). | TourDataProcessor.kt Zeile 203: `if (!istVergangenheit)` um Befüllung von tourListenErledigt. |
| 3 | Fälligkeit ignoriert kundenTermine/ausnahmeTermine: customerFaelligAm, naechstesFaelligAmDatum, getFaelligAmDatum* nutzen nur berechneAlleTermineFuerKunde. | TourDataFilter.customerFaelligAm; TerminBerechnungUtils.naechstesFaelligAmDatum; TourPlannerDateUtils.getFaelligAmDatumFuerAbholung/Auslieferung. |
| 4 | Doppelte Implementierung isIntervallFaelligAm (ListeIntervall): unterschiedliche Semantik bei !wiederholen (A oder L einzeln vs. nur Start). | TourDataFilter.kt Zeile 119 ff.; TourPlannerDateUtils.kt Zeile 68 ff. |

### 9.3 Was man ändern muss

- **Termin-Quelle vereinheitlichen:** Eine Funktion (z. B. in TerminBerechnungUtils) die **alle** Termine liefert: berechneAlleTermineFuerKunde + kundenTermine + ausnahmeTermine im Zeitraum, sortiert. customerFaelligAm, naechstesFaelligAmDatum, getFaelligAmDatumFuerAbholung/Auslieferung und ggf. TerminCache darauf umstellen.  
- **Bugs 1 und 2:** Nach Freigabe: (1) In TerminAusKundeUtils alle effectiveAbholungWochentage in die Intervall-/Termin-Logik einbeziehen. (2) In TourDataProcessor die Erledigt-Logik für Listen auch bei istVergangenheit ausführen (tourListenErledigt befüllen).  
- **Doppelte Intervall-Logik:** Eine gemeinsame isListeIntervallFaelligAm (z. B. in TerminBerechnungUtils) implementieren; TourDataFilter und TourPlannerDateUtils darauf umstellen.  
- **AgentDebugLog:** Nur in Debug aktivieren (z. B. if (BuildConfig.DEBUG) in log() oder Aufrufer wrappen), damit in Release keine Log-Datei geschrieben wird.  
- **AppErrorMapper:** Texte in strings.xml auslagern und über Context/Application resolven (oder Mapper mit (Context) -> String), damit Lokalisierung und Konsistenz gewährleistet sind.

### 9.4 Vorschläge (ohne Muss)

- Customer.getFaelligAm() entfernen (keine Aufrufer); alle Stellen auf TerminBerechnungUtils.naechstesFaelligAmDatum/effectiveFaelligAmDatum verweisen.  
- Repository: Einheitlich nur Result<T> für update/delete (oder nur Boolean) – keine parallelen APIs.  
- LoadState in ViewModels nutzen, die bisher nur isLoading/errorMessage führen (z. B. Statistics, SevDeskImport).  
- ARCHITEKTUR_ACTIVITY_COORDINATOR.md: „TerminRegelRepository“ durch „TerminRegelManager / regelNameByRegelId (aktuell emptyMap)“ ersetzen.  
- KundenTermin und AusnahmeTermin: optional zu einem Typ „Einzeltermin(datum, typ, quelle: AUSNAHME | KUNDE)“ zusammenführen, um Merge-Logik und UI zu vereinfachen.

### 9.5 App für Zukunft verbessern

- **Performance:** Bei >500 Kunden Paging/Lazy-Loading im Kundenmanager; getCustomersForTourFlow bleibt, ggf. customers_for_tour Index weiter nutzen.  
- **Stabilität:** Vollständige Termin-Quelle (siehe 9.3); danach Bugs 1 und 2 beheben; Crashlytics konsequent für alle catch-Blöcke nutzen.  
- **Wartbarkeit:** Doppelte Logik entfernen; Strings zentral (strings.xml + AppErrorMapper); einheitlich Result/LoadState.  
- **UX:** Barrierefreiheit (contentDescription, 48dp, Kontrast); Dark Mode optional aktivierbar; regelNameByRegelId aus echten Regeldaten füllen, falls Termin-Regeln irgendwo geladen werden.  
- **Technik:** Kein runBlocking in UI-Thread; Coroutinen/Flow durchgängig; Tests für neue Termin-Quelle und für TourDataProcessor (Vergangenheit).

### 9.6 Neue Funktionen einbauen (Ideen)

- **Termin-Quelle „vollständig“:** Eine einzige API für „alle Termine inkl. Kunden-/Ausnahme-Termine“ (siehe 5.1), inkl. Nutzung in Fälligkeit und Erledigung.  
- **History-Log für Kunden:** Änderungen (wer, wann, was) pro Kunde protokollieren; optional in Firebase oder lokal.  
- **Benachrichtigungen:** WorkManager + Notification „Morgen X Kunden fällig“ / Erinnerung pausierte Kunden.  
- **Export/PDF:** Monatsbericht, Export für Buchhaltung, Tour-Last; ggf. Nutzung CustomerExportHelper erweitern.  
- **Echte Karten-UI:** In-App-Karte mit Markern/Route statt nur Maps-Intent; Filter „nur heute“.  
- **Tour-Reihenfolge:** Drag & Drop speichern (tourReihenfolge existiert), an Karten-App als Wegpunkte übergeben.  
- **Paging:** Kundenmanager und ggf. Listen mit Paging/Lazy-Loading für große Datenmengen.  
- **Rollen:** Admin / Wäscherei / Fahrer mit eingeschränkter Sicht/Bearbeitung (vgl. ZUKUNFTSPLAENE.md).  
- **Kalender-Export:** Termine in Gerätekalender (iCal/CalDAV).  
- **Drucker/Belege:** Klebestreifen mit Alias + Endpreis (ESC/POS, Bluetooth) wie in PROJECT_MANIFEST.md.

---

**Letzte Aktualisierung:** Feb 2026
