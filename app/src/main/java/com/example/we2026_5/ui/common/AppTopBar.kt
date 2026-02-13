package com.example.we2026_5.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.ui.theme.AppColors

/**
 * Einheitliche TopBar für die gesamte App.
 *
 * Nutzung:
 * ```
 * AppTopBar(
 *     title = "Einstellungen",
 *     onBack = { finish() }
 * )
 * ```
 *
 * @param title Titel-Text in der TopBar
 * @param onBack Wenn gesetzt, wird ein Zurück-Pfeil angezeigt. Null = kein Pfeil.
 * @param actions Slot für Action-Icons rechts (z.B. Filter, Suche, …)
 * @param containerColor Hintergrundfarbe (Standard: PrimaryBlue)
 * @param contentColor Text-/Icon-Farbe (Standard: Weiß)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    containerColor: Color = AppColors.PrimaryBlue,
    contentColor: Color = AppColors.White
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                color = contentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.content_desc_back),
                        tint = contentColor
                    )
                }
            }
        },
        actions = { actions() },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor)
    )
}
