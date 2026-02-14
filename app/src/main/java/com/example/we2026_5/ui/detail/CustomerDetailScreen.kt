package com.example.we2026_5.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.R
import com.example.we2026_5.util.DialogBaseHelper
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.intervallTageOrDefault
import com.example.we2026_5.util.tageAzuLOrDefault
import com.example.we2026_5.ui.addcustomer.AddCustomerState
import com.example.we2026_5.ui.common.DetailUiConstants
import com.example.we2026_5.ui.wasch.BelegMonat

/** Phase C2: Screen nimmt gebündelten [CustomerDetailUiState] und [CustomerDetailActions]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    state: CustomerDetailUiState,
    actions: CustomerDetailActions
) {
    val isAdmin = state.isAdmin
    val customer = state.customer
    val isInEditMode = state.isInEditMode
    val editIntervalle = state.editIntervalle
    val editFormState = state.editFormState
    val isLoading = state.isLoading
    val isUploading = state.isUploading
    val isOffline = state.isOffline
    val showSaveAndNext = state.showSaveAndNext
    val terminePairs365 = state.terminePairs365
    val tourListenName = state.tourListenName
    val belegMonateForCustomer = state.belegMonateForCustomer
    val belegMonateErledigtForCustomer = state.belegMonateErledigtForCustomer
    val onUpdateEditFormState = actions.onUpdateEditFormState
    val onBack = actions.onBack
    val onEdit = actions.onEdit
    val onPerformSave = actions.onPerformSave
    val onDelete = actions.onDelete
    val onTerminAnlegen = actions.onTerminAnlegen
    val onPauseCustomer = actions.onPauseCustomer
    val onResumeCustomer = actions.onResumeCustomer
    val onTakePhoto = actions.onTakePhoto
    val onAdresseClick = actions.onAdresseClick
    val onTelefonClick = actions.onTelefonClick
    val onPhotoClick = actions.onPhotoClick
    val onDeletePhoto = actions.onDeletePhoto
    val onDatumSelected = actions.onDatumSelected
    val onDeleteIntervall = actions.onDeleteIntervall
    val onRemoveRegel = actions.onRemoveRegel
    val onResetToAutomatic = actions.onResetToAutomatic
    val onRegelClick = actions.onRegelClick
    val onUrlaubStartActivity = actions.onUrlaubStartActivity
    val onAddMonthlyIntervall = actions.onAddMonthlyIntervall
    val onAddAbholungTermin = actions.onAddAbholungTermin
    val onAddAusnahmeTermin = actions.onAddAusnahmeTermin
    val onDeleteNextTermin = actions.onDeleteNextTermin
    val onDeleteAusnahmeTermin = actions.onDeleteAusnahmeTermin
    val onDeleteKundenTermin = actions.onDeleteKundenTermin
    val onNeueErfassungKameraFotoBelege = actions.onNeueErfassungKameraFotoBelege
    val onNeueErfassungFormularBelege = actions.onNeueErfassungFormularBelege
    val onNeueErfassungManuellBelege = actions.onNeueErfassungManuellBelege
    val onBelegClick = actions.onBelegClick

    val context = LocalContext.current
    val primaryBlue = colorResource(R.color.primary_blue)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val surfaceWhite = colorResource(R.color.surface_white)
    val statusOverdue = colorResource(R.color.status_overdue)

    // Phase 4: Überfällig-Hinweis (1 Source of Truth: TerminBerechnungUtils.istKundeUeberfaelligHeute)
    val isCustomerOverdue = remember(customer?.id) {
        customer != null && com.example.we2026_5.util.TerminBerechnungUtils.istKundeUeberfaelligHeute(customer)
    }

    // C4: Einzige State-Quelle für Formular = ViewModel (editFormState). Kein lokales formState mehr.
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    val initialFormStateFromCustomer = remember(customer?.id) {
        customer?.let { c ->
            val tageAzuL = c.tageAzuLOrDefault(7)
            val intervallTage = c.intervallTageOrDefault(7)
            AddCustomerState(
                name = c.name,
                alias = c.alias,
                adresse = c.adresse,
                latitude = c.latitude,
                longitude = c.longitude,
                stadt = c.stadt,
                plz = c.plz,
                telefon = c.telefon,
                notizen = c.notizen,
                kundenArt = c.kundenArt,
                kundenTyp = c.kundenTyp,
                tageAzuL = tageAzuL,
                intervallTage = intervallTage,
                kundennummer = c.kundennummer,
                abholungWochentage = c.effectiveAbholungWochentage,
                auslieferungWochentage = c.effectiveAuslieferungWochentage,
                defaultUhrzeit = c.defaultUhrzeit,
                tagsInput = c.tags.joinToString(", "),
                tourStadt = c.tourSlot?.stadt ?: "",
                tourZeitStart = c.tourSlot?.zeitfenster?.start ?: "",
                tourZeitEnde = c.tourSlot?.zeitfenster?.ende ?: "",
                ohneTour = c.ohneTour,
                erstelltAm = c.erstelltAm
            )
        }
    }
    val hasUnsavedChanges = isInEditMode && customer != null && initialFormStateFromCustomer != null &&
        editFormState != initialFormStateFromCustomer

    BackHandler(enabled = hasUnsavedChanges) {
        showUnsavedChangesDialog = true
    }

    val typeLabel = when (customer?.kundenArt) {
        "Privat" -> stringResource(R.string.label_type_privat)
        "Listenkunden" -> stringResource(R.string.label_type_tour)
        else -> stringResource(R.string.label_type_gewerblich)
    }
    val typeLetter = when (customer?.kundenArt) {
        "Privat" -> stringResource(R.string.label_type_p_letter)
        "Listenkunden" -> stringResource(R.string.label_type_l_letter)
        else -> stringResource(R.string.label_type_g)
    }
    val typeColor = when (customer?.kundenArt) {
        "Privat" -> colorResource(R.color.button_privat_glossy)
        "Listenkunden" -> colorResource(R.color.button_liste_glossy)
        else -> colorResource(R.color.button_gewerblich_glossy)
    }

    var selectedTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            CustomerDetailTopBar(
                typeLetter = typeLetter,
                typeColor = typeColor,
                displayName = customer?.displayName ?: stringResource(R.string.label_customer_name),
                isInEditMode = isInEditMode,
                isOffline = isOffline,
                statusOverdue = statusOverdue,
                onBack = { if (hasUnsavedChanges) showUnsavedChangesDialog = true else onBack() },
                onDelete = onDelete,
                onEdit = if (isAdmin) onEdit else null,
                isAdmin = isAdmin,
                overflowMenuExpanded = overflowMenuExpanded,
                onOverflowMenuDismiss = { overflowMenuExpanded = false },
                onOverflowMenuExpand = { overflowMenuExpanded = true },
                onSave = if (customer != null && isInEditMode) {{ onPerformSave(false) }} else null,
                showSaveAndNext = showSaveAndNext,
                onSaveAndNext = if (customer != null && isInEditMode && showSaveAndNext) {{ onPerformSave(true) }} else null
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
            var showAddWeeklySheet by remember { mutableStateOf(false) }
            var showNeuerTerminArtSheet by remember { mutableStateOf(false) }
            // C4: Nur ViewModel-State; bei customer != null ist initialFormStateFromCustomer gesetzt
            val currentFormState: AddCustomerState = editFormState ?: initialFormStateFromCustomer!!
            val onUpdateFormState: (AddCustomerState) -> Unit = { newState ->
                onUpdateEditFormState(newState)
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
                        text = {
                            Text(
                                stringResource(R.string.tab_stammdaten),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(
                                stringResource(R.string.tab_termine_tour),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                    Tab(
                        selected = selectedTabIndex == 2,
                        onClick = { if (!isInEditMode) selectedTabIndex = 2 },
                        enabled = !isInEditMode,
                        text = {
                            Text(
                                stringResource(R.string.tab_belege),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
                Spacer(Modifier.height(DetailUiConstants.SectionSpacing))
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
                        onAdresseClick = onAdresseClick,
                        onTelefonClick = onTelefonClick,
                        onTakePhoto = onTakePhoto,
                        onPhotoClick = onPhotoClick,
                        onDeletePhoto = onDeletePhoto,
                        isUploading = isUploading,
                        isOverdue = isCustomerOverdue
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
                                NeuerTerminArt.WOECHENTLICH -> showAddWeeklySheet = true
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
                        onDeleteKundenTermin = onDeleteKundenTermin,
                        editIntervalle = editIntervalle,
                        onDeleteIntervall = onDeleteIntervall,
                        showAddWeeklySheet = showAddWeeklySheet,
                        onDismissAddWeeklySheet = { showAddWeeklySheet = false },
                        onConfirmAddWeekly = {
                            onAddMonthlyIntervall?.invoke(it)
                            showAddWeeklySheet = false
                        }
                    )
                    2 -> CustomerDetailBelegeTab(
                        customer = customer,
                        belegMonate = belegMonateForCustomer,
                        belegMonateErledigt = belegMonateErledigtForCustomer,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onNeueErfassungKameraFoto = onNeueErfassungKameraFotoBelege,
                        onNeueErfassungFormular = onNeueErfassungFormularBelege,
                        onNeueErfassungManuell = onNeueErfassungManuellBelege,
                        onBelegClick = onBelegClick
                    )
                }
            }
        }
    }

    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text(stringResource(R.string.dialog_unsaved_changes_title)) },
            text = { Text(stringResource(R.string.dialog_unsaved_changes_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedChangesDialog = false
                    onBack()
                }) {
                    Text(stringResource(R.string.dialog_unsaved_changes_discard), color = colorResource(R.color.status_overdue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedChangesDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}

