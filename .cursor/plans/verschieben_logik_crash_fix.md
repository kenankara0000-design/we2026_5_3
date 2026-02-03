# Plan: Verschieben-Logik absturzfest korrigieren

## Analyse

Mögliche Absturzursachen nach der Umstellung auf `verschobeneTermine`:

1. **updateChildren() + List**  
   Firebase Realtime Database (Android) serialisiert bei `updateChildren(Map)` komplexe Werte anders als bei `setValue()`. Eine direkt übergebene **Liste** (`List<Map<String, Any>>`) kann zu „Failed to parse node“ oder zu einer Struktur führen, die beim späteren Lesen nicht als Liste erkannt wird und dann beim Deserialisieren abstürzt.

2. **Enum-Feld "typ"**  
   Es wird `"typ" to it.typ.name` (String) geschrieben. Beim Lesen macht `getValue(Customer::class.java)` daraus **kein** Enum – die Firebase-SDK konvertiert Strings nicht automatisch in Enums. Beim Befüllen von `VerschobenerTermin.typ` kann es zu einer Exception kommen.

3. **Zahlen als Double**  
   In der DB werden Zahlen oft als Double gespeichert. Beim Deserialisieren in `Long`-Felder kann das je nach SDK-Version zu Problemen führen (nur bei Bedarf als zweiter Schritt prüfen).

## Minimal nötige Korrekturen

### 1. Liste als Map mit Index-Keys speichern (nur Schreibseite)

**Datei:** [TourPlannerCallbackHandler.kt](app/src/main/java/com/example/we2026_5/tourplanner/TourPlannerCallbackHandler.kt)

- Statt `"verschobeneTermine" to serialized` (Liste) eine **Map mit Keys "0", "1", "2", …** übergeben, damit die Struktur eindeutig als Liste interpretierbar ist und `updateChildren()` sie zuverlässig übernimmt.
- Konkret: `serializeVerschobeneTermine` so anpassen, dass sie `Map<String, Map<String, Any>>` zurückgibt (Keys `"0"`, `"1"`, …). Beim Aufruf von `updateCustomer` dann `mapOf("verschobeneTermine" to serializeVerschobeneTermine(newList))` verwenden, wobei der Wert jetzt diese Map ist.

### 2. Nur originalDatum und verschobenAufDatum schreiben

**Datei:** [TourPlannerCallbackHandler.kt](app/src/main/java/com/example/we2026_5/tourplanner/TourPlannerCallbackHandler.kt)

- In der Serialisierung **keine** Felder `"typ"` und `"intervallId"` mehr mitschreiben.
- Beim Lesen bleiben dann die Defaults von `VerschobenerTermin` (typ = ABHOLUNG, intervallId = null) – das reicht für die aktuelle Verschieben-Logik und vermeidet Enum-Deserialisierungs-Abstürze.

### 3. Keine weiteren Änderungen

- Repository und `getValue(Customer::class.java)` unverändert lassen.
- Falls nach 1 und 2 weiterhin ein Crash beim **Laden** auftritt (z. B. Long/Double), kann als nächster Schritt nur das Feld `verschobeneTermine` im Repository manuell aus dem Snapshot gelesen und mit sicherem Double→Long in eine Liste umgewandelt werden. Das erst bei Bedarf.

## Betroffene Stelle

| Datei | Änderung |
|-------|----------|
| [TourPlannerCallbackHandler.kt](app/src/main/java/com/example/we2026_5/tourplanner/TourPlannerCallbackHandler.kt) | `serializeVerschobeneTermine`: Rückgabetyp auf `Map<String, Map<String, Any>>` umstellen (Keys "0", "1", …); pro Eintrag nur `originalDatum` und `verschobenAufDatum` (beide als Long) in der inneren Map speichern. Aufrufer verwenden weiterhin dieselbe Map für `"verschobeneTermine"`. |

## Kurzablauf nach der Korrektur

1. Nutzer verschiebt einen Termin → `handleVerschieben` baut wie bisher `newList` und ruft `serializeVerschobeneTermine(newList)` auf.
2. Statt einer Liste wird eine Map `{"0": {"originalDatum": …, "verschobenAufDatum": …}, …}` an `updateCustomer(…, mapOf("verschobeneTermine" to …))` übergeben.
3. Firebase speichert diese Map; beim nächsten Laden erkennt die SDK die numerischen Keys und kann daraus wieder eine Liste machen; `VerschobenerTermin` wird nur mit Long-Feldern befüllt, `typ`/`intervallId` bleiben Default – kein Enum-/String-Deserialisierungsfehler.
