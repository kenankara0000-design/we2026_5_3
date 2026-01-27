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
import com.example.we2026_5.tourplanner.TourPlannerDialogHelper
import com.example.we2026_5.tourplanner.TourPlannerDateUtils
import com.example.we2026_5.tourplanner.TourPlannerCallbackHandler
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
    
    // Helper-Klassen
    private lateinit var dialogHelper: TourPlannerDialogHelper
    private lateinit var dateUtils: TourPlannerDateUtils
    private lateinit var callbackHandler: TourPlannerCallbackHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTourPlannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Helper initialisieren
        dateUtils = TourPlannerDateUtils(listeRepository)
        dialogHelper = TourPlannerDialogHelper(
            activity = this,
            onKundeAnzeigen = { customer ->
                val intent = Intent(this, CustomerDetailActivity::class.java).apply {
                    putExtra("CUSTOMER_ID", customer.id)
                }
                startActivity(intent)
            },
            onTerminLoeschen = { customer, terminDatum ->
                loescheEinzelnenTermin(customer, terminDatum)
            }
        )

        // Adapter ZUERST initialisieren (wird von callbackHandler benötigt)
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
        
        // Callbacks für Firebase-Operationen setzen (initialisiert auch callbackHandler)
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
        
        // Drag & Drop für Kunden
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
                val heuteTimestamp = dateUtils.getStartOfDay(heuteStart.timeInMillis)
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
                adapter.updateData(items, dateUtils.getStartOfDay(viewDate.timeInMillis))
                
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
        // Aktualisiere CallbackHandler mit neuem Adapter
        callbackHandler = TourPlannerCallbackHandler(
            context = this,
            repository = repository,
            dateUtils = dateUtils,
            viewDate = viewDate,
            adapter = adapter,
            reloadCurrentView = { reloadCurrentView() },
            resetTourCycle = { customerId -> resetTourCycle(customerId) }
        )
        callbackHandler.setupCallbacks()
        
        // Termin-Klick: Öffne Termin-Detail-Dialog
        adapter.onTerminClick = { customer, terminDatum ->
            dialogHelper.showTerminDetailDialog(customer, terminDatum)
        }
    }
    
    // Alle Callback-Funktionen entfernt - jetzt in TourPlannerCallbackHandler
    
    private fun calculateAbholungDatum(customer: Customer, viewDateStart: Long): Long {
        val heuteStart = dateUtils.getStartOfDay(System.currentTimeMillis())
        return dateUtils.calculateAbholungDatum(customer, viewDateStart, heuteStart)
    }
    
    // Alte Implementierung entfernt - jetzt in TourPlannerDateUtils
    
    private fun calculateAuslieferungDatum(customer: Customer, viewDateStart: Long): Long {
        // NEUE STRUKTUR: Verwende Intervalle-Liste wenn vorhanden
        if (customer.intervalle.isNotEmpty()) {
            val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                startDatum = viewDateStart - java.util.concurrent.TimeUnit.DAYS.toMillis(1),
                tageVoraus = 2 // Nur 2 Tage (gestern, heute, morgen)
            )
            // Prüfe ob am angezeigten Tag ein Auslieferungstermin vorhanden ist
            return termine.firstOrNull { 
                it.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG &&
                com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(it.datum) == viewDateStart
            }?.datum ?: 0L
        }
        
        // Verwende DateUtils für Listen-Kunden und andere Fälle
        val heuteStart = dateUtils.getStartOfDay(System.currentTimeMillis())
        return dateUtils.calculateAuslieferungDatum(customer, viewDateStart, heuteStart)
    }
    
    private fun isIntervallFaelligAm(intervall: com.example.we2026_5.ListeIntervall, datum: Long): Boolean {
        return dateUtils.isIntervallFaelligAm(intervall, datum)
    }
    
    // Alte Implementierungen entfernt - jetzt in TourPlannerDateUtils und TourPlannerDialogHelper
    
    private fun loescheEinzelnenTermin(customer: Customer, terminDatum: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            // Normalisiere das Datum auf Tagesanfang für Vergleich
            val terminDatumStart = dateUtils.getStartOfDay(terminDatum)
            
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
        return dateUtils.getStartOfDay(ts)
    }
    
    /**
     * Berechnet das Fälligkeitsdatum für Abholung (wenn überfällig)
     */
    private fun getFaelligAmDatumFuerAbholung(customer: Customer, heuteStart: Long): Long {
        return dateUtils.getFaelligAmDatumFuerAbholung(customer, heuteStart)
    }
    
    /**
     * Berechnet das Fälligkeitsdatum für Auslieferung (wenn überfällig)
     */
    private fun getFaelligAmDatumFuerAuslieferung(customer: Customer, heuteStart: Long): Long {
        return dateUtils.getFaelligAmDatumFuerAuslieferung(customer, heuteStart)
    }
}
