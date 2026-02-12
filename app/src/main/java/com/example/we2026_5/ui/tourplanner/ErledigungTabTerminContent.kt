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
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter

@Composable
fun ErledigungTabTerminContent(
    customer: Customer,
    getTerminePairs365: (Customer) -> List<Pair<Long, Long>>
) {
    val pairs = getTerminePairs365(customer)

    val textSecondary = colorResource(R.color.text_secondary)
    val badgeA = colorResource(R.color.button_abholung)
    val badgeL = colorResource(R.color.button_auslieferung)
    val badgeAusnahme = colorResource(R.color.status_ausnahme)

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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorResource(R.color.background_light), shape = MaterialTheme.shapes.small)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (aDatum > 0L) {
                            val (label, color) = if (isAusnahmeA) "A-A" to badgeAusnahme else "A" to badgeA
                            Text(
                                text = "$label ${DateFormatter.formatDateShortWithYear(aDatum)}",
                                fontSize = 13.sp,
                                color = color,
                                modifier = Modifier
                                    .background(color.copy(alpha = 0.2f), MaterialTheme.shapes.small)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (lDatum > 0L) {
                            val (label, color) = if (isAusnahmeL) "A-L" to badgeAusnahme else "L" to badgeL
                            Text(
                                text = "$label ${DateFormatter.formatDateShortWithYear(lDatum)}",
                                fontSize = 13.sp,
                                color = color,
                                modifier = Modifier
                                    .background(color.copy(alpha = 0.2f), MaterialTheme.shapes.small)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
