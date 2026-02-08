# Plan: Tour-Planner Performance – 3-Tage-Fenster & Flüssigkeit

**Ziel:** App soll beim Start immer auf heute anzeigen, nur Daten für heute−1 / heute / heute+1 laden und beim Tagwechsel flüssig bleiben.

**Stand:** Feb 2026 – Analyse abgeschlossen, Umsetzungsplan erstellt.

---

## Prioritätsliste (Umsetzungsreihenfolge)

| Prio | Aufgabe | Phase | Status |
|------|---------|-------|--------|
| 1 | Start immer auf Heute | 1 | ✅ Erledigt |
| 2 | TourDataProcessor: 3-Tage-Fenster + 60 Tage Überfällig | 2.1 | ✅ Erledigt |
| 3 | TourDataFilter: 3-Tage + Überfällig 60 Tage | 2.2 | ✅ Erledigt |
| 4 | TourListenProcessorImpl: 3-Tage-Fenster | 2.3 | ✅ Erledigt |
| 5 | TourPlannerStatusBadge: 3-Tage-Fenster | 2.4 | ✅ Erledigt |
| 6 | TourPlannerDateUtils: Fenster reduzieren | 2.5 | ✅ Erledigt |
| 7 | +1-Tag-Vorberechnung (Cache, preload) | 3 | ✅ Erledigt |
| 8 | MainViewModel: getCustomersForTour + reduziertes Fenster | 4 | ✅ Erledigt |

---

## Phase 1: Startdatum auf „Heute“

**Ziel:** Tour-Planner öffnet immer auf heute, nicht auf letztem gespeichertem Datum.

| Nr | Aufgabe | Betroffene Dateien | Risiko |
|----|---------|-------------------|--------|
| 1.1 | Standard-Datum auf heute setzen; `last_view_date` nur optional für „Zurück“-Funktion | TourPlannerActivity.kt | Gering |
| 1.2 | Prüfen: Gibt es Nutzeranforderungen für „letzte Ansicht beibehalten“? Falls ja: Einstellung/Einstellungsoption vorsehen | (optional) | Gering |

---

## Phase 2: Term-Berechnungen auf 3-Tage-Fenster reduzieren

**Ziel:** Statt 365+730 Tagen pro Kunde nur noch heute−1, heute, heute+1 berechnen (für Tour-Planner-Kontext).

| Nr | Aufgabe | Betroffene Stellen | Risiko |
|----|---------|-------------------|--------|
| 2.1 | **TourDataProcessor:** `berechneAlleTermineFuerKunde` – Fenster von 365/730 auf 3 Tage umstellen (viewDateStart−1 bis viewDateStart+2) | TourDataProcessor.kt, private Methode berechneAlleTermineFuerKunde | Mittel – Überfällig-Logik prüfen |
| 2.2 | **TourDataFilter:** `istKundeUeberfaellig`, `hatKundeTerminAmDatum`, `customerFaelligAm` – Fenster auf 3 Tage (Überfällig: evtl. kleines Vergangenheitsfenster, z.B. 60 Tage) | TourDataFilter.kt | Mittel |
| 2.3 | **TourListenProcessorImpl:** Term-Berechnung von 365/730 auf 3-Tage-Fenster | TourListenProcessorImpl.kt | Mittel |
| 2.4 | **TourPlannerStatusBadge:** `compute()` – Fenster auf 3 Tage | TourPlannerStatusBadge.kt | Gering |
| 2.5 | **TourPlannerDateUtils:** `calculateAbholungDatum`, `calculateAuslieferungDatum`, `getNaechstesTourDatum` – Fenster prüfen und ggf. reduzieren | TourPlannerDateUtils.kt | Mittel |
| 2.6 | **Überfällig-Sonderfall:** Überfällige Termine können beliebig weit zurückliegen. Optionen: (A) Eigenes kleines Vergangenheitsfenster (z.B. 60 Tage) für Überfällig-Prüfung, (B) Erstes fälliges Datum pro Kunde cachen. Entscheidung dokumentieren. | Diverse | Mittel |

---

## Phase 3: +1-Tag-Vorberechnung (Flüssigkeit bei Tagwechsel)

**Ziel:** Beim Anzeigen von heute bereits morgen vorberechnen, damit Tagwechsel ohne Verzögerung wirkt.

| Nr | Aufgabe | Betroffene Stellen | Risiko |
|----|---------|-------------------|--------|
| 3.1 | Cache-Struktur für vorberechnete Tour-Daten (z.B. `Map<Long, TourProcessResult>` oder äquivalent) definieren | TourPlannerViewModel.kt (oder neues Modul) | Gering |
| 3.2 | Wenn heute angezeigt wird: Im Hintergrund Tour-Daten für morgen berechnen und cachen | TourPlannerViewModel.kt | Gering |
| 3.3 | Bei `nextDay()` oder Datum-Wechsel: Zuerst Cache prüfen; falls vorhanden, sofort anzeigen; sonst berechnen und cachen | TourPlannerViewModel.kt | Gering |
| 3.4 | Optional: Bei Anzeige von heute auch gestern (−1 Tag) vorberechnen für flüssiges Zurückblättern | TourPlannerViewModel.kt | Gering |

---

## Phase 4: MainViewModel entlasten (Startup)

**Ziel:** Tour-Fälligkeitsanzahl und Slots ohne vollständigen Kundenstamm berechnen, soweit möglich.

| Nr | Aufgabe | Betroffene Stellen | Risiko |
|----|---------|-------------------|--------|
| 4.1 | Tour-Count (`tourFälligCount`): Von `getAllCustomersFlow()` auf `getCustomersForTourFlow()` umstellen (nur Tour-Kunden für Count) | MainViewModel.kt | Gering – prüfen ob alle relevanten Kunden in customers_for_tour sind |
| 4.2 | Tour-Count: `getFälligCount` mit reduziertem Term-Fenster (nur heute) aufrufen – dazu TourDataProcessor/getFälligCount anpassen oder parametrisierbar machen | MainViewModel.kt, TourDataProcessor.kt | Mittel |
| 4.3 | Slot-Vorschläge: `tageVoraus` von 30 auf z.B. 7 oder 14 reduzieren, wenn ausreichend | MainViewModel.kt | Gering |

---

## Phase 5: Kundenliste (optional, mittelfristig)

**Ziel:** Bei >500 Kunden Paging/Lazy-Loading (bereits im Manifest genannt).

| Nr | Aufgabe | Betroffene Stellen | Risiko |
|----|---------|-------------------|--------|
| 5.1 | Anforderungen für Paging klären (Seitengröße, Lade-Strategie) | — | — |
| 5.2 | CustomerManagerViewModel / UI auf paged Data umstellen | CustomerManagerViewModel.kt, CustomerManagerScreen, ggf. CustomerRepository | Hoch – architektonisch |
| 5.3 | Firebase: Kein nativer Paging-Support – Optionen: (A) clientseitig limit/offset, (B) Pagination mit orderByKey/startAt, (C) lokale Room-DB als Cache | — | Hoch |

*Hinweis: Phase 5 ist laut Manifest für >500 Kunden empfohlen; bei kleineren Stämmen zurückstellen.*

---

## Reihenfolge der Umsetzung

1. **Phase 1** (schnell, niedriges Risiko)
2. **Phase 2** (Kern-Performance, muss sorgfältig getestet werden – Überfällig-Logik!)
3. **Phase 3** (Flüssigkeit, baut auf Phase 2 auf)
4. **Phase 4** (Startup-Verbesserung)
5. **Phase 5** (nur bei Bedarf)

---

## Abhängigkeiten & Tests

- **Phase 2:** Nach jeder Änderung (2.1–2.6) Regressionstests: Überfällig-Bereich, Heute-Bereich, Erledigt-Bereich, Listen-Kunden, Wochentagslisten.
- **Phase 3:** Manuell prüfen: Schneller Tagwechsel (← / →), kein Ruckeln.
- **Phase 4:** Main-Screen: Tour-Count und Ad-hoc-Slots korrekt nach Login und bei Datenänderung.

---

## Offene Punkte

- Soll `last_view_date` komplett entfallen oder als Option (z.B. Einstellung) erhalten bleiben?
- Überfällig: Maximale Vergangenheitstiefe (60 Tage? 90 Tage?) festlegen.
- Phase 5: Zeitpunkt für Paging-Implementierung (nach Erreichen von ~500 Kunden?).

---

**Letzte Aktualisierung:** Feb 2026
