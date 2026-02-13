# Referenz: Analysen und aktueller Stand

**Zweck:** Übersicht über zentrale Aussagen und den Stand aus den Analyse- und Plan-Dateien (ohne Bugs, ohne Zukunftsplanung). Dient als schnelle Referenz für Architektur, Migrationen und technische Meilensteine.

*Quellen: COMPOSE_MIGRATION_PROTOKOLL, ARCHITEKTUR_ACTIVITY_COORDINATOR, PLAN_PERFORMANCE_OFFLINE, PLAN_TOURPLANNER_PERFORMANCE_3TAGE, PLAN_UMSETZUNG, GESAMTANALYSE_APP_2026_02, BERICHT_APP_ZUSTAND_2026_02, BERICHT_TIEFENANALYSE_APP_2026, WASCHLISTE_OCR_OPTIONEN.*

---

## Compose-Migration

- **Strategie:** Neu bauen nur in Compose; bestehende Screens schrittweise migriert.
- **Stand:** Phase 0–4 abgeschlossen. Alle genannten Screens (Statistics, ListeErstellen, TerminRegel*, MapView, KundenListen, ListeBearbeiten, AddCustomer, MainActivity, CustomerManager, CustomerDetail, TourPlanner, Erledigung-BottomSheet) in Compose. Keine weiteren Screens zur Migration offen (LoginActivity ohne sichtbare UI).
- **Quelle:** COMPOSE_MIGRATION_PROTOKOLL.md.

---

## Architektur (schwere Activities)

- **Regel:** Activity nur Binding, ViewModel beobachten, Coordinator/Intents; keine Geschäftslogik in der Activity.
- **CustomerDetail:** CustomerDetailViewModel (State, load/save/delete), CustomerDetailCoordinator (UISetup, Callbacks, EditManager, PhotoManager), Activity schlank.
- **TourPlanner:** TourPlannerCoordinator (DateUtils, DialogHelper, CallbackHandler, Sheet), TourPlannerViewModel (selectedTimestamp, tourItems, loadTourData), Activity nur setContent und Observer.
- **Hinweis:** ARCHITEKTUR nennt „TerminRegelRepository“ für Coordinator; im Code gibt es TerminRegelManager (object), kein TerminRegelRepository in Koin. CustomerDetailActivity übergibt regelNameByRegelId = emptyMap().
- **Quelle:** ARCHITEKTUR_ACTIVITY_COORDINATOR.md.

---

## Performance & Offline

- **Prio 1:** Keine Doppelladung, Listener nur bei Bedarf, Firebase Offline aktiv, Offline-/Sync-Hinweis in der UI → alle erledigt (Feb 2026).
- **Prio 2:** Paging im Kundenmanager, Suche/Filter mit Paging → offen (bei Bedarf vor ~500 Kunden).
- **Prio 3:** Fotos als Thumbnails → erledigt.
- **Prio 4:** TourPlanner-Berechnung im Hintergrund, Firebase-Persistence → dauerhaft abgesichert.
- **Prio 5:** Dateigröße/Aufteilung großer Screens → offen (auf Nutzer-Bestätigung warten).
- **Quelle:** PLAN_PERFORMANCE_OFFLINE.md.

---

## TourPlanner Performance (3-Tage-Fenster)

- **Ziel:** Start auf Heute, nur Daten für heute±1 (bzw. 3-Tage-Fenster) + Überfällig (z. B. 60 Tage); flüssiger Tagwechsel.
- **Stand:** Phasen 1–4 erledigt (Start auf Heute, TourDataProcessor/TourDataFilter/TourListenProcessorImpl/TourPlannerStatusBadge/TourPlannerDateUtils mit reduziertem Fenster, +1-Tag-Vorberechnung, MainViewModel getCustomersForTour + reduziertes Fenster). Phase 5 (Paging) optional, bei >500 Kunden.
- **Quelle:** PLAN_TOURPLANNER_PERFORMANCE_3TAGE.md.

---

## Plan-Umsetzung (Zentralisierung 2026-02-12)

- **Status:** Phasen 1–4 abgeschlossen.
- **Umgesetzt:** ComposeDialogHelper, AppNavigation, FirebaseRetryHelper, Result/Loading, FirebaseConstants, AppLogger, BaseViewModel, Colors (Beispiel MainScreen).
- **Noch ausstehend (empfohlen):** Weitere Repositories auf FirebaseRetryHelper, weitere Activities auf AppNavigation; alle Log/printStackTrace → AppLogger; weitere ViewModels von BaseViewModel erben.
- **Quelle:** PLAN_UMSETZUNG.md.

---

## Termin-Logik (Überblick)

- **Zentrale Stelle:** TerminBerechnungUtils (getStartOfDay, berechneAlleTermineFuerKunde, naechstesFaelligAmDatum, …). TerminCache (365 Tage), TourDataFilter, TourDataProcessor.
- **Lücke (dokumentiert in BEKANNTE_FEHLER):** Fälligkeit berücksichtigt nicht kundenTermine/ausnahmeTermine in customerFaelligAm, naechstesFaelligAmDatum, getFaelligAmDatum*.
- **Termin-Arten:** Regelmäßig (Intervall), Monatlich (n-ter Wochentag), Wochentag, Unregelmäßig/Ad-hoc, Kunden-Termine, Ausnahme-Termine, Listen-Termine, Tour-Listen (Wochentag). Siehe BERICHT_APP_ZUSTAND Abschnitt 6.

---

## Größte Dateien (Wartbarkeit, Stand Analysen)

| Datei | Zeilen (ca.) | Hinweis |
|-------|--------------|--------|
| TerminBerechnungUtils.kt | ~480 | Zentrale Terminlogik |
| SevDeskApi.kt | ~451 | Evtl. in Endpoint-Module aufteilen |
| TourDataProcessor.kt | ~409 | Gut in Kategorizer/Filter getrennt |
| WaschenErfassungViewModel.kt | ~422 | Evtl. Use-Cases extrahieren |
| CustomerRepository.kt | ~383 | SnapshotParser ausgelagert |
| CustomerStammdatenForm.kt | ~383 | Evtl. Sektionen in Composables |
| TourPlannerScreen.kt | ~380 | TopBar/StateViews ausgelagert |

Faustregel aus Projektregeln: 200–400 Zeilen gut, bis ~600 überschaubar.

---

## Wäscheliste / OCR (aktuell)

- **Engine:** ML Kit Text Recognition (Latin, On-Device). Bereich zwischen „Wäscheliste“ und „Vielen Dank“; Felder: Name, Adresse, Telefon, Artikeltabelle (Mengen). Sonstiges nicht aus OCR.
- **Einschränkung:** Handschrift deutlich schlechter als Druck. OCR als Hilfe verstehen; Erkanntes prüfen, Rest manuell.
- **Cloud-Optionen (Zukunft):** Google Vision/Document AI, AWS Textract, Azure Document Intelligence, OCR.space – siehe WASCHLISTE_OCR_OPTIONEN.md und ZUKUNFTSPLAENE_REFERENZ.md.

---

## Projektidentität (aus PROJECT_MANIFEST)

- **Name:** TourPlaner 2026 (intern we2026_5).
- **Scope:** Bis 500 Kunden; ab ~500 Paging empfohlen.
- **Technik:** Firebase Realtime DB, Auth (anonym), Storage (Fotos), Jetpack Compose, Koin, ViewModel + Coordinator für schwere Screens.

---

*Stand: Feb 2026. Bei Änderungen in den Quell-Dateien diese Referenz ggf. anpassen.*
