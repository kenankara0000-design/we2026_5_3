# Analyse 04: UX/Layout (Screen-für-Screen)

**Status:** ✅ Erledigt (2026-02-13)  
**Priorität:** 🔴 Höchste

---

## Screens (Reihenfolge nach Nutzungshäufigkeit)

| Nr. | Screen | Status |
|-----|--------|--------|
| 1 | Tourenplaner (Kunden-Karte, Sektionen) | ✅ |
| 2 | Kundendetail – Tab Stammdaten | ✅ |
| 3 | Kundendetail – Tab Termine | ✅ |
| 4 | Kundendetail – Tab Belege | ✅ |
| 5 | Hauptbildschirm | ✅ |
| 6 | Kundenliste / Kundenmanager | ✅ |
| 7 | Erfassung-Menü | ✅ |
| 8 | Wäsche-Erfassung (Formular) | ✅ |
| 9 | Belege-Übersicht | ✅ |
| 10 | Listen-Übersicht | ✅ |
| 11 | Liste erstellen | ✅ |
| 12 | Liste bearbeiten | ✅ |
| 13 | Neuer Kunde | ✅ |
| 14 | Preise-Menü | ✅ |
| 15 | Kundenpreise | ✅ |
| 16 | Listen-/Privat-Kundenpreise | ✅ |
| 17 | Statistiken | ✅ |
| 18 | Einstellungen | ✅ |
| 19 | SevDesk-Import | ✅ |
| 20 | Urlaub | ✅ |
| 21 | Ausnahme-Termine | ✅ |
| 22 | Artikel-Verwaltung | ✅ |
| 23 | Login | ✅ |
| 24 | MapView | ✅ |

---

## Ergebnisse

### 1. Tourenplaner (`TourPlannerScreen.kt`)

**UI-Elemente (Reihenfolge):**

| Element | Datei | Beschreibung |
|---------|-------|--------------|
| TopBar | `TourPlannerTopBar.kt` | Datum (←/→), Tour-Counts (A/L), Refresh, Kebab-Menü, Offline-Banner |
| Buttons-Row | `TourPlannerTopBar.kt` | Karte, Heute, Erledigte (N) |
| LazyColumn | `TourPlannerScreen.kt` Z.274–332 | Sections + Customer-Rows |
| SectionHeader | `TourPlannerSectionHeader.kt` | Überfällig/Heute/Erledigt, klappbar |
| ListeHeader | `TourPlannerListeHeader.kt` | Listen-Header |
| TourListeCard | `TourListeCardRow.kt` | Listen-Karten, ein-/ausklappbar |
| ErledigungSheet | `ErledigungSheetContent.kt` | ModalBottomSheet mit Tabs |
| ErledigtSheet | `TourPlannerErledigtSheet.kt` | ModalBottomSheet |
| OverviewDialog | `TourPlannerOverviewDialog.kt` | AlertDialog bei Karten-Klick |

**Kunden-Karte (`TourPlannerCustomerRow.kt` Z.49–238):**
- Kundenart-Badge (G/P/L), displayName, AlWochentagText, verschobenInfo, Status-Badge, Button „Aktionen"
- **Fehlt:** Adresse/PLZ, Telefon, nächster Termin

**Spacing:**
- Screen-Padding: `16.dp`, LazyColumn: `spacedBy(12.dp)`, Card-Padding: `16.dp`
- Swipe links/rechts für Tagwechsel

**Bottom-Sheets:**
- ErledigungSheet: feste Höhe `520.dp`
- ErledigtSheet: `verticalScroll`

---

### 2. Kundendetail – Tab Stammdaten (`CustomerDetailStammdatenTab.kt`)

**UI-Elemente:**
- Read-Only-Modus: Label + Text in grauen Boxen (`Color(0xFFE0E0E0)` hardcodiert), klickbar (Adresse → Maps, Telefon → Anruf)
- ActionsRow: „Urlaub", „Bearbeiten" (nur Admin); Upload-Fortschritt
- Edit-Modus: `CustomerStammdatenForm` (Name, Alias, Adresse, PLZ/Stadt, Koordinaten, Telefon, Notizen, eingeklappt: Uhrzeit, Kundennr., Tags, Tour)
- FotosSection: `LazyRow` mit Thumbnails, „Foto hinzufügen"
- **Pflichtfeld:** Nur Name (`hint_name_required`)

**Kritischer Punkt:** Kein sichtbarer Zurück-Button in TopBar (`navigationIcon = { }`)

---

### 3. Kundendetail – Tab Termine (`CustomerDetailTermineTab.kt`)

**UI-Elemente (Reihenfolge):**
- TermineTourForm (Edit-Modus): Kundenart, Typ, Startdatum, Intervall, A/L-Tage
- Tour-Listen-Hinweis
- StatusSection: Aktiv/Pausiert/Ad-hoc Switch, „+ Termin"
- NaechsterTermin: Fälligkeitsdatum
- KundenTypSection (Read-Only)
- AusnahmeTermineSection (einklappbar)
- KundenTermineSection (einklappbar)
- AlleTermineBlock: max. 6 Zeilen, 365 Tage

**Bottom-Sheets:** AddMonthlyIntervallSheet, NeuerTerminArtSheet

**Scrolling:** `verticalScroll`

---

### 4. Kundendetail – Tab Belege (`CustomerDetailBelegeTab.kt`)

**UI-Elemente:**
- SegmentedButtonRow: Offen / Erledigt
- LazyColumn mit Beleg-Cards (Monatslabel, Anzahl Erfassungen)
- Button „Neue Erfassung" → AlertDialog (Kamera/Foto, Formular, manuell)

---

### 5. Hauptbildschirm (`MainScreen.kt`)

**UI-Elemente (Reihenfolge):**
- Offline-Badge (gelb), Sync-Badge (blau), Offline-Hinweis-Text
- Titel (`28.sp`, Bold)
- Tour-Hero-Card mit Fälligkeits-Badge + Button
- Zeile: Kunden + Neu Kunde (72dp)
- 2×2 Outlined: Listen, Statistiken, Erfassung, Settings (64dp)
- Slot-Sektion: max. 5 Ad-hoc-Slot-Vorschläge

**Probleme:**
- Mix aus 72dp/64dp Button-Höhen und 16sp/14sp Font-Größen
- Spacing inkonsistent: 12, 16, 20, 24, 28dp gemischt
- `contentDescription = null` bei Icons (Barrierefreiheit)
- `verticalScroll` (kein LazyColumn – bei kurzem Screen OK)

---

### 6. Kundenliste / Manager (`CustomerManagerScreen.kt`)

**UI-Elemente:**
- TopAppBar: Titel „Kunden", Offline-Badge
- Admin-Buttons: Auswählen, Exportieren, Neuer Kunde (FAB 48dp)
- TabRow: Gewerblich / Privat / Listen
- SearchAndFilter: Suchfeld + FilterChips
- LazyColumn mit CustomerManagerCards
- BulkBar: Fix unten bei Mehrfachauswahl

**Kunden-Karte (`CustomerManagerCard.kt` Z.49–126):**
- Foto-Thumbnail (40×40dp), Kundenart-Badge, displayName (18sp Bold), Adresse, A/L-Wochentage
- **Fehlt:** Status (Pausiert/Ad-hoc), Telefon, Listenzugehörigkeit, Ohne-Tour-Markierung

**Spacing:** `spacedBy(8.dp)`, Card-Padding 12dp

---

### 7. Erfassung-Menü (`ErfassungMenuScreen.kt`)

- TopAppBar mit Zurück, 2 Buttons vertikal (56dp): „Erfassung starten", „Belege"

---

### 8. Wäsche-Erfassung (`WaschenErfassungScreen.kt`)

**States/Content:**

| State | Inhalt |
|-------|--------|
| KundeSuchen | Suchfeld + LazyColumn Kunden |
| ErfassungenListe | Kundenname, Tab Offen/Erledigt, LazyColumn Belege, „Neue Erfassung" |
| Erfassen | Notiz, ErfassungPositionenSection (Artikelsuche, max 8 Treffer), Speichern |
| Formular | Name, Adresse, Tel, Artikl-Mengen (2 Spalten), Sonstiges, Kamera/Foto, Speichern/Abbrechen |
| BelegDetail | Erfassungen als Cards, Gesamtzeilen, Gesamtpreis, Kebab-Menü |

---

### 9. Belege-Übersicht (`BelegeScreen.kt`)

- Suchfeld, Tab Offen/Erledigt, LazyColumn mit Beleg-Cards (Kundenname, Monat, Erfassungsanzahl)

---

### 10. Listen-Übersicht (`KundenListenScreen.kt`)

- TopAppBar, FAB, „Neue Liste" + „Aktualisieren", Suchfeld, FilterChips (Name/Anzahl), LazyColumn
- Listen-Karten: Name, Art, Kundenanzahl, ErstelltAm, Wochentag-Hinweis

---

### 11. Liste erstellen (`ListeErstellenScreen.kt`)

- Name (Pflicht), RadioButtons (Gewerbe/Privat/Listenkunden), Checkbox Wochentagsliste, WeekdaySelector
- Speichern-Button 56dp

---

### 12. Liste bearbeiten (`ListeBearbeitenScreen.kt`)

- Metadaten-Block, ListeIntervallSection, Kunden in Liste, Verfügbare Kunden
- **Kein LazyColumn** – `forEach` in Column

---

### 13. Neuer Kunde (`AddCustomerScreen.kt`)

- `CustomerStammdatenForm` mit `showTermineTourSection = true`
- Pflichtfeld: nur Name
- Speichern-Button 56dp
- `verticalScroll`, kein LazyColumn

---

### 14–16. Preise

| Screen | Inhalt |
|--------|--------|
| PreiseScreen | 3 Buttons: Kundenpreise, Listen-/Privat-KP, Artikel |
| KundenpreiseScreen | Kundensuche → Preisliste. **Problem:** Kein Back in TopBar; `onBackToKundeSuchen` nicht genutzt |
| ListenPrivatKundenpreiseScreen | LazyColumn Preis-Cards, FAB zum Hinzufügen, CustomDialog |

---

### 17–19. Statistiken, Einstellungen, SevDesk

| Screen | Inhalt |
|--------|--------|
| StatisticsScreen | 9 StatCards, `verticalScroll`. Aktuell `STATISTICS_SLEEP_MODE = true` (deaktiviert) |
| SettingsScreen | 3 Buttons (Preise, Data Import, Abmelden), Menü: Reset |
| SevDeskImportScreen | Token-Feld, Progress, Import-/Lösch-Buttons |

---

### 20–24. Urlaub, Ausnahme, Artikel, Login, MapView

| Screen | Inhalt |
|--------|--------|
| UrlaubScreen | Cards (von–bis), Edit/Delete, „Neuer Urlaub". **`forEach` statt LazyColumn** |
| AusnahmeTerminActivity | Kalender, Typ-Dialog, Bestätigung |
| ArtikelVerwaltungScreen | LazyColumn Artikel-Cards. **Delete-Button `enabled = false`** |
| LoginScreenContent | Titel, „Anonym weiter", Loading, Error+Retry |
| MapViewScreen | Loading → Success → Maps-Intent → finish |

---

## Querschnittsbefunde

### Back-Button fehlt in TopBar

| Screen | Back vorhanden? |
|--------|-----------------|
| CustomerDetailScreen | ❌ (`navigationIcon = { }`) |
| CustomerManagerScreen | ❌ |
| KundenpreiseScreen | ❌ |
| ListenPrivatKundenpreiseScreen | ❌ |
| StatisticsScreen | ❌ |
| SettingsScreen | ❌ |
| SevDeskImportScreen | ❌ |
| UrlaubScreen | ❌ |
| MapViewScreen | ❌ |
| AusnahmeTerminActivity | ❌ |
| ArtikelVerwaltungScreen | ❌ |
| TerminAnlegenUnregelmaessigActivity | ❌ |
| ErfassungMenuScreen | ✅ |
| PreiseScreen | ✅ |
| DataImportScreen | ✅ |

### Button-Höhen inkonsistent

| Höhe | Screens |
|------|---------|
| 48dp | PreiseScreen, DataImportScreen, FABs |
| 56dp | ErfassungMenu, ListeErstellen, AddCustomer, SevDesk |
| 64dp | MainScreen (Outlined 2×2) |
| 72dp | MainScreen (Kunden/Neu Kunde) |

### LazyColumn vs. forEach/verticalScroll (bei dynamischen Listen)

| Screen | Methode | Potenzielles Problem |
|--------|---------|---------------------|
| ListeBearbeitenScreen | `forEach` in Column | Bei vielen Kunden ineffizient |
| UrlaubScreen | `forEachIndexed` | Bei vielen Urlauben ineffizient |
| BelegDetail | `forEach` in Column | Bei vielen Erfassungen ineffizient |

---

*Keine Umsetzung ohne ausdrückliche Freigabe.*
