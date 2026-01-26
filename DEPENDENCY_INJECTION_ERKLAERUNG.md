# 🔧 Dependency Injection (DI) - Erklärung

## Was ist Dependency Injection?

**Dependency Injection (DI)** ist ein Design-Pattern, bei dem Abhängigkeiten (Dependencies) von außen in eine Klasse "injiziert" werden, anstatt dass die Klasse sie selbst erstellt.

### Beispiel ohne DI:
```kotlin
class CustomerManagerActivity {
    // ❌ Schlecht: Direkte Erstellung der Abhängigkeit
    private val db = FirebaseFirestore.getInstance()
    
    fun loadCustomers() {
        db.collection("customers")...
    }
}
```

### Beispiel mit DI (Hilt):
```kotlin
@AndroidEntryPoint
class CustomerManagerActivity {
    // ✅ Gut: Abhängigkeit wird injiziert
    @Inject lateinit var customerRepository: CustomerRepository
    
    fun loadCustomers() {
        customerRepository.getAllCustomers()...
    }
}
```

## Warum Dependency Injection?

### ✅ **Vorteile:**
1. **Testbarkeit**: Leicht mockbare Abhängigkeiten für Unit-Tests
2. **Flexibilität**: Einfacher Austausch von Implementierungen
3. **Wartbarkeit**: Zentrale Verwaltung von Abhängigkeiten
4. **Lose Kopplung**: Klassen sind weniger voneinander abhängig
5. **Code-Wiederverwendung**: Einmal definiert, überall verwendbar

## Hilt vs. Koin

### **Hilt** (Empfohlen für dieses Projekt)
- ✅ Von Google entwickelt (offiziell unterstützt)
- ✅ Basierend auf Dagger 2 (bewährt, performant)
- ✅ Kompilierzeit-Validierung (Fehler werden früh erkannt)
- ✅ Gute Integration mit Android Lifecycle
- ✅ Perfekt für Firebase-Projekte

### **Koin**
- ✅ Einfacher zu lernen (keine Annotationen)
- ✅ Laufzeit-Validierung
- ⚠️ Etwas langsamer als Hilt
- ⚠️ Weniger Features

## Wie funktioniert Hilt?

### 1. **Module** - Definiert Abhängigkeiten
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
}
```

### 2. **Repository** - Nutzt injizierte Abhängigkeiten
```kotlin
class CustomerRepository @Inject constructor(
    private val db: FirebaseFirestore
) {
    suspend fun getAllCustomers(): List<Customer> {
        // ...
    }
}
```

### 3. **ViewModel** - Nutzt Repository
```kotlin
@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel() {
    val customers = repository.getAllCustomers()
}
```

### 4. **Activity** - Nutzt ViewModel
```kotlin
@AndroidEntryPoint
class CustomerManagerActivity : AppCompatActivity() {
    private val viewModel: CustomerViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.customers.observe(this) { customers ->
            // UI aktualisieren
        }
    }
}
```

## MVVM-Pattern mit Hilt

```
Activity/Fragment (UI)
    ↓ verwendet
ViewModel (Logik)
    ↓ verwendet
Repository (Daten)
    ↓ verwendet
Firebase/API (Datenquelle)
```

### **Vorteile:**
- ✅ **Separation of Concerns**: UI, Logik und Daten getrennt
- ✅ **Testbarkeit**: Jede Schicht einzeln testbar
- ✅ **Wiederverwendbarkeit**: ViewModels können von mehreren Activities genutzt werden
- ✅ **Lifecycle-Aware**: ViewModels überleben Configuration Changes

## Unit-Tests mit DI

### Ohne DI (schwer testbar):
```kotlin
class CustomerManagerActivity {
    private val db = FirebaseFirestore.getInstance() // ❌ Kann nicht gemockt werden
}
```

### Mit DI (leicht testbar):
```kotlin
@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val repository: CustomerRepository
) : ViewModel()

// In Tests:
val mockRepository = mock<CustomerRepository>()
val viewModel = CustomerViewModel(mockRepository) // ✅ Einfach zu testen
```

## Zusammenfassung

**Dependency Injection** macht Code:
- ✅ Testbarer
- ✅ Wartbarer
- ✅ Flexibler
- ✅ Professioneller

**Hilt** ist die beste Wahl für Android-Projekte mit Firebase!
