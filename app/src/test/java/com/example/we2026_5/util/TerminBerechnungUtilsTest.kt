package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.TerminTyp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Unit-Tests für TerminBerechnungUtils (getStartOfDay; berechneAlleTermineFuerKunde mit liste=null).
 */
class TerminBerechnungUtilsTest {

    @Test
    fun berechneAlleTermineFuerKunde_mitIntervalle_listeNull_berechnetTermine() {
        val cal = AppTimeZone.newCalendar()
        cal.set(2026, Calendar.JANUARY, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val abholung = start
        val auslieferung = start + TimeUnit.DAYS.toMillis(7)
        val customer = Customer(
            id = "test-1",
            name = "Test",
            intervalle = listOf(
                CustomerIntervall(
                    id = "iv-1",
                    abholungDatum = abholung,
                    auslieferungDatum = auslieferung,
                    wiederholen = false
                )
            )
        )
        val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
            customer = customer,
            liste = null,
            startDatum = start,
            tageVoraus = 30
        )
        assertTrue(termine.any { it.typ == TerminTyp.ABHOLUNG })
        assertTrue(termine.any { it.typ == TerminTyp.AUSLIEFERUNG })
    }

    @Test
    fun getStartOfDay_normalizes_to_midnight() {
        val cal = AppTimeZone.newCalendar()
        cal.set(2026, Calendar.JANUARY, 15, 14, 30, 45)
        cal.set(Calendar.MILLISECOND, 123)
        val input = cal.timeInMillis
        val result = TerminBerechnungUtils.getStartOfDay(input)
        val resultCal = AppTimeZone.newCalendar().apply { timeInMillis = result }
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
        val cal = AppTimeZone.newCalendar()
        cal.set(2026, Calendar.JANUARY, 20, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val input = cal.timeInMillis
        val result = TerminBerechnungUtils.getStartOfDay(input)
        assertEquals(input, result)
    }
}
