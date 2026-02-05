# Termin-Anzeige & Berechnung – Stand & offene Punkte

**Stand: 05.02.2026**

---

## Was besprochen wurde

- Kunde „Elisenhof“: Di + So für A, Di + So für L (2× A, 2× L pro Woche).
- Probleme: Sonntag keine Termine; Dienstag 2+ Karten für denselben Kunden; in der Vergangenheit weiße „normale“ Karten obwohl nur Überfällig sinnvoll ist.

---

## Umgesetzte Änderungen

### 1. TerminBerechnungUtils (Termin-Erzeugung)
- **Wochentags-Ergänzung:** Termine werden zusätzlich aus allen **A-/L-Wochentagen** erzeugt (nicht nur aus dem ersten). Sonntag bekommt A+L; Dienstag A+L.
- **Hinweis:** Aktuell werden A-Tage und L-Tage **unabhängig** genutzt. Regel des Nutzers: **L = nur A + Tage** (kein eigener L-Tag). Das ist noch nicht strikt umgesetzt – ggf. später anpassen.

### 2. TourDataProcessor (Anzeige)
- **Eine Karte pro Kunde pro Tag:** Überfällig-Liste nach `customer.id` dedupliziert. Wer schon in Überfällig ist, erscheint nicht nochmal in Listen/Normal (`bereitsAngezeigtCustomerIds`).
- **Vergangenheit:** Wenn `viewDateStart < heuteStart` → keine Listen-Karten und keine normalen Karten; nur Überfällige (und ggf. Erledigte). Keine weißen „normalen“ Karten in der Vergangenheit mehr.

---

## Termin-Berechnung (vom Nutzer gewünscht, umgesetzt)

- **A-Termin:** Kommt vom Kunden (A-Tage), wird mit **Intervall +7** erstellt. Alle Termine starten mit A.
- **L-Termin:** Wird **nur** berechnet als **L = A + Tage** (z. B. 7). Keine eigenen L-Wochentage für die Erzeugung. Intervall hat nur Einfluss auf A.
- **Umsetzung:** In `TerminBerechnungUtils.berechneTermineAusWochentagen` werden nur noch A-Termine aus A-Tagen erzeugt; L-Termine entstehen ausschließlich als A-Datum + tageAzuL (aus Intervall oder Standard 7).

---

## Dateien, die geändert wurden

- `app/.../util/TerminBerechnungUtils.kt` – Wochentags-Termine, `berechneTermineAusWochentagen`, Einbindung in `berechneAlleTermineFuerKunde`.
- `app/.../tourplanner/TourDataProcessor.kt` – Überfällig-Deduplizierung, eine Karte pro Kunde pro Tag, Vergangenheit nur Überfällig/Erledigt.
- (zuvor: `Customer.kt` – `getDefaultAbholungWochentage`/`getDefaultAuslieferungWochentage` → `effectiveAbholungWochentage`/`effectiveAuslieferungWochentage` wegen JVM-Signatur-Clash.)

---

## Beim Weitermachen beachten

- Termin-Berechnung: Regel „L nur aus A + Tage“ ggf. in Wochentags-Logik umsetzen.
- Test: Eine Karte pro Kunde pro Tag (heute + Zukunft); in der Vergangenheit nur Überfällige.
