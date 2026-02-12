package com.example.we2026_5.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun CustomerDetailBelegeTab(
    customer: Customer,
    belegMonate: List<BelegMonat>,
    textPrimary: Color,
    textSecondary: Color,
    onBelegErstellen: () -> Unit,
    onBelegClick: (BelegMonat) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            stringResource(R.string.tab_belege),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onBelegErstellen,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.beleg_erstellen))
            }
        }
        if (belegMonate.isEmpty()) {
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
                items(belegMonate) { beleg ->
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
