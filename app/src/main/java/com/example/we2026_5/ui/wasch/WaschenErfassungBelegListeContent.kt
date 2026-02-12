package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaschenErfassungBelegListeContent(
    customer: Customer,
    belege: List<BelegMonat>,
    showErledigtTab: Boolean = false,
    onShowErledigtTabChange: (Boolean) -> Unit = {},
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    onBackToKundeSuchen: () -> Unit,
    onNeueErfassungFromListe: () -> Unit,
    onWaeschelisteFormularFromListe: () -> Unit = {},
    onBelegClick: (BelegMonat) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            customer.displayName,
            fontSize = 18.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = textPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.wasch_belege),
            fontSize = 14.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !showErledigtTab,
                onClick = { onShowErledigtTabChange(false) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text(stringResource(R.string.beleg_tab_offen))
            }
            SegmentedButton(
                selected = showErledigtTab,
                onClick = { onShowErledigtTabChange(true) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text(stringResource(R.string.beleg_tab_erledigt))
            }
        }
        Spacer(Modifier.height(8.dp))
        if (!showErledigtTab) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNeueErfassungFromListe,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_manuell_erfassen))
                }
                Button(
                    onClick = onWaeschelisteFormularFromListe,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.btn_waescheliste_formular))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        if (belege.isEmpty()) {
            Text(
                stringResource(R.string.wasch_keine_erfassungen),
                fontSize = 14.sp,
                color = textSecondary,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
}
