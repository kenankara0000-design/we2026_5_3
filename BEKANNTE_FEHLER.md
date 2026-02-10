# Bekannte Fehler (Bug-Punkte)

**Single Source of Truth** für alle dokumentierten Bugs der App we2026_5.  
Neue bekannte Fehler hier eintragen; behobene Fehler mit Datum/Hinweis entfernen oder als „behoben“ markieren.

Keine Änderung am Verhalten (Bug-Fix) ohne ausdrückliche Freigabe (vgl. `.cursor/rules/nicht-kaputt-machen.mdc`).

---

## Offen

### Mehrere A-Tage: nur der erste wird für Intervalle genutzt

- **Symptom:** Kunde hat mehrere A-Wochentage (z. B. Mo und Mi) und 7-Tage-Intervall. Von Di aus sollte morgen (Mi) ein Termin angezeigt werden – wird er nicht. Sobald Mo als A-Tag entfernt wird, erscheint der Termin für Mi (morgen).
- **Ursache:** Beim Anlegen/Speichern der Kunden-Intervalle wird nur **ein** Start-Wochentag verwendet (`effectiveAbholungWochentage.firstOrNull()`). Die 365-Tage-Berechnung nutzt bei vorhandenen Intervallen ausschließlich diese Intervalle; die Wochentags-Liste mit **allen** A-Tagen wird in dem Fall nicht genutzt.
- **Relevante Stellen:**
  - `TerminAusKundeUtils.kt` – `abholTag = effectiveAbholungWochentage.firstOrNull()`
  - `TerminBerechnungUtils.kt` – `berechneAlleTermineFuerKunde`: wenn `intervalle` nicht leer, nur Intervalle; `berechneTermineAusWochentagen` berücksichtigt alle A-Tage, wird bei vorhandenen Intervallen aber nicht aufgerufen.
- **Gewünschtes Verhalten (Prototyp):** `app_prototyp_termine_mehrere_atage.html` – mehrere A- und L-Tage wählbar (Mehrfachauswahl), Vorschau z. B. „Di: A+L. So: A+L.“ oder „Mo: A+L. Mi: A+L.“; L am selben Tag oder L eine Woche später.
- **Dokumentiert:** Feb 2026 (PROJECT_MANIFEST → BEKANNTE_FEHLER.md)

### Erledigte-Liste bei vergangenem Datum leer (z. B. gestern)

- **Symptom:** Am 10.02.2026 auf den 09.02.2026 wechseln – die „Erledigte (N)“-Liste / das Erledigt-Sheet ist leer, obwohl am 09.02 viele Kunden erledigt waren.
- **Ursache:** Bei **Vergangenheit** (`viewDateStart < heuteStart`) wird der komplette Block „Kunden nach Listen“ (Tour-Listen + Wochentagslisten) **nicht** ausgeführt (`if (!istVergangenheit) { … }`). Dadurch wird `tourListenErledigt` nie befüllt – alle erledigten Kunden aus **Listen** fehlen im Erledigt-Sheet. Angezeigt werden nur noch erledigte Kunden **ohne Liste** (Gewerblich/Privat mit `listeId` leer). Wenn die meisten erledigten Kunden in Tour-Listen oder Wochentagslisten stehen, wirkt die Erledigt-Liste leer.
- **Relevante Stellen:** `TourDataProcessor.kt` – Zeilen 203–276: `if (!istVergangenheit)` umschließt das Befüllen von `tourListenErledigt`; für Vergangenheit bleibt die Liste leer, nur `doneOhneListen` (ohne Liste) wird aus dem ersten Durchlauf genutzt.
- **Dokumentiert:** Feb 2026

---

## Behoben

*(Einträge hierher verschieben mit kurzem Hinweis, z. B. „Behoben Feb 2026: …“)*
