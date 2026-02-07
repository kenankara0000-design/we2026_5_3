package com.example.we2026_5.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R

/**
 * Auswahl für monatliche Wochentags-Regel: Woche im Monat (1.–4. oder Letzte) + Wochentag (Mo–So).
 * Eigenständige Datei, damit keine bestehende Datei unnötig wächst (Plan: kein Refactoring nötig).
 */
@Composable
fun MonthlyWeekdayRuleContent(
    monthWeekOfMonth: Int,
    monthWeekday: Int,
    onMonthWeekChange: (Int) -> Unit,
    onWeekdayChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryBlue = colorResource(R.color.primary_blue)
    val textPrimary = colorResource(R.color.text_primary)
    val weekLabels = listOf(
        1 to stringResource(R.string.label_monthly_week_1),
        2 to stringResource(R.string.label_monthly_week_2),
        3 to stringResource(R.string.label_monthly_week_3),
        4 to stringResource(R.string.label_monthly_week_4),
        5 to stringResource(R.string.label_monthly_week_last)
    )
    Column(modifier = modifier.fillMaxWidth().padding(8.dp)) {
        Text(
            stringResource(R.string.label_monthly_week_of_month),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(Modifier.height(6.dp))
        WeekOfMonthChipRow(
            selected = monthWeekOfMonth,
            options = weekLabels,
            onSelect = onMonthWeekChange,
            primaryBlue = primaryBlue,
            textPrimary = textPrimary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.label_wochentag),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(Modifier.height(6.dp))
        WochentagChipRowFromResources(
            selected = monthWeekday,
            onSelect = onWeekdayChange,
            primaryBlue = primaryBlue,
            textPrimary = textPrimary
        )
    }
}

@Composable
private fun WeekOfMonthChipRow(
    selected: Int,
    options: List<Pair<Int, String>>,
    onSelect: (Int) -> Unit,
    primaryBlue: Color,
    textPrimary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = selected == value
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (isSelected) primaryBlue else colorResource(R.color.section_done_bg),
                        RoundedCornerShape(6.dp)
                    )
                    .clickable { onSelect(value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White else textPrimary,
                    maxLines = 1
                )
            }
        }
    }
}
