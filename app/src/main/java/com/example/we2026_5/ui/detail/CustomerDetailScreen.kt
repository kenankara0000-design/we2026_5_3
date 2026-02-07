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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.R
import com.example.we2026_5.util.TerminAusKundeUtils
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.DialogBaseHelper
import com.example.we2026_5.util.intervallTageOrDefault
import com.example.we2026_5.util.tageAzuLOrDefault
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
    showSaveAndNext: Boolean = false,
    onSaveAndNext: ((Map<String, Any>, List<CustomerIntervall>) -> Unit)? = null,
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
    onResetToAutomatic: () -> Unit = {},
    regelNameByRegelId: Map<String, String> = emptyMap(),
    onRegelClick: (String) -> Unit = {},
    onUrlaubStartActivity: (String) -> Unit = {},
    onErfassungClick: () -> Unit = {}
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
                    alias = customer.alias,
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
                    tourZeitEnde = customer.tourSlot?.zeitfenster?.ende ?: "",
                    ohneTour = customer.ohneTour,
                    erstelltAm = customer.erstelltAm
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
            CustomerDetailTopBar(
                typeLetter = typeLetter,
                typeColor = typeColor,
                displayName = customer?.displayName ?: stringResource(R.string.label_customer_name),
                isInEditMode = isInEditMode,
                statusOverdue = statusOverdue,
                onBack = onBack,
                onDelete = onDelete,
                overflowMenuExpanded = overflowMenuExpanded,
                onOverflowMenuDismiss = { overflowMenuExpanded = false },
                onOverflowMenuExpand = { overflowMenuExpanded = true }
            )
        }
    ) { paddingValues ->
        val contentModifier = Modifier.fillMaxWidth().padding(paddingValues).padding(32.dp)
        if (isLoading) {
            CustomerDetailLoadingView(modifier = contentModifier, textSecondary = textSecondary)
        } else if (customer == null) {
            CustomerDetailNotFoundView(modifier = contentModifier, textSecondary = textSecondary)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (!isInEditMode) {
                    CustomerDetailActionsRow(
                        primaryBlue = primaryBlue,
                        onUrlaub = { customer.id?.let { onUrlaubStartActivity(it) } },
                        onEdit = onEdit,
                        isUploading = isUploading
                    )
                CustomerDetailStatusSection(
                    customer = customer,
                    onPauseCustomer = onPauseCustomer,
                    onResumeCustomer = onResumeCustomer,
                    textPrimary = textPrimary,
                    surfaceWhite = surfaceWhite
                )
                Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
                val nextTermin = com.example.we2026_5.util.TerminBerechnungUtils.naechstesFaelligAmDatum(customer)
                CustomerDetailNaechsterTermin(
                    nextTerminMillis = nextTermin,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
                CustomerDetailKundenTypSection(
                    typeLabel = typeLabel,
                    kundenTyp = customer.kundenTyp,
                    effectiveAbholungWochentage = customer.effectiveAbholungWochentage,
                    effectiveAuslieferungWochentage = customer.effectiveAuslieferungWochentage,
                    textPrimary = textPrimary
                )
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
                    val ctx = LocalContext.current
                    CustomerStammdatenForm(
                        state = currentFormState,
                        onUpdate = { newState ->
                            if (isInEditMode) onUpdateEditFormState(newState) else formState = newState
                        },
                        onStartDatumClick = {
                            DialogBaseHelper.showDatePickerDialog(
                                context = ctx,
                                initialDate = currentFormState.erstelltAm.takeIf { it > 0 } ?: System.currentTimeMillis(),
                                title = ctx.getString(R.string.label_startdatum_a),
                                onDateSelected = { selected ->
                                    val newState = currentFormState.copy(erstelltAm = TerminBerechnungUtils.getStartOfDay(selected))
                                    if (isInEditMode) onUpdateEditFormState(newState) else formState = newState
                                }
                            )
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
                    Text(stringResource(R.string.wasch_erfassungen), fontSize = DetailUiConstants.FieldLabelSp, fontWeight = FontWeight.Bold, color = textPrimary)
                    androidx.compose.material3.OutlinedButton(
                        onClick = onErfassungClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.wasch_artikel_hinzufuegen))
                    }
                    Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
                    if (isUploading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.label_foto_uploading), fontSize = 12.sp, color = primaryBlue)
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val stateForSave = editFormState ?: formState
                        val performSave: (Boolean) -> Unit = { andNext ->
                            val name = stateForSave.name.trim()
                            if (name.isEmpty()) {
                                if (isInEditMode) onUpdateEditFormState(stateForSave.copy(errorMessage = validationNameMissing))
                                else formState = formState.copy(errorMessage = validationNameMissing)
                            } else {
                                val updates = buildMap {
                                    put("name", name)
                                    put("alias", stateForSave.alias.trim())
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
                                            put("ohneTour", stateForSave.ohneTour)
                                            put("erstelltAm", stateForSave.erstelltAm.takeIf { it > 0 } ?: TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis()))
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
                                            val startDatumA = stateForSave.erstelltAm.takeIf { it > 0 } ?: TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
                                            // Haupt-Intervall immer aus den zwei Originalfeldern (Intervall, L-Termin) im Formular; ggf. weitere Regeln anhängen.
                                            val intervalleToSave = if (stateForSave.kundenTyp == KundenTyp.REGELMAESSIG && stateForSave.abholungWochentage.isNotEmpty() && customer != null) {
                                                val customerForIntervall = customer.copy(
                                                    defaultAbholungWochentag = stateForSave.abholungWochentage.firstOrNull() ?: -1,
                                                    defaultAuslieferungWochentag = stateForSave.auslieferungWochentage.firstOrNull() ?: -1,
                                                    defaultAbholungWochentage = stateForSave.abholungWochentage,
                                                    defaultAuslieferungWochentage = stateForSave.auslieferungWochentage,
                                                    tourSlotId = slotId
                                                )
                                                val mainFromForm = TerminAusKundeUtils.erstelleIntervallAusKunde(customerForIntervall, startDatumA, stateForSave.tageAzuL, stateForSave.intervallTage)
                                                val fromRules = editIntervalle.filter { it.terminRegelId.isNotBlank() }
                                                (mainFromForm?.let { listOf(it) } ?: emptyList()) + fromRules
                                    } else editIntervalle
                                    put("intervalle", intervalleToSave.map {
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
                                }
                                if (andNext && onSaveAndNext != null) onSaveAndNext(updates, editIntervalle)
                                else onSave(updates, editIntervalle)
                            }
                        }
                        androidx.compose.material3.Button(
                            onClick = { performSave(false) },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = colorResource(R.color.termin_regel_button_save),
                                contentColor = Color.White
                            )
                        ) { Text(stringResource(R.string.btn_save)) }
                        if (showSaveAndNext && onSaveAndNext != null) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = { performSave(true) },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.btn_save_and_next_customer)) }
                        }
                    }
                }

                Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
                val intervalleToShow = if (isInEditMode) editIntervalle else customer.intervalle
                CustomerDetailTerminRegelCard(
                    intervalleToShow = intervalleToShow,
                    isInEditMode = isInEditMode,
                    kundenTyp = customer.kundenTyp,
                    regelNameByRegelId = regelNameByRegelId,
                    primaryBlue = primaryBlue,
                    textSecondary = textSecondary,
                    surfaceWhite = surfaceWhite,
                    onRegelClick = onRegelClick,
                    onRemoveRegel = onRemoveRegel,
                    onResetToAutomatic = onResetToAutomatic,
                    onTerminAnlegen = onTerminAnlegen
                )
                CustomerDetailFotosSection(
                    fotoUrls = customer.fotoUrls,
                    isInEditMode = isInEditMode,
                    textPrimary = textPrimary,
                    onPhotoClick = onPhotoClick,
                    onTakePhoto = onTakePhoto
                )
            }
        }
    }
}

