package com.example.we2026_5.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Unit-Tests für TerminBerechnungUtils (getStartOfDay als Single Source of Truth).
 */
class TerminBerechnungUtilsTest {

    @Test
    fun getStartOfDay_normalizes_to_midnight() {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(2026, Calendar.JANUARY, 15, 14, 30, 45)
        cal.set(Calendar.MILLISECOND, 123)
        val input = cal.timeInMillis
        val result = TerminBerechnungUtils.getStartOfDay(input)
        val resultCal = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = result }
        assertEquals(2026, resultCal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, resultCal.get(Calendar.MONTH))
        assertEquals(15, resultCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, resultCal.get(Calendar.MINUTE))
        assertEquals(0, resultCal.get(Calendar.SECOND))
        assertEquals(0, resultCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun getStartOfDay_same_day_unchanged() {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(2026, Calendar.JANUARY, 20, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val input = cal.timeInMillis
        val result = TerminBerechnungUtils.getStartOfDay(input)
        assertEquals(input, result)
    }
}
