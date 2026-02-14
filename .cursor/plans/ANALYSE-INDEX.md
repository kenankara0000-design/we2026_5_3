# Analyse-Index – TourPlaner 2026

**Zentrale Übersicht aller Analysen.** Diese Datei ist die Single Source of Truth für den Analyse-Status.

---

## Status-Übersicht

| Nr. | Analyse | Datei | Status |
|-----|---------|-------|--------|
| 01 | Design-Konsistenz | `analyse-01-design-konsistenz.md` | ✅ Erledigt |
| 02 | Code-Architektur | `analyse-02-code-architektur.md` | ✅ Erledigt |
| 03 | Kompatibilität & Dependencies | `analyse-03-kompatibilitaet.md` | ✅ Erledigt |
| 04 | UX/Layout (Screen-für-Screen) | `analyse-04-ux-layout.md` | ✅ Erledigt |
| 05 | User-Flow (Klick-Pfade) | `analyse-05-user-flow.md` | ✅ Erledigt |
| 06 | Informationsarchitektur | `analyse-06-informationsarchitektur.md` | ✅ Erledigt |
| 07 | Offline-Verhalten | `analyse-07-offline-verhalten.md` | ✅ Erledigt |
| 08 | Fehlerbehandlung (UX) | `analyse-08-fehlerbehandlung-ux.md` | ✅ Erledigt |
| 09 | Performance | `analyse-09-performance.md` | ✅ Erledigt |

**Erledigt:** 9 / 9 | **Offen:** 0 / 9

---

## Weitere Dateien

| Datei | Inhalt |
|-------|--------|
| `2026-02-13_21-47.massnahmenplan.md` | **Konsolidierter Maßnahmenplan** (73 Aufgaben, 8 Phasen, ~12–18 Wochen) – AKTUELL |
| `2026-02-13_23-29.ux-kundendetail-analyse.plan.md` | **UX Kundendetail** (3 Tabs) – Phase A+B ✅ erledigt, Phase C/D offen |
| `2026-02-13_21-02.plan.md` | Original-Tiefenanalyse (Befunde Abschn. 1–16, alter Maßnahmenplan Abschn. 17 → ersetzt durch konsolidierten Plan) |
| `2026-02-13_21-44.plan.md` | Alter Maßnahmenplan 04–09 → ersetzt durch konsolidierten Plan |
| `2026-02-13_21-02.design-vorher-nachher.html` | Visueller Vorher/Nachher-Vergleich (Design, HTML-Mockups) |

---

## Analyse-Reihenfolge (Ergebnis)

Alle Analysen wurden am 2026-02-13 durchgeführt. Screen-für-Screen-Ansatz: alle Aspekte (04–09) gleichzeitig pro Screen geprüft.

---

## Top-Befunde (Querschnitt über alle Analysen)

### 🔴 Kritisch

| Befund | Analyse | Details |
|--------|---------|---------|
| Back-Button fehlt in 12 von 24 Screens | 04, 05 | Nur System-Back; UX-Problem |
| Loading-State nie angezeigt (TourPlanner, CustomerManager) | 08 | `_isLoading` wird nie auf `true` gesetzt |
| Nur 3 von 24 Screens zeigen Offline-Status | 07 | Kundendetail, Erfassung, Listen etc. ohne Hinweis |
| SevDesk-Import ohne Offline-Hinweis | 07 | Einziger Screen der Netzwerk braucht |
| Stille Fehler (getSheetState==null, customerId ?: return) | 08 | Nutzer tippt, nichts passiert |

### 🟡 Mittel

| Befund | Analyse | Details |
|--------|---------|---------|
| Adresse/Telefon fehlt auf Tour-Karte | 06 | Für Fahrer relevant |
| Status (Pausiert/Ad-hoc) fehlt in Kundenliste | 06 | Nicht auf einen Blick erkennbar |
| Button-Höhen inkonsistent (48/56/64/72dp) | 04 | Kein einheitliches Design-System |
| Erledigung erfordert Zwischen-Dialog | 05 | 4–5 Klicks statt 2–3 |
| Kein Sync-Feedback | 07, 08 | Nutzer weiß nie ob Daten synchronisiert |
| forEach statt LazyColumn (ListeBearbeiten, BelegDetail) | 04, 09 | Bei vielen Einträgen ineffizient |
| Error-Anzeige inkonsistent (Toast vs. Snackbar vs. Inline vs. View) | 08 | 4 verschiedene Mechanismen |

### 🟢 Langfristig

| Befund | Analyse | Details |
|--------|---------|---------|
| Paging bei 500+ Kunden | 09 | CustomerManager + TourPlanner |
| derivedStateOf im TourPlanner | 09 | displayItems Recomposition |
| Filter-Performance bei 500+ Kunden | 09 | 7 Flows + Suche |
| contentDescription = null bei Icons | 04 | Barrierefreiheit |

---

## Umsetzungs-Reihenfolge (nach Abschluss aller Analysen)

| Prio | Screens | Warum zuerst |
|------|---------|-------------|
| 🔴 1 | Tourenplaner + Kunden-Karte | Täglich am meisten genutzt |
| 🔴 2 | Kundendetail (3 Tabs) | Zweithäufigstes Fenster |
| 🔴 3 | Hauptbildschirm | Einstieg in alles |
| 🟡 4 | Kundenliste/Manager | Suche, Filter |
| 🟡 5 | Erfassung + Belege | Geschäftskritisch |
| 🟡 6 | Listen (Erstellen, Bearbeiten) | Regelmäßig für Listenkunden |
| 🟢 7 | Neuer Kunde | Seltener |
| 🟢 8 | Preise, Statistiken, Einstellungen | Eher Büro |
| 🟢 9 | SevDesk-Import, Urlaub, Ausnahmen | Am seltensten |

---

## Wie diese Dateien zusammenhängen

1. **Dieser Index** (`ANALYSE-INDEX.md`) → Übersicht: welche Analysen gibt es, was ist erledigt, was offen
2. **Analyse-Dateien** (`analyse-01` bis `analyse-09`) → Befunde pro Thema (was wurde gefunden)
3. **Maßnahmenplan** (`2026-02-13_21-02.plan.md`, Abschnitt 17) → Konkrete Umsetzungsschritte (Phase 1–6, 39 Aufgaben, geschätzt 10–16 Wochen)
4. **Design-Vorher/Nachher** (`2026-02-13_21-02.design-vorher-nachher.html`) → Visueller Vergleich (HTML, im Browser öffnen)

### Aktueller Stand

- **Alle 9 Analysen erledigt** → Befunde stehen in den jeweiligen Dateien
- **Konsolidierter Maßnahmenplan** (`2026-02-13_21-47.massnahmenplan.md`): Phasen 1–8 ✅ (8.06, 8.10, 8.11 optional)
- **UX Kundendetail** (`2026-02-13_23-29.ux-kundendetail-analyse.plan.md`): Phase A+B ✅, Phase C/D offen

### Plan-Priorität (wichtig)

**Nur ein Plan pro Session.** Massnahmenplan hat Vorrang. UX Kundendetail Phase B ist erledigt; Phase C (Code-Duplizierung, Parameter-Reduktion) nur mit Freigabe.

### Nächster Schritt

→ **Build testen.** Danach optional: Massnahmenplan 8.06/8.10/8.11 oder UX Kundendetail Phase C (Freigabe erforderlich).

---

### Für neue Session

1. **Einstieg:** Diese Datei (`ANALYSE-INDEX.md`) lesen.
2. **Pläne:** `2026-02-13_21-47.massnahmenplan.md` und `2026-02-13_23-29.ux-kundendetail-analyse.plan.md` → Abschnitt **Session-Handoff** (oben).
3. **Nächster Schritt:** Build testen. Massnahmenplan Phase 8 optional (8.06 Strings, 8.10 LazyColumn, 8.11 derivedStateOf). UX Kundendetail Phase C nur mit Freigabe.
4. **Regel:** Nur ein Plan pro Session; keine parallele Arbeit.

*Letzte Aktualisierung: 2026-02-14 (Phase 8 + UX Phase B vollständig)*
