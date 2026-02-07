package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R

@Composable
fun ErledigungSheetKopf(
    title: String,
    primaryBlueDark: androidx.compose.ui.graphics.Color,
    textSecondary: androidx.compose.ui.graphics.Color,
    divider: androidx.compose.ui.graphics.Color
) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = primaryBlueDark
    )
    Text(
        text = stringResource(R.string.sheet_legend),
        fontSize = 11.sp,
        color = textSecondary,
        modifier = Modifier.padding(top = 4.dp)
    )
    Spacer(Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(vertical = 12.dp)
            .background(divider)
    )
    Spacer(Modifier.height(12.dp))
}
