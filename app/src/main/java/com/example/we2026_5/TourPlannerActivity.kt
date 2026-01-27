package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.databinding.ActivityTourPlannerBinding
import com.example.we2026_5.ui.tourplanner.TourPlannerViewModel
import com.example.we2026_5.tourplanner.TourPlannerDialogHelper
import com.example.we2026_5.tourplanner.TourPlannerDateUtils
import com.example.we2026_5.tourplanner.TourPlannerCallbackHandler
import com.example.we2026_5.tourplanner.TourPlannerUISetup
import com.example.we2026_5.tourplanner.TourPlannerGestureHandler
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
    private lateinit var networkMonitor: NetworkMonitor
    private var pressedHeaderButton: String? = null // "Karte", "Heute", "Woche"
    
    // Helper-Klassen
    private lateinit var dialogHelper: TourPlannerDialogHelper
    private lateinit var dateUtils: TourPlannerDateUtils
    private lateinit var callbackHandler: TourPlannerCallbackHandler
    private lateinit var uiSetup: TourPlannerUISetup
    private lateinit var gestureHandler: TourPlannerGestureHandler

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
        
        // UI-Setup initialisieren
        uiSetup = TourPlannerUISetup(
            activity = this,
            binding = binding,
            setupAdapterCallbacks = { setupAdapterCallbacks() },
            setupAdapterCallbacksForAdapter = { adapter -> setupAdapterCallbacksForAdapter(adapter) }
        )
        uiSetup.setupAdapters(adapter, weekAdapter)
        
        // Callback für Section-Toggle setzen
        adapter.onSectionToggle = { sectionType ->
            viewModel.toggleSection(sectionType)
            reloadCurrentView() // Daten neu laden wenn Section getoggelt wird
        }
        
        // Initial: Tagesansicht
        updateViewMode()
        uiSetup.updateHeaderButtonStates(pressedHeaderButton) // Initial Header-Button-Zustand setzen
        
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
            uiSetup.updateHeaderButtonStates(pressedHeaderButton)
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
            uiSetup.updateHeaderButtonStates(pressedHeaderButton)
            val intent = Intent(this, MapViewActivity::class.java)
            startActivity(intent)
            // Nach Rückkehr Button-Zustand zurücksetzen
            pressedHeaderButton = null
            uiSetup.updateHeaderButtonStates(pressedHeaderButton)
        }
        
        // Toggle zwischen Tag- und Wochenansicht
        binding.btnToggleView.setOnClickListener {
            pressedHeaderButton = if (isWeekView) null else "Woche"
            isWeekView = !isWeekView
            updateViewMode()
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
        gestureHandler = TourPlannerGestureHandler(
            binding = binding,
            viewDate = viewDate,
            onDateChanged = { updateDisplay() }
        )
        gestureHandler.setupSwipeGestures()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
    }
    

    override fun onStart() {
        super.onStart()
        updateDisplay()
        // Button-Zustand zurücksetzen wenn von MapViewActivity zurückgekehrt
        if (pressedHeaderButton == "Karte") {
            pressedHeaderButton = null
            uiSetup.updateHeaderButtonStates(pressedHeaderButton)
        }
    }

    private fun observeViewModel() {
        // Tour-Items beobachten (Tagesansicht)
        viewModel.tourItems.observe(this) { items ->
            if (!isWeekView) {
                adapter.updateData(items, dateUtils.getStartOfDay(viewDate.timeInMillis))
                
                // Empty State anzeigen wenn keine Kunden vorhanden
                uiSetup.updateEmptyState(items.isEmpty(), isWeekView = false)
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
                uiSetup.updateEmptyState(!hasAnyData, isWeekView = true)
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
        uiSetup.updateViewMode(isWeekView)
        uiSetup.updateHeaderButtonStates(pressedHeaderButton) // Button-Zustände aktualisieren
        updateDisplay()
    }
    
    private fun updateTodayButtonState() {
        // Diese Funktion wird jetzt von updateHeaderButtonStates() übernommen
        // Behalten für Kompatibilität, aber Logik ist in updateHeaderButtonStates()
        uiSetup.updateHeaderButtonStates(pressedHeaderButton)
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
        uiSetup.showErrorState(message) {
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
