package com.example.we2026_5.tourplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.ui.theme.AppColors
import com.example.we2026_5.ui.theme.AppSpacing
import com.example.we2026_5.util.DateFormatter

/**
 * Compose-Dialog für Termin-Details im TourPlanner.
 * Ersetzt dialog_termin_detail.xml und TourPlannerDialogHelper.
 */
@Composable
fun TerminDetailDialog(
    customer: Customer,
    terminDatum: Long,
    onDismiss: () -> Unit,
    onKundeAnzeigen: () -> Unit,
    onTerminLoeschen: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = AppColors.SurfaceWhite,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = stringResource(R.string.label_termin_detail),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.PrimaryBlueDark,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Kundenname mit Typ-Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Kunde-Typ Button (G/P/L) wird hier nur als Text angezeigt
                    // Um die volle Logik von CustomerTypeButtonHelper zu nutzen,
                    // müsste man eine Compose-Version erstellen
                    val typeText = when (customer.kundenArt) {
                        "Geschäftskunden" -> "G"
                        "Privatkunden" -> "P"
                        "Listenkunden" -> "L"
                        else -> "?"
                    }
                    val typeColor = when (customer.kundenArt) {
                        "Geschäftskunden", "Gewerblich" -> AppColors.ButtonGewerblichGlossy
                        "Privatkunden", "Privat" -> AppColors.ButtonPrivatGlossy
                        "Listenkunden" -> AppColors.ButtonListeGlossy
                        else -> AppColors.PrimaryBlue
                    }
                    Text(
                        text = typeText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier
                            .background(
                                typeColor,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )

                    Text(
                        text = customer.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.PrimaryBlueDark,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Adresse
                InfoRow(
                    label = stringResource(R.string.label_address_label),
                    value = customer.adresse
                )

                // Telefon
                InfoRow(
                    label = stringResource(R.string.label_phone_label),
                    value = customer.telefon
                )

                // Termin-Datum
                InfoRow(
                    label = stringResource(R.string.label_termin),
                    value = DateFormatter.formatDate(terminDatum)
                )

                // Notizen
                Column(
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_notes_label),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = customer.notizen.ifEmpty { stringResource(R.string.label_no_notes) },
                        fontSize = 14.sp,
                        color = AppColors.TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.SurfaceLight, MaterialTheme.shapes.small)
                            .padding(8.dp)
                            .heightIn(min = 48.dp)
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onKundeAnzeigen,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.ButtonBlue
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_show_customer),
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }

                    Button(
                        onClick = onTerminLoeschen,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.ButtonActive
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.label_delete_termin),
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextSecondary,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = AppColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
