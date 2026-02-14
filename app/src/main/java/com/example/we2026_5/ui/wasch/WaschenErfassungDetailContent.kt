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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.wasch.WaschErfassung
import androidx.compose.foundation.layout.size
import java.util.Locale
import androidx.compose.foundation.layout.widthIn

@Composable
fun WaschenErfassungDetailContent(
    erfassung: WaschErfassung,
    positionenAnzeige: List<ErfassungPositionAnzeige>,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    onDeleteErfassung: (WaschErfassung) -> Unit
) {
    val datumStr = DateFormatter.formatDate(erfassung.datum)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                "$datumStr ${erfassung.zeit.ifBlank { "-" }}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Button(
                onClick = { onDeleteErfassung(erfassung) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.label_delete), Modifier.size(18.dp))
                Spacer(Modifier.widthIn(4.dp))
                Text(stringResource(R.string.wasch_erfassung_loeschen), fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        for (pos in positionenAnzeige) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(pos.artikelName, modifier = Modifier.fillMaxWidth(), fontSize = 14.sp, color = textPrimary)
                Text("${formatMenge(pos.menge)} ${pos.einheit}", fontSize = 14.sp, color = textSecondary)
            }
        }
    }
}

private fun formatMenge(value: Double): String =
    if (value == value.toLong().toDouble()) "%.0f".format(Locale.GERMAN, value)
    else "%.2f".format(Locale.GERMAN, value).trimEnd('0').trimEnd(',')
