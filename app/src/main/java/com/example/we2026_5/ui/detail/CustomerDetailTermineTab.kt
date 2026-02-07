package com.example.we2026_5.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.ui.addcustomer.AddCustomerState
import com.example.we2026_5.ui.common.DetailUiConstants

/**
 * Tab-Inhalt „Termine & Tour“ für Kunden-Detail (Plan Punkt 3).
 * Eigenständige Datei, um CustomerDetailScreen schlank zu halten.
 */
@Composable
fun CustomerDetailTermineTab(
    customer: Customer,
    isInEditMode: Boolean,
    intervalleToShow: List<CustomerIntervall>,
    currentFormState: AddCustomerState,
    onUpdateFormState: (AddCustomerState) -> Unit,
    onStartDatumClick: () -> Unit,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    surfaceWhite: androidx.compose.ui.graphics.Color,
    primaryBlue: androidx.compose.ui.graphics.Color,
    typeLabel: String,
    regelNameByRegelId: Map<String, String>,
    onPauseCustomer: (pauseEndeWochen: Int?) -> Unit,
    onResumeCustomer: () -> Unit,
    onRegelClick: (String) -> Unit,
    onRemoveRegel: ((String) -> Unit)?,
    onResetToAutomatic: () -> Unit,
    onTerminAnlegen: () -> Unit,
    onAddMonthlyClick: (() -> Unit)?,
    tourSlotId: String,
    onAddMonthlyIntervall: ((CustomerIntervall) -> Unit)?,
    showAddMonthlySheet: Boolean,
    onDismissAddMonthlySheet: () -> Unit,
    onConfirmAddMonthly: (CustomerIntervall) -> Unit
) {
    val nextTermin = TerminBerechnungUtils.naechstesFaelligAmDatum(customer)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (isInEditMode) {
            CustomerDetailTermineTourForm(
                state = currentFormState,
                onUpdate = onUpdateFormState,
                onStartDatumClick = onStartDatumClick
            )
            Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
        }
        CustomerDetailStatusSection(
            customer = customer,
            onPauseCustomer = onPauseCustomer,
            onResumeCustomer = onResumeCustomer,
            textPrimary = textPrimary,
            surfaceWhite = surfaceWhite
        )
        Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
        CustomerDetailNaechsterTermin(
            nextTerminMillis = nextTermin,
            textPrimary = textPrimary,
            textSecondary = textSecondary
        )
        if (!isInEditMode) {
            CustomerDetailKundenTypSection(
                typeLabel = typeLabel,
                kundenTyp = customer.kundenTyp,
                effectiveAbholungWochentage = customer.effectiveAbholungWochentage,
                effectiveAuslieferungWochentage = customer.effectiveAuslieferungWochentage,
                textPrimary = textPrimary
            )
        }
        Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
        CustomerDetailTerminRegelCard(
            intervalleToShow = intervalleToShow,
            isInEditMode = isInEditMode,
            kundenTyp = customer.kundenTyp,
            regelNameByRegelId = regelNameByRegelId,
            primaryBlue = primaryBlue,
            textSecondary = textSecondary,
            surfaceWhite = surfaceWhite,
            onRegelClick = onRegelClick,
            onRemoveRegel = onRemoveRegel,
            onResetToAutomatic = onResetToAutomatic,
            onTerminAnlegen = onTerminAnlegen,
            onAddMonthlyClick = onAddMonthlyClick
        )
        AddMonthlyIntervallSheet(
            visible = showAddMonthlySheet,
            tourSlotId = tourSlotId,
            onDismiss = onDismissAddMonthlySheet,
            onAdd = onConfirmAddMonthly
        )
    }
}
