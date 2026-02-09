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
import com.example.we2026_5.auth.AdminChecker
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.ui.tourplanner.CustomerOverviewPayload
import com.example.we2026_5.ui.tourplanner.ErledigungSheetArgs
import com.example.we2026_5.ui.tourplanner.ErledigtSheetContent
import com.example.we2026_5.ui.tourplanner.TourPlannerScreen
import com.example.we2026_5.ui.tourplanner.TourPlannerViewModel
import com.example.we2026_5.tourplanner.ErledigungSheetState
import com.example.we2026_5.tourplanner.TourPlannerCoordinator
import com.example.we2026_5.util.AgentDebugLog
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
    private val adminChecker: AdminChecker by inject()
    private lateinit var coordinator: TourPlannerCoordinator
    private lateinit var networkMonitor: NetworkMonitor

    private var pressedHeaderButton by mutableStateOf<String?>(null)
    private var erledigungSheet by mutableStateOf<ErledigungSheetArgs?>(null)
    private var overviewPayload by mutableStateOf<CustomerOverviewPayload?>(null)
    private var overviewRegelNamen by mutableStateOf<String?>(null)
    private var erledigtSheetVisible by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // #region agent log
        val onCreateStart = System.currentTimeMillis()
        AgentDebugLog.resetGetStatusBadgeTextCount()
        AgentDebugLog.log("TourPlannerActivity.kt", "onCreate_start", mapOf(), "H4")
        // #endregion
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("tourplanner_prefs", MODE_PRIVATE)
        // Beim Start immer heutiges Datum anzeigen
        val initialDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

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
            val isAdmin = adminChecker.isAdmin()
            // #region agent log
            AgentDebugLog.log("TourPlannerActivity.kt", "setContent_isAdmin", mapOf("isAdmin" to isAdmin), "H2")
            // #endregion

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
                // #region agent log
                val c = AgentDebugLog.incGetStatusBadgeTextCount()
                if (c <= 3 || c % 10 == 0) AgentDebugLog.log("TourPlannerActivity.kt", "getStatusBadgeText", mapOf("customerId" to customer.id, "callCount" to c), "H3")
                // #endregion
                val ts = viewModel.getSelectedTimestamp() ?: return ""
                val viewDateStart = coordinator.dateUtils.getStartOfDay(ts)
                val heuteStart = coordinator.dateUtils.getStartOfDay(System.currentTimeMillis())
                return com.example.we2026_5.tourplanner.TourPlannerStatusBadge.compute(customer, viewDateStart, heuteStart)
            }

            val ts = selectedTimestamp
            // #region agent log
            val countsT0 = System.currentTimeMillis()
            AgentDebugLog.log("TourPlannerActivity.kt", "counts_start", mapOf("tourItemsSize" to tourItems.size, "customerCount" to tourItems.filterIsInstance<ListItem.CustomerItem>().size), "H2")
            // #endregion
            val counts = ts?.let { timestamp ->
                val viewDateStart = coordinator.dateUtils.getStartOfDay(timestamp)
                val customers = tourItems.filterIsInstance<ListItem.CustomerItem>().map { it.customer }
                // #region agent log
                AgentDebugLog.log("TourPlannerActivity.kt", "counts_flatMap", mapOf("customersToProcess" to customers.size), "H2")
                // #endregion
                val termine = customers.flatMap { c ->
                    TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                        customer = c,
                        liste = null,
                        startDatum = viewDateStart,
                        tageVoraus = 1
                    ).filter { coordinator.dateUtils.getStartOfDay(it.datum) == viewDateStart }
                }
                val aCount = termine.count { it.typ == TerminTyp.ABHOLUNG }
                val lCount = termine.count { it.typ == TerminTyp.AUSLIEFERUNG }
                aCount to lCount
            } ?: (0 to 0)
            // #region agent log
            AgentDebugLog.log("TourPlannerActivity.kt", "counts_end", mapOf("duration_ms" to (System.currentTimeMillis() - countsT0)), "H2")
            // #endregion
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
                    val addresses = tourItems.filterIsInstance<ListItem.CustomerItem>()
                        .map { it.customer.adresse }
                        .filter { it.isNotBlank() }
                    val intent = Intent(this@TourPlannerActivity, MapViewActivity::class.java)
                    if (addresses.isNotEmpty()) {
                        intent.putStringArrayListExtra(MapViewActivity.EXTRA_ADDRESSES, ArrayList(addresses))
                    }
                    startActivity(intent)
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
                        TerminBerechnungUtils.berechneAlleTermineFuerKunde(c, null, start, days)
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
                },
                onReorder = { fromListIndex, toListIndex ->
                    // #region agent log
                    AgentDebugLog.log("TourPlannerActivity.kt", "onReorder_entry", mapOf("from" to fromListIndex, "to" to toListIndex, "isAdmin" to isAdmin), "H1")
                    // #endregion
                    if (!isAdmin) {
                        // #region agent log
                        AgentDebugLog.log("TourPlannerActivity.kt", "onReorder_early_return_isAdmin", mapOf(), "H2")
                        // #endregion
                        return@TourPlannerScreen
                    }
                    val timestamp = viewModel.getSelectedTimestamp() ?: return@TourPlannerScreen
                    if (tourItems.getOrNull(fromListIndex) is ListItem.CustomerItem &&
                        tourItems.getOrNull(toListIndex) is ListItem.CustomerItem
                    ) {
                        val newList = tourItems.toMutableList().apply {
                            add(toListIndex, removeAt(fromListIndex))
                        }
                        val newIds = newList.filterIsInstance<ListItem.CustomerItem>().map { it.customer.id }
                        // #region agent log
                        AgentDebugLog.log("TourPlannerActivity.kt", "onReorder_setTourOrder", mapOf("idsCount" to newIds.size), "H3")
                        // #endregion
                        viewModel.setTourOrder(timestamp, newIds)
                    } else {
                        // #region agent log
                        AgentDebugLog.log("TourPlannerActivity.kt", "onReorder_no_customer_items", mapOf("fromItem" to (tourItems.getOrNull(fromListIndex)?.let { it::class.simpleName } ?: "null"), "toItem" to (tourItems.getOrNull(toListIndex)?.let { it::class.simpleName } ?: "null")), "H3")
                        // #endregion
                    }
                }
            )
        }

        // #region agent log
        AgentDebugLog.log("TourPlannerActivity.kt", "onCreate_end", mapOf("duration_ms" to (System.currentTimeMillis() - onCreateStart)), "H4")
        // #endregion
        lifecycleScope.launch { coordinator.reloadCurrentView() }
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
