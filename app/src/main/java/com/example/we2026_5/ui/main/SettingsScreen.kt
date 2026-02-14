package com.example.we2026_5.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.AppTopBar
import com.example.we2026_5.util.ComposeDialogHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenPreise: () -> Unit,
    onOpenDataImport: () -> Unit,
    onResetAppData: () -> Unit,
    onAbmelden: () -> Unit,
    onBack: () -> Unit,
    // Phase 4: Kartenanzeige + Funktionen
    showAddressOnCard: Boolean = true,
    onShowAddressOnCardChange: (Boolean) -> Unit = {},
    showPhoneOnCard: Boolean = false,
    onShowPhoneOnCardChange: (Boolean) -> Unit = {},
    showNotesOnCard: Boolean = false,
    onShowNotesOnCardChange: (Boolean) -> Unit = {},
    showSaveAndNext: Boolean = false,
    onShowSaveAndNextChange: (Boolean) -> Unit = {}
) {
    val textSecondary = colorResource(R.color.text_secondary)
    var menuExpanded by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.settings_menu),
                            tint = Color.White
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_reset_app_data)) },
                            onClick = {
                                menuExpanded = false
                                showResetConfirm = true
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onOpenPreise,
                modifier = Modifier.fillMaxWidth().height(com.example.we2026_5.ui.theme.AppSpacing.ButtonHeight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.settings_btn_preise), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onOpenDataImport,
                modifier = Modifier.fillMaxWidth().height(com.example.we2026_5.ui.theme.AppSpacing.ButtonHeight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.settings_btn_data_import), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                stringResource(R.string.settings_sevdesk_readonly_hint),
                fontSize = 12.sp,
                color = textSecondary,
                modifier = Modifier.fillMaxWidth()
            )

            // Phase 4: Kartenanzeige-Konfiguration
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                stringResource(R.string.settings_section_card_display),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.text_primary)
            )
            SettingsToggleRow(
                label = stringResource(R.string.settings_card_show_address),
                checked = showAddressOnCard,
                onCheckedChange = onShowAddressOnCardChange
            )
            SettingsToggleRow(
                label = stringResource(R.string.settings_card_show_phone),
                checked = showPhoneOnCard,
                onCheckedChange = onShowPhoneOnCardChange
            )
            SettingsToggleRow(
                label = stringResource(R.string.settings_card_show_notes),
                checked = showNotesOnCard,
                onCheckedChange = onShowNotesOnCardChange
            )

            // Phase 4: Optionale Funktionen
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                stringResource(R.string.settings_section_features),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.text_primary)
            )
            SettingsToggleRow(
                label = stringResource(R.string.settings_feature_save_and_next),
                checked = showSaveAndNext,
                onCheckedChange = onShowSaveAndNextChange
            )
            Text(
                stringResource(R.string.settings_feature_save_and_next_hint),
                fontSize = 12.sp,
                color = textSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            OutlinedButton(
                onClick = onAbmelden,
                modifier = Modifier.fillMaxWidth().height(com.example.we2026_5.ui.theme.AppSpacing.ButtonHeight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.settings_abmelden), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    ComposeDialogHelper.ConfirmDialog(
        visible = showResetConfirm,
        title = stringResource(R.string.settings_reset_app_data),
        message = stringResource(R.string.settings_reset_app_data_confirm),
        confirmText = stringResource(R.string.settings_reset_app_data),
        isDestructive = true,
        onDismiss = { showResetConfirm = false },
        onConfirm = onResetAppData
    )
}

/** Toggle-Zeile: Label links, Switch rechts. */
@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
