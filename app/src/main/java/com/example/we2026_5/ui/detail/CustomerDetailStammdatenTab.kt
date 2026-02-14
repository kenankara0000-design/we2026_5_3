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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.ui.theme.AppColors
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
    onAdresseClick: () -> Unit,
    onTelefonClick: () -> Unit,
    onTakePhoto: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onDeletePhoto: ((String) -> Unit)? = null,
    isUploading: Boolean = false,
    /** Phase 4: Zeigt Überfällig-Hinweis am Anfang des Tabs. */
    isOverdue: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Phase 4: Überfällig-Hinweis
        if (isOverdue && !isInEditMode) {
            val overdueColor = colorResource(R.color.status_overdue)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(overdueColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = stringResource(R.string.content_desc_overdue),
                    tint = overdueColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.customer_overdue_hint),
                    color = overdueColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
        }
        if (!isInEditMode) {
            if (isUploading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    stringResource(R.string.label_foto_uploading),
                    fontSize = 12.sp,
                    color = primaryBlue
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.LightGray)
                    .padding(12.dp)
                    .clickable(onClick = onAdresseClick),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = fullAddress.ifEmpty { stringResource(R.string.label_not_set) },
                    color = if (fullAddress.isNotEmpty()) textPrimary else textSecondary,
                    fontSize = DetailUiConstants.BodySp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = stringResource(R.string.content_desc_navigation),
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
            Text(
                stringResource(R.string.label_phone_label),
                fontSize = DetailUiConstants.FieldLabelSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.LightGray)
                    .padding(12.dp)
                    .clickable(onClick = onTelefonClick),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = customer.telefon.ifEmpty { stringResource(R.string.label_not_set) },
                    color = if (customer.telefon.isNotEmpty()) textPrimary else textSecondary,
                    fontSize = DetailUiConstants.BodySp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = stringResource(R.string.label_phone_label),
                    tint = textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
            Text(
                stringResource(R.string.label_notes_label),
                fontSize = DetailUiConstants.FieldLabelSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Text(
                text = customer.notizen.ifEmpty { stringResource(R.string.label_not_set) },
                modifier = Modifier.fillMaxWidth().background(AppColors.LightGray).padding(12.dp),
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

        // Pending-Uploads aus StorageUploadManager abfragen
        val pendingUploads by com.example.we2026_5.util.StorageUploadManager
            .pendingUploadCount
            .collectAsState()

        CustomerDetailFotosSection(
            fotoUrls = customer.fotoUrls,
            fotoThumbUrls = customer.fotoThumbUrls,
            isInEditMode = isInEditMode,
            textPrimary = textPrimary,
            onPhotoClick = onPhotoClick,
            onTakePhoto = onTakePhoto,
            onDeletePhoto = onDeletePhoto,
            pendingUploadCount = pendingUploads
        )
    }
}
