# Zukunftspläne – TourPlaner 2026

**Regel:** Punkte, die mit „Zukunft“ oder „später bauen“ markiert werden, werden hier eingetragen. **Nichts davon wird ohne ausdrückliche Freigabe umgesetzt** (vgl. `.cursor/rules/zukunftsplan.mdc`).

*Quellen: REFERENZ_ANALYSEN_UND_BERICHTE.md, PROJECT_MANIFEST.md. Neue Ideen jederzeit unter „Ideen & Vorschläge“ ergänzen.*

---

## Ideen & Vorschläge (noch nicht umgesetzt)

*Einträge werden hier ergänzt, sobald bei einer Umsetzung „Zukunftsplan“ gesagt wird.*

### Bereits als „Zukunft“ markiert

- **Kalender-Export:** Termine in Gerätekalender exportieren (iCal/CalDAV)
- **Onboarding:** Bei leerer App kurze Anleitung „Erstelle deine erste Kundenliste“ mit Navigation

### Login & Nutzer

- **Benutzer und Rollen (3 Rollen, vordefinierte Benutzer):**
  - 3 Benutzer anlegen: 1 = Admin, 2 = Wäscherei, 3 = Fahrer. Vordefiniert (nicht erst zur Laufzeit anlegen).
  - Jeder Benutzer bekommt ein Kennwort (Login mit E-Mail/Name + Passwort).
  - Rollenbasierte Sicht und Bearbeitung: Admin = vollen Zugriff; Wäscherei = Erledigungen, Termine (evtl. keine Stammdaten); Fahrer = Tourenplaner, Erledigungen, Karte (evtl. keine Stammdaten/Termin-Regeln).
  - Konkrete Rechte pro Rolle bei Umsetzung festlegen.

### Optionale Features

- **Echte Karten-UI:** In-App mit Markern/Route statt nur Maps-Intent.
- **Tour-Reihenfolge:** Drag & Drop speichern und an Karten-App übergeben (Wegpunkte in Reihenfolge).
- **Benachrichtigungen:** Push „Morgen X Kunden fällig“; Erinnerung für pausierte Kunden („Kunde X ist seit Y Tagen pausiert“).
- **Paging im Kundenmanager:** Ab ~500 Kunden (Seitengröße 50–100; Suche/Filter mit Paging).

### History-Log & Backlog

- **History-Log für Kunden:** Alle Änderungen protokollieren (wer, wann, was). Empfehlung: zentrales Log für alle Kunden, jeder Eintrag mit Kunden-ID – pro Kunde filterbar, global zeitlich sortiert.

### Berichte & Export

- Monatliche Berichte (PDF/CSV)
- Export für Buchhaltung: Liste erledigter Abholungen/Auslieferungen
- Tour-Last-Anzeige (Kapazität pro Tag)

### Technik & UX

- **Performance:** Lazy Loading, Pagination bei vielen Kunden (>500).
- **Accessibility:** contentDescription für alle Icons, Mindest-Touch-Target 48dp, Kontrast prüfen.
- **Dark Mode:** Optional aktivierbar (derzeit global deaktiviert).
- **Offline-Anzeige:** Hinweis wenn keine Verbindung, Änderungen werden synchronisiert (bereits vorhanden; optional: Echtzeit-Sync-Status „X Änderungen ausstehend“ oder Fortschrittsbalken).
- **LoadState durchgängig:** ViewModels mit LoadState&lt;T&gt; (Loading/Success/Error) statt ad-hoc isLoading/errorMessage.
- **AgentDebugLog:** Nur in Debug-Build aktivieren (nicht in Release).
- **LoginActivity:** Sichtbare Login-UI in Compose (aktuell [ ]).

### Technische Refactorings

- Alle Repositories auf FirebaseRetryHelper umstellen
- Alle Activities auf AppNavigation umstellen
- Alle Log/printStackTrace → AppLogger ersetzen
- Weitere ViewModels von BaseViewModel erben
- Crashlytics in AppLogger integrieren
- AppErrorMapper-Texte in strings.xml auslagern
- Dateigröße/Wartbarkeit: Sehr große Screens/ViewModels in kleinere Teile aufteilen

### Termin-Logik & Domain

- **Vollständige Termin-Quelle:** Zentrale API für alle Termine (inkl. kundenTermine, ausnahmeTermine); alle Fälligkeitsstellen darauf umstellen (REFERENZ_ANALYSEN_UND_BERICHTE.md Abschn. 2 und 9).
- **Termin-Logik vereinfachen:** Einheitliches „Termin-Datum + Typ (A/L)“-Modell (z. B. TerminInfo mit Quelle); ein Dialog „Termin hinzufügen“ (Regelmäßig / Einmalig / Ausnahme); Listen: „Listen-Termin hinzufügen“ mit Datum + A+L.
- **Termin-Regeln erweitern:** Feiertage, Schließtage, „jeden 2. Montag im Monat“.
- **Listen-Vorlagen:** Vorgefertigte Regeln für typische Listen (z. B. „Wöchentlich Mo/Do“) mit einem Klick anwenden.
- **Filter im Tourenplaner:** Nach Kundenart/Liste/Region/PLZ.
- **Erledigungsquote pro Zeitraum:** Woche/Monat, nicht nur „heute“.

### Domain & Erweiterungen

- **Auftrags-/Mengenverwaltung:** Pro Termin Mengen (Säcke, Kilo) oder Checkliste; Auswertung in Statistiken.
- **Rechnungsrelevanz:** Markierung „abgerechnet“, Verknüpfung zu Belegen; Export für Buchhaltung.
- **Kunden-Kommunikation:** E-Mail/SMS-Erinnerung an Kunden vor Abholung (optional mit Opt-in).
- **Check-in am Kunden:** Beim Erledigen optional „Angekommen“- und „Fertig“-Zeitpunkt (Zeiterfassung pro Tour).

### Sonstiges

- Vertragsende/Kündigungsdatum: Kunde auto-pausieren ab Datum
- Bulk-Import von Kunden (CSV/Excel)
- Adress-Validierung (Geocoding)
- Mehrsprachigkeit

### OCR / Wäscheliste

- **Cloud-OCR:** Für bessere Handschrift-Erkennung Optionen prüfen: Google Vision API, Document AI, AWS Textract, Azure Document Intelligence, OCR.space. Datenschutz (DSGVO) beachten. Details: REFERENZ_ANALYSEN_UND_BERICHTE.md Abschn. 8.
- **Bildvorverarbeitung:** Kontrast, Begradigung, Ausschnitt zur Verbesserung der On-Device-OCR.

### Qualität & Infrastruktur

- **Crashlytics/Analytics konsequent:** Alle catch-Blöcke und optional Nutzungs-Analytics nutzen.
- **Echtzeit-Sync-Status:** „X Änderungen ausstehend“ oder Fortschrittsbalken bei großer Sync-Queue.

---

## Drucker für Belege (Klebestreifen / Etiketten)

*Ziel: Beleg drucken = Klebestreifen mit Kunden-Alias und Endpreis (keine Rechnungen). Rechnungen kommen später.*

- **Thermo-Etikettendrucker (Bluetooth, schmal):** 58 mm oder 40 mm Breite. Bluetooth direkt vom Handy. ESC/POS – viele Drucker verstehen diesen Befehlssatz.
- **Typen (Beispiele):** 58-mm-Thermo-Drucker, Etiketten-Drucker 40/58 mm; Marken: Epson TM-Serie, Star Micronics, Bixolon, Sunmi, No-Name mit ESC/POS.
- **Wichtig:** ESC/POS-Unterstützung (Library z. B. DantSu ESC/POS-ThermalPrinter-Android), Bluetooth, Papier/Etiketten in gewünschter Breite.
- **Für die App:** ESC/POS-Library einbinden, Text (Alias, Zeilen/Summen, Endpreis) als Beleg an den Drucker senden. Siehe auch PROJECT_MANIFEST.md Abschn. 16.

---

**Bei Umsetzung eines Punkts:** In PROJECT_MANIFEST.md (Backlog) abgleichen und hier ggf. als umgesetzt vermerken.

*Stand: Feb 2026*
