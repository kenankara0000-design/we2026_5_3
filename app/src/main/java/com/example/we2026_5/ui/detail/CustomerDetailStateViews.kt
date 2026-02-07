package com.example.we2026_5.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.we2026_5.R

@Composable
fun CustomerDetailLoadingView(
    modifier: Modifier,
    textSecondary: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.stat_loading), color = textSecondary)
    }
}

@Composable
fun CustomerDetailNotFoundView(
    modifier: Modifier,
    textSecondary: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.error_customer_not_found), color = textSecondary)
    }
}
