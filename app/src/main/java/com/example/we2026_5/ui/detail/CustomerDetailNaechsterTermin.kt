package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.we2026_5.ui.common.DetailUiConstants
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.R

@Composable
fun CustomerDetailNaechsterTermin(
    nextTerminMillis: Long,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color
) {
    Text(
        stringResource(R.string.label_next_termin),
        fontSize = DetailUiConstants.FieldLabelSp,
        fontWeight = FontWeight.Bold,
        color = textPrimary
    )
    Text(
        text = if (nextTerminMillis > 0) DateFormatter.formatDateWithWeekday(nextTerminMillis) else stringResource(R.string.label_not_set),
        modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp),
        color = if (nextTerminMillis > 0) textPrimary else textSecondary,
        fontSize = DetailUiConstants.BodySp
    )
    Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
}
