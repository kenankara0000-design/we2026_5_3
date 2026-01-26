# Logcat-Analyse und Lösungen

**Datum:** 26. Januar 2026

---

## 🔴 KRITISCHES PROBLEM: Firestore API nicht aktiviert

### Fehlermeldung:
```
PERMISSION_DENIED: Cloud Firestore API has not been used in project tourplaner2026 before or it is disabled. 
Enable it by visiting https://console.developers.google.com/apis/api/firestore.googleapis.com/overview?project=tourplaner2026
```

### Lösung:

1. **Firestore API aktivieren:**
   - Öffne: https://console.developers.google.com/apis/api/firestore.googleapis.com/overview?project=tourplaner2026
   - Klicke auf **"ENABLE"** (Aktivieren)
   - Warte 2-5 Minuten, bis die API aktiviert ist

2. **Alternative über Firebase Console:**
   - Gehe zu: https://console.firebase.google.com/
   - Wähle Projekt "tourplaner2026"
   - Gehe zu **Firestore Database**
   - Falls noch nicht erstellt: Klicke auf **"Create database"**
   - Wähle **"Start in test mode"** (für Entwicklung) oder **"Production mode"** (für Produktion)

3. **Nach Aktivierung:**
   - App neu starten
   - Die PERMISSION_DENIED Fehler sollten verschwinden
   - Daten werden jetzt online synchronisiert

---

## ✅ Was funktioniert:

### 1. Offline-Modus funktioniert:
```
CustomerRepository: Save completed (timeout, but saved locally)
CustomerRepository: Update completed (timeout, but updated locally)
```
- ✅ Daten werden lokal gespeichert, auch ohne Internet
- ✅ Timeout-Logik (2 Sekunden) funktioniert

### 2. FirebaseSyncManager funktioniert:
```
FirebaseSyncManager: Firestore network enabled
FirebaseSyncManager: Firestore network disabled
```
- ✅ Netzwerk wird korrekt aktiviert/deaktiviert
- ✅ Automatische Steuerung basierend auf Online-Status

### 3. WorkManager funktioniert:
```
WM-WrkMgrInitializer: Initializing WorkManager with default configuration.
WM-Schedulers: Created SystemJobScheduler and enabled SystemJobService
```
- ✅ WorkManager ist initialisiert
- ✅ Bereit für Offline-Upload-Queue

### 4. Speichern funktioniert:
```
AddCustomer: Save successful: true, will close activity: true
```
- ✅ Kunden werden erfolgreich gespeichert (lokal)
- ✅ UI-Feedback funktioniert

---

## ⚠️ Weitere Probleme:

### 1. Firestore CustomClassMapper Warnung:
```
[CustomClassMapper]: No setter/field for faelligAm found on class Customer
```

**Problem:** Firestore versucht ein Feld `faelligAm` zu lesen, das nicht in der `Customer` Klasse existiert.

**Lösung:** 
- `faelligAm` ist wahrscheinlich eine berechnete Eigenschaft (getter-Methode)
- Firestore kann nur Felder speichern, keine berechneten Eigenschaften
- **Option A:** Feld als `@Ignore` markieren (wenn es nur berechnet wird)
- **Option B:** Feld explizit in Customer-Klasse hinzufügen (wenn es gespeichert werden soll)

**Code-Fix:**
```kotlin
// In Customer.kt
@get:Ignore
val faelligAm: Long
    get() = getFaelligAm() // Berechnete Eigenschaft
```

### 2. Bitmap-Recycling Fehler:
```
java.lang.IllegalStateException: Can't compress a recycled bitmap
at ImageUtils.kt:55
```

**Problem:** Bitmap wird recycelt, bevor es komprimiert wird.

**Lösung:** In `ImageUtils.kt` sicherstellen, dass Bitmap nicht recycelt wird, bevor Komprimierung abgeschlossen ist.

**Code-Fix:**
```kotlin
// In ImageUtils.kt - compressImage Funktion
// Stelle sicher, dass Bitmap nicht recycelt wird
if (bitmap.isRecycled) {
    // Erstelle neue Bitmap
    return null
}
```

### 3. GoogleApiManager Fehler (kann ignoriert werden):
```
GoogleApiManager: Failed to get service from broker
SecurityException: Unknown calling package name 'com.google.android.gms'
```

**Status:** ⚠️ Kann ignoriert werden
- Dies ist ein Emulator-spezifischer Fehler
- Funktioniert auf echten Geräten normalerweise
- Beeinträchtigt die App-Funktionalität nicht

---

## 📊 Zusammenfassung:

### ✅ Funktioniert:
- Offline-Speicherung
- FirebaseSyncManager
- WorkManager
- Kunden speichern (lokal)

### ❌ Muss behoben werden:
1. **KRITISCH:** Firestore API aktivieren (siehe oben)
2. **Wichtig:** `faelligAm` Feld-Problem beheben
3. **Wichtig:** Bitmap-Recycling Fehler beheben

### ⚠️ Kann ignoriert werden:
- GoogleApiManager Fehler (Emulator-spezifisch)

---

## 🔧 Nächste Schritte:

### Sofort (Priorität 1):
1. ✅ Firestore API aktivieren
2. ✅ App neu starten
3. ✅ Prüfen ob PERMISSION_DENIED Fehler verschwindet

### Kurzfristig (Priorität 2):
4. ✅ `faelligAm` Feld-Problem beheben
5. ✅ Bitmap-Recycling Fehler beheben

---

**Ende der Analyse**
