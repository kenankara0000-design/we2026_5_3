# App-Analyse: Fehler und Verbesserungen

**Projekt:** we2026_5 (TourPlaner 2026)  
**Datum:** 02.02.2026  
**Scope:** Alle möglichen Fehler, Risiken und Verbesserungen

---

## 1. Kritische Punkte / Potenzielle Fehler

### 1.1 Unsichere Null-Assertion (`!!`)

| Datei | Zeile | Beschreibung |
|-------|--------|---------------|
| **CustomerAdapter.kt** | 236 | `binder!!.bind(...)` – Binder wird in derselben Methode gesetzt; durch die Bedingung `binder == null` ist der Aufruf abgesichert. Trotzdem besser: `requireNotNull(binder) { "binder must be set" }.bind(...)` oder lokale Variable nach dem `if` verwenden. |
| **UrlaubScreen.kt** | 61–63 | `customer!!` im Block `if (hasUrlaub)`. Wenn `hasUrlaub` true ist, ist `customer` logisch nicht null, der Compiler erkennt das nicht. **Empfehlung:** Null-sicher formulieren, z. B. `customer?.let { c -> stringResource(..., DateFormatter.formatDate(c.urlaubVon), DateFormatter.formatDate(c.urlaubBis)) } ?: stringResource(R.string.dialog_urlaub_no_urlaub)` |

### 1.2 Fehlerbehandlung in Composable (UrlaubActivity)

In **UrlaubActivity** wird `errorMessage` im Composable ausgewertet und bei Vorhandensein ein Toast gezeigt sowie `viewModel.clearErrorMessage()` aufgerufen. Das läuft bei jeder Recomposition, solange `errorMessage != null` ist – Toast und Clear können mehrfach ausgelöst werden.

**Empfehlung:** In `LaunchedEffect(errorMessage)` kapseln und nur einmal pro neuem Fehler Toast anzeigen und danach clearen.

### 1.3 ImageUploadWorker – Retry bei allen Exceptions

In **ImageUploadWorker.kt** wird bei jeder `Exception` `Result.retry()` zurückgegeben. Nicht wiederholbare Fehler (z. B. fehlende Parameter, fehlende Datei) führen so zu unnötigen Retries.

**Empfehlung:**  
- Bei fehlenden Parametern oder fehlender Datei: `Result.failure()`  
- Nur bei Netzwerk-/Timeout-Fehlern: `Result.retry()`

### 1.4 FirebaseConfig – setPersistenceEnabled Aufrufreihenfolge

`FirebaseDatabase.getInstance().setPersistenceEnabled(true)` wird in `FirebaseConfig.onCreate()` aufgerufen. Das ist korrekt, sofern vorher nirgends auf die Realtime Database zugegriffen wird. Sollte irgendwo (z. B. in einem ContentProvider) vorher eine DB-Referenz erzeugt werden, müsste die Persistence dort bereits gesetzt sein – aktuell unkritisch, aber bei Erweiterung beachten.

---

## 2. Manifest & Konfiguration

### 2.1 FileProvider Meta-Daten

```xml
<meta-data android:name="android.support.FILE_PROVIDER_PATHS" ... />
```

Der Name ist die alte Support-Library-Konstante. AndroidX akzeptiert das aus Kompatibilitätsgründen. Für Neuprojekte ist die Konstante aus `androidx.core.content.FileProvider` (gleicher String) üblich – funktional kein Fehler.

### 2.2 Doppelte Backup-/Extraction-Angaben

`android:dataExtractionRules` und `android:fullBackupContent` sind gesetzt, die zugehörigen XML-Dateien existieren (`data_extraction_rules.xml`, `backup_rules.xml`). In `data_extraction_rules.xml` steht ein TODO zu `<include>`/`<exclude>` – bei sensiblen Daten (z. B. Firebase-bezogen) Backup-Regeln prüfen und ggf. einschränken.

### 2.3 Theme-Konsistenz

Einige Activities haben explizit `android:theme="@style/Theme.We2026_5.LightBackground"` (z. B. MainActivity, AddCustomerActivity, UrlaubActivity), andere nicht (CustomerManagerActivity, TourPlannerActivity, …). Wenn überall heller Hintergrund gewünscht ist, Theme entweder in einem Basis-Theme oder pro Activity setzen.

### 2.4 Versionen (libs.versions.toml vs. build.gradle.kts)

In **libs.versions.toml** steht `kotlin = "1.8.22"`, in **build.gradle.kts** (Root) wird Kotlin `2.0.21` verwendet. Die Katalog-Version wird für Koin genutzt; Kotlin kommt aus dem Plugin. Empfehlung: Kotlin-Version im Katalog an die tatsächlich genutzte Version (2.0.21) anpassen, um Verwirrung zu vermeiden.

---

## 3. Build & Dependencies

### 3.1 ProGuard / Release

In **app/build.gradle.kts** ist `isMinifyEnabled = false`. Für Release-Builds wäre Minify/R8 + ProGuard-Regeln sinnvoll (kleinere APK, schwerer rückentwickelbar). Dafür ProGuard-Regeln für Firebase, Koin, Modellklassen (z. B. `Customer`, `KundenListe`) und ggf. Kotlin Metadata prüfen.

### 3.2 Glide mit Kotlin

Es wird `annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")` verwendet. Bei reinem Kotlin-Projekt ohne kapt kann der Glide-Compiler ggf. nicht laufen. Wenn Glide korrekt arbeitet, ist nichts zu tun; sonst kapt für die Glide-Annotationen aktivieren.

---

## 4. Architektur & Code-Qualität

### 4.1 Unnötiger Code (ListeErstellenActivity, etc.)

In **ListeErstellenActivity**, **TerminRegelErstellenActivity**, **StatisticsActivity** gibt es:

```kotlin
@Suppress("UNUSED_EXPRESSION")
viewModel
```

Damit wird nur das ViewModel „angetippt“, damit es vor `setContent` erzeugt wird. Das ist redundant – das ViewModel wird ohnehin beim ersten Zugriff (z. B. `viewModel.state`) erstellt. **Empfehlung:** Diese Zeilen und das `@Suppress` entfernen.

### 4.2 AddCustomerViewModel ohne Repository

**AddCustomerViewModel** hat keine Repository-Abhängigkeit; das Speichern passiert in **AddCustomerActivity** mit injiziertem `CustomerRepository`. Das ist ein bewusster Architektur-Entscheid (ViewModel nur UI-State, Activity koordiniert Persistenz). Konsistenz zu anderen Screens prüfen; ansonsten kein Fehler.

### 4.3 LiveData vs. StateFlow in ViewModels

Viele ViewModels nutzen noch **LiveData** und die UI **observeAsState**. Für neue Compose-Screens ist **StateFlow** + `collectAsState()` üblicher und vermeidet Lifecycle-Details. Kein Fehler, aber eine mögliche zukünftige Vereinheitlichung.

---

## 5. Netzwerk & Firebase

### 5.1 FirebaseSyncManager – „Syncing“-Anzeige

`FirebaseSyncManager.waitForSync()` und `hasPendingWrites()` sind für die Realtime Database im Wesentlichen No-Ops (sofort zurück, bzw. immer false). Die UI zeigt trotzdem kurz „Syncing“ an (z. B. in MainActivity), weil `checkPendingWrites()` `_isSyncing.postValue(true)` setzt und danach sofort wieder false. Nutzer könnte einen kurzen Flackern sehen. Entweder Logik an echte Sync-Information anbinden (falls verfügbar) oder Anzeige entfernen/umschreiben.

### 5.2 Logging

In Repositories und Workern wird teils `android.util.Log` mit festen Tags verwendet. Für Produktion: Log-Level begrenzen oder einen zentralen Logger (z. B. nur in Debug-Builds oder über Crashlytics) verwenden, um keine sensiblen Daten in Logs zu haben.

---

## 6. Barrierefreiheit & UX

### 6.1 Content Descriptions

Viele Icons haben `contentDescription = null` (z. B. in MainScreen, CustomerManagerScreen, ErledigungSheetContent, TourPlannerScreen). Wenn die Icons semantisch wichtig sind (z. B. Aktionen), sollten sie eine kurze, sprechende Beschreibung bekommen (z. B. aus `strings.xml`), um Barrierefreiheit zu verbessern.

### 6.2 Hardcodierte Strings (CustomerPhotoManager)

In **CustomerPhotoManager.kt** (Dialog):  
`"Foto hinzufügen"`, `"Kamera"`, `"Galerie"`, `"Abbrechen"` sind fest im Code. **Empfehlung:** In `strings.xml` auslagern und `getString()`/`stringResource()` verwenden (auch für Lokalisierung).

---

## 7. Berechtigungen & Android-Versionen

### 7.1 WRITE_EXTERNAL_STORAGE

`WRITE_EXTERNAL_STORAGE` mit `android:maxSdkVersion="32"` ist gesetzt – ab API 33 wird sie nicht mehr genutzt, was korrekt ist. Prüfen, ob für eure Nutzung überhaupt Schreibzugriffe auf externen Speicher nötig sind; Fotos liegen bereits in `getExternalFilesDir(Environment.DIRECTORY_PICTURES)` – dann reicht ggf. keine weitere Storage-Permission.

### 7.2 Android 13+ (API 33) – Galerie

Für Bildauswahl aus der Galerie wird `ActivityResultContracts.GetContent()` genutzt. Ab Android 13 gibt es granulare Medienberechtigungen (z. B. READ_MEDIA_IMAGES). Mit GetContent() öffnet sich der System-Picker; oft ist keine zusätzliche Permission nötig. Falls bei bestimmten Geräten/Herstellern Probleme auftreten, READ_MEDIA_IMAGES (mit maxSdkVersion/Alternativen je nach Ziel-API) prüfen.

---

## 8. Deprecated APIs & Modell

### 8.1 Customer / KundenListe

In **Customer.kt** und **KundenListe.kt** sind mehrere Felder und Methoden mit `@Deprecated` markiert (z. B. alte Termin-Felder, Migration). Das ist dokumentiert; langfristig sollten alle Aufrufer auf die neuen Strukturen (z. B. `intervalle`, `verschobeneTermine`) umgestellt und die veralteten Teile entfernt werden.

---

## 9. Kurzüberblick: Prioritäten

| Priorität | Thema | Aktion |
|-----------|--------|--------|
| Hoch | UrlaubScreen `customer!!` | Null-sicher umbauen (z. B. `customer?.let { ... }`) |
| Hoch | UrlaubActivity Fehler-Anzeige | Toast/Clear in `LaunchedEffect(errorMessage)` |
| Mittel | ImageUploadWorker | Retry nur bei Netzwerk-/Timeout-Fehlern, sonst `Result.failure()` |
| Mittel | CustomerPhotoManager | Dialog-Texte in strings.xml auslagern |
| Mittel | ListeErstellenActivity etc. | Überflüssige `viewModel`-Zeile + @Suppress entfernen |
| Niedrig | Syncing-Anzeige | An echte Sync-Info anbinden oder Anzeige anpassen |
| Niedrig | contentDescription | Fehlende Descriptions für wichtige Icons ergänzen |
| Niedrig | libs.versions.toml | Kotlin-Version auf 2.0.21 angleichen |
| Niedrig | Release Build | ProGuard/R8 + Regeln prüfen |

---

## 10. Positive Punkte

- Koin für DI, ViewModels sauber eingebunden (inkl. parameterisierter UrlaubViewModel).
- Firebase Realtime Database mit Persistence und Retry (FirebaseRetryHelper) genutzt.
- Einheitliche Hilfstypen: LoadState, Result, AppErrorMapper.
- Wichtige XML-Layouts und viele Compose-Screens haben contentDescription bzw. stringResource für Texte.
- Aktivitäten prüfen z. B. CUSTOMER_ID und beenden sich bei fehlenden Extras.
- NetworkMonitor lifecycle-aware (CoroutineScope, unregister in Activity).

Wenn du möchtest, kann ich dir konkrete Code-Änderungen (Patches) für die hochpriorisierten Punkte vorschlagen (UrlaubScreen, UrlaubActivity, ImageUploadWorker, CustomerPhotoManager, ListeErstellenActivity).
