package com.example.we2026_5.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.we2026_5.R
import com.example.we2026_5.util.TerminBerechnungUtils
import java.util.Calendar

/**
 * Größerer Kalender für Termin-Datumsauswahl (inline oder im Dialog).
 * A-Tage des Kunden werden farblich hervorgehoben – alle Tage bleiben auswählbar.
 */
@Composable
fun TerminDatumKalenderContent(
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
    /** Wochentage 0=Mo..6=So, an denen A stattfindet – werden hervorgehoben. */
    aWochentage: List<Int> = emptyList(),
    /** Startdatum; Kalender zeigt diesen Monat initial. */
    initialDate: Long = System.currentTimeMillis(),
    modifier: Modifier = Modifier
) {
    val calInit = remember(initialDate) {
        Calendar.getInstance().apply { timeInMillis = initialDate }
    }
    var currentYear by remember(initialDate) { mutableStateOf(calInit.get(Calendar.YEAR)) }
    var currentMonth by remember(initialDate) { mutableStateOf(calInit.get(Calendar.MONTH)) }
    val primaryBlue = MaterialTheme.colorScheme.primary
    val aDayHighlight = MaterialTheme.colorScheme.primaryContainer
    val aDayText = MaterialTheme.colorScheme.onPrimaryContainer
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    val monatNamen = listOf(
        stringResource(R.string.label_month_jan), stringResource(R.string.label_month_feb),
        stringResource(R.string.label_month_mar), stringResource(R.string.label_month_apr),
        stringResource(R.string.label_month_may), stringResource(R.string.label_month_jun),
        stringResource(R.string.label_month_jul), stringResource(R.string.label_month_aug),
        stringResource(R.string.label_month_sep), stringResource(R.string.label_month_oct),
        stringResource(R.string.label_month_nov), stringResource(R.string.label_month_dec)
    )
    val wochentagKurz = listOf(
        stringResource(R.string.label_weekday_short_mo),
        stringResource(R.string.label_weekday_short_tu),
        stringResource(R.string.label_weekday_short_mi),
        stringResource(R.string.label_weekday_short_do),
        stringResource(R.string.label_weekday_short_fr),
        stringResource(R.string.label_weekday_short_sa),
        stringResource(R.string.label_weekday_short_su)
    )

    fun getWeekday(date: Calendar): Int = (date.get(Calendar.DAY_OF_WEEK) + 5) % 7 // 0=Mo .. 6=So
    fun getDaysInMonth(year: Int, month: Int): Int {
        val c = Calendar.getInstance()
        c.set(year, month, 1)
        return c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    fun getFirstWeekdayOfMonth(year: Int, month: Int): Int {
        val c = Calendar.getInstance()
        c.set(year, month, 1)
        return getWeekday(c)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = stringResource(R.string.termin_anlegen_ausnahme_datum_waehlen),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (aWochentage.isNotEmpty()) {
                    val aStr = aWochentage.sorted().joinToString(", ") { wochentagKurz.getOrNull(it) ?: "" }
                    Text(
                        text = stringResource(R.string.label_calendar_a_days_hint, aStr),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (currentMonth == Calendar.JANUARY) {
                            currentMonth = Calendar.DECEMBER
                            currentYear--
                        } else {
                            currentMonth--
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.content_desc_prev_month))
                    }
                    Text(
                        text = "${monatNamen.getOrNull(currentMonth) ?: ""} $currentYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = {
                        if (currentMonth == Calendar.DECEMBER) {
                            currentMonth = Calendar.JANUARY
                            currentYear++
                        } else {
                            currentMonth++
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.content_desc_next_month))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    wochentagKurz.forEach { day ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val firstWd = getFirstWeekdayOfMonth(currentYear, currentMonth)
                val daysInMonth = getDaysInMonth(currentYear, currentMonth)
                val cells = firstWd + daysInMonth
                val rows = (cells + 6) / 7
                val todayStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(rows) { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(7) { col ->
                                val cellIndex = row * 7 + col
                                val dayNum = if (cellIndex >= firstWd && cellIndex < firstWd + daysInMonth) {
                                    cellIndex - firstWd + 1
                                } else null
                                val cal = Calendar.getInstance()
                                cal.set(currentYear, currentMonth, dayNum ?: 1, 0, 0, 0)
                                cal.set(Calendar.MILLISECOND, 0)
                                val dayMs = if (dayNum != null) cal.timeInMillis else 0L
                                val wd = if (dayNum != null) getWeekday(cal) else -1
                                val isADay = wd in aWochentage
                                val isToday = dayNum != null && dayMs == todayStart

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(2.dp)
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                dayNum == null -> androidx.compose.ui.graphics.Color.Transparent
                                                isToday -> primaryBlue.copy(alpha = 0.3f)
                                                isADay -> aDayHighlight
                                                else -> surfaceVariant.copy(alpha = 0.5f)
                                            }
                                        )
                                        .then(
                                            if (dayNum != null) Modifier.clickable {
                                                onDateSelected(dayMs)
                                                onDismiss()
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (dayNum != null) {
                                        Text(
                                            text = "$dayNum",
                                            fontSize = 16.sp,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                            color = when {
                                                isToday -> primaryBlue
                                                isADay -> aDayText
                                                else -> onSurface
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
            }
        }
    }
}

/**
 * Kalender als Dialog (überlagert den Bildschirm).
 */
@Composable
fun TerminDatumKalenderDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
    aWochentage: List<Int> = emptyList(),
    initialDate: Long = System.currentTimeMillis(),
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        TerminDatumKalenderContent(
            onDismiss = onDismiss,
            onDateSelected = onDateSelected,
            aWochentage = aWochentage,
            initialDate = initialDate,
            modifier = modifier.fillMaxWidth(0.95f)
        )
    }
}
