package com.example.we2026_5.ui.sevdesk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.we2026_5.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SevDeskImportScreen(
    state: SevDeskImportState,
    onBack: () -> Unit,
    onTokenChange: (String) -> Unit,
    onSaveToken: () -> Unit,
    onImportContacts: () -> Unit,
    onImportArticles: () -> Unit,
    onClearMessage: () -> Unit,
    onRunApiTest: () -> Unit,
    onDiscoveryTest: () -> Unit,
    onClearTestResult: () -> Unit,
    onExportTestResult: (String) -> Unit
) {
    val context = LocalContext.current
    val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
    val textSecondary = colorResource(R.color.text_secondary)
    val isBusy = state.isImportingContacts || state.isImportingArticles || state.isRunningApiTest

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sevdesk_import_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = { },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (isBusy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = primaryBlue
                )
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = state.token,
                onValueChange = onTokenChange,
                label = { Text(stringResource(R.string.sevdesk_token_hint)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSaveToken, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                    Text(stringResource(R.string.sevdesk_save_token))
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colorResource(R.color.surface_light)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.sevdesk_api_overview_title), fontWeight = FontWeight.SemiBold, color = textSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(R.string.sevdesk_api_overview), color = textSecondary, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.sevdesk_before_reimport), fontWeight = FontWeight.SemiBold, color = textSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onImportContacts,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (state.isImportingContacts) "…" else stringResource(R.string.sevdesk_import_contacts))
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onImportArticles,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (state.isImportingArticles) "…" else stringResource(R.string.sevdesk_import_articles))
            }
            state.message?.let { msg ->
                Spacer(Modifier.height(16.dp))
                Text(msg, color = primaryBlue, fontSize = 14.sp)
            }
            state.error?.let { err ->
                Spacer(Modifier.height(16.dp))
                Text(err, color = Color.Red, fontSize = 14.sp)
            }

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.sevdesk_test_section), fontWeight = FontWeight.SemiBold, color = textSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDiscoveryTest,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (state.isRunningApiTest) "…" else stringResource(R.string.sevdesk_discovery_btn))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRunApiTest,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (state.isRunningApiTest) "…" else stringResource(R.string.sevdesk_test_btn))
            }
            state.testResult?.let { result ->
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onExportTestResult(result) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text(stringResource(R.string.sevdesk_test_export))
                    }
                    OutlinedButton(onClick = onClearTestResult, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                        Text(stringResource(R.string.sevdesk_test_clear))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(result, fontSize = 11.sp, color = textSecondary)
                }
            }
        }
    }
}
