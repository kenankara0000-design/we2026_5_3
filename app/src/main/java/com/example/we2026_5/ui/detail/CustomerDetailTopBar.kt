package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailTopBar(
    typeLetter: String,
    typeColor: androidx.compose.ui.graphics.Color,
    displayName: String,
    isInEditMode: Boolean,
    statusOverdue: androidx.compose.ui.graphics.Color,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    overflowMenuExpanded: Boolean,
    onOverflowMenuDismiss: () -> Unit,
    onOverflowMenuExpand: () -> Unit,
    onSave: (() -> Unit)? = null,
    showSaveAndNext: Boolean = false,
    onSaveAndNext: (() -> Unit)? = null
) {
    val primaryBlue = colorResource(R.color.primary_blue)
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(1f)
            ) {
                Text(
                    text = typeLetter,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(typeColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    text = displayName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        },
        navigationIcon = { },
        actions = {
            if (isInEditMode && onSave != null) {
                TextButton(onClick = onSave) {
                    Text(stringResource(R.string.btn_save), color = Color.White)
                }
            }
            if (isInEditMode && showSaveAndNext && onSaveAndNext != null) {
                TextButton(onClick = onSaveAndNext) {
                    Text(stringResource(R.string.btn_save_and_next_customer), color = Color.White)
                }
            }
            if (isInEditMode) {
                Box {
                    IconButton(onClick = onOverflowMenuExpand) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = stringResource(R.string.content_desc_more_options),
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = overflowMenuExpanded,
                        onDismissRequest = onOverflowMenuDismiss
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.label_delete), color = statusOverdue) },
                            onClick = {
                                onOverflowMenuDismiss()
                                onDelete()
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue)
    )
}
