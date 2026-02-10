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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import com.example.we2026_5.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaschenErfassungAlleBelegeContent(
    belegEintraege: List<BelegEintrag>,
    nameFilter: String,
    onNameFilterChange: (String) -> Unit,
    showErledigtTab: Boolean = false,
    onShowErledigtTabChange: (Boolean) -> Unit = {},
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    onBelegEintragClick: (BelegEintrag) -> Unit
) {
    val filtered = belegEintraege.filter {
        nameFilter.isBlank() || it.customer.displayName.contains(nameFilter, ignoreCase = true)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = nameFilter,
            onValueChange = onNameFilterChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.wasch_belege_filter_kunde), color = textSecondary) },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
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
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.wasch_belege),
            fontSize = 14.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (filtered.isEmpty()) {
            Text(
                stringResource(R.string.wasch_keine_belege),
                fontSize = 14.sp,
                color = textSecondary,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered) { eintrag ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBelegEintragClick(eintrag) },
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    eintrag.customer.displayName,
                                    fontSize = 16.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    color = textPrimary
                                )
                                Text(
                                    eintrag.beleg.monthLabel,
                                    fontSize = 14.sp,
                                    color = textSecondary
                                )
                            }
                            Text(
                                stringResource(R.string.wasch_x_erfassungen, eintrag.beleg.erfassungen.size),
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
