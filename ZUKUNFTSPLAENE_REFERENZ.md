# Zukunftspläne – TourPlaner 2026

**Zweck:** Zentrale Referenz für Ideen und Zukunftspläne. Punkte, die mit „Zukunft“ oder „später bauen“ markiert werden, werden hier eingetragen. Nichts davon ohne ausdrückliche Freigabe umsetzen (vgl. `.cursor/rules/zukunftsplan.mdc`).

*Quellen: BERICHT_TIEFENANALYSE_APP_2026, GESAMTANALYSE_APP_2026_02, BERICHT_APP_ZUSTAND_2026_02, PLAN_PERFORMANCE_OFFLINE, PLAN_TOURPLANNER_PERFORMANCE_3TAGE, WASCHLISTE_OCR_OPTIONEN, ARCHITEKTUR_ACTIVITY_COORDINATOR, COMPOSE_MIGRATION_PROTOKOLL.*

---

## Übersicht (bereits geführt)

- Kalender-Export (iCal/CalDAV), Onboarding bei leerer App  
- Benachrichtigungen („Morgen X Kunden fällig“, Erinnerung pausierte Kunden)  
- Berichte & Export (PDF/CSV, Buchhaltung, Tour-Last)  
- Paging/Lazy-Loading im Kundenmanager (ab ~500 Kunden)  
- Echte Karten-UI (In-App mit Markern/Route statt nur Maps-Intent)  
- Tour-Reihenfolge speichern und an Karten-App übergeben  
- Benutzer und Rollen (Admin, Wäscherei, Fahrer)  
- Drucker/Belege: Klebestreifen mit Alias + Endpreis (ESC/POS, Bluetooth, 58 mm/40 mm)

---

## Aus PROJECT_MANIFEST Backlog übernommen

### Login & Nutzer

- **Login-Feedback (5.2 Mittlere Priorität):** Bei Fehler der anonymen Anmeldung kurze Meldung (Snackbar/Toast) und Retry-Button statt sofort finish(). Quelle: BERICHT_TIEFENANALYSE_APP_2026, Abschnitt 5.2.
- **Benutzer und Rollen (3 Rollen, vordefinierte Benutzer):**
  - 3 Benutzer anlegen: 1 = Admin, 2 = Wäscherei, 3 = Fahrer. Vordefiniert (nicht erst zur Laufzeit anlegen).
  - Jeder Benutzer bekommt ein Kennwort (Login mit E-Mail/Name + Passwort).
  - Rollenbasierte Sicht und Bearbeitung: Admin = vollen Zugriff; Wäscherei = Erledigungen, Termine (evtl. keine Stammdaten); Fahrer = Tourenplaner, Erledigungen, Karte (evtl. keine Stammdaten/Termin-Regeln).
  - Konkrete Rechte pro Rolle bei Umsetzung festlegen.

### Optionale Features

- **Echte Karten-UI:** In-App mit Markern/Route statt nur Maps-Intent.
- **Tour-Reihenfolge:** Drag & Drop speichern und an Karten-App übergeben.
- **Benachrichtigungen:** Push „Morgen X Kunden fällig“; Erinnerung für pausierte Kunden.
- **Paging im Kundenmanager:** Ab ~500 Kunden (laut BERICHT_TIEFENANALYSE_APP_2026).

### History-Log & frühere Ideen

- **History-Log für Kunden:** Alle Änderungen protokollieren (wer, wann, was). Empfehlung: zentrales Log für alle Kunden, jeder Eintrag mit Kunden-ID – pro Kunde filterbar, global zeitlich sortiert. (Vorschlag Feb 2026.)
- **Kalender-Export:** Termine in Gerätekalender (iCal/CalDAV).
- **Onboarding:** Bei leerer App kurze Anleitung „Erstelle deine erste Kundenliste“ mit Navigation.

### Berichte, Technik & Sonstiges

- **Berichte & Export:** Monatliche Berichte (PDF/CSV); Export für Buchhaltung; Tour-Last-Anzeige.
- **Technik & UX:** Accessibility (contentDescription, 48dp Touch-Targets); Dark Mode (derzeit global deaktiviert).
- **Sonstiges:** Vertragsende/Kündigungsdatum mit Auto-Pause; Bulk-Import (CSV/Excel); Adress-Validierung (Geocoding); Mehrsprachigkeit.
- **Tourenplaner:** Filter nach Kundenart/Liste/Region; Erledigungsquote pro Zeitraum (Woche/Monat).
- **Termin-Logik (empfohlen):** Vollständige Termin-Quelle – zentrale API für alle Termine (inkl. kundenTermine, ausnahmeTermine); alle Stellen (customerFaelligAm, naechstesFaelligAmDatum, getFaelligAmDatum*) darauf umstellen (BERICHT_APP_ZUSTAND_2026_02, GESAMTANALYSE_APP_2026_02).

---

## Drucker für Belege (Klebestreifen / Etiketten)

*Ziel: Beleg drucken = Klebestreifen mit Kunden-Alias und Endpreis (keine Rechnungen). Rechnungen kommen später.*

### Gut geeignet (Klebestreifen, Alias + Endpreis, Android)

- **Thermo-Etikettendrucker (Bluetooth, schmal):** 58 mm oder 40 mm Breite – typisch für Belege/Tickets/Etiketten. Bluetooth direkt vom Handy. ESC/POS – viele Drucker verstehen diesen Befehlssatz.
- **Konkrete Typen (Beispiele):** 58-mm-Thermo-Drucker, schmale Etiketten-Drucker (40 mm, 58 mm); Marken: Epson TM-Serie, Star Micronics, Bixolon, Sunmi, No-Name mit ESC/POS.
- **Wichtig:** ESC/POS-Unterstützung (Library z. B. DantSu ESC/POS-ThermalPrinter-Android), Bluetooth, Papier/Etiketten in gewünschter Breite.
- **Für die App:** ESC/POS-Library einbinden, Text (Alias, Zeilen/Summen, Endpreis) als einfachen Beleg an den Drucker senden. 58 mm (oder 40 mm) reicht für Klebestreifen gut aus.

---

## Aus Analysen ergänzt

### Technik & Performance

- **Paging (vor ~500 Kunden):** PLAN_PERFORMANCE_OFFLINE Prio 2.1/2.2 – Kundenmanager mit Seitengröße 50–100; Suche/Filter mit Paging. PLAN_TOURPLANNER Phase 5 optional.
- **Dateigröße/Wartbarkeit:** Sehr große Screens/ViewModels in kleinere Teile aufteilen (PLAN_PERFORMANCE Prio 5.1; GESAMTANALYSE).
- **LoadState durchgängig:** ViewModels mit LoadState&lt;T&gt; (Loading/Success/Error) statt ad-hoc isLoading/errorMessage (GESAMTANALYSE, PLAN_UMSETZUNG).
- **AgentDebugLog nur in Debug:** Log-Ausgaben nicht in Release (GESAMTANALYSE, BERICHT_APP_ZUSTAND).

### UX & Barrierefreiheit

- **Accessibility:** contentDescription für alle Icons, Mindest-Touch-Target 48dp, Kontrast prüfen (IDEEN_ZUKUNFT, BERICHT_TIEFENANALYSE, GESAMTANALYSE).
- **Dark Mode:** Optional aktivierbar; derzeit global deaktiviert (GESAMTANALYSE, BERICHT_TIEFENANALYSE).
- **LoginActivity:** Sichtbare Login-UI in Compose (COMPOSE_MIGRATION Phase 2 – aktuell [ ]).

### OCR / Wäscheliste

- **Cloud-OCR:** Für bessere Handschrift-Erkennung Optionen prüfen: Google Vision API, Document AI, AWS Textract, Azure Document Intelligence, OCR.space (WASCHLISTE_OCR_OPTIONEN). Datenschutz (DSGVO) beachten.
- **Bildvorverarbeitung:** Kontrast, Begradigung, Ausschnitt zur Verbesserung der On-Device-OCR (WASCHLISTE_OCR_OPTIONEN).

### Tourenplaner & Statistiken

- **Filter im Tourenplaner:** Nach Kundenart/Liste/Region/PLZ (BERICHT_TIEFENANALYSE, GESAMTANALYSE).
- **Erledigungsquote pro Zeitraum:** Woche/Monat, nicht nur „heute“ (BERICHT_TIEFENANALYSE, PROJECT_MANIFEST).

### Domain & Erweiterungen

- **History-Log für Kunden:** Wer, wann, was geändert (GESAMTANALYSE, PROJECT_MANIFEST).
- **Termin-Regeln erweitern:** Feiertage, Schließtage, „jeden 2. Montag“ (BERICHT_TIEFENANALYSE).
- **Listen-Vorlagen:** Vorgefertigte Regeln mit einem Klick anwenden (BERICHT_TIEFENANALYSE).
- **Auftrags-/Mengenverwaltung, Rechnungsrelevanz, Kunden-Kommunikation (E-Mail/SMS), Check-in am Kunden (Zeiterfassung):** aus BERICHT_TIEFENANALYSE 4.2.
- **Vertragsende/Kündigungsdatum mit Auto-Pause, Bulk-Import (CSV/Excel), Adress-Validierung (Geocoding), Mehrsprachigkeit:** IDEEN_ZUKUNFT.

### Qualität & Infrastruktur

- **Crashlytics/Analytics konsequent:** Alle catch-Blöcke und optional Nutzungs-Analytics (BERICHT_TIEFENANALYSE, PLAN_UMSETZUNG).
- **Echtzeit-Sync-Status:** „X Änderungen ausstehend“ oder Fortschrittsbalken (BERICHT_TIEFENANALYSE).
- **Vollständige Termin-Quelle:** Siehe PROJECT_MANIFEST Backlog und BEKANNTE_FEHLER (Fälligkeit kundenTermine/ausnahmeTermine).

---

*Stand: Feb 2026. Bei Umsetzung eines Punkts: in PROJECT_MANIFEST abgleichen und ggf. hier als umgesetzt vermerken.*
