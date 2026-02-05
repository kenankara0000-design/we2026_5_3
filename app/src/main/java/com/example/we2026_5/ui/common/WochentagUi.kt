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

private val WOCHENTAG_SHORT_RES = listOf(
    R.string.label_weekday_short_mo,
    R.string.label_weekday_short_tu,
    R.string.label_weekday_short_mi,
    R.string.label_weekday_short_do,
    R.string.label_weekday_short_fr,
    R.string.label_weekday_short_sa,
    R.string.label_weekday_short_su
)

/**
 * Kurzbezeichnungen Mo–So aus String-Ressourcen (für nicht-Composable Kontext).
 * Index = Wochentag 0..6.
 */
fun getWochentagShortResIds(): List<Int> = WOCHENTAG_SHORT_RES

/**
 * Formatiert A/L-Wochentag als String (z. B. "Mo A / Do L").
 */
fun formatALWochentag(customer: Customer, getString: (Int) -> String): String {
    val a = customer.defaultAbholungWochentag
    val l = customer.defaultAuslieferungWochentag
    val aStr = if (a in 0..6) getString(WOCHENTAG_SHORT_RES[a]) else null
    val lStr = if (l in 0..6) getString(WOCHENTAG_SHORT_RES[l]) else null
    return when {
        aStr != null && lStr != null -> "$aStr A / $lStr L"
        aStr != null -> "$aStr A"
        lStr != null -> "$lStr L"
        else -> ""
    }
}

@Composable
fun AlWochentagText(customer: Customer, color: Color) {
    val a = customer.defaultAbholungWochentag
    val l = customer.defaultAuslieferungWochentag
    val wochen = WOCHENTAG_SHORT_RES.map { stringResource(it) }
    val aStr = if (a in 0..6) wochen[a] else null
    val lStr = if (l in 0..6) wochen[l] else null
    val txt = when {
        aStr != null && lStr != null -> "$aStr A / $lStr L"
        aStr != null -> "$aStr A"
        lStr != null -> "$lStr L"
        else -> return
    }
    Text(txt, fontSize = 12.sp, color = color, modifier = Modifier.padding(top = 2.dp))
}

/**
 * Einzeilige Chip-Row für einen Wochentag (Single-Select).
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
    val chipBg = Color(0xFFE0E0E0)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        weekdays.forEachIndexed { index, title ->
            val isSelected = selected == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = 0.dp)
                    .heightIn(min = 36.dp)
                    .background(
                        if (isSelected) primaryBlue else chipBg,
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelect(index) }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = title, color = if (isSelected) Color.White else textPrimary, fontSize = 13.sp, maxLines = 1)
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
    val weekdayColorLight = Color(0xFFE0E0E0)
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
