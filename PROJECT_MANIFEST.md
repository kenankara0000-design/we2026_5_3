# PROJECT_MANIFEST.md

**Single Source of Truth** für Design-Ziele, Scope und Planung der App we2026_5 (TourPlaner 2026).

Diese Datei muss gepflegt werden. Wenn sich Ziele, Scope oder Planung ändern: Hier aktualisieren.

---

## 1. Projektidentität

- **Name:** TourPlaner 2026 (intern: we2026_5)
- **Zweck:** Touren- und Kundenverwaltungs-App für Wäscherei-/Textildienstleistung
- **Zielgruppe:** Fahrer, Disponenten, Büro im Betrieb

---

## 2. Geplanter Scope (Kundenanzahl)

**Die App ist von Anfang an für bis zu 500 Kunden ausgelegt.**

- Ab ca. 500 Kunden: Paging/Lazy-Loading im Kundenmanager empfohlen (laut BERICHT_TIEFENANALYSE_APP_2026)
- Aktuell: Alle Kunden werden geladen; Parsing läuft im Hintergrund (Feb 2026)

---

## 3. Kernfunktionen (geplant / vorhanden)

- Kundenstamm (Gewerblich, Privat, Listen)
- Tourenplaner (Tagesansicht, Überfällig, Heute, Erledigt)
- Abholung / Auslieferung / KW erfassen
- Termin-Regeln, Intervalle, Listen
- Statistiken, MapView, Offline-Fähigkeit, Fotos

---

## 4. Projekt-Gesetze

Die verbindlichen Regeln stehen in **`.cursorrules`** und **`.cursor/rules/`**.  
Das Manifest ergänzt sie um Ziele und Scope; es ersetzt sie nicht.

---

## 5. Technik

- **Datenbank:** Firebase Realtime Database (nicht Firestore)
- **UI:** Jetpack Compose (Migration weitgehend abgeschlossen)
- **DI:** Koin

---

**Letzte Aktualisierung:** Feb 2026
