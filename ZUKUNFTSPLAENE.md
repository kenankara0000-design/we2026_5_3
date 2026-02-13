# Zukunftspläne – TourPlaner 2026

**Regel:** Punkte, die mit „Zukunft“ oder „später bauen“ markiert werden, werden hier eingetragen. **Nichts davon wird ohne ausdrückliche Freigabe umgesetzt** (vgl. `.cursor/rules/zukunftsplan.mdc`).

*Quellen: REFERENZ_ANALYSEN_UND_BERICHTE.md, PROJECT_MANIFEST.md. Neue Ideen jederzeit unter „Ideen & Vorschläge“ ergänzen.*

---

## Teilweise umgesetzt (Stand: Feb 2026)

*Diese Punkte sind in der App bereits angelegt oder an einigen Stellen eingeführt; vollständig umgesetzt sind sie nicht. Rest-Arbeit steht weiter unten unter „Ideen & Vorschläge“.*

### Technik & Infrastruktur

1. **Offline-Anzeige:** NetworkMonitor und Sync-Hinweise (MainScreen, TourPlanner-, Kundenmanager-TopBar) vorhanden. Noch offen: Echtzeit-Sync-Status („X Änderungen ausstehend“ / Fortschrittsbalken).
2. **FirebaseRetryHelper:** Existiert, wird z. B. im CustomerRepository genutzt – noch nicht in allen Repositories.
3. **AppNavigation:** Wird in MainActivity, CustomerDetailActivity, SettingsActivity genutzt – noch nicht in allen Activities.
4. **AppLogger:** Existiert (mit TODO für BuildConfig.DEBUG), wird an einigen Stellen genutzt – noch nicht überall statt Log/printStackTrace.
5. **ComposeDialogHelper:** Wird z. B. in SettingsScreen, SevDeskImportScreen, StandardPreislisteScreen genutzt – noch nicht in allen Dialogen.
6. **BaseViewModel:** Klasse vorhanden – wird von keinem ViewModel genutzt (nur als Vorlage).

### Features

7. **Tour-Reihenfolge:** Firebase-Knoten `tourReihenfolge` und TourOrderRepository (Speichern/Lesen pro Wochentag) vorhanden. Noch offen: Drag-&-Drop-UI im Tourenplaner, Weitergabe der Reihenfolge an die Karten-App.
8. **Export:** CustomerExportHelper mit Kunden-Export (CSV/Text) im Kundenmanager vorhanden. Noch offen: Monatliche Berichte (PDF/CSV), Export für Buchhaltung, Tour-Last-Anzeige.
9. **Accessibility:** An vielen Stellen contentDescription gesetzt – noch nicht für alle Icons; 48 dp Touch-Targets und Kontrast nicht systematisch geprüft.
10. **OCR/Wäscheliste:** ML Kit (On-Device) fürs Wäscheliste-Formular umgesetzt. Noch offen: Cloud-OCR, Bildvorverarbeitung.

### Noch nicht wie gewünscht

11. **AgentDebugLog:** Wird genutzt, ist aber **nicht** an BuildConfig.DEBUG gekoppelt (läuft auch in Release). Soll nur in Debug aktiv sein.
12. **LoadState:** Sealed Class existiert, wird in der App kaum genutzt; die meisten ViewModels haben weiter ad-hoc isLoading/errorMessage.
13. **AppErrorMapper:** Existiert, Texte noch fest im Code – noch nicht in strings.xml ausgelagert.

---

## Ideen & Vorschläge (noch nicht umgesetzt)

*Einträge werden hier ergänzt, sobald bei einer Umsetzung „Zukunftsplan“ gesagt wird.*

### Bereits als „Zukunft“ markiert

14. **Kalender-Export:** Termine in Gerätekalender exportieren (iCal/CalDAV)
15. **Onboarding:** Bei leerer App kurze Anleitung „Erstelle deine erste Kundenliste“ mit Navigation

### Login & Nutzer

16. **Benutzer und Rollen (3 Rollen, vordefinierte Benutzer):** 3 Benutzer anlegen (Admin, Wäscherei, Fahrer); Kennwort; rollenbasierte Sicht und Bearbeitung; konkrete Rechte bei Umsetzung festlegen.

### Optionale Features

17. **Echte Karten-UI:** In-App mit Markern/Route statt nur Maps-Intent.
18. **Tour-Reihenfolge (UI):** Drag & Drop speichern und an Karten-App übergeben (Wegpunkte in Reihenfolge).
19. **Benachrichtigungen:** Push „Morgen X Kunden fällig“; Erinnerung für pausierte Kunden („Kunde X ist seit Y Tagen pausiert“).
20. **Paging im Kundenmanager:** Ab ~500 Kunden (Seitengröße 50–100; Suche/Filter mit Paging).

### History-Log & Backlog

21. **History-Log für Kunden:** Alle Änderungen protokollieren (wer, wann, was). Empfehlung: zentrales Log für alle Kunden, jeder Eintrag mit Kunden-ID – pro Kunde filterbar, global zeitlich sortiert.

### Berichte & Export

22. Monatliche Berichte (PDF/CSV)
23. Export für Buchhaltung: Liste erledigter Abholungen/Auslieferungen
24. Tour-Last-Anzeige (Kapazität pro Tag)

### Technik & UX

25. **Performance:** Lazy Loading, Pagination bei vielen Kunden (>500).
26. **Accessibility:** contentDescription für alle Icons, Mindest-Touch-Target 48dp, Kontrast prüfen.
27. **Dark Mode:** Optional aktivierbar (derzeit global deaktiviert).
28. **Offline-Anzeige:** Echtzeit-Sync-Status „X Änderungen ausstehend“ oder Fortschrittsbalken (Hinweis „Änderungen werden synchronisiert“ bereits vorhanden).
29. **LoadState durchgängig:** ViewModels mit LoadState&lt;T&gt; (Loading/Success/Error) statt ad-hoc isLoading/errorMessage.
30. **AgentDebugLog:** Nur in Debug-Build aktivieren (nicht in Release).
31. **LoginActivity:** Sichtbare Login-UI in Compose (aktuell [ ]).

### Technische Refactorings

32. Alle Repositories auf FirebaseRetryHelper umstellen
33. Alle Activities auf AppNavigation umstellen
34. Alle Log/printStackTrace → AppLogger ersetzen
35. Weitere ViewModels von BaseViewModel erben
36. Crashlytics in AppLogger integrieren
37. AppErrorMapper-Texte in strings.xml auslagern
38. Dateigröße/Wartbarkeit: Sehr große Screens/ViewModels in kleinere Teile aufteilen

### Termin-Logik & Domain

39. **Vollständige Termin-Quelle:** Zentrale API für alle Termine (inkl. kundenTermine, ausnahmeTermine); alle Fälligkeitsstellen darauf umstellen (REFERENZ_ANALYSEN_UND_BERICHTE.md Abschn. 2 und 9).
40. **Termin-Logik vereinfachen:** Einheitliches „Termin-Datum + Typ (A/L)“-Modell (z. B. TerminInfo mit Quelle); ein Dialog „Termin hinzufügen“ (Regelmäßig / Einmalig / Ausnahme); Listen: „Listen-Termin hinzufügen“ mit Datum + A+L.
41. **Termin-Regeln erweitern:** Feiertage, Schließtage, „jeden 2. Montag im Monat“.
42. **Listen-Vorlagen:** Vorgefertigte Regeln für typische Listen (z. B. „Wöchentlich Mo/Do“) mit einem Klick anwenden.
43. **Filter im Tourenplaner:** Nach Kundenart/Liste/Region/PLZ.
44. **Erledigungsquote pro Zeitraum:** Woche/Monat, nicht nur „heute“.

### Domain & Erweiterungen

45. **Auftrags-/Mengenverwaltung:** Pro Termin Mengen (Säcke, Kilo) oder Checkliste; Auswertung in Statistiken.
46. **Rechnungsrelevanz:** Markierung „abgerechnet“, Verknüpfung zu Belegen; Export für Buchhaltung.
47. **Kunden-Kommunikation:** E-Mail/SMS-Erinnerung an Kunden vor Abholung (optional mit Opt-in).
48. **Check-in am Kunden:** Beim Erledigen optional „Angekommen“- und „Fertig“-Zeitpunkt (Zeiterfassung pro Tour).

### Sonstiges

49. Vertragsende/Kündigungsdatum: Kunde auto-pausieren ab Datum
50. Bulk-Import von Kunden (CSV/Excel)
51. Adress-Validierung (Geocoding)
52. Mehrsprachigkeit

### OCR / Wäscheliste

53. **Cloud-OCR:** Für bessere Handschrift-Erkennung Optionen prüfen: Google Vision API, Document AI, AWS Textract, Azure Document Intelligence, OCR.space. Datenschutz (DSGVO) beachten. Details: REFERENZ_ANALYSEN_UND_BERICHTE.md Abschn. 8.
54. **Bildvorverarbeitung:** Kontrast, Begradigung, Ausschnitt zur Verbesserung der On-Device-OCR.

### Qualität & Infrastruktur

55. **Crashlytics/Analytics konsequent:** Alle catch-Blöcke und optional Nutzungs-Analytics nutzen.
56. **Echtzeit-Sync-Status:** „X Änderungen ausstehend“ oder Fortschrittsbalken bei großer Sync-Queue.

---

## Drucker für Belege (Klebestreifen / Etiketten)

*Ziel: Beleg drucken = Klebestreifen mit Kunden-Alias und Endpreis (keine Rechnungen). Rechnungen kommen später.*

57. **Thermo-Etikettendrucker (Bluetooth, schmal):** 58 mm oder 40 mm Breite. Bluetooth direkt vom Handy. ESC/POS – viele Drucker verstehen diesen Befehlssatz.
58. **Typen (Beispiele):** 58-mm-Thermo-Drucker, Etiketten-Drucker 40/58 mm; Marken: Epson TM-Serie, Star Micronics, Bixolon, Sunmi, No-Name mit ESC/POS.
59. **Wichtig:** ESC/POS-Unterstützung (Library z. B. DantSu ESC/POS-ThermalPrinter-Android), Bluetooth, Papier/Etiketten in gewünschter Breite.
60. **Für die App:** ESC/POS-Library einbinden, Text (Alias, Zeilen/Summen, Endpreis) als Beleg an den Drucker senden. Siehe auch PROJECT_MANIFEST.md Abschn. 16.

---

**Bei Umsetzung eines Punkts:** In PROJECT_MANIFEST.md (Backlog) abgleichen und hier ggf. als umgesetzt vermerken.

*Stand: Feb 2026*
