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

/** Zeilenhöhe ca. 52dp. 6 Zeilen sichtbar. */
private const val MAX_VISIBLE_ROWS = 6
private val ROW_HEIGHT_DP = 52.dp
private val MAX_HEIGHT_DP = ROW_HEIGHT_DP * MAX_VISIBLE_ROWS
private val BADGE_FONT_SP = 15.sp
private val ROW_PADDING_DP = 14.dp

/**
 * Block „Alle Termine“: einklappbar, scrollbare Liste. Paare (A, L); 0 = nur anderer Typ (nur A oder nur L bei Ausnahme).
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
                                .padding(ROW_PADDING_DP),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            if (aDatum > 0L) {
                                Text(
                                    text = "A ${DateFormatter.formatDateShortWithYear(aDatum)}",
                                    fontSize = BADGE_FONT_SP,
                                    color = badgeA,
                                    modifier = Modifier
                                        .background(badgeA.copy(alpha = 0.2f), MaterialTheme.shapes.small)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                            if (lDatum > 0L) {
                                Text(
                                    text = "L ${DateFormatter.formatDateShortWithYear(lDatum)}",
                                    fontSize = BADGE_FONT_SP,
                                    color = badgeL,
                                    modifier = Modifier
                                        .background(badgeL.copy(alpha = 0.2f), MaterialTheme.shapes.small)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
