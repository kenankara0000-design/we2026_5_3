package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.R
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils

/**
 * Tour-Liste Card: Äußere Card mit Listen-Name, darin jede Kunde als eigene Karte.
 */
@Composable
internal fun TourListeCardRow(
    liste: KundenListe,
    kunden: List<Pair<Customer, Boolean>>,
    viewDateMillis: Long,
    getStatusBadgeText: (Customer) -> String,
    onCustomerClick: (com.example.we2026_5.ui.tourplanner.CustomerOverviewPayload) -> Unit,
    onAktionenClick: (Customer) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val surfaceBg = colorResource(R.color.termin_regel_card_bg)
    val textPrimary = colorResource(R.color.text_primary)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = liste.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${kunden.size} ${if (kunden.size == 1) "Kunde" else "Kunden"}",
                    fontSize = 14.sp,
                    color = textPrimary
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontSize = 16.sp,
                    color = textPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                kunden.forEachIndexed { index, (customer, isOverdue) ->
                val isInUrlaub = TerminFilterUtils.istTerminInUrlaubEintraege(viewDateMillis, customer)
                val viewStart = TerminBerechnungUtils.getStartOfDay(viewDateMillis)
                val urlaubInfo = if (isInUrlaub) {
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
                } else ""
                val payload = CustomerOverviewPayload(
                    customer = customer,
                    urlaubInfo = urlaubInfo.takeIf { it.isNotEmpty() },
                    verschobenInfo = null,
                    verschobenVonInfo = null,
                    ueberfaelligInfo = null
                )
                TourCustomerRow(
                    customer = customer,
                    isOverdue = isOverdue,
                    isInUrlaub = isInUrlaub,
                    statusBadgeText = getStatusBadgeText(customer),
                    viewDateMillis = viewDateMillis,
                    onCustomerClick = { onCustomerClick(payload) },
                    onAktionenClick = { onAktionenClick(customer) }
                )
                if (index < kunden.size - 1) Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
