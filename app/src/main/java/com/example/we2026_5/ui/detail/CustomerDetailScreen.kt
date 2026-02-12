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
import com.example.we2026_5.TerminRegelTyp
import com.example.we2026_5.R
import com.example.we2026_5.util.TerminAusKundeUtils
import com.example.we2026_5.util.DialogBaseHelper
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.intervallTageOrDefault
import com.example.we2026_5.util.tageAzuLOrDefault
import com.example.we2026_5.ui.addcustomer.AddCustomerState
import com.example.we2026_5.ui.wasch.BelegMonat
import java.util.concurrent.TimeUnit

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
    onSave: (Map<String, Any>, List<CustomerIntervall>, Int?) -> Unit,
    showSaveAndNext: Boolean = false,
    onSaveAndNext: ((Map<String, Any>, List<CustomerIntervall>, Int?) -> Unit)? = null,
    onDelete: () -> Unit,
    onTerminAnlegen: () -> Unit,
    onPauseCustomer: (pauseEndeWochen: Int?) -> Unit,
    onResumeCustomer: () -> Unit,
    onTakePhoto: () -> Unit,
    onAdresseClick: () -> Unit,
    onTelefonClick: () -> Unit,
    onPhotoClick: (String) -> Unit,
    onDeletePhoto: ((String) -> Unit)? = null,
    onDatumSelected: (Int, Boolean) -> Unit,
    onDeleteIntervall: ((Int) -> Unit)? = null,
    onRemoveRegel: ((String) -> Unit)? = null,
    onResetToAutomatic: () -> Unit = {},
    regelNameByRegelId: Map<String, String> = emptyMap(),
    onRegelClick: (String) -> Unit = {},
    onUrlaubStartActivity: (String) -> Unit = {},
    onErfassungClick: () -> Unit = {},
    onAddMonthlyIntervall: ((CustomerIntervall) -> Unit)? = null,
    onAddAbholungTermin: (Customer) -> Unit = {},
    onAddAusnahmeTermin: (Customer) -> Unit = {},
    /** A/L-Paare für „Alle Termine“-Block (Termine-Tab). */
    terminePairs365: List<Pair<Long, Long>> = emptyList(),
    /** Name der Tour-Liste, zu der der Kunde gehört (nur bei Tour-Kunden). Hinweis „Gehört zu Tour-Liste: …“. */
    tourListenName: String? = null,
    onDeleteNextTermin: (Long) -> Unit = {},
    onDeleteAusnahmeTermin: (com.example.we2026_5.AusnahmeTermin) -> Unit = {},
    onDeleteKundenTermin: (List<com.example.we2026_5.KundenTermin>) -> Unit = {},
    /** Belege für Tab Belege (vom ViewModel). */
    belegMonateForCustomer: List<BelegMonat> = emptyList(),
    onBelegErstellen: () -> Unit = {},
    onBelegClick: (BelegMonat) -> Unit = {}
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
                stateForSave.latitude?.let { put("latitude", it) }
                stateForSave.longitude?.let { put("longitude", it) }
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
                put("tageAzuL", stateForSave.tageAzuL?.coerceIn(0, 365) ?: 7)
                stateForSave.sameDayLStrategy?.let { put("sameDayLStrategy", it) }
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
                val tageAzuLForSave = stateForSave.tageAzuL?.coerceIn(0, 365) ?: 7
                val intervalleToSaveRaw = if (stateForSave.kundenTyp == KundenTyp.REGELMAESSIG && stateForSave.abholungWochentage.isNotEmpty()) {
                    val customerForIntervall = c.copy(
                        defaultAbholungWochentag = stateForSave.abholungWochentage.firstOrNull() ?: -1,
                        defaultAuslieferungWochentag = stateForSave.auslieferungWochentage.firstOrNull() ?: -1,
                        defaultAbholungWochentage = stateForSave.abholungWochentage,
                        defaultAuslieferungWochentage = stateForSave.auslieferungWochentage,
                        sameDayLStrategy = stateForSave.sameDayLStrategy,
                        tourSlotId = slotId
                    )
                    val mainFromForm = TerminAusKundeUtils.erstelleIntervalleAusKunde(customerForIntervall, startDatumA, tageAzuLForSave, stateForSave.intervallTage ?: 7)
                    val fromRules = editIntervalle.filter { it.terminRegelId.isNotBlank() || it.regelTyp == TerminRegelTyp.MONTHLY_WEEKDAY }
                    mainFromForm + fromRules
                } else editIntervalle
                val intervalleToSave = intervalleToSaveRaw.map { iv ->
                    if (iv.regelTyp == TerminRegelTyp.WEEKLY && iv.abholungDatum > 0) iv
                    else if (iv.abholungDatum > 0) iv.copy(
                        auslieferungDatum = TerminBerechnungUtils.getStartOfDay(iv.abholungDatum + TimeUnit.DAYS.toMillis(tageAzuLForSave.toLong()))
                    ) else iv
                }
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
            if (andNext && onSaveAndNext != null) onSaveAndNext(updates, editIntervalle, stateForSave.tageAzuL)
            else onSave(updates, editIntervalle, stateForSave.tageAzuL)
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
            var showNeuerTerminArtSheet by remember { mutableStateOf(false) }
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
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        text = { Text(stringResource(R.string.tab_belege)) }
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
                        onDeletePhoto = onDeletePhoto,
                        isUploading = isUploading
                    )
                    1 -> CustomerDetailTermineTab(
                        isAdmin = isAdmin,
                        customer = customer,
                        isInEditMode = isInEditMode,
                        currentFormState = currentFormState,
                        onUpdateFormState = onUpdateFormState,
                        onStartDatumClick = onStartDatumClick,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        surfaceWhite = surfaceWhite,
                        primaryBlue = primaryBlue,
                        onPauseCustomer = onPauseCustomer,
                        onResumeCustomer = onResumeCustomer,
                        terminePairs365 = terminePairs365,
                        showAddMonthlySheet = showAddMonthlySheet,
                        onDismissAddMonthlySheet = { showAddMonthlySheet = false },
                        onConfirmAddMonthly = {
                            onAddMonthlyIntervall?.invoke(it)
                            showAddMonthlySheet = false
                        },
                        tourSlotId = customer.tourSlotId,
                        showNeuerTerminArtSheet = showNeuerTerminArtSheet,
                        onDismissNeuerTerminArtSheet = { showNeuerTerminArtSheet = false },
                        onNeuerTerminArtSelected = { art ->
                            showNeuerTerminArtSheet = false
                            when (art) {
                                NeuerTerminArt.REGELMAESSIG -> onEdit()
                                NeuerTerminArt.MONATLICH -> showAddMonthlySheet = true
                                NeuerTerminArt.EINMALIG_KUNDEN_TERMIN -> customer?.let { onAddAbholungTermin(it) }
                                NeuerTerminArt.EINMALIG_AUSNAHME -> customer?.let { onAddAusnahmeTermin(it) }
                                NeuerTerminArt.URLAUB -> customer?.id?.let { onUrlaubStartActivity(it) }
                            }
                        },
                        onNeuerTerminClick = { showNeuerTerminArtSheet = true },
                        tourListenName = tourListenName,
                        typeLabel = typeLabel,
                        onDeleteNextTermin = onDeleteNextTermin,
                        ausnahmeTermine = customer?.ausnahmeTermine ?: emptyList(),
                        onDeleteAusnahmeTermin = onDeleteAusnahmeTermin,
                        kundenTermine = customer?.kundenTermine ?: emptyList(),
                        onAddAbholungTermin = { customer?.let { onAddAbholungTermin(it) } },
                        onDeleteKundenTermin = onDeleteKundenTermin
                    )
                    2 -> CustomerDetailBelegeTab(
                        customer = customer,
                        belegMonate = belegMonateForCustomer,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onBelegErstellen = onBelegErstellen,
                        onBelegClick = onBelegClick
                    )
                }
            }
        }
    }
}

