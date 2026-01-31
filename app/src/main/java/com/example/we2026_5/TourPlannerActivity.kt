package com.example.we2026_5

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.we2026_5.adapter.CustomerButtonVisibilityHelper
import com.example.we2026_5.adapter.CustomerDialogHelper
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.ui.tourplanner.ErledigungSheetArgs
import com.example.we2026_5.ui.tourplanner.TourPlannerScreen
import com.example.we2026_5.ui.tourplanner.TourPlannerViewModel
import com.example.we2026_5.tourplanner.ErledigungSheetState
import com.example.we2026_5.tourplanner.TourPlannerCallbackHandler
import com.example.we2026_5.tourplanner.TourPlannerDateUtils
import com.example.we2026_5.tourplanner.TourPlannerDialogHelper
import com.example.we2026_5.util.TerminBerechnungUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*

class TourPlannerActivity : AppCompatActivity() {

    private val viewModel: TourPlannerViewModel by viewModel()
    private val repository: CustomerRepository by inject()
    private val listeRepository: com.example.we2026_5.data.repository.KundenListeRepository by inject()
    private val regelRepository: TerminRegelRepository by inject()
    private lateinit var dateUtils: TourPlannerDateUtils
    private lateinit var dialogHelper: TourPlannerDialogHelper
    private lateinit var callbackHandler: TourPlannerCallbackHandler
    private var sheetDialogHelper: CustomerDialogHelper? = null
    private lateinit var networkMonitor: NetworkMonitor
    private val viewDate = Calendar.getInstance()

    private var pressedHeaderButton by mutableStateOf<String?>(null)
    private var erledigungSheet by mutableStateOf<ErledigungSheetArgs?>(null)
    private var overviewCustomer by mutableStateOf<Customer?>(null)
    /** ID des Kunden für „Details öffnen“ – beim Tippen gesetzt, damit die richtige ID übergeben wird. */
    private var overviewCustomerId by mutableStateOf<String?>(null)
    private var overviewRegelNamen by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dateUtils = TourPlannerDateUtils { viewModel.getListen() }
        viewModel.selectedTimestamp.observe(this) { ts -> ts?.let { viewDate.timeInMillis = it } }

        dialogHelper = TourPlannerDialogHelper(
            activity = this,
            onKundeAnzeigen = { customer ->
                startActivity(Intent(this, CustomerDetailActivity::class.java).apply {
                    putExtra("CUSTOMER_ID", customer.id)
                })
            },
            onTerminLoeschen = { customer, terminDatum -> loescheEinzelnenTermin(customer, terminDatum) }
        )
        callbackHandler = TourPlannerCallbackHandler(
            context = this,
            repository = repository,
            listeRepository = listeRepository,
            getListen = { viewModel.getListen() },
            dateUtils = dateUtils,
            viewDate = viewDate,
            adapter = null,
            reloadCurrentView = { reloadCurrentView() },
            resetTourCycle = { customerId -> resetTourCycle(customerId) },
            onError = { msg -> viewModel.setError(msg) }
        )
        sheetDialogHelper = CustomerDialogHelper(
            context = this,
            onVerschieben = { c, newDate, alle -> callbackHandler.handleVerschiebenPublic(c, newDate, alle) },
            onUrlaub = { c, von, bis -> callbackHandler.handleUrlaubPublic(c, von, bis) },
            onRueckgaengig = { c -> callbackHandler.handleRueckgaengigPublic(c) },
            onButtonStateReset = { reloadCurrentView() }
        )

        viewModel.setSelectedTimestamp(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis)

        networkMonitor = NetworkMonitor(this)
        networkMonitor.startMonitoring()

        setContent {
            val tourItems by viewModel.tourItems.observeAsState(initial = emptyList())
            val selectedTimestamp by viewModel.selectedTimestamp.observeAsState(initial = null)
            val isLoading by viewModel.isLoading.observeAsState(initial = false)
            val errorMessage by viewModel.error.observeAsState(initial = null)
            val isOnline by networkMonitor.isOnline.observeAsState(initial = true)
            val isOffline = !isOnline

            val dateText = selectedTimestamp?.let { ts ->
                val cal = Calendar.getInstance().apply { timeInMillis = ts }
                SimpleDateFormat("EEE, dd.MM.yyyy", Locale.GERMANY).format(cal.time)
            } ?: getString(R.string.tour_label_date)

            fun getStatusBadgeText(customer: Customer): String {
                val ts = viewModel.getSelectedTimestamp() ?: return ""
                val viewDateStart = dateUtils.getStartOfDay(ts)
                val heuteStart = dateUtils.getStartOfDay(System.currentTimeMillis())
                val getAbholungDatum: (Customer) -> Long = { c -> dateUtils.calculateAbholungDatum(c, viewDateStart, heuteStart) }
                val getAuslieferungDatum: (Customer) -> Long = { c -> dateUtils.calculateAuslieferungDatum(c, viewDateStart, heuteStart) }
                val getTermineFuerKunde = { c: Customer, start: Long, days: Int ->
                    TerminBerechnungUtils.berechneAlleTermineFuerKunde(c, viewModel.getListen().find { l -> l.id == c.listeId }, start, days)
                }
                val helper = CustomerButtonVisibilityHelper(this@TourPlannerActivity, ts, emptyMap(), getAbholungDatum, getAuslieferungDatum, getTermineFuerKunde)
                return helper.getSheetState(customer)?.statusBadgeText ?: ""
            }

            val ts = selectedTimestamp
            val isToday = ts != null &&
                dateUtils.getStartOfDay(ts) == dateUtils.getStartOfDay(System.currentTimeMillis())
            TourPlannerScreen(
                tourItems = tourItems,
                dateText = dateText,
                isToday = isToday,
                isLoading = isLoading,
                errorMessage = errorMessage,
                isOffline = isOffline,
                pressedHeaderButton = pressedHeaderButton,
                erledigungSheet = erledigungSheet,
                onBack = { finish() },
                onPrevDay = { viewModel.prevDay() },
                onNextDay = { viewModel.nextDay() },
                onToday = {
                    pressedHeaderButton = "Heute"
                    viewModel.goToToday()
                },
                onMap = {
                    pressedHeaderButton = "Karte"
                    startActivity(Intent(this@TourPlannerActivity, MapViewActivity::class.java))
                    pressedHeaderButton = null
                },
                onRefresh = { reloadCurrentView() },
                onRetry = { reloadCurrentView() },
                isSectionExpanded = { viewModel.isSectionExpanded(it) },
                getStatusBadgeText = { getStatusBadgeText(it) },
                onToggleSection = { sectionType ->
                    viewModel.toggleSection(sectionType)
                    reloadCurrentView()
                },
                onCustomerClick = { customer ->
                    overviewCustomer = customer
                    overviewCustomerId = customer.id
                    overviewRegelNamen = null
                    lifecycleScope.launch {
                        val namen = withContext(Dispatchers.IO) {
                            customer.intervalle
                                .filter { it.terminRegelId.isNotBlank() }
                                .mapNotNull { regelRepository.getRegelById(it.terminRegelId)?.name }
                                .distinct()
                                .joinToString("\n")
                        }
                        overviewRegelNamen = if (namen.isNotEmpty()) namen else null
                    }
                },
                onAktionenClick = { customer ->
                    val ts = viewModel.getSelectedTimestamp() ?: return@TourPlannerScreen
                    val viewDateStart = dateUtils.getStartOfDay(ts)
                    val heuteStart = dateUtils.getStartOfDay(System.currentTimeMillis())
                    val getAbholungDatum: (Customer) -> Long = { c -> dateUtils.calculateAbholungDatum(c, viewDateStart, heuteStart) }
                    val getAuslieferungDatum: (Customer) -> Long = { c -> dateUtils.calculateAuslieferungDatum(c, viewDateStart, heuteStart) }
                    val getTermineFuerKunde = { c: Customer, start: Long, days: Int ->
                        TerminBerechnungUtils.berechneAlleTermineFuerKunde(c, viewModel.getListen().find { l -> l.id == c.listeId }, start, days)
                    }
                    val helper = CustomerButtonVisibilityHelper(this@TourPlannerActivity, ts, emptyMap(), getAbholungDatum, getAuslieferungDatum, getTermineFuerKunde)
                    val state: ErledigungSheetState? = helper.getSheetState(customer)
                    if (state != null) {
                        erledigungSheet = ErledigungSheetArgs(customer, ts, state)
                    }
                },
                onDismissErledigungSheet = { erledigungSheet = null },
                onAbholung = { callbackHandler.handleAbholungPublic(it) },
                onAuslieferung = { callbackHandler.handleAuslieferungPublic(it) },
                onKw = { callbackHandler.handleKwPublic(it) },
                onRueckgaengig = { callbackHandler.handleRueckgaengigPublic(it) },
                onVerschieben = { sheetDialogHelper?.showVerschiebenDialog(it) ?: Unit },
                onUrlaub = { sheetDialogHelper?.showUrlaubDialog(it) ?: Unit },
                getNaechstesTourDatum = { dateUtils.getNaechstesTourDatum(it) },
                showToast = { msg -> Toast.makeText(this@TourPlannerActivity, msg, Toast.LENGTH_LONG).show() },
                onTelefonClick = { tel -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))) },
                overviewCustomer = overviewCustomer,
                overviewRegelNamen = overviewRegelNamen,
                onDismissOverview = { overviewCustomer = null; overviewCustomerId = null; overviewRegelNamen = null },
                onOpenDetails = { customerId ->
                    overviewCustomer = null
                    overviewCustomerId = null
                    overviewRegelNamen = null
                    if (customerId.isNotBlank()) {
                        startActivity(Intent(this@TourPlannerActivity, CustomerDetailActivity::class.java).apply { putExtra("CUSTOMER_ID", customerId) })
                    }
                },
                overviewCustomerIdForDetails = overviewCustomerId
            )
        }

        viewModel.getSelectedTimestamp()?.let { viewModel.loadTourData(it) { viewModel.isSectionExpanded(it) } }
    }

    override fun onDestroy() {
        networkMonitor.stopMonitoring()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        viewModel.getSelectedTimestamp()?.let { viewModel.loadTourData(it) { viewModel.isSectionExpanded(it) } }
        if (pressedHeaderButton == "Karte") pressedHeaderButton = null
    }

    private fun reloadCurrentView() {
        viewModel.getSelectedTimestamp()?.let { viewModel.loadTourData(it) { viewModel.isSectionExpanded(it) } }
    }

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
}
