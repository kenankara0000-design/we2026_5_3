package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.we2026_5.tourplanner.ErledigungSheetState
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.activity.compose.BackHandler
import com.example.we2026_5.Customer
import com.example.we2026_5.ListItem
import com.example.we2026_5.ui.tourplanner.ErledigtSheetContent
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils
import com.example.we2026_5.R
import com.example.we2026_5.SectionType
import com.example.we2026_5.ui.tourplanner.ListeHeaderRow
import com.example.we2026_5.ui.tourplanner.SectionHeaderRow
import com.example.we2026_5.ui.tourplanner.TourCustomerRow
import com.example.we2026_5.ui.tourplanner.TourListeErledigtRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourPlannerScreen(
    tourItems: List<ListItem>,
    viewDateMillis: Long?,
    dateText: String,
    tourCounts: Pair<Int, Int>,
    isToday: Boolean, // true wenn angezeigtes Datum heute ist → Heute-Button orange
    isLoading: Boolean,
    errorMessage: String?,
    isOffline: Boolean,
    pressedHeaderButton: String?, // "Karte" | "Heute" | null
    erledigungSheet: ErledigungSheetArgs?,
    onBack: () -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onMap: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    isSectionExpanded: (SectionType) -> Boolean,
    getStatusBadgeText: (Customer) -> String,
    onToggleSection: (SectionType) -> Unit,
    onCustomerClick: (CustomerOverviewPayload) -> Unit,
    onAktionenClick: (Customer) -> Unit,
    onDismissErledigungSheet: () -> Unit,
    onAbholung: (Customer) -> Unit,
    onAuslieferung: (Customer) -> Unit,
    onKw: (Customer) -> Unit,
    onRueckgaengig: (Customer) -> Unit,
    onVerschieben: (Customer) -> Unit,
    getNaechstesTourDatum: (Customer) -> Long?,
    showToast: (String) -> Unit,
    onTelefonClick: (String) -> Unit,
    overviewPayload: CustomerOverviewPayload?,
    overviewRegelNamen: String?,
    onDismissOverview: () -> Unit,
    onOpenDetails: (customerId: String) -> Unit,
    erledigtCount: Int = 0,
    erledigtSheetVisible: Boolean = false,
    erledigtSheetContent: ErledigtSheetContent? = null,
    onErledigtClick: () -> Unit = {},
    onDismissErledigtSheet: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (overviewPayload != null) {
        TourPlannerOverviewDialog(
            payload = overviewPayload,
            overviewRegelNamen = overviewRegelNamen,
            onDismiss = onDismissOverview,
            onOpenDetails = onOpenDetails
        )
    }

    BackHandler(enabled = erledigungSheet != null) {
        onDismissErledigungSheet()
    }
    BackHandler(enabled = erledigtSheetVisible) {
        onDismissErledigtSheet()
    }
    if (erledigungSheet != null) {
        ModalBottomSheet(
            onDismissRequest = onDismissErledigungSheet,
            sheetState = sheetState
        ) {
            ErledigungSheetContent(
                customer = erledigungSheet.customer,
                viewDateMillis = erledigungSheet.viewDateMillis,
                state = erledigungSheet.state,
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissErledigungSheet() }
                },
                onAbholung = onAbholung,
                onAuslieferung = onAuslieferung,
                onKw = onKw,
                onRueckgaengig = onRueckgaengig,
                onVerschieben = onVerschieben,
                getNaechstesTourDatum = getNaechstesTourDatum,
                showToast = showToast,
                onTelefonClick = onTelefonClick
            )
        }
    }

    val primaryBlue = colorResource(R.color.primary_blue)
    val textSecondary = colorResource(R.color.text_secondary)
    val sectionOverdueBg = colorResource(R.color.section_overdue_bg)
    val sectionOverdueText = colorResource(R.color.section_overdue_text)
    val sectionDoneBg = colorResource(R.color.section_done_bg)
    val sectionDoneText = colorResource(R.color.section_done_text)

    Scaffold(
        topBar = {
            TourPlannerTopBar(
                dateText = dateText,
                tourCounts = tourCounts,
                isToday = isToday,
                isOffline = isOffline,
                pressedHeaderButton = pressedHeaderButton,
                erledigtCount = erledigtCount,
                onBack = onBack,
                onPrevDay = onPrevDay,
                onNextDay = onNextDay,
                onToday = onToday,
                onMap = onMap,
                onRefresh = onRefresh,
                onErledigtClick = onErledigtClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> TourPlannerLoadingView(primaryBlue = primaryBlue)
                errorMessage != null -> TourPlannerErrorView(
                    errorMessage = errorMessage,
                    textSecondary = textSecondary,
                    onRetry = onRetry
                )
                tourItems.isEmpty() -> TourPlannerEmptyView(textSecondary = textSecondary)
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colorResource(R.color.background_light))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(tourItems, key = { index, _ -> index }) { _, item ->
                            when (item) {
                                is ListItem.ErledigtSection -> { /* Erledigt-Bereich nur im Sheet, nicht in der Liste */ }
                                is ListItem.SectionHeader -> SectionHeaderRow(
                                    title = item.title,
                                    countText = when (item.sectionType) {
                                        SectionType.OVERDUE -> "(${item.count})"
                                        else -> "${item.erledigtCount}/${item.count}"
                                    },
                                    isExpanded = isSectionExpanded(item.sectionType),
                                    sectionType = item.sectionType,
                                    sectionOverdueBg = sectionOverdueBg,
                                    sectionOverdueText = sectionOverdueText,
                                    sectionDoneBg = sectionDoneBg,
                                    sectionDoneText = sectionDoneText,
                                    onToggle = { onToggleSection(item.sectionType) }
                                )
                                is ListItem.ListeHeader -> ListeHeaderRow(
                                    listeName = item.listeName,
                                    countText = "${item.erledigtCount}/${item.kundenCount}",
                                    isExpanded = false,
                                    sectionDoneBg = sectionDoneBg,
                                    sectionDoneText = sectionDoneText,
                                    onToggle = { }
                                )
                                is ListItem.TourListeErledigt -> {
                                    TourListeErledigtRow(
                                        listeName = item.listeName,
                                        erledigteKunden = item.erledigteKunden,
                                        viewDateMillis = viewDateMillis,
                                        getStatusBadgeText = getStatusBadgeText,
                                        onCustomerClick = onCustomerClick,
                                        onAktionenClick = onAktionenClick
                                    )
                                }
                                is ListItem.CustomerItem -> {
                                    val isInUrlaub = viewDateMillis != null &&
                                        TerminFilterUtils.istTerminInUrlaubEintraege(viewDateMillis!!, item.customer)
                                    val viewDate = viewDateMillis ?: 0L
                                    val urlaubInfo = if (isInUrlaub) {
                                        val viewStart = TerminBerechnungUtils.getStartOfDay(viewDate)
                                        val urlaubEntry = TerminFilterUtils.getEffectiveUrlaubEintraege(item.customer)
                                            .firstOrNull { e ->
                                                val vonStart = TerminBerechnungUtils.getStartOfDay(e.von)
                                                val bisStart = TerminBerechnungUtils.getStartOfDay(e.bis)
                                                viewStart in vonStart..bisStart
                                            }
                                        urlaubEntry?.let { "${DateFormatter.formatDate(it.von)} – ${DateFormatter.formatDate(it.bis)}" }
                                            ?: if (item.customer.urlaubVon > 0 && item.customer.urlaubBis > 0)
                                                "${DateFormatter.formatDate(item.customer.urlaubVon)} – ${DateFormatter.formatDate(item.customer.urlaubBis)}"
                                            else ""
                                    } else null
                                    val ueberfaelligInfo = if (item.isOverdue && item.customer.faelligAmDatum > 0) {
                                        val faelligStr = DateFormatter.formatDate(item.customer.faelligAmDatum)
                                        val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
                                        val faelligStart = TerminBerechnungUtils.getStartOfDay(item.customer.faelligAmDatum)
                                        val tageUeberfaellig = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(heuteStart - faelligStart).toInt()
                                        buildString {
                                            append("Fällig: $faelligStr")
                                            if (tageUeberfaellig > 0) append(", seit $tageUeberfaellig Tagen überfällig")
                                        }
                                    } else null
                                    val payload = CustomerOverviewPayload(
                                        customer = item.customer,
                                        urlaubInfo = urlaubInfo?.takeIf { it.isNotEmpty() },
                                        verschobenInfo = item.verschobenInfo?.takeIf { it.isNotEmpty() },
                                        verschobenVonInfo = item.verschobenVonInfo?.takeIf { it.isNotEmpty() },
                                        ueberfaelligInfo = ueberfaelligInfo
                                    )
                                    TourCustomerRow(
                                        customer = item.customer,
                                        isOverdue = item.isOverdue,
                                        isInUrlaub = isInUrlaub,
                                        isVerschobenAmFaelligkeitstag = item.isVerschobenAmFaelligkeitstag,
                                        verschobenInfo = item.verschobenInfo,
                                        verschobenVonInfo = item.verschobenVonInfo,
                                        statusBadgeText = getStatusBadgeText(item.customer),
                                        viewDateMillis = viewDate,
                                        onCustomerClick = { onCustomerClick(payload) },
                                        onAktionenClick = { onAktionenClick(item.customer) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (erledigtSheetVisible && erledigtSheetContent != null) {
                TourPlannerErledigtSheet(
                    visible = true,
                    content = erledigtSheetContent,
                    viewDateMillis = viewDateMillis,
                    getStatusBadgeText = getStatusBadgeText,
                    onCustomerClick = onCustomerClick,
                    onAktionenClick = onAktionenClick,
                    onDismiss = onDismissErledigtSheet
                )
            }
        }
    }
}

