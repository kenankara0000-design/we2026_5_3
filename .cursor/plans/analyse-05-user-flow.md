# Analyse 05: User-Flow (Klick-Pfade)

**Status:** ✅ Erledigt (2026-02-13)  
**Priorität:** 🔴 Hoch

---

## Typische Aufgaben

### 1. Tour erledigen (A/L/KW für Kunde)

| Schritt | Aktion |
|---------|--------|
| 1 | Hauptbildschirm → Tour-Hero-Card |
| 2 | Kunden-Karte tippen → OverviewDialog |
| 3 | Dialog schließen |
| 4 | „Aktionen" tippen → ErledigungSheet |
| 5 | A / L / KW wählen |
| 6 | (KW: zusätzlicher Bestätigungsdialog) |

**→ 4–5 Klicks.** Verbesserungspotenzial: Direkt „Aktionen" ohne Zwischen-Dialog.

---

### 2. Überfälligen Kunden bearbeiten

| Schritt | Aktion |
|---------|--------|
| 1 | Tourenplaner öffnen (Tour-Hero) |
| 2 | Sektion „Überfällig" ist standardmäßig offen |
| 3 | Karte tippen → OverviewDialog → „Details öffnen" |

**→ 3 Klicks** bis Kundendetail.

---

### 3. Kunden verschieben

| Schritt | Aktion |
|---------|--------|
| 1 | Im Tourenplaner: „Aktionen" → ErledigungSheet |
| 2 | „Termin verschieben" |
| 3 | Bestätigungs-Dialog |
| 4 | (Falls A+L: „A oder L?" wählen) |
| 5 | Datum-Dialog |
| 6 | Optional: „Alle zukünftigen" / „Nur diesen" |

**→ 4–6 Klicks.**

---

### 4. Kunden-Stammdaten ansehen/ändern

**Ansehen (aus Tourenplaner):**

| Schritt | Aktion |
|---------|--------|
| 1 | Karte tippen → OverviewDialog |
| 2 | „Details öffnen" → Kundendetail |

**→ 2 Klicks.**

**Ansehen (aus Kundenliste):**

| Schritt | Aktion |
|---------|--------|
| 1 | Hauptbildschirm → „Kunden" |
| 2 | Karten-Klick → Kundendetail |

**→ 2 Klicks.**

**Ändern:**

| Schritt | Aktion |
|---------|--------|
| 1 | Im Kundendetail: Stammdaten-Tab |
| 2 | „Bearbeiten" (nur Admin) |
| 3 | Felder ändern |
| 4 | „Speichern" in TopBar |

**→ 4 Schritte** (inkl. 2 Klicks für Navigation + 2 Klicks für Edit/Save).

---

### 5. Neuen Kunden anlegen

| Schritt | Aktion |
|---------|--------|
| 1 | Hauptbildschirm → „+ Neu Kunde" (nur Admin) |
| 2 | Formular ausfüllen |
| 3 | „Speichern" |

**→ 2 Klicks** + Formular. Schneller Zugang.

---

### 6. Wäsche erfassen (manuell)

| Schritt | Aktion |
|---------|--------|
| 1 | Hauptbildschirm → „Erfassung" |
| 2 | „Erfassung starten" |
| 3 | Kunde suchen + auswählen |
| 4 | „Neue Erfassung" |
| 5 | Positionen hinzufügen + Speichern |

**→ 5 Klicks.** Relativ tief; alternatives Einstieg über Kundendetail-Belege-Tab: 3 Klicks.

---

### 7. Wäsche erfassen (Kamera/Foto)

| Schritt | Aktion |
|---------|--------|
| 1–4 | Wie manuell (bis „Neue Erfassung") |
| 5 | Dialog: „Kamera/Foto" wählen |
| 6 | Foto aufnehmen |
| 7 | OCR-Ergebnis prüfen + Speichern |

**→ 6–7 Klicks.**

---

### 8. Beleg anschauen

**Über Erfassung-Menü:**

| Schritt | Aktion |
|---------|--------|
| 1 | Hauptbildschirm → „Erfassung" |
| 2 | „Belege" |
| 3 | Kunden-Card tippen |

**→ 3 Klicks.**

**Über Kundendetail:**

| Schritt | Aktion |
|---------|--------|
| 1 | Kundendetail → Belege-Tab |
| 2 | Beleg-Card tippen |

**→ 2 Klicks** (wenn bereits im Kundendetail).

---

### 9. Liste erstellen

| Schritt | Aktion |
|---------|--------|
| 1 | Hauptbildschirm → „Kunden Listen" |
| 2 | „Neue Liste" |
| 3 | Formular ausfüllen + Speichern |

**→ 3 Klicks.**

---

### 10. Kunden einer Liste zuordnen

| Schritt | Aktion |
|---------|--------|
| 1 | Hauptbildschirm → „Kunden Listen" |
| 2 | Liste öffnen |
| 3 | Bearbeiten |
| 4 | Bei „Verfügbare Kunden" auf Hinzufügen klicken |

**→ 4 Klicks.** Alternativ: über Kundendetail-Stammdaten (Bearbeiten → Liste zuordnen).

---

### 11. Kundenpreise einsehen/ändern

| Schritt | Aktion |
|---------|--------|
| 1 | Hauptbildschirm → Einstellungen |
| 2 | „Preise" |
| 3 | „Kundenpreise" |
| 4 | Kunde suchen + auswählen |
| 5 | Preise bearbeiten |

**→ 5 Klicks.** Tief verschachtelt.

---

### 12. Urlaub eintragen

| Schritt | Aktion |
|---------|--------|
| 1 | Kundendetail → Termine-Tab |
| 2 | „+ Termin" → Sheet: „Urlaub" |
| 3 | → UrlaubActivity |
| 4 | „Neuer Urlaub" → Datum-Picker |

**→ 4 Klicks.**

---

### 13. Statistiken ansehen

| Schritt | Aktion |
|---------|--------|
| 1 | Hauptbildschirm → „Statistiken" |

**→ 1 Klick.** Schnell erreichbar.

---

### 14. SevDesk-Import durchführen

| Schritt | Aktion |
|---------|--------|
| 1 | Hauptbildschirm → „Einstellungen" |
| 2 | „Data Import" |
| 3 | „SevDesk Import" |
| 4 | Token eingeben/speichern |
| 5 | „Kontakte importieren" / „Artikel importieren" |

**→ 5 Klicks.** Selten genutzt, Tiefe akzeptabel.

---

### 15. Zur Kundenadresse navigieren (Maps)

**Aus Tourenplaner:**

| Schritt | Aktion |
|---------|--------|
| 1 | TopBar → „Karte" |
| 2 | Maps-Intent öffnet sich automatisch |

**→ 1–2 Klicks.**

**Aus Kundendetail:**

| Schritt | Aktion |
|---------|--------|
| 1 | Adresse tippen (klickbar) → Maps |

**→ 1 Klick.**

---

## Sackgassen und Probleme

| Problem | Screen | Details |
|---------|--------|---------|
| Kein Back in TopBar | 12 von 24 Screens | Nutzer muss System-Back verwenden |
| Kein „Nächster Kunde" | CustomerDetail | `showSaveAndNext = false` – deaktiviert |
| KundenpreiseScreen | Kein Zurück zur Suche | `onBackToKundeSuchen` nicht genutzt; System-Back beendet Activity |
| Erledigung erfordert Zwischen-Dialog | TourPlanner | Karte → OverviewDialog → schließen → „Aktionen" |

---

## Zusammenfassung Klick-Pfade

| Aufgabe | Klicks | Bewertung |
|---------|--------|-----------|
| Tour erledigen | 4–5 | ⚠️ Optimierbar (Zwischen-Dialog) |
| Stammdaten ansehen | 2 | ✅ OK |
| Stammdaten ändern | 4 | ✅ OK |
| Neuer Kunde | 2 | ✅ Schnell |
| Wäsche erfassen | 5 | ⚠️ Tief |
| Beleg ansehen | 2–3 | ✅ OK |
| Liste erstellen | 3 | ✅ OK |
| Kunden zuordnen | 4 | ⚠️ OK aber tief |
| Kundenpreise | 5 | ⚠️ Tief verschachtelt |
| Urlaub eintragen | 4 | ✅ OK |
| Statistiken | 1 | ✅ Schnell |
| SevDesk-Import | 5 | ✅ OK (selten) |
| Karte/Maps | 1–2 | ✅ Schnell |

---

*Keine Umsetzung ohne ausdrückliche Freigabe.*
