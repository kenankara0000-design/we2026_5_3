# Ergänzung zu Plan 2026-02-12_12-30: Option B + TourPreisliste → Standardpreisliste

Erstellt am: 2026-02-13

## Festlegung

- **Option B** wird umgesetzt: `kundenArt` „Tour" → „Listenkunden", `listeArt` „Tour" → „Listenkunden" in Firebase + Code.
- **TourPreisliste → Standardpreisliste**, da sie für **Listenkunden + Privat** gilt (neutraler, kein Kundentyp im Namen).

---

## TourPreisliste → Standardpreisliste: Betroffene Dateien

| Datei / Element | Änderung |
|-----------------|----------|
| `data/repository/TourPreiseRepository.kt` | → `StandardPreiseRepository.kt`; Firebase-Pfad `tourPreise` → `standardPreise` |
| `wasch/TourPreis.kt` | → `StandardPreis.kt` (data class) |
| `TourPreislisteActivity.kt` | → `StandardPreislisteActivity.kt` |
| `ui/wasch/TourPreislisteScreen.kt` | → `StandardPreislisteScreen.kt` |
| `ui/wasch/TourPreislisteViewModel.kt` | → `StandardPreislisteViewModel.kt` |
| Koin-Injection (AppModule / KoinModule) | `TourPreiseRepository` → `StandardPreiseRepository` |
| `util/AppNavigation.kt` | `toTourPreisliste()` → `toStandardPreisliste()` |
| `PreiseActivity.kt` | `onTourPreisliste` → `onStandardPreisliste` |
| `ui/main/PreiseScreen.kt` | `onTourPreisliste` → `onStandardPreisliste` |
| `ui/wasch/BelegeViewModel.kt` | `tourPreiseRepository` → `standardPreiseRepository` |
| `ui/wasch/WaschenErfassungViewModel.kt` | `tourPreiseRepository` → `standardPreiseRepository` |
| AndroidManifest.xml | `TourPreislisteActivity` → `StandardPreislisteActivity` |

**Code-Referenzen (alle umbenennen):**
- `getTourPreise()` → `getStandardPreise()`
- `getTourPreiseFlow()` → `getStandardPreiseFlow()`
- `setTourPreis()` → `setStandardPreis()`
- `removeTourPreis()` → `removeStandardPreis()`
- `tourPreise`, `tourPreiseFlow`, `parseTourPreis()` → `standardPreise`, `standardPreiseFlow`, `parseStandardPreis()`

**Strings (res/values/strings.xml):**

| Key | Aktuell | Neu |
|-----|---------|-----|
| `erfassung_menu_tourpreise` | „Preisliste Tour / Privat" | „Standardpreisliste" |
| `tour_preis_hinweis` | „Einheitliche Preise für Tour- und Privat-Kunden. …" | „Einheitliche Preise für Listenkunden und Privat-Kunden. …" |
| `tour_preis_leer` | „Noch keine Tour-Preise. …" | „Noch keine Standardpreise. …" |
| `tour_preis_add` | „Preis hinzufügen" | „Preis hinzufügen" (unverändert) |
| `tour_preis_article` | „Artikel wählen" | „Artikel wählen" (unverändert) |
| `tour_preis_keine_artikel` | „Alle Artikel haben bereits einen Tour-Preis." | „Alle Artikel haben bereits einen Standardpreis." |
| `tour_preis_keine_treffer` | „Keine passenden Artikel." | (unverändert) |
| `error_tourpreis_netto_brutto` | „Bitte Netto oder Brutto eingeben." | (unverändert oder → `error_standardpreis_netto_brutto`) |
| `dialog_preisliste_preis_loeschen_title` | „Preis löschen?" | (unverändert) |
| `dialog_preisliste_preis_loeschen_message` | „Preis für „%1$s" wirklich löschen?" | (unverändert) |

**Neue String-Keys (optional):**
- `erfassung_menu_standardpreise` = „Standardpreisliste"
- `standard_preis_hinweis` = „Einheitliche Preise für Listenkunden und Privat-Kunden. Bei Erfassung werden diese Preise automatisch verwendet, wenn keine Kundenpreise hinterlegt sind."
- `standard_preis_leer` = „Noch keine Standardpreise. Tippen Sie auf + um Preise hinzuzufügen."
- `standard_preis_keine_artikel` = „Alle Artikel haben bereits einen Standardpreis."

---

## Firebase-Migration

**Pfad:** `tourPreise/` → `standardPreise/`

**Strategie:**
1. Einmaliges Migrations-Script in `util/` (z. B. `StandardPreisMigration.kt`):
   - Prüfen, ob `standardPreise/` leer ist.
   - Wenn ja: alle Daten von `tourPreise/` nach `standardPreise/` kopieren.
   - SharedPreferences-Flag `standard_preis_migration_done` setzen.
2. Beim App-Start (MainActivity) Migration ausführen (analog zu `ListeToTourMigration`).
3. **Alte Daten:** `tourPreise/` kann nach erfolgreicher Migration manuell gelöscht werden (oder vorerst bestehen lassen für Rollback).

---

## Zusammenfassung aller Änderungen (Option B)

**1. Kundenart „Tour" → „Listenkunden"**
- Migration: `kundenArt` „Tour" → „Listenkunden" in Firebase (alle Kunden).
- Code: alle Vergleiche `kundenArt == "Tour"` → `== "Listenkunden"`.
- UI: Tab-Name „Listenkunden", Badge „L", Strings anpassen (siehe Plan 2026-02-12_12-30).

**2. Listen-Art „Tour" → „Listenkunden"**
- Migration: `listeArt` „Tour" → „Listenkunden" in Firebase (kundenListen).
- Code: alle Vergleiche `listeArt == "Tour"` → `== "Listenkunden"`.

**3. TourPreisliste → Standardpreisliste**
- Migration: Firebase-Pfad `tourPreise/` → `standardPreise/`.
- Code: Klassen, Methoden, Variablen umbenennen (siehe Tabelle oben).
- Strings: `tour_preis_*` → `standard_preis_*` oder neue Keys.

**Zukunftssicher:** Einheitliche, klare Begriffe in Daten + UI. Keine Verwechslung mit Tourenplaner.
