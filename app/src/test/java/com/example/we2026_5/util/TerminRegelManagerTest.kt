package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerStatus
import com.example.we2026_5.TourSlot
import com.example.we2026_5.Zeitfenster
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminRegelManagerTest {

    @Test
    fun schlageSlotsVor_returnsSuggestions_forAdHocCustomer() {
        val customer = Customer(
            id = "c1",
            name = "Test Kunde",
            stadt = "Leipzig",
            defaultAbholungWochentag = 1,
            defaultAuslieferungWochentag = 2,
            status = CustomerStatus.ADHOC
        )
        val tourSlots = listOf(
            TourSlot(
                id = "slot1",
                wochentag = 1,
                stadt = "Leipzig",
                zeitfenster = Zeitfenster("09:00", "11:00")
            )
        )

        val result = TerminRegelManager.schlageSlotsVor(
            kunde = customer,
            tourSlots = tourSlots,
            startDatum = System.currentTimeMillis(),
            tageVoraus = 7
        )

        assertTrue("Es sollen Slot-Vorschläge erzeugt werden.", result.isNotEmpty())
        assertEquals("c1", result.first().customerId)
        assertEquals("Test Kunde", result.first().customerName)
    }
}
