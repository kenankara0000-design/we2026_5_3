# Bericht: Anforderungen vs. aktueller App-Stand (TourPlaner 2026)

**Stand:** Feb 2026  
**Grundlage:** PROJECT_MANIFEST.md, Code-Analyse.  
**Ziel:** Prüfung der 14 formulierten Anforderungen und zusätzliche Vorschläge.

---

## 1. Anforderung vs. Umsetzung

### Punkt 1 – Buttons nur sichtbar, wenn kontextuell passend

**Anforderung:** Touren-Planner: Abholung, Auslieferung, Verschiebung, Urlaub-Buttons nur gegenüber dem Kunden sichtbar, wenn es fachlich zutrifft.

**Status:** ✅ **Umgesetzt.**  
`CustomerButtonVisibilityHelper` berechnet pro Kunde und Anzeige-Datum den Zustand des Erledigungs-Sheets. A-Button nur wenn Abholung heute/überfällig oder heute erledigt; L-Button analog; KW nur an A-/L-Tag; Verschieben wenn Termin am Tag oder verschobener Termin; Urlaub jederzeit („Urlaub eintragen“). Die Compose-UI (ErledigungSheetContent) zeigt Buttons nur an, wenn der Helper sie freigibt.

---

### Punkt 2 – Erledigt → in „Erledigte“ verschieben, unterhalb tagesaktueller Kunden

**Anforderung:** Wenn Abholung und Auslieferung erledigt sind, Kunde als erledigt markieren und in den Bereich „Erledigte“ verschieben; dieser soll unterhalb der tagesaktuellen Kunden stehen.

**Status:** ✅ **Umgesetzt.**  
TourDataProcessor/TourDataCategorizer sortieren: 1. Überfällig (oben), 2. Listen/Normal („Heute“), 3. Erledigt (unten). Erledigung setzt `abholungErfolgt`/`auslieferungErfolgt` und Zeitstempel; die Kategorisierung ordnet den Kunden dem Erledigt-Bereich zu.

---

### Punkt 3 – Kundenstamm: Überall Kunde anklickbar → Übersicht, Bearbeiten, Löschen nur im Bearbeitungsmodus mit Bestätigung

**Anforderung:** Überall wo man auf einen Kunden tippt: Übersicht (Name, Adresse, Telefon, Notizen, Abholung/Auslieferung/Verschiebung/Urlaub-Bereiche auswählbar). Bearbeiten möglich. Löschen nur im Bearbeitungsmodus; vor endgültigem Löschen Abfrage „Sind Sie sicher?“.

**Status:** ✅ **Umgesetzt.**  
- **Kundendetail:** CustomerDetailActivity/Screen mit Stammdaten- und Termine-Tab; Name, Adresse, Telefon, Notizen, Intervalle, Urlaub, Verschieben, Erledigungsstatus; Bearbeiten über „Bearbeiten“ → `isInEditMode`; Speichern/Löschen nur in diesem Modus.
- **Löschen:** Löschen-Button nur bei `isInEditMode` (CustomerDetailTopBar); beim Klick erscheint AlertDialog mit `dialog_delete_customer_title` und `dialog_delete_customer_message`; erst nach „Löschen“ wird `viewModel.deleteCustomer()` aufgerufen.
- **Von überall zum Kunden:** Tourenplaner-Karte → Kundendetail; Kundenmanager → Kundendetail; Ad-hoc-Slot auf Hauptbildschirm → Kundendetail. Einheitlicher Einstieg gegeben.

---

### Punkt 4 – App-Start: Kunden, Touren, Neuer Kunde; Kundenmanager mit Suche; überall Zurück-Button

**Anforderung:** Beim Öffnen: Buttons „Kunden“, „Touren“, „Neuer Kunde“. Kunden öffnet Kundenmanager mit allen Kunden. Im Kundenbereich Suchfunktion. Überall außer Hauptbildschirm ein Zurück-Button zur vorherigen Seite.

**Status:** ✅ **Weitgehend umgesetzt.**  
- **Hauptbildschirm (MainScreen):** Zeile „Kunden“ | „+ Neu Kunde“, großer Button „Tour Planner (n fällig)“; zusätzlich 2×2 mit Kunden Listen, Statistiken, Erfassung, Einstellungen. Die drei gewünschten Hauptaktionen (Kunden, Touren, Neu Kunde) sind klar vorhanden.
- **Kundenmanager:** CustomerManagerActivity mit Suche (Debounce), Filter, Liste aller Kunden; Klick öffnet Kundendetail.
- **Zurück:** TopAppBar mit Back-Icon auf CustomerDetail, TourPlanner, KundenListen, ListeBearbeiten, Einstellungen, ErfassungMenu, SevDeskImport, Statistics, MapView, AddCustomer, etc. Einheitlich umgesetzt.

**Hinweis:** Auf dem Hauptbildschirm gibt es zusätzlich die 2×2-Kacheln (Listen, Statistiken, Erfassung, Einstellungen). Wenn gewünscht nur „Kunden | Touren | Neu Kunde“ ohne weitere Kacheln, wäre das eine bewusste Layout-Entscheidung (aktuell mehr Einstiege).

---

### Punkt 5 – Tourenbereich: tagesaktuelle Kunden, Datum wechseln (Swipe/Pfeile), Überfällige oben rot

**Anforderung:** Im Tourenbereich tagesaktuelle Kunden sehen; per Swipe oder zwei Pfeile zwischen Datum wechseln (Vergangenheit/Zukunft). Nicht abgeholte Kunden am nächsten Tag in „Überfällig“ oberhalb der tagesaktuellen Kunden; Überfällige rot markieren.

**Status:** ✅ **Umgesetzt.**  
- TourPlannerScreen: Datum in TopBar, Pfeile „Vorheriger Tag“ / „Nächster Tag“ (content_desc_prev_day, content_desc_next_day); ViewModel `prevDay`/`nextDay`/`goToToday`.
- Sektionen: Überfällig (oben), dann Heute, dann Erledigt (unten). Überfällige Kunden werden mit Status-Badge „Überfällig“ und Farbe (status_badge_overdue, statusOverdue) rot/auffällig dargestellt (TourPlannerCustomerRow, colors.xml).
- Swipe: optional in der Implementierung; Pfeile sind vorhanden.

---

### Punkt 6 – Firebase, online, mindestens 500 Kunden

**Anforderung:** App läuft über Firebase online; soll mindestens für 500 Kunden ausgelegt sein.

**Status:** ✅ **Entspricht dem Plan.**  
- Firebase Realtime Database (nicht Firestore); Persistence für Offline; Auth, Storage für Fotos. PROJECT_MANIFEST §2: „Die App ist von Anfang an für bis zu 500 Kunden ausgelegt.“  
- Ab ca. 500 Kunden wird Paging/Lazy-Loading im Kundenmanager empfohlen (Backlog); aktuell werden alle Kunden geladen, Parsing im Hintergrund.

---

### Punkt 7 – Beim Erstellen: Touren-Intervalle festlegen (1, 2, 4 Wochen etc.)

**Anforderung:** Beim Anlegen eines Kunden soll man Touren-Intervalle festlegen können; ab dem Tag Intervall-Tage eingeben (z. B. jede Woche, alle 2 Wochen, alle 4 Wochen).

**Status:** ✅ **Umgesetzt.**  
- Customer hat `intervalle: List<CustomerIntervall>`; jedes Intervall hat `abholungDatum`, `auslieferungDatum`, `wiederholen`, `intervallTage` (1–365), `intervallAnzahl`.  
- AddCustomer legt Kunden an; Termine/Intervalle werden danach im Kundendetail (Termin anlegen, Intervalle bearbeiten) gepflegt. ListeBearbeitenScreen und CustomerDetailStammdatenTab/CustomerDetailTermineTab erlauben Intervalle und Intervall-Tage (z. B. 7, 14, 28 Tage). Regelmäßige Kunden mit wöchentlich/2-wöchentlich/4-wöchentlich werden so abgebildet.

---

### Punkt 8 – Urlaub: nur Termine im Urlaubszeitraum als Urlaub markieren, Rest unverändert

**Anforderung:** Wenn ein Kunde im Urlaub ist, sollen nur die Termine, die in den Urlaubszeitraum fallen, als Urlaub markiert werden; alle anderen Termine unverändert bleiben.

**Status:** ✅ **Umgesetzt.**  
- Urlaub über `urlaubVon`/`urlaubBis` oder `urlaubEintraege` (mehrere Zeiträume). Terminlogik in `TerminFilterUtils.istTerminImUrlaub(terminDatum, urlaubVon, urlaubBis)` bzw. effektive Urlaubseinträge pro Kunde.  
- Tourenplaner und Terminberechnung berücksichtigen Urlaub nur für Daten innerhalb des Urlaubszeitraums; Termine außerhalb bleiben normal (nicht verschoben, nicht gelöscht).

---

### Punkt 9 – Verschieben: nur diesen Termin oder optional alle restlichen

**Anforderung:** Beim Verschieben eines Termins soll nur dieser Termin betroffen sein; optional soll man wählen können, ob alle restlichen Termine mitverschoben werden.

**Status:** ✅ **Umgesetzt.**  
- `verschobeneTermine: List<VerschobenerTermin>` pro Kunde (Einzeltermine). Dialog: „Nur diesen Termin“ (dialog_verschieben_single) vs. „Alle zukünftigen Termine“ (dialog_verschieben_all); bei A/L getrennt wählbar (TourPlannerVerschiebenUrlaubHandler, CustomerDialogHelper). Einzeltermin-Verschiebung und „alle zukünftigen“ sind implementiert.

---

### Punkt 10 – Navigation zu Kunde mit Google Maps

**Anforderung:** Beim Tipp auf Kunde soll man direkt per Navigation (Google Maps) dorthin navigieren können.

**Status:** ✅ **Umgesetzt.**  
- Kundendetail: Adresse klickbar → Intent `google.navigation:q=...` mit Maps-Paket; bei fehlender Maps-App Hinweis (toast_keine_adresse, error_maps_not_installed). MapViewActivity öffnet Maps mit Wegpunkten für (gefilterte) Kunden. Direkte Navigation von der Kundenansicht aus ist möglich.

---

### Punkt 11 – Fotos: Thumbnails, Vergrößerung; im Bearbeitungsmodus Fotos aufnehmen

**Anforderung:** In der Kundenübersicht Fotos sichtbar (Abhol-/Auslieferungsort); im Bearbeitungsmodus Fotos aufnehmen; Thumbnails anzeigen; beim Klick Vergrößerung.

**Status:** ✅ **Umgesetzt.**  
- Customer hat `fotoUrls`; CustomerPhotoManager (Kamera/Galerie), StorageUploadManager, ImageUploadWorker. CustomerDetailFotosSection: Thumbnails; Klick öffnet Vollbild (showImageInDialog). Fotos können im Bearbeitungsmodus hinzugefügt werden (onTakePhoto → Kamera/Galerie).

---

### Punkt 12 – Modern, robust, stabil, einfach bedienbar

**Anforderung:** App soll modern, robust, stabil und einfach zu bedienen sein.

**Status:** ✅ **Architektur und UX darauf ausgerichtet.**  
- Modern: Jetpack Compose, Material 3, klare Struktur (ViewModel, Repository, Coordinator wo vorgesehen).  
- Robust: Offline-Persistence, NetworkMonitor, Fehlerbehandlung (Result, AppErrorMapper), Migrations beim Start.  
- Einfache Bedienung: klare Hauptbuttons, einheitliche Zurück-Navigation, Erledigung in einem Sheet, kontextabhängige Buttons (Punkt 1).  
- Weitere Verbesserungen (Fehlerbehandlung vereinheitlichen, Paging bei >500 Kunden, Barrierefreiheit) sind im Backlog (PROJECT_MANIFEST §14, IDEEN_ZUKUNFT.md).

---

### Punkt 13 – Analyse und zusätzliche Vorschläge

**Anforderung:** Gesamte Anforderungen analysieren und Bericht plus zusätzliche Vorschläge liefern.

**Status:** ✅ **Dieser Bericht.** Zusätzliche Vorschläge siehe unten.

---

### Punkt 14 – Gesamtziel: App für Touren, Auslieferung, Abholung mit Kundenstamm und nötigen Infos

**Anforderung:** Eine App für Touren, Auslieferung, Abholung mit Kundenstamm und allen nötigen Informationen.

**Status:** ✅ **Entspricht dem aktuellen Produkt.**  
TourPlaner 2026 ist genau dafür gebaut: Kundenstamm (Stammdaten, Intervalle, Urlaub, Fotos), Tourenplaner (tagesbezogen, Überfällig/Heute/Erledigt), Abholung/Auslieferung/KW-Erfassung, Verschieben, Urlaub, Listen, Firebase, Offline, Maps, Statistiken. Die 14 Punkte decken sich weitgehend mit dem vorhandenen Funktionsumfang.

---

## 2. Kurzfassung: Abweichungen / Optionen

- **Hauptbildschirm:** Aktuell 2×2 mit Listen, Statistiken, Erfassung, Einstellungen zusätzlich zu Kunden/Touren/Neu Kunde. Wenn gewünscht „nur drei Buttons“, könnte das Layout reduziert werden (Produktentscheidung).
- **Swipe für Datum:** Datum-Wechsel per Pfeile ist da; Swipe zwischen Tagen optional ergänzbar.
- **500+ Kunden:** Paging/Lazy-Loading im Kundenmanager ist empfohlen, aber noch Backlog (PROJECT_MANIFEST §14).

---

## 3. Zusätzliche Vorschläge

### Sofort nutzbar / kleine Verbesserungen

- **Login-Feedback:** Bei Anmeldefehler kurze Meldung + „Erneut versuchen“ statt sofort Schließen (bereits in ZUKUNFTSPLAENE.md).
- **Erledigungsbestätigung:** Nach „Abholung erledigen“ optional kurzer Hinweis „Auslieferung jetzt erledigen?“ (String `toast_abholung_dann_auslieferung` existiert bereits).
- **Barrierefreiheit:** contentDescription für alle relevanten Icons prüfen; Mindest-Touch-Target 48dp (IDEEN_ZUKUNFT.md).

### Mittelfristig

- **Paging im Kundenmanager:** Ab ca. 500 Kunden Lazy-Loading/Paging für bessere Performance und Stabilität.
- **Filter im Tourenplaner:** Nach Kundenart (G/P/L), Liste oder Region/PLZ filtern.
- **Erledigungsquote pro Zeitraum:** In Statistiken nicht nur „heute“, sondern z. B. „diese Woche“ / „dieser Monat“.

### Optional / Erweiterung

- **Benachrichtigungen:** z. B. „Morgen X Kunden fällig“ (WorkManager + Notification).
- **Kalender-Export:** Termine in Gerätekalender (iCal/CalDAV).
- **Echte Karten-UI:** In-App-Karte mit Markern und optional Route statt nur Maps-Intent (Backlog).
- **Rollen:** Mehrere Benutzer (Admin, Wäscherei, Fahrer) mit unterschiedlichen Rechten (ZUKUNFTSPLAENE.md).

---

## 4. Referenzen

- **PROJECT_MANIFEST.md** – Scope, Technik, Aktivitäten, Ideen-Status, Backlog.
- **IDEEN_ZUKUNFT.md** – Ideen für spätere Versionen.
- **ZUKUNFTSPLAENE.md** – Ideen & Vorschläge (nicht ohne Freigabe umsetzen).
- **.cursor/rules/zukunftsplan.mdc** – Regel: Zukunftsplan = in ZUKUNFTSPLAENE.md eintragen.

---

*Ende des Berichts.*
