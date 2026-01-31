package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.databinding.ActivityTourPlannerBinding
import com.example.we2026_5.databinding.DialogCustomerOverviewBinding
import com.example.we2026_5.ui.tourplanner.TourPlannerViewModel
import com.example.we2026_5.tourplanner.TourPlannerDialogHelper
import com.example.we2026_5.tourplanner.TourPlannerDateUtils
import com.example.we2026_5.tourplanner.TourPlannerCallbackHandler
import com.example.we2026_5.tourplanner.TourPlannerUISetup
import com.example.we2026_5.tourplanner.ErledigungBottomSheetDialogFragment
import com.example.we2026_5.tourplanner.ErledigungSheetState
import com.example.we2026_5.adapter.CustomerDialogHelper
import com.example.we2026_5.tourplanner.TourPlannerGestureHandler
import com.example.we2026_5.FirebaseRetryHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class TourPlannerActivity : AppCompatActivity(), ErledigungBottomSheetDialogFragment.ErledigungSheetCallbacks {

    private lateinit var binding: ActivityTourPlannerBinding
    private val viewModel: TourPlannerViewModel by viewModel()
    private val repository: CustomerRepository by inject()
    private val listeRepository: com.example.we2026_5.data.repository.KundenListeRepository by inject()
    private val regelRepository: TerminRegelRepository by inject()
    private lateinit var adapter: CustomerAdapter
    private var viewDate = Calendar.getInstance()
    private lateinit var networkMonitor: NetworkMonitor
    private var pressedHeaderButton: String? = null // "Karte", "Heute", "Woche"
    
    // Helper-Klassen
    private lateinit var dialogHelper: TourPlannerDialogHelper
    private lateinit var dateUtils: TourPlannerDateUtils
    private lateinit var callbackHandler: TourPlannerCallbackHandler
    private var sheetDialogHelper: CustomerDialogHelper? = null
    private lateinit var uiSetup: TourPlannerUISetup
    private lateinit var gestureHandler: TourPlannerGestureHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTourPlannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Helper initialisieren
        dateUtils = TourPlannerDateUtils { viewModel.getListen() }
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
            onClick = { customer -> showCustomerOverviewDialog(customer) }
        )
        
        // Callbacks für Firebase-Operationen setzen (initialisiert auch callbackHandler)
        setupAdapterCallbacks()
        
        // UI-Setup initialisieren
        uiSetup = TourPlannerUISetup(
            activity = this,
            binding = binding,
            setupAdapterCallbacks = { setupAdapterCallbacks() },
            setupAdapterCallbacksForAdapter = { adapter -> setupAdapterCallbacksForAdapter(adapter) }
        )
        uiSetup.setupAdapters(adapter)
        
        // Callback für Section-Toggle setzen
        adapter.callbacks = adapter.callbacks.copy(
            onSectionToggle = { sectionType ->
                viewModel.toggleSection(sectionType)
                reloadCurrentView() // Daten neu laden wenn Section getoggelt wird
            }
        )
        
        // Datum im ViewModel setzen (Single Source of Truth)
        viewModel.setSelectedTimestamp(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis)
        // Initial: Tagesansicht
        updateViewMode()
        uiSetup.updateHeaderButtonStates(pressedHeaderButton)
        observeViewModel()

        binding.btnBackFromTour.setOnClickListener { finish() }

        binding.btnPrevDay.setOnClickListener {
            viewModel.prevDay()
        }

        binding.btnNextDay.setOnClickListener {
            viewModel.nextDay()
        }

        binding.btnToday.setOnClickListener {
            pressedHeaderButton = "Heute"
            uiSetup.updateHeaderButtonStates(pressedHeaderButton)
            viewModel.goToToday()
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
        // Datum aus ViewModel sync (Anzeige + viewDate für Helper)
        viewModel.selectedTimestamp.observe(this) { ts ->
            ts?.let {
                viewDate.timeInMillis = it
                val fmt = SimpleDateFormat("EEE, dd.MM.yyyy", Locale.GERMANY)
                binding.tvCurrentDate.text = fmt.format(viewDate.time)
                updateTodayButtonState()
                setupAdapterCallbacksForAdapter(adapter)
            }
        }
        // Tour-Items beobachten (Tagesansicht)
        viewModel.tourItems.observe(this) { items ->
            val ts = viewModel.getSelectedTimestamp() ?: viewDate.timeInMillis
            adapter.updateData(items, dateUtils.getStartOfDay(ts))
            uiSetup.updateEmptyState(items.isEmpty())
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
        // Datum aus ViewModel lesen und Anzeige + loadTourData aktualisieren
        val ts = viewModel.getSelectedTimestamp() ?: Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        viewDate.timeInMillis = ts
        val fmt = SimpleDateFormat("EEE, dd.MM.yyyy", Locale.GERMANY)
        binding.tvCurrentDate.text = fmt.format(viewDate.time)
        loadTourData(ts)
        setupAdapterCallbacksForAdapter(adapter)
    }
    
    private fun updateViewMode() {
        uiSetup.updateViewMode()
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
        
    private fun reloadCurrentView() {
        viewModel.getSelectedTimestamp()?.let { loadTourData(it) }
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
            listeRepository = listeRepository,
            getListen = { viewModel.getListen() },
            dateUtils = dateUtils,
            viewDate = viewDate,
            adapter = adapter,
            reloadCurrentView = { reloadCurrentView() },
            resetTourCycle = { customerId -> resetTourCycle(customerId) },
            onError = { msg -> viewModel.setError(msg) }
        )
        callbackHandler.setupCallbacks()

        sheetDialogHelper = CustomerDialogHelper(
            context = this,
            onVerschieben = { c, newDate, alle -> adapter.callbacks.onVerschieben?.invoke(c, newDate, alle) },
            onUrlaub = { c, von, bis -> adapter.callbacks.onUrlaub?.invoke(c, von, bis) },
            onRueckgaengig = { c -> adapter.callbacks.onRueckgaengig?.invoke(c) },
            onButtonStateReset = { reloadCurrentView() }
        )
        
        adapter.callbacks = adapter.callbacks.copy(
            onTerminClick = { customer, terminDatum ->
                dialogHelper.showTerminDetailDialog(customer, terminDatum)
            },
            onAktionenClick = { customer, state ->
                showErledigungSheet(customer, state)
            }
        )
    }

    private fun showErledigungSheet(customer: Customer, state: ErledigungSheetState) {
        val sheet = ErledigungBottomSheetDialogFragment.newInstance(customer, viewDate.timeInMillis, state)
        sheet.show(supportFragmentManager, "ErledigungSheet")
    }

    override fun onAbholung(customer: Customer) { callbackHandler.handleAbholungPublic(customer) }
    override fun onAuslieferung(customer: Customer) { callbackHandler.handleAuslieferungPublic(customer) }
    override fun onKw(customer: Customer) { callbackHandler.handleKwPublic(customer) }
    override fun onRueckgaengig(customer: Customer) { callbackHandler.handleRueckgaengigPublic(customer) }
    override fun onVerschieben(customer: Customer) { sheetDialogHelper?.showVerschiebenDialog(customer) }
    override fun onUrlaub(customer: Customer) { sheetDialogHelper?.showUrlaubDialog(customer) }
    override fun getNaechstesTourDatum(customer: Customer): Long? = adapter.callbacks.getNaechstesTourDatum?.invoke(customer)
    
    /**
     * Zeigt das Kunden-Übersichtsfenster (Termin-Regeln elegant, nur Namen, ohne Kasten).
     * Bei Klick auf "Details öffnen" → CustomerDetailActivity.
     */
    private fun showCustomerOverviewDialog(customer: Customer) {
        val dialogBinding = DialogCustomerOverviewBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tvOverviewCustomerName.text = customer.name
        
        // Regelnamen asynchron laden (nur Namen, elegant)
        CoroutineScope(Dispatchers.Main).launch {
            val regelNamen = withContext(Dispatchers.IO) {
                val namen = mutableListOf<String>()
                for (intervall in customer.intervalle) {
                    if (intervall.terminRegelId.isNotEmpty()) {
                        regelRepository.getRegelById(intervall.terminRegelId)?.name?.let { namen.add(it) }
                    }
                }
                namen.distinct()
            }
            dialogBinding.tvOverviewRegelNames.text = if (regelNamen.isNotEmpty()) {
                regelNamen.joinToString("\n")
            } else {
                getString(R.string.no_termin_regeln)
            }
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()
        
        dialogBinding.btnOverviewDetails.setOnClickListener {
            dialog.dismiss()
            val intent = Intent(this, CustomerDetailActivity::class.java).apply {
                putExtra("CUSTOMER_ID", customer.id)
            }
            startActivity(intent)
        }
        
        dialog.show()
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
        lifecycleScope.launch {
            when (val result = viewModel.deleteTerminFromCustomer(customer, terminDatum)) {
                is com.example.we2026_5.util.Result.Success -> {
                    viewModel.clearError()
                    android.widget.Toast.makeText(this@TourPlannerActivity, getString(R.string.toast_termin_deleted), android.widget.Toast.LENGTH_SHORT).show()
                    reloadCurrentView()
                }
                is com.example.we2026_5.util.Result.Error -> {
                    viewModel.setError(result.message)
                    android.widget.Toast.makeText(this@TourPlannerActivity, result.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun resetTourCycle(customerId: String) {
        lifecycleScope.launch {
            when (val result = viewModel.resetTourCycle(customerId)) {
                is com.example.we2026_5.util.Result.Success -> {
                    viewModel.clearError()
                    android.widget.Toast.makeText(this@TourPlannerActivity, getString(R.string.toast_tour_completed), android.widget.Toast.LENGTH_SHORT).show()
                    reloadCurrentView()
                }
                is com.example.we2026_5.util.Result.Error -> {
                    viewModel.setError(result.message)
                    android.widget.Toast.makeText(this@TourPlannerActivity, result.message, android.widget.Toast.LENGTH_SHORT).show()
                }
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
