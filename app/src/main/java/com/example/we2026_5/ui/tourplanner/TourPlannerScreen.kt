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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import coil.compose.AsyncImage
import com.example.we2026_5.Customer
import com.example.we2026_5.ListItem
import com.example.we2026_5.R
import com.example.we2026_5.SectionType

/** Argumente für das Erledigungs-Bottom-Sheet (Compose). */
data class ErledigungSheetArgs(
    val customer: Customer,
    val viewDateMillis: Long,
    val state: ErledigungSheetState
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourPlannerScreen(
    tourItems: List<ListItem>,
    dateText: String,
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
    onCustomerClick: (Customer) -> Unit,
    onAktionenClick: (Customer) -> Unit,
    onDismissErledigungSheet: () -> Unit,
    onAbholung: (Customer) -> Unit,
    onAuslieferung: (Customer) -> Unit,
    onKw: (Customer) -> Unit,
    onRueckgaengig: (Customer) -> Unit,
    onVerschieben: (Customer) -> Unit,
    onUrlaub: (Customer) -> Unit,
    getNaechstesTourDatum: (Customer) -> Long?,
    showToast: (String) -> Unit,
    onTelefonClick: (String) -> Unit,
    overviewCustomer: Customer?,
    overviewRegelNamen: String?,
    overviewCustomerIdForDetails: String?,
    onDismissOverview: () -> Unit,
    onOpenDetails: (customerId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (overviewCustomer != null) {
        AlertDialog(
            onDismissRequest = onDismissOverview,
            title = { Text(overviewCustomer.name, fontWeight = FontWeight.Bold) },
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
                }
            },
            confirmButton = {
                Button(onClick = {
                    val id = overviewCustomerIdForDetails ?: overviewCustomer?.id ?: ""
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
                onUrlaub = onUrlaub,
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
                        }
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
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tourItems, key = { item ->
                            when (item) {
                                is ListItem.CustomerItem -> "c-${item.customer.id}"
                                is ListItem.SectionHeader -> "s-${item.sectionType}"
                                is ListItem.ListeHeader -> "l-${item.listeId}"
                            }
                        }) { item ->
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
                                is ListItem.CustomerItem -> TourCustomerRow(
                                    customer = item.customer,
                                    isOverdue = item.isOverdue,
                                    statusBadgeText = getStatusBadgeText(item.customer),
                                    onCustomerClick = { onCustomerClick(item.customer) },
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

@Composable
private fun TourCustomerRow(
    customer: Customer,
    isOverdue: Boolean,
    statusBadgeText: String,
    onCustomerClick: () -> Unit,
    onAktionenClick: () -> Unit
) {
    val cardBg = if (isOverdue) colorResource(R.color.customer_overdue_bg) else colorResource(R.color.surface_white)
    val nameColor = if (isOverdue) colorResource(R.color.status_overdue) else colorResource(R.color.text_primary)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCustomerClick),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (customer.fotoUrls.isNotEmpty()) {
                        Card(
                            modifier = Modifier.size(32.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            AsyncImage(
                                model = customer.fotoUrls.first(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.size(8.dp))
                    }
                    Text(
                        text = customer.name,
                        color = nameColor,
                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                    )
                }
                if (statusBadgeText.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = statusBadgeText,
                        fontSize = 12.sp,
                        color = Color.White,
                        modifier = Modifier
                            .background(
                                color = if (isOverdue) colorResource(R.color.status_overdue) else colorResource(R.color.primary_blue),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Button(
                onClick = onAktionenClick,
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.button_blue))
            ) {
                Text(stringResource(R.string.sheet_aktionen))
            }
        }
    }
}
