package com.example.we2026_5.ui.listebearbeiten

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.we2026_5.KundenTermin
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.WochentagChipRowFromResources
import com.example.we2026_5.util.DateFormatter

@Composable
fun ListeBearbeitenListenTermineSection(
    listenTermine: List<KundenTermin>,
    wochentagA: Int?,
    tageAzuL: Int,
    surfaceWhite: Color,
    textPrimary: Color,
    textSecondary: Color,
    primaryBlue: Color,
    onWochentagAChange: (Int?) -> Unit,
    onTageAzuLChange: (Int) -> Unit,
    onAddTermin: () -> Unit,
    onDeleteTermine: (List<KundenTermin>) -> Unit
) {
    var terminToDelete by rememberSaveable { mutableStateOf<KundenTermin?>(null) }
    var terminLToDeleteWithA by rememberSaveable { mutableStateOf<KundenTermin?>(null) }
    val context = LocalContext.current
    val textPrimaryColor = Color(ContextCompat.getColor(context, R.color.text_primary))
    val aList = listenTermine.filter { it.typ == "A" }.sortedBy { it.datum }
    val lList = listenTermine.filter { it.typ == "L" }.sortedBy { it.datum }
    val pairs = aList.zip(lList)
    val orphanAs = aList.drop(lList.size)
    val orphanLs = lList.drop(aList.size)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceWhite),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.label_listen_termine),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = primaryBlue
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.label_listen_termine_hint),
                fontSize = 12.sp,
                color = textSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.label_listen_wochentag_a),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            WochentagChipRowFromResources(
                selected = wochentagA?.coerceIn(-1, 6) ?: -1,
                onSelect = { d -> onWochentagAChange(if (d >= 0) d else null) },
                primaryBlue = primaryBlue,
                textPrimary = textPrimaryColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = tageAzuL.toString(),
                    onValueChange = { v ->
                        val n = v.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 365)
                        if (n != null) onTageAzuLChange(n)
                    },
                    modifier = Modifier.width(72.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.label_l_gleich_a_plus, tageAzuL),
                    fontSize = 14.sp,
                    color = textPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.Button(
                onClick = onAddTermin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.label_termine_anlegen))
            }
            if (pairs.isNotEmpty() || orphanAs.isNotEmpty() || orphanLs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    pairs.forEach { (aTermin, lTermin) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F0FE))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("A", modifier = Modifier
                                .background(Color(0xFF1976D2).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color(0xFF1976D2), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(DateFormatter.formatDateWithWeekday(aTermin.datum),
                                modifier = Modifier.padding(start = 8.dp, end = 12.dp),
                                color = textPrimary, fontSize = 13.sp)
                            Text("·", modifier = Modifier.padding(horizontal = 4.dp), color = textSecondary, fontSize = 13.sp)
                            Text("L", modifier = Modifier
                                .background(Color(0xFF388E3C).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color(0xFF388E3C), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(DateFormatter.formatDateWithWeekday(lTermin.datum),
                                modifier = Modifier.padding(start = 8.dp),
                                color = textPrimary, fontSize = 13.sp)
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = { terminToDelete = aTermin; terminLToDeleteWithA = lTermin },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.content_desc_delete_kunden_termin), tint = textPrimary)
                            }
                        }
                    }
                    orphanAs.forEach { aTermin ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F0FE))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("A", modifier = Modifier
                                .background(Color(0xFF1976D2).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color(0xFF1976D2), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(DateFormatter.formatDateWithWeekday(aTermin.datum),
                                modifier = Modifier.padding(start = 8.dp, end = 12.dp),
                                color = textPrimary, fontSize = 13.sp)
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = { terminToDelete = aTermin; terminLToDeleteWithA = null },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.content_desc_delete_kunden_termin), tint = textPrimary)
                            }
                        }
                    }
                    orphanLs.forEach { lTermin ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F0FE))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("L", modifier = Modifier
                                .background(Color(0xFF388E3C).copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Color(0xFF388E3C), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text(DateFormatter.formatDateWithWeekday(lTermin.datum),
                                modifier = Modifier.padding(start = 8.dp, end = 12.dp),
                                color = textPrimary, fontSize = 13.sp)
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                onClick = { terminToDelete = lTermin; terminLToDeleteWithA = null },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.content_desc_delete_kunden_termin), tint = textPrimary)
                            }
                        }
                    }
                }
            }
        }
    }

    terminToDelete?.let { termin ->
        AlertDialog(
            onDismissRequest = { terminToDelete = null; terminLToDeleteWithA = null },
            title = { Text(stringResource(R.string.dialog_listen_termin_delete_title)) },
            text = { Text(stringResource(R.string.dialog_listen_termin_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = listOf(termin) + (terminLToDeleteWithA?.let { listOf(it) } ?: emptyList())
                        onDeleteTermine(toDelete)
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
