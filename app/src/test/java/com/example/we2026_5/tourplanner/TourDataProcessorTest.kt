package com.example.we2026_5.tourplanner

import com.example.we2026_5.ListItem
import com.example.we2026_5.SectionType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit-Tests für TourDataProcessor (getFälligCount, processTourData mit leerer Liste).
 */
class TourDataProcessorTest {

    private val processor = TourDataProcessor(TerminCache())

    @Test
    fun getFaelligCount_empty_lists_returns_zero() {
        val count = processor.getFälligCount(
            allCustomers = emptyList(),
            allListen = emptyList(),
            selectedTimestamp = System.currentTimeMillis()
        )
        assertEquals(0, count)
    }

    @Test
    fun processTourData_empty_customers_returns_empty_or_only_headers() {
        val items = processor.processTourData(
            allCustomers = emptyList(),
            allListen = emptyList(),
            selectedTimestamp = System.currentTimeMillis(),
            expandedSections = setOf(SectionType.DONE)
        )
        // Leere Kunden → keine CustomerItems; ggf. leere SectionHeader
        val customerItemCount = items.count { it is ListItem.CustomerItem }
        assertEquals(0, customerItemCount)
    }
}
