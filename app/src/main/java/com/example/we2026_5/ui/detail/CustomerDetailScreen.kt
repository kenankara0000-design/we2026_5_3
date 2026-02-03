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

// Einheitliche UI-Werte (Abstände, Schriftgrößen)
private val SectionSpacing = 20.dp
private val FieldSpacing = 12.dp
private val SectionTitleSp = 16.sp
private val FieldLabelSp = 14.sp
private val BodySp = 14.sp
private val IntervalCardPaddingH = 12.dp
private val IntervalCardPaddingTop = 6.dp
private val IntervalCardPaddingBottom = 12.dp
private val IntervalRowPaddingVertical = 4.dp
private val IntervalRowSpacing = 4.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customer: Customer?,
    isInEditMode: Boolean,
    editIntervalle: List<CustomerIntervall>,
    isLoading: Boolean,
    isUploading: Boolean = false,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSave: (Map<String, Any>, List<CustomerIntervall>) -> Unit,
    onDelete: () -> Unit,
    onTerminAnlegen: () -> Unit,
    onPauseCustomer: () -> Unit,
    onResumeCustomer: () -> Unit,
    onTakePhoto: () -> Unit,
    onAdresseClick: () -> Unit,
    onTelefonClick: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onDatumSelected: (Int, Boolean) -> Unit,
    onDeleteIntervall: ((Int) -> Unit)? = null,
    onRemoveRegel: ((String) -> Unit)? = null,
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

    var editName by remember(customer?.id, isInEditMode) { mutableStateOf(customer?.name ?: "") }
    var editAdresse by remember(customer?.id, isInEditMode) { mutableStateOf(customer?.adresse ?: "") }
    var editTelefon by remember(customer?.id, isInEditMode) { mutableStateOf(customer?.telefon ?: "") }
    var editNotizen by remember(customer?.id, isInEditMode) { mutableStateOf(customer?.notizen ?: "") }
    var editKundenArt by remember(customer?.id, isInEditMode) { mutableStateOf(customer?.kundenArt ?: "Gewerblich") }
    var editKundenTyp by remember(customer?.id, isInEditMode) { mutableStateOf(customer?.kundenTyp ?: KundenTyp.REGELMAESSIG) }
    var editAbholungWochentag by remember(customer?.id, isInEditMode) { mutableStateOf(customer?.defaultAbholungWochentag ?: -1) }
    var editAuslieferungWochentag by remember(customer?.id, isInEditMode) { mutableStateOf(customer?.defaultAuslieferungWochentag ?: -1) }
    var editIntervallTageUnregel by remember(customer?.id, isInEditMode) {
        mutableStateOf((customer?.intervallTage?.takeIf { it in 1..365 } ?: 7))
    }
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    val validationNameMissing = stringResource(R.string.validation_name_missing)

    val weekdays = listOf(
        "MO" to R.string.label_weekday_short_mo,
        "DI" to R.string.label_weekday_short_tu,
        "MI" to R.string.label_weekday_short_mi,
        "DO" to R.string.label_weekday_short_do,
        "FR" to R.string.label_weekday_short_fr,
        "SA" to R.string.label_weekday_short_sa,
        "SO" to R.string.label_weekday_short_su
    )

    val typeLabel = when (customer?.kundenArt) {
        "Privat" -> stringResource(R.string.label_type_privat)
        "Liste" -> stringResource(R.string.label_type_liste)
        else -> stringResource(R.string.label_type_gewerblich)
    }
    val typeLetter = when (customer?.kundenArt) {
        "Privat" -> stringResource(R.string.label_type_p_letter)
        "Liste" -> stringResource(R.string.label_type_l_letter)
        else -> stringResource(R.string.label_type_g)
    }
    val typeColor = when (customer?.kundenArt) {
        "Privat" -> colorResource(R.color.button_privat_glossy)
        "Liste" -> colorResource(R.color.button_liste_glossy)
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
                        horizontalArrangement = Arrangement.spacedBy(FieldSpacing)
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
                    Spacer(Modifier.height(SectionSpacing))
            if (!isInEditMode && customer != null) {
                CustomerStatusSection(
                    customer = customer,
                    onPauseCustomer = onPauseCustomer,
                    onResumeCustomer = onResumeCustomer,
                    textPrimary = textPrimary,
                    surfaceWhite = surfaceWhite
                )
                Spacer(Modifier.height(SectionSpacing))
                val nextTermin = customer.getFaelligAm()
                Text(stringResource(R.string.label_next_termin), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text(
                    text = if (nextTermin > 0) DateFormatter.formatDateWithWeekday(nextTermin) else stringResource(R.string.label_not_set),
                    modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp),
                    color = if (nextTermin > 0) textPrimary else textSecondary,
                    fontSize = BodySp
                )
                Spacer(Modifier.height(FieldSpacing))
            }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(FieldSpacing)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_customer_type), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(text = typeLabel, modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp), color = textPrimary, fontSize = BodySp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.label_kunden_typ), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(
                                text = when (customer.kundenTyp) {
                                    com.example.we2026_5.KundenTyp.REGELMAESSIG -> stringResource(R.string.label_kunden_typ_regelmaessig)
                                    com.example.we2026_5.KundenTyp.UNREGELMAESSIG -> stringResource(R.string.label_kunden_typ_unregelmaessig)
                                },
                                modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp),
                                color = textPrimary,
                                fontSize = BodySp
                            )
                        }
                        if (customer.defaultAbholungWochentag in 0..6 || customer.defaultAuslieferungWochentag in 0..6) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.label_abholung_auslieferung_tag), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                                val wochen = listOf("Mo","Di","Mi","Do","Fr","Sa","So")
                                val a = if (customer.defaultAbholungWochentag in 0..6) wochen[customer.defaultAbholungWochentag] + " A" else ""
                                val l = if (customer.defaultAuslieferungWochentag in 0..6) wochen[customer.defaultAuslieferungWochentag] + " L" else ""
                                Text(text = listOf(a, l).filter { it.isNotEmpty() }.joinToString(" / "), modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp), color = textPrimary, fontSize = BodySp)
                            }
                        }
                    }
                    Spacer(Modifier.height(FieldSpacing))
                    Text(stringResource(R.string.label_address_label), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(
                        text = customer.adresse.ifEmpty { stringResource(R.string.label_not_set) },
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp).clickable(onClick = onAdresseClick),
                        color = if (customer.adresse.isNotEmpty()) textPrimary else textSecondary,
                        fontSize = BodySp
                    )
                    Spacer(Modifier.height(FieldSpacing))
                    Text(stringResource(R.string.label_phone_label), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(
                        text = customer.telefon.ifEmpty { stringResource(R.string.label_not_set) },
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp).clickable(onClick = onTelefonClick),
                        color = if (customer.telefon.isNotEmpty()) textPrimary else textSecondary,
                        fontSize = BodySp
                    )
                    Spacer(Modifier.height(FieldSpacing))
                    Text(stringResource(R.string.label_notes_label), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text(
                        text = customer.notizen.ifEmpty { stringResource(R.string.label_not_set) },
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFE0E0E0)).padding(12.dp),
                        color = if (customer.notizen.isNotEmpty()) textPrimary else textSecondary,
                        fontSize = BodySp
                    )
                } else {
                    Text(stringResource(R.string.label_name), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it; nameError = null },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = nameError != null,
                        supportingText = nameError?.let { err -> { Text(err, color = statusOverdue) } }
                    )
                    Spacer(Modifier.height(FieldSpacing))
                    Text(stringResource(R.string.label_customer_type), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { editKundenArt = "Gewerblich" }
                        ) {
                            RadioButton(selected = editKundenArt == "Gewerblich", onClick = { editKundenArt = "Gewerblich" })
                            Text(stringResource(R.string.label_type_gewerblich), color = textPrimary, fontSize = BodySp, maxLines = 1)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { editKundenArt = "Privat" }
                        ) {
                            RadioButton(selected = editKundenArt == "Privat", onClick = { editKundenArt = "Privat" })
                            Text(stringResource(R.string.label_type_privat), color = textPrimary, fontSize = BodySp, maxLines = 1)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { editKundenArt = "Liste" }
                        ) {
                            RadioButton(selected = editKundenArt == "Liste", onClick = { editKundenArt = "Liste" })
                            Text(stringResource(R.string.label_type_liste), color = textPrimary, fontSize = BodySp, maxLines = 1)
                        }
                    }
                    Spacer(Modifier.height(FieldSpacing))
                    Text(stringResource(R.string.label_kunden_typ), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { editKundenTyp = KundenTyp.REGELMAESSIG }) {
                            RadioButton(selected = editKundenTyp == KundenTyp.REGELMAESSIG, onClick = { editKundenTyp = KundenTyp.REGELMAESSIG })
                            Text(stringResource(R.string.label_kunden_typ_regelmaessig), color = textPrimary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { editKundenTyp = KundenTyp.UNREGELMAESSIG }) {
                            RadioButton(selected = editKundenTyp == KundenTyp.UNREGELMAESSIG, onClick = { editKundenTyp = KundenTyp.UNREGELMAESSIG })
                            Text(stringResource(R.string.label_kunden_typ_unregelmaessig), color = textPrimary)
                        }
                    }
                    Spacer(Modifier.height(FieldSpacing))
                    Text(stringResource(R.string.label_address_label), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    OutlinedTextField(value = editAdresse, onValueChange = { editAdresse = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(FieldSpacing))
                    Text(stringResource(R.string.label_phone_label), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    OutlinedTextField(value = editTelefon, onValueChange = { editTelefon = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(FieldSpacing))
                    Text(stringResource(R.string.label_notes_label), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    OutlinedTextField(value = editNotizen, onValueChange = { editNotizen = it }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    
                    Spacer(Modifier.height(FieldSpacing))
                    Text(stringResource(R.string.label_default_pickup_day), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        weekdays.forEachIndexed { index, (_, labelResId) ->
                            FilterChip(
                                selected = editAbholungWochentag == index,
                                onClick = { editAbholungWochentag = if (editAbholungWochentag == index) -1 else index },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(labelResId), maxLines = 1) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.label_default_delivery_day), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        weekdays.forEachIndexed { index, (_, labelResId) ->
                            FilterChip(
                                selected = editAuslieferungWochentag == index,
                                onClick = { editAuslieferungWochentag = if (editAuslieferungWochentag == index) -1 else index },
                                modifier = Modifier.weight(1f),
                                label = { Text(stringResource(labelResId), maxLines = 1) }
                            )
                        }
                    }
                    if (editKundenTyp == KundenTyp.UNREGELMAESSIG) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.label_a_plus_tage_l), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                        OutlinedTextField(
                            value = editIntervallTageUnregel.toString(),
                            onValueChange = { s ->
                                val v = s.filter { it.isDigit() }.toIntOrNull()?.coerceIn(1, 365) ?: 7
                                editIntervallTageUnregel = v
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = { Text(stringResource(R.string.hint_a_plus_tage), color = textSecondary, fontSize = 12.sp) }
                        )
                    }
                    Spacer(Modifier.height(FieldSpacing))
                    Text(stringResource(R.string.label_urlaub), fontSize = FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    androidx.compose.material3.Button(
                        onClick = { customer?.id?.let { onUrlaubStartActivity(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = colorResource(R.color.button_urlaub))
                    ) {
                        Text(stringResource(R.string.label_urlaub))
                    }
                    Spacer(Modifier.height(SectionSpacing))
                    if (isUploading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.label_foto_uploading), fontSize = 12.sp, color = primaryBlue)
                        Spacer(Modifier.height(8.dp))
                    }
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Button(
                            onClick = {
                                val name = editName.trim()
                                if (name.isEmpty()) nameError = validationNameMissing
                                else {
                                    nameError = null
                                    onSave(
                                        mapOf(
                                            "name" to name,
                                            "adresse" to editAdresse.trim(),
                                            "telefon" to editTelefon.trim(),
                                            "notizen" to editNotizen.trim(),
                                            "kundenArt" to editKundenArt,
                                            "kundenTyp" to editKundenTyp.name,
                                            "defaultAbholungWochentag" to editAbholungWochentag,
                                            "defaultAuslieferungWochentag" to editAuslieferungWochentag,
                                            "intervallTage" to editIntervallTageUnregel,
                                            "intervalle" to editIntervalle.map {
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
                                            }
                                        ),
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

                Spacer(Modifier.height(SectionSpacing))
                Text(stringResource(R.string.label_termin_regel), fontSize = SectionTitleSp, fontWeight = FontWeight.Bold, color = primaryBlue)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = surfaceWhite),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            start = IntervalCardPaddingH,
                            top = IntervalCardPaddingTop,
                            end = IntervalCardPaddingH,
                            bottom = IntervalCardPaddingBottom
                        )
                    ) {
                        val intervalleToShow = if (isInEditMode) editIntervalle else customer.intervalle
                        // In beiden Modi nur Termin-Regel-Namen anzeigen (keine 365 Intervall-Zeilen bei täglich)
                        val distinctRegelIds = intervalleToShow.map { it.terminRegelId }.distinct()
                        distinctRegelIds.forEach { regelId ->
                            RegelNameRow(
                                regelName = if (regelId.isBlank()) stringResource(R.string.label_not_set)
                                    else regelNameByRegelId[regelId] ?: stringResource(R.string.label_not_set),
                                isClickable = regelId.isNotBlank(),
                                primaryBlue = primaryBlue,
                                textSecondary = textSecondary,
                                onClick = { if (regelId.isNotBlank()) onRegelClick(regelId) },
                                showDeleteButton = isInEditMode && regelId.isNotBlank(),
                                onDeleteClick = if (isInEditMode && regelId.isNotBlank()) { { onRemoveRegel?.invoke(regelId) } } else null
                            )
                            Spacer(Modifier.height(IntervalRowSpacing))
                        }
                        Spacer(Modifier.height(FieldSpacing))
                        if (!isInEditMode) {
                            androidx.compose.material3.Button(onClick = onTerminAnlegen, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.label_termine_anlegen))
                            }
                        }
                    }
                }

                if (!isInEditMode && customer.fotoUrls.isNotEmpty()) {
                    Spacer(Modifier.height(SectionSpacing))
                    Text(stringResource(R.string.label_fotos), fontSize = SectionTitleSp, fontWeight = FontWeight.Bold, color = textPrimary)
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
                    Spacer(Modifier.height(SectionSpacing))
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

@Composable
private fun CustomerStatusSection(
    customer: Customer,
    onPauseCustomer: () -> Unit,
    onResumeCustomer: () -> Unit,
    textPrimary: Color,
    surfaceWhite: Color
) {
    val statusText = when (customer.status) {
        CustomerStatus.AKTIV -> stringResource(R.string.customer_status_active)
        CustomerStatus.PAUSIERT -> stringResource(R.string.customer_status_paused)
        CustomerStatus.ADHOC -> stringResource(R.string.customer_status_adhoc)
    }
    val statusColor = when (customer.status) {
        CustomerStatus.AKTIV -> colorResource(R.color.status_done)
        CustomerStatus.PAUSIERT -> colorResource(R.color.status_warning)
        CustomerStatus.ADHOC -> colorResource(R.color.status_info)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = surfaceWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.customer_status_title),
                fontSize = SectionTitleSp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = statusText, color = statusColor, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.customer_status_current, statusText),
                    color = textPrimary,
                    fontSize = BodySp
                )
            }
            if (customer.pauseEnde > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.customer_status_paused_until,
                        DateFormatter.formatDateWithWeekday(customer.pauseEnde)
                    ),
                    color = textPrimary,
                    fontSize = BodySp
                )
            }
            if (customer.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.customer_tags_label, customer.tags.joinToString(", ")),
                    color = textPrimary,
                    fontSize = BodySp
                )
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = if (customer.status == CustomerStatus.PAUSIERT) onResumeCustomer else onPauseCustomer,
                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.primary_blue))
            ) {
                Text(
                    text = if (customer.status == CustomerStatus.PAUSIERT) {
                        stringResource(R.string.customer_btn_resume)
                    } else {
                        stringResource(R.string.customer_btn_pause)
                    }
                )
            }
        }
    }
}

@Composable
fun RegelNameRow(
    regelName: String,
    isClickable: Boolean,
    primaryBlue: Color,
    textSecondary: Color,
    onClick: () -> Unit,
    showDeleteButton: Boolean = false,
    onDeleteClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isClickable, onClick = onClick)
            .padding(vertical = IntervalRowPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = regelName,
            fontSize = BodySp,
            fontWeight = FontWeight.Medium,
            color = if (isClickable) primaryBlue else textSecondary,
            modifier = Modifier.weight(1f)
        )
        if (showDeleteButton && onDeleteClick != null) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.label_delete),
                    tint = colorResource(R.color.status_overdue),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun CustomerIntervallRow(
    intervall: CustomerIntervall,
    isEditMode: Boolean,
    onAbholungClick: () -> Unit,
    onAuslieferungClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val abholungText = if (intervall.abholungDatum > 0) DateFormatter.formatDateWithLeadingZeros(intervall.abholungDatum) else stringResource(R.string.label_not_set)
    val auslieferungText = if (intervall.auslieferungDatum > 0) DateFormatter.formatDateWithLeadingZeros(intervall.auslieferungDatum) else stringResource(R.string.label_not_set)
    val abholungIsPlaceholder = intervall.abholungDatum <= 0
    val auslieferungIsPlaceholder = intervall.auslieferungDatum <= 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = if (isEditMode) Modifier.clickable(onClick = onAbholungClick) else Modifier) {
                Text(stringResource(R.string.label_abholung_date), fontSize = 12.sp, color = textSecondary)
                Text(abholungText, fontSize = BodySp, color = if (abholungIsPlaceholder) textSecondary else textPrimary)
            }
            Column(modifier = if (isEditMode) Modifier.clickable(onClick = onAuslieferungClick) else Modifier) {
                Text(stringResource(R.string.label_auslieferung_date), fontSize = 12.sp, color = textSecondary)
                Text(auslieferungText, fontSize = BodySp, color = if (auslieferungIsPlaceholder) textSecondary else textPrimary)
            }
        }
        if (isEditMode && onDeleteClick != null) {
            Spacer(Modifier.size(8.dp))
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.label_delete),
                    tint = colorResource(R.color.status_overdue),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}