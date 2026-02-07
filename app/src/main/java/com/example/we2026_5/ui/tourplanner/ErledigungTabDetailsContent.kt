package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter

@Composable
fun ErledigungTabDetailsContent(
    customer: Customer,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    getNaechstesTourDatum: (Customer) -> Long?,
    onTelefonClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (customer.telefon.isNotBlank()) {
            Text(stringResource(R.string.sheet_telefon), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary)
            Text(
                text = customer.telefon,
                fontSize = 15.sp,
                color = textPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clickable { onTelefonClick(customer.telefon.trim()) }
            )
        }
        Text(stringResource(R.string.sheet_naechste_tour), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary)
        val naechsteTour = getNaechstesTourDatum(customer)
        Text(
            text = if (naechsteTour != null && naechsteTour > 0) DateFormatter.formatDate(naechsteTour) else stringResource(R.string.sheet_kein_termin),
            fontSize = 15.sp,
            color = textPrimary,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .then(Modifier.fillMaxWidth())
        )
        if (customer.notizen.isNotBlank()) {
            Text(stringResource(R.string.sheet_notizen), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary)
            Text(text = customer.notizen, fontSize = 15.sp, color = textPrimary, modifier = Modifier.fillMaxWidth())
        }
    }
}
