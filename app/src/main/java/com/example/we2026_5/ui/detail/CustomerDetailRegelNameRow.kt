package com.example.we2026_5.ui.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.DetailUiConstants

@Composable
fun CustomerDetailRegelNameRow(
    regelName: String,
    isClickable: Boolean,
    primaryBlue: Color,
    textSecondary: Color,
    onClick: () -> Unit,
    showDeleteButton: Boolean = false,
    onDeleteClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable, onClick = onClick)
            .padding(vertical = DetailUiConstants.IntervalRowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = regelName,
            fontSize = DetailUiConstants.BodySp,
            fontWeight = FontWeight.Medium,
            color = if (isClickable) primaryBlue else textSecondary,
            modifier = Modifier.weight(1f)
        )
        if (showDeleteButton && onDeleteClick != null) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ) {
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
