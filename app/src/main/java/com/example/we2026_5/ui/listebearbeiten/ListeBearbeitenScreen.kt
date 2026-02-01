package com.example.we2026_5.ui.listebearbeiten

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListeBearbeitenScreen(
    state: ListeBearbeitenState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSave: (name: String, listeArt: String) -> Unit,
    onDelete: () -> Unit,
    onTerminAnlegen: () -> Unit,
    onRemoveKunde: (Customer) -> Unit,
    onAddKunde: (Customer) -> Unit,
    onRefresh: () -> Unit,
    onDatumSelected: (position: Int, isAbholung: Boolean) -> Unit
) {
    val context = LocalContext.current
    val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
    val surfaceWhite = Color(ContextCompat.getColor(context, R.color.surface_white))
    val textPrimary = Color(ContextCompat.getColor(context, R.color.text_primary))
    val textSecondary = Color(ContextCompat.getColor(context, R.color.text_secondary))
    val statusOverdue = Color(ContextCompat.getColor(context, R.color.status_overdue))
    val statusDone = Color(ContextCompat.getColor(context, R.color.status_done))

    var editName by remember(state.liste?.name) { mutableStateOf(state.liste?.name ?: "") }
    var editListeArt by remember(state.liste?.listeArt) { mutableStateOf(state.liste?.listeArt ?: "Gewerbe") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    val validationNameMissing = stringResource(R.string.validation_name_missing)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.liste?.name ?: stringResource(R.string.label_list_edit),
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
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.stat_loading),
                            tint = Color.White
                        )
                    }
                    if (state.isInEditMode) {
                        Box {
                            IconButton(onClick = { overflowMenuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = stringResource(R.string.content_desc_more_options),
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = overflowMenuExpanded,
                                onDismissRequest = { overflowMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.label_delete), color = statusOverdue) },
                                    onClick = {
                                        overflowMenuExpanded = false
                                        onDelete()
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue)
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.stat_loading), color = textSecondary)
            }
        } else if (state.isEmpty && state.kundenInListe.isEmpty() && state.verfuegbareKunden.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📋", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.list_empty_customers), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textSecondary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (!state.isInEditMode) {
                    androidx.compose.material3.Button(
                        onClick = onEdit,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.label_list_edit)) }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = stringResource(R.string.label_list_name_field), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(text = state.liste?.name ?: "", modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(8.dp), color = textPrimary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.label_list_type), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(text = state.liste?.listeArt ?: "", modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(8.dp), color = textPrimary)
                } else {
                    Text(text = stringResource(R.string.label_list_name_field), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { value -> editName = value; nameError = null },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = nameError != null,
                        supportingText = nameError?.let { err -> { Text(err, color = statusOverdue) } }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.label_list_type), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = editListeArt == "Gewerbe", onClick = { editListeArt = "Gewerbe" })
                            Text(stringResource(R.string.label_type_gewerbe), color = textPrimary)
                        }
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = editListeArt == "Privat", onClick = { editListeArt = "Privat" })
                            Text(stringResource(R.string.label_type_privat), color = textPrimary)
                        }
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = editListeArt == "Liste", onClick = { editListeArt = "Liste" })
                            Text(stringResource(R.string.label_type_liste), color = textPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.material3.Button(
                            onClick = {
                                val name = editName.trim()
                                if (name.isEmpty()) nameError = validationNameMissing
                                else { nameError = null; onSave(name, editListeArt) }
                            },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Color(ContextCompat.getColor(context, R.color.termin_regel_button_save)),
                                contentColor = Color.White
                            )
                        ) { Text(stringResource(R.string.btn_save)) }
                    }
                }

                if (state.intervalle.isNotEmpty() || state.isInEditMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = surfaceWhite),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(stringResource(R.string.label_intervals), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            state.intervalle.forEachIndexed { index, intervall ->
                                IntervallRow(
                                    intervall = intervall,
                                    isEditMode = state.isInEditMode,
                                    onAbholungClick = { onDatumSelected(index, true) },
                                    onAuslieferungClick = { onDatumSelected(index, false) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            androidx.compose.material3.Button(onClick = onTerminAnlegen, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.label_termine_anlegen)) }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.Button(onClick = onTerminAnlegen, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.label_termine_anlegen)) }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.label_customers_in_list), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                state.kundenInListe.forEach { kunde ->
                    KundeInListeItem(kunde, showRemove = true, onRemove = { onRemoveKunde(kunde) }, onAdd = {}, textPrimary = textPrimary, textSecondary = textSecondary, statusOverdue = statusOverdue, statusDone = statusDone)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.label_add_available_customers), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                state.verfuegbareKunden.forEach { kunde ->
                    KundeInListeItem(kunde, showRemove = false, onRemove = {}, onAdd = { onAddKunde(kunde) }, textPrimary = textPrimary, textSecondary = textSecondary, statusOverdue = statusOverdue, statusDone = statusDone)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun IntervallRow(
    intervall: ListeIntervall,
    isEditMode: Boolean,
    onAbholungClick: () -> Unit,
    onAuslieferungClick: () -> Unit
) {
    val abholungText = if (intervall.abholungDatum > 0) DateFormatter.formatDateWithLeadingZeros(intervall.abholungDatum) else stringResource(R.string.label_not_set)
    val auslieferungText = if (intervall.auslieferungDatum > 0) DateFormatter.formatDateWithLeadingZeros(intervall.auslieferungDatum) else stringResource(R.string.label_not_set)
    val textSecondary = Color(androidx.compose.ui.platform.LocalContext.current.resources.getColor(R.color.text_secondary, null))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isEditMode) Modifier.clickable(onClick = {}) else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = if (isEditMode) Modifier.clickable(onClick = onAbholungClick) else Modifier) {
            Text(stringResource(R.string.label_abholung_date), fontSize = 12.sp, color = textSecondary)
            Text(abholungText, fontSize = 14.sp)
        }
        Column(modifier = if (isEditMode) Modifier.clickable(onClick = onAuslieferungClick) else Modifier) {
            Text(stringResource(R.string.label_auslieferung_date), fontSize = 12.sp, color = textSecondary)
            Text(auslieferungText, fontSize = 14.sp)
        }
    }
}

@Composable
private fun KundeInListeItem(
    kunde: Customer,
    showRemove: Boolean,
    onRemove: () -> Unit,
    onAdd: () -> Unit,
    textPrimary: Color,
    textSecondary: Color,
    statusOverdue: Color,
    statusDone: Color = Color.Unspecified
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(androidx.compose.ui.platform.LocalContext.current.resources.getColor(R.color.surface_white, null))),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (kunde.fotoUrls.isNotEmpty()) {
                Card(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    AsyncImage(
                        model = kunde.fotoUrls.first(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
                Spacer(Modifier.size(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when (kunde.kundenArt) {
                            "Privat" -> stringResource(R.string.label_type_p_letter)
                            "Liste" -> stringResource(R.string.label_type_l_letter)
                            else -> stringResource(R.string.label_type_g)
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(kunde.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }
                Text(kunde.adresse, fontSize = 14.sp, color = textSecondary)
            }
            if (showRemove) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.content_desc_remove_from_list), tint = statusOverdue)
                }
            } else {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.content_desc_add_to_list), tint = statusDone)
                }
            }
        }
    }
}
