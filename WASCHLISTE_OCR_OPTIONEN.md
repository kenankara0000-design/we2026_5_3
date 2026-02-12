# Wäscheliste-Formular: OCR – Optionen & Cloud-Dienste (Dokumentation für die Zukunft)

Stand: Februar 2026. Diese Datei fasst die im Projekt umgesetzte OCR-Lösung, Verbesserungsideen und Cloud-Alternativen zusammen.

---

## 1. Aktuell in der App (On-Device)

- **Engine:** ML Kit Text Recognition (Latin, gebündelt) – `com.google.mlkit:text-recognition:16.0.1`
- **Bereich:** Nur der Abschnitt zwischen „Wäscheliste“ und „Vielen Dank für Ihren Auftrag!“ wird ausgewertet; Kopf- und Fußbereich werden ignoriert.
- **Felder:** Name, Adresse, Telefon (per Label NAME/ADRESSE/TELEFON), Artikeltabelle mit Mengen. **Sonstiges** wird von der App nicht aus dem OCR befüllt (nur manuell).
- **Mengen:** Häkchen (✔, ✓, …) werden ignoriert; es zählt die **erste** Zahl 1–999 als Menge (nicht das Maximum), damit „2✔“ nicht als 4 gewertet wird.

**Einschränkung:** On-Device-OCR ist bei **Handschrift** deutlich schlechter als bei Druck; die Erkennungsrate bleibt begrenzt.

---

## 2. Warum die Erkennung begrenzt ist

- Handschrift ist für Standard-OCR (auch ML Kit) schwieriger als gedruckter Text.
- On-Device bedeutet: schnell, datenschutzfreundlich, aber weniger leistungsstark als große Cloud-Modelle.
- Verschiedene Stifte, Schriften und Papierqualität erschweren eine „eine Lösung für alles“.

---

## 3. Verbesserung ohne Technik-Wechsel

- **Foto:** Gleichmäßiges Licht, scharf, Zettel flach; nur Formularbereich (Wäscheliste bis „Vielen Dank“) im Bild.
- **Formular:** Hinweis auf dem Zettel: „Bitte in Druckbuchstaben und mit Kugelschreiber/Filzstift“.
- **Nutzung:** OCR als **Hilfe** verstehen – Erkanntes prüfen, Rest **manuell** eintragen; optional digitales Formular als Alternative zum Zettel-Foto.

---

## 4. Technische Optionen (Überblick)

| Option | Beschreibung | Aufwand |
|--------|--------------|--------|
| **Andere On-Device-Engine** | z. B. Tesseract; Handschrift bleibt schwierig | Mittel |
| **Cloud-OCR** | Google, AWS, Azure, OCR.space – bessere Erkennung, v. a. Handschrift | Mittel bis hoch (API, Datenschutz) |
| **Eigenes Modell trainieren** | Nur für dieses Formular; braucht viele annotierte Daten + ML-Know-how | Sehr hoch, oft nicht sinnvoll |
| **Bildvorverarbeitung** | Kontrast, Begradigung, Ausschnitt – kann etwas helfen | Gering |

**Empfehlung:** Für spürbar bessere Erkennung Cloud-OCR prüfen (z. B. Google Vision API oder Document AI, AWS Textract). Eigenes Modell nur bei sehr vielen annotierten Daten und ML-Erfahrung.

---

## 5. Cloud-Dienste: Kosten & Free-Versionen

### Google

**Google Cloud Vision API (Text Detection / OCR)**  
- **Free:** Erste **1.000 Einheiten/Monat** kostenlos (1 Bild = 1 Einheit).  
- **Danach:** ca. 1,50 US-$ pro 1.000 Einheiten.  
- **Handschrift:** Unterstützt, aber nicht speziell optimiert.  
- **Hinweis:** Google Cloud Konto, Kreditkarte für Abrechnung.

**Google Document AI**  
- **Free:** Kein dauerhafter Free Tier; nur Testguthaben möglich.  
- **Kosten:** z. B. ab ca. 1,50 US-$ pro 1.000 Seiten.  
- **Handschrift:** Besser als reine Vision API, gut für Formulare.

---

### Amazon AWS Textract

- **Free:** Teil der **AWS Free Tier** – oft **1.000 Seiten/Monat** für die **ersten 3 Monate**.  
- **Danach:** Bezahlung pro Seite (je nach API: „Detect Document Text“ vs. „Analyze Document“).  
- **Handschrift:** Ja; auch Formulare und Tabellen.  
- **Hinweis:** AWS-Konto, Kreditkarte; genaue Grenzen auf der AWS-Textract-Preis-Seite prüfen.

---

### Microsoft Azure Document Intelligence

- **Free:** Oft **500 Seiten/Monat** im Free Tier (je nach Region/Angebot).  
- **Danach:** Bezahlung pro Seite, je nach Modell.  
- **Handschrift:** Ja; OCR, Layout, Formulare.  
- **Hinweis:** Azure-Konto; Preise im Azure-Preisrechner prüfen.

---

### OCR.space

- **Free:** **Kostenlose API** mit Registrierung (z. B. per E-Mail, Key sofort nutzbar).  
- **Limits:** u. a. Dateigröße (z. B. 5 MB), keine Garantie für 24/7 bei Free.  
- **Engine 3:** Deutlich bessere Qualität inkl. **Handschrift-OCR**.  
- **Einsatz:** Einfacher Einstieg; Datenschutz und Server-Standort prüfen.

---

## 6. Grober Vergleich Cloud-OCR

| Anbieter | Free-Version | Typische Kosten danach | Handschrift |
|----------|--------------|-------------------------|-------------|
| **Google Vision API** | 1.000 Einheiten/Monat | ca. 1,50 $ / 1.000 | Unterstützt |
| **Google Document AI** | Kein dauerhaftes Free | ca. 1,50 $ / 1.000 Seiten | Besser |
| **AWS Textract** | Oft 1.000 Seiten/Monat, 3 Monate | Pro Seite | Ja |
| **Azure Document Intelligence** | Oft 500 Seiten/Monat | Pro Seite | Ja |
| **OCR.space** | Free API mit Limits | Kostenlos (mit Limits) | Engine 3: Handschrift |

*Preise und Free-Tier können sich ändern – immer auf den offiziellen Preis-Seiten der Anbieter prüfen.*

---

## 7. Datenschutz (DSGVO)

- Bei allen Cloud-Lösungen: **Daten verlassen das Gerät** und werden auf Servern des Anbieters verarbeitet.
- Für personenbezogene Kundendaten (Name, Adresse, Telefon): Anbieter mit **EU/DSGVO-konformen** Optionen wählen oder Nutzung auf anonymisierte/Test-Daten beschränken.
- On-Device-OCR (ML Kit) verarbeitet lokal – keine Weitergabe an Cloud für die Texterkennung.

---

## 8. Relevante Dateien im Projekt

- Parser: `app/.../wasch/WaeschelisteOcrParser.kt`
- Formular-State & Merge: `app/.../wasch/WaeschelisteFormularState.kt`
- ViewModel (OCR-Aufruf, Bitmap laden): `app/.../ui/wasch/WaschenErfassungViewModel.kt`
- UI Formular: `app/.../ui/wasch/WaeschelisteFormularContent.kt`
