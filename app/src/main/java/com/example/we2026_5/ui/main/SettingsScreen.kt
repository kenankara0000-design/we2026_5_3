package com.example.we2026_5.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSevDeskImport: () -> Unit,
    onAbmelden: () -> Unit,
    onBack: () -> Unit
) {
    val primaryBlue = colorResource(R.color.primary_blue)
    val textSecondary = colorResource(R.color.text_secondary)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.content_desc_back), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue, titleContentColor = Color.White)
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
            Text(
                stringResource(R.string.settings_sevdesk_import),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.text_primary)
            )
            Button(
                onClick = onSevDeskImport,
                modifier = Modifier.fillMaxWidth().height(48.dp),
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
            Spacer(Modifier.height(24.dp))
            OutlinedButton(
                onClick = onAbmelden,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.settings_abmelden), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
