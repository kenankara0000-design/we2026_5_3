package com.example.we2026_5.ui.addcustomer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.R
import com.example.we2026_5.ui.addcustomer.AddCustomerIntervallSchnellauswahl
import com.example.we2026_5.ui.addcustomer.AddCustomerRadioOption
import com.example.we2026_5.ui.addcustomer.AddCustomerWeekdaySelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerScreen(
    state: AddCustomerState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onAdresseChange: (String) -> Unit,
    onStadtChange: (String) -> Unit,
    onPlzChange: (String) -> Unit,
    onTelefonChange: (String) -> Unit,
    onNotizenChange: (String) -> Unit,
    onKundenArtChange: (String) -> Unit,
    onKundenTypChange: (KundenTyp) -> Unit,
    onIntervallTageChange: (Int) -> Unit,
    onAbholungTagChange: (Int) -> Unit,
    onAuslieferungTagChange: (Int) -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
    val surfaceWhite = Color(ContextCompat.getColor(context, R.color.surface_white))
    val backgroundLight = Color(ContextCompat.getColor(context, R.color.background_light))
    val textPrimary = Color(ContextCompat.getColor(context, R.color.text_primary))
    val textSecondary = Color(ContextCompat.getColor(context, R.color.text_secondary))

    Scaffold(
        containerColor = backgroundLight,
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
                .fillMaxSize()
                .background(backgroundLight)
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
                value = state.stadt,
                onValueChange = onStadtChange,
                label = { Text(stringResource(R.string.label_city_optional)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.plz,
                onValueChange = onPlzChange,
                label = { Text(stringResource(R.string.label_zip_optional)) },
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
                text = stringResource(R.string.label_kunden_typ),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AddCustomerRadioOption(
                    text = stringResource(R.string.label_kunden_typ_regelmaessig),
                    selected = state.kundenTyp == KundenTyp.REGELMAESSIG,
                    onSelect = { onKundenTypChange(KundenTyp.REGELMAESSIG) },
                    textPrimary = textPrimary
                )
                AddCustomerRadioOption(
                    text = stringResource(R.string.label_kunden_typ_unregelmaessig),
                    selected = state.kundenTyp == KundenTyp.UNREGELMAESSIG,
                    onSelect = { onKundenTypChange(KundenTyp.UNREGELMAESSIG) },
                    textPrimary = textPrimary
                )
                AddCustomerRadioOption(
                    text = stringResource(R.string.label_kunden_typ_auf_abruf),
                    selected = state.kundenTyp == KundenTyp.AUF_ABRUF,
                    onSelect = { onKundenTypChange(KundenTyp.AUF_ABRUF) },
                    textPrimary = textPrimary
                )
            }
            if (state.kundenTyp == KundenTyp.REGELMAESSIG) {
                Spacer(modifier = Modifier.height(12.dp))
                AddCustomerIntervallSchnellauswahl(
                    selected = state.intervallTage,
                    onSelect = onIntervallTageChange,
                    textPrimary = textPrimary
                )
            }
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
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AddCustomerRadioOption(
                    text = stringResource(R.string.label_type_gewerblich),
                    selected = state.kundenArt == "Gewerblich",
                    onSelect = { onKundenArtChange("Gewerblich") },
                    textPrimary = textPrimary
                )
                AddCustomerRadioOption(
                    text = stringResource(R.string.label_type_privat),
                    selected = state.kundenArt == "Privat",
                    onSelect = { onKundenArtChange("Privat") },
                    textPrimary = textPrimary
                )
                AddCustomerRadioOption(
                    text = stringResource(R.string.label_type_tour),
                    selected = state.kundenArt == "Tour",
                    onSelect = { onKundenArtChange("Tour") },
                    textPrimary = textPrimary
                )
            }
            if (state.kundenTyp != KundenTyp.AUF_ABRUF) {
                Spacer(modifier = Modifier.height(12.dp))
                AddCustomerWeekdaySelector(
                    label = stringResource(R.string.label_default_pickup_day),
                    selected = state.abholungWochentag,
                    onSelect = onAbholungTagChange
                )
                Spacer(modifier = Modifier.height(12.dp))
                AddCustomerWeekdaySelector(
                    label = stringResource(R.string.label_default_delivery_day),
                    selected = state.auslieferungWochentag,
                    onSelect = onAuslieferungTagChange
                )
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
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(ContextCompat.getColor(context, R.color.termin_regel_button_save)),
                    contentColor = Color.White
                ),
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
