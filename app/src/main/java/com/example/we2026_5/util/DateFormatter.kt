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
        val cal = AppTimeZone.newCalendar()
        cal.timeInMillis = timeInMillis
        return formatDate(cal)
    }
    
    /**
     * Formatiert ein Datum als "26.01.2026" (Tag.Monat.Jahr mit führenden Nullen)
     */
    fun formatDateWithLeadingZeros(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).apply { timeZone = AppTimeZone.timeZone }
        return dateFormat.format(calendar.time)
    }
    
    /**
     * Formatiert ein Datum als "26.01.2026" (Tag.Monat.Jahr mit führenden Nullen)
     * @param timeInMillis Zeitstempel in Millisekunden
     */
    fun formatDateWithLeadingZeros(timeInMillis: Long): String {
        val cal = AppTimeZone.newCalendar()
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
        val cal = AppTimeZone.newCalendar()
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
        val cal = AppTimeZone.newCalendar()
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
        val cal = AppTimeZone.newCalendar()
        cal.timeInMillis = timeInMillis
        return formatDateShort(cal)
    }

    /**
     * Formatiert ein Datum als "09.02.26" (dd.MM.yy mit führenden Nullen)
     */
    fun formatDateShortWithYear(timeInMillis: Long): String {
        val cal = AppTimeZone.newCalendar()
        cal.timeInMillis = timeInMillis
        return SimpleDateFormat("dd.MM.yy", Locale.getDefault()).apply { timeZone = AppTimeZone.timeZone }.format(cal.time)
    }
    
    /**
     * Formatiert ein Datum mit Wochentag als "Mo, 26.01.2026"
     * @param calendar Calendar-Objekt
     */
    fun formatDateWithWeekday(calendar: Calendar): String {
        val weekday = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
        val dateStr = formatDateWithLeadingZeros(calendar)
        return "$weekday, $dateStr"
    }
    
    /**
     * Formatiert ein Datum mit Wochentag als "Mo, 26.01.2026"
     * @param timeInMillis Zeitstempel in Millisekunden
     */
    fun formatDateWithWeekday(timeInMillis: Long): String {
        val cal = AppTimeZone.newCalendar()
        cal.timeInMillis = timeInMillis
        return formatDateWithWeekday(cal)
    }
    
    /**
     * Formatiert ein Datum mit vollem Wochentag als "Montag, 26.01.2026"
     * @param calendar Calendar-Objekt
     */
    fun formatDateWithFullWeekday(calendar: Calendar): String {
        val weekday = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
        val dateStr = formatDateWithLeadingZeros(calendar)
        return "$weekday, $dateStr"
    }
    
    /**
     * Formatiert ein Datum mit vollem Wochentag als "Montag, 26.01.2026"
     * @param timeInMillis Zeitstempel in Millisekunden
     */
    fun formatDateWithFullWeekday(timeInMillis: Long): String {
        val cal = AppTimeZone.newCalendar()
        cal.timeInMillis = timeInMillis
        return formatDateWithFullWeekday(cal)
    }
    
    /**
     * Formatiert ein Datum relativ zum heutigen Tag
     * Gibt zurück: "Heute", "Morgen", "Übermorgen", oder formatiertes Datum
     * @param timeInMillis Zeitstempel in Millisekunden
     */
    fun formatDateRelative(timeInMillis: Long): String {
        val today = AppTimeZone.newCalendar()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        
        val target = AppTimeZone.newCalendar()
        target.timeInMillis = timeInMillis
        target.set(Calendar.HOUR_OF_DAY, 0)
        target.set(Calendar.MINUTE, 0)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)
        
        val diff = ((target.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        
        return when (diff) {
            0 -> "Heute"
            1 -> "Morgen"
            2 -> "Übermorgen"
            -1 -> "Gestern"
            else -> formatDateWithWeekday(timeInMillis)
        }
    }
    
    /**
     * Prüft ob ein Datum heute ist
     * @param timeInMillis Zeitstempel in Millisekunden
     */
    fun isToday(timeInMillis: Long): Boolean {
        val today = AppTimeZone.newCalendar()
        val target = AppTimeZone.newCalendar()
        target.timeInMillis = timeInMillis
        
        return today.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Prüft ob ein Datum morgen ist
     * @param timeInMillis Zeitstempel in Millisekunden
     */
    fun isTomorrow(timeInMillis: Long): Boolean {
        val today = AppTimeZone.newCalendar()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val tomorrow = AppTimeZone.newCalendar()
        tomorrow.timeInMillis = today.timeInMillis
        tomorrow.add(Calendar.DAY_OF_YEAR, 1)
        val target = AppTimeZone.newCalendar()
        target.timeInMillis = timeInMillis
        
        return tomorrow.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               tomorrow.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Prüft ob ein Datum in der Vergangenheit liegt
     * @param timeInMillis Zeitstempel in Millisekunden
     */
    fun isPast(timeInMillis: Long): Boolean {
        val today = AppTimeZone.newCalendar()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        
        val target = AppTimeZone.newCalendar()
        target.timeInMillis = timeInMillis
        target.set(Calendar.HOUR_OF_DAY, 0)
        target.set(Calendar.MINUTE, 0)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)
        
        return target.timeInMillis < today.timeInMillis
    }
}
