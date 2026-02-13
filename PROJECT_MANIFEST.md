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

- Ab ca. 500 Kunden: Paging/Lazy-Loading im Kundenmanager empfohlen (siehe REFERENZ_ANALYSEN_UND_BERICHTE.md)
- Aktuell: Alle Kunden werden geladen; Parsing läuft im Hintergrund (Feb 2026)

---

## 3. Kernfunktionen (geplant / vorhanden)

- **Kundenstamm:** Gewerblich, Privat, Listenkunden; Stammdaten, Intervalle, Termin-Regeln, Status (Aktiv/Pausiert/Ad-hoc), Urlaub, Fotos
- **Tourenplaner:** Tagesansicht (Überfällig, Heute, Erledigt); Erledigung (A/L/KW), Verschieben, Urlaub, Einzeltermin aussetzen
- **Abholung / Auslieferung / KW:** Erfassen im Tourenplaner; Listen mit A/L-Wochentag und Intervallen
- **Termin-Regeln, Intervalle, Listen:** Kunden-Intervalle (inkl. monatlich/Wochentag), KundenListen mit Listenintervallen; Tour-Slots (tourPlaene); Termin-Regeln am Kunden (Regel-Vorlagen-Strings in UI, kein eigener Haupt-Einstieg)
- **Statistiken, MapView:** Statistiken-Screen; MapView für Kunden/Adressen
- **Offline-Fähigkeit:** Firebase Realtime DB Persistence; NetworkMonitor; Sync-Hinweise in UI
- **Erfassung:** Waschen/Artikel erfassen (Kunde, Positionen); Belege pro Kunde (Tab „Belege“ in Kundendetail, BelegeActivity); Wäscheliste-Formular (Kundendaten, Artikl-Mengen, Kamera/Foto für OCR, Stammdaten-Ergänzung). **Preise:** Kundenpreise pro Kunde/Artikel; Listen- und Privat-Kundenpreise (standardPreise) als Fallback bei Erfassung. Artikelverwaltung; SevDesk-Artikel-Import als Quelle. **OCR:** ML Kit Text Recognition (On-Device); Cloud-Optionen siehe REFERENZ_ANALYSEN_UND_BERICHTE.md (Abschnitt 8).

---

## 4. Projekt-Gesetze

Die verbindlichen Regeln stehen in **`.cursorrules`** und **`.cursor/rules/`**.  
Das Manifest ergänzt sie um Ziele und Scope; es ersetzt sie nicht.

---

## 5. Technik

- **Application:** FirebaseConfig (setzt Persistence, Koin, Dark Mode deaktiviert)
- **Datenbank:** Firebase Realtime Database (nicht Firestore)
- **Auth:** Firebase Auth (LoginActivity als Launcher; MainActivity nach Login). Aktuell **nur anonymer Login** („Anonym weiter“); AdminChecker prüft E-Mail für Debug/Test. Alle angemeldeten Nutzer haben volle Rechte (keine Admin/Anonymous-Trennung).
- **Storage:** Firebase Storage (Fotos; ImageUploadWorker, StorageUploadManager, WorkManager)
- **Offline:** Firebase Persistence, NetworkMonitor (isOnline, isSyncing), Sync-Hinweise in UI
- **UI:** Jetpack Compose (Migration abgeschlossen); schwere Screens mit ViewModel + Coordinator (siehe REFERENZ_ANALYSEN_UND_BERICHTE.md Abschnitte 1 und 3)
- **Navigation:** AppNavigation (Intent-Factory für typ-sichere Activity-Navigation)
- **DI:** Koin (appModule: Repositories, ViewModels)

---

## 6. Firebase-Struktur (Realtime Database)

- **customers** – Kundenstammdaten (Customer)
- **customers_for_tour** – Index für Tourenplaner (Migration/Backfill aus customers)
- **kundenListen** – Listen (KundenListe, mit Intervallen)
- **tourPlaene** – Tour-Slots (Wochentag, Stadt, Zeitfenster; TourSlot)
- **tourReihenfolge** – Manuelle Reihenfolge der Kunden pro Wochentag (Keys: wochentag_1 … wochentag_7, Value: kommagetrennte Kunden-IDs); geräteübergreifend
- **articles** – Artikel (z. B. nach SevDesk-Import)
- **waschErfassungen** – Erfassungen (Kunde, Positionen, Datum)
- **kundenPreise** – Kundenpreise pro Kunde/Artikel (customerId → articleId → priceNet, priceGross)
- **standardPreise** – **Listen- und Privat-Kundenpreise** (Listenkunden + Privat) (articleId → priceNet, priceGross); kann bei der Erfassung genutzt werden (Fallback, wenn keine Kundenpreise hinterlegt sind)
- Fotos: Firebase Storage (Pfade in Customer.fotoUrls)

---

## 7. SevDesk-Anbindung

- **Nur Lesen:** Die App nutzt die SevDesk-API ausschließlich zum **Lesen** (GET). Es werden keine Daten in SevDesk erstellt, geändert oder gelöscht. Import = Kontakte/Artikel in die App übernehmen.
- **SevDesk Import:** Erreichbar über **Einstellungen → Data Import → SevDesk Import**. DataImportActivity zeigt DataImportScreen mit SevDesk-Button; öffnet SevDeskImportActivity.

### SevDesk Kundenpreise (Artikel ↔ Kunde)

In SevDesk gibt es unter **Kontakte → Kunde → Tab „Kunden Preise“** die Möglichkeit, pro Kunde kundenspezifische Preise für Artikel aus der allgemeinen Artikelliste zu hinterlegen. Andere Kunden bekommen die Listen- und Privat-Kundenpreise aus der Artikelliste.

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
| LoginActivity | Launcher; Firebase Auth (anonym) | LoginScreenContent (Compose) |
| MainActivity | Hauptbildschirm nach Login | MainScreen |
| AddCustomerActivity | Neuer Kunde | AddCustomerScreen, CustomerStammdatenForm |
| CustomerManagerActivity | Kundenliste, Suche, Filter, Mehrfachauswahl | CustomerManagerScreen (+ TopBar, Cards, StateViews) |
| CustomerDetailActivity | Kunde anzeigen/bearbeiten, Termine, Fotos, Urlaub | CustomerDetailScreen (StammdatenTab, TermineTab, BelegeTab) |
| TourPlannerActivity | Tourenplaner (Tag, Überfällig/Heute/Erledigt) | TourPlannerScreen (+ ErledigungSheet, TopBar, …) |
| KundenListenActivity | Listen übersicht | KundenListenScreen |
| ListeErstellenActivity | Neue Liste | ListeErstellenScreen |
| ListeBearbeitenActivity | Liste bearbeiten, Kunden zuordnen | ListeBearbeitenScreen |
| TerminAnlegenUnregelmaessigActivity | Einzel-/Ausnahme-Termine anlegen | (Compose) |
| UrlaubActivity | Urlaub (von/bis, Einträge) | UrlaubScreen |
| WaschenErfassungActivity | Erfassung starten (Kunde, Positionen); Belege pro Monat; Wäscheliste-Formular, manuell | WaschenErfassungScreen |
| BelegeActivity | Belege-Übersicht (Monatsauswahl, Kundenfilter) | (Compose) |
| ArtikelVerwaltungActivity | Artikel verwalten | ArtikelVerwaltungScreen |
| ErfassungMenuActivity | Erfassung → „Erfassung starten“ / „Belege“ | ErfassungMenuScreen |
| PreiseActivity | Preis-Menü: Kundenpreise, Listen-/Privat-Kundenpreise, Artikel | PreiseScreen |
| KundenpreiseActivity | Kundenpreise pro Kunde/Artikel | KundenpreiseScreen |
| ListenPrivatKundenpreiseActivity | Listen- und Privat-Kundenpreise (standardPreise) | ListenPrivatKundenpreiseScreen |
| DataImportActivity | Datenimport-Menü (SevDesk Import) | DataImportScreen |
| SevDeskImportActivity | SevDesk Import (Kontakte, Artikel, Preise) | SevDeskImportScreen |
| SettingsActivity | Einstellungen (Preise, Data Import, App-Daten zurücksetzen, Abmelden) | SettingsScreen |
| StatisticsActivity | Statistiken | StatisticsScreen |
| MapViewActivity | Kartenansicht (Trampolin zu Google Maps) | MapViewScreen |
| AusnahmeTerminActivity | Ausnahme-Termine (A/L) | (Compose) |

---

## 9. Hauptbildschirm (Stand Feb 2026)

- **Einstieg:** Nach Login nur MainActivity (kein Regel-Vorlagen-Button auf dem Hauptbildschirm).
- Zeile: **Kunden** | **+ Neu Kunde**
- Großer Button: **Tour Planner** (mit Fälligkeitsanzahl)
- 2×2: **Kunden Listen**, **Statistiken** | **Erfassung**, **Einstellungen**
- **Erfassung** öffnet ErfassungMenuActivity: „Erfassung starten“, „Belege“.
- **Einstellungen** öffnet SettingsActivity: Buttons „Preise“, „Data Import“; Menü: App-Daten zurücksetzen, Abmelden. Preise → PreiseActivity (Kundenpreise, Listen-/Privat-Kundenpreise, Artikel verwalten). Data Import → DataImportActivity (SevDesk Import).
- **Ad-hoc Termin-Slots:** Vorschläge für die nächsten möglichen Termine bei Kunden mit Ad-hoc-Regel; Tipp öffnet Kundendetail

Hinweis: Strings für „Regel-Vorlagen“ (main_btn_termin_regeln) existieren; ein eigener Einstieg vom Hauptbildschirm ist aktuell nicht umgesetzt. Termin-Regeln werden am Kunden (Detail, Termin anlegen) und über Intervalle/Regel-Zuordnung genutzt.

---

## 10. Wichtige Modelle und Repositories

- **Customer:** id, name, alias, Adresse, intervalle (CustomerIntervall), kundenArt, listeId, kundenTyp (Regelmaessig/Unregelmaessig/AufAbruf), status, Tour-Slot, Urlaub, verschobeneTermine, ausnahmeTermine, Fotos, Erledigungsfelder, …
- **CustomerIntervall:** Abholung/Auslieferung, wiederholen, intervallTage, terminRegelId, regelTyp (WEEKLY, FLEXIBLE_CYCLE, ADHOC, MONTHLY_WEEKDAY), …
- **KundenListe:** name, listeArt, wochentag, intervalle (ListeIntervall)
- **TourSlot:** wochentag, stadt, zeitfenster (Zeitfenster)
- **ListenPrivatKundenpreis:** articleId, priceNet, priceGross (Firebase-Pfad: standardPreise)
- **Repositories:** CustomerRepository, KundenListeRepository, TourPlanRepository, TourOrderRepository (tourReihenfolge), ArticleRepository, ErfassungRepository, KundenPreiseRepository, ListenPrivatKundenpreiseRepository (alle Koin, Firebase Realtime DB)

---

## 11. Migrations und Start

- Beim Start (MainActivity): runListeToTourMigration, runListeArtToTourMigration, runTourToListenkundenMigration, runListeArtTourToListenkundenMigration, runListenPrivatKundenpreiseMigration, runListeIntervalleMigration, runRemoveDeprecatedFieldsMigration, runPauseExpiredReset (Hintergrund).

---

## 12. Berechtigungen

- INTERNET, CAMERA, WRITE_EXTERNAL_STORAGE (maxSdkVersion 32). FileProvider für Fotos (Kamera, Galerie-Auswahl). `queries` für Google Maps (resolveActivity).

---

## 13. Ursprüngliche Ideen (von Anfang an) – Status

*Quelle: REFERENZ_ANALYSEN_UND_BERICHTE.md (Abschn. 9), ehem. Tiefenanalyse. Stand: Feb 2026.*

| Idee | Status | Anmerkung |
|------|--------|-----------|
| Abholung und Auslieferung von Wäsche bei Kunden | ✅ | Erfassen im Tourenplaner (A/L/KW), Erledigung-Sheet |
| Planung und Abarbeitung von Touren (tagesbezogen: überfällig, heute, erledigt) | ✅ | TourPlannerActivity, Sektionen Überfällig/Heute/Erledigt |
| Kundenstamm (Gewerblich, Privat, Listenkunden) mit Adresse, Telefon, Notizen, Fotos | ✅ | Customer, KundenListe; CustomerDetail mit Fotos |
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

*Details und Priorisierung in **ZUKUNFTSPLAENE.md**. Nichts davon ohne ausdrückliche Freigabe umsetzen (vgl. `.cursor/rules/zukunftsplan.mdc`).*

### Aus ZUKUNFTSPLAENE.md (Auswahl)

- History-Log, Optionale Features (Karten-UI, Tour-Reihenfolge, Benachrichtigungen, Paging), Benutzer und Rollen (Admin/Wäscherei/Fahrer), Kalender-Export, Onboarding.
- Benachrichtigungen, Berichte & Export, Technik & UX (Paging, Accessibility, Dark Mode), Sonstiges (Vertragsende, Bulk-Import, Adress-Validierung, Mehrsprachigkeit), Tourenplaner-Filter, Erledigungsquote pro Zeitraum.

---

## 15. Bekannte Fehler (Bug-Punkte)

**Alle bekannten Bugs stehen in `BEKANNTE_FEHLER.md`.**  
Dort werden Symptom, Ursache und relevante Code-Stellen geführt. Keine Änderung am Verhalten (Fix) ohne Freigabe. Vgl. `.cursor/rules/bugs-dokumentation.mdc`.

---

## 16. Drucker für Belege (Klebestreifen / Etiketten)

*Ziel: Beleg drucken = Klebestreifen mit Kunden-Alias und Endpreis (keine Rechnungen). Rechnungen kommen später.*

### Gut geeignet für die Aufgabe (Klebestreifen, Alias + Endpreis, Android)

**1. Thermo-Etikettendrucker (Bluetooth, schmal)**

- **58 mm oder 40 mm Breite** – typisch für Belege/Tickets/Etiketten.
- **Bluetooth** – direkt vom Android-Handy ohne Kabel.
- **ESC/POS** – viele dieser Drucker verstehen den ESC/POS-Befehlssatz; dann könnt ihr aus der App Text (Alias, Preise, Endpreis) direkt senden.

**2. Konkrete Typen (Beispiele, ohne Kaufempfehlung)**

- **58-mm-Thermo-Drucker** für „Ticket/Beleg“ (wie im Gastronomie-/Takeaway-Bereich): oft Bluetooth, Android-tauglich, Kleberrolle oder Endlos-Papier.
- **Schmale Etiketten-Drucker** (z. B. 40 mm, 58 mm) mit selbstklebenden Rollen – für Klebestreifen mit Kundenname + Preis.
- **Marken**, die oft mit Android-Apps genutzt werden: z. B. Epson (TM-Serie), Star Micronics, Bixolon, Sunmi (wenn POS-Geräte genutzt werden), sowie viele No-Name/ODM-Geräte mit ESC/POS über Bluetooth.

**3. Worauf ihr achten solltet**

- **ESC/POS-Unterstützung** – dann könnt ihr mit einer Library (z. B. DantSu ESC/POS-ThermalPrinter-Android oder ähnlich) aus der App drucken, ohne Hersteller-App.
- **Bluetooth** – für Nutzung am Handy ohne Kabel.
- **Papier/Etiketten:** Rolle in der gewünschten Breite (z. B. 58 mm), ob Endlos oder mit Klebestreifen – je nachdem, was ihr physisch haben wollt.

### Für die App

- Drucker mit **ESC/POS** und **Bluetooth** wählen.
- In der App eine **ESC/POS-Library** einbinden und Text (Kunden-Alias, Zeilen/Summen, Endpreis) als einfachen Beleg/Etikett-Text an den Drucker senden.
- **58 mm (oder 40 mm)** reicht für kurze Belege/Klebestreifen mit Alias und Endpreis gut aus.

---

## 17. Übersicht Analysen und Berichte

- **Index (was wo liegt):** **DOKUMENTATION_REFERENZ_INDEX.md**
- **Konsolidierte Referenz (vollständiger Inhalt):** **REFERENZ_ANALYSEN_UND_BERICHTE.md**

---

## 18. Tiefenanalyse & Maßnahmenplan (Stand Feb 2026)

- **Analyse-Index:** `.cursor/plans/ANALYSE-INDEX.md` – Übersicht aller 9 Analysen (alle ✅ erledigt)
- **Konsolidierter Maßnahmenplan:** `.cursor/plans/2026-02-13_21-47.massnahmenplan.md` – 73 Aufgaben, 8 Phasen (~12–18 Wochen)
- **Status:** Phase 1 (Sofort-Fixes) umgesetzt (9/9 Aufgaben, 2026-02-13). Phase 2 (Design-System) umgesetzt (10/10 Aufgaben, 2026-02-13). Phase 3 (Composables + Error-Handling) umgesetzt (11/11 Aufgaben, 2026-02-13). Phase 4+ noch offen. Keine Änderung ohne Freigabe.

**Analysen im Überblick:**

| Nr. | Thema | Datei |
|-----|-------|-------|
| 01 | Design-Konsistenz | `analyse-01-design-konsistenz.md` |
| 02 | Code-Architektur | `analyse-02-code-architektur.md` |
| 03 | Kompatibilität | `analyse-03-kompatibilitaet.md` |
| 04 | UX/Layout (24 Screens) | `analyse-04-ux-layout.md` |
| 05 | User-Flow (15 Klick-Pfade) | `analyse-05-user-flow.md` |
| 06 | Informationsarchitektur | `analyse-06-informationsarchitektur.md` |
| 07 | Offline-Verhalten | `analyse-07-offline-verhalten.md` |
| 08 | Fehlerbehandlung UX | `analyse-08-fehlerbehandlung-ux.md` |
| 09 | Performance | `analyse-09-performance.md` |

---

**Letzte Aktualisierung:** Feb 2026
