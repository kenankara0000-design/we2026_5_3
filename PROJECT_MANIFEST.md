# PROJECT_MANIFEST.md

**Single Source of Truth** für Design-Ziele, Scope und Planung der App we2026_5 (TourPlaner 2026).

Diese Datei muss gepflegt werden. Wenn sich Ziele, Scope oder Planung ändern: Hier aktualisieren.

---

## 1. Projektidentität

- **Name:** TourPlaner 2026 (intern: we2026_5)
- **Zweck:** Touren- und Kundenverwaltungs-App für Wäscherei-/Textildienstleistung
- **Zielgruppe:** Fahrer, Disponenten, Büro im Betrieb

---

## 2. Geplanter Scope (Kundenanzahl)

**Die App ist von Anfang an für bis zu 500 Kunden ausgelegt.**

- Ab ca. 500 Kunden: Paging/Lazy-Loading im Kundenmanager empfohlen (laut BERICHT_TIEFENANALYSE_APP_2026)
- Aktuell: Alle Kunden werden geladen; Parsing läuft im Hintergrund (Feb 2026)

---

## 3. Kernfunktionen (geplant / vorhanden)

- **Kundenstamm:** Gewerblich, Privat, Listen; Stammdaten, Intervalle, Termin-Regeln, Status (Aktiv/Pausiert/Ad-hoc), Urlaub, Fotos
- **Tourenplaner:** Tagesansicht (Überfällig, Heute, Erledigt); Erledigung (A/L/KW), Verschieben, Urlaub, Einzeltermin aussetzen
- **Abholung / Auslieferung / KW:** Erfassen im Tourenplaner; Listen mit A/L-Wochentag und Intervallen
- **Termin-Regeln, Intervalle, Listen:** Kunden-Intervalle (inkl. monatlich/Wochentag), KundenListen mit Listenintervallen; Tour-Slots (tourPlaene); Termin-Regeln am Kunden (Regel-Vorlagen-Strings in UI, kein eigener Haupt-Einstieg)
- **Statistiken, MapView:** Statistiken-Screen; MapView für Kunden/Adressen
- **Offline-Fähigkeit:** Firebase Realtime DB Persistence; NetworkMonitor; Sync-Hinweise in UI
- **Erfassung:** Waschen/Artikel erfassen (Kunde, Positionen); Artikelverwaltung; SevDesk-Artikel-Import als Quelle

---

## 4. Projekt-Gesetze

Die verbindlichen Regeln stehen in **`.cursorrules`** und **`.cursor/rules/`**.  
Das Manifest ergänzt sie um Ziele und Scope; es ersetzt sie nicht.

---

## 5. Technik

- **Datenbank:** Firebase Realtime Database (nicht Firestore)
- **Auth:** Firebase Auth (LoginActivity als Launcher; MainActivity nach Login)
- **Storage:** Firebase Storage (Fotos; ImageUploadWorker, StorageUploadManager)
- **UI:** Jetpack Compose (Migration weitgehend abgeschlossen); schwere Screens mit ViewModel + Coordinator (siehe ARCHITEKTUR_ACTIVITY_COORDINATOR.md)
- **DI:** Koin (appModule: Repositories, ViewModels)

---

## 6. Firebase-Struktur (Realtime Database)

- **customers** – Kundenstammdaten (Customer)
- **customers_for_tour** – Index für Tourenplaner (Migration/Backfill aus customers)
- **kundenListen** – Listen (KundenListe, mit Intervallen)
- **tourPlaene** – Tour-Slots (Wochentag, Stadt, Zeitfenster; TourSlot)
- **articles** – Artikel (z. B. nach SevDesk-Import)
- **waschErfassungen** – Erfassungen (Kunde, Positionen, Datum)
- Fotos: Firebase Storage (Pfade in Customer.fotoUrls)

---

## 7. SevDesk-Anbindung

- **Nur Lesen:** Die App nutzt die SevDesk-API ausschließlich zum **Lesen** (GET). Es werden keine Daten in SevDesk erstellt, geändert oder gelöscht. Import = Kontakte/Artikel in die App übernehmen.
- **SevDesk Import:** Erreichbar über **Einstellungen** (Hauptbildschirm → Einstellungen → SevDesk Import). SevDeskImportActivity mit SevDeskImportScreen.

### SevDesk Kundenpreise (Artikel ↔ Kunde)

In SevDesk gibt es unter **Kontakte → Kunde → Tab „Kunden Preise“** die Möglichkeit, pro Kunde kundenspezifische Preise für Artikel aus der allgemeinen Artikelliste zu hinterlegen. Andere Kunden bekommen die Standardpreise aus der Artikelliste.

- **API-Modell:** **PartContactPrice** (nicht ContactPartPrice, PartUnitPrice oder PartPrice – diese liefern 400 „Model not found“).
- **Endpunkte (my.sevdesk.de/api/v1):**
  - **GET /PartContactPrice?limit=100&offset=0** – Liste aller Kundenpreise (Contact + Part + Preis).
  - Optional Filter nach Kunde: `contact[id]=<Contact-ID>&contact[objectName]=Contact` (falls von der API unterstützt; sonst clientseitig filtern).
  - POST /PartContactPrice (anlegen), PUT /PartContactPrice/{id} (ändern), DELETE /PartContactPrice/{id} (löschen) – für spätere Nutzung, falls Kundenpreise in der App genutzt werden sollen.
- **Modellfelder:** contact, part, type, priceNet, priceGross (und _create, _update, _sev_client).
- Quelle: Offizielle API-Doku (api.sevdesk.de), PHP-Client-Docs (Pommespanzer/sevdesk-php-client: PartContactPriceApi, ModelPartContactPrice). Stand: Feb 2026.

---

## 8. Aktivitäten und Screens (Stand Feb 2026)

| Activity | Zweck | UI (Compose) |
|----------|--------|--------------|
| LoginActivity | Launcher; Firebase Auth | (eigenes Layout) |
| MainActivity | Hauptbildschirm nach Login | MainScreen |
| AddCustomerActivity | Neuer Kunde | AddCustomerScreen, CustomerStammdatenForm |
| CustomerManagerActivity | Kundenliste, Suche, Filter, Mehrfachauswahl | CustomerManagerScreen (+ TopBar, Cards, StateViews) |
| CustomerDetailActivity | Kunde anzeigen/bearbeiten, Termine, Fotos, Urlaub | CustomerDetailScreen (StammdatenTab, TermineTab, …) |
| TourPlannerActivity | Tourenplaner (Tag, Überfällig/Heute/Erledigt) | TourPlannerScreen (+ ErledigungSheet, TopBar, …) |
| KundenListenActivity | Listen übersicht | KundenListenScreen |
| ListeErstellenActivity | Neue Liste | ListeErstellenScreen |
| ListeBearbeitenActivity | Liste bearbeiten, Kunden zuordnen | ListeBearbeitenScreen |
| TerminAnlegenUnregelmaessigActivity | Einzel-/Ausnahme-Termine anlegen | (Compose) |
| UrlaubActivity | Urlaub (von/bis, Einträge) | UrlaubScreen |
| WaschenErfassungActivity | Erfassung starten (Kunde, Positionen) | WaschenErfassungScreen |
| ArtikelVerwaltungActivity | Artikel verwalten | ArtikelVerwaltungScreen |
| SevDeskImportActivity | SevDesk Import | SevDeskImportScreen |
| ErfassungMenuActivity | Erfassung → „Erfassung starten“ / „Artikel verwalten“ | ErfassungMenuScreen |
| SettingsActivity | Einstellungen (SevDesk, Abmelden) | SettingsScreen |
| StatisticsActivity | Statistiken | StatisticsScreen |
| MapViewActivity | Kartenansicht | MapViewScreen |
| AusnahmeTerminActivity | Ausnahme-Termine (A/L) | (vorhanden) |

---

## 9. Hauptbildschirm (Stand Feb 2026)

- **Einstieg:** Nach Login nur MainActivity (kein Regel-Vorlagen-Button auf dem Hauptbildschirm).
- Zeile: **Kunden** | **+ Neu Kunde**
- Großer Button: **Tour Planner** (mit Fälligkeitsanzahl)
- 2×2: **Kunden Listen**, **Statistiken** | **Erfassung**, **Einstellungen**
- **Erfassung** öffnet ErfassungMenuActivity: „Erfassung starten“, „Artikel verwalten“
- **Ad-hoc Termin-Slots:** Vorschläge für die nächsten möglichen Termine bei Kunden mit Ad-hoc-Regel; Tipp öffnet Kundendetail

Hinweis: Strings für „Regel-Vorlagen“ (main_btn_termin_regeln) existieren; ein eigener Einstieg vom Hauptbildschirm ist aktuell nicht umgesetzt. Termin-Regeln werden am Kunden (Detail, Termin anlegen) und über Intervalle/Regel-Zuordnung genutzt.

---

## 10. Wichtige Modelle und Repositories

- **Customer:** id, name, alias, Adresse, intervalle (CustomerIntervall), kundenArt, listeId, kundenTyp (Regelmaessig/Unregelmaessig/AufAbruf), status, Tour-Slot, Urlaub, verschobeneTermine, ausnahmeTermine, Fotos, Erledigungsfelder, …
- **CustomerIntervall:** Abholung/Auslieferung, wiederholen, intervallTage, terminRegelId, regelTyp (WEEKLY, FLEXIBLE_CYCLE, ADHOC, MONTHLY_WEEKDAY), …
- **KundenListe:** name, listeArt, wochentag, intervalle (ListeIntervall)
- **TourSlot:** wochentag, stadt, zeitfenster (Zeitfenster)
- **Repositories:** CustomerRepository, KundenListeRepository, TourPlanRepository, ArticleRepository, ErfassungRepository (alle Koin, Firebase Realtime DB)

---

## 11. Migrations und Start

- Beim Start (MainActivity): runListeToTourMigration, runListeArtToTourMigration, runListeIntervalleMigration, runRemoveDeprecatedFieldsMigration, runPauseExpiredReset (Hintergrund).

---

## 12. Berechtigungen

- INTERNET, CAMERA, WRITE_EXTERNAL_STORAGE (maxSdkVersion 32). FileProvider für Fotos.

---

## 13. Ursprüngliche Ideen (von Anfang an) – Status

*Quelle: BERICHT_TIEFENANALYSE_APP_2026.md, Abschnitte 1.1 und 1.2. Stand: Feb 2026.*

| Idee | Status | Anmerkung |
|------|--------|-----------|
| Abholung und Auslieferung von Wäsche bei Kunden | ✅ | Erfassen im Tourenplaner (A/L/KW), Erledigung-Sheet |
| Planung und Abarbeitung von Touren (tagesbezogen: überfällig, heute, erledigt) | ✅ | TourPlannerActivity, Sektionen Überfällig/Heute/Erledigt |
| Kundenstamm (Gewerblich, Privat, Listen) mit Adresse, Telefon, Notizen, Fotos | ✅ | Customer, KundenListe; CustomerDetail mit Fotos |
| Termin-Regeln (wöchentlich, flexibel, wochentagsbasiert, monatlich, Ad-hoc) | ✅ | Am Kunden/Listen umgesetzt; Intervalle, regelTyp; **kein** eigener „Termin-Regeln“-Einstieg auf Hauptbildschirm |
| Listen mit mehreren Intervallen und zugeordneten Privat-Kunden | ✅ | KundenListen, ListeBearbeiten, ListeIntervall |
| Statistiken (heute/Woche/Monat fällig, überfällig, erledigt, Erledigungsquote, Gesamtkunden) | ✅ | StatisticsActivity, StatisticsScreen |
| Offline-Fähigkeit (Persistence, Sync bei Wiederkehr) | ✅ | Firebase Persistence, NetworkMonitor, Sync-Hinweise in UI |
| Navigation zu Kundenadressen (Google Maps) | ✅ | MapViewActivity öffnet Maps-Intent mit Wegpunkten |
| Fotos (Abhol-/Auslieferungsort) mit Thumbnails und Vergrößerung | ✅ | CustomerPhotoManager, fotoUrls, Storage, Upload |
| Hauptmenü: Neu Kunde, Kunden, Touren Planer (mit Fälligkeitsanzahl), Kunden Listen, Statistiken, **Termin-Regeln** | ⚠️ | Termin-Regeln als eigener Einstieg **nicht** auf Hauptbildschirm (nur Erfassung, Einstellungen in 2×2); Regeln am Kunden/Listen nutzbar |
| Karte: echte In-App-Karten-UI mit Markern/Route | ❌ | Aktuell nur Trampolin zu Google Maps (Intent); echte Karte siehe Backlog |

---

## 14. Noch zu machen / Backlog

*Details und Priorisierung in **IDEEN_ZUKUNFT.md** und **ZUKUNFTSPLAENE.md**. Nichts davon ohne ausdrückliche Freigabe umsetzen (vgl. `.cursor/rules/zukunftsplan.mdc`).*

### Aus ZUKUNFTSPLAENE.md (Ideen & Vorschläge)

- **Login-Feedback:** Bei Fehler der Anmeldung Meldung + Retry statt sofort finish().
- **History-Log für Kunden:** Änderungen protokollieren (wer, wann, was).
- **Optionale Features:** Echte Karten-UI, Tour-Reihenfolge (Drag & Drop), Benachrichtigungen, Paging im Kundenmanager.
- **Benutzer und Rollen:** 3 vordefinierte Benutzer (Admin, Wäscherei, Fahrer) mit rollenbasierter Sicht/Bearbeitung.

### Aus IDEEN_ZUKUNFT.md (bereits als „Zukunft“ markiert)

- **Kalender-Export:** Termine in Gerätekalender (iCal/CalDAV).
- **Onboarding:** Bei leerer App kurze Anleitung „Erstelle deine erste Kundenliste“ mit Navigation.

### Weitere Ideen (IDEEN_ZUKUNFT / BERICHT 4.x)

- **Benachrichtigungen:** Push „Morgen X Kunden fällig“; Erinnerung für pausierte Kunden.
- **Berichte & Export:** Monatliche Berichte (PDF/CSV); Export für Buchhaltung; Tour-Last-Anzeige.
- **Technik & UX:** Paging/Lazy-Loading bei vielen Kunden (>500); Accessibility (contentDescription, 48dp Touch-Targets); Dark Mode (derzeit global deaktiviert).
- **Sonstiges:** Vertragsende/Kündigungsdatum mit Auto-Pause; Bulk-Import (CSV/Excel); Adress-Validierung (Geocoding); Mehrsprachigkeit.
- **Tourenplaner:** Filter nach Kundenart/Liste/Region; Erledigungsquote pro Zeitraum (Woche/Monat).

---

**Letzte Aktualisierung:** Feb 2026
