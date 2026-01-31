# Tiefenanalyse: TourPlaner 2026

**Umfang:** Gesamte App – Code, Design, Domain, Erweiterungspotenzial  
**Stand:** Januar 2026  
**Auftrag:** Sehr gründliche Analyse ohne Code-Änderungen, nur Bericht.

---

## 1. Was die App ist und was sie soll

### 1.1 Zweck und Zielgruppe

Die App **TourPlaner 2026** (intern we2026_5) ist eine **Touren- und Kundenverwaltungs-App für ein Wäscherei-/Textildienstleistungs-Geschäft**. Sie unterstützt:

- **Abholung** und **Auslieferung** von Wäsche bei Kunden
- Planung und Abarbeitung von **Touren** (tagesbezogen: wer ist heute dran, wer überfällig, wer erledigt)
- **Kundenstamm** (Gewerblich, Privat, Listen) mit Adressen, Telefon, Notizen, Fotos
- **Termin-Regeln** (wöchentlich, 2-wöchentlich, wochentagsbasiert usw.) zur Erzeugung von Abhol-/Ausliefer-Terminen
- **Listen** (z. B. „Borna P“, „Kitzscher P“) mit mehreren Intervallen und zugeordneten Privat-Kunden
- **Statistiken** (heute/Woche/Monat fällig, überfällig, erledigt heute, Erledigungsquote, Gesamtkunden)
- **Offline-Fähigkeit** mit Firebase Realtime Database (Persistence) und Synchronisation bei Wiederkehr des Netzes
- **Navigation** zu Kundenadressen über Google Maps
- **Fotos** (Abhol-/Auslieferungsort) mit Thumbnails und Vergrößerung

**Zielgruppe:** Fahrer/Disponenten und Büro im Wäschereibetrieb, die Touren planen, Abholung/Auslieferung erfassen und den Kundenstamm pflegen.

### 1.2 Kernfunktionen im Überblick

| Bereich | Funktionen |
|--------|------------|
| **Start** | Login (anonym), Hauptmenü: Neu Kunde, Kunden, Touren Planer (mit Fälligkeitsanzahl), Kunden Listen, Statistiken, Termin-Regeln; Offline-/Sync-Status |
| **Kunden** | Anlegen (G/P/L), Manager mit Tabs (Gewerblich/Privat/Liste), Suche (Debounce), Klick → Detail; Detail: Übersicht, Bearbeiten, Löschen (nur im Bearbeitungsmodus mit Bestätigung), Fotos, Intervalle, Termin-Regeln anwenden, Navigation, Anruf |
| **Touren** | Tagesansicht (optional Woche/Karte); Datum wechseln (Pfeile/Swipe); Sektionen: Überfällig, Heute, Erledigt, Listen; Kundenkarte mit Status-Badge und „Aktionen“ → Bottom-Sheet (Erledigung, Termin, Details); Abholung/Auslieferung/KW/Verschieben/Urlaub/Rückgängig |
| **Listen** | Listen verwalten, erstellen, bearbeiten; Listen-Intervalle; Kunden zu Listen zuordnen |
| **Termin-Regeln** | Regeln anlegen/bearbeiten (Name, Wochentag oder Datum, Intervall); auf Kunden/Listen anwenden → Intervalle erzeugen |
| **Statistiken** | Heute fällig, Diese Woche, Dieser Monat, Überfällig, Erledigt heute, Erledigungsquote heute, Gesamt Kunden |
| **Karte** | MapViewActivity öffnet Google Maps mit allen Kundenadressen als Wegpunkte (Intent) |

---

## 2. Code-Analyse

### 2.1 Architektur – Gesamtbild

- **UI:** Überwiegend **Activities** (kein Jetpack Compose), **ViewBinding**, vereinzelt **Fragments** (z. B. ErledigungBottomSheetDialogFragment).
- **State/Logik:** Teilweise **ViewModels** (Main, Statistics, CustomerManager, TourPlanner, TerminRegelErstellen), teilweise **Repository + Listener/Coroutinen** direkt in der Activity (CustomerDetail, KundenListen, MapView, AddCustomer).
- **Daten:** **Firebase Realtime Database** als Hauptspeicher; **Firebase Storage** für Fotos; **Firebase Auth** (anonym). **Koin** für DI; Repositories als Single/ViewModel über Koin.
- **Domain:** Klare Datenmodelle (Customer, KundenListe, TerminRegel, CustomerIntervall, ListeIntervall, VerschobenerTermin, TerminTyp). Terminlogik zentral in **TerminBerechnungUtils** und **TerminFilterUtils**; Tourenlogik in **TourDataProcessor**, **TourDataFilter**, **TourDataCategorizer**, **TourPlannerDateUtils**.

**Bewertung:** Die App ist **halbwegs schichtorientiert** (UI → ViewModel/Repository → Firebase), aber **inkonsistent**: Manche Screens haben ViewModel und Flow, andere direkten Repository-Zugriff und Callbacks. Das erschwert einheitliche Tests und Wartung.

### 2.2 Stärken im Code

- **Zentrale Terminlogik:** `TerminBerechnungUtils` (getStartOfDay, berechneAlleTermineFuerKunde, hatTerminAmDatum usw.) wird an vielen Stellen genutzt; Duplikate wurden reduziert (TourPlannerDateUtils, TourDataCategorizer delegieren).
- **Repository-Interface:** `CustomerRepositoryInterface` mit `FakeCustomerRepository` für Tests; Flow-API (`getAllCustomersFlow`) für reaktive UI.
- **Offline-First:** Realtime Database mit Persistence; NetworkMonitor und FirebaseSyncManager für Anzeige von Offline/Sync-Status; Speichern mit Timeout-Handling bei Offline.
- **Koin:** Einheitliche DI (AppModule); ViewModels und Repositories zentral gebunden.
- **Deprecated-Markierungen:** Alte Felder (Customer: abholungDatum, auslieferungDatum usw.) und alte Listener-API sind als deprecated gekennzeichnet; Migration zur Intervall-/Flow-Struktur ist nachvollziehbar.
- **Kein runBlocking** in kritischen Pfaden (laut vorheriger Refactoring-Arbeit); Coroutinen und ViewModelScope werden genutzt.
- **Vorhandene Tests:** Unit-Tests für TerminBerechnungUtils, TourDataProcessor, CustomerRepository (inkl. Fake), CustomerManagerViewModel, ValidationHelper.

### 2.3 Schwächen und technische Schulden

- **Schwere Activities:** TourPlannerActivity und CustomerDetailActivity koordinieren viele Helper (TourPlannerUISetup, TourPlannerCallbackHandler, TourPlannerDateUtils, TourPlannerDialogHelper, TourPlannerGestureHandler; CustomerDetailUISetup, CustomerDetailCallbacks, CustomerEditManager, CustomerPhotoManager). Die Verantwortung liegt stark in der Activity; Fehlersuche und Erweiterung sind aufwendig.
- **Doppelte State-Quellen:** Im TourPlanner wurde das Datum ins ViewModel verschoben (selectedTimestamp), aber vereinzelt könnte noch Kalender- oder View-State in der Activity existieren; generell gilt: Wo ViewModel + Activity beide State halten, besteht Drift-Risiko.
- **Listener vs. Flow:** CustomerDetailActivity und ggf. andere nutzen noch die als deprecated markierten Listener (addCustomerListener); vollständige Umstellung auf Flow/ViewModel würde die Architektur vereinheitlichen.
- **CustomerAdapter:** Sehr viele Callbacks (onAbholung, onAuslieferung, onKw, onVerschieben, onUrlaub, onRueckgaengig, getStartOfDay, getTermineFuerKunde, …). Ein einziges Callback-Interface oder eine Datenklasse würde Lesbarkeit und Testbarkeit erhöhen.
- **Fehlerbehandlung:** Kein durchgängiges Konzept (z. B. LoadState/Result oder sealed class). Fehler werden teils als Toast, teils als Log, teils in der UI (errorLayout) angezeigt; keine zentrale „Firebase-Fehler → User-Text“-Zuordnung.
- **Hardcodierte Texte:** Einige wenige Texte stehen noch im Layout oder im Code (z. B. „TourPlaner 2026“, „Name fehlt“, „Speichere...“, „Offline“, „Synchronisiere...“, Statistiken-Labels wie „Heute fällig“). Vollständige Auslagerung in strings.xml fehlt für Lokalisierung und Konsistenz.
- **MapViewActivity:** Lädt alle Kunden, filtert nach Adresse, baut eine Google-Maps-URL und beendet sich dann (startActivity + finish). Es gibt keine „echte“ Karten-UI in der App; die Aktivität dient nur als Trampolin. Bei vielen Adressen kann die URL-Größe problematisch werden.
-

### 2.4 Abhängigkeiten und Build

- **Kotlin, Android SDK 34, minSdk 24;** Java 17.  
- **Wichtige Libs:** AndroidX (Core, AppCompat, Material, ConstraintLayout, CardView, SwipeRefresh, Lifecycle, Activity, Fragment), Kotlin Coroutines, Glide, Firebase (BOM: Auth, Database, Storage, Firestore, Crashlytics), WorkManager, Koin.  
- **Tests:** JUnit, Mockito, Kotlin Coroutines Test, Architecture Core Testing, Espresso.  
- Kein Compose; kein Room (alles Firebase).

---

## 3. Design-Analyse

### 3.1 Konsistenz und Design-System

- **Design-Manifest vorhanden:** DESIGN_MANIFEST.md beschreibt Material Design 3, Farbsystem (Primär Blau, Status Rot/Grün/Orange), Typografie, Komponenten. Das gibt eine klare Richtung.
- **Themes:** Theme.We2026_5 (Material3.DayNight.NoActionBar); values-night mit dunklem Hintergrund und angepassten Primärfarben. Farben und Flächen sind in colors.xml und themes.xml definiert.
- **Komponenten:** Buttons (glossy für A/L/KW/V/U, Material-Buttons für Aktionen), CardViews für Kunden und Statistiken, TabLayouts (Touren-Bottom-Sheet, CustomerManager), Bottom-Sheet mit Tabs (Erledigung, Termin, Details). Status-Badges (overdue, done, today, verschoben) und Section-Header (overdue, done) sind einheitlich verwendbar.
- **Hauptbildschirm:** Klare Hierarchie – drei große Haupt-Buttons (Neu Kunde, Kunden, Touren), darunter „Weitere“ mit Listen, Statistiken, Termin-Regeln. Tour-Button zeigt dynamisch „Tour Planner (n fällig)“.

### 3.2 UX-Stärken

- Tourenplaner: Überfällige oben, dann Heute, dann Erledigt; Status-Badge auf der Karte; ein „Aktionen“-Button führt in ein Sheet statt vieler Einzelbuttons.
- Erledigung: Semantische Farben (Abholung Blau, Auslieferung Grün, KW Orange, Rückgängig Rot); Buttons nur sichtbar/aktiv wenn kontextuell sinnvoll (CustomerButtonVisibilityHelper, ErledigungSheetState).
- Suche im Kundenmanager mit Debounce; SwipeRefresh auf Listen-Seiten; Zurück-Buttons auf allen Screens.
- Offline- und Sync-Hinweise auf der MainActivity; klarer Hinweis „Änderungen werden beim nächsten Online-Zustand synchronisiert“.

### 3.3 Design-Schwächen und Verbesserungspotenzial

- **App-Name:** Im Hauptbildschirm steht „TourPlaner 2026“, im Manifest/Label „we2026_5“ (app_name). Für Nutzer sollte überall ein einheitlicher, verständlicher Name erscheinen.
- **Statistik-Labels:** Texte wie „Heute fällig“, „Diese Woche“, „Überfällig“ usw. sind teils hardcodiert im Layout; für Mehrsprachigkeit und einheitliche Begriffe sollten sie in strings.xml.
- **Statistik-Zurück-Button:** Verwendet `ic_menu_revert` statt des sonst genutzten ic_arrow_back; optische Inkonsistenz.
- **Barrierefreiheit:** contentDescription für wichtige Buttons vorhanden; systematische Prüfung von TalkBack, Kontrast (WCAG) und Touch-Target-Größen ist nicht dokumentiert. Einige Views haben keine oder generische Descriptions.
- **Kundenkarte:** Sehr viele Informationen und Buttons (Typ, Name, Adresse, Status-Badge, Aktionen, Nächste Tour, Navigation). Auf kleinen Screens kann das gedrängt wirken; optional könnte Telefon nur im Sheet gezeigt werden, um die Karte zu entlasten.
- **Dark Theme:** values-night ist angelegt, aber nicht alle Screens/Layouts nutzen zwangsläufig die definierten Farben (z. B. manche Hintergründe oder Texte könnten noch angepasst werden).

### 3.4 Layout-Struktur

- Durchgängig XML-Layouts (keine Compose); LinearLayout und RelativeLayout häufig, ConstraintLayout vereinzelt. RecyclerViews für Kunden, Listen, Intervalle, Termin-Regeln, Fotos.  
- item_customer.xml ist detaillreich (G/P/L-Button, Name, Adresse, Status-Badge, Aktionen, Legacy-Button-Bereich ausgeblendet, Nächste Tour, Navigation). Section-Header (item_section_header.xml) für Überfällig/Erledigt.

---

## 4. Was man noch bauen kann (Ideen)

### 4.1 Direkt aus dem bestehenden Kontext

- **Echte Karten-Integration:** Statt nur Intent zu Google Maps: In-App-Karte (z. B. Google Maps SDK oder OSM) mit Markern pro Kunde, Route für den Tag, Filter nach „heute fällig“. MapViewActivity könnte zu einer echten Kartenansicht werden.
- **Tour-Reihenfolge:** Im Tourenplaner eine manuelle Reihenfolge (Drag & Drop) für die Route speichern und an die Karten-App übergeben (Wegpunkte in Reihenfolge).
- **Benachrichtigungen:** Am Vortag oder am Morgen Erinnerung „X Kunden morgen/heute fällig“ (WorkManager + Notification).
- **Export/Backup:** Kundenliste oder Touren-Daten als CSV/Excel exportieren (CustomerExportHelper existiert bereits im Kontext CustomerManager); erweiterbar um Termine und Listen.
- **Erledigungsquote pro Zeitraum:** In Statistiken nicht nur „Erledigungsquote heute“, sondern z. B. „Diese Woche“ oder „Dieser Monat“.
- **Filter im Tourenplaner:** Nach Kundenart (G/P/L), nach Liste oder nach Region/PLZ filtern.
- **Mehrere Nutzer/Geräte:** Firebase Auth mit E-Mail/Google statt nur anonym; optional Berechtigungen (nur lesen vs. erfassen) oder Geräte-/Nutzer-spezifische Sicht.

### 4.2 Erweiterung der Domain

- **Auftrags-/Mengenverwaltung:** Pro Termin erfasste Mengen (Säcke, Kilo) oder einfache Checkliste (Artikel A/B/C); Auswertung in Statistiken.
- **Rechnungs-/Abrechnungsrelevanz:** Markierung „abgerechnet“ oder Verknüpfung zu Belegen; Export für Buchhaltung.
- **Kunden-Kommunikation:** E-Mail/SMS-Erinnerung an Kunden vor Abholung (optional mit Opt-in).
- **Check-in am Kunden:** Beim Erledigen optional „Angekommen“-Zeitpunkt und „Fertig“-Zeitpunkt für einfache Zeiterfassung pro Tour.
- **Termin-Regeln erweitern:** Ausnahmen (Feiertage, Schließtage), „jeden 2. Montag im Monat“ o. Ä.
- **Listen-Vorlagen:** Vorgefertigte Regeln für typische Listen (z. B. „Wöchentlich Mo/Do“) mit einem Klick anwenden.

### 4.3 Technik und Qualität

- **Compose-Migration (schrittweise):** Neue Screens oder Teile (z. B. Statistiken, Einstellungen) in Jetpack Compose; langfristig weniger XML und einheitliches UI-Toolkit.
- **Echtzeit-Sync-Status:** Anzeige „X Änderungen ausstehend“ oder Fortschrittsbalken bei großer Sync-Queue.
- **Paging:** Bei sehr vielen Kunden (z. B. > 500) im Kundenmanager oder in Listen Paging/Lazy-Loading für bessere Performance und weniger Speicher.
- **Crashlytics/Analytics:** Crashlytics ist eingebunden; konsequente Nutzung für Abstürze und optional Analytics für Nutzung (welche Screens, welche Aktionen) für Stabilität und UX-Optimierung.

---

## 5. Was man verbessern kann (priorisierte Empfehlungen)

### 5.1 Hohe Priorität (Stabilität, Wartbarkeit)

- **CustomerDetailActivity auf ViewModel umstellen:** Kunden-State und Speichern/Löschen in ein CustomerDetailViewModel; Activity nur UI und Launcher. Reduziert Komplexität und ermöglicht bessere Tests.
- **Einheitliche Fehlerbehandlung:** Gemeinsames Konzept (z. B. LoadState mit Loading/Success/Error oder Result<T>); Repository gibt Fehler zurück, ViewModel mappt auf UI (z. B. errorMessage); Activity zeigt nur an. Optional zentrale Mapper von Firebase-Fehlern zu strings.xml.
- **Restliche hardcodierte Strings auslagern:** Alle sichtbaren Texte (inkl. „TourPlaner 2026“, „Name fehlt“, „Speichere...“, Offline/Sync, Statistik-Labels) in strings.xml; contentDescription wo noch fehlend.
- **Listener-API vollständig ablösen:** Wo noch addCustomerListener/addCustomersListener genutzt wird, auf Flow + ViewModel umstellen; danach deprecated Listener entfernen.

### 5.2 Mittlere Priorität (UX, Konsistenz)

- **Login-Feedback:** Bei Fehler der anonymen Anmeldung kurze Meldung anzeigen (z. B. Snackbar/Toast) und Retry-Button statt sofort finish().
- **App-Name vereinheitlichen:** Ein Name (z. B. „TourPlaner 2026“) in Manifest und auf dem Hauptbildschirm; „we2026_5“ nur intern.
- **Statistik-Zurück-Button:** Gleiches Icon wie auf anderen Screens (ic_arrow_back) und gleicher Stil.
- **CustomerAdapter-Callbacks bündeln:** Ein Interface oder eine Datenklasse für alle Callbacks; Adapter und Tourenplaner-Code werden lesbarer und testbarer.
- **Barrierefreiheit prüfen:** Kontraste (primär/secondary, Status-Farben), Mindest-Touch-Targets (48dp), TalkBack durch alle wichtigen Flows; fehlende contentDescriptions ergänzen.

### 5.3 Niedrige Priorität (Nice-to-have)

- **TourPlannerActivity entlasten:** Weitere Logik aus Helpern in ViewModel oder Use-Cases verschieben; Activity nur noch Koordination und UI-Binding.
- **Dark Theme durchgängig prüfen:** Alle Screens in Night-Modus testen und Farben/Hintergründe anpassen.
- **MapView bei vielen Adressen:** Bei sehr vielen Wegpunkten alternative Darstellung (z. B. nur „Heute“-Kunden) oder Aufteilung in mehrere Karten-Intents.

---

## 6. Zusammenfassung

Die App **TourPlaner 2026** erfüllt ihren Zweck als **Touren- und Kundenverwaltung für einen Wäschereibetrieb** mit Abholung, Auslieferung, Termin-Regeln, Listen, Statistiken, Offline-Fähigkeit, Fotos und Maps-Navigation. Die **Domain-Modelle** sind klar, die **Terminlogik** ist weitgehend zentralisiert, **ViewModels** und **Repository-Interface** mit Fakes sind vorhanden und **Tests** decken wichtige Teile ab.

**Schwächen** liegen in der **inkonsistenten Architektur** (teils ViewModel/Flow, teils Activity + Listener), den **sehr großen Activities** (TourPlanner, CustomerDetail) mit vielen Helfern, vereinzelten **hardcodierten Texten**, fehlender **zentraler Fehlerbehandlung** und der **trampolinartigen MapView**. **Design** und **UX** sind durch das Design-Manifest und die bestehenden Komponenten gut geführt; Verbesserungen betreffen vor allem **einheitlichen App-Namen**, **vollständige String-Auslagerung**, **Barrierefreiheit** und **Login-Feedback**.

**Mögliche nächste Schritte:** Zuerst Stabilität und Wartbarkeit (ViewModel für CustomerDetail, Fehlerbehandlung, Strings), dann UX und Konsistenz (Login, App-Name, Icons, Callbacks bündeln), danach neue Features (Benachrichtigungen, Export, echte Karte, erweiterte Statistiken). Die vorliegende Tiefenanalyse kann als Grundlage für eine priorisierte Roadmap und für konkrete Refactoring- und Feature-Tickets dienen.

---

*Ende des Berichts. Kein Code geändert; nur Analyse und Empfehlungen.*
