package com.example.we2026_5.ui.addcustomer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.we2026_5.R

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
    onAbholungTagChange: (Int) -> Unit,
    onAuslieferungTagChange: (Int) -> Unit,
    onDefaultUhrzeitChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onTourWochentagChange: (Int) -> Unit,
    onTourStadtChange: (String) -> Unit,
    onTourZeitStartChange: (String) -> Unit,
    onTourZeitEndeChange: (String) -> Unit,
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
                RadioOption(
                    text = stringResource(R.string.label_type_gewerblich),
                    selected = state.kundenArt == "Gewerblich",
                    onSelect = { onKundenArtChange("Gewerblich") },
                    textPrimary = textPrimary
                )
                RadioOption(
                    text = stringResource(R.string.label_type_privat),
                    selected = state.kundenArt == "Privat",
                    onSelect = { onKundenArtChange("Privat") },
                    textPrimary = textPrimary
                )
                RadioOption(
                    text = stringResource(R.string.label_type_liste),
                    selected = state.kundenArt == "Liste",
                    onSelect = { onKundenArtChange("Liste") },
                    textPrimary = textPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            WeekdaySelector(
                label = stringResource(R.string.label_default_pickup_day),
                selected = state.abholungWochentag,
                onSelect = onAbholungTagChange
            )
            Spacer(modifier = Modifier.height(12.dp))
            WeekdaySelector(
                label = stringResource(R.string.label_default_delivery_day),
                selected = state.auslieferungWochentag,
                onSelect = onAuslieferungTagChange
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.defaultUhrzeit,
                onValueChange = onDefaultUhrzeitChange,
                label = { Text(stringResource(R.string.label_default_time_optional)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("09:00") }
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.tagsInput,
                onValueChange = onTagsChange,
                label = { Text(stringResource(R.string.label_customer_tags)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                placeholder = { Text(stringResource(R.string.hint_tags_example)) }
            )
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
            Text(
                text = stringResource(R.string.label_tour_plan),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            WeekdaySelector(
                label = stringResource(R.string.label_tour_weekday),
                selected = state.tourWochentag,
                onSelect = onTourWochentagChange
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = state.tourStadt,
                onValueChange = onTourStadtChange,
                label = { Text(stringResource(R.string.label_tour_city)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.tourZeitStart,
                    onValueChange = onTourZeitStartChange,
                    label = { Text(stringResource(R.string.label_time_from)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("09:00") }
                )
                OutlinedTextField(
                    value = state.tourZeitEnde,
                    onValueChange = onTourZeitEndeChange,
                    label = { Text(stringResource(R.string.label_time_to)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("13:00") }
                )
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

@Composable
private fun RadioOption(
    text: String,
    selected: Boolean,
    onSelect: () -> Unit,
    textPrimary: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clickable { onSelect() }
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text, color = textPrimary, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
private fun WeekdaySelector(
    label: String,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val context = LocalContext.current
    val textPrimary = Color(ContextCompat.getColor(context, R.color.text_primary))
    val weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
    Column {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            weekdays.forEachIndexed { index, title ->
                FilterChip(
                    selected = selected == index,
                    onClick = { onSelect(index) },
                    label = { Text(title) }
                )
            }
        }
    }
}
