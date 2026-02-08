package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.wasch.Article
import com.example.we2026_5.wasch.WaschErfassung
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn

@Composable
fun WaschenErfassungBelegDetailContent(
    customerName: String,
    monthLabel: String,
    erfassungen: List<WaschErfassung>,
    articlesMap: Map<String, Article>,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    onBack: () -> Unit,
    onDeleteErfassung: (WaschErfassung) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            customerName,
            fontSize = 16.sp,
            color = textSecondary
        )
        Text(
            monthLabel,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        erfassungen.forEach { erfassung ->
            val datumStr = DateFormatter.formatDate(erfassung.datum)
            val zeitStr = erfassung.zeit.ifBlank { "-" }
            val positionenAnzeige = erfassung.positionen.map { pos ->
                val name = articlesMap[pos.articleId]?.name ?: pos.articleId
                ErfassungPositionAnzeige(name, pos.menge, pos.einheit.ifBlank { "Stk" })
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            "$datumStr $zeitStr",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Button(
                            onClick = { onDeleteErfassung(erfassung) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, Modifier.size(18.dp))
                            Spacer(Modifier.widthIn(4.dp))
                            Text(stringResource(R.string.wasch_erfassung_loeschen), fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    positionenAnzeige.forEach { pos ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(pos.artikelName, fontSize = 14.sp, color = textPrimary, modifier = Modifier.weight(1f))
                            Text("${pos.menge} ${pos.einheit}", fontSize = 14.sp, color = textSecondary)
                        }
                    }
                }
            }
        }
    }
}
