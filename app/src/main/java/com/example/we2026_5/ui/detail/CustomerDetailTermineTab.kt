package com.example.we2026_5.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.R
import com.example.we2026_5.TerminRegelTyp
import com.example.we2026_5.AusnahmeTermin
import com.example.we2026_5.KundenTermin
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.AgentDebugLog
import com.example.we2026_5.ui.addcustomer.AddCustomerState
import com.example.we2026_5.ui.common.DetailUiConstants

/**
 * Tab-Inhalt „Termine & Tour“ für Kunden-Detail (Plan Punkt 3).
 * Eigenständige Datei, um CustomerDetailScreen schlank zu halten.
 */
@Composable
fun CustomerDetailTermineTab(
    isAdmin: Boolean,
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
    onConfirmAddMonthly: (CustomerIntervall) -> Unit,
    onDeleteNextTermin: (Long) -> Unit = {},
    onDeleteAusnahmeTermin: (AusnahmeTermin) -> Unit = {},
    onAddAbholungTermin: () -> Unit = {},
    onDeleteKundenTermin: (List<KundenTermin>) -> Unit = {},
    showNeuerTerminArtSheet: Boolean = false,
    onDismissNeuerTerminArtSheet: () -> Unit = {},
    onNeuerTerminArtSelected: (NeuerTerminArt) -> Unit = {},
    onNeuerTerminClick: () -> Unit = {}
) {
    val nextTermin = TerminBerechnungUtils.naechstesFaelligAmDatum(customer)
    // #region agent log
    val hasMonthlyWeekday = intervalleToShow.any { it.regelTyp == TerminRegelTyp.MONTHLY_WEEKDAY }
    AgentDebugLog.log("CustomerDetailTermineTab.kt", "nextTermin_computed", mapOf("customerId" to customer.id, "nextTermin" to nextTermin, "hasMonthlyWeekday" to hasMonthlyWeekday, "intervalleSize" to customer.intervalle.size), "H6")
    // #endregion
    val canDeleteNextTermin = hasMonthlyWeekday
    val useCentralNeuerTermin = isAdmin
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = if (useCentralNeuerTermin) 88.dp else 0.dp)
        ) {
            if (isInEditMode) {
                CustomerDetailTermineTourForm(
                    state = currentFormState,
                    onUpdate = onUpdateFormState,
                    onStartDatumClick = onStartDatumClick,
                    kundennummerReadOnly = true
                )
                Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
            }
            CustomerDetailStatusSection(
                customer = customer,
                canChangeStatus = isAdmin,
                onPauseCustomer = onPauseCustomer,
                onResumeCustomer = onResumeCustomer,
                textPrimary = textPrimary,
                surfaceWhite = surfaceWhite
            )
            Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
            CustomerDetailNaechsterTermin(
                nextTerminMillis = nextTermin,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                canDeleteNextTermin = canDeleteNextTermin,
                onDeleteNextTermin = { if (nextTermin > 0) onDeleteNextTermin(nextTermin) }
            )
            CustomerDetailKundenTermineSection(
                kundenTermine = customer.kundenTermine,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                canDeleteTermin = isInEditMode,
                onAddAbholungTermin = onAddAbholungTermin,
                onDeleteKundenTermin = onDeleteKundenTermin,
                showAddButton = !useCentralNeuerTermin
            )
            CustomerDetailAusnahmeTermineSection(
                ausnahmeTermine = customer.ausnahmeTermine,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                canDeleteTermin = isInEditMode,
                onDeleteAusnahmeTermin = onDeleteAusnahmeTermin
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
                canTerminAnlegen = isAdmin,
                kundenTyp = customer.kundenTyp,
                regelNameByRegelId = regelNameByRegelId,
                primaryBlue = primaryBlue,
                textSecondary = textSecondary,
                surfaceWhite = surfaceWhite,
                onRegelClick = onRegelClick,
                onRemoveRegel = onRemoveRegel,
                onResetToAutomatic = onResetToAutomatic,
                onTerminAnlegen = onTerminAnlegen,
                onAddMonthlyClick = onAddMonthlyClick,
                useCentralNeuerTerminButton = useCentralNeuerTermin
            )
            AddMonthlyIntervallSheet(
                visible = showAddMonthlySheet,
                tourSlotId = tourSlotId,
                onDismiss = onDismissAddMonthlySheet,
                onAdd = onConfirmAddMonthly
            )
            NeuerTerminArtSheet(
                visible = showNeuerTerminArtSheet,
                onDismiss = onDismissNeuerTerminArtSheet,
                onArtSelected = onNeuerTerminArtSelected
            )
        }
        if (useCentralNeuerTermin) {
            FloatingActionButton(
                onClick = onNeuerTerminClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                contentColor = androidx.compose.ui.graphics.Color.White,
                containerColor = primaryBlue
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.label_neuer_termin_anlegen)
                )
            }
        }
    }
}
