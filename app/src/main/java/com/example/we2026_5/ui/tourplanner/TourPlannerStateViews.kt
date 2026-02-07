package com.example.we2026_5.ui.tourplanner

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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R

@Composable
fun TourPlannerLoadingView(
    primaryBlue: androidx.compose.ui.graphics.Color
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = primaryBlue)
    }
}

@Composable
fun TourPlannerErrorView(
    errorMessage: String,
    textSecondary: androidx.compose.ui.graphics.Color,
    onRetry: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.tour_error_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.status_overdue)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            errorMessage,
            fontSize = 14.sp,
            color = textSecondary
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.tour_retry))
        }
    }
}

@Composable
fun TourPlannerEmptyView(
    textSecondary: androidx.compose.ui.graphics.Color
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📅", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.tour_empty_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textSecondary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.tour_empty_subtitle),
            fontSize = 14.sp,
            color = textSecondary
        )
    }
}
