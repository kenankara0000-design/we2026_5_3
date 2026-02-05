# Analyse: Kunde Elisenhof & Termine (ohne Code)

**Stand: 05.02.2026**

---

## Konfiguration „Elisenhof“ (laut Plan)

- **A (Abholung):** Di + So → 2× pro Woche
- **L (Auslieferung):** Di + So → 2× pro Woche  
  (Hinweis: In der aktuellen Logik gilt **L = A + Tage**; eigene L-Wochentage werden für die Erzeugung nicht genutzt – L entsteht nur als A-Datum + `tageAzuL`, z. B. 7 Tage.)

---

## Woher kommen die Termine für so einen Kunden?

1. **Intervalle (customer.intervalle)**  
   Wenn der Kunde Intervalle hat (erstes Abholungs-/Auslieferungsdatum, Wiederholung, Intervalltage): Daraus werden wiederholende A- und L-Termine berechnet (klassische Intervall-Logik).

2. **A-/L-Wochentage (effectiveAbholungWochentage)**  
   Wenn der Kunde A-Wochentage hat (z. B. Di=2, So=6):
   - Es werden **nur A-Termine** an diesen Tagen erzeugt (jeder Wochentag im Zeitraum, der in den A-Tagen liegt).
   - **L-Termine** entstehen ausschließlich als **A-Datum + tageAzuL** (aus dem ersten Intervall oder Standard 7). Es gibt keine separate Erzeugung aus „L-Wochentagen“; die L-Wochentage am Kunden werden für die Termin-Erzeugung aktuell nicht verwendet.

3. **Kombination**  
   Termine aus Intervalle und aus Wochentagen werden zusammengeführt; Doppel (gleiches Datum + Typ) werden vermieden.

Für „Elisenhof“ mit nur Di+So (ohne Intervalle oder mit leeren Intervallen) bedeutet das: A an jedem Di und So; L jeweils 7 Tage nach jedem A (also wieder Di und So), sodass faktisch 2× A und 2× L pro Woche an Di und So herauskommen.

---

## Anzeige im Tour-Planner (eine Karte pro Kunde pro Tag)

- **Überfällig:** Kunden mit überfälligen Terminen erscheinen zuerst, pro Kunde nur **einmal** (Deduplizierung nach `customer.id`).
- **bereitsAngezeigtCustomerIds:** Jeder Kunde, der bereits in „Überfällig“ geführt wird, wird an diesem Tag in Listen und im Normal-Bereich **nicht nochmal** angezeigt → vermeidet mehrere Karten für denselben Kunden am selben Tag.
- **Vergangenheit:** Wenn der angezeigte Tag in der Vergangenheit liegt, werden **keine** Listen-Karten und **keine** normalen Karten gezeichnet; nur Überfällige und ggf. Erledigte. Damit entfallen die früheren „weißen normalen Karten“ in der Vergangenheit.

---

## Frühere Probleme (laut Plan) und aktuelle Logik

| Problem | Ursache / aktuelle Lösung |
|--------|----------------------------|
| Sonntag keine Termine | Termine werden aus **allen** A-Wochentagen erzeugt (nicht nur dem ersten). Sonntag (6) wird mit erfasst, sofern in `effectiveAbholungWochentage`. |
| Dienstag 2+ Karten für denselben Kunden | Eine Karte pro Kunde pro Tag: Überfällige zuerst, dann `bereitsAngezeigtCustomerIds`; wer in Überfällig ist, kommt in Listen/Normal nicht nochmal. |
| Vergangenheit: weiße „normale“ Karten | Wenn `viewDateStart < heuteStart`: keine Listen- und keine Normal-Karten; nur Überfällig (und Erledigt). |

---

## Wichtige Dateien (nur zur Einordnung, kein Code)

- **Termin-Erzeugung:** `TerminBerechnungUtils` – `berechneTermineAusWochentagen` (nur A-Tage; L = A + tageAzuL), `berechneAlleTermineFuerKunde` (Intervalle + Wochentage zusammenführen).
- **Kunde:** `Customer` – `effectiveAbholungWochentage` / `effectiveAuslieferungWochentage`; Wochentage kommen aus `defaultAbholungWochentage`/`defaultAuslieferungWochentage` oder den einzelnen Default-Wochentagsfeldern.
- **Anzeige:** `TourDataProcessor` – Überfällig-Deduplizierung, `bereitsAngezeigtCustomerIds`, Vergangenheits-Logik (keine normalen Karten).

---

## Offener Punkt (aus Plan)

- **L = nur A + Tage:** Die Nutzerregel „L nur aus A + Tage, kein eigener L-Tag“ ist in der Wochentags-Logik umgesetzt. Eigene L-Wochentage am Kunden werden für die Termin-Berechnung nicht genutzt; ob sie woanders (z. B. Anzeige/Validierung) eine Rolle spielen, wäre ggf. separat zu prüfen.

---

## Echte Ursache (behoben): Wochentags-Ergänzung lief nur bei liste == null

**A am So steht.** Die Regel L = A+7 ist für Di und So dieselbe. Trotzdem: L für Di wurde erzeugt, L für So nicht.

**Grund im Code:** Die Logik „A aus Kunden-Wochentagen + L = A+7“ wurde **nur** ausgeführt, wenn **keine Liste** mitgegeben wurde (`liste == null`). Ihr habt die Listen-Logik entfernt – Tageslisten und manuell erstellte Listen sollen **keinen Einfluss** auf Termin-Logik/Erstellung haben. Im Code war aber weiterhin die Bedingung `liste == null` für die Wochentags-Ergänzung → dadurch wurde sie in Kontexten mit Liste nicht ausgeführt.

**Änderung:** Die Wochentags-Ergänzung wird jetzt **immer** ausgeführt, wenn der Kunde A-Wochentage hat (`effectiveAbholungWochentage.isNotEmpty()`), unabhängig von `liste`. Duplikate werden wie bisher über `existingKeys` vermieden.

---

## Frühere Vermutung: Fenster (nur Teil der Wahrheit)

**Kern:** L entsteht nur als „A + 7 Tage“. Wenn die Termin-Berechnung nur in einem **kurzen Zeitfenster** läuft (z. B. ab dem angezeigten Tag oder 1 Tag davor), werden nur A-Termine in diesem Fenster erzeugt. Der A-Termin, der **7 Tage vor** dem angezeigten Tag liegt, liegt dann **außerhalb** des Fensters → wird nie erzeugt → das zugehörige L am angezeigten Tag wird nie erzeugt.

Das trifft **alle Wochentage**, nicht nur Sonntag:  
- L am **So 15.02** braucht A am **So 08.02**.  
- L am **Di 17.02** braucht A am **Di 10.02**.  
Wenn das Fenster z. B. bei 15.02 oder 17.02 startet, fehlt jeweils der A-Termin 7 Tage davor, also fehlt das L an dem Tag.

**Warum wirkt es sich oft besonders bei Sonntag aus?**  
Je nach Nutzung: Wenn z. B. die App beim Öffnen „heute“ als Start nimmt und man oft in der Woche unterwegs ist, fällt der Blick oft auf Werktage. An Tagen wie Di 10.02 liegt der vorherige A (Di 03.02) evtl. noch im Fenster (z. B. wenn 1 Woche zurückgeblättert wird). Sonntage werden seltener angefahren, oder das Fenster ist so gewählt, dass der vorherige Sonntag-A nie im Fenster liegt → dann fehlt L am Sonntag **durchgängig**. Faktisch: Überall, wo mit **zu kurzem Fenster** (z. B. `startDatum = viewDateStart` oder `viewDateStart - 1 Tag`) gerechnet wird, fehlt L am angezeigten Tag.

**Wo passiert das konkret (ohne Code)?**  
- Anzeige/Prüfung „Hat der Kunde am Tag X einen Termin?“ nutzt teilweise ein Fenster von nur 1–2 Tagen um X.  
- Zählung/Badge „wie viele A/L heute“ nutzt teilweise ein Fenster, das am angezeigten Tag startet.  
In beiden Fällen wird der A von vor 7 Tagen nicht mitberechnet → L am aktuell angezeigten Tag wird nicht erzeugt.

**Lösungsrichtung:**  
Das Berechnungsfenster für die Wochentags-Logik muss **rückwärts** mindestens so weit reichen, dass der A-Termin, der das L am angezeigten Tag liefert, enthalten ist. Also Start mindestens **`viewDateStart − tageAzuL`** (z. B. 7 Tage zurück) oder konsistent z. B. 365 Tage zurück (wie an anderer Stelle bereits genutzt), damit an **jedem** Tag – inkl. Sonntag – auch die L-Termine erscheinen.
