package com.example.we2026_5.ui.common

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.we2026_5.ui.theme.AppColors
import com.example.we2026_5.ui.theme.AppSpacing

/**
 * Einheitliche Card für die gesamte App.
 *
 * Nutzung:
 * ```
 * AppCard {
 *     Text("Inhalt")
 * }
 * ```
 *
 * @param modifier Optionaler äußerer Modifier
 * @param onClick Wenn gesetzt, ist die Card klickbar
 * @param content Card-Inhalt (bereits mit Standard-Padding)
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = AppSpacing.CardElevation),
            shape = RoundedCornerShape(AppSpacing.CornerRadius)
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(AppSpacing.CardPadding)
            ) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = AppSpacing.CardElevation),
            shape = RoundedCornerShape(AppSpacing.CornerRadius)
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(AppSpacing.CardPadding)
            ) {
                content()
            }
        }
    }
}
