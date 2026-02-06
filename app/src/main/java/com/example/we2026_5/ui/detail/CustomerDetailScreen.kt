package com.example.we2026_5.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.CustomerStatus
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.intervallTageOrDefault
import com.example.we2026_5.util.tageAzuLOrDefault
import com.example.we2026_5.ui.detail.CustomerDetailIntervallRow
import com.example.we2026_5.ui.detail.CustomerDetailRegelNameRow
import com.example.we2026_5.ui.detail.CustomerDetailStatusSection
import com.example.we2026_5.ui.common.DetailUiConstants
import com.example.we2026_5.ui.addcustomer.AddCustomerState
import com.example.we2026_5.ui.addcustomer.CustomerStammdatenForm

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customer: Customer?,
    isInEditMode: Boolean,
    editIntervalle: List<CustomerIntervall>,
    editFormState: AddCustomerState?,
    onUpdateEditFormState: (AddCustomerState) -> Unit,
    isLoading: Boolean,
    isUploading: Boolean = false,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSave: (Map<String, Any>, List<CustomerIntervall>) -> Unit,
    onDelete: () -> Unit,
    onTerminAnlegen: () -> Unit,
    onPauseCustomer: (pauseEndeWochen: Int?) -> Unit,
    onResumeCustomer: () -> Unit,
    onTakePhoto: () -> Unit,
    onAdresseClick: () -> Unit,
    onTelefonClick: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onDatumSelected: (Int, Boolean) -> Unit,
    onDeleteIntervall: ((Int) -> Unit)? = null,
    onRemoveRegel: ((String) -> Unit)? = null,
    onTageAzuLZyklusChange: ((Int, Int) -> Unit)? = null,
    regelNameByRegelId: Map<String, String> = emptyMap(),
    onRegelClick: (String) -> Unit = {},
    onUrlaubStartActivity: (String) -> Unit = {} // Callback to start UrlaubActivity
) {
    val context = LocalContext.current
    val primaryBlue = colorResource(R.color.primary_blue)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val surfaceWhite = colorResource(R.color.surface_white)
    val statusOverdue = colorResource(R.color.status_overdue)

    var formState by remember(customer?.id, isInEditMode) {
        mutableStateOf(
            if (customer != null) {
                val tageAzuL = customer.tageAzuLOrDefault(7)
                val intervallTage = customer.intervallTageOrDefault(7)
                AddCustomerState(
                    name = customer.name,
                    adresse = customer.adresse,
                    stadt = customer.stadt,
                    plz = customer.plz,
                    telefon = customer.telefon,
                    notizen = customer.notizen,
                    kundenArt = customer.kundenArt,
                    kundenTyp = customer.kundenTyp,
                    tageAzuL = tageAzuL,
                    intervallTage = intervallTage,
                    kundennummer = customer.kundennummer,
                    abholungWochentage = customer.effectiveAbholungWochentage,
                    auslieferungWochentage = customer.effectiveAuslieferungWochentage,
                    defaultUhrzeit = customer.defaultUhrzeit,
                    tagsInput = customer.tags.joinToString(", "),
                    tourStadt = customer.tourSlot?.stadt ?: "",
                    tourZeitStart = customer.tourSlot?.zeitfenster?.start ?: "",
                    tourZeitEnde = customer.tourSlot?.zeitfenster?.ende ?: ""
                )
            } else AddCustomerState()
        )
    }
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    val validationNameMissing = stringResource(R.string.validation_name_missing)

    val typeLabel = when (customer?.kundenArt) {
        "Privat" -> stringResource(R.string.label_type_privat)
        "Tour" -> stringResource(R.string.label_type_tour)
        else -> stringResource(R.string.label_type_gewerblich)
    }
    val typeLetter = when (customer?.kundenArt) {
        "Privat" -> stringResource(R.string.label_type_p_letter)
        "Tour" -> stringResource(R.string.label_type_t_letter)
        else -> stringResource(R.string.label_type_g)
    }
    val typeColor = when (customer?.kundenArt) {
        "Privat" -> colorResource(R.color.button_privat_glossy)
        "Tour" -> colorResource(R.color.button_liste_glossy)
        else -> colorResource(R.color.button_gewerblich_glossy)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(1f)
                    ) {
                        Text(
                            text = typeLetter,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(typeColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            text = customer?.name ?: stringResource(R.string.label_customer_name),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
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
                actions = {
                    if (isInEditMode) {
                        Box {
                            IconButton(onClick = { overflowMenuExpanded = true }) {
                                Icon(
                                    Icons.Filled.MoreVert,
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DetailUiConstants.FieldSpacing)
                    ) {
                        androidx.compose.material3.Button(
                            onClick = { customer.id?.let { onUrlaubStartActivity(it) } },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colorResource(R.color.button_urlaub))
                        ) {
                            Text(stringResource(R.string.label_urlaub))
                        }
                        androidx.compose.material3.Button(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(stringResource(R.string.label_edit))
                        }
                    }
                    if (isUploading) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.label_foto_uploading), fontSize = 12.sp, color = primaryBlue)
                    }
                    Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
                CustomerDetailStatusSection(
                    customer = customer,
                    onPauseCustomer = onPauseCustomer,
                    onResumeCustomer = onResumeCustomer,
                    textPrimary = textPrimary,
                    surfaceWhite = surfaceWhite
                )
                Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
                val nextTermin = customer.getFaelligAm()
                Text(stringResource(R.string.label_next_termin), fontSize = DetailUiConstants.FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text(
                    text = if (nextTermin > 0) DateFormatter.formatDateWithWeekday(nextTermin) else stringResource(R.string.label_not_set),
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp),
                    color = if (nextTermin > 0) textPrimary else textSecondary,
                    fontSize = DetailUiConstants.BodySp
                )
                Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(DetailUiConstants.FieldSpacing)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_customer_type), fontSize = DetailUiConstants.FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(text = typeLabel, modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp), color = textPrimary, fontSize = DetailUiConstants.BodySp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_kunden_typ), fontSize = DetailUiConstants.FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(
                                text = when (customer.kundenTyp) {
                                    com.example.we2026_5.KundenTyp.REGELMAESSIG -> stringResource(R.string.label_kunden_typ_regelmaessig)
                                    com.example.we2026_5.KundenTyp.UNREGELMAESSIG -> stringResource(R.string.label_kunden_typ_unregelmaessig)
                                    com.example.we2026_5.KundenTyp.AUF_ABRUF -> stringResource(R.string.label_kunden_typ_auf_abruf)
                                },
                                modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp),
                                color = textPrimary,
                                fontSize = DetailUiConstants.BodySp
                            )
                        }
                        if (customer.kundenTyp != com.example.we2026_5.KundenTyp.AUF_ABRUF && (customer.effectiveAbholungWochentage.isNotEmpty() || customer.effectiveAuslieferungWochentage.isNotEmpty())) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.label_abholung_auslieferung_tag), fontSize = DetailUiConstants.FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                                val wochen = listOf("Mo","Di","Mi","Do","Fr","Sa","So")
                                val a = customer.effectiveAbholungWochentage.map { wochen[it] }.joinToString(", ").takeIf { it.isNotEmpty() }?.let { "$it A" } ?: ""
                                val l = customer.effectiveAuslieferungWochentage.map { wochen[it] }.joinToString(", ").takeIf { it.isNotEmpty() }?.let { "$it L" } ?: ""
                                Text(text = listOf(a, l).filter { it.isNotEmpty() }.joinToString(" / "), modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp), color = textPrimary, fontSize = DetailUiConstants.BodySp)
                            }
                        }
                        if (customer.kundenTyp == com.example.we2026_5.KundenTyp.AUF_ABRUF) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.label_abholung_auslieferung_tag), fontSize = DetailUiConstants.FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text(text = "–", modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp), color = textPrimary, fontSize = DetailUiConstants.BodySp)
                            }
                        }
                    }
                    Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
                    Text(stringResource(R.string.label_address_label), fontSize = DetailUiConstants.FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(
                        text = customer.adresse.ifEmpty { stringResource(R.string.label_not_set) },
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp).clickable(onClick = onAdresseClick),
                        color = if (customer.adresse.isNotEmpty()) textPrimary else textSecondary,
                        fontSize = DetailUiConstants.BodySp
                    )
                    Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
                    Text(stringResource(R.string.label_phone_label), fontSize = DetailUiConstants.FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(
                        text = customer.telefon.ifEmpty { stringResource(R.string.label_not_set) },
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp).clickable(onClick = onTelefonClick),
                        color = if (customer.telefon.isNotEmpty()) textPrimary else textSecondary,
                        fontSize = DetailUiConstants.BodySp
                    )
                    Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
                    Text(stringResource(R.string.label_notes_label), fontSize = DetailUiConstants.FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(
                        text = customer.notizen.ifEmpty { stringResource(R.string.label_not_set) },
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp),
                        color = if (customer.notizen.isNotEmpty()) textPrimary else textSecondary,
                        fontSize = DetailUiConstants.BodySp
                    )
                } else {
                    val currentFormState = editFormState ?: formState
                    CustomerStammdatenForm(
                        state = currentFormState,
                        onUpdate = { newState ->
                            if (isInEditMode) onUpdateEditFormState(newState) else formState = newState
                        }
                    )
                    Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
                    Text(stringResource(R.string.label_urlaub), fontSize = DetailUiConstants.FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    androidx.compose.material3.Button(
                        onClick = { customer?.id?.let { onUrlaubStartActivity(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colorResource(R.color.button_urlaub))
                    ) {
                        Text(stringResource(R.string.label_urlaub))
                    }
                    Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
                    if (isUploading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.label_foto_uploading), fontSize = 12.sp, color = primaryBlue)
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val stateForSave = editFormState ?: formState
                        androidx.compose.material3.Button(
                            onClick = {
                                val name = stateForSave.name.trim()
                                if (name.isEmpty()) {
                                    if (isInEditMode) onUpdateEditFormState(stateForSave.copy(errorMessage = validationNameMissing))
                                    else formState = formState.copy(errorMessage = validationNameMissing)
                                } else {
                                    onSave(
                                        buildMap {
                                            put("name", name)
                                            put("adresse", stateForSave.adresse.trim())
                                            put("stadt", stateForSave.stadt.trim())
                                            put("plz", stateForSave.plz.trim())
                                            put("telefon", stateForSave.telefon.trim())
                                            put("notizen", stateForSave.notizen.trim())
                                            put("kundenArt", stateForSave.kundenArt)
                                            put("kundenTyp", stateForSave.kundenTyp.name)
                                            put("defaultAbholungWochentag", stateForSave.abholungWochentage.firstOrNull() ?: -1)
                                            put("defaultAuslieferungWochentag", stateForSave.auslieferungWochentage.firstOrNull() ?: -1)
                                            put("defaultAbholungWochentage", stateForSave.abholungWochentage)
                                            put("defaultAuslieferungWochentage", stateForSave.auslieferungWochentage)
                                            put("kundennummer", stateForSave.kundennummer.trim())
                                            put("defaultUhrzeit", stateForSave.defaultUhrzeit.trim())
                                            put("tags", stateForSave.tagsInput.split(",").mapNotNull { it.trim().ifEmpty { null } })
                                            val hasTour = stateForSave.abholungWochentage.isNotEmpty() || stateForSave.tourStadt.isNotBlank() || stateForSave.tourZeitStart.isNotBlank() || stateForSave.tourZeitEnde.isNotBlank()
                                            val slotId = if (hasTour) (customer?.tourSlot?.id ?: "customer-${customer?.id}") else ""
                                            put("tourSlotId", slotId)
                                            put("tourSlot", if (hasTour) mapOf<String, Any>(
                                                "id" to slotId,
                                                "wochentag" to (stateForSave.abholungWochentage.firstOrNull() ?: -1),
                                                "stadt" to stateForSave.tourStadt.trim(),
                                                "zeitfenster" to mapOf(
                                                    "start" to stateForSave.tourZeitStart.trim(),
                                                    "ende" to stateForSave.tourZeitEnde.trim()
                                                )
                                            ) else emptyMap<String, Any>())
                                            put("intervalle", editIntervalle.map {
                                                mapOf(
                                                    "id" to it.id,
                                                    "abholungDatum" to it.abholungDatum,
                                                    "auslieferungDatum" to it.auslieferungDatum,
                                                    "wiederholen" to it.wiederholen,
                                                    "intervallTage" to it.intervallTage,
                                                    "intervallAnzahl" to it.intervallAnzahl,
                                                    "erstelltAm" to it.erstelltAm,
                                                    "terminRegelId" to it.terminRegelId
                                                )
                                            })
                                        },
                                        editIntervalle
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.termin_regel_button_save),
                                contentColor = Color.White
                            )
                        ) { Text(stringResource(R.string.btn_save)) }
                    }
                }

                Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
                Text(stringResource(R.string.label_termin_regel), fontSize = DetailUiConstants.SectionTitleSp, fontWeight = FontWeight.Bold, color = primaryBlue)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceWhite),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = DetailUiConstants.IntervalCardPaddingH,
                            top = DetailUiConstants.IntervalCardPaddingTop,
                            end = DetailUiConstants.IntervalCardPaddingH,
                            bottom = DetailUiConstants.IntervalCardPaddingBottom
                        )
                    ) {
                        val intervalleToShow = if (isInEditMode) editIntervalle else customer.intervalle
                        if (customer?.kundenTyp == KundenTyp.REGELMAESSIG && isInEditMode && editIntervalle.isNotEmpty()) {
                            val first = editIntervalle.first()
                            val tageAzuL = if (first.abholungDatum > 0) {
                                (kotlin.math.round((first.auslieferungDatum - first.abholungDatum) / 86400000.0)).toInt().coerceIn(0, 365)
                            } else 7
                            val zyklusTage = first.intervallTage.coerceIn(1, 365)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.label_l_termin), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                    Spacer(Modifier.height(4.dp))
                                    androidx.compose.material3.OutlinedTextField(
                                        value = tageAzuL.toString(),
                                        onValueChange = { s ->
                                            s.filter { it.isDigit() }.toIntOrNull()?.coerceIn(0, 365)?.let { newVal ->
                                                onTageAzuLZyklusChange?.invoke(newVal, zyklusTage)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stringResource(R.string.label_intervall), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                                    Spacer(Modifier.height(4.dp))
                                    androidx.compose.material3.OutlinedTextField(
                                        value = zyklusTage.toString(),
                                        onValueChange = { s ->
                                            s.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 365)?.let { newVal ->
                                                onTageAzuLZyklusChange?.invoke(tageAzuL, newVal)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                                    )
                                }
                            }
                            Spacer(Modifier.height(DetailUiConstants.IntervalRowSpacing))
                        }
                        // In beiden Modi nur Termin-Regel-Namen anzeigen (keine 365 Intervall-Zeilen bei täglich)
                        val distinctRegelIds = intervalleToShow.map { it.terminRegelId }.distinct()
                        distinctRegelIds.forEach { regelId ->
                            CustomerDetailRegelNameRow(
                                regelName = if (regelId.isBlank()) stringResource(R.string.label_not_set)
                                    else regelNameByRegelId[regelId] ?: stringResource(R.string.label_not_set),
                                isClickable = regelId.isNotBlank(),
                                primaryBlue = primaryBlue,
                                textSecondary = textSecondary,
                                onClick = { if (regelId.isNotBlank()) onRegelClick(regelId) },
                                showDeleteButton = isInEditMode && regelId.isNotBlank(),
                                onDeleteClick = if (isInEditMode && regelId.isNotBlank()) { { onRemoveRegel?.invoke(regelId) } } else null
                            )
                            Spacer(Modifier.height(DetailUiConstants.IntervalRowSpacing))
                        }
                        Spacer(Modifier.height(DetailUiConstants.FieldSpacing))
                        if (!isInEditMode) {
                            androidx.compose.material3.Button(onClick = onTerminAnlegen, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.label_termine_anlegen))
                            }
                        }
                    }
                }

                if (!isInEditMode && customer.fotoUrls.isNotEmpty()) {
                    Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
                    Text(stringResource(R.string.label_fotos), fontSize = DetailUiConstants.SectionTitleSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(customer.fotoUrls, key = { it }) { url ->
                            Card(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clickable(onClick = { onPhotoClick(url) }),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(R.drawable.ic_camera),
                                    error = painterResource(R.drawable.ic_camera)
                                )
                            }
                        }
                    }
                }
                if (isInEditMode) {
                    Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
                    androidx.compose.material3.OutlinedButton(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) {
                        Icon(painter = painterResource(R.drawable.ic_camera), contentDescription = null, Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.label_add_photo))
                    }
                }
            }
        }
    }
}

