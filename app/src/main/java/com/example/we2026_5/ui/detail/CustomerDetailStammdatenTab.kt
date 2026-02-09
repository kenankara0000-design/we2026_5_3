package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.ui.addcustomer.AddCustomerState
import com.example.we2026_5.ui.addcustomer.CustomerStammdatenForm
import com.example.we2026_5.ui.common.DetailUiConstants

/**
 * Tab-Inhalt „Stammdaten“ für Kunden-Detail (Plan Punkt 3).
 * Eigenständige Datei, um CustomerDetailScreen schlank zu halten.
 */
@Composable
fun CustomerDetailStammdatenTab(
    isAdmin: Boolean,
    customer: Customer,
    isInEditMode: Boolean,
    currentFormState: AddCustomerState,
    onUpdateFormState: (AddCustomerState) -> Unit,
    primaryBlue: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    onUrlaub: () -> Unit,
    onEdit: () -> Unit,
    onAdresseClick: () -> Unit,
    onTelefonClick: () -> Unit,
    onErfassungClick: () -> Unit,
    onTakePhoto: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onDeletePhoto: ((String) -> Unit)? = null,
    isUploading: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (!isInEditMode) {
            if (isAdmin) {
                CustomerDetailActionsRow(
                    primaryBlue = primaryBlue,
                    onUrlaub = onUrlaub,
                    onEdit = onEdit,
                    isUploading = isUploading
                )
                Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
            }
            Text(
                stringResource(R.string.label_address_label),
                fontSize = DetailUiConstants.FieldLabelSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            val fullAddress = buildString {
                if (customer.adresse.isNotBlank()) append(customer.adresse.trim())
                val plzStadt = listOf(customer.plz.trim(), customer.stadt.trim()).filter { it.isNotEmpty() }.joinToString(" ")
                if (plzStadt.isNotEmpty()) {
                    if (isNotEmpty()) append(", ")
                    append(plzStadt)
                }
            }.trim()
            Text(
                text = fullAddress.ifEmpty { stringResource(R.string.label_not_set) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0E0E0))
                    .padding(12.dp)
                    .clickable(onClick = onAdresseClick),
                color = if (fullAddress.isNotEmpty()) textPrimary else textSecondary,
                fontSize = DetailUiConstants.BodySp
            )
            Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
            Text(
                stringResource(R.string.label_phone_label),
                fontSize = DetailUiConstants.FieldLabelSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                text = customer.telefon.ifEmpty { stringResource(R.string.label_not_set) },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0E0E0))
                    .padding(12.dp)
                    .clickable(onClick = onTelefonClick),
                color = if (customer.telefon.isNotEmpty()) textPrimary else textSecondary,
                fontSize = DetailUiConstants.BodySp
            )
            Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
            Text(
                stringResource(R.string.label_notes_label),
                fontSize = DetailUiConstants.FieldLabelSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                text = customer.notizen.ifEmpty { stringResource(R.string.label_not_set) },
                modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp),
                color = if (customer.notizen.isNotEmpty()) textPrimary else textSecondary,
                fontSize = DetailUiConstants.BodySp
            )
        } else if (isAdmin) {
            CustomerStammdatenForm(
                state = currentFormState,
                onUpdate = onUpdateFormState,
                onStartDatumClick = { },
                showTermineTourSection = false,
                kundennummerReadOnly = true
            )
            Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
            Text(
                stringResource(R.string.label_urlaub),
                fontSize = DetailUiConstants.FieldLabelSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Button(
                onClick = onUrlaub,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = colorResource(R.color.button_urlaub)
                )
            ) {
                Text(stringResource(R.string.label_urlaub))
            }
            Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
            Text(
                stringResource(R.string.wasch_erfassungen),
                fontSize = DetailUiConstants.FieldLabelSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            OutlinedButton(onClick = onErfassungClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.wasch_artikel_hinzufuegen))
            }
            Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.label_foto_uploading),
                    fontSize = 12.sp,
                    color = primaryBlue
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
        CustomerDetailFotosSection(
            fotoUrls = customer.fotoUrls,
            fotoThumbUrls = customer.fotoThumbUrls,
            isInEditMode = isInEditMode,
            textPrimary = textPrimary,
            onPhotoClick = onPhotoClick,
            onTakePhoto = onTakePhoto,
            onDeletePhoto = onDeletePhoto
        )
    }
}
