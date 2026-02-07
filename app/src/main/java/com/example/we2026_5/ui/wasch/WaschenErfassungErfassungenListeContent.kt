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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.wasch.WaschErfassung
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn

@Composable
fun WaschenErfassungErfassungenListeContent(
    customer: Customer,
    erfassungen: List<WaschErfassung>,
    primaryBlue: androidx.compose.ui.graphics.Color,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    onBackToKundeSuchen: () -> Unit,
    onNeueErfassungFromListe: () -> Unit,
    onErfassungClick: (WaschErfassung) -> Unit,
    onDeleteErfassung: (WaschErfassung) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                customer.displayName,
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = textPrimary
            )
            IconButton(onClick = onBackToKundeSuchen) {
                Text("↩", fontSize = 18.sp, color = primaryBlue)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.wasch_erfasste_sachen),
            fontSize = 14.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(
            onClick = onNeueErfassungFromListe,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(stringResource(R.string.wasch_neue_erfassung))
        }
        Spacer(Modifier.height(12.dp))
        if (erfassungen.isEmpty()) {
            Text(
                stringResource(R.string.wasch_keine_erfassungen),
                fontSize = 14.sp,
                color = textSecondary,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(erfassungen) { _, e ->
                    val datumStr = DateFormatter.formatDate(e.datum)
                    val zeitStr = if (e.zeit.isNotBlank()) " ${e.zeit}" else ""
                    val count = e.positionen.size
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onErfassungClick(e) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$datumStr$zeitStr", fontSize = 16.sp, color = textPrimary)
                                Spacer(Modifier.widthIn(8.dp))
                                Text(stringResource(R.string.wasch_x_artikel, count), fontSize = 14.sp, color = textSecondary)
                            }
                            IconButton(
                                onClick = { onDeleteErfassung(e) },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.wasch_erfassung_loeschen), tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}
