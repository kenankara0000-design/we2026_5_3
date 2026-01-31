package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customer: Customer?,
    isInEditMode: Boolean,
    editIntervalle: List<CustomerIntervall>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSave: (Map<String, Any>) -> Unit,
    onDelete: () -> Unit,
    onTerminAnlegen: () -> Unit,
    onTakePhoto: () -> Unit,
    onAdresseClick: () -> Unit,
    onTelefonClick: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onDatumSelected: (Int, Boolean) -> Unit
) {
    val context = LocalContext.current
    val primaryBlue = colorResource(R.color.primary_blue)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val surfaceWhite = colorResource(R.color.surface_white)
    val statusOverdue = colorResource(R.color.status_overdue)

    var editName by remember(customer?.id, isInEditMode) { mutableStateOf(customer?.name ?: "") }
    var editAdresse by remember(customer?.id, isInEditMode) { mutableStateOf(customer?.adresse ?: "") }
    var editTelefon by remember(customer?.id, isInEditMode) { mutableStateOf(customer?.telefon ?: "") }
    var editNotizen by remember(customer?.id, isInEditMode) { mutableStateOf(customer?.notizen ?: "") }
    var editKundenArt by remember(customer?.id, isInEditMode) { mutableStateOf(customer?.kundenArt ?: "Gewerblich") }
    var nameError by remember { mutableStateOf<String?>(null) }
    val validationNameMissing = stringResource(R.string.validation_name_missing)

    val typeLabel = when (customer?.kundenArt) {
        "Privat" -> stringResource(R.string.label_type_privat)
        "Liste" -> stringResource(R.string.label_type_liste)
        else -> stringResource(R.string.label_type_gewerblich)
    }
    val typeLetter = when (customer?.kundenArt) {
        "Privat" -> "P"
        "Liste" -> "L"
        else -> "G"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = typeLetter,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            text = customer?.name ?: stringResource(R.string.label_customer_name),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.content_desc_back),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.stat_loading), color = textSecondary)
            }
        } else if (customer == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.error_customer_not_found), color = textSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (!isInEditMode) {
                    androidx.compose.material3.Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.label_edit))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.label_customer_type), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(text = typeLabel, modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp), color = textPrimary)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.label_address_label), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(
                        text = customer.adresse,
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp).clickable(onClick = onAdresseClick),
                        color = textPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.label_phone_label), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(
                        text = customer.telefon,
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp).clickable(onClick = onTelefonClick),
                        color = textPrimary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.label_notes_label), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(text = customer.notizen, modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp), color = textPrimary)
                } else {
                    Text(stringResource(R.string.label_name), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it; nameError = null },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = nameError != null,
                        supportingText = nameError?.let { err -> { Text(err, color = statusOverdue) } }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.label_customer_type), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        listOf("Gewerblich" to stringResource(R.string.label_type_gewerblich), "Privat" to stringResource(R.string.label_type_privat), "Liste" to stringResource(R.string.label_type_liste)).forEach { (value, label) ->
                            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = editKundenArt == value, onClick = { editKundenArt = value })
                                Text(label, color = textPrimary, fontSize = 14.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.label_address_label), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    OutlinedTextField(value = editAdresse, onValueChange = { editAdresse = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.label_phone_label), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    OutlinedTextField(value = editTelefon, onValueChange = { editTelefon = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.label_notes_label), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    OutlinedTextField(value = editNotizen, onValueChange = { editNotizen = it }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Button(
                            onClick = {
                                val name = editName.trim()
                                if (name.isEmpty()) nameError = validationNameMissing
                                else {
                                    nameError = null
                                    onSave(mapOf(
                                        "name" to name,
                                        "adresse" to editAdresse.trim(),
                                        "telefon" to editTelefon.trim(),
                                        "notizen" to editNotizen.trim(),
                                        "kundenArt" to editKundenArt,
                                        "wochentag" to 0,
                                        "intervalle" to editIntervalle.map {
                                            mapOf(
                                                "id" to it.id,
                                                "abholungDatum" to it.abholungDatum,
                                                "auslieferungDatum" to it.auslieferungDatum,
                                                "wiederholen" to it.wiederholen,
                                                "intervallTage" to it.intervallTage,
                                                "intervallAnzahl" to it.intervallAnzahl,
                                                "erstelltAm" to it.erstelltAm
                                            )
                                        }
                                    ))
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.btn_save)) }
                        androidx.compose.material3.Button(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = statusOverdue)
                        ) { Text(stringResource(R.string.label_delete)) }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.label_intervals), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryBlue)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceWhite),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        val intervalleToShow = if (isInEditMode) editIntervalle else customer.intervalle
                        intervalleToShow.forEachIndexed { index, intervall ->
                            CustomerIntervallRow(
                                intervall = intervall,
                                isEditMode = isInEditMode,
                                onAbholungClick = { onDatumSelected(index, true) },
                                onAuslieferungClick = { onDatumSelected(index, false) }
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        androidx.compose.material3.Button(onClick = onTerminAnlegen, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.label_termine_anlegen))
                        }
                    }
                }

                if (!isInEditMode && customer.fotoUrls.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Fotos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(customer.fotoUrls, key = { it }) { url ->
                            Card(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clickable(onClick = { onPhotoClick(url) }),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Icon(painter = painterResource(R.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(24.dp), tint = textSecondary)
                                }
                            }
                        }
                    }
                }
                if (isInEditMode) {
                    Spacer(Modifier.height(16.dp))
                    androidx.compose.material3.OutlinedButton(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) {
                        Icon(painter = painterResource(R.drawable.ic_camera), contentDescription = null, Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Foto hinzufügen")
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerIntervallRow(
    intervall: CustomerIntervall,
    isEditMode: Boolean,
    onAbholungClick: () -> Unit,
    onAuslieferungClick: () -> Unit
) {
    val abholungText = if (intervall.abholungDatum > 0) DateFormatter.formatDateWithLeadingZeros(intervall.abholungDatum) else stringResource(R.string.label_not_set)
    val auslieferungText = if (intervall.auslieferungDatum > 0) DateFormatter.formatDateWithLeadingZeros(intervall.auslieferungDatum) else stringResource(R.string.label_not_set)
    val textSecondary = colorResource(R.color.text_secondary)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = if (isEditMode) Modifier.clickable(onClick = onAbholungClick) else Modifier) {
            Text(stringResource(R.string.label_abholung_date), fontSize = 12.sp, color = textSecondary)
            Text(abholungText, fontSize = 14.sp, color = colorResource(R.color.text_primary))
        }
        Column(modifier = if (isEditMode) Modifier.clickable(onClick = onAuslieferungClick) else Modifier) {
            Text(stringResource(R.string.label_auslieferung_date), fontSize = 12.sp, color = textSecondary)
            Text(auslieferungText, fontSize = 14.sp, color = colorResource(R.color.text_primary))
        }
    }
}
