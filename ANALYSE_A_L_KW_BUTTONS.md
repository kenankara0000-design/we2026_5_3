# Analyse: A-, L- und KW-Buttons – Funktionsweise und Vorgaben

**Datum:** 30. Januar 2026

---

## 1. Vorgesehene Funktionsweise

### A (Abholung)
- **Anzeige:** Button nur sichtbar, wenn am angezeigten Tag ein Abholungstermin fällig oder überfällig ist (oder heute bereits erledigt).
- **Klick:** Erledigung der Abholung **nur am Tag „Heute“**.
- **Regel:** Abholung darf nur erledigt werden, wenn der Termin **heute** fällig ist (oder überfällig und heute angezeigt wird).

### L (Auslieferung)
- **Anzeige:** Button nur sichtbar, wenn am angezeigten Tag ein Auslieferungstermin fällig oder überfällig ist (oder heute bereits erledigt); nach KW-Erledigung am Tag wird L ausgeblendet.
- **Klick:** Erledigung der Auslieferung **nur am Tag „Heute“**.
- **Regel:** Auslieferung darf **nur nach erledigter Abholung** und nur, wenn der Termin **heute** fällig ist (oder überfällig und heute angezeigt wird).

### KW (Keine Wäsche)
- **Anzeige:** Button sichtbar, wenn am Tag Abholung **oder** Auslieferung fällig/überfällig ist.
- **Klick:** Erledigung **nur am Tag „Heute“**; setzt „Keine Wäsche“ und ggf. an diesem Tag auch A und L als erledigt (wenn heute fällig).

---

## 2. Ablauf im Code (Ist-Zustand)

### 2.1 Erste Prüfung – Binder (CustomerViewHolderBinder)

**Datei:** `adapter/CustomerViewHolderBinder.kt` → `setupClickListeners`

- **Bedingung:** `istHeute = (viewDateStart == heuteStart)`
  - `viewDateStart` = Start des **im Tourenplaner angezeigten** Tages (`displayedDateMillis`).
  - `heuteStart` = Start des **aktuellen** Tages (`System.currentTimeMillis()`).
- **Verhalten:** Wenn **nicht** heute angezeigt wird (`!istHeute`):
  - Toast: *„Termine können nur am Tag Heute erledigt werden.“*
  - Handler (onAbholung / onAuslieferung / onKw) wird **nicht** aufgerufen.
- **Ergebnis:** A/L/KW werden nur ausgeführt, wenn der Nutzer den Tag **Heute** anzeigt und tippt. Entspricht der Vorgabe.

### 2.2 Zweite Prüfung – Handler (TourPlannerCallbackHandler)

**Datei:** `tourplanner/TourPlannerCallbackHandler.kt`

#### A – handleAbholung
- Wenn bereits `customer.abholungErfolgt` → keine Aktion.
- Sonst: Coroutine starten, **Liste** mit `withContext(Dispatchers.IO) { listeRepository.getListeById(...) }` laden (für Listen-Kunden).
- **istTerminHeuteFaellig(customer, ABHOLUNG, heuteStart, liste)**:
  - Wenn `false` → Toast *„Abholung kann nur erledigt werden, wenn das Datum heute ist.“*, Buttons zurücksetzen, Ende.
  - Wenn `true` → Updates bauen (abholungErfolgt, abholungErledigtAm, ggf. faelligAmDatum), Firebase-Update, Erfolgs-Toast, Reload.
- **Ergebnis:** Erledigung nur, wenn der Abholungstermin **heute** fällig oder (überfällig und heute angezeigt) ist. Entspricht der Vorgabe.

#### L – handleAuslieferung
- Wenn bereits `customer.auslieferungErfolgt` → keine Aktion.
- Wenn **nicht** `customer.abholungErfolgt` → Toast *„Auslieferung kann nicht erledigt werden, solange die Abholung nicht erledigt ist.“*, Buttons zurücksetzen.
- Sonst: Coroutine, Liste laden, **istTerminHeuteFaellig(customer, AUSLIEFERUNG, heuteStart, liste)**:
  - Wenn `false` → Toast *„Auslieferung kann nur erledigt werden, wenn das Datum heute ist.“*, Ende.
  - Wenn `true` → Updates (auslieferungErfolgt, auslieferungErledigtAm, ggf. faelligAmDatum), Firebase, Erfolgs-Toast, Reload.
- **Ergebnis:** L nur nach A und nur bei heute fälligem/angezeigtem Auslieferungstermin. Entspricht der Vorgabe.

#### KW – handleKw
- Coroutine, Liste laden.
- **hatAbholungHeute** = istTerminHeuteFaellig(ABHOLUNG, …), **hatAuslieferungHeute** = istTerminHeuteFaellig(AUSLIEFERUNG, …).
- Wenn **weder** Abholung **noch** Auslieferung heute fällig → Toast *„KW (Keine Wäsche) nur an Abholungs- oder Auslieferungstag.“*, Ende.
- Sonst: Updates (keinerWäscheErfolgt, keinerWäscheErledigtAm); wenn hatAbholungHeute und A noch nicht erledigt → A-Updates; wenn hatAuslieferungHeute und L noch nicht erledigt → L-Updates. Firebase, Erfolgs-Toast, Reload.
- **Ergebnis:** KW nur an Tagen, an denen A oder L heute fällig ist; setzt ggf. A und L mit. Entspricht der Vorgabe.

### 2.3 Direkte Prüfung: „Hat Kunde am Datum einen Termin?“ (Kern der zweiten Prüfung)

**Neue API (TerminBerechnungUtils):**  
`hatTerminAmDatum(customer, liste, datum, typ): Boolean`  
→ Klare Ja/Nein-Antwort: Hat der Kunde **an genau diesem Datum** einen Termin des Typs (A oder L)? Kein „Suchen“ im Aufrufer; Fenster/Intervall ist nur noch Implementierungsdetail in der Util.

**Handler (TourPlannerCallbackHandler.kt) – istTerminHeuteFaellig:**

- **Eingabe:** customer, terminTyp, heuteStart, liste.
- **Logik:**
  1. **Normal fällig:** `TerminBerechnungUtils.hatTerminAmDatum(customer, liste, heuteStart, terminTyp)` → wenn `true`, Rückgabe `true`.
  2. **Überfällig:** Sonst werden vergangene Termine (z. B. 60 Tage zurück) geholt; es wird geprüft, ob ein überfälliger Termin dieses Typs **sollUeberfaelligAnzeigen(…, heuteStart, heuteStart)** erfüllt und noch nicht erledigt ist → dann `true`.
  3. Sonst → `false`.

**sollUeberfaelligAnzeigen** (TerminFilterUtils): Liefert true, wenn Fälligkeitstag = Anzeigedatum **oder** (Anzeigedatum = heute **und** Fälligkeit &lt; heute). Entspricht der Vorgabe.

---

## 3. Button-Sichtbarkeit (CustomerButtonVisibilityHelper)

- **A:** Sichtbar, wenn Abholung heute fällig, überfällig oder heute bereits erledigt.
- **L:** Sichtbar, wenn Auslieferung heute fällig, überfällig oder heute erledigt; **nicht** sichtbar, wenn KW am Tag bereits erledigt (L wird ausgeblendet).
- **KW:** Sichtbar, wenn A **oder** L am Tag fällig/überfällig/erledigt ist.

Damit sind Anzeige und Klick-Logik aufeinander abgestimmt.

---

## 4. Durchgeführte Anpassung: Direkte Prüfung

- **Neue Funktion:** `TerminBerechnungUtils.hatTerminAmDatum(customer, liste, datum, typ)`  
  Beantwortet direkt: Hat der Kunde **an diesem Datum** einen Termin des Typs (A oder L)? Intern wird ein sicherer Bereich um das Datum genutzt (Implementierungsdetail).
- **istTerminHeuteFaellig** nutzt nun diese direkte Prüfung statt eigenem Fenster/Suchen: zuerst `hatTerminAmDatum(customer, liste, heuteStart, typ)`; nur für den Überfällig-Fall werden vergangene Termine geholt und `sollUeberfaelligAnzeigen` geprüft.
- **Vorteil:** Klare API („Kunde + Datum?“), kein willkürliches Fenster im Handler, bessere Wartbarkeit.

---

## 5. Fazit

| Aspekt | Status |
|--------|--------|
| A/L/KW nur am Tag „Heute“ klickbar (Binder) | ✅ wie vorgesehen |
| A: Erledigung nur bei heute fälligem/angezeigtem Termin | ✅ wie vorgesehen |
| L: Nur nach A, nur bei heute fälligem/angezeigtem Termin | ✅ wie vorgesehen |
| KW: Nur an A- oder L-Tag, setzt ggf. A und L | ✅ wie vorgesehen |
| Listen-Kunden: Liste wird für Prüfung geladen | ✅ withContext(IO), keine runBlocking |
| Direkte Prüfung „Termin am Datum?“ (hatTerminAmDatum) | ✅ umgesetzt |

Die A-, L- und KW-Buttons verhalten sich damit wie vorgesehen; die Prüfung „heute fällig?“ erfolgt über die direkte API **hatTerminAmDatum(customer, liste, heuteStart, typ)**.
