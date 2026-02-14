package com.example.we2026_5.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.we2026_5.ui.addcustomer.AddCustomerState
import com.example.we2026_5.ui.addcustomer.TermineTourFormFields

/**
 * Nur der Termin- und Tour-Bereich (Kundenart, T.-Typ, Startdatum, Ohne Tour, Intervall, L-Termin, Abhol-/Auslieferungstage, Weitere Angaben).
 * Wird im Tab „Termine & Tour“ angezeigt (Kunden-Detail Bearbeitung).
 * Phase C1: Delegation an gemeinsame [TermineTourFormFields].
 */
@Composable
fun CustomerDetailTermineTourForm(
    state: AddCustomerState,
    onUpdate: (AddCustomerState) -> Unit,
    onStartDatumClick: () -> Unit,
    /** Kundennummer (SevDesk-ID etc.) nur anzeigen, nicht bearbeitbar – grau, read-only. */
    kundennummerReadOnly: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TermineTourFormFields(
            state = state,
            onUpdate = onUpdate,
            onStartDatumClick = onStartDatumClick,
            kundennummerReadOnly = kundennummerReadOnly,
            includeCoordinatesInWeitereAngaben = false
        )
    }
}
