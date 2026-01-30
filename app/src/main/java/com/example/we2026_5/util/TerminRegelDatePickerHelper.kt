package com.example.we2026_5.util

import android.app.DatePickerDialog
import android.content.Context
import java.util.Calendar

/**
 * Zentrale Hilfsfunktionen für Datumsauswahl und -formatierung in Termin-Regel-UI.
 * Einheitliches Format: TT.MM.JJJJ
 */
object TerminRegelDatePickerHelper {

    private const val DATE_FORMAT = "%02d.%02d.%04d"

    /**
     * Formatiert Tag, Monat (0-basiert), Jahr als "TT.MM.JJJJ".
     */
    fun formatDate(day: Int, month: Int, year: Int): String =
        String.format(DATE_FORMAT, day, month + 1, year)

    /**
     * Formatiert einen Calendar oder Zeitstempel als "TT.MM.JJJJ".
     */
    fun formatDateFromMillis(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return formatDate(
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.YEAR)
        )
    }

    /**
     * Zeigt DatePickerDialog; bei Auswahl wird der Zeitstempel (Start des Tages) an onDateSelected übergeben.
     */
    fun showDatePicker(
        context: Context,
        initialYear: Int,
        initialMonth: Int,
        initialDay: Int,
        onDateSelected: (timestamp: Long) -> Unit
    ) {
        DatePickerDialog(
            context,
            android.R.style.Theme_Material_Dialog_Alert,
            { _, selectedYear, selectedMonth, selectedDay ->
                val cal = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onDateSelected(cal.timeInMillis)
            },
            initialYear,
            initialMonth,
            initialDay
        ).show()
    }
}
