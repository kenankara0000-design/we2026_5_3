package com.example.we2026_5.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.we2026_5.ui.theme.AppColors
import com.example.we2026_5.ui.theme.AppSpacing
import com.example.we2026_5.ui.theme.AppTypography

/**
 * Primärer Button – einheitlich für die gesamte App.
 *
 * Nutzung:
 * ```
 * AppPrimaryButton(text = "Speichern", onClick = { ... })
 * ```
 *
 * @param text Button-Label
 * @param onClick Klick-Handler
 * @param modifier Optionaler Modifier
 * @param enabled Aktiviert/deaktiviert
 * @param fillWidth Wenn true (Standard), füllt die volle Breite
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(AppSpacing.MinTouchTarget),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = AppColors.PrimaryBlue,
            contentColor = AppColors.White,
            disabledContainerColor = AppColors.ButtonInactive,
            disabledContentColor = AppColors.White
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = text, style = AppTypography.ButtonText)
    }
}

/**
 * Sekundärer Button (Outlined) – einheitlich für die gesamte App.
 *
 * @param text Button-Label
 * @param onClick Klick-Handler
 * @param modifier Optionaler Modifier
 * @param enabled Aktiviert/deaktiviert
 * @param fillWidth Wenn true (Standard), füllt die volle Breite
 */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(AppSpacing.MinTouchTarget),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = AppColors.PrimaryBlue,
            disabledContentColor = AppColors.ButtonInactive
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = text, style = AppTypography.ButtonText)
    }
}
