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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onImportPrices: () -> Unit,
    onDeleteSevDeskContacts: () -> Unit,
    onDeleteSevDeskArticles: () -> Unit,
    onClearReimportList: () -> Unit,
    onClearMessage: () -> Unit
) {
    val context = LocalContext.current
    val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
    val textSecondary = colorResource(R.color.text_secondary)
    val isBusy = state.isImportingContacts || state.isImportingArticles || state.isImportingPrices
        || state.isDeletingSevDeskContacts || state.isDeletingSevDeskArticles
    var showDeleteContactsConfirm by remember { mutableStateOf(false) }
    var showDeleteArticlesConfirm by remember { mutableStateOf(false) }
    var showClearReimportConfirm by remember { mutableStateOf(false) }

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
            Text(stringResource(R.string.sevdesk_before_reimport), fontWeight = FontWeight.SemiBold, color = textSecondary, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showDeleteContactsConfirm = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isBusy,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (state.isDeletingSevDeskContacts) "…" else stringResource(R.string.sevdesk_delete_contacts))
                }
                OutlinedButton(
                    onClick = { showDeleteArticlesConfirm = true },
                    modifier = Modifier.weight(1f),
                    enabled = !isBusy,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (state.isDeletingSevDeskArticles) "…" else stringResource(R.string.sevdesk_delete_articles))
                }
            }
            OutlinedButton(
                onClick = { showClearReimportConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.sevdesk_clear_reimport_list))
            }
            if (showDeleteContactsConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteContactsConfirm = false },
                    title = { Text(stringResource(R.string.sevdesk_delete_contacts)) },
                    text = { Text(stringResource(R.string.sevdesk_delete_contacts_confirm)) },
                    confirmButton = {
                        Button(onClick = {
                            showDeleteContactsConfirm = false
                            onDeleteSevDeskContacts()
                        }, shape = RoundedCornerShape(8.dp)) {
                            Text(stringResource(R.string.sevdesk_delete_confirm_ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteContactsConfirm = false }) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                    }
                )
            }
            if (showDeleteArticlesConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteArticlesConfirm = false },
                    title = { Text(stringResource(R.string.sevdesk_delete_articles)) },
                    text = { Text(stringResource(R.string.sevdesk_delete_articles_confirm)) },
                    confirmButton = {
                        Button(onClick = {
                            showDeleteArticlesConfirm = false
                            onDeleteSevDeskArticles()
                        }, shape = RoundedCornerShape(8.dp)) {
                            Text(stringResource(R.string.sevdesk_delete_confirm_ok))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteArticlesConfirm = false }) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                    }
                )
            }
            if (showClearReimportConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearReimportConfirm = false },
                    title = { Text(stringResource(R.string.sevdesk_clear_reimport_list)) },
                    text = { Text(stringResource(R.string.sevdesk_clear_reimport_list_confirm)) },
                    confirmButton = {
                        Button(onClick = {
                            showClearReimportConfirm = false
                            onClearReimportList()
                        }, shape = RoundedCornerShape(8.dp)) {
                            Text(stringResource(R.string.sevdesk_clear_reimport_list))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearReimportConfirm = false }) {
                            Text(stringResource(R.string.btn_cancel))
                        }
                    }
                )
            }
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
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onImportPrices,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (state.isImportingPrices) "…" else stringResource(R.string.sevdesk_import_prices))
            }
            state.message?.let { msg ->
                Spacer(Modifier.height(16.dp))
                Text(msg, color = primaryBlue, fontSize = 14.sp)
            }
            state.error?.let { err ->
                Spacer(Modifier.height(16.dp))
                Text(err, color = Color.Red, fontSize = 14.sp)
            }
        }
    }
}
