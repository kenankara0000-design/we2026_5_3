package com.example.we2026_5.ui.addcustomer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    state: AddCustomerState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onAdresseChange: (String) -> Unit,
    onTelefonChange: (String) -> Unit,
    onNotizenChange: (String) -> Unit,
    onKundenArtChange: (String) -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
    val surfaceWhite = Color(ContextCompat.getColor(context, R.color.surface_white))
    val textPrimary = Color(ContextCompat.getColor(context, R.color.text_primary))
    val textSecondary = Color(ContextCompat.getColor(context, R.color.text_secondary))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.add_customer_title),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.hint_name_required)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.errorMessage != null
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.adresse,
                onValueChange = onAdresseChange,
                label = { Text(stringResource(R.string.hint_address_optional)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.telefon,
                onValueChange = onTelefonChange,
                label = { Text(stringResource(R.string.hint_phone_optional)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.label_customer_type),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.kundenArt == "Gewerblich",
                        onClick = { onKundenArtChange("Gewerblich") }
                    )
                    Text(stringResource(R.string.label_type_gewerblich), color = textPrimary)
                }
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.kundenArt == "Privat",
                        onClick = { onKundenArtChange("Privat") }
                    )
                    Text(stringResource(R.string.label_type_privat), color = textPrimary)
                }
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = state.kundenArt == "Liste",
                        onClick = { onKundenArtChange("Liste") }
                    )
                    Text(stringResource(R.string.label_type_liste), color = textPrimary)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = surfaceWhite),
                shape = androidx.compose.material3.MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.add_customer_hint),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.add_customer_hint_text),
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.notizen,
                onValueChange = onNotizenChange,
                label = { Text(stringResource(R.string.hint_notes)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.Button(
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = primaryBlue),
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !state.isSaving
            ) {
                Text(
                    if (state.isSaving) stringResource(R.string.save_in_progress)
                    else stringResource(R.string.btn_save)
                )
            }
        }
    }
}
