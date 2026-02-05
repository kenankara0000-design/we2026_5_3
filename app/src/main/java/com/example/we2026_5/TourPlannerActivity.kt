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
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.ui.tourplanner.CustomerOverviewPayload
import com.example.we2026_5.ui.tourplanner.ErledigungSheetArgs
import com.example.we2026_5.ui.tourplanner.ErledigtSheetContent
import com.example.we2026_5.ui.tourplanner.TourPlannerScreen
import com.example.we2026_5.ui.tourplanner.TourPlannerViewModel
import com.example.we2026_5.tourplanner.ErledigungSheetState
import com.example.we2026_5.tourplanner.TourPlannerCoordinator
import com.example.we2026_5.util.TerminBerechnungUtils
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.text.SimpleDateFormat
import java.util.*

class TourPlannerActivity : AppCompatActivity() {

    private val viewModel: TourPlannerViewModel by viewModel()
    private val repository: CustomerRepository by inject()
    private val listeRepository: com.example.we2026_5.data.repository.KundenListeRepository by inject()
    private lateinit var coordinator: TourPlannerCoordinator
    private lateinit var networkMonitor: NetworkMonitor

    private var pressedHeaderButton by mutableStateOf<String?>(null)
    private var erledigungSheet by mutableStateOf<ErledigungSheetArgs?>(null)
    private var overviewPayload by mutableStateOf<CustomerOverviewPayload?>(null)
    private var overviewRegelNamen by mutableStateOf<String?>(null)
    private var erledigtSheetVisible by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("tourplanner_prefs", MODE_PRIVATE)
        val savedDate = prefs.getLong("last_view_date", 0L)
        val initialDate = if (savedDate > 0L) {
            TerminBerechnungUtils.getStartOfDay(savedDate)
        } else {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }

        coordinator = TourPlannerCoordinator(
            activity = this,
            viewModel = viewModel,
            repository = repository,
            listeRepository = listeRepository
        )

        viewModel.setSelectedTimestamp(initialDate)

        networkMonitor = NetworkMonitor(this, lifecycleScope)
        networkMonitor.startMonitoring()

        setContent {
            val tourItems by viewModel.tourItems.observeAsState(initial = emptyList())
            val erledigtCount by viewModel.erledigtCount.observeAsState(initial = 0)
            val erledigtSheetContent by viewModel.erledigtSheetContent.observeAsState(initial = null)
            val selectedTimestamp by viewModel.selectedTimestamp.observeAsState(initial = null)
            val isLoading by viewModel.isLoading.observeAsState(initial = false)
            val errorMessage by viewModel.error.observeAsState(initial = null)
            val isOnline by networkMonitor.isOnline.observeAsState(initial = true)
            val isOffline = !isOnline

            androidx.compose.runtime.LaunchedEffect(selectedTimestamp) {
                selectedTimestamp?.let { ts ->
                    prefs.edit().putLong("last_view_date", TerminBerechnungUtils.getStartOfDay(ts)).apply()
                }
            }

            val dateText = selectedTimestamp?.let { ts ->
                val cal = Calendar.getInstance().apply { timeInMillis = ts }
                SimpleDateFormat("EEE, dd.MM.yyyy", Locale.GERMANY).format(cal.time)
            } ?: getString(R.string.tour_label_date)

            fun getStatusBadgeText(customer: Customer): String {
                val ts = viewModel.getSelectedTimestamp() ?: return ""
                val viewDateStart = coordinator.dateUtils.getStartOfDay(ts)
                val heuteStart = coordinator.dateUtils.getStartOfDay(System.currentTimeMillis())
                val getAbholungDatum: (Customer) -> Long = { c -> coordinator.dateUtils.calculateAbholungDatum(c, viewDateStart, heuteStart) }
                val getAuslieferungDatum: (Customer) -> Long = { c -> coordinator.dateUtils.calculateAuslieferungDatum(c, viewDateStart, heuteStart) }
                val getTermineFuerKunde = { c: Customer, start: Long, days: Int ->
                    TerminBerechnungUtils.berechneAlleTermineFuerKunde(c, viewModel.getListen().find { l -> l.id == c.listeId }, start, days)
                }
                val helper = CustomerButtonVisibilityHelper(this@TourPlannerActivity, ts, emptyMap(), getAbholungDatum, getAuslieferungDatum, getTermineFuerKunde)
                return helper.getSheetState(customer)?.statusBadgeText ?: ""
            }

            val ts = selectedTimestamp
            val counts = ts?.let { timestamp ->
                val viewDateStart = coordinator.dateUtils.getStartOfDay(timestamp)
                val listen = viewModel.getListen()
                val customers = tourItems.filterIsInstance<ListItem.CustomerItem>().map { it.customer }
                val termine = customers.flatMap { c ->
                    TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                        customer = c,
                        liste = listen.find { it.id == c.listeId },
                        startDatum = viewDateStart,
                        tageVoraus = 1
                    ).filter { coordinator.dateUtils.getStartOfDay(it.datum) == viewDateStart }
                }
                val aCount = termine.count { it.typ == TerminTyp.ABHOLUNG }
                val lCount = termine.count { it.typ == TerminTyp.AUSLIEFERUNG }
                aCount to lCount
            } ?: (0 to 0)
            val isToday = ts != null &&
                coordinator.dateUtils.getStartOfDay(ts) == coordinator.dateUtils.getStartOfDay(System.currentTimeMillis())
            TourPlannerScreen(
                tourItems = tourItems,
                viewDateMillis = ts,
                dateText = dateText,
                tourCounts = counts,
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
                onRefresh = { coordinator.reloadCurrentView() },
                onRetry = { coordinator.reloadCurrentView() },
                isSectionExpanded = { viewModel.isSectionExpanded(it) },
                getStatusBadgeText = { getStatusBadgeText(it) },
                onToggleSection = { viewModel.toggleSection(it) },
                onCustomerClick = { payload ->
                    overviewPayload = payload
                    overviewRegelNamen = null
                },
                onAktionenClick = { customer ->
                    val ts = viewModel.getSelectedTimestamp() ?: return@TourPlannerScreen
                    val viewDateStart = coordinator.dateUtils.getStartOfDay(ts)
                    val heuteStart = coordinator.dateUtils.getStartOfDay(System.currentTimeMillis())
                    val getAbholungDatum: (Customer) -> Long = { c -> coordinator.dateUtils.calculateAbholungDatum(c, viewDateStart, heuteStart) }
                    val getAuslieferungDatum: (Customer) -> Long = { c -> coordinator.dateUtils.calculateAuslieferungDatum(c, viewDateStart, heuteStart) }
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
                onAbholung = { coordinator.callbackHandler.handleAbholungPublic(it) },
                onAuslieferung = { coordinator.callbackHandler.handleAuslieferungPublic(it) },
                onKw = { coordinator.callbackHandler.handleKwPublic(it) },
                onRueckgaengig = { coordinator.callbackHandler.handleRueckgaengigPublic(it) },
                onVerschieben = { coordinator.showVerschiebenDialog(it) },
                getNaechstesTourDatum = { coordinator.dateUtils.getNaechstesTourDatum(it) },
                showToast = { msg -> Toast.makeText(this@TourPlannerActivity, msg, Toast.LENGTH_LONG).show() },
                onTelefonClick = { tel -> startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))) },
                overviewPayload = overviewPayload,
                overviewRegelNamen = overviewRegelNamen,
                onDismissOverview = { overviewPayload = null; overviewRegelNamen = null },
                erledigtCount = erledigtCount,
                erledigtSheetVisible = erledigtSheetVisible,
                erledigtSheetContent = erledigtSheetContent,
                onErledigtClick = { erledigtSheetVisible = true },
                onDismissErledigtSheet = { erledigtSheetVisible = false },
                onOpenDetails = { customerId ->
                    overviewPayload = null
                    overviewRegelNamen = null
                    erledigtSheetVisible = false
                    if (customerId.isNotBlank()) {
                        startActivity(Intent(this@TourPlannerActivity, CustomerDetailActivity::class.java).apply { putExtra("CUSTOMER_ID", customerId) })
                    }
                }
            )
        }

        coordinator.reloadCurrentView()
    }

    override fun onDestroy() {
        networkMonitor.stopMonitoring()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        coordinator.reloadCurrentView()
        if (pressedHeaderButton == "Karte") pressedHeaderButton = null
    }
}
