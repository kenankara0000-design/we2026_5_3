package com.example.we2026_5

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
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
import com.example.we2026_5.tourplanner.TerminCache
import com.example.we2026_5.tourplanner.TourPlannerCoordinator
import com.example.we2026_5.ui.theme.AppTheme
import com.example.we2026_5.util.AgentDebugLog
import com.example.we2026_5.util.AppPreferences
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.tageAzuLOrDefault
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
    private val adminChecker: AdminChecker by inject()
    private val termincache: TerminCache by inject()
    private lateinit var coordinator: TourPlannerCoordinator
    private lateinit var networkMonitor: NetworkMonitor

    private val appPrefs by lazy { AppPreferences(this) }
    private var pressedHeaderButton by mutableStateOf<String?>(null)
    private var erledigungSheet by mutableStateOf<ErledigungSheetArgs?>(null)
    private var overviewPayload by mutableStateOf<CustomerOverviewPayload?>(null)
    private var overviewRegelNamen by mutableStateOf<String?>(null)
    private var erledigtSheetVisible by mutableStateOf(false)
    private var reihenfolgeBearbeitenMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        // #region agent log
        val onCreateStart = System.currentTimeMillis()
        AgentDebugLog.resetGetStatusBadgeTextCount()
        AgentDebugLog.log("TourPlannerActivity.kt", "onCreate_start", mapOf(), "H4")
        // #endregion
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("tourplanner_prefs", MODE_PRIVATE)
        // Beim Start immer heutiges Datum anzeigen (Berlin)
        val initialDate = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())

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
            AppTheme {
            val tourItems by viewModel.tourItems.observeAsState(initial = emptyList())
            val erledigtCount by viewModel.erledigtCount.observeAsState(initial = 0)
            val erledigtSheetContent by viewModel.erledigtSheetContent.observeAsState(initial = null)
            val selectedTimestamp by viewModel.selectedTimestamp.observeAsState(initial = null)
            val isLoading by viewModel.isLoading.collectAsState(initial = false)
            val errorMessage by viewModel.error.collectAsState(initial = null)
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
                val cal = com.example.we2026_5.util.AppTimeZone.newCalendar().apply { timeInMillis = ts }
                SimpleDateFormat("EEE, dd.MM.yyyy", Locale.GERMANY).apply { timeZone = com.example.we2026_5.util.AppTimeZone.timeZone }.format(cal.time)
            } ?: getString(R.string.tour_label_date)

            fun getStatusBadgeText(customer: Customer): String {
                // #region agent log
                val c = AgentDebugLog.incGetStatusBadgeTextCount()
                if (c <= 3 || c % 10 == 0) AgentDebugLog.log("TourPlannerActivity.kt", "getStatusBadgeText", mapOf("customerId" to customer.id, "callCount" to c), "H3")
                // #endregion
                val ts = viewModel.getSelectedTimestamp() ?: return ""
                val viewDateStart = coordinator.dateUtils.getStartOfDay(ts)
                val heuteStart = coordinator.dateUtils.getStartOfDay(System.currentTimeMillis())
                val liste = viewModel.getListen().find { it.id == customer.listeId }
                return com.example.we2026_5.tourplanner.TourPlannerStatusBadge.compute(customer, viewDateStart, heuteStart, termincache, liste)
            }

            val ts = selectedTimestamp
            // #region agent log
            val countsT0 = System.currentTimeMillis()
            AgentDebugLog.log("TourPlannerActivity.kt", "counts_start", mapOf("tourItemsSize" to tourItems.size, "customerCount" to tourItems.filterIsInstance<ListItem.CustomerItem>().size), "H2")
            // #endregion
            val allCustomersFromItems = tourItems.flatMap { item ->
                when (item) {
                    is ListItem.CustomerItem -> listOf(item.customer)
                    is ListItem.TourListeCard -> item.kunden.map { (c, _, _) -> c }
                    else -> emptyList()
                }
            }
            val counts = ts?.let { timestamp ->
                val viewDateStart = coordinator.dateUtils.getStartOfDay(timestamp)
                val customers = allCustomersFromItems
                // #region agent log
                AgentDebugLog.log("TourPlannerActivity.kt", "counts_flatMap", mapOf("customersToProcess" to customers.size), "H2")
                // #endregion
                val termine = customers.flatMap { c ->
                    val liste = viewModel.getListen().find { it.id == c.listeId }
                    termincache.getTermineInRange(c, viewDateStart, 1, liste)
                        .filter { coordinator.dateUtils.getStartOfDay(it.datum) == viewDateStart }
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
                    val addresses = allCustomersFromItems
                        .map { it.adresse }
                        .filter { it.isNotBlank() }
                    if (addresses.isNotEmpty()) {
                        startActivity(com.example.we2026_5.util.AppNavigation.toMapViewWithAddresses(this@TourPlannerActivity, ArrayList(addresses)))
                    } else {
                        startActivity(com.example.we2026_5.util.AppNavigation.toMapView(this@TourPlannerActivity))
                    }
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
                        termincache.getTermineInRange(c, start, days)
                    }
                    val helper = CustomerButtonVisibilityHelper(this@TourPlannerActivity, ts, emptyMap(), getAbholungDatum, getAuslieferungDatum, getTermineFuerKunde)
                    val state: ErledigungSheetState? = helper.getSheetState(customer)
                    if (state != null) {
                        erledigungSheet = ErledigungSheetArgs(customer, ts, state)
                    } else {
                        Toast.makeText(this@TourPlannerActivity, getString(R.string.error_keine_aktion_verfuegbar), Toast.LENGTH_SHORT).show()
                    }
                },
                onDismissErledigungSheet = { erledigungSheet = null },
                onAbholung = { coordinator.callbackHandler.handleAbholungPublic(it) },
                onAuslieferung = { coordinator.callbackHandler.handleAuslieferungPublic(it) },
                onKw = { customer ->
                    AlertDialog.Builder(this@TourPlannerActivity)
                        .setTitle(getString(R.string.dialog_kw_confirm_title))
                        .setMessage(getString(R.string.dialog_kw_confirm_message))
                        .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                            coordinator.callbackHandler.handleKwPublic(customer)
                            erledigungSheet = null
                        }
                        .setNegativeButton(getString(R.string.btn_cancel), null)
                        .show()
                },
                onRueckgaengig = { coordinator.callbackHandler.handleRueckgaengigPublic(it) },
                onVerschieben = { customer ->
                    AlertDialog.Builder(this@TourPlannerActivity)
                        .setTitle(getString(R.string.dialog_verschieben_confirm_title))
                        .setMessage(getString(R.string.dialog_verschieben_confirm_message))
                        .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                            erledigungSheet = null
                            coordinator.showVerschiebenDialog(customer)
                        }
                        .setNegativeButton(getString(R.string.btn_cancel), null)
                        .show()
                },
                onAddAbholungTermin = { customer, viewDateMillis ->
                    lifecycleScope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            repository.addKundenAbholungMitLieferung(
                                customer.id,
                                viewDateMillis,
                                customer.tageAzuLOrDefault(7)
                            )
                        }
                        erledigungSheet = null
                        coordinator.reloadCurrentView()
                        if (ok) {
                            Toast.makeText(this@TourPlannerActivity, getString(R.string.toast_abholungstermin_angelegt), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@TourPlannerActivity, getString(R.string.error_save_generic), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                getNaechstesTourDatum = { coordinator.dateUtils.getNaechstesTourDatum(it) },
                getTerminePairs365 = { customer ->
                    val liste = viewModel.getListen().find { it.id == customer.listeId }
                    termincache.getTerminePairs365(customer, liste)
                },
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
                        startActivity(com.example.we2026_5.util.AppNavigation.toCustomerDetail(this@TourPlannerActivity, customerId))
                    }
                },
                onNavigate = { customer ->
                    val dest = when {
                        customer.latitude != null && customer.longitude != null ->
                            "${customer.latitude},${customer.longitude}"
                        customer.adresse.isNotBlank() || customer.plz.isNotBlank() || customer.stadt.isNotBlank() -> {
                            buildString {
                                if (customer.adresse.isNotBlank()) append(customer.adresse.trim())
                                val plzStadt = listOf(customer.plz.trim(), customer.stadt.trim()).filter { it.isNotEmpty() }.joinToString(" ")
                                if (plzStadt.isNotEmpty()) {
                                    if (isNotEmpty()) append(", ")
                                    append(plzStadt)
                                }
                                if (isNotEmpty()) append(", Deutschland")
                            }.trim().takeIf { it.isNotEmpty() }
                        }
                        else -> null
                    }
                    if (dest != null) {
                        try {
                            val uri = if (customer.latitude != null && customer.longitude != null) {
                                Uri.parse("google.navigation:q=${customer.latitude},${customer.longitude}")
                            } else {
                                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(dest)}&dir_action=navigate")
                            }
                            startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps"))
                        } catch (_: android.content.ActivityNotFoundException) {
                            Toast.makeText(this@TourPlannerActivity, getString(R.string.error_maps_not_installed), Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@TourPlannerActivity, getString(R.string.toast_keine_adresse), Toast.LENGTH_SHORT).show()
                    }
                    overviewPayload = null
                    overviewRegelNamen = null
                },
                onReorder = { ids ->
                    viewModel.getSelectedTimestamp()?.let { ts ->
                        viewModel.setTourOrder(ts, ids)
                    }
                },
                isReihenfolgeBearbeiten = reihenfolgeBearbeitenMode,
                onReihenfolgeBearbeiten = { reihenfolgeBearbeitenMode = true },
                onReihenfolgeFertig = { reihenfolgeBearbeitenMode = false },
                cardShowAddress = appPrefs.showAddressOnCard,
                cardShowPhone = appPrefs.showPhoneOnCard,
                cardShowNotes = appPrefs.showNotesOnCard
            )
            }
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
