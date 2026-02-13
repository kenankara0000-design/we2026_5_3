package com.example.we2026_5.ui.customermanager

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.AppEmptyView
import com.example.we2026_5.ui.common.AppLoadingView

@Composable
fun CustomerManagerLoadingView(
    @Suppress("UNUSED_PARAMETER") textSecondary: androidx.compose.ui.graphics.Color
) {
    AppLoadingView(text = stringResource(R.string.stat_loading))
}

@Composable
fun CustomerManagerEmptyView(
    @Suppress("UNUSED_PARAMETER") textSecondary: androidx.compose.ui.graphics.Color
) {
    AppEmptyView(
        title = stringResource(R.string.cm_empty_title),
        emoji = "👥",
        subtitle = stringResource(R.string.cm_empty_subtitle)
    )
}
