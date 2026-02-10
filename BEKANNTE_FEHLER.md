# Bekannte Fehler (Bug-Punkte)

**Single Source of Truth** für alle dokumentierten Bugs der App we2026_5.  
Neue bekannte Fehler hier eintragen; behobene Fehler mit Datum/Hinweis entfernen oder als „behoben“ markieren.

Keine Änderung am Verhalten (Bug-Fix) ohne ausdrückliche Freigabe (vgl. `.cursor/rules/nicht-kaputt-machen.mdc`).

---

## Offen

*(Keine offenen Bugs)*

---

## Behoben

### Mehrere A-Tage: nur der erste wurde für Intervalle genutzt

- **Behoben Feb 2026:** `TerminAusKundeUtils.erstelleIntervalleAusKunde` erstellt jetzt ein Intervall **pro** A-Wochentag. Kunden mit mehreren A-Tagen (z. B. Mo + Mi) erhalten alle Termine in der 365-Tage-Berechnung. Rückwärtskompatibel: Ein A-Tag = ein Intervall wie zuvor.

### SevDesk Preise-Import: „chain validation failed“

- **Behoben/Bekannt Feb 2026:** Tritt nur im **Emulator** auf; auf echten Geräten funktioniert der SevDesk-Import normal. Kein App-Fix nötig (SSL-Phänomen des Emulators).

### Erledigte-Liste bei vergangenem Datum leer (z. B. gestern)

- **Behoben Feb 2026:** `TourDataProcessor` befüllt `tourListenErledigt` jetzt auch bei Vergangenheit – Hilfsfunktion `sammleErledigteInListen()` wird im else-Zweig für `istVergangenheit` aufgerufen. Erledigt-Sheet zeigt Listen-Kunden auch bei Datumswechsel auf gestern.
