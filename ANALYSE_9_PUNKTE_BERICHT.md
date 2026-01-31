# Analysebericht: 9 Punkte (UI, Navigation, Tourenplaner)

**Datum:** 30.01.2026  
**Auftrag:** Analyse der genannten Punkte, **ohne Code** – nur Ursachen und Lösungsrichtungen.

---

## 1. App Hauptbildschirm – Hintergrund schwarz, warum?

**Ursache:**  
Der Hauptbildschirm (MainScreen) setzt im Compose-Layout ausdrücklich `background(backgroundLight)` (Hellgrau #FAFAFA) auf die zentrale Spalte. Wenn trotzdem ein **dunkler/schwarzer Hintergrund** sichtbar ist, kommt das sehr wahrscheinlich vom **Theme**:

- Unter **values-night/themes.xml** ist `android:windowBackground` auf **#121212** (fast schwarz) gesetzt.
- Wenn das Gerät den **Dark Mode** nutzt (Systemeinstellung oder Batteriesparmodus), wird dieses Night-Theme verwendet. Die Fensterfläche hinter dem Compose-Inhalt ist dann dunkel.
- Wenn die Compose-Oberfläche nicht bis an den Rand geht oder es einen kurzen Moment beim Start gibt, in dem nur das Fenster sichtbar ist, wirkt der Bildschirm „schwarz“.

**Lösungsrichtung:**  
- Sicherstellen, dass der **wurzelnde Container** des MainScreen (z. B. die äußere Column/Box) explizit `Modifier.background(backgroundLight)` (oder die gewünschte Hellfarbe) hat und `fillMaxSize()` nutzt, damit keine Fensterfläche durchscheint.  
- Optional: Für die Hauptansicht das Night-Theme so anpassen, dass `windowBackground` dort ebenfalls hell bleibt, oder die Activity/Themes so wählen, dass der Startbildschirm immer das Hell-Theme nutzt.

---

## 2. „Neue Kunden erstellen“ – Hinweis wegen Termine, Hintergrund schwarz, warum?

**Hinweis-Termine:**  
Der Hinweis ist gewollt: In **AddCustomerScreen** gibt es eine **Card** mit dem Text zu Termin-Regeln (z. B. `add_customer_hint` / `add_customer_hint_text`). Das ist die geplante Info für den Nutzer.

**Hintergrund schwarz:**  
- **AddCustomerScreen** baut ein **Scaffold** mit **TopAppBar**, aber **ohne** gesetzte `containerColor` für den **Inhaltsbereich**.  
- Der Inhaltsbereich nutzt damit die **Standard-Surface-Farbe des Themes**.  
- Im **Dark Mode** (values-night) ist `colorSurface` auf **#1E1E1E** gesetzt → der Bereich unter der AppBar wirkt dunkel/schwarz.

**Lösungsrichtung:**  
- Für das Scaffold (oder die umschließende Column) des AddCustomer-Bildschirms eine **explizite helle Hintergrundfarbe** setzen (z. B. `surface_white` oder `background_light`), unabhängig vom System-Theme.  
- So bleibt der Bereich „Neuer Kunde“ auch bei Dark Mode hell; der Hinweis-Termine-Bereich behält seine aktuelle Card-Optik.

---

## 3. Neuen Kunden speichern – unterschiedliche Rücknavigation (Hauptbildschirm vs. Kundenmanager)

**Aktuelles Verhalten:**  
- **Von Hauptbildschirm** „Neuer Kunde“ → Speichern → **AddCustomerActivity** wird mit `finish()` geschlossen → Nutzer landet wieder auf dem **Hauptbildschirm**.  
- **Von Kundenmanager** „Neuer Kunde“ → Speichern → dieselbe **AddCustomerActivity** wird mit `finish()` geschlossen → Nutzer landet wieder im **Kundenmanager** (weil dieser die Activity gestartet hat).

**Warum das so ist:**  
Es gibt nur **eine** AddCustomerActivity. Wer sie startet (MainActivity oder CustomerManagerActivity), bleibt „darunter“ im Back-Stack. Nach `finish()` kommt man immer dorthin zurück. Das ist technisch konsistent, wirkt für den Nutzer aber uneinheitlich (einmal „zurück zur App-Startseite“, einmal „zurück zur Kundenliste“).

**Lösungsrichtung (ohne Code):**  
- **Option A – Einheitlich:** Immer nach erfolgreichem Speichern auf den **Hauptbildschirm** gehen: AddCustomerActivity mit `finish()` beenden und zusätzlich alle darüberliegenden Activities (z. B. Kundenmanager) per `setResult` + gezieltem Beenden oder mit einem „SingleTask“/„ClearTop“-Start der MainActivity so schließen, dass nur die Main sichtbar ist.  
- **Option B – Kontext beibehalten (aktuelles Verhalten dokumentieren):** So lassen und in der UI z. B. kurz anzeigen: „Kunde gespeichert. Sie bleiben im Kundenmanager“ bzw. „Sie sind auf der Startseite“.  
- **Option C – Explizite Wahl:** Beim Start von AddCustomerActivity einen Parameter mitgeben (z. B. „Zurück-zum-Hauptbildschirm“). Nach Speichern je nach Parameter entweder nur `finish()` (zurück zum Aufrufer) oder `finish()` + Start der MainActivity mit ClearTop (immer Hauptbildschirm).  

Empfehlung: Entweder A für einheitliches Erlebnis oder C für konfigurierbares Verhalten.

---

## 4. Kunden haben G/P/L-Buttons (Art) – Privat orange, Listen braun

**Aktueller Stand in den Ressourcen:**  
- In **colors.xml** sind für die G/P/L-Buttons definiert:  
  - **Gewerblich:** `button_gewerblich_glossy` (Blau),  
  - **Privat:** `button_privat_glossy` (Orange #E65100),  
  - **Liste:** `button_liste_glossy` (Braun #5D4037).  

Diese Farben sind für die **Tourplaner-/Adapter-Buttons** (Abholung, Auslieferung, Verschieben, …) hinterlegt.

**Im Kundenmanager (Compose):**  
- In **CustomerManagerScreen** wird in der **CustomerRow** nur ein **einzelner Buchstabe** (G / P / L) in einer Box angezeigt.  
- Dort ist aktuell **einheitlich** `colorResource(R.color.primary_blue)` als Hintergrund der Box gesetzt – also **nicht** nach Art (Privat/Liste) unterschieden.

**Ursache der Abweichung:**  
Die gewünschte Farbzuordnung (Privat = Orange, Listen = Braun) ist in den Farbressourcen vorhanden, wird aber in der **Kundenmanager-Liste** nicht verwendet; dort wird nur Blau genutzt.

**Lösungsrichtung:**  
- In der **CustomerRow** die Hintergrundfarbe der G/P/L-Box abhängig von `customer.kundenArt` setzen:  
  - „Privat“ → Orange (z. B. `button_privat_glossy` oder `status_warning`),  
  - „Liste“ → Braun (`button_liste_glossy`),  
  - „Gewerblich“ → Blau (wie bisher).  
- Kein neuer Code-Konzept nötig – nur Nutzung der bestehenden Farben an der richtigen Stelle.

---

## 5. Kundenmanager – beim Antippen eines Kunden: „Kunde nicht mehr vorhanden“

**Ursache:**  
- Beim Tippen wird **CustomerDetailActivity** mit **CUSTOMER_ID** aus dem angezeigten **Customer**-Objekt gestartet.  
- Die Detailseite lädt den Kunden per ID aus dem **CustomerRepository** (z. B. aus Firebase).  
- Die Meldung **„Kunde nicht mehr vorhanden“** erscheint, wenn der Kunde **nicht gefunden** wird (z. B. `currentCustomer == null` in CustomerDetailScreen).  

Häufige technische Ursache: Die **Customer-ID** ist beim Objekt aus der Liste **leer oder falsch**. Das kann passieren, wenn:

- Daten aus der **Firebase Realtime Database** mit `getValue(Customer::class.java)` gelesen werden und die **ID nicht** aus dem **Firebase-Key** (`child.key`) in das `Customer`-Objekt übernommen wird (z. B. weil `id` im Modell nicht automatisch aus dem Key gesetzt wird).

**Lösungsrichtung:**  
- Im **CustomerRepository** bei allen Stellen, an denen Kunden aus Firebase-Kindern erzeugt werden (z. B. `getAllCustomersFlow`, `getAllCustomers`, `getCustomerFlow`, `getCustomerById`), die **ID des Customer-Objekts explizit** aus dem **Firebase-Key** setzen (z. B. `child.key` bzw. die entsprechende ID des Knotens).  
- So haben die Kunden in der Kundenmanager-Liste immer eine gültige ID, und der Aufruf von CustomerDetailActivity mit dieser ID findet den gleichen Kunden im Repository.  
- Optional: In CustomerDetailActivity bei fehlender/leerer ID direkt eine Fehlermeldung anzeigen und die Activity beenden, damit nicht mit leerer ID geladen wird.

---

## 6. Kundenmanager – Kunden in CardView, schmal und Hintergrund schwarz

**Cards:**  
- Im Compose-**CustomerManagerScreen** werden die Kunden als **Card** (Material3) mit `fillMaxWidth()`, `surface_white` und abgerundeten Ecken dargestellt – das entspricht der gewünschten Card-Optik.

**„Hintergrund schwarz“:**  
- Das **Scaffold** im CustomerManagerScreen setzt **keine** eigene Hintergrundfarbe für den **Inhaltsbereich**.  
- Im **Dark Mode** (values-night) ist die Surface-Farbe dunkel (#1E1E1E) → der Bereich **zwischen und um die Cards** wirkt dunkel/schwarz. Die Cards selbst bleiben hell (surface_white).

**„Schmal“:**  
- Wenn die Cards „schmal“ wirken, kann das an **Padding** (z. B. 16.dp horizontal), an der **LazyColumn** oder an der **Breite** der Row-Inhalte liegen. Ohne genaue Geräte-/Layout-Angabe ist „schmal“ oft: zu viel Abstand, zu kleine Schrift oder die Card nimmt nicht die volle nutzbare Breite.

**Lösungsrichtung:**  
- **Hintergrund:** Dem Inhalts-Container des Kundenmanagers (z. B. Column/Box unter dem Scaffold) eine **explizite helle Hintergrundfarbe** geben (z. B. `background_light` oder `surface_light`), damit der Bereich um die Cards auch im Dark Mode hell ist.  
- **Schmal:** Layout prüfen: Padding reduzieren oder Breite der LazyColumn/Row so setzen, dass die Cards bis an den Rand des nutzbaren Bereichs gehen; Schriftgrößen und Abstände prüfen, damit die Karten nicht „schmal“ wirken.

---

## 7. Tourenplaner – „Erledigt“-Bereich klappt nicht aus (+ Zeichen)

**Ursache:**  
- Im Tourenplaner gibt es einen **ERLEDIGT**-Bereich, der als **SectionHeader** (mit „+“/„-“) dargestellt wird.  
- Beim Tippen wird **onToggleSection(SectionType.DONE)** aufgerufen und der **expandedSections**-State im ViewModel wird korrekt umgeschaltet.  
- **ABER:** Die **Liste der anzuzeigenden Items** kommt aus **TourDataProcessor.processTourData(...)**.  
- Dort wird für „Erledigt“ **nur ein einziges Item** erzeugt: der **SectionHeader** mit Titel „ERLEDIGT“ und der Liste der erledigten Kunden **im Objekt** (z. B. `SectionHeader(..., kunden = doneOhneListen)`).  
- **Es werden keine weiteren Listeneinträge** (z. B. einzelne **CustomerItem**s) für diese Kunden erzeugt, wenn die Sektion „expanded“ ist.  
- Der Parameter **expandedSections** wird in **processTourData** aktuell **nicht** genutzt, um bei „Erledigt“ ausgeklappt die Kunden als eigene Zeilen in die Item-Liste einzufügen.  
- Folge: Die UI zeigt „+“/„-“ und wechselt den State, aber unter dem Header erscheinen **keine** Kundenzeilen, weil es in der LazyColumn keine entsprechenden Einträge gibt.

**Lösungsrichtung:**  
- In **TourDataProcessor.processTourData** die **expandedSections** auswerten:  
  - Wenn **SectionType.DONE** in **expandedSections** enthalten ist, **nach** dem Eintrag **SectionHeader("ERLEDIGT", ...)** für jeden Kunden in **doneOhneListen** einen Eintrag **ListItem.CustomerItem(...)** an die **items**-Liste anhängen.  
- So enthält die Liste bei „Erledigt ausgeklappt“ zuerst den Header, dann die einzelnen Kundenzeilen; die LazyColumn kann sie alle anzeigen.  
- Analog prüfen, ob „Überfällig“ oder andere Sektionen mit Header + ausklappbaren Kunden dieselbe Logik brauchen (Header + bei expanded die zugehörigen CustomerItems einfügen).

---

## 8. Tourenplaner – Immer mit „Heute“ starten, „Heute“-Button orange

**Start mit „Heute“:**  
- In **TourPlannerActivity** wird in **onCreate** bereits **setSelectedTimestamp** mit dem **heutigen Datum** (0 Uhr) aufgerufen.  
- Der Tourenplaner startet damit **mit dem Tag „Heute“**. Das Verhalten ist also bereits so umgesetzt.

**„Heute“-Button orange:**  
- Es gibt einen **„Heute“-Button** in der Toolbar, dessen Farbe von **pressedHeaderButton** abhängt:  
  - Wenn **pressedHeaderButton == "Heute"**, ist die Button-Farbe **statusWarning** (Orange).  
  - **pressedHeaderButton** wird auf **"Heute"** gesetzt, **nur** wenn der Nutzer explizit auf den **„Heute“-Button** tippt (onToday).  
- Beim **ersten Öffnen** ist **pressedHeaderButton** in der Regel **null** → der Button ist **blau**, obwohl das angezeigte Datum schon „Heute“ ist.  
- Gewünscht ist vermutlich: **Immer wenn das angezeigte Datum „Heute“ ist, soll der „Heute“-Button orange sein** – unabhängig davon, ob gerade darauf geklickt wurde.

**Weitere Anforderung (aus der Beschreibung):**  
- Wenn man das Datum wechselt (z. B. Vor/Zurück), soll der „Heute“-Button **nicht** orange sein.  
- Wenn man auf „Heute“ tippt, soll er **orange** werden und die Ansicht auf **heute** springen.

**Lösungsrichtung:**  
- Die Farbe des „Heute“-Buttons **nicht** nur von **pressedHeaderButton** abhängig machen, sondern von der **Logik:**  
  - **Orange,** wenn `selectedTimestamp` dem **heutigen Tag** entspricht (Datum vergleichen, z. B. nur Kalendertag).  
  - **Blau,** wenn ein anderes Datum gewählt ist.  
- **pressedHeaderButton** kann weiterhin beim Klick auf „Heute“ gesetzt werden (z. B. für kurzes Feedback), die **Darstellung** (Orange/Blau) sollte aber vom **tatsächlich angezeigten Datum** abhängen.  
- Beim **Öffnen** der Activity ist `selectedTimestamp == heute` → Button von Anfang an orange. Nach Wechsel auf gestern/morgen wird er blau; nach erneutem Tipp auf „Heute“ wieder orange und Datum springt auf heute.

---

## 9. Zusammenfassung

| Nr. | Thema | Kernursache | Richtung Lösung |
|-----|--------|-------------|------------------|
| 1 | Hauptbildschirm schwarz | Dark-Theme: windowBackground #121212; evtl. kein flächendeckender heller Hintergrund | Hell-Hintergrund auf Wurzel-Container; ggf. Theme anpassen |
| 2 | Neuer Kunde – Hinweis schwarz | Scaffold-Inhalt nutzt Theme-Surface → im Dark Mode dunkel | Helle Hintergrundfarbe für AddCustomer-Inhalt setzen |
| 3 | Verschiedene Rücknavigation nach Speichern | Eine AddCustomerActivity; Rückkehr immer zum Aufrufer (Main vs. Kundenmanager) | Einheitliche Regel (z. B. immer Hauptbildschirm) oder expliziter Parameter beim Start |
| 4 | G/P/L – Privat orange, Listen braun | Farben in colors.xml vorhanden; in CustomerManagerScreen wird nur Blau verwendet | In CustomerRow Hintergrund der G/P/L-Box nach kundenArt setzen (Privat=Orange, Liste=Braun, G=Blau) |
| 5 | Kundenmanager – „Kunde nicht mehr vorhanden“ | Customer-ID aus Liste leer/falsch (z. B. Firebase-Key nicht ins Customer-Objekt übernommen) | Im Repository bei Firebase-Lesen ID aus child.key setzen; ggf. leere ID in Detail-Activity abfangen |
| 6 | Kundenmanager Cards schmal, Hintergrund schwarz | Kein heller Hintergrund im Scaffold-Inhalt (Dark Mode); „schmal“ evtl. Padding/Layout | Helle Hintergrundfarbe für Inhaltsbereich; Layout/Padding für Card-Breite prüfen |
| 7 | Erledigt-Bereich klappt nicht aus | processTourData liefert nur SectionHeader für ERLEDIGT, keine CustomerItems bei expanded; expandedSections ungenutzt | In processTourData bei expanded SectionType.DONE die erledigten Kunden als CustomerItems in die Liste einfügen |
| 8 | Tourenplaner Heute / Orange | Start bereits „Heute“; Button-Farbe hängt nur von Klick ab, nicht vom angezeigten Datum | „Heute“-Button orange, wenn selectedTimestamp == heute; sonst blau |

---

*Ende des Berichts. Kein Code – nur Analyse und Lösungsrichtungen.*
