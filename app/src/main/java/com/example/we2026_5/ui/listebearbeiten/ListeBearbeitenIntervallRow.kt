package com.example.we2026_5.ui.listebearbeiten

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter

@Composable
internal fun ListeBearbeitenIntervallRow(
    intervall: ListeIntervall,
    isEditMode: Boolean,
    onAbholungClick: () -> Unit,
    onAuslieferungClick: () -> Unit
) {
    val abholungText = if (intervall.abholungDatum > 0) DateFormatter.formatDateWithLeadingZeros(intervall.abholungDatum) else stringResource(R.string.label_not_set)
    val auslieferungText = if (intervall.auslieferungDatum > 0) DateFormatter.formatDateWithLeadingZeros(intervall.auslieferungDatum) else stringResource(R.string.label_not_set)
    val textSecondary = Color(ContextCompat.getColor(LocalContext.current, R.color.text_secondary))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isEditMode) Modifier.clickable(onClick = {}) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = if (isEditMode) Modifier.clickable(onClick = onAbholungClick) else Modifier) {
            Text(stringResource(R.string.label_abholung_date), fontSize = 12.sp, color = textSecondary)
            Text(abholungText, fontSize = 14.sp)
        }
        Column(modifier = if (isEditMode) Modifier.clickable(onClick = onAuslieferungClick) else Modifier) {
            Text(stringResource(R.string.label_auslieferung_date), fontSize = 12.sp, color = textSecondary)
            Text(auslieferungText, fontSize = 14.sp)
        }
    }
}
