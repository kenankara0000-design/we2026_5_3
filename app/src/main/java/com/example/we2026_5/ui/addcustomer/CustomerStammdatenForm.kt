package com.example.we2026_5.ui.addcustomer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.DetailUiConstants
import com.example.we2026_5.ui.common.ExpandableSection
import com.example.we2026_5.ui.addcustomer.TermineTourFormFields

/**
 * Gemeinsame Formular-Composable für Kunde anlegen und Kunde bearbeiten.
 * Enthält alle Stammdaten-Felder und eine eingeklappte Sektion „Weitere Angaben“.
 */
@Composable
fun CustomerStammdatenForm(
    state: AddCustomerState,
    onUpdate: (AddCustomerState) -> Unit,
    onStartDatumClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    /** Wenn false, werden nur reine Stammdaten (Name, Adresse, Telefon, Notizen) gezeigt; Termine/Tour-Bereich steht dann z. B. im Tab „Termine & Tour“. */
    showTermineTourSection: Boolean = true,
    /** Kundennummer (SevDesk-ID etc.) nur anzeigen, nicht bearbeitbar – grau, read-only. */
    kundennummerReadOnly: Boolean = false
) {
    val context = LocalContext.current
    val textPrimary = Color(ContextCompat.getColor(context, R.color.text_primary))
    val textSecondary = Color(ContextCompat.getColor(context, R.color.text_secondary))
    val spacing = DetailUiConstants.FieldSpacing

    Column(modifier = modifier) {
        OutlinedTextField(
            value = state.name,
            onValueChange = { onUpdate(state.copy(name = it, errorMessage = null)) },
            label = { Text(stringResource(R.string.hint_name_required)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = state.errorMessage != null
        )
        Spacer(modifier = Modifier.height(spacing))
        OutlinedTextField(
            value = state.alias,
            onValueChange = { onUpdate(state.copy(alias = it)) },
            label = { Text(stringResource(R.string.label_customer_alias)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(spacing))
        OutlinedTextField(
            value = state.adresse,
            onValueChange = { onUpdate(state.copy(adresse = it)) },
            label = { Text(stringResource(R.string.hint_address_optional)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(spacing))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = state.plz,
                onValueChange = { onUpdate(state.copy(plz = it)) },
                label = { Text(stringResource(R.string.label_zip_optional)) },
                modifier = Modifier.weight(0.35f),
                singleLine = true
            )
            OutlinedTextField(
                value = state.stadt,
                onValueChange = { onUpdate(state.copy(stadt = it)) },
                label = { Text(stringResource(R.string.label_city_optional)) },
                modifier = Modifier.weight(0.65f),
                singleLine = true
            )
        }
        Spacer(modifier = Modifier.height(spacing))
        OutlinedTextField(
            value = state.telefon,
            onValueChange = { onUpdate(state.copy(telefon = it)) },
            label = { Text(stringResource(R.string.hint_phone_optional)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(spacing))

        // Phase C1: Gemeinsame TermineTourFormFields (Stammdaten + Termine/Tour in einem Formular, z. B. AddCustomer)
        if (showTermineTourSection) {
            TermineTourFormFields(
                state = state,
                onUpdate = onUpdate,
                onStartDatumClick = onStartDatumClick,
                kundennummerReadOnly = kundennummerReadOnly,
                includeCoordinatesInWeitereAngaben = true
            )
        }

        if (!showTermineTourSection) {
            Spacer(modifier = Modifier.height(DetailUiConstants.SectionSpacing))
            ExpandableSection(
                defaultExpanded = false,
                textPrimary = textPrimary
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = when {
                        state.latitude != null && state.longitude != null -> "${state.latitude}, ${state.longitude}"
                        else -> ""
                    },
                    onValueChange = { s ->
                        val parts = s.split(",").map { it.replace(',', '.').trim() }
                        val lat = parts.getOrNull(0)?.toDoubleOrNull()?.takeIf { it in -90.0..90.0 }
                        val lng = parts.getOrNull(1)?.toDoubleOrNull()?.takeIf { it in -180.0..180.0 }
                        onUpdate(state.copy(latitude = lat, longitude = lng))
                    },
                    label = { Text(stringResource(R.string.label_koordinaten)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.hint_koordinaten)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing))
        OutlinedTextField(
            value = state.notizen,
            onValueChange = { onUpdate(state.copy(notizen = it)) },
            label = { Text(stringResource(R.string.hint_notes)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
    }
}
