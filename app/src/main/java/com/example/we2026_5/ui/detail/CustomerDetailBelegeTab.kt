package com.example.we2026_5.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.ui.wasch.BelegMonat
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailBelegeTab(
    customer: Customer,
    belegMonate: List<BelegMonat>,
    belegMonateErledigt: List<BelegMonat>,
    textPrimary: Color,
    textSecondary: Color,
    onNeueErfassungKameraFoto: () -> Unit,
    onNeueErfassungFormular: () -> Unit,
    onNeueErfassungManuell: () -> Unit,
    onBelegClick: (BelegMonat) -> Unit
) {
    var showErledigtTab by remember { mutableStateOf(false) }
    val showNeueErfassungDialog = remember { mutableStateOf(false) }
    val belege = if (showErledigtTab) belegMonateErledigt else belegMonate
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            stringResource(R.string.tab_belege),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !showErledigtTab,
                onClick = { showErledigtTab = false },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(stringResource(R.string.beleg_tab_offen))
            }
            SegmentedButton(
                selected = showErledigtTab,
                onClick = { showErledigtTab = true },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(stringResource(R.string.beleg_tab_erledigt))
            }
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            if (belege.isEmpty()) {
                Text(
                    stringResource(R.string.wasch_keine_erfassungen),
                    fontSize = 14.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    items(belege) { beleg ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onBelegClick(beleg) },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    beleg.monthLabel,
                                    fontSize = 16.sp,
                                    color = textPrimary
                                )
                                Text(
                                    stringResource(R.string.wasch_x_erfassungen, beleg.erfassungen.size),
                                    fontSize = 14.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
        Button(
            onClick = { showNeueErfassungDialog.value = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.wasch_btn_neue_erfassung))
        }
    }

    if (showNeueErfassungDialog.value) {
        AlertDialog(
            onDismissRequest = { showNeueErfassungDialog.value = false },
            title = { Text(stringResource(R.string.wasch_btn_neue_erfassung), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showNeueErfassungDialog.value = false
                            onNeueErfassungKameraFoto()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(stringResource(R.string.btn_kamera_foto)) }
                    Button(
                        onClick = {
                            showNeueErfassungDialog.value = false
                            onNeueErfassungFormular()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(stringResource(R.string.btn_waescheliste_formular)) }
                    Button(
                        onClick = {
                            showNeueErfassungDialog.value = false
                            onNeueErfassungManuell()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(stringResource(R.string.btn_manuell_erfassen)) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showNeueErfassungDialog.value = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
