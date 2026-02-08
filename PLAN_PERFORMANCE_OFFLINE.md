# Umsetzungsplan: Performance & Offline

**Single Source of Truth** für die Prioritätenliste und die konkrete Umsetzung. Kein Code – nur Ziele, Prüfpunkte und Schritte.

---

## Teil 1: Prioritätenliste

### Prio 1 – Jetzt prüfen / schnell umsetzen

| # | Thema | Konkret zu tun |
|---|--------|----------------|
| 1.1 | Keine Doppelladung | Prüfen: Nutzt irgendwo (MainScreen, Badge, Statistiken) noch die volle Kundenliste, obwohl nur Tour-Kunden nötig sind? Wenn ja: dort nur die Tour-Kunden-Quelle verwenden. |
| 1.2 | Listener nur bei Bedarf | Prüfen: Werden Kunden- oder Listen-Flows schon beim App-Start bzw. in MainActivity gesammelt, obwohl der Nutzer z. B. nur Einstellungen offen hat? Wenn ja: diese Flows erst sammeln, wenn TourPlanner oder Kundenmanager wirklich sichtbar sind; beim Verlassen des Screens nicht mehr sammeln. |
| 1.3 | Firebase Offline aktiv | Prüfen: Ist Realtime-Database-Persistence beim App-Start aktiviert? Wenn nicht: aktivieren, damit Daten lokal zwischengespeichert werden und die App offline mit zuletzt geladenen Daten weiterläuft. |
| 1.4 | Offline-/Sync-Hinweis in der UI | Prüfen: Zeigt die App an, ob offline oder Sync läuft (z. B. Hauptbildschirm)? Wenn nicht: einen klaren Hinweis einbauen (z. B. „Offline“ / „Daten werden aktualisiert“), damit Nutzer den Zustand erkennen. |

### Prio 2 – Vor Erreichen von ~500 Kunden

| # | Thema | Konkret zu tun |
|---|--------|----------------|
| 2.1 | Paging im Kundenmanager | In der Kundenverwaltung nicht mehr alle Kunden auf einmal laden. Stattdessen: z. B. erste 50–100 laden, beim Scrollen weitere „Seiten“ nachladen. Firebase-Listener so anbinden, dass nur der benötigte Ausschnitt genutzt wird. |
| 2.2 | Suche/Filter mit Paging | Wenn Paging umgesetzt ist: Suche und Filter (z. B. Ohne Tour, Status) so anpassen, dass sie mit dem Seitenmodell zusammenarbeiten (Filter zuerst, dann paginierte Anzeige). |

### Prio 3 – Bei vielen Fotos / Speicher

| # | Thema | Konkret zu tun |
|---|--------|----------------|
| 3.1 | Fotos als Thumbnails | Beim Laden und Anzeigen von Kundenfotos zuerst nur kleine Vorschaubilder (Thumbnails) laden und anzeigen; volle Auflösung erst bei Tipp/Vergrößern. Prüfen, ob Firebase Storage oder bestehende Logik Thumbnail-URLs liefert; wenn nicht, Konzept für Thumbnails festlegen. |

### Prio 4 – Dauerhaft beibehalten / absichern

| # | Thema | Konkret zu tun |
|---|--------|----------------|
| 4.1 | TourPlanner: schwere Berechnung | Touren-/Termin-Logik weiter im Hintergrund halten; Debounce bei schnellen Firebase-Updates beibehalten. Caching pro Tag (z. B. 3-Tage-Vorausberechnung) beibehalten oder bei wachsender Datenmenge ausbauen. |
| 4.2 | Firebase-Persistence | Offline-Persistence aktiv lassen (nicht abschalten). Nach Änderungen an Start/Init prüfen, dass Persistence weiterhin aktiv ist. |

### Prio 5 – Wartbarkeit (unterstützt spätere Performance)

| # | Thema | Konkret zu tun |
|---|--------|----------------|
| 5.1 | Dateigröße / Aufteilung | Sehr große Screens/ViewModels (z. B. TourPlanner, Kundenmanager, Kundendetail) in kleinere Teile aufteilen (z. B. TopBar, Listenbereich, Detailbereich). Keine Verhaltensänderung, nur bessere Struktur und Wartbarkeit. |

### Kurzüberblick

- **Prio 1:** Doppelladung vermeiden, Listener nur bei Bedarf, Firebase Offline aktiv, Offline-/Sync-Hinweis in der UI.
- **Prio 2:** Paging + Suche/Filter im Kundenmanager vor ~500 Kunden.
- **Prio 3:** Thumbnails bei vielen Fotos.
- **Prio 4:** TourPlanner-Berechnung und Caching beibehalten/ausbauen, Persistence aktiv lassen.
- **Prio 5:** Große Dateien aufteilen für Wartbarkeit.

---

## Teil 2: Umsetzungsplan (Phasen)

### Phase 0: Vorbereitung

- Prioritätenliste (Teil 1) als Referenz bereithalten (z. B. in dieser Plan-Datei oder Projekt-Wiki).
- Manifest prüfen: PROJECT_MANIFEST.md und ggf. BERICHT_TIEFENANALYSE_APP_2026 für 500-Kunden-Ziel und Offline-Ziele lesen.
- Keine Änderung ohne Prüfung: Erst analysieren, wo welche Flows/Listener genutzt werden, dann anpassen. Regel: Nichts kaputt machen.

---

### Phase 1: Prio 1 – Prüfen und ggf. umsetzen

**Reihenfolge:** 1.1 → 1.2 → 1.3 → 1.4 (1.3 und 1.4 können parallel zu 1.1/1.2 laufen).

**1.1 Keine Doppelladung**

1. Analyse: In der Codebasis suchen, wo die volle Kundenliste (alle Kunden) geladen wird. Alle Stellen notieren (z. B. MainScreen, Badge/Fälligkeitsanzahl, Statistiken, weitere Screens).
2. Bewertung: Pro Stelle prüfen: Wird nur die Anzahl oder nur „Tour-relevante“ Kunden benötigt? Wenn ja → Kandidat für Umstellung auf Tour-Kunden-Quelle.
3. Umsetzung: An allen solchen Stellen die Datenquelle von „alle Kunden“ auf „nur Tour-Kunden“ umstellen. Keine neuen Abfragen einführen; nur Quelle tauschen.
4. Prüfung: App starten, Hauptbildschirm, Tour-Planner, Statistiken durchklicken; prüfen, dass Anzeigen (z. B. Fälligkeitsanzahl) unverändert sinnvoll sind und keine Regression entsteht.

**1.2 Listener nur bei Bedarf**

1. Analyse: Herausfinden, wo Kunden-Flow und Listen-Flow gesammelt werden (z. B. MainActivity, MainViewModel, Application, Koin-Modul). Prüfen: Ab wann wird collect gestartet – beim App-Start oder erst beim Öffnen von TourPlanner/Kundenmanager?
2. Bewertung: Wenn die Flows bereits beim Start (oder beim Öffnen des Hauptbildschirms) gesammelt werden, obwohl sie nur in TourPlanner bzw. Kundenmanager gebraucht werden → Listener-Lifecycle anpassen.
3. Umsetzung: Sicherstellen, dass die Sammlung der Flows an das Öffnen der jeweiligen Activity/Screen gekoppelt ist und beim Verlassen (z. B. onCleared / Lifecycle) beendet wird. MainActivity/Hauptbildschirm soll keine Kunden-/Listen-Flows sammeln, wenn nur Einstellungen oder andere Bereiche genutzt werden.
4. Prüfung: App starten, nur Einstellungen öffnen (ohne TourPlanner/Kundenmanager) – prüfen, ob keine Kunden-/Listen-Listener aktiv sind. Dann TourPlanner öffnen – Daten müssen korrekt erscheinen. Nach Verlassen: keine dauerhafte Sammlung mehr für diese Flows.

**1.3 Firebase Offline aktiv**

1. Analyse: In der App-Initialisierung (Application-Klasse oder Stelle, an der Firebase Realtime Database zuerst genutzt wird) prüfen, ob Persistence explizit aktiviert wird.
2. Bewertung: Wenn Persistence nirgends aktiviert ist oder explizit deaktiviert wird → aktivieren. Wenn bereits aktiv → in Phase 4 nur absichern (4.2).
3. Umsetzung: An der passenden Stelle (vor der ersten Nutzung der Realtime Database) die Aktivierung der Offline-Persistence einbauen. Keine weiteren Logikänderungen.
4. Prüfung: App einmal mit Netz starten (Daten laden), dann Flugmodus/Offline – Kunden-/Tour-Daten müssen weiter nutzbar sein. Nach erneutem Online: Änderungen sollen synchronisiert werden.

**1.4 Offline-/Sync-Hinweis in der UI**

1. Analyse: Prüfen, ob es bereits einen NetworkMonitor oder ähnliches gibt und ob irgendwo ein Offline- oder Sync-Status angezeigt wird (Hauptbildschirm, Einstellungen, TourPlanner).
2. Bewertung: Wenn nirgends sichtbar → neuen, klar sichtbaren Hinweis einplanen (z. B. Hauptbildschirm oder globale TopBar). Wenn vorhanden, aber unauffällig → Platz und Text prüfen.
3. Umsetzung: Netzwerkstatus (offline / verbunden / ggf. „Daten werden aktualisiert“) an einer festen Stelle anzeigen. Quelle: bestehender NetworkMonitor oder Firebase-Connection-Status, ohne neue Hintergrund-Logik wo möglich.
4. Prüfung: Offline schalten – Hinweis erscheint; wieder online – Hinweis verschwindet oder wechselt zu „Sync“. Mit Manifest und bestehenden Sync-Hinweisen abgleichen.

**Phase-1-Abschluss:** Alle vier Punkte erledigt oder bewusst „nicht nötig“ dokumentiert. Kurz festhalten: wo umgestellt, wo Listener gekoppelt, ob Persistence aktiv, wo Offline-Hinweis sitzt.

---

### Phase 2: Prio 2 – Paging im Kundenmanager (vor ~500 Kunden)

**Voraussetzung:** Prio 1 erledigt; Entscheidung getroffen, Paging vor Erreichen von ~500 Kunden umzusetzen.

**2.1 Paging im Kundenmanager**

1. Konzept: Seitengröße festlegen (z. B. 50–100 Kunden pro Seite). Entscheiden: Laden aus lokalem Cache (bereits geladene volle Liste) oder direkte Firebase-Nutzung mit Limit/Offset oder vergleichbarem Mechanismus. Firebase Realtime DB hat kein klassisches Offset – daher entweder: einmalige/gecachte Liste clientseitig paginieren oder Konzept für „Ausschnitt laden“ (z. B. nach Alphabet/ID-Bereich) festlegen.
2. Umsetzung: Kundenmanager-UI so umbauen, dass initial nur die erste „Seite“ angezeigt wird. Beim Scrollen zum Ende der Liste die nächste Seite nachladen und an die Liste anhängen. Keine Änderung an Filter-Logik in dieser Schrittfolge; Filter in 2.2 anbinden.
3. Listener: Sicherstellen, dass der Firebase-Listener für Kunden (oder der gecachte Bestand) nur in dem Umfang genutzt wird, der für Paging nötig ist – z. B. eine vollständige Liste einmal halten und clientseitig paginieren oder, falls möglich, nur benötigte Bereiche anbinden.
4. Prüfung: Mit vielen Kunden (oder Testdaten) prüfen: erste Seite lädt schnell, Scrollen lädt nach, Liste bleibt konsistent, keine doppelten oder fehlenden Einträge.

**2.2 Suche/Filter mit Paging**

1. Reihenfolge: Filter und Suche zuerst anwenden, auf das Ergebnis Paging anwenden (nicht umgekehrt).
2. Umsetzung: Bestehende Filter (z. B. Ohne Tour, Status) und Suche so anbinden, dass sie auf den für Paging verwendeten Datenbestand wirken; die paginierte Anzeige arbeitet auf dem gefilterten/gesuchten Ergebnis. Bei Suche/Filterwechsel: Zurück auf „Seite 1“, Liste neu aufbauen.
3. Prüfung: Filter setzen → nur passende Kunden; Suche → nur Treffer; beides kombiniert; Paging läuft auf gefilterter Liste korrekt.

**Phase-2-Abschluss:** Kundenmanager läuft mit Paging und Filter/Suche; Verhalten bei ~500 Kunden getestet oder Testdaten-Szenario beschrieben.

---

### Phase 3: Prio 3 – Thumbnails bei vielen Fotos

**Auslöser:** Viele Kunden mit Fotos oder spürbarer Speicher-/Netzwerkbedarf.

**3.1 Fotos als Thumbnails**

1. Analyse: Wo werden Kundenfotos geladen und angezeigt (Kundendetail, Listen, Karten)? Prüfen, ob Firebase Storage oder bestehende Logik bereits Thumbnail-URLs oder -Varianten anbietet.
2. Konzept: Wenn Thumbnails vorhanden: diese für Listen und Vorschau nutzen; volle Auflösung nur bei Tipp/Vergrößern. Wenn nicht: Optionen festlegen (z. B. Thumbnail-URLs in Storage, oder clientseitige Verkleinerung beim ersten Laden, oder separates Thumbnail-Feld im Kundenmodell).
3. Umsetzung: Anzeige-Logik so anpassen, dass in Listen und Vorschau nur Thumbnails geladen werden; beim Öffnen der Großansicht das volle Bild laden. Keine unnötigen großen Bildläufe in Übersichten.
4. Prüfung: Listen mit vielen Fotos: geringerer Speicherverbrauch und schnellere Darstellung; Großansicht weiterhin in voller Qualität.

**Phase-3-Abschluss:** Thumbnail-Strategie umgesetzt und getestet; bei Bedarf in PROJECT_MANIFEST oder Architektur-Doku festgehalten.

---

### Phase 4: Prio 4 – Dauerhaft absichern (kein eigener Sprint)

**4.1 TourPlanner: schwere Berechnung**

- Laufend: Bei jeder Änderung am TourPlanner prüfen: Touren-/Termin-Logik läuft weiter im Hintergrund (z. B. Default-Dispatcher); Debounce bei Firebase-Updates bleibt erhalten; Caching (z. B. 3-Tage-Vorausberechnung) bleibt aktiv.
- Optional: Bei wachsender Kundenanzahl Caching ausbauen (mehr Tage oder feiner granulieren), ohne Blockierung der UI.

**4.2 Firebase-Persistence**

- Nach Änderungen an App-Start oder Firebase-Init: Prüfen, dass Persistence weiterhin aktiv ist (z. B. kurzer Offline-Test wie in 1.3).
- Checkliste: In Release-Checkliste oder Umsetzungsplan einen Punkt „Persistence aktiv?“ aufnehmen.

---

### Phase 5: Prio 5 – Wartbarkeit (bei Gelegenheit)

**5.1 Dateigröße / Aufteilung**

1. Analyse: Sehr große Dateien identifizieren (z. B. TourPlanner-Screen/ViewModel, Kundenmanager, Kundendetail) – Ziel: z. B. unter 500–600 Zeilen pro Datei.
2. Planung: Pro großer Datei sinnvolle Aufteilung festlegen (z. B. TopBar, Listenbereich, Detailbereich, Erledigung/Sheet). Keine Verhaltensänderung, nur Aufteilung in mehrere Dateien/Module.
3. Umsetzung: Schrittweise aufteilen (eine Datei nach der anderen), nach jedem Schritt bauen und Tests/manuelle Prüfung.
4. Prüfung: Funktionalität unverändert; Navigation und Lesbarkeit verbessert.

**Phase-5-Abschluss:** Große Screens/ViewModels aufgeteilt; Aufteilung in Doku oder Plan kurz notiert.

---

## Teil 3: Abhängigkeiten und Reihenfolge

- **Phase 1** zuerst und vollständig; 1.1 und 1.2 entlasten sofort, 1.3/1.4 sichern Offline ab.
- **Phase 2** baut nicht auf Phase 1 auf, sollte aber erst nach Abschluss von Phase 1 starten, damit keine Doppelladung oder falsche Listener-Nutzung das Paging beeinflusst.
- **Phase 3** unabhängig; kann parallel zu Phase 2 geplant werden, wenn Fotos-Thema akut ist.
- **Phase 4** ist Daueraufgabe; keine eigene Phase, sondern in alle Touren- und Firebase-Änderungen einfließen lassen.
- **Phase 5** jederzeit möglich, ideal wenn ohnehin an den betroffenen Screens gearbeitet wird.

---

## Teil 4: Dokumentation im Projekt

- In einer Plan-Datei (z. B. unter .cursor/plans/ oder als PLAN_PERFORMANCE_OFFLINE.md) festhalten:
  - Prioritätenliste (Teil 1),
  - diesen Umsetzungsplan (Teil 2–4),
  - nach Phase 1: kurze Notiz zu 1.1–1.4 (umgesetzt / nicht nötig / wo geändert),
  - nach Phase 2/3: kurze Notiz zu Paging und Thumbnails,
  - bei 4.2: „Persistence aktiv?“ in Release-Checkliste.
- Kein Code im Plan; Verweis auf konkrete Dateien/Module nur bei Bedarf (z. B. „MainViewModel“, „CustomerManagerActivity“).

---

**Letzte Aktualisierung:** Feb 2026
