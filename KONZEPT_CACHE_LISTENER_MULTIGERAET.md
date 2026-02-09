# Konzept: Cache, Listener und Multi-Gerät (Zukunft)

**Zweck:** Architektur-Vorgaben für sparsames Laden, Echtzeit wo nötig und Skalierung. Für die Zukunft der App merken und bei Erweiterungen beachten.

---

## Ausgangslage

- **Kunden- und Artikelstamm** ändern sich selten.
- **Mindestens 3 Geräte** – Erledigungsstand (Abholung / Keine Wäsche / Auslieferung) soll auf allen Geräten sichtbar sein.
- **Erledigt-Bereich** = hohe Priorität für Aktualität.
- **Urlaub, Verschiebung, Termin anlegen/bearbeiten/löschen** sollen sofort wirken und auf anderen Geräten ankommen.
- **Ziel:** Nur geänderte Daten nachziehen, nicht jedes Mal die komplette Kunden- oder Artikelliste neu laden.

---

## Empfohlene Richtung

### 1. Stammdaten (Kundenliste, Artikel) – weiter sparsam laden

- **Kein** dauerhafter Listener auf die komplette Kunden- oder Artikelliste.
- **Laden:** einmal beim ersten Zugriff (oder pro Tag/Session), danach aus **Cache**.
- **Invalidierung:** wenn *dieses* Gerät schreibt (Kunde/Artikel anlegen, bearbeiten, löschen, Import) → Cache verwerfen, beim nächsten Zugriff neu laden.
- **Andere Geräte:** Änderungen an Stammdaten siehst du beim nächsten Öffnen des Bereichs oder nach manuellem „Aktualisieren“. Das reicht, weil sich Stammdaten selten ändern.

**Erweiterung:** Später kann man optional einen **leichten** Listener nur auf eine kleine „Version“ oder „lastModified“-Info legen: Wenn sich *irgendwo* was geändert hat, einmal komplette Liste neu laden. Oder bei Erweiterung auf viele Geräte: gezieltes Nachladen nur geänderter Kunden (siehe unten).

---

### 2. Erledigt-Bereich (Abholung, Keine Wäsche, Auslieferung) – gezielt „live“

- **Genau diese Daten** (welcher Kunde ist heute als „Erledigt“ mit welcher Art markiert) sollten über **Listener** oder gezieltes Nachladen aktuell gehalten werden.
- **Option A – Listener auf Erledigungs-/Touren-Daten:**  
  Nur den Firebase-Bereich abonnieren, der für „Tourenplan heute“ und „Erledigt“ zuständig ist (z.B. `customers_for_tour` oder die Knoten, in denen Erledigungsstatus/Termin-Erledigung gespeichert wird).  
  → Wenn ein Gerät „Erledigt“ setzt, sehen die anderen Geräte es schnell.
- **Option B – Kurze Abfrage beim Öffnen/Wechsel:**  
  Beim Öffnen des Tourenplaners (oder Wechsel zum Erledigt-Bereich) einmal die aktuellen Erledigungsdaten für den Tag laden, Rest aus Cache.  
  → Weniger Echtzeit, aber weniger Listener; guter Kompromiss.

**Empfehlung:** Für „mindestens 3 Geräte“ und „Erledigt soll sichtbar sein“: **Listener nur auf den Erledigungs-/Touren-Bereich** (nicht auf die ganze Kundenliste). So siehst du Abholung/Keine Wäsche/Auslieferung aktuell, ohne die großen Stammdaten ständig zu ziehen.

---

### 3. Urlaub, Verschiebung, Termin anlegen/bearbeiten/löschen

- **Listener** auf die **betroffenen Daten** (z.B. Urlaub, verschobene Termine, Ausnahme-Termine, ggf. Termin-Regeln).
- Wenn sich *irgendetwas* in diesen Knoten ändert → **nur diese Bereiche** im UI aktualisieren (z.B. Tourenplaner neu berechnen oder nur die geänderten Kunden/Listen nachladen).
- So werden Urlaub/Verschiebung/neue Termine sofort auf allen Geräten sichtbar, ohne die ganze App oder die komplette Kundenliste neu zu laden.

---

### 4. Nur den geänderten Kunden / Termin aktualisieren („Delta“)

**Ja, das ist die bessere Langzeit-Lösung:**

- **Prinzip:**
  - Beim ersten Laden: Kundenliste (oder Artikel) einmal holen und cachen.
  - Bei **Änderungen** (von diesem oder anderem Gerät):
    - Entweder **nur die geänderte Entität** aus Firebase lesen (z.B. ein Kunde per ID, ein Termin pro Kunde) und im Cache ersetzen/einfügen,
    - oder **Listen mit „lastModified“ / Version:** Listener nur auf eine kleine Struktur (z.B. „welche Kunden-IDs haben sich wann geändert“); wenn sich was ändert, nur diese IDs nachladen und Cache aktualisieren.

- **Vorteile:**
  - Kein komplettes Neuladen der Kunden- oder Artikelliste.
  - Skalierbar (500+ Kunden): nur Deltas nachziehen.
  - Geeignet für Erweiterung (mehr Geräte, mehr Nutzer, später evtl. Backend mit „Änderungs-Feed“).

- **Umsetzungsidee in Firebase:**
  - Pro Kunde (und ggf. pro Artikel) ein Feld wie `updatedAt` oder eine zentrale „Änderungsliste“ (z.B. Kunden-IDs + Zeitstempel).
  - Listener nur auf diese Änderungsinfo oder auf einzelne Kunden-Knoten.
  - Bei Änderung: nur diese Kunden (oder Termine) nachladen und lokalen Cache aktualisieren.
  - Für **Termine** (Urlaub, Verschiebung, neu anlegen): gleiches Prinzip – Listener oder Abfrage nur auf Termin-/Urlaub-/Verschiebungs-Daten; bei Änderung nur die betroffenen Kunden oder Termin-Blöcke nachladen, nicht die ganze DB.

---

### 5. Was braucht „immer“ Firebase, was kann warten?

| Bereich | Empfehlung |
|--------|------------|
| **Kundenliste (Stamm)** | Cache; Invalidierung bei Schreibaktionen; optional später: nur Deltas/geänderte Kunden nachladen. |
| **Artikelliste** | Wie jetzt: 1× pro Tag + Invalidierung bei Änderung; kein Listener. |
| **Erledigt (Abholung / Keine Wäsche / Auslieferung)** | Listener (oder gezieltes Refresh) nur auf Erledigungs-/Touren-Daten. |
| **Urlaub, Verschiebung, Termin** | Listener auf diese Strukturen; bei Änderung nur betroffene Daten/Cache aktualisieren. |
| **Löschen, Bearbeiten, neuer Termin** | Sofort auf Firebase schreiben; danach entweder Listener bringt Update auf anderen Geräten, oder gezieltes Nachladen nur der geänderten Entität. |

---

### 6. Für die Zukunft (Erweiterung)

- **Einheitliches Muster:**
  - **Stammdaten:** Cache + Invalidierung (und optional Delta-Updates).
  - **Echtzeit-Bereiche:** Listener nur auf die relevanten Knoten (Erledigt, Termine, Urlaub, Verschiebung).
  - **Schreibaktionen:** sofort schreiben, dann Cache/Listener für die betroffenen Entitäten aktualisieren.

- **Skalierung:**
  - Kunden/Artikel: **Delta-Updates** (nur geänderte IDs/Entitäten) statt Vollständiges Neuladen.
  - Tourenplaner: Listener nur auf die Daten, die den aktuellen Tag und Erledigungsstatus betreffen.

- **Später möglich:**
  - Zentrale „Änderungs-Queue“ oder „lastModified“-Struktur in Firebase, an der ein Listener hängt; App lädt dann nur die geänderten Kunden/Termine.
  - Oder Backend/Cloud-Funktionen, die bei Schreibzugriffen gezielt Deltas an die Geräte pushen.

---

## Kurzfassung

- **Kunden-/Artikelstamm:** Selten laden, Cache + Invalidierung; kein Listener auf die komplette Liste; später nur geänderte Kunden/Artikel nachladen.
- **Erledigt-Bereich:** Listener (oder gezieltes Refresh) nur auf Erledigungs-/Touren-Daten, damit 3+ Geräte den gleichen Stand sehen.
- **Urlaub, Verschiebung, Termin:** Listener auf diese Daten; bei Änderung nur betroffene Kunden/Termine aktualisieren, nicht die ganze App.
- **Löschen/Bearbeiten/Neuer Termin:** Sofort in Firebase schreiben; Aktualisierung auf anderen Geräten über genau diese Listener oder über gezieltes Nachladen nur der geänderten Entität (beste Lösung für Performance und Erweiterung).

---

**Nächster Schritt (optional):** Firebase-Struktur für „Erledigt“ und „Termine“ durchgehen (welche Knoten für Listener, welche für Delta-Updates).

**Letzte Aktualisierung:** Feb 2026
