package com.example.we2026_5.ui.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.tourplanner.ErledigungSheetState

/** Argumente für das Erledigungs-Bottom-Sheet (Compose). */
data class ErledigungSheetArgs(
    val customer: Customer,
    val viewDateMillis: Long,
    val state: ErledigungSheetState
)

/** Payload für den Overview-Dialog beim Klick auf eine Kundenkarte. */
data class CustomerOverviewPayload(
    val customer: Customer,
    val urlaubInfo: String?,
    val verschobenInfo: String?,
    val verschobenVonInfo: String?,
    val ueberfaelligInfo: String?
)
