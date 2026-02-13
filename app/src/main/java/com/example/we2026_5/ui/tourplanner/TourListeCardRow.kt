package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.background
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
 * Listen-Card (ohne Wochentag): Äußere Card mit Listen-Name, darin jede Kunde als eigene Karte.
 * Bei A-Terminen: A-Farbe und A-Badge; bei L-Terminen: L-Farbe und L-Badge.
 */
@Composable
internal fun TourListeCardRow(
    liste: KundenListe,
    /** Customer, isOverdue, overdueAlSuffix („A“/„L“/„AL“ oder null) */
    kunden: List<Triple<Customer, Boolean, String?>>,
    aCount: Int = 0,
    lCount: Int = 0,
    viewDateMillis: Long,
    getStatusBadgeText: (Customer) -> String,
    onCustomerClick: (com.example.we2026_5.ui.tourplanner.CustomerOverviewPayload) -> Unit,
    onAktionenClick: (Customer) -> Unit,
    cardShowAddress: Boolean = true,
    cardShowPhone: Boolean = false,
    cardShowNotes: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val surfaceBg = colorResource(R.color.termin_regel_card_bg)
    val textPrimary = colorResource(R.color.text_primary)
    val colorA = colorResource(R.color.button_abholung)
    val colorL = colorResource(R.color.button_auslieferung)
    val headerBg = when {
        aCount > 0 -> colorA.copy(alpha = 0.25f)
        lCount > 0 -> colorL.copy(alpha = 0.25f)
        else -> surfaceBg
    }
    val headerText = when {
        aCount > 0 -> colorA
        lCount > 0 -> colorL
        else -> textPrimary
    }
    val badgeText = when {
        aCount > 0 -> "A"
        lCount > 0 -> "L"
        else -> null
    }
    val countForHeader = when {
        aCount > 0 -> aCount
        lCount > 0 -> lCount
        else -> 0
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .then(if (badgeText != null) Modifier.background(headerBg, RoundedCornerShape(6.dp)).padding(8.dp) else Modifier),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (badgeText != null) {
                        "${liste.name} $countForHeader ---"
                    } else {
                        liste.name
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = headerText
                )
                if (badgeText != null) {
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = badgeText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier
                            .background(headerText, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                if (badgeText == null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${kunden.size} ${if (kunden.size == 1) "Kunde" else "Kunden"}",
                        fontSize = 14.sp,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.padding(start = 4.dp))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Text(
                    text = if (expanded) "▲" else "▼",
                    fontSize = 16.sp,
                    color = headerText,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                kunden.forEachIndexed { index, (customer, isOverdue, overdueAlSuffix) ->
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
                    overdueAlSuffix = overdueAlSuffix,
                    viewDateMillis = viewDateMillis,
                    onCustomerClick = { onCustomerClick(payload) },
                    onAktionenClick = { onAktionenClick(customer) },
                    showAddress = cardShowAddress,
                    showPhone = cardShowPhone,
                    showNotes = cardShowNotes
                )
                if (index < kunden.size - 1) Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
