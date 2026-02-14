package com.example.we2026_5.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.DetailUiConstants

@Composable
fun CustomerDetailActionsRow(
    primaryBlue: androidx.compose.ui.graphics.Color,
    onEdit: () -> Unit,
    isUploading: Boolean = false,
    textPrimary: androidx.compose.ui.graphics.Color = colorResource(R.color.text_primary)
) {
    var kebabExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            IconButton(onClick = { kebabExpanded = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.content_desc_more_options),
                    tint = textPrimary
                )
            }
            DropdownMenu(
                expanded = kebabExpanded,
                onDismissRequest = { kebabExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.label_edit), color = textPrimary) },
                    onClick = {
                        kebabExpanded = false
                        onEdit()
                    }
                )
            }
        }
    }
    if (isUploading) {
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.label_foto_uploading), fontSize = 12.sp, color = primaryBlue)
    }
    Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
}
