package com.example.we2026_5.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataImportScreen(
    onSevDeskImport: () -> Unit,
    onBack: () -> Unit
) {
    val primaryBlue = colorResource(R.color.primary_blue)
    val textSecondary = colorResource(R.color.text_secondary)
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.data_import_title),
                onBack = onBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onSevDeskImport,
                modifier = Modifier.fillMaxWidth().height(com.example.we2026_5.ui.theme.AppSpacing.ButtonHeight),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = primaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.settings_sevdesk_import), fontSize = 15.sp)
            }
            Text(
                stringResource(R.string.settings_sevdesk_readonly_hint),
                fontSize = 12.sp,
                color = textSecondary,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

