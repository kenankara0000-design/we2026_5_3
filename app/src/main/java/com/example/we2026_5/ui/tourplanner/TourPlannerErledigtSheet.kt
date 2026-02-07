package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.example.we2026_5.Customer
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils
import com.example.we2026_5.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TourPlannerErledigtSheet(
    visible: Boolean,
    content: ErledigtSheetContent?,
    viewDateMillis: Long?,
    getStatusBadgeText: (Customer) -> String,
    onCustomerClick: (CustomerOverviewPayload) -> Unit,
    onAktionenClick: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible || content == null) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorResource(R.color.section_done_bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                stringResource(R.string.status_erledigt),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(8.dp)
            )
            if (content.doneOhneListen.isEmpty() && content.tourListenErledigt.isEmpty()) {
                Text(
                    stringResource(R.string.tour_erledigt_empty),
                    modifier = Modifier.padding(8.dp),
                    color = Color.White
                )
            }
            content.doneOhneListen.forEach { customer ->
                val isInUrlaub = viewDateMillis != null &&
                    TerminFilterUtils.istTerminInUrlaubEintraege(viewDateMillis, customer)
                val viewDate = viewDateMillis ?: 0L
                val urlaubInfo = if (isInUrlaub) {
                    val viewStart = TerminBerechnungUtils.getStartOfDay(viewDate)
                    val urlaubEntry = TerminFilterUtils.getEffectiveUrlaubEintraege(customer)
                        .firstOrNull { e ->
                            val vonStart = TerminBerechnungUtils.getStartOfDay(e.von)
                            val bisStart = TerminBerechnungUtils.getStartOfDay(e.bis)
                            viewStart in vonStart..bisStart
                        }
                    urlaubEntry?.let { "${com.example.we2026_5.util.DateFormatter.formatDate(it.von)} – ${com.example.we2026_5.util.DateFormatter.formatDate(it.bis)}" }
                        ?: if (customer.urlaubVon > 0 && customer.urlaubBis > 0)
                            "${com.example.we2026_5.util.DateFormatter.formatDate(customer.urlaubVon)} – ${com.example.we2026_5.util.DateFormatter.formatDate(customer.urlaubBis)}"
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
            content.tourListenErledigt.forEach { (listeName, kunden) ->
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
