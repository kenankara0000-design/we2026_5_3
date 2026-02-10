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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.we2026_5.KundenTermin
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.DetailUiConstants
import com.example.we2026_5.ui.common.ExpandableSection
import com.example.we2026_5.util.DateFormatter

@Composable
fun CustomerDetailKundenTermineSection(
    kundenTermine: List<KundenTermin>,
    textPrimary: Color,
    textSecondary: Color,
    canDeleteTermin: Boolean,
    onAddAbholungTermin: () -> Unit,
    onDeleteKundenTermin: (List<KundenTermin>) -> Unit,
    /** Wenn false: Add-Button ausgeblendet (zentrale FAB „Neuer Termin“ ersetzt ihn). */
    showAddButton: Boolean = true
) {
    var terminToDelete by rememberSaveable { mutableStateOf<KundenTermin?>(null) }
    var terminLToDeleteWithA by rememberSaveable { mutableStateOf<KundenTermin?>(null) }

    ExpandableSection(
        titleResId = R.string.label_kunden_termine,
        defaultExpanded = false,
        textPrimary = textPrimary
    ) {
        if (showAddButton) {
            Button(
                onClick = onAddAbholungTermin,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text(stringResource(R.string.label_neu_termin), color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
        }
if (kundenTermine.isNotEmpty()) {
            val sorted = kundenTermine.sortedBy { it.datum }
            val aList = sorted.filter { it.typ == "A" }
            val lList = sorted.filter { it.typ == "L" }
            val pairs = aList.zip(lList) // A/L-Paare in einer Zeile
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                pairs.forEach { (aTermin, lTermin) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F0FE))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "A",
                            modifier = Modifier
                                .background(Color(0xFF1976D2).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color(0xFF1976D2),
                            fontSize = DetailUiConstants.BodySp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = DateFormatter.formatDateWithWeekday(aTermin.datum),
                            modifier = Modifier.padding(start = 8.dp, end = 12.dp),
                            color = textPrimary,
                            fontSize = DetailUiConstants.BodySp
                        )
                        Text(
                            text = "·",
                            modifier = Modifier.padding(horizontal = 4.dp),
                            color = textSecondary,
                            fontSize = DetailUiConstants.BodySp
                        )
                        Text(
                            text = "L",
                            modifier = Modifier
                                .background(Color(0xFF388E3C).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color(0xFF388E3C),
                            fontSize = DetailUiConstants.BodySp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = DateFormatter.formatDateWithWeekday(lTermin.datum),
                            modifier = Modifier.padding(start = 8.dp),
                            color = textPrimary,
                            fontSize = DetailUiConstants.BodySp
                        )
                        if (canDeleteTermin) {
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = { terminToDelete = aTermin; terminLToDeleteWithA = lTermin },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.content_desc_delete_kunden_termin),
                                    tint = textPrimary
                                )
                            }
                        }
                    }
                }
                // Ungepaarte A oder L (falls Liste ungleich)
                aList.drop(pairs.size).forEach { termin ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F0FE))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${DateFormatter.formatDateWithWeekday(termin.datum)} – A",
                            modifier = Modifier.weight(1f),
                            color = textPrimary,
                            fontSize = DetailUiConstants.BodySp
                        )
                        if (canDeleteTermin) {
                            IconButton(onClick = { terminToDelete = termin; terminLToDeleteWithA = null }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.content_desc_delete_kunden_termin), tint = textPrimary)
                            }
                        }
                    }
                }
                lList.drop(pairs.size).forEach { termin ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F0FE))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${DateFormatter.formatDateWithWeekday(termin.datum)} – L",
                            modifier = Modifier.weight(1f),
                            color = textPrimary,
                            fontSize = DetailUiConstants.BodySp
                        )
                        if (canDeleteTermin) {
                            IconButton(onClick = { terminToDelete = termin; terminLToDeleteWithA = null }, modifier = Modifier.size(40.dp)) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.content_desc_delete_kunden_termin), tint = textPrimary)
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
            onDismissRequest = { terminToDelete = null; terminLToDeleteWithA = null },
            title = { Text(stringResource(R.string.dialog_kunden_termin_delete_title)) },
            text = { Text(stringResource(R.string.dialog_kunden_termin_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = listOf(termin) + (terminLToDeleteWithA?.let { listOf(it) } ?: emptyList())
                        onDeleteKundenTermin(toDelete)
                        terminToDelete = null
                        terminLToDeleteWithA = null
                    }
                ) {
                    Text(stringResource(R.string.dialog_urlaub_delete_entry), color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { terminToDelete = null; terminLToDeleteWithA = null }) {
                    Text(stringResource(android.R.string.cancel), color = textPrimary)
                }
            }
        )
    }
}
