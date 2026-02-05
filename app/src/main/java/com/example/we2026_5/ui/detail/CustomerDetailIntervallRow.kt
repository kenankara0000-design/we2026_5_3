package com.example.we2026_5.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.ui.common.DetailUiConstants

@Composable
fun CustomerDetailIntervallRow(
    intervall: CustomerIntervall,
    isEditMode: Boolean,
    onAbholungClick: () -> Unit,
    onAuslieferungClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val abholungText = if (intervall.abholungDatum > 0) DateFormatter.formatDateWithLeadingZeros(intervall.abholungDatum) else stringResource(R.string.label_not_set)
    val auslieferungText = if (intervall.auslieferungDatum > 0) DateFormatter.formatDateWithLeadingZeros(intervall.auslieferungDatum) else stringResource(R.string.label_not_set)
    val abholungIsPlaceholder = intervall.abholungDatum <= 0
    val auslieferungIsPlaceholder = intervall.auslieferungDatum <= 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = if (isEditMode) Modifier.clickable(onClick = onAbholungClick) else Modifier) {
                Text(stringResource(R.string.label_abholung_date), fontSize = 12.sp, color = textSecondary)
                Text(abholungText, fontSize = DetailUiConstants.BodySp, color = if (abholungIsPlaceholder) textSecondary else textPrimary)
            }
            Column(modifier = if (isEditMode) Modifier.clickable(onClick = onAuslieferungClick) else Modifier) {
                Text(stringResource(R.string.label_auslieferung_date), fontSize = 12.sp, color = textSecondary)
                Text(auslieferungText, fontSize = DetailUiConstants.BodySp, color = if (auslieferungIsPlaceholder) textSecondary else textPrimary)
            }
        }
        if (isEditMode && onDeleteClick != null) {
            Spacer(Modifier.size(8.dp))
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.label_delete),
                    tint = colorResource(R.color.status_overdue),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
