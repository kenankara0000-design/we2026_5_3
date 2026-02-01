# Tiefenanalyse-Bericht: TourPlaner 2026 (we2026_5)

**Erstellt:** 01.02.2026  
**Umfang:** Gesamte App – Code, Design, Funktionen, alter Code, Lücken, Verbesserungen

---

## 1. Wie die App funktioniert (Kurzüberblick)

### 1.1 Zweck

**TourPlaner 2026** ist eine Touren- und Kundenverwaltungs-App für ein Wäscherei-/Textildienstleistungs-Geschäft. Sie unterstützt:

- **Kundenstamm:** Gewerblich (G), Privat (P), Listen (L) mit Adresse, Telefon, Notizen, Fotos
- **Termin-Regeln:** Wiederverwendbare Regeln (wöchentlich, wochentagsbasiert etc.), anwendbar auf Kunden und Listen
- **Intervalle:** Pro Kunde/Liste mehrere Abhol-/Auslieferungs-Intervalle (neu); alte Einzelfelder sind deprecated
- **Tourenplaner:** Tagesansicht mit Überfällig → Heute → Listen → Erledigt; Erledigung (Abholung, Auslieferung, KW, Verschieben, Urlaub, Rückgängig)
- **Listen:** Kunden-Listen mit Intervallen, Kunden zuordnen/entfernen
- **Statistiken:** Heute/Woche/Monat fällig, Überfällig, Erledigt heute, Erledigungsquote, Gesamtkunden
- **Offline-First:** Firebase Realtime Database mit Persistence, Sync bei Wiederkehr des Netzes
- **Weitere:** Karte (Intent zu Google Maps), Fotos (Storage), anonyme Firebase Auth

### 1.2 Ablauf (User Journey)

1. **Login:** Anonyme Anmeldung → bei Erfolg MainActivity, bei Fehler Toast + `finish()` (kein Retry-Button).
2. **Hauptbildschirm (Compose):** Buttons: Neu Kunde, Kunden, Touren Planer (X fällig), Kunden Listen, Statistiken, Termin-Regeln; Offline/Sync-Status.
3. **Navigation:** Jeder Button startet eine eigene Activity (kein Single-Activity-Navigation mit Compose).
4. **Kunden:** CustomerManagerActivity (Compose, Tabs G/P/L, Suche) → Klick auf Kunde → CustomerDetailActivity (Compose, ViewModel, Fotos, Intervalle, Termin anlegen, Navigation, Anruf).
5. **Tourenplaner:** TourPlannerActivity (Compose), Datum wählen (Pfeile/Heute), Sektionen Überfällig/Listen/Normal/Erledigt; Kundenkarte → „Aktionen“ → Bottom-Sheet (Erledigung/Termin/Details).
6. **Listen/Statistiken/Termin-Regeln:** Jeweils eigene Activity mit Compose-UI und ViewModel.

### 1.3 Technischer Stack

- **UI:** Jetpack Compose (alle Haupt-Screens migriert), ViewBinding nur noch in wenigen Adapter/Item-Helpern
- **State:** ViewModels (Koin), StateFlow/LiveData, Flow aus Repositories
- **Daten:** Firebase Realtime Database, Firebase Storage (Fotos), Firebase Auth (anonym)
- **DI:** Koin (Repositories, ViewModels)
- **Sonst:** WorkManager (ImageUploadWorker), NetworkMonitor, Result/LoadState/AppErrorMapper

---

## 2. Code-Design und Architektur

### 2.1 Stärken

- **Klare Schichtung:** UI (Activity + Compose) → ViewModel → Repository → Firebase. Kein Repository in der UI (außer Weitergabe an Helper).
- **Einheitliche Fehlerbehandlung:** `Result<T>`, `LoadState<T>`, `AppErrorMapper`; Repositories liefern Result wo nötig, ViewModels setzen Fehlermeldungen, UI zeigt nur an.
- **Customer-ID konsistent:** Im `CustomerRepository` wird die ID aus dem Firebase-Key gesetzt (`copy(id = key)` / `copy(id = customerId)`), sodass Listen- und Detail-Ansicht dieselbe ID nutzen.
- **Terminlogik zentral:** `TerminBerechnungUtils`, `TerminFilterUtils`, `TourDataProcessor`, `TourDataCategorizer`, `TourDataFilter`; wenig Duplikat.
- **Erweiterbarkeit:** `CustomerRepositoryInterface` für Tests; Flow-API für reaktive UI.
- **Compose-Migration abgeschlossen:** Alle beschriebenen Screens laufen in Compose (laut COMPOSE_MIGRATION_PROTOKOLL).

### 2.2 Schwächen / technische Schulden

- **TourPlannerActivity noch „schwer“:** Viele Helper (TourPlannerDateUtils, TourPlannerDialogHelper, TourPlannerCallbackHandler, CustomerDialogHelper); Coordinator laut ARCHITEKTUR_ACTIVITY_COORDINATOR für TourPlanner „geplant“, nicht umgesetzt.
- **Doppelte State-Quellen im TourPlanner:** `expandedSections` (MutableSet) und `expandedSectionsFlow` (StateFlow) – beide werden gepflegt; funktioniert, ist aber redundant.
- **reloadCurrentView nach toggleSection:** Nach `viewModel.toggleSection()` wird `reloadCurrentView()` aufgerufen. Die UI würde sich bereits über `expandedSectionsFlow` und `tourItems` LiveData aktualisieren; der Aufruf ist redundant (nicht fehlerhaft).
- **CustomerAdapter-Path ungenutzt:** `CustomerAdapter`, `CustomerItemHelper`, `CustomerViewHolderBinder`, `TourPlannerCallbackHandler(adapter = …)` – im TourPlanner wird `adapter = null` übergeben, die gesamte RecyclerView/Adapter-Logik für den Tourenplaner ist durch Compose ersetzt. Die Klassen existieren noch (toter bzw. Legacy-Code).
- **ListItem/SectionType:** Definiert in `CustomerAdapter.kt` (sealed class ListItem, enum SectionType) – von TourDataProcessor und TourPlannerScreen (Compose) genutzt; enge Kopplung an eine Adapter-Datei.

---

## 3. Was fehlt oder nicht vollständig ist

### 3.1 Hardcodierte Texte (noch nicht in strings.xml)

| Ort | Hardcodierter Text | Empfehlung |
|-----|--------------------|------------|
| `CustomerDetailActivity.kt` | `"Kunde löschen?"`, `"Möchten Sie diesen Kunden wirklich löschen? …"`, `"Löschen"`, `"Abbrechen"` | In strings.xml auslagern (z. B. `dialog_delete_customer_*`, `dialog_loeschen`, `btn_cancel`). |
| `CustomerManagerActivity.kt` | `"Mehrere Kunden als erledigt markieren?"` | z. B. `dialog_mark_multiple_done_title`. |
| `KundenListenActivity.kt` | `"Liste löschen?"` | z. B. `dialog_delete_list_title`. |
| `CustomerDialogHelper.kt` | `"Wie möchten Sie vorgehen?"`, `"Möchten Sie die Erledigung wirklich rückgängig machen?"` | z. B. `dialog_erledigung_*`. |
| `TourPlannerDialogHelper.kt` | `"Möchten Sie diesen Termin wirklich löschen?"` | z. B. `dialog_delete_termin_confirm_message`. |

Hinweis: `dialog_loeschen`, `dialog_ok`, `btn_cancel` existieren teils schon in strings.xml; Dialog-Titel und -Meldungen oben fehlen oder sind uneinheitlich.

### 3.2 Login

- Bei Anmeldefehler: Toast + `finish()` – **kein Retry-Button**, kein sichtbares Layout (LoginActivity hat weder `setContentView` noch `setContent`).  
- Laut STATUS_TIEFENANALYSE ist „Login-Feedback“ bewusst im Zukunftsplan.

### 3.3 Fehlende oder optionale Funktionen (aus früheren Analysen)

- **Echte Karten-UI:** MapViewActivity ist Trampolin (Intent zu Google Maps, dann `finish()`); bei vielen Adressen Filter „nur heute fällig“ und MAX_WAYPOINTS bereits umgesetzt.
- **Tour-Reihenfolge / Drag & Drop** für Route: nicht vorhanden.
- **Benachrichtigungen** (z. B. „X Kunden morgen/heute fällig“): nicht umgesetzt.
- **Export/Backup:** CustomerExportHelper vorhanden; erweiterter Export (z. B. Termine, Listen) optional.
- **Paging** bei sehr vielen Kunden: nicht implementiert.

---

## 4. Funktionen – was funktioniert, was geprüft werden sollte

### 4.1 Als funktionierend angenommene Bereiche (aus Code-Analyse)

- **Hauptbildschirm:** Tour-Fällig-Zähler (`MainViewModel` + `TourDataProcessor.getFälligCount`), Offline/Sync, Navigation zu allen Activities.
- **Kundenmanager:** Tabs (G/P/L), Suche, Flow-basierte Liste; G/P/L-Farben (Privat = Orange, Liste = Braun) in `CustomerManagerScreen` umgesetzt.
- **Kundendetail:** ViewModel, Flow, Speichern/Löschen, Fotos (CustomerPhotoManager), Intervalle, Termin-Regeln, Navigation, Anruf; Lösch-Dialog mit hardcodierten Texten.
- **Tourenplaner:** Datum (selectedTimestamp), Sektionen (Überfällig, Listen, Normal, Erledigt); **Erledigt-Bereich ausklappbar** – `TourDataProcessor.processTourData` nutzt `expandedSections` und fügt bei `SectionType.DONE in expandedSections` die Kunden als `ListItem.CustomerItem` ein. **Heute-Button:** Farbe abhängig von `isToday` (orange wenn angezeigtes Datum heute).
- **Statistiken, Listen, Termin-Regeln, MapView, AddCustomer, ListeErstellen, ListeBearbeiten:** Compose + ViewModel; grundsätzlich konsistent aufgebaut.

### 4.2 Punkte, die man manuell prüfen sollte

- **„Kunde nicht mehr vorhanden“:** Wenn die Meldung trotz gültiger ID aus der Liste erscheint, kann das an Timing/Offline (Firebase noch nicht geladen) oder an einer leeren ID in der Liste liegen. Code-seitig ist die ID-Übernahme aus dem Firebase-Key im Repository korrekt; bei flackernder Meldung: Verzögerung/Retry oder bessere UX (z. B. „Kunde wird geladen…“).
- **Rücknavigation nach „Neuer Kunde“ speichern:** Von Hauptbildschirm → Speichern → zurück zum Hauptbildschirm; von Kundenmanager → Speichern → zurück zum Kundenmanager. Gewollt, aber uneinheitlich aus Nutzersicht (laut ANALYSE_9_PUNKTE).
- **Dark Theme / Hintergrund:** Werte in values-night vorhanden; ob alle Compose-Screens (Main, AddCustomer, CustomerManager etc.) überall einen expliziten hellen Hintergrund setzen, sollte auf dem Gerät geprüft werden, um „schwarzen“ Hintergrund zu vermeiden.

---

## 5. Alter Code und Rückwärtskompatibilität

### 5.1 Deprecated Felder (bewusst beibehalten)

- **Customer:** `abholungDatum`, `auslieferungDatum`, `wiederholen`, `intervallTage`, `letzterTermin`, `wochentagOld`, `verschobenAufDatum` – alle mit Hinweis „Verwende intervalle / verschobeneTermine“.  
- **Customer.getFaelligAm():** deprecated, Nutzung von `TerminBerechnungUtils.berechneAlleTermineFuerKunde()` empfohlen.  
- **KundenListe:** deprecated Felder `abholungWochentag`, `auslieferungWochentag`, `wiederholen` (Intervalle-Struktur ist neu).

Diese Felder werden teils noch von Firebase gelesen/geschrieben und für Migration beibehalten; neue Logik baut auf `intervalle` und `verschobeneTermine` auf.

### 5.2 Ungenutzter / Legacy-Code

- **CustomerAdapter**, **CustomerItemHelper**, **CustomerViewHolderBinder:** Werden nur noch indirekt referenziert; TourPlanner nutzt sie nicht (`adapter = null`). Die Tourenplaner-Liste ist vollständig Compose (`TourPlannerScreen` + `ListItem`). Die XML-Layouts `item_customer.xml`, `item_section_header.xml` werden von diesem Adapter-Pfad verwendet – könnten entfernt werden, wenn der Adapter-Pfad komplett obsolet ist (oder für andere zukünftige RecyclerViews behalten).
- **TourPlannerCallbackHandler:** Bekommt `adapter: CustomerAdapter? = null`; alle Callbacks (Abholung, Auslieferung, Verschieben, …) werden von der Activity/Compose-Seite angestoßen, der Handler enthält die Geschäftslogik. Der Parameter `adapter` ist ungenutzt.
- **activity_login.xml:** LoginActivity setzt kein Layout (`setContentView`/`setContent` fehlen); wenn dieses Layout nirgends referenziert wird, ist es Restbestand.

### 5.3 Weitere XML-Layouts (noch in Verwendung)

- **item_section_header.xml**, **item_customer.xml:** Für CustomerAdapter/ItemHelper (der im Tourenplaner nicht mehr genutzt wird).
- **dialog_fullscreen_image.xml**, **dialog_termin_detail.xml:** Können von Dialogen (z. B. Foto-Vollbild, Termin-Detail) noch verwendet werden.
- **item_week_day.xml**, **item_intervall.xml**, **item_kunde_liste.xml**, **item_photo.xml:** Für Listen/Detail-Ansichten (z. B. Intervalle, Fotos) – prüfen, ob diese Teile bereits in Compose migriert sind oder noch mit RecyclerView laufen.

---

## 6. Verbesserungsvorschläge (priorisiert)

### 6.1 Hohe Priorität (Wartbarkeit, Konsistenz)

1. **Hardcodierte Dialog-Texte auslagern:** Alle in Abschnitt 3.1 genannten Texte in strings.xml überführen und in den Activities/Helfern `getString(...)` nutzen.
2. **TourPlannerCoordinator einführen:** Wie in ARCHITEKTUR_ACTIVITY_COORDINATOR beschrieben – Helper (DateUtils, DialogHelper, CallbackHandler, CustomerDialogHelper) in einen Coordinator auslagern, Activity nur noch Binding und Aufrufe.
3. **ListItem/SectionType auslagern:** Aus `CustomerAdapter.kt` in eine eigene Datei (z. B. `ListItem.kt` / `TourModels.kt`) verschieben, da sie von TourDataProcessor und Compose-UI genutzt werden und nicht zur Adapter-Implementierung gehören.

### 6.2 Mittlere Priorität (Klärung, Aufräumen)

4. **CustomerAdapter-Pfad klären:** Entweder entfernen (inkl. ItemHelper, ViewHolderBinder, zugehörige XML-Items), wenn nirgends mehr genutzt, oder explizit als „nur für Tests / zukünftige Verwendung“ dokumentieren und `adapter`-Parameter in TourPlannerCallbackHandler entfernen oder durch ein kleines Interface ersetzen.
5. **reloadCurrentView nach toggleSection:** Entfernen (ViewModel/Flow aktualisieren die Liste bereits).
6. **expandedSections vereinheitlichen:** Nur eine Quelle (z. B. nur `expandedSectionsFlow`); das lokale `expandedSections`-Set aus dem ViewModel entfernen und alle Zugriffe über den Flow abwickeln.

### 6.3 Niedrige Priorität (UX, Zukunftsplan)

7. **Login:** Retry-Button und ggf. minimales Layout bei Fehler (bereits in ZUKUNFTSPLAENE.md).
8. **Rücknavigation nach „Neuer Kunde“:** Einheitlich „immer Hauptbildschirm“ oder expliziter Parameter (bereits in ANALYSE_9_PUNKTE beschrieben).
9. **Barrierefreiheit:** Systematische Prüfung TalkBack, Kontrast, 48dp-Touch-Targets (laut STATUS nur teilweise umgesetzt).

---

## 7. Zusammenfassung

| Bereich | Befund |
|--------|--------|
| **Funktionalität** | Tourenplaner, Kunden, Listen, Termin-Regeln, Statistiken, MapView, Offline/Sync und Fotos sind im Code konsistent angebunden. Erledigt-Bereich klappt aus; Heute-Button ist datumsabhängig orange. |
| **Design/Architektur** | ViewModel/Repository/Flow durchgängig; Fehlerbehandlung mit Result/AppErrorMapper. TourPlanner noch mit vielen Helfern in der Activity; Coordinator geplant, nicht umgesetzt. |
| **Fehlendes** | Hardcodierte Dialog-Texte an mehreren Stellen; Login ohne Retry; optional Karten-UI, Benachrichtigungen, Paging. |
| **Alter Code** | Deprecated Felder in Customer/KundenListe bewusst für Migration; CustomerAdapter/ItemHelper/ViewHolderBinder im Tourenplaner ungenutzt (adapter = null). |
| **Verbesserungen** | Strings auslagern, TourPlannerCoordinator, ListItem/SectionType auslagern, Adapter-Pfad aufräumen oder entfernen, kleine ViewModel-Optimierungen (reloadCurrentView, expandedSections). |

Dieser Bericht kann als Grundlage für Refactoring-Tickets und die Priorisierung der nächsten Schritte dienen.

---

*Ende des Berichts. Kein Code geändert.*
