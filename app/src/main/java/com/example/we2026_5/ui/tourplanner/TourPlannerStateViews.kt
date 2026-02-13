package com.example.we2026_5.ui.tourplanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.AppEmptyView
import com.example.we2026_5.ui.common.AppErrorView
import com.example.we2026_5.ui.common.AppLoadingView

@Composable
fun TourPlannerLoadingView(
    @Suppress("UNUSED_PARAMETER") primaryBlue: androidx.compose.ui.graphics.Color
) {
    AppLoadingView()
}

@Composable
fun TourPlannerErrorView(
    errorMessage: String,
    @Suppress("UNUSED_PARAMETER") textSecondary: androidx.compose.ui.graphics.Color,
    onRetry: () -> Unit
) {
    AppErrorView(
        message = errorMessage,
        onRetry = onRetry
    )
}

@Composable
fun TourPlannerEmptyView(
    @Suppress("UNUSED_PARAMETER") textSecondary: androidx.compose.ui.graphics.Color
) {
    AppEmptyView(
        title = stringResource(R.string.tour_empty_title),
        emoji = "📅",
        subtitle = stringResource(R.string.tour_empty_subtitle)
    )
}
