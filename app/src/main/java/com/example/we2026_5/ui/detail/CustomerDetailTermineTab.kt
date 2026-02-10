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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.AusnahmeTermin
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.KundenTermin
import com.example.we2026_5.R
import com.example.we2026_5.TerminRegelTyp
import com.example.we2026_5.ui.addcustomer.AddCustomerState
import com.example.we2026_5.ui.common.DetailUiConstants
import com.example.we2026_5.util.TerminBerechnungUtils

/**
 * Tab-Inhalt „Termine & Tour“ für Kunden-Detail.
 * Eine Überschrift „Alle Termine“, scrollbare Liste (max. 6 Zeilen sichtbar), „+ Termin“-Chip.
 */
@Composable
fun CustomerDetailTermineTab(
    isAdmin: Boolean,
    customer: Customer,
    isInEditMode: Boolean,
    currentFormState: AddCustomerState,
    onUpdateFormState: (AddCustomerState) -> Unit,
    onStartDatumClick: () -> Unit,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    surfaceWhite: androidx.compose.ui.graphics.Color,
    primaryBlue: androidx.compose.ui.graphics.Color,
    onPauseCustomer: (pauseEndeWochen: Int?) -> Unit,
    onResumeCustomer: () -> Unit,
    terminePairs365: List<Pair<Long, Long>>,
    showAddMonthlySheet: Boolean = false,
    onDismissAddMonthlySheet: () -> Unit = {},
    onConfirmAddMonthly: (CustomerIntervall) -> Unit = {},
    tourSlotId: String = "",
    showNeuerTerminArtSheet: Boolean = false,
    onDismissNeuerTerminArtSheet: () -> Unit = {},
    onNeuerTerminArtSelected: (NeuerTerminArt) -> Unit = {},
    onNeuerTerminClick: () -> Unit = {},
    tourListenName: String? = null,
    typeLabel: String = "",
    onDeleteNextTermin: (Long) -> Unit = {},
    ausnahmeTermine: List<AusnahmeTermin> = emptyList(),
    onDeleteAusnahmeTermin: (AusnahmeTermin) -> Unit = {},
    kundenTermine: List<KundenTermin> = emptyList(),
    onAddAbholungTermin: () -> Unit = {},
    onDeleteKundenTermin: (List<KundenTermin>) -> Unit = {}
) {
    val useCentralNeuerTermin = isAdmin
    val nextTermin = TerminBerechnungUtils.naechstesFaelligAmDatum(customer)
    val canDeleteNextTermin = customer.intervalle.any { it.regelTyp == TerminRegelTyp.MONTHLY_WEEKDAY }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
            if (tourListenName != null) {
                Text(
                    text = stringResource(R.string.label_gehoert_zu_tour_liste, tourListenName),
                    fontSize = 14.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
            }
            CustomerDetailStatusSection(
                customer = customer,
                canChangeStatus = isAdmin,
                onPauseCustomer = onPauseCustomer,
                onResumeCustomer = onResumeCustomer,
                textPrimary = textPrimary,
                surfaceWhite = surfaceWhite,
                trailingContent = if (useCentralNeuerTermin) {
                    {
                        Button(
                            onClick = onNeuerTerminClick,
                            colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                        ) {
                            Text(stringResource(R.string.label_plus_termin), color = androidx.compose.ui.graphics.Color.White)
                        }
                    }
                } else null
            )
            Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
            CustomerDetailNaechsterTermin(
                nextTerminMillis = nextTermin,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                canDeleteNextTermin = canDeleteNextTermin,
                onDeleteNextTermin = { if (nextTermin > 0) onDeleteNextTermin(nextTermin) }
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
            CustomerDetailAusnahmeTermineSection(
                ausnahmeTermine = ausnahmeTermine,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                canDeleteTermin = isAdmin,
                onDeleteAusnahmeTermin = onDeleteAusnahmeTermin
            )
            Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
            CustomerDetailKundenTermineSection(
                kundenTermine = kundenTermine,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                canDeleteTermin = isAdmin,
                onAddAbholungTermin = onAddAbholungTermin,
                onDeleteKundenTermin = onDeleteKundenTermin,
                showAddButton = !useCentralNeuerTermin
            )
            Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
            AlleTermineBlock(
                pairs = terminePairs365,
                textPrimary = textPrimary,
                textSecondary = textSecondary
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
                onArtSelected = onNeuerTerminArtSelected,
                kundenTyp = customer.kundenTyp
            )
        }
    }
}
