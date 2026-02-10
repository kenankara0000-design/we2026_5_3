# Bericht: Zustand der App (Februar 2026)

**Zweck:** Stand der App, Code-Verbesserungen, mögliche Fehler, TODOs, Logik-Vereinheitlichung, Design und Termin-Felder – sowie Ideen, wie die vielen Termin-/Intervall-Arten einfacher werden können.

---

## 1. Gesamtzustand der App

- **Manifest (PROJECT_MANIFEST.md):** Klar, Scope (bis 500 Kunden), Kernfunktionen und Technik sind dokumentiert.
- **Architektur:** Schwere Screens nutzen Coordinator + ViewModel (ARCHITEKTUR_ACTIVITY_COORDINATOR.md); Termin-Logik liegt in Utils/Processor, nicht in der Activity.
- **Termin-Berechnung:** Zentrale Stelle ist **TerminBerechnungUtils** (Intervall, Wochentag, listenTermine, MONTHLY_WEEKDAY). Cache in **TerminCache** (365 Tage), Filter in **TourDataFilter**.
- **Keine TODO/FIXME** in Kotlin-Dateien gefunden (grep).

---

## 2. Code-Verbesserungen (ohne Verhalten zu brechen)

| Thema | Befund | Vorschlag |
|-------|--------|-----------|
| **Deprecated Felder** | Customer: abholungDatum, auslieferungDatum, wiederholen, intervallTage, letzterTermin, wochentag, listenWochentag mit @Exclude/@Deprecated. | Beibehalten bis Migration abgeschlossen; keine Löschung ohne Prüfung aller Referenzen (Regel „nicht kaputt machen“). |
| **Customer.getFaelligAm()** | Bereits @Deprecated, delegiert an TerminBerechnungUtils.naechstesFaelligAmDatum. | Alle Aufrufer auf naechstesFaelligAmDatum/effectiveFaelligAmDatum umstellen, dann getFaelligAm() entfernen. |
| **Doppelte Intervall-Logik** | TourDataFilter: isIntervallFaelligAm / isIntervallFaelligInZukunft für **ListeIntervall**. TourPlannerDateUtils: isIntervallFaelligAm für ListeIntervall. | Eine gemeinsame Implementierung (z. B. in TerminBerechnungUtils oder kleinem IntervallBerechnungUtils) nutzen, beide Stellen darauf zugreifen. |
| **AgentDebugLog** | In TerminBerechnungUtils und CustomerDetailTermineTab für „agent log“. | Für Produktion optional konfigurierbar (z. B. nur in Debug-Build) oder entfernen, wenn nicht mehr gebraucht. |

---

## 3. Mögliche Fehler / Lücken

| Thema | Befund | Risiko |
|-------|--------|--------|
| **Fälligkeit nur aus berechneten Terminen** | **TourDataFilter.customerFaelligAm()** und **TourPlannerDateUtils.getFaelligAmDatumFuerAbholung/getFaelligAmDatumFuerAuslieferung** nutzen ausschließlich **TerminBerechnungUtils.berechneAlleTermineFuerKunde**. Diese liefert nur: Intervalle, Wochentag, listenTermine – **nicht** kundenTermine und **nicht** ausnahmeTermine. | Kunden **nur** mit kundenTermine oder ausnahmeTermine (z. B. Unregelmäßig/Ad-hoc) haben in diesen Funktionen **kein** Fälligkeitsdatum. Erledigung und „nächster Termin“ können dann 0 liefern oder falsch sein. |
| **Konsistenz „Termin am Datum“** | **TourDataFilter.hatKundeTerminAmDatum** prüft korrekt: zuerst ausnahmeTermine, dann kundenTermine, dann Cache (berechnet). | OK. |
| **TerminCache.getTermine365** | Liefert nur berechneAlleTermineFuerKunde (ohne kundenTermine/ausnahmeTermine). getTerminePairs365 merged sie für die Anzeige. | Wer nur getTermine365/getTermineInRange für „wann fällig?“ nutzt, sieht keine Kunden-/Ausnahme-Termine. |

**Empfehlung:**  
- Entweder **berechneAlleTermineFuerKunde** um optionale Merge-Logik für kundenTermine + ausnahmeTermine erweitern (mit Parameter oder separater Funktion „alle Termine inkl. Kunden/Ausnahme“),  
- **oder** an allen Stellen, die „nächstes Fälligkeitsdatum“ bzw. „fälliger A/L-Termin“ brauchen (TourDataFilter.customerFaelligAm, TourPlannerDateUtils.getFaelligAmDatum*, ggf. naechstesFaelligAmDatum), eine **einheitliche** Quelle nutzen, die **alle** Quellen (Intervall, Wochentag, Liste, kundenTermine, ausnahmeTermine) zusammenführt und daraus das nächste Datum ermittelt.

---

## 4. Termin-Berechnung vereinheitlichen

- **Eine zentrale Stelle:** TerminBerechnungUtils bleibt die zentrale Stelle für „berechnete“ Termine (Intervall, Wochentag, listenTermine, MONTHLY_WEEKDAY).
- **Lücke:** Kunden-Termine und Ausnahme-Termine sind **nicht** in berechneAlleTermineFuerKunde enthalten; der Merge passiert an verschiedenen Stellen (TerminCache.getTerminePairs365, TourDataFilter.hatKundeTerminAmDatum, ErledigungHandler/ButtonHelper prüfen listen separat).
- **Vereinheitlichung:**  
  - **Option A:** Neue Funktion z. B. `berechneAlleTermineFuerKundeVollstaendig(customer, liste, …)` die berechnete Termine + kundenTermine + ausnahmeTermine zu einer gemeinsamen Liste (z. B. TerminInfo oder ein gemeinsames DTO) zusammenführt. Alle „Fälligkeit“- und „Termin am Datum“-Logiken nutzen nur noch diese Funktion (und ggf. TerminCache darauf aufsetzen).  
  - **Option B:** TerminBerechnungUtils.berechneAlleTermineFuerKunde um optionalen Merge erweitern (z. B. `includeKundenUndAusnahmeTermine: Boolean = false`), und überall wo „vollständige“ Termine nötig sind, diesen Modus nutzen.  
So wird vermieden, dass kundenTermine/ausnahmeTermine bei Fälligkeit/Erledigung „vergessen“ werden.

---

## 5. Design und Termin-Felder vereinfachen

- **Viele Termin-Arten im Modell:**  
  - Beim **Kunden:** intervalle (CustomerIntervall), Wochentage (defaultAbholungWochentag/-Wochentage), listenTermine (über Liste), kundenTermine, ausnahmeTermine, verschobeneTermine, geloeschteTermine, faelligAmDatum.  
  - **Listen:** listenTermine (KundenTermin), intervalle (ListeIntervall), wochentagA, tageAzuL.  
  - **Regel-Typen:** TerminRegelTyp (WEEKLY, FLEXIBLE_CYCLE, ADHOC, MONTHLY_WEEKDAY); KundenTyp (REGELMAESSIG, UNREGELMAESSIG, AUF_ABRUF).
- **UI:**  
  - Kunde: CustomerDetailTermineTab (Nächster Termin, Kunden-Termine, Ausnahme-Termine, Termin-Regeln/Intervalle, Tour).  
  - Liste: ListeBearbeitenScreen mit Listen-Termine-Sektion (ListeBearbeitenListenTermineSection).  
  - Tour: Termin anlegen über TerminAnlegenUnregelmaessigActivity; Ausnahme über AusnahmeTerminActivity.

**Vereinfachung (konzeptionell):**  
- **Ein einheitliches „Termin-Datum + Typ (A/L)“-Modell** für Anzeige und Berechnung: z. B. überall TerminInfo (oder ein gemeinsames DTO) mit datum, typ (A/L), quelle (intervall | wochentag | liste | kundenTermin | ausnahme). So kann eine einzige Liste „alle Termine“ für einen Kunden/Liste/Kontext darstellen.  
- **Gemeinsame UI-Bausteine:** Eine wiederverwendbare Komponente „Termin-Liste (A/L mit Datum, optional Quelle)“ für Kunden-Termine, Ausnahme-Termine und Listen-Termine – mit unterschiedlichen Labels („Kunden-Termine“, „Ausnahme“, „Listen-Termine“), aber gleicher Darstellung und gleichem Löschen/Bearbeiten-Muster.  
- **Einstiege bündeln:** Statt mehrerer Wege („Termin anlegen“ unregelmäßig, „Ausnahme-Termin“, „Listen-Termin erstellen“) könnte ein **einheitlicher „Termin hinzufügen“**-Flow angeboten werden, der nach Kontext (Kunde vs. Liste) und Art (einmalig A, einmalig A+L, Ausnahme, Kunden-Termin) fragt und dann die richtige Liste (ausnahmeTermine, kundenTermine, listenTermine) befüllt.

---

## 6. Termin- und Intervall-Arten: Wie alles einfacher werden kann

Überblick der aktuellen Arten:

| Art | Wo | Daten |
|-----|-----|-------|
| **Regelmäßig (Intervall)** | Customer.intervalle (CustomerIntervall) | abholungDatum, auslieferungDatum, wiederholen, intervallTage, regelTyp (WEEKLY, …) |
| **Monatlich (n-ter Wochentag)** | CustomerIntervall mit regelTyp MONTHLY_WEEKDAY | monthWeekOfMonth, monthWeekday, tageAzuL |
| **Wochentag (ohne Intervall)** | Customer ohne intervalle, mit effectiveAbholungWochentage | Termine an jedem dieser Wochentage, L = A + tageAzuL |
| **Unregelmäßig / Ad-hoc** | kundenTyp UNREGELMAESSIG/AUF_ABRUF | Termine über kundenTermine oder ausnahmeTermine |
| **Kunden-Termine** | Customer.kundenTermine (KundenTermin) | datum, typ "A"/"L" – vom Kunden vorgegeben |
| **Ausnahme-Termine** | Customer.ausnahmeTermine (AusnahmeTermin) | datum, typ "A"/"L" – einmalige Sonderfälle |
| **Listen-Termine** | KundenListe.listenTermine (KundenTermin) | A/L für die ganze Liste, für alle Kunden der Liste |
| **Tour-Listen (Wochentag)** | KundenListe mit wochentag in 0..6 | wochentagA, tageAzuL; nächster A an diesem Tag |

**Strukturelle Duplikation:**  
- **AusnahmeTermin** und **KundenTermin** sind identisch (datum, typ "A"/"L"). Nur die Semantik unterscheidet sich (Ausnahme vs. kundenvorgegeben). Eine Vereinheitlichung wäre z. B.: ein gemeinsamer Typ `Einzeltermin(datum, typ, quelle: AUSNAHME | KUNDE)` und getrennte Listen am Customer, oder eine Liste `einzelTermine: List<Einzeltermin>` mit einem Feld „quelle“. Das würde UI und Merge-Logik vereinfachen (eine Liste „alle Einzeltermine“ durchlaufen).

**Vereinfachungsideen (ohne sofortige Umsetzung):**  
1. **Eine „Termin-Quelle“-Abstraktion:** Alle Termine (Intervall, Wochentag, Liste, Kunden-Termin, Ausnahme) als „Termin-Quelle“ modellieren; TerminBerechnungUtils fragt alle Quellen ab und merged zu einer sortierten Liste. Dann gibt es nur noch **eine** Stelle für „alle Termine für Kunde X am Tag T“.  
2. **Listen-Intervall vs. Kunden-Intervall:** ListeIntervall hat keine regelTyp/monthWeekday; KundenListe hat listenTermine und ggf. wochentagA/tageAzuL. Hier könnte man optional „ListeIntervall“ an CustomerIntervall annähern (gemeinsame Felder), um eine gemeinsame Berechnungslogik zu haben – Aufwand und Nutzen müssten abgewogen werden.  
3. **Einstiege:** Ein Dialog „Termin hinzufügen“ mit Auswahl: „Regelmäßig (Intervall) / Einmalig (Kunden-Termin) / Ausnahme“ und dann die gleichen Felder (Datum, A/L, ggf. A+L). Für Listen: „Listen-Termin hinzufügen“ mit Datum + A+L.

---

## 7. Kurzfassung

- **App-Stand:** Stabil, Architektur und Manifest stimmig, keine TODOs in Kotlin.
- **Verbesserungen:** Deprecated sauber abräumen, doppelte Intervall-Logik zusammenführen, Debug-Log optional.
- **Wichtiger Punkt:** Fälligkeit und Erledigung berücksichtigen aktuell **nicht** kundenTermine/ausnahmeTermine in allen Stellen; Vereinheitlichung über eine „vollständige“ Termin-Quelle wird empfohlen.
- **Vereinfachung:** Termin-Berechnung an einer Stelle inkl. Kunden-/Ausnahme-Termine; gemeinsame UI für Termin-Listen; optional einheitliches Modell für Einzeltermine (Kunden + Ausnahme) und ein gemeinsamer „Termin hinzufügen“-Flow.

**Letzte Aktualisierung:** Feb 2026
