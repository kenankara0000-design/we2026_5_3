package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.AusnahmeTermin
import com.example.we2026_5.R
import com.example.we2026_5.ui.theme.AppColors
import com.example.we2026_5.ui.common.DetailUiConstants
import com.example.we2026_5.ui.common.ExpandableSection
import com.example.we2026_5.util.DateFormatter

@Composable
fun CustomerDetailAusnahmeTermineSection(
    ausnahmeTermine: List<AusnahmeTermin>,
    textPrimary: Color,
    textSecondary: Color,
    canDeleteTermin: Boolean,
    onDeleteAusnahmeTermin: (AusnahmeTermin) -> Unit
) {
    var terminToDelete by rememberSaveable { mutableStateOf<AusnahmeTermin?>(null) }

    ExpandableSection(
        titleResId = R.string.label_ausnahme_termine,
        defaultExpanded = false,
        textPrimary = textPrimary
    ) {
        if (ausnahmeTermine.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ausnahmeTermine.sortedBy { it.datum }.forEach { termin ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AppColors.LightGray)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val typLabel = if (termin.typ == "A") stringResource(R.string.termin_anlegen_ausnahme_abholung)
                        else stringResource(R.string.termin_anlegen_ausnahme_auslieferung)
                        Text(
                            text = "${DateFormatter.formatDateWithWeekday(termin.datum)} – $typLabel",
                            modifier = Modifier.weight(1f),
                            color = textPrimary,
                            fontSize = 15.sp
                        )
                        if (canDeleteTermin) {
                            IconButton(
                                onClick = { terminToDelete = termin },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.content_desc_delete_ausnahme_termin),
                                    tint = textPrimary
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
        }
    }

    terminToDelete?.let { termin ->
        AlertDialog(
            onDismissRequest = { terminToDelete = null },
            title = { Text(stringResource(R.string.dialog_ausnahme_termin_delete_title)) },
            text = { Text(stringResource(R.string.dialog_ausnahme_termin_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAusnahmeTermin(termin)
                        terminToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.dialog_urlaub_delete_entry), color = AppColors.ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { terminToDelete = null }) {
                    Text(stringResource(android.R.string.cancel), color = textPrimary)
                }
            }
        )
    }
}
