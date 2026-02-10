package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.ExpandableSection
import com.example.we2026_5.util.DateFormatter

/** Zeilenhöhe ca. 44dp (padding + Text). 6 Zeilen = ~264dp. */
private const val MAX_VISIBLE_ROWS = 6
private val ROW_HEIGHT_DP = 44.dp
private val MAX_HEIGHT_DP = ROW_HEIGHT_DP * MAX_VISIBLE_ROWS

/**
 * Block „Alle Termine“: einklappbar (standardmäßig eingeklappt), scrollbare Liste von A/L-Paaren (max. 6 Zeilen sichtbar).
 */
@Composable
fun AlleTermineBlock(
    pairs: List<Pair<Long, Long>>,
    modifier: Modifier = Modifier,
    textPrimary: Color = colorResource(R.color.text_primary),
    textSecondary: Color = colorResource(R.color.text_secondary)
) {
    val badgeA = colorResource(R.color.button_abholung)
    val badgeL = colorResource(R.color.button_auslieferung)
    ExpandableSection(
        titleResId = R.string.label_alle_termine,
        defaultExpanded = false,
        textPrimary = textPrimary
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = MAX_HEIGHT_DP)
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
}
