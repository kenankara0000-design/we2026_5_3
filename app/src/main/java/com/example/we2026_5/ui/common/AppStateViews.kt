package com.example.we2026_5.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.ui.theme.AppColors
import com.example.we2026_5.ui.theme.AppSpacing

/**
 * Einheitliche Lade-Ansicht (zentrierter Spinner).
 *
 * @param modifier Optionaler Modifier (Standard: fillMaxSize)
 * @param text Optionaler Text unter dem Spinner
 */
@Composable
fun AppLoadingView(
    modifier: Modifier = Modifier.fillMaxSize(),
    text: String? = null
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = AppColors.PrimaryBlue)
            if (text != null) {
                Spacer(Modifier.height(AppSpacing.Medium))
                Text(
                    text = text,
                    fontSize = 14.sp,
                    color = AppColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Einheitliche Fehler-Ansicht (Emoji + Titel + Nachricht + optionaler Retry-Button).
 *
 * @param message Fehlernachricht
 * @param title Optionaler Titel (Standard: „Fehler beim Laden")
 * @param emoji Optionales Emoji
 * @param onRetry Wenn gesetzt, wird ein „Erneut versuchen"-Button angezeigt
 * @param modifier Optionaler Modifier
 */
@Composable
fun AppErrorView(
    message: String,
    modifier: Modifier = Modifier.fillMaxSize(),
    title: String = stringResource(R.string.tour_error_title),
    emoji: String = "⚠️",
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(AppSpacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 48.sp)
        Spacer(Modifier.height(AppSpacing.Default))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.StatusOverdue
        )
        Spacer(Modifier.height(AppSpacing.Small))
        Text(
            text = message,
            fontSize = 14.sp,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(Modifier.height(AppSpacing.Default))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.tour_retry))
            }
        }
    }
}

/**
 * Einheitliche Leer-Ansicht (Emoji + Titel + optionaler Untertitel).
 *
 * @param title Haupttext
 * @param emoji Optionales Emoji
 * @param subtitle Optionaler Untertitel
 * @param modifier Optionaler Modifier
 */
@Composable
fun AppEmptyView(
    title: String,
    modifier: Modifier = Modifier.fillMaxSize(),
    emoji: String = "📭",
    subtitle: String? = null
) {
    Column(
        modifier = modifier.padding(AppSpacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 48.sp)
        Spacer(Modifier.height(AppSpacing.Default))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Spacer(Modifier.height(AppSpacing.Small))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = AppColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
