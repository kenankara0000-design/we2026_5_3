package com.example.we2026_5.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.R
import com.example.we2026_5.util.TerminAusKundeUtils
import com.example.we2026_5.util.DialogBaseHelper
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.intervallTageOrDefault
import com.example.we2026_5.util.tageAzuLOrDefault
import com.example.we2026_5.ui.addcustomer.AddCustomerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    isAdmin: Boolean,
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
    onErfassungClick: () -> Unit = {},
    onAddMonthlyIntervall: ((CustomerIntervall) -> Unit)? = null
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

    var selectedTabIndex by remember { mutableStateOf(0) }
    val performSave = performSave@ { andNext: Boolean ->
        val c = customer ?: return@performSave
        val stateForSave = editFormState ?: formState
        val name = stateForSave.name.trim()
        if (name.isEmpty()) {
            if (isInEditMode) onUpdateEditFormState(stateForSave.copy(errorMessage = validationNameMissing))
            else formState = formState.copy(errorMessage = validationNameMissing)
        } else {
            val updates = buildMap<String, Any> {
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
                val slotId = if (hasTour) (c.tourSlot?.id ?: "customer-${c.id}") else ""
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
                val intervalleToSave = if (stateForSave.kundenTyp == KundenTyp.REGELMAESSIG && stateForSave.abholungWochentage.isNotEmpty()) {
                    val customerForIntervall = c.copy(
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
                        "terminRegelId" to it.terminRegelId,
                        "regelTyp" to it.regelTyp.name,
                        "tourSlotId" to it.tourSlotId,
                        "zyklusTage" to it.zyklusTage,
                        "monthWeekOfMonth" to it.monthWeekOfMonth,
                        "monthWeekday" to it.monthWeekday
                    )
                })
            }
            if (andNext && onSaveAndNext != null) onSaveAndNext(updates, editIntervalle)
            else onSave(updates, editIntervalle)
        }
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
                onOverflowMenuExpand = { overflowMenuExpanded = true },
                onSave = if (customer != null && isInEditMode) {{ performSave(false) }} else null,
                showSaveAndNext = showSaveAndNext,
                onSaveAndNext = if (customer != null && isInEditMode && showSaveAndNext && onSaveAndNext != null) {{ performSave(true) }} else null
            )
        }
    ) { paddingValues ->
        val contentModifier = Modifier.fillMaxWidth().padding(paddingValues).padding(32.dp)
        if (isLoading) {
            CustomerDetailLoadingView(modifier = contentModifier, textSecondary = textSecondary)
        } else if (customer == null) {
            CustomerDetailNotFoundView(modifier = contentModifier, textSecondary = textSecondary)
        } else {
            val context = LocalContext.current
            var showAddMonthlySheet by remember { mutableStateOf(false) }
            val currentFormState = editFormState ?: formState
            val onUpdateFormState: (AddCustomerState) -> Unit = { newState ->
                if (isInEditMode) onUpdateEditFormState(newState) else formState = newState
            }
            val onStartDatumClick: () -> Unit = {
                DialogBaseHelper.showDatePickerDialog(
                    context = context,
                    initialDate = currentFormState.erstelltAm.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    title = context.getString(R.string.label_startdatum_a),
                    onDateSelected = { selected ->
                        onUpdateFormState(currentFormState.copy(erstelltAm = TerminBerechnungUtils.getStartOfDay(selected)))
                    }
                )
            }
            Column(modifier = Modifier.fillMaxWidth().padding(paddingValues).padding(16.dp)) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text(stringResource(R.string.tab_stammdaten)) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text(stringResource(R.string.tab_termine_tour)) }
                    )
                }
                when (selectedTabIndex) {
                    0 -> CustomerDetailStammdatenTab(
                        isAdmin = isAdmin,
                        customer = customer,
                        isInEditMode = isInEditMode,
                        currentFormState = currentFormState,
                        onUpdateFormState = onUpdateFormState,
                        primaryBlue = primaryBlue,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onUrlaub = { customer.id?.let { onUrlaubStartActivity(it) } },
                        onEdit = onEdit,
                        onAdresseClick = onAdresseClick,
                        onTelefonClick = onTelefonClick,
                        onErfassungClick = onErfassungClick,
                        onTakePhoto = onTakePhoto,
                        onPhotoClick = onPhotoClick,
                        isUploading = isUploading
                    )
                    1 -> CustomerDetailTermineTab(
                        isAdmin = isAdmin,
                        customer = customer,
                        isInEditMode = isInEditMode,
                        intervalleToShow = if (isInEditMode) editIntervalle else customer.intervalle,
                        currentFormState = currentFormState,
                        onUpdateFormState = onUpdateFormState,
                        onStartDatumClick = onStartDatumClick,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        surfaceWhite = surfaceWhite,
                        primaryBlue = primaryBlue,
                        typeLabel = typeLabel,
                        regelNameByRegelId = regelNameByRegelId,
                        onPauseCustomer = onPauseCustomer,
                        onResumeCustomer = onResumeCustomer,
                        onRegelClick = onRegelClick,
                        onRemoveRegel = onRemoveRegel,
                        onResetToAutomatic = onResetToAutomatic,
                        onTerminAnlegen = onTerminAnlegen,
                        onAddMonthlyClick = onAddMonthlyIntervall?.let { { showAddMonthlySheet = true } },
                        tourSlotId = customer.tourSlotId,
                        onAddMonthlyIntervall = onAddMonthlyIntervall,
                        showAddMonthlySheet = showAddMonthlySheet,
                        onDismissAddMonthlySheet = { showAddMonthlySheet = false },
                        onConfirmAddMonthly = {
                            onAddMonthlyIntervall?.invoke(it)
                            showAddMonthlySheet = false
                        }
                    )
                }
            }
        }
    }
}

