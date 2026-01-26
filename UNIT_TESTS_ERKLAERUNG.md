# Unit-Tests Erklärung und Status

## 📚 Was sind Unit-Tests?

**Unit-Tests** sind automatisierte Tests, die einzelne Funktionen oder Methoden (sogenannte "Units") isoliert testen. Sie prüfen, ob Code wie erwartet funktioniert, ohne die gesamte App starten zu müssen.

### Warum sind Unit-Tests wichtig?

1. **Fehler früh finden:** Probleme werden entdeckt, bevor die App auf dem Gerät läuft
2. **Refactoring sicherer:** Code kann umgeschrieben werden, ohne Angst vor Fehlern
3. **Dokumentation:** Tests zeigen, wie Code verwendet werden soll
4. **Vertrauen:** Man kann Änderungen machen, ohne alles manuell testen zu müssen

### Beispiel:

```kotlin
// Funktion die getestet wird
fun add(a: Int, b: Int): Int {
    return a + b
}

// Unit-Test
@Test
fun `add returns correct sum`() {
    val result = add(2, 3)
    assertEquals(5, result)  // Prüft ob Ergebnis = 5 ist
}
```

---

## 📊 Aktueller Status in we2026_5

### ✅ Vorhandene Unit-Tests:

1. **CustomerRepositoryTest** (`app/src/test/java/com/example/we2026_5/data/repository/CustomerRepositoryTest.kt`)
   - ✅ Testet `getAllCustomers()`
   - ✅ Testet `saveCustomer()`
   - ✅ Testet `deleteCustomer()`
   - ⚠️ **Fehlt:** Tests für `updateCustomer()`, Offline-Szenarien

2. **CustomerManagerViewModelTest** (`app/src/test/java/com/example/we2026_5/ui/customermanager/CustomerManagerViewModelTest.kt`)
   - ✅ Testet `loadCustomers()`
   - ✅ Testet `filterCustomers()`
   - ⚠️ **Fehlt:** Tests für weitere ViewModel-Methoden

3. **ValidationHelperTest** (`app/src/test/java/com/example/we2026_5/ValidationHelperTest.kt`)
   - ✅ Testet Validierungs-Logik

4. **ExampleUnitTest** (`app/src/test/java/com/example/we2026_5/ExampleUnitTest.kt`)
   - ⚠️ Nur Beispiel-Test (2+2=4)

---

## ❌ Fehlende Unit-Tests (aus Analysebericht)

### 1. Repository-Methoden testen

**Was fehlt:**
- `updateCustomer()` Tests
- `getCustomerById()` Tests
- Offline/Online-Szenarien
- Timeout-Verhalten (2 Sekunden)
- Fehlerbehandlung

**Beispiel für fehlenden Test:**

```kotlin
@Test
fun `updateCustomer returns true on success`() = runTest {
    // Arrange
    val customerId = "1"
    val updates = mapOf("name" to "Neuer Name")
    val mockDocument = mock<DocumentReference>()
    val mockTask: Task<Void> = mock()
    
    whenever(mockCollection.document(customerId)).thenReturn(mockDocument)
    whenever(mockDocument.update(updates)).thenReturn(mockTask)
    whenever(mockTask.isComplete).thenReturn(true)
    whenever(mockTask.isSuccessful).thenReturn(true)
    
    // Act
    val result = repository.updateCustomer(customerId, updates)
    
    // Assert
    assertTrue(result)
    verify(mockDocument).update(updates)
}

@Test
fun `saveCustomer handles offline mode with timeout`() = runTest {
    // Test für Offline-Verhalten mit 2 Sekunden Timeout
    // ...
}
```

### 2. Offline/Online-Szenarien testen

**Was fehlt:**
- Tests für `NetworkMonitor`
- Tests für `FirebaseSyncManager`
- Tests für Offline-Speicherung
- Tests für Synchronisierung nach Wiederverbindung

**Beispiel:**

```kotlin
@Test
fun `NetworkMonitor detects offline status`() {
    // Test ob NetworkMonitor korrekt Offline-Status erkennt
}

@Test
fun `FirebaseSyncManager waits for pending writes`() = runTest {
    // Test ob Synchronisierung funktioniert
}
```

### 3. Storage-Upload-Tests

**Was fehlt:**
- Tests für `StorageUploadManager`
- Tests für `ImageUploadWorker`
- Tests für Offline-Upload-Queue

**Beispiel:**

```kotlin
@Test
fun `StorageUploadManager queues upload when offline`() = runTest {
    // Test ob Upload in Queue kommt, wenn offline
}

@Test
fun `ImageUploadWorker uploads image successfully`() = runTest {
    // Test ob Worker korrekt funktioniert
}
```

### 4. KundenListeRepository Tests

**Was fehlt:**
- Komplette Test-Suite für `KundenListeRepository`
- Tests für `saveListe()`, `updateListe()`, `deleteListe()`

### 5. RealtimeDatabaseRepository Tests

**Was fehlt:**
- Komplette Test-Suite für `RealtimeDatabaseRepository`
- Tests für alle CRUD-Operationen

---

## 🎯 Langfristige Verbesserungen (aus Analysebericht)

### 1. Unit-Tests erweitern

**Priorität:** Mittel  
**Aufwand:** 2-3 Tage

**Was zu tun ist:**
- Fehlende Repository-Tests hinzufügen
- Offline/Online-Szenarien testen
- Storage-Upload-Tests implementieren
- ViewModel-Tests vervollständigen

**Vorteile:**
- Mehr Vertrauen bei Code-Änderungen
- Fehler werden früher gefunden
- Code-Qualität steigt

---

### 2. Firestore Security Rules prüfen

**Priorität:** Hoch  
**Aufwand:** 1 Tag

**Was zu tun ist:**
- Security Rules in Firebase Console prüfen
- Testen mit verschiedenen Benutzer-Rollen
- PERMISSION_DENIED Fehler beheben (wurde in Analysebericht erwähnt)

**Vorteile:**
- App funktioniert korrekt
- Keine unerwarteten Fehler
- Sicherheit gewährleistet

---

### 3. Performance-Optimierung

**Priorität:** Niedrig  
**Aufwand:** 3-5 Tage

**Was zu tun ist:**
- **Pagination** für große Datenmengen
  - Aktuell werden alle Kunden auf einmal geladen
  - Bei 1000+ Kunden könnte das langsam werden
  - Lösung: Nur 20-50 Kunden pro Seite laden

- **Caching-Strategien optimieren**
  - Firestore Cache ist bereits aktiviert
  - Könnte für bestimmte Daten optimiert werden

**Vorteile:**
- App läuft schneller
- Weniger Datenverbrauch
- Bessere Benutzererfahrung

---

### 4. Error-Handling verbessern

**Priorität:** Mittel  
**Aufwand:** 2 Tage

**Was zu tun ist:**
- Spezifische Fehlermeldungen für verschiedene Fehlertypen
  - Aktuell: Generische Fehlermeldungen
  - Besser: "Keine Internetverbindung", "Berechtigung verweigert", etc.

- Retry-Strategien für verschiedene Fehler
  - Aktuell: Einheitliche Retry-Logik
  - Besser: Unterschiedliche Strategien je nach Fehlertyp

**Vorteile:**
- Benutzer verstehen Fehler besser
- Bessere Fehlerbehandlung
- Weniger Frustration

---

## 📝 Zusammenfassung

### Was bereits vorhanden ist:
- ✅ Grundlegende Repository-Tests
- ✅ ViewModel-Tests (teilweise)
- ✅ Validierungs-Tests

### Was fehlt:
- ❌ Vollständige Repository-Tests
- ❌ Offline/Online-Szenarien-Tests
- ❌ Storage-Upload-Tests
- ❌ RealtimeDatabaseRepository-Tests
- ❌ Performance-Tests

### Empfohlene nächste Schritte:

1. **Sofort (Priorität Hoch):**
   - Firestore Security Rules prüfen und beheben

2. **Kurzfristig (Priorität Mittel):**
   - Fehlende Repository-Tests hinzufügen
   - Offline/Online-Szenarien testen

3. **Mittelfristig (Priorität Niedrig):**
   - Performance-Optimierung (Pagination)
   - Error-Handling verbessern

---

## 🔧 Wie man Unit-Tests ausführt

### In Android Studio:

1. **Einzelnen Test ausführen:**
   - Rechtsklick auf Test-Methode → "Run 'testName'"

2. **Alle Tests in einer Klasse ausführen:**
   - Rechtsklick auf Test-Klasse → "Run 'ClassName'"

3. **Alle Tests ausführen:**
   - Rechtsklick auf `app/src/test` → "Run 'Tests in we2026_5'"

### Über Terminal:

```bash
# Alle Unit-Tests ausführen
./gradlew test

# Nur bestimmte Test-Klasse
./gradlew test --tests "CustomerRepositoryTest"

# Test-Report ansehen
./gradlew test
# Report: app/build/reports/tests/test/index.html
```

---

## 📚 Weitere Ressourcen

- [Android Testing Guide](https://developer.android.com/training/testing)
- [JUnit Documentation](https://junit.org/junit4/)
- [Mockito Documentation](https://site.mockito.org/)

---

**Ende der Erklärung**
