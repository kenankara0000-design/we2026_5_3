package com.example.we2026_5.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.AppEmptyView
import com.example.we2026_5.ui.common.AppLoadingView

@Composable
fun CustomerDetailLoadingView(
    modifier: Modifier,
    @Suppress("UNUSED_PARAMETER") textSecondary: androidx.compose.ui.graphics.Color
) {
    AppLoadingView(modifier = modifier, text = stringResource(R.string.stat_loading))
}

@Composable
fun CustomerDetailNotFoundView(
    modifier: Modifier,
    @Suppress("UNUSED_PARAMETER") textSecondary: androidx.compose.ui.graphics.Color
) {
    AppEmptyView(
        modifier = modifier,
        title = stringResource(R.string.error_customer_not_found),
        emoji = "🔍"
    )
}
