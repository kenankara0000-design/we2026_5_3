package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.tageAzuLOrDefault
import java.util.concurrent.TimeUnit

@Composable
fun ErledigungTabTerminContent(
    customer: Customer,
    getTerminePairs365: (Customer) -> List<Pair<Long, Long>>
) {
    val pairs = getTerminePairs365(customer)
    val tageAzuL = customer.tageAzuLOrDefault(7)
    val angelegtePairs: Set<Pair<Long, Long>> =
        buildSet {
            // Kunden-Termine (A -> L = A + tageAzuL)
            customer.kundenTermine
                .filter { it.typ == "A" }
                .forEach { a ->
                    val aStart = TerminBerechnungUtils.getStartOfDay(a.datum)
                    val lDatum = TerminBerechnungUtils.getStartOfDay(aStart + TimeUnit.DAYS.toMillis(tageAzuL.toLong()))
                    add(Pair(aStart, lDatum))
                }
            // Robuster: falls DB historisch nur L gespeichert hat (ohne A)
            customer.kundenTermine
                .filter { it.typ == "L" }
                .forEach { l ->
                    val lStart = TerminBerechnungUtils.getStartOfDay(l.datum)
                    val aStart = TerminBerechnungUtils.getStartOfDay(lStart - TimeUnit.DAYS.toMillis(tageAzuL.toLong()))
                    if (aStart > 0L) add(Pair(aStart, lStart))
                }
            // Ausnahme-Termine (nur A oder nur L)
            customer.ausnahmeTermine.forEach { t ->
                val d = TerminBerechnungUtils.getStartOfDay(t.datum)
                if (t.typ == "A") add(Pair(d, 0L))
                else if (t.typ == "L") add(Pair(0L, d))
            }
        }

    val textSecondary = colorResource(R.color.text_secondary)
    val badgeA = colorResource(R.color.button_abholung)
    val badgeL = colorResource(R.color.button_auslieferung)
    val badgeAusnahme = colorResource(R.color.status_ausnahme)
    val graueMoegliche = customer.kundenTyp == KundenTyp.UNREGELMAESSIG
    val possibleBadgeColor = textSecondary
    val possibleBadgeBg = textSecondary.copy(alpha = 0.18f)
    val possibleRowBg = textSecondary.copy(alpha = 0.06f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (pairs.isEmpty()) {
            Text(
                text = stringResource(R.string.tour_keine_termine),
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pairs.forEach { (aDatum, lDatum) ->
                    val isAusnahmeA = aDatum > 0L && lDatum == 0L
                    val isAusnahmeL = aDatum == 0L && lDatum > 0L
                    val isAngelegt = angelegtePairs.contains(Pair(aDatum, lDatum))
                    val isMoeglich = !isAngelegt
                    val showAsPossible = graueMoegliche && isMoeglich
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (showAsPossible) possibleRowBg else colorResource(R.color.background_light),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (aDatum > 0L) {
                            val (label, baseColor) = if (isAusnahmeA) "A-A" to badgeAusnahme else "A" to badgeA
                            val color = if (showAsPossible) possibleBadgeColor else baseColor
                            val bg = if (showAsPossible) possibleBadgeBg else baseColor.copy(alpha = 0.2f)
                            Text(
                                text = "$label ${DateFormatter.formatDateShortWithYear(aDatum)}",
                                fontSize = 13.sp,
                                color = color,
                                modifier = Modifier
                                    .background(bg, MaterialTheme.shapes.small)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (lDatum > 0L) {
                            val (label, baseColor) = if (isAusnahmeL) "A-L" to badgeAusnahme else "L" to badgeL
                            val color = if (showAsPossible) possibleBadgeColor else baseColor
                            val bg = if (showAsPossible) possibleBadgeBg else baseColor.copy(alpha = 0.2f)
                            Text(
                                text = "$label ${DateFormatter.formatDateShortWithYear(lDatum)}",
                                fontSize = 13.sp,
                                color = color,
                                modifier = Modifier
                                    .background(bg, MaterialTheme.shapes.small)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
