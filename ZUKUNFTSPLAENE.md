# Zukunftspläne – TourPlaner 2026

**Regel:** Punkte, die du mit „Zukunft“ oder „später bauen“ markierst, werden hier eingetragen. Nichts davon wird ohne deine ausdrückliche Freigabe umgesetzt.

---

## Ideen & Vorschläge (noch nicht umgesetzt)

*Einträge werden hier ergänzt, sobald du bei einer Umsetzung „Zukunftsplan“ sagst.*

- **Login-Feedback (5.2 Mittlere Priorität):** Bei Fehler der anonymen Anmeldung kurze Meldung anzeigen (z. B. Snackbar/Toast) und Retry-Button statt sofort finish(). Quelle: BERICHT_TIEFENANALYSE_APP_2026.md, Abschnitt 5.2.
- 
- **History-Log für Kunden:** Alle Änderungen an Kunden protokollieren (wer, wann, was). Empfehlung: ein zentrales Log für alle Kunden, jeder Eintrag mit Kunden-ID – dann pro Kunde filterbar und global zeitlich sortiert nutzbar. (Vorschlag Feb 2026.)
- **Optionale Features (nicht umgesetzt):** Echte Karten-UI, Tour-Reihenfolge, Benachrichtigungen, Paging.
- **Benutzer und Rollen (3 Rollen, vordefinierte Benutzer):**
  - **3 Benutzer anlegen:** 1 = Admin, 2 = Wäscherei, 3 = Fahrer. Diese Benutzer sollen direkt/vordefiniert angelegt sein (nicht erst zur Laufzeit anlegen).
  - **Jeder Benutzer bekommt ein Kennwort** (Login mit E-Mail/Name + Passwort).
  - **Rollenbasierte Sicht und Bearbeitung:** Jeder Benutzer darf nur bestimmte Funktionen sehen bzw. bearbeiten:
    - **Admin:** vollen Zugriff (Kunden, Listen, Termin-Regeln, Tourenplaner, Einstellungen, ggf. Benutzerverwaltung).
    - **Wäscherei:** nur die für die Wäscherei relevanten Bereiche (z. B. Erledigungen erfassen, Termine sehen, evtl. keine Kundenstammdaten bearbeiten).
    - **Fahrer:** nur die für den Fahrer relevanten Bereiche (z. B. Tourenplaner, Erledigungen, Karte; evtl. keine Stammdaten oder Termin-Regeln bearbeiten).
  - Konkrete Rechte pro Rolle und wo Login/Auth eingebaut wird, bei Umsetzung festlegen.

---

## Quelle

Grundlage: **BERICHT_TIEFENANALYSE_APP_2026.md** (Januar 2026).  
Neue Ideen können jederzeit ergänzt werden.
