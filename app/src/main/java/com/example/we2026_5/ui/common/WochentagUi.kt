package com.example.we2026_5.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.ui.theme.AppColors

private val WOCHENTAG_SHORT_RES = listOf(
    R.string.label_weekday_short_mo,
    R.string.label_weekday_short_tu,
    R.string.label_weekday_short_mi,
    R.string.label_weekday_short_do,
    R.string.label_weekday_short_fr,
    R.string.label_weekday_short_sa,
    R.string.label_weekday_short_su
)

private val WOCHENTAG_FULL_RES = listOf(
    R.string.label_weekday_monday,
    R.string.label_weekday_tuesday,
    R.string.label_weekday_wednesday,
    R.string.label_weekday_thursday,
    R.string.label_weekday_friday,
    R.string.label_weekday_saturday,
    R.string.label_weekday_sunday
)

/**
 * Kurzbezeichnungen Mo–So aus String-Ressourcen (für nicht-Composable Kontext).
 * Index = Wochentag 0..6.
 */
fun getWochentagShortResIds(): List<Int> = WOCHENTAG_SHORT_RES

/**
 * Vollständige Wochentagsnamen (Montag–Sonntag) aus String-Ressourcen.
 * Index = Wochentag 0..6.
 */
fun getWochentagFullResIds(): List<Int> = WOCHENTAG_FULL_RES

/**
 * Formatiert A/L-Wochentage als String (z. B. "Mo A / Do L" oder "Mo, Mi A / Di, Do L").
 */
fun formatALWochentag(customer: Customer, getString: (Int) -> String): String {
    val aDays = customer.effectiveAbholungWochentage
    val lDays = customer.effectiveAuslieferungWochentage
    val aStr = aDays.map { getString(WOCHENTAG_SHORT_RES[it]) }.joinToString(", ").takeIf { it.isNotEmpty() }?.let { "$it A" }
    val lStr = lDays.map { getString(WOCHENTAG_SHORT_RES[it]) }.joinToString(", ").takeIf { it.isNotEmpty() }?.let { "$it L" }
    return listOfNotNull(aStr, lStr).joinToString(" / ")
}

@Composable
fun AlWochentagText(customer: Customer, color: Color) {
    val aDays = customer.effectiveAbholungWochentage
    val lDays = customer.effectiveAuslieferungWochentage
    val wochen = WOCHENTAG_SHORT_RES.map { stringResource(it) }
    val aStr = aDays.map { wochen[it] }.joinToString(", ").takeIf { it.isNotEmpty() }?.let { "$it A" }
    val lStr = lDays.map { wochen[it] }.joinToString(", ").takeIf { it.isNotEmpty() }?.let { "$it L" }
    val txt = listOfNotNull(aStr, lStr).joinToString(" / ")
    if (txt.isEmpty()) return
    Text(txt, fontSize = 12.sp, color = color, modifier = Modifier.padding(top = 2.dp))
}

/** Hellgrau für Mo–Fr, hellrot für Sa/So (Standard Abhol-/Auslieferungstage). */
private val CHIP_BG_WEEKDAY = AppColors.LightGray

/**
 * Einzeilige Chip-Row für einen Wochentag (Single-Select).
 * Mo–Fr: hellgrauer Hintergrund, schwarzer Text. Sa/So: hellroter Hintergrund, roter Text.
 * selected: 0..6 oder -1 für keiner.
 */
@Composable
fun WochentagChipRow(
    selected: Int,
    weekdays: List<String>,
    onSelect: (Int) -> Unit,
    primaryBlue: Color,
    textPrimary: Color
) {
    val weekendBg = colorResource(R.color.section_overdue_bg)
    val weekendText = colorResource(R.color.section_overdue_text)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        weekdays.forEachIndexed { index, title ->
            val isWeekend = index == 5 || index == 6
            val isSelected = selected == index
            val (bgColor, textColor) = when {
                isSelected -> primaryBlue to Color.White
                isWeekend -> weekendBg to weekendText
                else -> CHIP_BG_WEEKDAY to textPrimary
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = 0.dp)
                    .heightIn(min = 36.dp)
                    .background(bgColor, RoundedCornerShape(6.dp))
                    .clickable { onSelect(index) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = title, color = textColor, fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

/**
 * Wochentag-Chips mit String-Ressourcen (Mo–So).
 */
@Composable
fun WochentagChipRowFromResources(
    selected: Int,
    onSelect: (Int) -> Unit,
    primaryBlue: Color,
    textPrimary: Color
) {
    val weekdays = WOCHENTAG_SHORT_RES.map { stringResource(it) }
    WochentagChipRow(selected = selected, weekdays = weekdays, onSelect = onSelect, primaryBlue = primaryBlue, textPrimary = textPrimary)
}

/**
 * Multi-Select Wochentags-Chips in einer Zeile (gleiche Optik wie WochentagChipRow: Mo–Fr grau, Sa/So hellrot).
 * Für Standard-Abhol-/Auslieferungstage mit Mehrfachauswahl.
 */
@Composable
fun WochentagChipRowMultiSelectFromResources(
    selectedDays: List<Int>,
    onDayToggle: (Int) -> Unit,
    primaryBlue: Color,
    textPrimary: Color
) {
    val weekendBg = colorResource(R.color.section_overdue_bg)
    val weekendText = colorResource(R.color.section_overdue_text)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        WOCHENTAG_SHORT_RES.forEachIndexed { index, resId ->
            val isWeekend = index == 5 || index == 6
            val isSelected = index in selectedDays
            val (bgColor, textColor) = when {
                isSelected -> primaryBlue to Color.White
                isWeekend -> weekendBg to weekendText
                else -> CHIP_BG_WEEKDAY to textPrimary
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = 0.dp)
                    .heightIn(min = 36.dp)
                    .background(bgColor, RoundedCornerShape(6.dp))
                    .clickable { onDayToggle(index) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(resId), color = textColor, fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

/**
 * Multi-Select Wochentags-Chips (z. B. für TerminRegel Abhol-/Auslieferungstage).
 * selectedDays: Indizes 0..6 die ausgewählt sind.
 */
@Composable
fun WochentagListenMenue(
    selectedDays: List<Int>,
    onDayToggle: (Int) -> Unit,
    primaryBlue: Color,
    textPrimary: Color
) {
    val weekendColor = colorResource(R.color.status_overdue)
    val weekendColorLight = weekendColor.copy(alpha = 0.25f)
    val weekdayColorLight = AppColors.LightGray
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        WOCHENTAG_SHORT_RES.forEachIndexed { index, resId ->
            val isWeekend = index == 5 || index == 6
            val isSelected = index in selectedDays
            val (bgColor, textColor) = when {
                isSelected && isWeekend -> weekendColor to Color.White
                isSelected && !isWeekend -> primaryBlue to Color.White
                !isSelected && isWeekend -> weekendColorLight to weekendColor
                else -> weekdayColorLight to textPrimary
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = 0.dp)
                    .heightIn(min = 48.dp)
                    .background(bgColor, RoundedCornerShape(8.dp))
                    .clickable { onDayToggle(index) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(resId),
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
