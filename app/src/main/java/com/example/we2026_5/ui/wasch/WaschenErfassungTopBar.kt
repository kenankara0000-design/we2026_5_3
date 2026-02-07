package com.example.we2026_5.ui.wasch

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaschenErfassungTopBar(
    primaryBlue: androidx.compose.ui.graphics.Color,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                stringResource(R.string.wasch_title),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = { },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue)
    )
}
