package com.example.we2026_5.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.ui.common.DetailUiConstants
import com.example.we2026_5.R

@Composable
fun CustomerDetailActionsRow(
    primaryBlue: androidx.compose.ui.graphics.Color,
    onUrlaub: () -> Unit,
    onEdit: () -> Unit,
    isUploading: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DetailUiConstants.FieldSpacing)
    ) {
        Button(
            onClick = onUrlaub,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.button_urlaub))
        ) {
            Text(stringResource(R.string.label_urlaub))
        }
        Button(
            onClick = onEdit,
            modifier = Modifier.weight(1f)
        ) {
            Text(stringResource(R.string.label_edit))
        }
    }
    if (isUploading) {
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.label_foto_uploading), fontSize = 12.sp, color = primaryBlue)
    }
    Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
}
