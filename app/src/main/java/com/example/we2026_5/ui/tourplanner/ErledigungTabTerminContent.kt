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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (pairs.isEmpty()) {
            Text(
                text = "Keine Termine",
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary,
                modifier = Modifier.padding(8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pairs.forEach { (aDatum, lDatum) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colorResource(R.color.background_light), shape = MaterialTheme.shapes.small)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "A ${DateFormatter.formatDateShortWithYear(aDatum)}",
                            fontSize = 13.sp,
                            color = badgeA,
                            modifier = Modifier
                                .background(badgeA.copy(alpha = 0.2f), MaterialTheme.shapes.small)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Text(
                            text = "L ${DateFormatter.formatDateShortWithYear(lDatum)}",
                            fontSize = 13.sp,
                            color = badgeL,
                            modifier = Modifier
                                .background(badgeL.copy(alpha = 0.2f), MaterialTheme.shapes.small)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
