package com.example.we2026_5.ui.listebearbeiten

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.AppEmptyView
import com.example.we2026_5.ui.common.AppLoadingView

@Composable
fun ListeBearbeitenLoadingView(
    modifier: Modifier,
    @Suppress("UNUSED_PARAMETER") textColor: Color
) {
    AppLoadingView(modifier = modifier, text = stringResource(R.string.stat_loading))
}

@Composable
fun ListeBearbeitenEmptyView(
    modifier: Modifier,
    @Suppress("UNUSED_PARAMETER") textColor: Color
) {
    AppEmptyView(
        modifier = modifier,
        title = stringResource(R.string.list_empty_customers),
        emoji = "📋"
    )
}
