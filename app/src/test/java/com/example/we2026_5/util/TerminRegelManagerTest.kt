package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerStatus
import com.example.we2026_5.TourSlot
import com.example.we2026_5.Zeitfenster
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            regel = null,
            tourSlots = tourSlots,
            startDatum = System.currentTimeMillis(),
            tageVoraus = 7
        )

        assertTrue(result.isNotEmpty(), "Es sollen Slot-Vorschläge erzeugt werden.")
        assertEquals("c1", result.first().customerId)
        assertEquals("Test Kunde", result.first().customerName)
    }
}
