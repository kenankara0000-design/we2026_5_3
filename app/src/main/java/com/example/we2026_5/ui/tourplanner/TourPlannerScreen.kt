package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.example.we2026_5.Customer
import com.example.we2026_5.ListItem
import com.example.we2026_5.R
import com.example.we2026_5.SectionType
import com.example.we2026_5.ui.tourplanner.ErledigtSheetContent
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourPlannerScreen(
    tourItems: List<ListItem>,
    viewDateMillis: Long?,
    dateText: String,
    tourCounts: Pair<Int, Int>,
    isToday: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    isOffline: Boolean,
    pressedHeaderButton: String?,
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
    onAddAbholungTermin: (Customer, Long) -> Unit,
    getNaechstesTourDatum: (Customer) -> Long?,
    getTerminePairs365: (Customer) -> List<Pair<Long, Long>>,
    showToast: (String) -> Unit,
    onTelefonClick: (String) -> Unit,
    overviewPayload: CustomerOverviewPayload?,
    overviewRegelNamen: String?,
    onDismissOverview: () -> Unit,
    onOpenDetails: (customerId: String) -> Unit,
    onNavigate: (Customer) -> Unit = {},
    erledigtCount: Int = 0,
    erledigtSheetVisible: Boolean = false,
    erledigtSheetContent: ErledigtSheetContent? = null,
    onErledigtClick: () -> Unit = {},
    onDismissErledigtSheet: () -> Unit = {},
    onReorder: (List<String>) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (overviewPayload != null) {
        TourPlannerOverviewDialog(
            payload = overviewPayload,
            overviewRegelNamen = overviewRegelNamen,
            onDismiss = onDismissOverview,
            onOpenDetails = onOpenDetails,
            onNavigate = onNavigate
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
                onAddAbholungTermin = onAddAbholungTermin,
                getNaechstesTourDatum = getNaechstesTourDatum,
                getTerminePairs365 = getTerminePairs365,
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
                isLoading -> Box(Modifier.fillMaxSize()) {
                    TourPlannerLoadingView(primaryBlue = primaryBlue)
                }
                errorMessage != null -> Box(Modifier.fillMaxSize()) {
                    TourPlannerErrorView(
                        errorMessage = errorMessage,
                        textSecondary = textSecondary,
                        onRetry = onRetry
                    )
                }
                tourItems.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    TourPlannerEmptyView(textSecondary = textSecondary)
                }
                else -> TourPlannerListContent(
                    tourItems = tourItems,
                    viewDateMillis = viewDateMillis,
                    isSectionExpanded = isSectionExpanded,
                    getStatusBadgeText = getStatusBadgeText,
                    onToggleSection = onToggleSection,
                    onCustomerClick = onCustomerClick,
                    onAktionenClick = onAktionenClick,
                    onReorder = onReorder,
                    sectionOverdueBg = sectionOverdueBg,
                    sectionOverdueText = sectionOverdueText,
                    sectionDoneBg = sectionDoneBg,
                    sectionDoneText = sectionDoneText
                )
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

@Composable
private fun TourPlannerListContent(
    tourItems: List<ListItem>,
    viewDateMillis: Long?,
    isSectionExpanded: (SectionType) -> Boolean,
    getStatusBadgeText: (Customer) -> String,
    onToggleSection: (SectionType) -> Unit,
    onCustomerClick: (CustomerOverviewPayload) -> Unit,
    onAktionenClick: (Customer) -> Unit,
    onReorder: (List<String>) -> Unit,
    sectionOverdueBg: androidx.compose.ui.graphics.Color,
    sectionOverdueText: androidx.compose.ui.graphics.Color,
    sectionDoneBg: androidx.compose.ui.graphics.Color,
    sectionDoneText: androidx.compose.ui.graphics.Color
) {
    val lazyListState = rememberLazyListState()
    var displayItems by remember { mutableStateOf(tourItems) }

    val dragState = remember(lazyListState) {
        TourPlannerDragDropState(
            lazyListState = lazyListState,
            isCustomerItem = { idx -> idx in displayItems.indices && displayItems[idx] is ListItem.CustomerItem },
            onMove = { from, to ->
                if (from != to && from in displayItems.indices && to in displayItems.indices &&
                    displayItems[from] is ListItem.CustomerItem && displayItems[to] is ListItem.CustomerItem) {
                    displayItems = displayItems.toMutableList().apply {
                        val el = removeAt(from)
                        add(to.coerceIn(0, size), el)
                    }
                }
            },
            onDragEnd = {
                val ids = displayItems.filterIsInstance<ListItem.CustomerItem>().map { it.customer.id }
                if (ids.isNotEmpty()) onReorder(ids)
                displayItems = tourItems
            }
        )
    }

    if (dragState.currentIndexOfDraggedItem == null && tourItems !== displayItems) {
        displayItems = tourItems
    }

    val scope = rememberCoroutineScope()

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_light))
            .padding(16.dp)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset: Offset -> dragState.onDragStart(offset) },
                    onDrag = { change: PointerInputChange, offset: Offset ->
                        change.consume()
                        dragState.onDrag(dragAmount = offset)
                        dragState.checkForOverScroll().takeIf { it != 0f }?.let { delta ->
                            scope.launch {
                                lazyListState.scroll { scrollBy(delta) }
                            }
                        }
                    },
                    onDragEnd = { dragState.onDragInterrupted() },
                    onDragCancel = { dragState.onDragInterrupted() }
                )
            },
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        itemsIndexed(
            items = displayItems,
            key = { index: Int, listItem: ListItem ->
                when (listItem) {
                    is ListItem.CustomerItem -> "c-${listItem.customer.id}"
                    is ListItem.SectionHeader -> "h-${listItem.sectionType}-$index"
                    is ListItem.ListeHeader -> "l-${listItem.listeId}-$index"
                    is ListItem.TourListeCard -> "tc-${listItem.liste.id}-$index"
                    is ListItem.TourListeErledigt -> "t-${listItem.listeName}-$index"
                    is ListItem.ErledigtSection -> "e-$index"
                }
            },
            itemContent = { index, item ->
            val dragOffset = if (index == dragState.currentIndexOfDraggedItem) dragState.elementDisplacement ?: 0f else 0f
            Box(modifier = Modifier.graphicsLayer { translationY = dragOffset }) {
                when (item) {
                    is ListItem.ErledigtSection -> { }
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
                is ListItem.TourListeCard -> TourListeCardRow(
                    liste = item.liste,
                    kunden = item.kunden,
                    aCount = item.aCount,
                    lCount = item.lCount,
                    viewDateMillis = viewDateMillis ?: 0L,
                    getStatusBadgeText = getStatusBadgeText,
                    onCustomerClick = onCustomerClick,
                    onAktionenClick = onAktionenClick
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
                    val payload = CustomerOverviewPayload(
                        customer = item.customer,
                        urlaubInfo = urlaubInfo?.takeIf { it.isNotEmpty() },
                        verschobenInfo = item.verschobenInfo?.takeIf { it.isNotEmpty() },
                        verschobenVonInfo = item.verschobenVonInfo?.takeIf { it.isNotEmpty() },
                        ueberfaelligInfo = null
                    )
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
                        isDragging = index == dragState.currentIndexOfDraggedItem
                    )
                }
            }
        }
        }
        )
    }
}
