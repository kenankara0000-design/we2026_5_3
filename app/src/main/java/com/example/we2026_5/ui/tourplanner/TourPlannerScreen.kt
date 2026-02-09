package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.we2026_5.tourplanner.ErledigungSheetState
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import kotlin.math.abs
import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.example.we2026_5.ui.tourplanner.TourCustomerRow
import com.example.we2026_5.ui.tourplanner.TourListeErledigtRow

@Composable
private fun TourPlannerDragHandleContent() {
    val dragHandleDesc = stringResource(R.string.tour_drag_handle_desc)
    IconButton(
        onClick = {},
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = dragHandleDesc }
    ) {
        Text(text = "⋮⋮", fontSize = 20.sp, color = colorResource(R.color.text_secondary))
    }
}

@Composable
private fun rememberSwipeBetweenDaysModifier(
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit
): Modifier {
    val density = LocalDensity.current
    val commitThresholdPx = remember(density) { with(density) { 40.dp.toPx() } }
    val triggerThresholdPx = remember(density) { with(density) { 80.dp.toPx() } }
    var totalX by remember { mutableStateOf(0f) }
    var totalY by remember { mutableStateOf(0f) }
    var committedHorizontal by remember { mutableStateOf(false) }
    return Modifier.pointerInput(onPrevDay, onNextDay) {
        detectDragGestures(
            onDragStart = {
                totalX = 0f
                totalY = 0f
                committedHorizontal = false
            },
            onDrag = { change, dragAmount ->
                totalX += dragAmount.x
                totalY += dragAmount.y
                if (!committedHorizontal) {
                    val ax = abs(totalX)
                    val ay = abs(totalY)
                    if (ax > commitThresholdPx || ay > commitThresholdPx) {
                        committedHorizontal = ax > ay * 1.5f
                        if (committedHorizontal) change.consume()
                    }
                } else {
                    change.consume()
                }
            },
            onDragEnd = {
                if (committedHorizontal && abs(totalX) > triggerThresholdPx) {
                    if (totalX > 0) onPrevDay() else onNextDay()
                }
            }
        )
    }
}

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
    onDismissErledigtSheet: () -> Unit = {},
    onReorder: ((fromListIndex: Int, toListIndex: Int) -> Unit)? = null
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
            val swipeModifier = rememberSwipeBetweenDaysModifier(onPrevDay, onNextDay)
            when {
                isLoading -> Box(Modifier.fillMaxSize().then(swipeModifier)) {
                    TourPlannerLoadingView(primaryBlue = primaryBlue)
                }
                errorMessage != null -> Box(Modifier.fillMaxSize().then(swipeModifier)) {
                    TourPlannerErrorView(
                        errorMessage = errorMessage,
                        textSecondary = textSecondary,
                        onRetry = onRetry
                    )
                }
                tourItems.isEmpty() -> Box(Modifier.fillMaxSize().then(swipeModifier)) {
                    TourPlannerEmptyView(textSecondary = textSecondary)
                }
                else -> {
                    val lazyListState = rememberLazyListState()
                    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
                        if (tourItems.getOrNull(from.index) is ListItem.CustomerItem &&
                            tourItems.getOrNull(to.index) is ListItem.CustomerItem
                        ) {
                            onReorder?.invoke(from.index, to.index)
                        }
                    }
                    val hapticFeedback = LocalHapticFeedback.current
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .then(swipeModifier)
                            .fillMaxSize()
                            .background(colorResource(R.color.background_light))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(tourItems, key = { index, listItem ->
                            when (listItem) {
                                is ListItem.CustomerItem -> "c-${listItem.customer.id}"
                                is ListItem.SectionHeader -> "h-${listItem.sectionType}-$index"
                                is ListItem.ListeHeader -> "l-${listItem.listeId}-$index"
                                is ListItem.TourListeErledigt -> "t-${listItem.listeName}-$index"
                                is ListItem.ErledigtSection -> "e-$index"
                            }
                        }) { index, item ->
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
                                    val faelligAmDisplay = TerminBerechnungUtils.effectiveFaelligAmDatum(item.customer)
                                    val ueberfaelligInfo = if (item.isOverdue && faelligAmDisplay > 0) {
                                        val faelligStr = DateFormatter.formatDate(faelligAmDisplay)
                                        val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
                                        val faelligStart = TerminBerechnungUtils.getStartOfDay(faelligAmDisplay)
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
                                    ReorderableItem(reorderableState, key = item.customer.id) { isDragging ->
                                        val interactionSource = remember { MutableInteractionSource() }
                                        val handleModifier = if (onReorder != null) Modifier.longPressDraggableHandle(
                                            onDragStarted = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
                                            onDragStopped = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) },
                                            interactionSource = interactionSource
                                        ) else null
                                        TourCustomerRow(
                                            customer = item.customer,
                                            isOverdue = item.isOverdue,
                                            isInUrlaub = isInUrlaub,
                                            isVerschobenAmFaelligkeitstag = item.isVerschobenAmFaelligkeitstag,
                                            verschobenInfo = item.verschobenInfo,
                                            verschobenVonInfo = item.verschobenVonInfo,
                                            statusBadgeText = item.statusBadgeText,
                                            viewDateMillis = viewDate,
                                            onCustomerClick = { onCustomerClick(payload) },
                                            onAktionenClick = { onAktionenClick(item.customer) },
                                            dragHandleModifier = handleModifier,
                                            dragHandleContent = if (onReorder != null) { { TourPlannerDragHandleContent() } } else null,
                                            cardDragModifier = null,
                                            cardInteractionSource = null
                                        )
                                    }
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

