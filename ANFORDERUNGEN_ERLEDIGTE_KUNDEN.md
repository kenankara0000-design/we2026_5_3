# Anforderungen: Erledigte Kunden und Anzeigelogik

## 1. Überfällige Kunden - Erledigung

### 1.1 Überfällige Kunden, die erledigt werden
- **Bedingung:** Ein Kunde war überfällig und wird an einem Tag, an dem er noch überfällig ist, als erledigt markiert
- **Aktion:** Kunde wird in den "Erledigte Kunden"-Bereich verschoben
- **Hinweise in CardView anzeigen:**
  - Fälligkeitsdatum (wann war der Kunde fällig?)
  - Erledigungsdatum (wann wurde er erledigt?)
  - Zeitstempel (Datum und Uhrzeit der Erledigung)
- **Anzeige:** Alle Hinweise werden direkt in der CardView des Kunden angezeigt

### 1.2 Kunden am tatsächlichen Abholtag
- **Bedingung:** Ein Kunde wird am geplanten Abholtag erledigt
- **Aktion:** Kunde wird als erledigt markiert
- **Hinweise in CardView anzeigen:**
  - Erledigungsdatum (wann genau wurde er erledigt?)
  - Zeitstempel (Datum und Uhrzeit der Erledigung)
- **Anzeige:** Alle Hinweise werden direkt in der CardView des Kunden angezeigt

### 1.3 Zeitstempel bei Erledigung
- **Bei jeder Erledigung:**
  - Zeitstempel speichern (Datum und Uhrzeit)
  - **Wichtig:** Zeitstempel wird **nur gespeichert**, nicht als separate Anzeige angezeigt
  - Zeitstempel wird direkt in der CardView gespeichert und zusammen mit den anderen Hinweisen angezeigt

## 2. Überfällige Kunden - Anzeigelogik

### 2.1 Anzeige nur an zwei Tagen
Überfällige Kunden werden **nur** an folgenden Tagen angezeigt:

1. **Am tatsächlichen Fälligkeitstag** (Termindatum)
   - Beispiel: Termin 20.01.2026 → Anzeige am 20.01.2026

2. **Am heutigen Tag** (wenn noch überfällig)
   - Beispiel: Heute 25.01.2026, Termin war 20.01.2026 → Anzeige am 25.01.2026

### 2.2 Zwischenzeit nicht anzeigen
- **Wichtig:** Zwischen dem Fälligkeitstag und heute werden überfällige Kunden **NICHT** angezeigt
- **Beispiel:**
  - Termin: 20.01.2026
  - Heute: 25.01.2026
  - ✅ Anzeige: 20.01.2026 (Fälligkeitstag) und 25.01.2026 (heute)
  - ❌ Keine Anzeige: 21.01.2026, 22.01.2026, 23.01.2026, 24.01.2026

### 2.3 Zusammenfassung der Logik
- Überfällige Kunden werden angezeigt, wenn:
  - Heute = Fälligkeitstag **ODER**
  - Heute > Fälligkeitstag (noch überfällig)
- Überfällige Kunden werden **NICHT** angezeigt, wenn:
  - Heute liegt zwischen Fälligkeitstag und heute (nur am Fälligkeitstag und am heutigen Tag)

## 3. Erledigt-Bereich

### 3.1 Verschiebung in den Erledigt-Bereich
Kunden werden in den "Erledigt"-Bereich verschoben, wenn sie markiert sind mit:
- **"A"** (Abholung erledigt) **ODER**
- **"L"** (Auslieferung erledigt) **ODER**
- **Beiden** ("A" und "L")

### 3.2 Sichtbarkeit des Erledigt-Bereichs
- Der "Erledigt"-Bereich ist **immer sichtbar**
- Auch wenn keine erledigten Termine vorhanden sind, bleibt der Bereich sichtbar (kann leer sein)

### 3.3 Position in der Ansicht
- Der "Erledigt"-Bereich wird **unterhalb der normalen Kunden** angezeigt
- **Reihenfolge:**
  1. Normale Kunden (oben)
  2. Erledigt-Bereich (unten)

## 4. Reihenfolge im Tourenplaner

### 4.1 Anzeigereihenfolge der Kundenbereiche
Die Kunden werden im Tourenplaner in folgender Reihenfolge angezeigt (von oben nach unten):

1. **Überfällige Kunden** (ganz oben)
   - Kunden mit überfälligen Terminen

2. **Kunden Listen**
   - Kunden, die zu Listen gehören

3. **Normale Kunden**
   - Alle anderen Kunden (nicht überfällig, nicht in Listen, nicht erledigt)

4. **Erledigt-Bereich** (ganz unten)
   - Kunden, die mit "A" (Abholung) oder "L" (Auslieferung) oder beiden markiert sind

### 4.2 Zusammenfassung der Reihenfolge
```
┌─────────────────────────┐
│ 1. Überfällig           │
├─────────────────────────┤
│ 2. Kunden Listen        │
├─────────────────────────┤
│ 3. Normale Kunden       │
├─────────────────────────┤
│ 4. Erledigt-Bereich     │
└─────────────────────────┘
```

## 5. Zusammenfassung aller Anforderungen

### 5.1 Erledigungslogik
- ✅ Überfällige Kunden → bei Erledigung in Erledigt-Bereich mit Hinweisen in CardView:
  - Fälligkeitsdatum (wann war fällig?)
  - Erledigungsdatum (wann wurde erledigt?)
  - Zeitstempel (Datum und Uhrzeit)
- ✅ Kunden am Abholtag → bei Erledigung markieren mit Hinweisen in CardView:
  - Erledigungsdatum (wann wurde erledigt?)
  - Zeitstempel (Datum und Uhrzeit)
- ✅ Zeitstempel → bei jeder Erledigung **nur speichern** (nicht als separate Anzeige), direkt in CardView gespeichert

### 5.2 Anzeigelogik für überfällige Kunden
- ✅ Nur am Fälligkeitstag anzeigen
- ✅ Nur am heutigen Tag anzeigen (wenn noch überfällig)
- ✅ Zwischenzeit NICHT anzeigen

### 5.3 Erledigt-Bereich
- ✅ Kunden mit "A" oder "L" oder beiden → Erledigt-Bereich
- ✅ Erledigt-Bereich **immer sichtbar** (auch wenn leer)
- ✅ Position: Unterhalb der normalen Kunden

### 5.4 Reihenfolge im Tourenplaner
- ✅ 1. Überfällig
- ✅ 2. Kunden Listen
- ✅ 3. Normale Kunden
- ✅ 4. Erledigt-Bereich
