package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.AusnahmeTermin
import com.example.we2026_5.KundenTermin
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.ExpandableSection
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.TerminBerechnungUtils

/** B4: Filter für „Alle Termine“ */
enum class AlleTermineFilter { ALLE, REGULAER, AUSNAHME, KUNDEN }

/** Zeilenhöhe ca. 52dp. */
private val BADGE_FONT_SP = 15.sp
private val ROW_PADDING_DP = 14.dp

/**
 * Block „Alle Termine“: einklappbar, scrollbare Liste. B4: Eine Sektion mit Typ-Filter-Chips.
 * Paare (A, L); 0 = nur anderer Typ (nur A oder nur L bei Ausnahme).
 */
@Composable
fun AlleTermineBlock(
    pairs: List<Pair<Long, Long>>,
    angelegtePairs: Set<Pair<Long, Long>> = emptySet(),
    graueMoeglicheTermine: Boolean = false,
    modifier: Modifier = Modifier,
    textPrimary: Color = colorResource(R.color.text_primary),
    textSecondary: Color = colorResource(R.color.text_secondary),
    /** B4: Typ-Filter (Alle / Regulär / Ausnahme / Kunden) */
    selectedFilter: AlleTermineFilter = AlleTermineFilter.ALLE,
    onFilterSelected: (AlleTermineFilter) -> Unit = {},
    /** Für Löschen im Edit-Modus */
    ausnahmeTermine: List<AusnahmeTermin> = emptyList(),
    kundenTermine: List<KundenTermin> = emptyList(),
    canDeleteTermin: Boolean = false,
    onDeleteAusnahmeTermin: (AusnahmeTermin) -> Unit = {},
    onDeleteKundenTermin: (List<KundenTermin>) -> Unit = {},
    onAddAbholungTermin: (() -> Unit)? = null
) {
    val filteredPairs = remember(pairs, angelegtePairs, selectedFilter) {
        pairs.filter { (aDatum, lDatum) ->
            val isAusnahmeA = aDatum > 0L && lDatum == 0L
            val isAusnahmeL = aDatum == 0L && lDatum > 0L
            val isAngelegt = angelegtePairs.contains(Pair(aDatum, lDatum))
            when (selectedFilter) {
                AlleTermineFilter.ALLE -> true
                AlleTermineFilter.REGULAER -> !isAusnahmeA && !isAusnahmeL && !isAngelegt
                AlleTermineFilter.AUSNAHME -> isAusnahmeA || isAusnahmeL
                AlleTermineFilter.KUNDEN -> isAngelegt && !isAusnahmeA && !isAusnahmeL
            }
        }
    }
    val badgeA = colorResource(R.color.button_abholung)
    val badgeL = colorResource(R.color.button_auslieferung)
    val badgeAusnahme = colorResource(R.color.status_ausnahme)
    val possibleBadgeColor = textSecondary
    val possibleBadgeBg = textSecondary.copy(alpha = 0.18f)
    val possibleRowBg = textSecondary.copy(alpha = 0.06f)
    val chipBg = colorResource(R.color.background_light)
    ExpandableSection(
        titleResId = R.string.label_alle_termine,
        defaultExpanded = false,
        textPrimary = textPrimary
    ) {
        Column(modifier = modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    AlleTermineFilter.ALLE to R.string.label_filter_all_termine,
                    AlleTermineFilter.REGULAER to R.string.label_filter_regulaer,
                    AlleTermineFilter.AUSNAHME to R.string.label_filter_ausnahme,
                    AlleTermineFilter.KUNDEN to R.string.label_filter_kunden
                ).forEach { (f, resId) ->
                    val selected = selectedFilter == f
                    Text(
                        text = stringResource(resId),
                        modifier = Modifier
                            .background(
                                if (selected) textPrimary.copy(alpha = 0.15f) else chipBg,
                                MaterialTheme.shapes.small
                            )
                            .clickable { onFilterSelected(f) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        color = if (selected) textPrimary else textSecondary
                    )
                }
            }
            if (onAddAbholungTermin != null) {
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text(
                        text = stringResource(R.string.label_neu_termin),
                        modifier = Modifier
                            .background(chipBg, MaterialTheme.shapes.small)
                            .clickable { onAddAbholungTermin() }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        fontSize = 13.sp,
                        color = textPrimary
                    )
                }
            }
            // C5: Kein innerer Scroll – Tab scrollt als Ganzes (kein Scroll-in-Scroll)
            Column(modifier = Modifier.fillMaxWidth()) {
                if (filteredPairs.isEmpty()) {
                    Text(
                        text = "Keine Termine",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textSecondary,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredPairs.forEach { (aDatum, lDatum) ->
                            val isAusnahmeA = aDatum > 0L && lDatum == 0L
                            val isAusnahmeL = aDatum == 0L && lDatum > 0L
                            val isAngelegt = angelegtePairs.contains(Pair(aDatum, lDatum))
                            val isMoeglich = !isAngelegt
                            val showAsPossible = graueMoeglicheTermine && isMoeglich
                            val canDeleteThis = canDeleteTermin && (isAusnahmeA || isAusnahmeL || isAngelegt)
                            val ausnahmeToDelete = when {
                                isAusnahmeA -> ausnahmeTermine.find { it.typ == "A" && TerminBerechnungUtils.getStartOfDay(it.datum) == aDatum }
                                isAusnahmeL -> ausnahmeTermine.find { it.typ == "L" && TerminBerechnungUtils.getStartOfDay(it.datum) == lDatum }
                                else -> null
                            }
                            val kundenToDelete = if (isAngelegt && aDatum > 0 && lDatum > 0) {
                                val aTermin = kundenTermine.find { it.typ == "A" && TerminBerechnungUtils.getStartOfDay(it.datum) == aDatum }
                                val lTermin = kundenTermine.find { it.typ == "L" && TerminBerechnungUtils.getStartOfDay(it.datum) == lDatum }
                                listOfNotNull(aTermin, lTermin)
                            } else emptyList()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (showAsPossible) possibleRowBg else colorResource(R.color.background_light),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .padding(ROW_PADDING_DP),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (aDatum > 0L) {
                                        val (label, baseColor) = if (isAusnahmeA) "A-A" to badgeAusnahme else "A" to badgeA
                                        val color = if (showAsPossible) possibleBadgeColor else baseColor
                                        val bg = if (showAsPossible) possibleBadgeBg else baseColor.copy(alpha = 0.2f)
                                        Text(
                                            text = "$label ${DateFormatter.formatDateShortWithYear(aDatum)}",
                                            fontSize = BADGE_FONT_SP,
                                            color = color,
                                            modifier = Modifier
                                                .background(bg, MaterialTheme.shapes.small)
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    if (lDatum > 0L) {
                                        val (label, baseColor) = if (isAusnahmeL) "A-L" to badgeAusnahme else "L" to badgeL
                                        val color = if (showAsPossible) possibleBadgeColor else baseColor
                                        val bg = if (showAsPossible) possibleBadgeBg else baseColor.copy(alpha = 0.2f)
                                        Text(
                                            text = "$label ${DateFormatter.formatDateShortWithYear(lDatum)}",
                                            fontSize = BADGE_FONT_SP,
                                            color = color,
                                            modifier = Modifier
                                                .background(bg, MaterialTheme.shapes.small)
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                if (canDeleteThis) {
                                    IconButton(
                                        onClick = {
                                            ausnahmeToDelete?.let { onDeleteAusnahmeTermin(it) }
                                            if (kundenToDelete.isNotEmpty()) onDeleteKundenTermin(kundenToDelete)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = stringResource(R.string.label_delete),
                                            tint = textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
