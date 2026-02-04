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

/** Argumente für das Erledigungs-Bottom-Sheet (Compose). */
data class ErledigungSheetArgs(
    val customer: Customer,
    val viewDateMillis: Long,
    val state: ErledigungSheetState
)

/** Payload für den Overview-Dialog beim Klick auf eine Kundenkarte. */
data class CustomerOverviewPayload(
    val customer: Customer,
    val urlaubInfo: String?,
    val verschobenInfo: String?,
    val verschobenVonInfo: String?,
    val ueberfaelligInfo: String?
)

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

@Composable
private fun SectionHeaderRow(
    title: String,
    countText: String,
    isExpanded: Boolean,
    sectionType: SectionType,
    sectionOverdueBg: Color,
    sectionOverdueText: Color,
    sectionDoneBg: Color,
    sectionDoneText: Color,
    onToggle: () -> Unit
) {
    val (bg, textColor) = when (sectionType) {
        SectionType.OVERDUE -> sectionOverdueBg to sectionOverdueText
        SectionType.DONE -> sectionDoneBg to sectionDoneText
        SectionType.LISTE -> sectionDoneBg to sectionDoneText
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = textColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(text = countText, color = textColor, fontSize = 14.sp)
            Spacer(Modifier.size(8.dp))
            Text(text = if (isExpanded) "-" else "+", color = textColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TourListeErledigtRow(
    listeName: String,
    erledigteKunden: List<Customer>,
    viewDateMillis: Long?,
    getStatusBadgeText: (Customer) -> String,
    onCustomerClick: (CustomerOverviewPayload) -> Unit,
    onAktionenClick: (Customer) -> Unit
) {
    val sectionDoneBg = colorResource(R.color.section_done_bg)
    val sectionDoneText = colorResource(R.color.section_done_text)
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = sectionDoneBg),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "— $listeName —",
                modifier = Modifier.padding(8.dp),
                color = sectionDoneText,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        erledigteKunden.forEach { customer ->
            val viewDate = viewDateMillis ?: 0L
            val isInUrlaub = viewDateMillis != null &&
                TerminFilterUtils.istTerminInUrlaubEintraege(viewDateMillis!!, customer)
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
                onCustomerClick = { onCustomerClick(payload) },
                onAktionenClick = { onAktionenClick(customer) }
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ListeHeaderRow(
    listeName: String,
    countText: String,
    isExpanded: Boolean,
    sectionDoneBg: Color,
    sectionDoneText: Color,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = sectionDoneBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = listeName,
                color = sectionDoneText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(text = countText, color = sectionDoneText, fontSize = 14.sp)
            Text(text = if (isExpanded) "-" else "+", color = sectionDoneText, fontWeight = FontWeight.Bold)
        }
    }
}

private val CustomerCardPadding = 16.dp
private val CustomerNameSp = 19.sp
private val CustomerBadgeSp = 16.sp
private val CustomerBadgePaddingH = 8.dp
private val CustomerBadgePaddingV = 4.dp
private val CustomerBadgeFixedWidth = 100.dp
private val CustomerButtonTextSp = 17.sp
private val CustomerButtonMinHeight = 44.dp

private fun getKundenArtLabel(customer: Customer): String = when {
    customer.kundenArt == "Gewerblich" -> "G"
    customer.kundenArt == "Privat" -> "P"
    customer.kundenArt == "Tour" || customer.kundenArt == "Liste" || customer.listeId.isNotEmpty() -> "T"
    else -> "G"
}

@Composable
private fun AlWochentagText(customer: Customer, color: Color) {
    val a = customer.defaultAbholungWochentag
    val l = customer.defaultAuslieferungWochentag
    val wochen = listOf(
        stringResource(R.string.label_weekday_short_mo), stringResource(R.string.label_weekday_short_tu),
        stringResource(R.string.label_weekday_short_mi), stringResource(R.string.label_weekday_short_do),
        stringResource(R.string.label_weekday_short_fr), stringResource(R.string.label_weekday_short_sa),
        stringResource(R.string.label_weekday_short_su)
    )
    val aStr = if (a in 0..6) wochen[a] else null
    val lStr = if (l in 0..6) wochen[l] else null
    val txt = when {
        aStr != null && lStr != null -> "$aStr A / $lStr L"
        aStr != null -> "$aStr A"
        lStr != null -> "$lStr L"
        else -> return
    }
    Text(txt, fontSize = 12.sp, color = color, modifier = Modifier.padding(top = 2.dp))
}


@Composable
private fun TourCustomerRow(
    customer: Customer,
    isOverdue: Boolean,
    isInUrlaub: Boolean,
    isVerschobenAmFaelligkeitstag: Boolean = false,
    verschobenInfo: String? = null,
    verschobenVonInfo: String? = null,
    statusBadgeText: String,
    viewDateMillis: Long = 0L,
    onCustomerClick: () -> Unit,
    onAktionenClick: () -> Unit
) {
    val isDeaktiviert = isVerschobenAmFaelligkeitstag
    val cardBg = when {
        isOverdue -> colorResource(R.color.section_overdue_bg)
        isInUrlaub -> colorResource(R.color.customer_urlaub_bg)
        isVerschobenAmFaelligkeitstag -> colorResource(R.color.surface_light)
        else -> colorResource(R.color.termin_regel_card_bg)
    }
    val nameColor = if (isOverdue) colorResource(R.color.section_overdue_text) else colorResource(R.color.text_primary)
    val gplColor = when {
        customer.kundenArt == "Gewerblich" -> colorResource(R.color.button_gewerblich_glossy)
        customer.listeId.isNotEmpty() -> colorResource(R.color.button_liste_glossy)
        else -> colorResource(R.color.button_privat_glossy)
    }
    // Badge-Text: Überfällig/Urlaub/Verschoben/A/L/AL mit vollem Text
    val badgeText = when {
        isOverdue -> stringResource(R.string.status_badge_overdue)
        isInUrlaub -> stringResource(R.string.label_urlaub)
        isVerschobenAmFaelligkeitstag -> stringResource(R.string.badge_verschoben)
        else -> statusBadgeText
    }
    val showBadge = isOverdue || isInUrlaub || isVerschobenAmFaelligkeitstag || statusBadgeText.isNotEmpty()
    val badgeColor = when {
        isOverdue -> colorResource(R.color.status_overdue)
        isInUrlaub -> colorResource(R.color.button_urlaub)
        isVerschobenAmFaelligkeitstag -> colorResource(R.color.status_info)
        statusBadgeText == "L" -> colorResource(R.color.termin_regel_auslieferung)
        else -> colorResource(R.color.termin_regel_abholung)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isDeaktiviert) Modifier else Modifier.clickable(onClick = onCustomerClick)
            ),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CustomerCardPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getKundenArtLabel(customer),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .background(gplColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    if (customer.fotoUrls.isNotEmpty()) {
                        Card(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            AsyncImage(
                                model = customer.fotoUrls.first(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.size(10.dp))
                    }
                    Text(
                        text = customer.name,
                        fontSize = CustomerNameSp,
                        color = nameColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                AlWochentagText(customer = customer, color = colorResource(R.color.text_secondary))
                val infoToShow = verschobenInfo ?: verschobenVonInfo
                if (infoToShow != null && infoToShow.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = infoToShow,
                        fontSize = 13.sp,
                        color = colorResource(R.color.text_secondary)
                    )
                }
                if (showBadge) {
                    Spacer(Modifier.height(8.dp))
                    val isAlBadge = statusBadgeText == "AL" && !isOverdue && !isInUrlaub && !isVerschobenAmFaelligkeitstag
                    if (isAlBadge) {
                        Row(
                            modifier = Modifier.width(CustomerBadgeFixedWidth),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(colorResource(R.color.termin_regel_abholung), RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp))
                                    .padding(horizontal = 4.dp, vertical = CustomerBadgePaddingV),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", fontSize = CustomerBadgeSp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(colorResource(R.color.termin_regel_auslieferung), RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                                    .padding(horizontal = 4.dp, vertical = CustomerBadgePaddingV),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("L", fontSize = CustomerBadgeSp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    } else {
                    val baseBadge = Modifier
                        .width(CustomerBadgeFixedWidth)
                        .background(color = badgeColor, shape = RoundedCornerShape(6.dp))
                        .padding(horizontal = CustomerBadgePaddingH, vertical = CustomerBadgePaddingV)
                    val badgeTextComposable = @Composable {
                        Text(
                            text = badgeText,
                            fontSize = CustomerBadgeSp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(modifier = baseBadge, contentAlignment = Alignment.Center) { badgeTextComposable() }
                    }
                }
            }
            Spacer(Modifier.size(12.dp))
            Button(
                onClick = if (isDeaktiviert) {{}} else onAktionenClick,
                modifier = Modifier.heightIn(min = CustomerButtonMinHeight),
                enabled = !isDeaktiviert,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.primary_blue_dark),
                    contentColor = Color.White,
                    disabledContainerColor = colorResource(R.color.button_inactive),
                    disabledContentColor = Color.White
                )
            ) {
                Text(
                    stringResource(R.string.sheet_aktionen),
                    fontSize = CustomerButtonTextSp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
