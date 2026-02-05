package com.example.we2026_5.ui.addcustomer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.WochentagChipRowFromResources

@Composable
internal fun AddCustomerRadioOption(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit,
    textPrimary: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { onSelect() }
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text, color = textPrimary, fontSize = 14.sp, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddCustomerTageAzuLField(
    value: Int,
    onValueChange: (Int) -> Unit,
    textPrimary: Color
) {
    Column {
        Text(
            text = stringResource(R.string.label_l_termin),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = if (value in 0..365) value.toString() else "",
            onValueChange = { s ->
                val digits = s.filter { it.isDigit() }
                if (digits.isEmpty()) onValueChange(7)
                else digits.toIntOrNull()?.coerceIn(0, 365)?.let { onValueChange(it) }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("z.B. 7") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = { Text("0–365 Tage", color = textPrimary.copy(alpha = 0.7f)) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddCustomerIntervallSchnellauswahl(
    selected: Int,
    onSelect: (Int) -> Unit,
    textPrimary: Color,
    labelResId: Int = R.string.label_intervall
) {
    val preset = listOf(7, 14, 21, 28)
    Column {
        Text(
            text = stringResource(labelResId),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = if (selected in 1..365) selected.toString() else "",
            onValueChange = { s ->
                val digits = s.filter { it.isDigit() }
                if (digits.isEmpty()) onSelect(7)
                else digits.toIntOrNull()?.coerceIn(1, 365)?.let { onSelect(it) }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("z.B. 7, 10, 14, 21 …") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = { Text("1–365 Tage", color = textPrimary.copy(alpha = 0.7f)) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            preset.forEach { tage ->
                FilterChip(
                    selected = selected == tage,
                    onClick = { onSelect(tage) },
                    label = { Text("$tage Tage") }
                )
            }
        }
    }
}

@Composable
internal fun AddCustomerWeekdaySelector(
    label: String,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val context = LocalContext.current
    val textPrimary = Color(ContextCompat.getColor(context, R.color.text_primary))
    val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
    Column {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        WochentagChipRowFromResources(
            selected = selected.coerceIn(-1, 6),
            onSelect = onSelect,
            primaryBlue = primaryBlue,
            textPrimary = textPrimary
        )
    }
}
