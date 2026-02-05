# Plan: Weitere Punkte (ein Plan, Punkte 1–5)

Ein gemeinsamer Plan für alle besprochenen Erweiterungen. Punkt 5 (Regel-Vorlagen) ist hier integriert, nicht in einem separaten Plan.

---

## Punkt 1: Neuer Termin-Typ „Auf Abruf“

*(Kurzreferenz – Details wie zuvor besprochen.)* Kunden-/Termin-Typ „Auf Abruf“, A und L am selben Tag, dynamische Terminvorschläge (nächste 2 Wochen Mo–Fr). Enum `KundenTyp` erweitern, TerminRegelManager/UI/Filter/Migration anpassen.

---

## Punkt 2: Vereinheitlichung „Kunde Anlegen“ und „Kunde Bearbeiten“

*(Kurzreferenz.)* AddCustomerScreen und CustomerDetailScreen (Bearbeitung) inhaltlich und optisch angleichen. Gemeinsame Composable z. B. `CustomerStammdatenForm`, fehlende Felder ergänzen (z. B. Stadt/PLZ).

---

## Punkt 3: Regelmäßig – „A + Zahl (Tage) = L“ und Zyklus-Intervall

*(Kurzreferenz.)* Zwei getrennte Angaben: „Tage zwischen A und L“ (z. B. 7) und „Zyklus (Tage bis zum nächsten A)“ (z. B. 28). `auslieferungDatum = abholungDatum + tageAzuL`, `intervallTage` = Zyklus. UI beim Anlegen/Bearbeiten anpassen.

---

## Punkt 4: Erledigt-Bereich – Card-Optik & Erledigt-Badge

**Ziel:** Erledigt-Bereich wie eine Kundenkarte wirken lassen, Header und Tour-Listen-Name hervorheben, erledigte Kunden klar erkennbar machen.

### 4.1 Erledigt-Header größer

- **Datei:** `app/.../ui/tourplanner/TourPlannerSectionHeader.kt`
- **Änderung:** Für `SectionType.DONE` Titel-Schriftgröße explizit erhöhen (z. B. 18.sp oder 20.sp), optional Padding 14–16.dp.

### 4.2 Erledigt-Bereich als eine Card (alle in einer Karte)

- **Dateien:** `TourPlannerScreen.kt`, ggf. neue Composable oder Anpassung der LazyColumn-Struktur.
- **Änderung:** Den gesamten Erledigt-Bereich (Header + alle erledigten Kunden inkl. Tour-Listen) in **eine** umschließende Card packen. Innen: größerer „ERLEDIGT“-Header, darunter Einzelkunden bzw. Tour-Listen mit Listen-Namen, darunter deren Kunden. So wirkt der Bereich wie eine Kundenkarte mit allen Erledigten.

### 4.3 Tour-Listen-Name im Erledigt-Bereich größer

- **Datei:** `app/.../ui/tourplanner/TourPlannerErledigtRow.kt`
- **Änderung:** „— listeName —“ von 14.sp auf 16.sp oder 18.sp erhöhen (FontWeight.Bold beibehalten).

### 4.4 Erledigt-Badge: Grünes Häkchen hinter dem A-Badge

- **Option:** A (Badge auf/neben dem A).
- **Konkret:** Bei erledigten Kunden im Erledigt-Bereich: **Auf dem A-Badge** – hinter „A“ ein **grünes Häkchen** anzeigen (also „A“ + ✓, z. B. „A ✓“ oder A mit kleinem ✓ rechts daneben).
- **Datei:** `app/.../ui/tourplanner/TourPlannerCustomerRow.kt` (und ggf. `TourPlannerErledigtRow.kt` / Aufrufer).
- **Umsetzung:** `TourCustomerRow` um einen Parameter erweitern (z. B. `isErledigtAmTag: Boolean` oder `showErledigtBadge: Boolean`). Wenn gesetzt und der Status-Badge „A“ oder „AL“ ist: Im Abholungs-Badge-Bereich hinter dem „A“ ein grünes Häkchen (✓) anzeigen (Icon oder Text), Farbe z. B. `colorResource(R.color.section_done_*)` oder neues `erledigt_badge_green`. Bei reinem „L“-Badge optional: Häkchen neben L (oder nur bei A/AL wie besprochen).

**Kurz:** Erledigt = A-Badge mit grünem Häkchen dahinter („A ✓“).

---

## Punkt 5: Regel-Vorlagen – „A+Zahl=L“ hinzufügen, Regel-Typ entfernen

**Ziel:** Bei neuer Regel-Erstellung (Regel-Vorlagen) das Feld **„A + Zahl (Tage) = L-Termin“** anbieten wie bei Kunden; **Regel-Typ** (Dropdown) aus der UI entfernen. Rest der Felder bleibt unverändert.

### 5.1 „A + Zahl (Tage) = L-Termin“ in neuer Regel

- **Bedeutung:** Tage zwischen Abholung (A) und Auslieferung (L). L-Datum = A-Datum + diese Zahl.
- **Dateien:** `TerminRegel.kt`, `TerminRegelErstellenScreen.kt`, `TerminRegelErstellenViewModel.kt`, ggf. `TerminRegelManager.kt` / Anwendung der Regel auf Kunden.
- **Umsetzung:**
  - In `TerminRegel` optionales Feld ergänzen (z. B. `tageAzuL: Int = 7`), falls noch nicht vorhanden. Beim Speichern/Anwenden der Regel: Auslieferung = Abholung + tageAzuL.
  - Im Screen: neues Eingabefeld „A + Zahl (Tage) = L-Termin“ (z. B. neben/vor dem Intervall-Bereich), Label/Hint wie bei Kunden.
  - ViewModel: State um `tageAzuL` (String/Int) erweitern, beim Speichern in die Regel übernehmen; beim Anwenden der Regel auf Kunden (TerminRegelManager/wendeRegelAufKundeAn) L-Datum aus A + tageAzuL berechnen.

### 5.2 Regel-Typ entfernen

- **Datei:** `TerminRegelErstellenScreen.kt`
- **Änderung:** Das komplette UI-Element **„Regel Typ“** entfernen: Label „Regel Typ“, `RegelTypDropdown` und alle zugehörigen Callbacks (`onRegelTypChange`). Im Screen keine Auswahl mehr für WEEKLY/FLEXIBLE_CYCLE/ADHOC.
- **Backend:** `regelTyp` im Modell `TerminRegel` und in `CustomerIntervall` beibehalten (wird an anderen Stellen genutzt). Beim Speichern aus dem Erstellen-Screen einen **festen Default** setzen (z. B. `TerminRegelTyp.WEEKLY`), damit bestehende Logik (TerminRegelManager, TerminAusKundeUtils) weiter funktioniert.
- **ViewModel:** `TerminRegelErstellenViewModel` – `regelTyp` aus dem State optional entfernen oder nur noch intern auf Default setzen; `onRegelTypChange`/`setRegelTyp` nicht mehr von außen aufrufbar; beim Erzeugen der `TerminRegel` in `saveRegel()` immer z. B. `regelTyp = TerminRegelTyp.WEEKLY` (oder ein anderer fester Wert). Beim Laden einer bestehenden Regel den gespeicherten `regelTyp` weiterhin lesen (für Anzeige in anderen Kontexten), nur die Bearbeitung in diesem Screen entfällt.

### 5.3 Rest bleibt

- Name, Beschreibung, Aktiv, Startdatum, Täglich, Abholung/Auslieferung-Wochentage, Wiederholen, Intervall-Tage, Intervall-Anzahl usw. unverändert.

---

## Betroffene Dateien (Punkt 4)

| Datei | Änderung |
|-------|----------|
| `TourPlannerSectionHeader.kt` | DONE-Header: größere Schrift (18.sp/20.sp), ggf. mehr Padding. |
| `TourPlannerScreen.kt` | Erledigt-Bereich in eine umschließende Card (Struktur anpassen). |
| `TourPlannerErledigtRow.kt` | Listen-Name 16.sp/18.sp; Aufruf von `TourCustomerRow` mit Erledigt-Flag. |
| `TourPlannerCustomerRow.kt` | Parameter `showErledigtBadge`/`isErledigtAmTag`; bei A/AL: hinter „A“ grünes Häkchen. |
| Ggf. `colors.xml` | Farbe für Erledigt-Häkchen (z. B. `erledigt_badge_green`) falls noch nicht vorhanden. |

---

## Betroffene Dateien (Punkt 5)

| Datei | Änderung |
|-------|----------|
| `TerminRegel.kt` | Optional: Feld `tageAzuL: Int` ergänzen (Default z. B. 7), falls L aus A abgeleitet werden soll. |
| `TerminRegelErstellenScreen.kt` | Feld „A + Zahl (Tage) = L-Termin“ hinzufügen; gesamtes Regel-Typ-UI (Label + Dropdown) entfernen; Callback `onRegelTypChange` entfernen. |
| `TerminRegelErstellenViewModel.kt` | State: `tageAzuL` hinzufügen, beim Speichern in Regel übernehmen; `regelTyp` beim Speichern fest auf z. B. WEEKLY setzen (keine Nutzerauswahl mehr). |
| `TerminRegelManager.kt` / Anwendung | Beim Anwenden der Regel auf Kunden: L-Datum = A-Datum + tageAzuL (falls Feld genutzt wird). |
| `strings.xml` | Ggf. neue Strings für Label/Hint „A + Zahl (Tage) = L-Termin“. |

---

**— Warte auf nächsten Punkt (Punkt 6). —**
