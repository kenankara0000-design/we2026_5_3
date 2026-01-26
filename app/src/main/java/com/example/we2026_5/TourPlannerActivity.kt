package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.databinding.ActivityTourPlannerBinding
import com.example.we2026_5.ui.tourplanner.TourPlannerViewModel
import com.example.we2026_5.FirebaseRetryHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class TourPlannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTourPlannerBinding
    private val viewModel: TourPlannerViewModel by viewModel()
    private val repository: CustomerRepository by inject()
    private val listeRepository: com.example.we2026_5.data.repository.KundenListeRepository by inject()
    private lateinit var adapter: CustomerAdapter
    private lateinit var weekAdapter: WeekViewAdapter
    private var viewDate = Calendar.getInstance()
    private var isWeekView = false
    private lateinit var gestureDetector: GestureDetectorCompat
    private lateinit var networkMonitor: NetworkMonitor
    private var pressedHeaderButton: String? = null // "Karte", "Heute", "Woche"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTourPlannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CustomerAdapter(
            items = mutableListOf(),
            context = this,
            onClick = { customer ->
                val intent = Intent(this, CustomerDetailActivity::class.java).apply {
                    putExtra("CUSTOMER_ID", customer.id)
                }
                startActivity(intent)
            }
        )
        
        // Callbacks für Firebase-Operationen setzen
        setupAdapterCallbacks()

        binding.rvTourList.layoutManager = LinearLayoutManager(this)
        binding.rvTourList.adapter = adapter
        
        // Wochenansicht-Adapter initialisieren
        weekAdapter = WeekViewAdapter(
            weekData = emptyMap(),
            context = this,
            onCustomerClick = { customer ->
                val intent = Intent(this, CustomerDetailActivity::class.java).apply {
                    putExtra("CUSTOMER_ID", customer.id)
                }
                startActivity(intent)
            },
            customerAdapterFactory = { items ->
                val dayAdapter = CustomerAdapter(
                    items = items.toMutableList(),
                    context = this,
                    onClick = { customer ->
                        val intent = Intent(this, CustomerDetailActivity::class.java).apply {
                            putExtra("CUSTOMER_ID", customer.id)
                        }
                        startActivity(intent)
                    }
                )
                setupAdapterCallbacksForAdapter(dayAdapter)
                dayAdapter
            }
        )
        binding.rvWeekView.layoutManager = LinearLayoutManager(this)
        binding.rvWeekView.adapter = weekAdapter
        
        // Drag & Drop für Kunden-Reihenfolge
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                return adapter.onItemMove(fromPosition, toPosition)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Nicht verwendet
            }
            
            override fun isLongPressDragEnabled(): Boolean {
                return true // Lang drücken zum Verschieben
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvTourList)
        
        // Callback für Section-Toggle setzen
        adapter.onSectionToggle = { sectionType ->
            viewModel.toggleSection(sectionType)
            reloadCurrentView() // Daten neu laden wenn Section getoggelt wird
        }
        
        // Initial: Tagesansicht
        updateViewMode()
        updateHeaderButtonStates() // Initial Header-Button-Zustand setzen
        
        // ViewModel Observer einrichten
        observeViewModel()

        binding.btnBackFromTour.setOnClickListener { finish() }

        binding.btnPrevDay.setOnClickListener {
            if (isWeekView) {
                viewDate.add(Calendar.WEEK_OF_YEAR, -1)
            } else {
                viewDate.add(Calendar.DAY_OF_YEAR, -1)
            }
            updateDisplay()
            updateTodayButtonState()
        }

        binding.btnNextDay.setOnClickListener {
            if (isWeekView) {
                viewDate.add(Calendar.WEEK_OF_YEAR, 1)
            } else {
                viewDate.add(Calendar.DAY_OF_YEAR, 1)
            }
            updateDisplay()
            updateTodayButtonState()
        }

        binding.btnToday.setOnClickListener {
            pressedHeaderButton = "Heute"
            updateHeaderButtonStates()
            // Auf heute springen - immer ein neues Calendar-Objekt erstellen
            val heute = Calendar.getInstance()
            val heuteStart = Calendar.getInstance().apply {
                set(Calendar.YEAR, heute.get(Calendar.YEAR))
                set(Calendar.MONTH, heute.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, heute.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            // Datum setzen
            viewDate = heuteStart
            
            // Wenn Wochenansicht aktiv ist, zur Tagesansicht wechseln
            if (isWeekView) {
                isWeekView = false
                updateViewMode()
            } else {
                // Tagesansicht: Datum aktualisieren und Daten neu laden
                // Explizit loadTourData aufrufen, um sicherzustellen, dass Daten geladen werden
                val heuteTimestamp = getStartOfDay(heuteStart.timeInMillis)
                loadTourData(heuteTimestamp)
                updateDisplay()
                updateTodayButtonState()
            }
        }

        binding.btnMapView.setOnClickListener {
            pressedHeaderButton = "Karte"
            updateHeaderButtonStates()
            val intent = Intent(this, MapViewActivity::class.java)
            startActivity(intent)
            // Nach Rückkehr Button-Zustand zurücksetzen
            pressedHeaderButton = null
            updateHeaderButtonStates()
        }
        
        // Toggle zwischen Tag- und Wochenansicht
        binding.btnToggleView.setOnClickListener {
            pressedHeaderButton = if (isWeekView) null else "Woche"
            isWeekView = !isWeekView
            updateViewMode()
            updateHeaderButtonStates()
        }
        
        // Pull-to-Refresh
        binding.swipeRefresh.setOnRefreshListener {
            reloadCurrentView()
            binding.swipeRefresh.isRefreshing = false
        }
        
        // Offline-Status-Monitoring
        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()
        networkMonitor.isOnline.observe(this) { isOnline ->
            binding.tvOfflineStatus.visibility = if (isOnline) View.GONE else View.VISIBLE
        }
        
        // Swipe-Gesten für Datum-Wechsel einrichten
        setupSwipeGestures()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
    }
    
    private fun setupSwipeGestures() {
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val deltaX = e2.x - e1.x
                val deltaY = e2.y - e1.y
                
                // Nur horizontale Swipes erkennen (nicht vertikale)
                if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 100) {
                    if (deltaX > 0) {
                        // Swipe nach rechts = vorheriger Tag
                        viewDate.add(Calendar.DAY_OF_YEAR, -1)
                        updateDisplay()
                        return true
                    } else {
                        // Swipe nach links = nächster Tag
                        viewDate.add(Calendar.DAY_OF_YEAR, 1)
                        updateDisplay()
                        return true
                    }
                }
                return false
            }
        })
        
        // Swipe-Gesten auf dem RecyclerView aktivieren
        binding.rvTourList.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event) || false
        }
        
        // Auch auf dem gesamten Layout aktivieren (falls RecyclerView leer ist)
        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event) || false
        }
    }

    override fun onStart() {
        super.onStart()
        updateDisplay()
        // Button-Zustand zurücksetzen wenn von MapViewActivity zurückgekehrt
        if (pressedHeaderButton == "Karte") {
            pressedHeaderButton = null
            updateHeaderButtonStates()
        }
    }

    private fun observeViewModel() {
        // Tour-Items beobachten (Tagesansicht)
        viewModel.tourItems.observe(this) { items ->
            if (!isWeekView) {
                adapter.updateData(items, getStartOfDay(viewDate.timeInMillis))
                
                // Empty State anzeigen wenn keine Kunden vorhanden
                if (items.isEmpty()) {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.rvTourList.visibility = View.GONE
                } else {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.rvTourList.visibility = View.VISIBLE
                }
            }
        }
        
        // Wochen-Items beobachten
        viewModel.weekItems.observe(this) { weekData ->
            if (isWeekView) {
                val weekStart = Calendar.getInstance().apply {
                    timeInMillis = viewDate.timeInMillis
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                
                weekAdapter.updateWeekData(weekData, weekStart)
                
                // Empty State prüfen
                val hasAnyData = weekData.values.any { it.isNotEmpty() }
                if (hasAnyData) {
                    binding.emptyStateLayout.visibility = View.GONE
                    binding.rvWeekView.visibility = View.VISIBLE
                } else {
                    binding.emptyStateLayout.visibility = View.VISIBLE
                    binding.rvWeekView.visibility = View.GONE
                }
            }
        }
        
        // Loading-State beobachten
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                binding.emptyStateLayout.visibility = View.GONE
                binding.errorStateLayout.visibility = View.GONE
            }
        }
        
        // Error-State beobachten
        viewModel.error.observe(this) { errorMessage ->
            if (errorMessage != null) {
                showErrorState(errorMessage)
            } else {
                binding.errorStateLayout.visibility = View.GONE
            }
        }
    }

    private fun updateDisplay() {
        if (isWeekView) {
            // Wochenansicht: Zeige Woche (z.B. "KW 4, 2026")
            val cal = Calendar.getInstance().apply {
                timeInMillis = viewDate.timeInMillis
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            val weekNumber = cal.get(Calendar.WEEK_OF_YEAR)
            val year = cal.get(Calendar.YEAR)
            binding.tvCurrentDate.text = "KW $weekNumber, $year"
            loadWeekData(viewDate.timeInMillis)
        } else {
            // Tagesansicht
            val fmt = SimpleDateFormat("EEE, dd.MM.yyyy", Locale.GERMANY)
            binding.tvCurrentDate.text = fmt.format(viewDate.time)
            loadTourData(viewDate.timeInMillis)
        }
    }
    
    private fun updateViewMode() {
        if (isWeekView) {
            binding.rvTourList.visibility = View.GONE
            binding.rvWeekView.visibility = View.VISIBLE
            binding.btnPrevDay.contentDescription = "Vorherige Woche"
            binding.btnNextDay.contentDescription = "Nächste Woche"
            binding.btnToggleView.contentDescription = "Tagesansicht"
        } else {
            binding.rvTourList.visibility = View.VISIBLE
            binding.rvWeekView.visibility = View.GONE
            binding.btnPrevDay.contentDescription = "Vorheriger Tag"
            binding.btnNextDay.contentDescription = "Nächster Tag"
            binding.btnToggleView.contentDescription = "Wochenansicht"
        }
        updateHeaderButtonStates() // Button-Zustände aktualisieren
        updateDisplay()
    }
    
    private fun updateTodayButtonState() {
        // Diese Funktion wird jetzt von updateHeaderButtonStates() übernommen
        // Behalten für Kompatibilität, aber Logik ist in updateHeaderButtonStates()
        updateHeaderButtonStates()
    }
    
    private fun updateHeaderButtonStates() {
        // Farben definieren
        val activeBackgroundColor = ContextCompat.getColor(this, R.color.status_warning) // Orange
        val inactiveBackgroundColor = ContextCompat.getColor(this, R.color.button_blue) // Blau
        val textColor = ContextCompat.getColor(this, R.color.white)
        
        // Button-Zeile bleibt immer blau
        val barBackgroundColor = ContextCompat.getColor(this, R.color.primary_blue)
        binding.buttonBar.setBackgroundColor(barBackgroundColor)
        
        when (pressedHeaderButton) {
            "Karte" -> {
                // Aktiver Button: Orange Hintergrund
                binding.btnMapView.setBackgroundColor(activeBackgroundColor)
                binding.btnMapView.setTextColor(textColor)
                binding.btnMapView.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                // Inaktive Buttons: Blau Hintergrund
                binding.btnToday.setBackgroundColor(inactiveBackgroundColor)
                binding.btnToday.setTextColor(textColor)
                binding.btnToday.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                binding.btnToggleView.setBackgroundColor(inactiveBackgroundColor)
                binding.btnToggleView.setTextColor(textColor)
                binding.btnToggleView.iconTint = android.content.res.ColorStateList.valueOf(textColor)
            }
            "Heute" -> {
                // Aktiver Button: Orange Hintergrund
                binding.btnToday.setBackgroundColor(activeBackgroundColor)
                binding.btnToday.setTextColor(textColor)
                binding.btnToday.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                // Inaktive Buttons: Blau Hintergrund
                binding.btnMapView.setBackgroundColor(inactiveBackgroundColor)
                binding.btnMapView.setTextColor(textColor)
                binding.btnMapView.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                binding.btnToggleView.setBackgroundColor(inactiveBackgroundColor)
                binding.btnToggleView.setTextColor(textColor)
                binding.btnToggleView.iconTint = android.content.res.ColorStateList.valueOf(textColor)
            }
            "Woche" -> {
                // Aktiver Button: Orange Hintergrund
                binding.btnToggleView.setBackgroundColor(activeBackgroundColor)
                binding.btnToggleView.setTextColor(textColor)
                binding.btnToggleView.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                // Inaktive Buttons: Blau Hintergrund
                binding.btnMapView.setBackgroundColor(inactiveBackgroundColor)
                binding.btnMapView.setTextColor(textColor)
                binding.btnMapView.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                binding.btnToday.setBackgroundColor(inactiveBackgroundColor)
                binding.btnToday.setTextColor(textColor)
                binding.btnToday.iconTint = android.content.res.ColorStateList.valueOf(textColor)
            }
            else -> {
                // Kein Button gedrückt: Alle blau
                binding.btnMapView.setBackgroundColor(inactiveBackgroundColor)
                binding.btnMapView.setTextColor(textColor)
                binding.btnMapView.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                binding.btnToday.setBackgroundColor(inactiveBackgroundColor)
                binding.btnToday.setTextColor(textColor)
                binding.btnToday.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                binding.btnToggleView.setBackgroundColor(inactiveBackgroundColor)
                binding.btnToggleView.setTextColor(textColor)
                binding.btnToggleView.iconTint = android.content.res.ColorStateList.valueOf(textColor)
            }
        }
    }

    private fun loadTourData(selectedTimestamp: Long) {
        viewModel.loadTourData(selectedTimestamp) { sectionType ->
            viewModel.isSectionExpanded(sectionType)
        }
    }
    
    private fun loadWeekData(selectedTimestamp: Long) {
        viewModel.loadWeekData(selectedTimestamp) { sectionType ->
            viewModel.isSectionExpanded(sectionType)
        }
    }
    
    private fun reloadCurrentView() {
        if (isWeekView) {
            loadWeekData(viewDate.timeInMillis)
        } else {
            loadTourData(viewDate.timeInMillis)
        }
    }
    
    private fun showErrorState(message: String) {
        binding.errorStateLayout.visibility = View.VISIBLE
        binding.rvTourList.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.tvErrorMessage.text = message
        
        binding.btnRetry.setOnClickListener {
            reloadCurrentView()
        }
    }

    private fun setupAdapterCallbacks() {
        setupAdapterCallbacksForAdapter(adapter)
    }
    
    private fun setupAdapterCallbacksForAdapter(adapter: CustomerAdapter) {
        // Abholung
        adapter.onAbholung = { customer ->
            if (!customer.abholungErfolgt) {
                CoroutineScope(Dispatchers.Main).launch {
                    val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                        operation = { 
                            repository.updateCustomer(customer.id, mapOf("abholungErfolgt" to true))
                        },
                        context = this@TourPlannerActivity,
                        errorMessage = "Fehler beim Registrieren der Abholung. Bitte erneut versuchen.",
                        maxRetries = 3
                    )
                    if (success == true) {
                        android.widget.Toast.makeText(this@TourPlannerActivity, "Abholung registriert", android.widget.Toast.LENGTH_SHORT).show()
                        // Daten werden automatisch durch Echtzeit-Listener aktualisiert
                        // WICHTIG: reloadCurrentView() NACH clearPressedButtons() aufrufen, damit Button-Zustand erhalten bleibt
                        // Button-Zustand erst nach längerer Verzögerung zurücksetzen, damit visuelle Änderung sichtbar bleibt
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            adapter.clearPressedButtons()
                            // Nach dem Zurücksetzen des Button-Zustands die View neu laden
                            reloadCurrentView() // Für Kompatibilität (wird durch Echtzeit-Listener automatisch aktualisiert)
                        }, 2000) // 2 Sekunden Verzögerung, damit visuelle Änderung deutlich sichtbar bleibt
                    }
                }
            }
        }
        
        // Auslieferung
        adapter.onAuslieferung = { customer ->
            if (!customer.auslieferungErfolgt) {
                val wasAbholungErfolgt = customer.abholungErfolgt
                CoroutineScope(Dispatchers.Main).launch {
                    val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                        operation = { 
                            repository.updateCustomer(customer.id, mapOf("auslieferungErfolgt" to true))
                        },
                        context = this@TourPlannerActivity,
                        errorMessage = "Fehler beim Registrieren der Auslieferung. Bitte erneut versuchen.",
                        maxRetries = 3
                    )
                    if (success == true) {
                        android.widget.Toast.makeText(this@TourPlannerActivity, "Auslieferung registriert", android.widget.Toast.LENGTH_SHORT).show()
                        // Daten werden automatisch durch Echtzeit-Listener aktualisiert
                        // WICHTIG: reloadCurrentView() NACH clearPressedButtons() aufrufen, damit Button-Zustand erhalten bleibt
                        // Button-Zustand erst nach längerer Verzögerung zurücksetzen, damit visuelle Änderung sichtbar bleibt
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            adapter.clearPressedButtons()
                            // Nach dem Zurücksetzen des Button-Zustands die View neu laden
                            if (wasAbholungErfolgt) {
                                resetTourCycle(customer.id)
                            } else {
                                reloadCurrentView() // Für Kompatibilität (wird durch Echtzeit-Listener automatisch aktualisiert)
                            }
                        }, 2000) // 2 Sekunden Verzögerung, damit visuelle Änderung deutlich sichtbar bleibt
                    }
                }
            }
        }
        
        // Tour-Zyklus zurücksetzen
        adapter.onResetTourCycle = { customerId ->
            resetTourCycle(customerId)
        }
        
        // Verschieben
        adapter.onVerschieben = { customer, newDate, alleVerschieben ->
            CoroutineScope(Dispatchers.Main).launch {
                val success = if (alleVerschieben) {
                    // Alle zukünftigen Termine verschieben
                    val aktuellerFaelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum
                                             else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
                    val diff = newDate - aktuellerFaelligAm
                    val neuerLetzterTermin = customer.letzterTermin + diff
                    FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                        operation = { 
                            repository.updateCustomer(customer.id, mapOf(
                                "letzterTermin" to neuerLetzterTermin,
                                "verschobenAufDatum" to 0
                            ))
                        },
                        context = this@TourPlannerActivity,
                        errorMessage = "Fehler beim Verschieben. Bitte erneut versuchen.",
                        maxRetries = 3
                    )
                } else {
                    // Nur diesen Termin verschieben
                    FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                        operation = { 
                            repository.updateCustomer(customer.id, mapOf("verschobenAufDatum" to newDate))
                        },
                        context = this@TourPlannerActivity,
                        errorMessage = "Fehler beim Verschieben. Bitte erneut versuchen.",
                        maxRetries = 3
                    )
                }
                if (success == true) {
                    android.widget.Toast.makeText(this@TourPlannerActivity, 
                        if (alleVerschieben) "Alle zukünftigen Termine verschoben" else "Termin verschoben", 
                        android.widget.Toast.LENGTH_SHORT).show()
                    // Daten werden automatisch durch Echtzeit-Listener aktualisiert
                    // WICHTIG: reloadCurrentView() NACH clearPressedButtons() aufrufen, damit Button-Zustand erhalten bleibt
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        adapter.clearPressedButtons()
                        reloadCurrentView() // Für Kompatibilität (wird durch Echtzeit-Listener automatisch aktualisiert)
                    }, 2000) // 2 Sekunden Verzögerung, damit visuelle Änderung deutlich sichtbar bleibt
                }
            }
        }
        
        // Urlaub
        adapter.onUrlaub = { customer, von, bis ->
            CoroutineScope(Dispatchers.Main).launch {
                val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = { 
                        repository.updateCustomer(customer.id, mapOf(
                            "urlaubVon" to von, 
                            "urlaubBis" to bis
                        ))
                    },
                    context = this@TourPlannerActivity,
                    errorMessage = "Fehler beim Eintragen des Urlaubs. Bitte erneut versuchen.",
                    maxRetries = 3
                )
                if (success == true) {
                    android.widget.Toast.makeText(this@TourPlannerActivity, "Urlaub eingetragen", android.widget.Toast.LENGTH_SHORT).show()
                    // Daten werden automatisch durch Echtzeit-Listener aktualisiert
                    // WICHTIG: reloadCurrentView() NACH clearPressedButtons() aufrufen, damit Button-Zustand erhalten bleibt
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        adapter.clearPressedButtons()
                        reloadCurrentView() // Für Kompatibilität (wird durch Echtzeit-Listener automatisch aktualisiert)
                    }, 2000) // 2 Sekunden Verzögerung, damit visuelle Änderung deutlich sichtbar bleibt
                }
            }
        }
        
        // Rückgängig
        adapter.onRueckgaengig = { customer ->
            val updates = mutableMapOf<String, Any>()
            if (customer.abholungErfolgt) updates["abholungErfolgt"] = false
            if (customer.auslieferungErfolgt) updates["auslieferungErfolgt"] = false
            
            CoroutineScope(Dispatchers.Main).launch {
                val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = { 
                        repository.updateCustomer(customer.id, updates)
                    },
                    context = this@TourPlannerActivity,
                    errorMessage = "Fehler beim Rückgängigmachen. Bitte erneut versuchen.",
                    maxRetries = 3
                )
                if (success == true) {
                    android.widget.Toast.makeText(this@TourPlannerActivity, "Rückgängig gemacht", android.widget.Toast.LENGTH_SHORT).show()
                    // Daten werden automatisch durch Echtzeit-Listener aktualisiert
                    reloadCurrentView() // Für Kompatibilität (wird durch Echtzeit-Listener automatisch aktualisiert)
                }
            }
        }
        
        // Termin-Klick: Öffne Termin-Detail-Dialog
        adapter.onTerminClick = { customer, terminDatum ->
            showTerminDetailDialog(customer, terminDatum)
        }
        
        // Callbacks für Datum-Berechnung (für A/L Button-Aktivierung)
        adapter.getAbholungDatum = { customer ->
            // Berechne Abholungsdatum für den angezeigten Tag
            val viewDateStart = getStartOfDay(viewDate.timeInMillis)
            calculateAbholungDatum(customer, viewDateStart)
        }
        
        adapter.getAuslieferungDatum = { customer ->
            // Berechne Auslieferungsdatum für den angezeigten Tag
            val viewDateStart = getStartOfDay(viewDate.timeInMillis)
            calculateAuslieferungDatum(customer, viewDateStart)
        }
    }
    
    private fun calculateAbholungDatum(customer: Customer, viewDateStart: Long): Long {
        // Für Listen-Kunden: Prüfe ob heute ein Abholungstag ist
        if (customer.listeId.isNotEmpty()) {
            // Lade Liste synchron (vereinfacht - könnte verbessert werden)
            var liste: com.example.we2026_5.KundenListe? = null
            kotlinx.coroutines.runBlocking {
                liste = listeRepository.getListeById(customer.listeId)
            }
            
            if (liste != null) {
                // Prüfe alle Intervalle der Liste
                liste!!.intervalle.forEach { intervall ->
                    val abholungStart = getStartOfDay(intervall.abholungDatum)
                    
                    if (!intervall.wiederholen) {
                        // Einmaliges Intervall: Prüfe ob Abholungsdatum heute fällig ist
                        if (abholungStart == viewDateStart) {
                            return intervall.abholungDatum
                        }
                    } else {
                        // Wiederholendes Intervall: Berechne korrektes Datum
                        val intervallTage = intervall.intervallTage.coerceIn(1, 365)
                        val intervallTageLong = intervallTage.toLong()
                        
                        if (viewDateStart >= abholungStart) {
                            val tageSeitAbholung = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(viewDateStart - abholungStart)
                            // Berechne Zyklus und erwartetes Datum
                            val zyklus = tageSeitAbholung / intervallTageLong
                            val erwartetesDatum = abholungStart + java.util.concurrent.TimeUnit.DAYS.toMillis(zyklus * intervallTageLong)
                            val erwartetesDatumStart = getStartOfDay(erwartetesDatum)
                            
                            // Prüfe ob viewDateStart genau auf einem Zyklus liegt
                            if (erwartetesDatumStart == viewDateStart && tageSeitAbholung <= 365) {
                                return erwartetesDatum
                            }
                        }
                    }
                }
            }
            return 0L // Nicht fällig an diesem Tag
        } else {
            // Für Kunden ohne Liste
            if (customer.verschobenAufDatum > 0) {
                val verschobenStart = getStartOfDay(customer.verschobenAufDatum)
                if (verschobenStart == viewDateStart) return customer.verschobenAufDatum
            }
            val abholungStart = getStartOfDay(customer.abholungDatum)
            if (abholungStart == viewDateStart) return customer.abholungDatum
            // Für wiederholende Termine
            if (customer.wiederholen && customer.letzterTermin > 0) {
                val naechsteAbholung = customer.letzterTermin + java.util.concurrent.TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
                val naechsteAbholungStart = getStartOfDay(naechsteAbholung)
                if (naechsteAbholungStart == viewDateStart) return naechsteAbholung
            }
            return 0L
        }
    }
    
    private fun calculateAuslieferungDatum(customer: Customer, viewDateStart: Long): Long {
        // Ähnlich wie Abholungsdatum
        if (customer.listeId.isNotEmpty()) {
            var liste: com.example.we2026_5.KundenListe? = null
            kotlinx.coroutines.runBlocking {
                liste = listeRepository.getListeById(customer.listeId)
            }
            
            if (liste != null) {
                liste!!.intervalle.forEach { intervall ->
                    val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
                    
                    if (!intervall.wiederholen) {
                        // Einmaliges Intervall: Prüfe ob Auslieferungsdatum heute fällig ist
                        if (auslieferungStart == viewDateStart) {
                            return intervall.auslieferungDatum
                        }
                    } else {
                        // Wiederholendes Intervall: Berechne korrektes Datum
                        val intervallTage = intervall.intervallTage.coerceIn(1, 365)
                        val intervallTageLong = intervallTage.toLong()
                        
                        if (viewDateStart >= auslieferungStart) {
                            val tageSeitAuslieferung = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(viewDateStart - auslieferungStart)
                            // Berechne Zyklus und erwartetes Datum
                            val zyklus = tageSeitAuslieferung / intervallTageLong
                            val erwartetesDatum = auslieferungStart + java.util.concurrent.TimeUnit.DAYS.toMillis(zyklus * intervallTageLong)
                            val erwartetesDatumStart = getStartOfDay(erwartetesDatum)
                            
                            // Prüfe ob viewDateStart genau auf einem Zyklus liegt
                            if (erwartetesDatumStart == viewDateStart && tageSeitAuslieferung <= 365) {
                                return erwartetesDatum
                            }
                        }
                    }
                }
            }
            return 0L
        } else {
            // Für Kunden ohne Liste
            if (customer.verschobenAufDatum > 0) {
                val verschobenStart = getStartOfDay(customer.verschobenAufDatum)
                if (verschobenStart == viewDateStart) return customer.verschobenAufDatum
            }
            val auslieferungStart = getStartOfDay(customer.auslieferungDatum)
            if (auslieferungStart == viewDateStart) return customer.auslieferungDatum
            return 0L
        }
    }
    
    private fun isIntervallFaelligAm(intervall: com.example.we2026_5.ListeIntervall, datum: Long): Boolean {
        val datumStart = getStartOfDay(datum)
        val abholungStart = getStartOfDay(intervall.abholungDatum)
        val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
        
        if (!intervall.wiederholen) {
            // Einmaliges Intervall: Prüfe ob Datum genau Abholungs- oder Auslieferungsdatum ist
            return datumStart == abholungStart || datumStart == auslieferungStart
        }
        
        // Wiederholendes Intervall: Prüfe ob Datum auf einem Wiederholungszyklus liegt
        val intervallTage = intervall.intervallTage.coerceIn(1, 365)
        val intervallTageLong = intervallTage.toLong()
        
        // Prüfe Abholungsdatum - generiere Termine für 365 Tage
        if (datumStart >= abholungStart) {
            val tageSeitAbholung = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(datumStart - abholungStart)
            // Prüfe ob das Datum auf einem Zyklus liegt (innerhalb von 365 Tagen)
            if (tageSeitAbholung <= 365 && tageSeitAbholung % intervallTageLong == 0L) {
                val erwartetesDatum = abholungStart + java.util.concurrent.TimeUnit.DAYS.toMillis(tageSeitAbholung)
                if (datumStart == erwartetesDatum) {
                    return true
                }
            }
        } else {
            // Datum liegt vor dem Startdatum - prüfe ob es ein zukünftiger Termin ist (innerhalb von 365 Tagen)
            val tageBisAbholung = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(abholungStart - datumStart)
            if (tageBisAbholung <= 365 && datumStart == abholungStart) {
                return true
            }
        }
        
        // Prüfe Auslieferungsdatum - generiere Termine für 365 Tage
        if (datumStart >= auslieferungStart) {
            val tageSeitAuslieferung = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(datumStart - auslieferungStart)
            // Prüfe ob das Datum auf einem Zyklus liegt (innerhalb von 365 Tagen)
            if (tageSeitAuslieferung <= 365 && tageSeitAuslieferung % intervallTageLong == 0L) {
                val erwartetesDatum = auslieferungStart + java.util.concurrent.TimeUnit.DAYS.toMillis(tageSeitAuslieferung)
                if (datumStart == erwartetesDatum) {
                    return true
                }
            }
        } else {
            // Datum liegt vor dem Startdatum - prüfe ob es ein zukünftiger Termin ist (innerhalb von 365 Tagen)
            val tageBisAuslieferung = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(auslieferungStart - datumStart)
            if (tageBisAuslieferung <= 365 && datumStart == auslieferungStart) {
                return true
            }
        }
        
        return false
    }
    
    private fun showTerminDetailDialog(customer: Customer, terminDatum: Long) {
        val dialogView = LayoutInflater.from(this).inflate(com.example.we2026_5.R.layout.dialog_termin_detail, null)
        val binding = com.example.we2026_5.databinding.DialogTerminDetailBinding.bind(dialogView)
        
        // Kundeninfos anzeigen
        binding.tvKundenname.text = customer.name
        binding.tvAdresse.text = customer.adresse
        binding.tvTelefon.text = customer.telefon
        binding.tvNotizen.text = customer.notizen.ifEmpty { "Keine Notizen" }
        
        // Termin-Datum formatieren
        val cal = Calendar.getInstance()
        cal.timeInMillis = terminDatum
        val dateStr = "${cal.get(Calendar.DAY_OF_MONTH)}.${cal.get(Calendar.MONTH) + 1}.${cal.get(Calendar.YEAR)}"
        binding.tvTerminDatum.text = dateStr
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        // Kunde anzeigen Button
        binding.btnKundeAnzeigen.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, CustomerDetailActivity::class.java).apply {
                putExtra("CUSTOMER_ID", customer.id)
            }
            startActivity(intent)
        }
        
        // Termin löschen Button
        binding.btnTerminLoeschen.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Termin löschen?")
                .setMessage("Möchten Sie diesen Termin wirklich löschen? Dies hat keine Auswirkung auf andere Termine.")
                .setPositiveButton("Löschen") { _, _ ->
                    loescheEinzelnenTermin(customer, terminDatum)
                    dialog.dismiss()
                }
                .setNegativeButton("Abbrechen", null)
                .show()
        }
        
        dialog.show()
    }
    
    private fun loescheEinzelnenTermin(customer: Customer, terminDatum: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            // Normalisiere das Datum auf Tagesanfang für Vergleich
            val terminDatumStart = getStartOfDay(terminDatum)
            
            // Aktuelle gelöschte Termine holen und neues Datum hinzufügen
            val aktuelleGeloeschteTermine = customer.geloeschteTermine.toMutableList()
            if (!aktuelleGeloeschteTermine.contains(terminDatumStart)) {
                aktuelleGeloeschteTermine.add(terminDatumStart)
            }
            
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = {
                    repository.updateCustomer(customer.id, mapOf(
                        "geloeschteTermine" to aktuelleGeloeschteTermine
                    ))
                },
                context = this@TourPlannerActivity,
                errorMessage = "Fehler beim Löschen des Termins. Bitte erneut versuchen.",
                maxRetries = 3
            )
            
            if (success == true) {
                android.widget.Toast.makeText(this@TourPlannerActivity, "Termin gelöscht", android.widget.Toast.LENGTH_SHORT).show()
                reloadCurrentView() // Daten neu laden
            }
        }
    }
    
    private fun resetTourCycle(customerId: String) {
        val resetData = mapOf(
            "letzterTermin" to System.currentTimeMillis(),
            "abholungErfolgt" to false,
            "auslieferungErfolgt" to false,
            "verschobenAufDatum" to 0
        )
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = { 
                    repository.updateCustomer(customerId, resetData)
                },
                context = this@TourPlannerActivity,
                errorMessage = "Fehler beim Zurücksetzen. Bitte erneut versuchen.",
                maxRetries = 3
            )
            if (success == true) {
                android.widget.Toast.makeText(this@TourPlannerActivity, "Tour abgeschlossen!", android.widget.Toast.LENGTH_SHORT).show()
                reloadCurrentView() // Daten neu laden
            }
        }
    }

    private fun getStartOfDay(ts: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
