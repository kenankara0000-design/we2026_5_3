# Plan: listeArt "Liste" → "Tour" (einheitlich wie Kunden)

**Regel:** Immer zuerst prüfen, ob das neue System (Firebase) Verbindung hat bzw. Schreiben erfolgreich ist – **erst dann** Migration als erledigt markieren („altes“ Verhalten/Flag löschen). Keine Migration als „done“ setzen, wenn Schreiben fehlschlägt.

**Geltungsbereich:** Alle Stellen in der App + Firebase Realtime DB (Kundenlisten-Knoten `kundenListen`).

---

## 1. Betroffene Stellen (Übersicht)

| Bereich | Datei / Ort | Aktuell | Ziel |
|--------|-------------|---------|------|
| **Datenmodell** | `KundenListe.kt` | Default/Kommentar „Liste“ | „Tour“ |
| **Firebase DB** | Alle Listen mit `listeArt: "Liste"` | Wert "Liste" | Wert "Tour" |
| **Migration** | Neu: `ListeArtToTourMigration.kt` (oder Erweiterung) | – | Einmalig "Liste" → "Tour" in Firebase, **nur bei Erfolg** Done setzen |
| **Neue Listen erstellen** | `ListeErstellenScreen.kt` / `ListeErstellenViewModel.kt` | selectedType "Liste" | "Tour" |
| **Listen bearbeiten** | `ListeBearbeitenScreen.kt` | editListeArt "Liste" | "Tour" |
| **Wochentagslisten anlegen** | `KundenListenViewModel.kt` | listeArt = "Liste" | "Tour" |
| **Repository (Lesen)** | `KundenListeRepository.kt` | Default "Gewerbe", liest listeArt | Optional: beim Lesen "Liste" → "Tour" mappen (Rückwärtskompatibilität bis Migration gelaufen) |
| **Anzeige** | `KundenListenListenItem.kt` | zeigt liste.listeArt | Zeigt dann "Tour" (kein Code nötig, wenn Wert migriert) |

**Keine Änderung nötig:** TourDataProcessor etc. unterscheiden nach `wochentag` (0..6 vs. -1), nicht nach listeArt-Wert. Keine Logik vergleicht `listeArt == "Liste"`.

---

## 2. Reihenfolge der Umsetzung (mit Verbindungsregel)

### Phase A: Code auf "Tour" umstellen (ohne Migration)

- **A1** Datenmodell: `KundenListe.kt` – Kommentar und ggf. Default von "Liste" auf "Tour".
- **A2** UI/Speichern: Überall, wo **neu** der Wert gesetzt wird, "Tour" verwenden:
  - `ListeErstellenScreen.kt`: `selectedType == "Tour"`, `onTypeChange("Tour")`.
  - `ListeErstellenViewModel.kt`: Default `selectedType = "Tour"` (oder "Gewerbe" belassen; dritte Option aber "Tour").
  - `ListeBearbeitenScreen.kt`: `editListeArt == "Tour"`, `onClick = { editListeArt = "Tour" }`, Default bei null/leer ggf. "Tour" oder "Gewerbe".
  - `KundenListenViewModel.kt`: Bei Erstellung der Wochentagslisten `listeArt = "Tour"`.
- **A3** Lesen (Rückwärtskompatibilität): In `KundenListeRepository.parseKundenListe()`: wenn `listeArt == "Liste"` gelesen wird → intern als `"Tour"` weitergeben (damit alte DB-Werte bis zur Migration korrekt angezeigt/verwendet werden).

Ergebnis: App schreibt und liest überall "Tour"; bestehende Firebase-Daten mit "Liste" werden beim Lesen als "Tour" behandelt.

### Phase B: Migration in Firebase (Verbindungsregel: erst Erfolg, dann Done)

- **B1** Neue Migration (z. B. `ListeArtToTourMigration.kt` oder in bestehender Migrations-Struktur):
  1. SharedPreferences-Check: wenn Migration bereits als erledigt markiert → Ende.
  2. **Optional:** Verbindung prüfen (z. B. eine kleine Lese- oder Schreib-Operation zu Firebase; bei Fehler/Timeout Migration abbrechen, **nicht** Done setzen).
  3. Alle Listen aus Firebase laden (`getAllListen()` o. ä.).
  4. Listen filtern mit `listeArt == "Liste"`.
  5. Für jede solche Liste: `updateListe(id, mapOf("listeArt" to "Tour"))` und auf Erfolg warten (z. B. `await` / Result). Bei Fehler: Abbruch, **kein** Done setzen, nächstes App-Start erneut versuchen.
  6. **Erst wenn alle Updates erfolgreich:** SharedPreferences `liste_art_to_tour_migration_done = true` setzen („altes“ Verhalten/Flag löschen).

- **B2** Migration beim App-Start aufrufen (z. B. in `MainActivity` nach bestehenden Migrationen), gleicher Kontext wie `runListeToTourMigration` (Customer).

Wichtig: Kein `KEY_DONE = true`, wenn Firebase nicht erreichbar war oder ein Schreibvorgang fehlgeschlagen ist.

---

## 3. Firebase-Datenbank

- **Knoten:** `kundenListen / <listeId> / listeArt`
- **Aktuell:** teils `"Liste"`.
- **Ziel:** überall `"Tour"` (durch Migration einmalig überschrieben; neue Listen schreiben von vornherein "Tour").

---

## 4. Kurz-Checkliste

- [ ] `KundenListe.kt`: Default/Kommentar "Tour".
- [ ] `ListeErstellenScreen.kt`: "Liste" → "Tour" (Wert + Vergleich).
- [ ] `ListeErstellenViewModel.kt`: dritte Option "Tour".
- [ ] `ListeBearbeitenScreen.kt`: "Liste" → "Tour".
- [ ] `KundenListenViewModel.kt`: Wochentagslisten `listeArt = "Tour"`.
- [ ] `KundenListeRepository.kt`: beim Parsen "Liste" → "Tour" mappen.
- [ ] Neue Migration: Listen mit listeArt "Liste" in Firebase auf "Tour" schreiben; **nur bei Erfolg** Done setzen; optional vorher Verbindung prüfen.
- [ ] MainActivity: Migration starten.

---

## 5. Risiko / Rollback

- **Risiko:** Gering; nur String-Wert geändert, keine Logik nutzt `listeArt == "Liste"` für Tourenlogik.
- **Rollback:** Falls nötig: Code wieder auf "Liste" stellen; in Firebase kann man manuell zurück auf "Liste" setzen (Migration-Flag in Prefs zurücksetzen, wenn man Migration erneut laufen lassen will).

---

**Nach deiner Zustimmung können die Änderungen umgesetzt werden.**
