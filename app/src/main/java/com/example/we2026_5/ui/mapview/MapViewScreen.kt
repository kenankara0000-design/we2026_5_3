package com.example.we2026_5.ui.mapview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.example.we2026_5.ui.common.AppTopBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.we2026_5.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapViewScreen(
    state: MapViewState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val textSecondary = Color(ContextCompat.getColor(context, R.color.text_secondary))

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.map_title),
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is MapViewState.Loading ->
                    Text(
                        text = stringResource(R.string.stat_loading),
                        color = textSecondary,
                        fontSize = 16.sp
                    )
                is MapViewState.Empty ->
                    Text(
                        text = stringResource(R.string.map_empty),
                        color = textSecondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                is MapViewState.Error ->
                    Text(
                        text = if (state.messageArg != null)
                            stringResource(state.messageResId, state.messageArg)
                        else
                            stringResource(state.messageResId),
                        color = textSecondary,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                is MapViewState.Success -> {
                    // Activity öffnet Maps und beendet sich – hier nichts anzeigen
                    Text(
                        text = stringResource(R.string.stat_loading),
                        color = textSecondary,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}
