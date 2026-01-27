package com.example.we2026_5.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Zentrale Klasse für Datum-Formatierung.
 * Stellt konsistente Datumsformate für die gesamte App bereit.
 */
object DateFormatter {
    
    /**
     * Formatiert ein Datum als "26.1.2026" (Tag.Monat.Jahr ohne führende Nullen)
     */
    fun formatDate(calendar: Calendar): String {
        return "${calendar.get(Calendar.DAY_OF_MONTH)}.${calendar.get(Calendar.MONTH) + 1}.${calendar.get(Calendar.YEAR)}"
    }
    
    /**
     * Formatiert ein Datum als "26.1.2026" (Tag.Monat.Jahr ohne führende Nullen)
     * @param timeInMillis Zeitstempel in Millisekunden
     */
    fun formatDate(timeInMillis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeInMillis
        return formatDate(cal)
    }
    
    /**
     * Formatiert ein Datum als "26.01.2026" (Tag.Monat.Jahr mit führenden Nullen)
     */
    fun formatDateWithLeadingZeros(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }
    
    /**
     * Formatiert ein Datum als "26.01.2026" (Tag.Monat.Jahr mit führenden Nullen)
     * @param timeInMillis Zeitstempel in Millisekunden
     */
    fun formatDateWithLeadingZeros(timeInMillis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeInMillis
        return formatDateWithLeadingZeros(cal)
    }
    
    /**
     * Formatiert eine Zeit als "14:30" (Stunde:Minute mit führenden Nullen)
     */
    fun formatTime(calendar: Calendar): String {
        return String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }
    
    /**
     * Formatiert eine Zeit als "14:30" (Stunde:Minute mit führenden Nullen)
     * @param timeInMillis Zeitstempel in Millisekunden
     */
    fun formatTime(timeInMillis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeInMillis
        return formatTime(cal)
    }
    
    /**
     * Formatiert Datum und Zeit als "26.1.2026 14:30" (Tag.Monat.Jahr Stunde:Minute)
     */
    fun formatDateTime(calendar: Calendar): String {
        val dateStr = formatDate(calendar)
        val timeStr = formatTime(calendar)
        return "$dateStr $timeStr"
    }
    
    /**
     * Formatiert Datum und Zeit als "26.1.2026 14:30" (Tag.Monat.Jahr Stunde:Minute)
     * @param timeInMillis Zeitstempel in Millisekunden
     */
    fun formatDateTime(timeInMillis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeInMillis
        return formatDateTime(cal)
    }
    
    /**
     * Formatiert ein kurzes Datum als "26.01" (Tag.Monat)
     */
    fun formatDateShort(calendar: Calendar): String {
        return "${calendar.get(Calendar.DAY_OF_MONTH)}.${String.format("%02d", calendar.get(Calendar.MONTH) + 1)}"
    }
    
    /**
     * Formatiert ein kurzes Datum als "26.01" (Tag.Monat)
     * @param timeInMillis Zeitstempel in Millisekunden
     */
    fun formatDateShort(timeInMillis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeInMillis
        return formatDateShort(cal)
    }
}
