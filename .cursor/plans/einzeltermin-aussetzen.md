# Plan: Einzelne Termine aussetzen + Badge „war ausgesetzt“ / „war KW“

**Ziel:** Einzelnen Termin als „ausgesetzt“ markieren können. In der Logik gleich wie KW: Wenn A-Termin ausgesetzt (oder KW) ist, wird der **nächste L-Termin als A-Termin** angezeigt, mit Badge „war ausgesetzt [Datum]“ bzw. „war KW [Datum]“.

---

## 1. Datenmodell & Persistenz

- **Neues Modell:** `AusgesetzterTermin(datum: Long, typ: TerminTyp)` (oder nur ABHOLUNG/AUSLIEFERUNG pro Datum).
- **Customer:** `ausgesetzteTermine: List<AusgesetzterTermin> = emptyList()` – mit `@Exclude`, in Firebase manuell lesen/schreiben (wie `verschobeneTermine`).
- **CustomerRepository:** In `parseCustomerSnapshot` Liste aus `ausgesetzteTermine` lesen; beim Update/Save serialisieren (Keys "0", "1", … mit datum, typ).

---

## 2. Logik (gleich für „aussetzen“ und KW)

- **Regel:** Ist der **fällige A-Termin** am Tag D **ausgesetzt** (Eintrag in `ausgesetzteTermine`) oder **KW** (`keinerWäscheErledigtAm == D`), dann gilt:
  - Dieser A-Termin zählt als „erledigt“ für die Kette.
  - Der **nächste** angezeigte Termin ist der **L-Termin** (L = A + tageAzuL), wird aber **als A angezeigt** (Button A, Badge, Überfällig-Logik für „A“).
  - Badge-Text: „war ausgesetzt [Datum]“ oder „war KW [Datum]“ (D = Datum des ausgesetzten/KW-A-Termins).

- **Wo anwenden:**
  - **TourPlannerDateUtils** (oder zentrale Helper): Bei Berechnung „nächster A-Termin“: wenn der nächste A am Datum D liegt und (D in `ausgesetzteTermine` für A **oder** D == `keinerWäscheErledigtAm`), dann stattdessen den **L-Termin** (D + tageAzuL) als „A“ zurückgeben und Zusatzinfo für Badge (D, warKW) mitliefern.
  - **CustomerButtonVisibilityHelper / ErledigungSheetState:** Nutzt diese „next A“-Logik; wenn Badge-Info gesetzt ist, Badge „war ausgesetzt …“ / „war KW …“ anzeigen.
  - **TourDataProcessor / TourDataFilter:** Beim Füllen von Touren/Karten und Fällig/Überfällig dieselbe Logik: „A“ kann der (wegen Aussetzen/KW) als A behandelte L-Termin sein.

- **KW bleibt:** Einzelnes Datum `keinerWäscheErledigtAm`; keine Liste nötig. Logik „A überspringen → nächster L als A“ wird für **aussetzen** und **KW** gemeinsam umgesetzt.

---

## 3. UI

- **Aktion „Aussetzen“:** Im Tourenplaner (oder Kunden-Detail) an einem A-Termin wählbar (z. B. Button „Aussetzen“ oder Menüpunkt). Beim Tipp: Eintrag `(datum, ABHOLUNG)` in `ausgesetzteTermine` speichern (Repository Update).
- **Badge:** Wo heute „A“/„L“/„A+L“ angezeigt wird, bei „A die aus L wird“ zusätzlich Badge „war ausgesetzt [Datum]“ oder „war KW [Datum]“ (Strings anlegen).

---

## 4. Reihenfolge Umsetzung

1. **AusgesetzterTermin + Customer.ausgesetzteTermine** (inkl. @Exclude), Repository lesen/schreiben.
2. **Zentrale Helper-Funktion:** „Nächster effektiver A-Termin“ inkl. Regel: A ausgesetzt/KW → nächster = L, angezeigt als A + Badge-Info (Datum, warKW). Diese Helper von TourPlannerDateUtils und ggf. TerminBerechnungUtils/TourDataFilter nutzen.
3. **TourPlannerDateUtils:** `calculateAbholungDatum` (und ggf. Überfällig) so erweitern, dass sie die neue „A = ausgesetzter L“-Logik und Badge-Info zurückgeben bzw. nutzbar machen.
4. **CustomerButtonVisibilityHelper / ErledigungSheetState:** Badge-Info verwenden, Strings „war ausgesetzt …“, „war KW …“.
5. **TourDataProcessor / TourDataFilter:** Fällig/Überfällig und Karteninhalt an dieselbe Logik anbinden.
6. **UI:** Button „Aussetzen“ am A-Termin (Sheet oder Karte), Speichern in `ausgesetzteTermine`.

---

## 5. Risiko

- Viele Stellen (DateUtils, Helper, Processor, Filter, Sheet) müssen einheitlich die gleiche „A ausgesetzt/KW → L als A mit Badge“-Regel nutzen. Eine zentrale Helper-Klasse/Util reduziert Duplikate und Fehler.
