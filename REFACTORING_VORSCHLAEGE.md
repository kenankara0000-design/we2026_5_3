# 🔧 Refactoring-Vorschläge für we2026_5

## 📊 Aktuelle Situation

### Große Dateien (nach Zeilen):
1. **CustomerAdapter.kt** - ~1000 Zeilen, 38 Funktionen
2. **TourPlannerActivity.kt** - ~1000 Zeilen, 36 Funktionen
3. **TourPlannerViewModel.kt** - ~1072 Zeilen (geschätzt)
4. **CustomerDetailActivity.kt** - ~657 Zeilen, 39 Funktionen

---

## 🎯 Refactoring-Vorschläge (Priorität: Hoch → Niedrig)

### **1. CustomerAdapter.kt - ViewHolder-Binding extrahieren** ⭐⭐⭐
**Aktuell:** `bindCustomerViewHolder` ist ~430 Zeilen lang

**Vorschlag:**
- **Neue Datei:** `adapter/CustomerViewHolderBinder.kt`
- **Extrahiere:**
  - Button-Sichtbarkeits-Logik (A/L/V/U/Rückgängig) → `setupButtonVisibility()`
  - Erledigungs-Hinweise-Anzeige → `setupCompletionHints()`
  - Kunden-Typ-Button-Setup → `setupCustomerTypeButton()`
  - Status-Styling → `applyStatusStyles()` (bereits vorhanden, könnte erweitert werden)
  - Multi-Select-Visualisierung → `applyMultiSelectStyles()`

**Vorteile:**
- `CustomerAdapter.kt` wird von ~1000 auf ~600 Zeilen reduziert
- Button-Logik ist isoliert testbar
- Bessere Lesbarkeit

**Geschätzter Aufwand:** Mittel (2-3 Stunden)

---

### **2. TourPlannerActivity.kt - Callback-Handler extrahieren** ⭐⭐⭐
**Aktuell:** `setupAdapterCallbacksForAdapter` ist ~370 Zeilen lang

**Vorschlag:**
- **Neue Datei:** `tourplanner/TourPlannerCallbackHandler.kt`
- **Extrahiere:**
  - `onAbholung` Callback-Logik → `handleAbholung()`
  - `onAuslieferung` Callback-Logik → `handleAuslieferung()`
  - `onRueckgaengig` Callback-Logik → `handleRueckgaengig()`
  - `onVerschieben` Callback-Logik → `handleVerschieben()`
  - `onUrlaub` Callback-Logik → `handleUrlaub()`

**Vorteile:**
- `TourPlannerActivity.kt` wird von ~1000 auf ~600 Zeilen reduziert
- Geschäftslogik (A/L-Erledigung) ist isoliert
- Einfacher zu testen

**Geschätzter Aufwand:** Mittel (2-3 Stunden)

---

### **3. TourPlannerViewModel.kt - Datenverarbeitung extrahieren** ⭐⭐
**Aktuell:** `processTourData` ist sehr lang und komplex

**Vorschlag:**
- **Neue Datei:** `tourplanner/TourDataProcessor.kt`
- **Extrahiere:**
  - `processTourData()` → Hauptlogik für Kategorisierung
  - `istKundeUeberfaellig()` → Überfälligkeits-Prüfung
  - `customerFaelligAm()` → Fälligkeits-Berechnung
  - `hatKundeTerminAmDatum()` → Termin-Prüfung

**Vorteile:**
- ViewModel wird schlanker
- Datenverarbeitungs-Logik ist wiederverwendbar
- Einfacher zu testen

**Geschätzter Aufwand:** Hoch (3-4 Stunden)

---

### **4. CustomerDetailActivity.kt - Photo-Management extrahieren** ⭐⭐
**Aktuell:** Photo-Upload/Verwaltung ist in der Activity

**Vorschlag:**
- **Neue Datei:** `detail/CustomerPhotoManager.kt`
- **Extrahiere:**
  - `uploadImage()` → Photo-Upload-Logik
  - `showImageInDialog()` → Photo-Anzeige
  - `deletePhoto()` → Photo-Löschung
  - Camera/Gallery-Launcher-Setup

**Vorteile:**
- Activity wird schlanker
- Photo-Logik ist wiederverwendbar
- Einfacher zu testen

**Geschätzter Aufwand:** Niedrig (1-2 Stunden)

---

### **5. Gemeinsame UI-Helper erstellen** ⭐
**Problem:** Ähnliche UI-Logik in mehreren Activities

**Vorschlag:**
- **Neue Datei:** `ui/CustomerTypeButtonHelper.kt`
- **Extrahiere:**
  - Kunden-Typ-Button-Setup (G/P/L mit Farben)
  - Wird in mehreren Dateien verwendet:
    - `CustomerAdapter.kt`
    - `CustomerDetailActivity.kt`
    - `ListeBearbeitenActivity.kt`
    - `TourPlannerActivity.kt` (Dialog)

**Vorteile:**
- DRY-Prinzip (Don't Repeat Yourself)
- Konsistente Darstellung
- Einfache Wartung

**Geschätzter Aufwand:** Niedrig (1 Stunde)

---

### **6. Datum-Formatierung zentralisieren** ⭐
**Problem:** Datum-Formatierung ist überall verstreut

**Vorschlag:**
- **Neue Datei:** `util/DateFormatter.kt`
- **Extrahiere:**
  - `formatDate(Calendar)` → "26.1.2026"
  - `formatDateTime(Calendar)` → "26.1.2026 14:30"
  - `formatDateShort(Calendar)` → "26.01"

**Vorteile:**
- Konsistente Datumsformate
- Einfache Änderungen (z.B. Locale)
- Wiederverwendbar

**Geschätzter Aufwand:** Sehr niedrig (30 Minuten)

---

## 📋 Empfohlene Reihenfolge

1. **CustomerAdapter ViewHolder-Binding** (größter Impact, mittlerer Aufwand)
2. **TourPlannerActivity Callback-Handler** (großer Impact, mittlerer Aufwand)
3. **Datum-Formatierung** (schneller Win, niedriger Aufwand)
4. **CustomerTypeButtonHelper** (DRY-Prinzip, niedriger Aufwand)
5. **TourPlannerViewModel Datenverarbeitung** (hoher Aufwand, aber wichtig)
6. **CustomerDetailActivity Photo-Management** (optional, niedrige Priorität)

---

## 💡 Zusätzliche Verbesserungen

### Code-Duplikation reduzieren:
- **`sindAmGleichenTag`-Logik** erscheint mehrfach → in `TerminBerechnungUtils` verschieben
- **Erledigungs-Hinweise-Formatierung** → in Helper extrahieren
- **Button-Styling** (grau/aktiv) → in Helper extrahieren

### Testbarkeit verbessern:
- Helper-Klassen sind einfacher zu testen als große Activities/Adapters
- Geschäftslogik isolieren → Unit-Tests möglich

---

## ⚠️ Wichtige Hinweise

- **Design bleibt unverändert:** Alle Refactorings ändern nur die Code-Struktur, nicht die UI
- **Schrittweise:** Refactorings können einzeln durchgeführt werden
- **Rückwärtskompatibel:** Alle Änderungen bleiben funktional identisch

---

## 🚀 Soll ich mit einem Refactoring beginnen?

Welches Refactoring soll ich zuerst durchführen?
