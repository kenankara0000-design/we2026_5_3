# Plan: Nur laden was nötig – Berechnung & Änderungen

**Stand:** Feb 2026  
**Ohne Code** – nur Ziele, Analyse und Schritte.  
Bezug: PLAN_PERFORMANCE_OFFLINE.md (Prio 1.2, 4.1), PROJECT_MANIFEST.md (500-Kunden-Ziel, Offline).

---

## 1. Was die App heute macht

### Laden

- Es werden überall **volle Listen** genutzt: Firebase Realtime DB mit ValueEventListener auf z. B. `customers` und `customers_for_tour`.
- Bei jeder Änderung unter dem Knoten kommt das **gesamte Snapshot** – es gibt keine „nur geänderte Einträge“ aus der DB.

### Termin-/Fälligkeitslogik

- **Hauptbildschirm:** Bei jeder Änderung von Kunden/Listen wird die Fälligkeitsanzahl (Badge) neu berechnet.
- **Tourenplaner:** Bei jeder Änderung von Kunden/Listen/Datum (nach Debounce) läuft die volle Tourenberechnung (Überfällig, Heute, Erledigt, Listen). Es gibt einen Tag-Cache für Nachbartage, aber „heute“ wird bei jeder Emission neu berechnet.
- **Statistiken:** Beim Öffnen des Screens wird einmal getCustomersForTourFlow().first() ausgeführt und dann die komplette Statistik berechnet – also bei jedem Öffnen, nicht „1× am Tag“.

### Listener

- Beim **Hauptbildschirm** sind die Flows schon an den Lifecycle gekoppelt (nur bei Bedarf sammeln).
- **Tourenplaner** und **Kundenmanager** starten ihre Flows dagegen im ViewModel-init – sobald die Activity da ist, wird also dauerhaft gesammelt, auch wenn der Nutzer den Screen gar nicht sieht.

---

## 2. Vorschläge: „Nur laden, was nötig ist“

### Listener nur bei sichtbarem Screen

- Tourenplaner und Kundenmanager wie den Hauptbildschirm behandeln: Flows **erst starten, wenn der jeweilige Screen sichtbar ist**, und **stoppen, wenn er verlassen wird** (z. B. über Lifecycle/Cleared).
- So werden Kunden-/Listen-Updates nur verarbeitet, wenn Tour-Planner oder Kundenmanager wirklich offen sind.

### Statistiken

- Kein automatisches Laden im init. **Erst beim tatsächlichen Öffnen** des Statistik-Screens laden.
- Optional Ergebnis **cachen** (z. B. „für heute“ oder „für diese Session“) und bei erneutem Öffnen zuerst Cache anzeigen, mit Möglichkeit „Aktualisieren“.

### Einzelkunden

- Bereits gut: Kundendetail nutzt getCustomerFlow(customerId) – nur ein Kunde. So beibehalten.

---

## 3. Vorschläge: „Liste/DB – nur Änderungen aktualisieren“

### Firebase

- Mit dem aktuellen ValueEventListener liefert Firebase immer den **ganzen Knoten**. Um nur Änderungen zu verarbeiten, müsste man auf **Child-Events** umstellen (onChildAdded, onChildChanged, onChildRemoved) und **lokal eine Liste** pflegen, die man pro Event aktualisiert (ein Eintrag hinzufügen/ändern/entfernen).
- Dann: nur die geänderten Einträge parsen, Rest aus der lokalen Liste; UI nur für betroffene Zeilen aktualisieren (z. B. stabile IDs + Differenz).

### Lokale Liste in der App

- Diese Liste (aus Child-Events befüllt) wird zur Single Source of Truth für die Anzeige.
- Schwere Berechnungen (Fällig-Count, Tourenplaner-Tag) können so angeknüpft werden, dass sie nur laufen, wenn sich **relevante** Daten geändert haben (z. B. nur bei geänderten/neu hinzugekommenen Kunden oder bei Tagwechsel).

---

## 4. Vorschläge: „Termin-Berechnung / Schweres nur 1× am Tag oder bei Bedarf“

### Fälligkeitsanzahl (Badge auf dem Hauptbildschirm)

- Statt bei jeder Firebase-Emission neu zu rechnen: **Ergebnis cachen** mit Tagesgrenze („berechnet für Tag X“). Wenn sich die zugrunde liegenden Daten nicht geändert haben und noch derselbe Tag ist, den Cache verwenden.
- Alternative: **einmal täglich** (z. B. morgens per WorkManager) berechnen und Ergebnis speichern; Badge liest nur diesen Wert und zeigt ihn an.

### Tourenplaner-Tagesansicht

- Berechnung **pro Tag** stärker cachen: z. B. „heute“ als berechnet markieren mit Gültigkeit „bis Mitternacht“ oder „bis zur nächsten Datenänderung“. Wenn weder Datum noch Kundendaten sich geändert haben, keine erneute schwere Berechnung.
- Den bestehenden Tag-Cache (z. B. ±1 Tag) beibehalten oder behutsam erweitern.

### Statistiken

- Entweder **Cache pro Tag**: einmal berechnen, Speicher „Statistik für Tag X“; beim erneuten Öffnen am selben Tag Cache anzeigen, optional Button „Aktualisieren“.
- Oder **einmal täglich** im Hintergrund berechnen und Ergebnis speichern; der Statistik-Screen zeigt nur die gespeicherten Werte (und ggf. manuellen Refresh).

---

## 5. Kurzüberblick

| Ziel | Hebel |
|------|--------|
| Nur laden, was nötig ist | Listener-Lifecycle für Tourenplaner und Kundenmanager (Flows nur bei sichtbarem Screen); Statistiken erst beim Öffnen laden, ggf. cachen. |
| Nur Änderungen aktualisieren | Firebase auf Child-Events umstellen, lokale Liste inkrementell pflegen; UI nur geänderte Zeilen aktualisieren. |
| Termin-Berechnung / Schweres seltener | Fällig-Count und Statistiken mit Tages-Cache oder 1× täglich; Tourenplaner „heute“ mit klarer Cache-Gültigkeit (Tag/Datenänderung), keine Neuberechnung ohne Grund. |

---

Die Ideen bauen auf PLAN_PERFORMANCE_OFFLINE.md und PROJECT_MANIFEST.md auf und führen das Prinzip „nur was nötig ist“ und „nur Änderungen / 1× am Tag“ konsequenter fort.
