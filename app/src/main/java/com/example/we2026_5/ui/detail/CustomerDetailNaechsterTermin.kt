package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.we2026_5.ui.common.DetailUiConstants
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.R
import com.example.we2026_5.ui.theme.AppColors

@Composable
fun CustomerDetailNaechsterTermin(
    nextTerminMillis: Long,
    textPrimary: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    canDeleteNextTermin: Boolean = false,
    onDeleteNextTermin: () -> Unit = {}
) {
    Text(
        stringResource(R.string.label_next_termin),
        fontSize = DetailUiConstants.FieldLabelSp,
        fontWeight = FontWeight.Bold,
        color = textPrimary
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.LightGray)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (nextTerminMillis > 0) DateFormatter.formatDateWithWeekday(nextTerminMillis) else stringResource(R.string.label_not_set),
            modifier = Modifier.weight(1f),
            color = if (nextTerminMillis > 0) textPrimary else textSecondary,
            fontSize = DetailUiConstants.BodySp
        )
        if (canDeleteNextTermin && nextTerminMillis > 0) {
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onDeleteNextTermin,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.content_desc_delete_next_termin),
                    tint = textPrimary
                )
            }
        }
    }
    Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
}
