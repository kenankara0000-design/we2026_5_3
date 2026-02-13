package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils

@Composable
internal fun TourListeErledigtRow(
    listeName: String,
    erledigteKunden: List<Customer>,
    viewDateMillis: Long?,
    getStatusBadgeText: (Customer) -> String,
    onCustomerClick: (CustomerOverviewPayload) -> Unit,
    onAktionenClick: (Customer) -> Unit,
    cardShowAddress: Boolean = true,
    cardShowPhone: Boolean = false,
    cardShowNotes: Boolean = false
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
                fontSize = 17.sp
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
                showErledigtBadge = true,
                onCustomerClick = { onCustomerClick(payload) },
                onAktionenClick = { onAktionenClick(customer) },
                showAddress = cardShowAddress,
                showPhone = cardShowPhone,
                showNotes = cardShowNotes
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
