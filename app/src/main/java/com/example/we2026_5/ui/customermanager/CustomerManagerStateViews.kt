package com.example.we2026_5.ui.customermanager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R

@Composable
fun CustomerManagerLoadingView(
    textSecondary: androidx.compose.ui.graphics.Color
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.stat_loading), color = textSecondary)
    }
}

@Composable
fun CustomerManagerEmptyView(
    textSecondary: androidx.compose.ui.graphics.Color
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("👥", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.cm_empty_title), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textSecondary)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.cm_empty_subtitle), fontSize = 14.sp, color = textSecondary)
        }
    }
}
