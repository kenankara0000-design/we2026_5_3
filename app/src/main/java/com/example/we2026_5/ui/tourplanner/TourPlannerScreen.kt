package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.example.we2026_5.tourplanner.ErledigungSheetState
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import com.example.we2026_5.Customer
import com.example.we2026_5.ListItem
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils
import com.example.we2026_5.R
import com.example.we2026_5.SectionType
import com.example.we2026_5.ui.CustomerTypeButtonHelper
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
    onOpenDetails: (customerId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (overviewPayload != null) {
        val customer = overviewPayload.customer
        AlertDialog(
            onDismissRequest = onDismissOverview,
            title = { Text(customer.name, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.label_termine_regeln),
                        fontSize = 13.sp,
                        color = colorResource(R.color.text_secondary)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = overviewRegelNamen ?: stringResource(R.string.no_termin_regeln),
                        fontSize = 16.sp,
                        color = colorResource(R.color.text_primary)
                    )
                    if (overviewPayload.urlaubInfo != null && overviewPayload.urlaubInfo.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.label_urlaub),
                            fontSize = 13.sp,
                            color = colorResource(R.color.text_secondary)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = overviewPayload.urlaubInfo,
                            fontSize = 16.sp,
                            color = colorResource(R.color.text_primary)
                        )
                    }
                    if (overviewPayload.verschobenInfo != null && overviewPayload.verschobenInfo.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.badge_verschoben),
                            fontSize = 13.sp,
                            color = colorResource(R.color.text_secondary)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = overviewPayload.verschobenInfo,
                            fontSize = 16.sp,
                            color = colorResource(R.color.text_primary)
                        )
                    }
                    if (overviewPayload.verschobenVonInfo != null && overviewPayload.verschobenVonInfo.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.badge_verschoben),
                            fontSize = 13.sp,
                            color = colorResource(R.color.text_secondary)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = overviewPayload.verschobenVonInfo,
                            fontSize = 16.sp,
                            color = colorResource(R.color.text_primary)
                        )
                    }
                    if (overviewPayload.ueberfaelligInfo != null && overviewPayload.ueberfaelligInfo.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.status_badge_overdue),
                            fontSize = 13.sp,
                            color = colorResource(R.color.text_secondary)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = overviewPayload.ueberfaelligInfo,
                            fontSize = 16.sp,
                            color = colorResource(R.color.text_primary)
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val id = customer.id
                    if (id.isNotBlank()) {
                        onOpenDetails(id)
                    }
                    onDismissOverview()
                }) {
                    Text(stringResource(R.string.label_details_open))
                }
            }
        )
    }

    BackHandler(enabled = erledigungSheet != null) {
        onDismissErledigungSheet()
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
    val buttonBlue = colorResource(R.color.button_blue)
    val statusWarning = colorResource(R.color.status_warning)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val surfaceWhite = colorResource(R.color.surface_white)
    val sectionOverdueBg = colorResource(R.color.section_overdue_bg)
    val sectionOverdueText = colorResource(R.color.section_overdue_text)
    val sectionDoneBg = colorResource(R.color.section_done_bg)
    val sectionDoneText = colorResource(R.color.section_done_text)

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(primaryBlue)) {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = stringResource(R.string.content_desc_back),
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue, navigationIconContentColor = Color.White),
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onPrevDay) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_back),
                                    contentDescription = stringResource(R.string.content_desc_prev_day),
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = dateText,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(onClick = onNextDay) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_forward),
                                    contentDescription = stringResource(R.string.content_desc_next_day),
                                    tint = Color.White
                                )
                            }
                            IconButton(onClick = onRefresh) {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.label_refresh),
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.label_tour_counts, tourCounts.first, tourCounts.second),
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                    }
                )
                if (isOffline) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_offline),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFFFEB3B)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            stringResource(R.string.main_offline),
                            color = Color(0xFFFFEB3B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onMap,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (pressedHeaderButton == "Karte") statusWarning else buttonBlue
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_map),
                            contentDescription = stringResource(R.string.content_desc_map),
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.tour_btn_map), color = Color.White)
                    }
                    Button(
                        onClick = onToday,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isToday) statusWarning else buttonBlue
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_calendar_today),
                            contentDescription = stringResource(R.string.content_desc_today),
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.tour_btn_today), color = Color.White)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryBlue)
                    }
                }
                errorMessage != null -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.tour_error_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.status_overdue)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            errorMessage,
                            fontSize = 14.sp,
                            color = textSecondary
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRetry) {
                            Text(stringResource(R.string.tour_retry))
                        }
                    }
                }
                tourItems.isEmpty() -> {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📅", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.tour_empty_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.tour_empty_subtitle),
                            fontSize = 14.sp,
                            color = textSecondary
                        )
                    }
                }
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
                                is ListItem.ErledigtSection -> {
                                    val expanded = isSectionExpanded(SectionType.DONE)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = colorResource(R.color.section_done_bg)),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            SectionHeaderRow(
                                                title = item.title,
                                                countText = "${item.erledigtCount}/${item.count}",
                                                isExpanded = expanded,
                                                sectionType = SectionType.DONE,
                                                sectionOverdueBg = sectionOverdueBg,
                                                sectionOverdueText = sectionOverdueText,
                                                sectionDoneBg = colorResource(R.color.section_done_bg),
                                                sectionDoneText = sectionDoneText,
                                                onToggle = { onToggleSection(SectionType.DONE) }
                                            )
                                            if (expanded) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    item.doneOhneListen.forEach { customer ->
                                                        val isInUrlaub = viewDateMillis != null &&
                                                            TerminFilterUtils.istTerminInUrlaubEintraege(viewDateMillis!!, customer)
                                                        val viewDate = viewDateMillis ?: 0L
                                                        val urlaubInfo = if (isInUrlaub) {
                                                            val viewStart = TerminBerechnungUtils.getStartOfDay(viewDate)
                                                            val urlaubEntry = TerminFilterUtils.getEffectiveUrlaubEintraege(customer)
                                                                .firstOrNull { e ->
                                                                    val vonStart = TerminBerechnungUtils.getStartOfDay(e.von)
                                                                    val bisStart = TerminBerechnungUtils.getStartOfDay(e.bis)
                                                                    viewStart in vonStart..bisStart
                                                                }
                                                            urlaubEntry?.let { "${DateFormatter.formatDate(it.von)} – ${DateFormatter.formatDate(it.bis)}" }
                                                                ?: if (customer.urlaubVon > 0 && customer.urlaubBis > 0)
                                                                    "${DateFormatter.formatDate(customer.urlaubVon)} – ${DateFormatter.formatDate(customer.urlaubBis)}"
                                                                else ""
                                                        } else null
                                                        val payload = CustomerOverviewPayload(
                                                            customer = customer,
                                                            urlaubInfo = urlaubInfo?.takeIf { it.isNotEmpty() },
                                                            verschobenInfo = null,
                                                            verschobenVonInfo = null,
                                                            ueberfaelligInfo = null
                                                        )
                                                        TourCustomerRow(
                                                            customer = customer,
                                                            isOverdue = false,
                                                            isInUrlaub = isInUrlaub,
                                                            isVerschobenAmFaelligkeitstag = false,
                                                            verschobenInfo = null,
                                                            verschobenVonInfo = null,
                                                            statusBadgeText = getStatusBadgeText(customer),
                                                            viewDateMillis = viewDate,
                                                            showErledigtBadge = true,
                                                            onCustomerClick = { onCustomerClick(payload) },
                                                            onAktionenClick = { onAktionenClick(customer) }
                                                        )
                                                    }
                                                    item.tourListenErledigt.forEach { (listeName, kunden) ->
                                                        TourListeErledigtRow(
                                                            listeName = listeName,
                                                            erledigteKunden = kunden,
                                                            viewDateMillis = viewDateMillis,
                                                            getStatusBadgeText = getStatusBadgeText,
                                                            onCustomerClick = onCustomerClick,
                                                            onAktionenClick = onAktionenClick
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
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
        }
    }
}

