package com.example.we2026_5.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Zentraler MaterialTheme-Wrapper für die gesamte App.
 *
 * Nutzung in jeder Activity:
 * ```
 * setContent {
 *     AppTheme {
 *         MyScreen(...)
 *     }
 * }
 * ```
 *
 * Liefert konsistente Farben und Typografie passend zu colors.xml / themes.xml.
 */

private val AppColorScheme = lightColorScheme(
    primary = AppColors.PrimaryBlue,
    onPrimary = AppColors.White,
    primaryContainer = AppColors.PrimaryBlueLight,
    onPrimaryContainer = AppColors.PrimaryBlueDark,

    secondary = AppColors.ButtonBlue,
    onSecondary = AppColors.White,
    secondaryContainer = AppColors.InfoBlueBg,
    onSecondaryContainer = AppColors.PrimaryBlueDark,

    tertiary = AppColors.AccentOrange,
    onTertiary = AppColors.White,

    error = AppColors.ErrorRed,
    onError = AppColors.White,
    errorContainer = AppColors.SectionOverdueBg,
    onErrorContainer = AppColors.SectionOverdueText,

    background = AppColors.BackgroundLight,
    onBackground = AppColors.TextPrimary,

    surface = AppColors.SurfaceWhite,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.SurfaceLight,
    onSurfaceVariant = AppColors.TextSecondary,

    outline = AppColors.LightGray
)

private val AppMaterialTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp
    )
)

@Composable
fun AppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppMaterialTypography,
        content = content
    )
}
