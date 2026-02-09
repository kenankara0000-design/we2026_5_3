package com.example.we2026_5.ui.listebearbeiten

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.example.we2026_5.Customer
import com.example.we2026_5.R

@Composable
fun ListeBearbeitenScreen(
    state: ListeBearbeitenState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onSave: (name: String, listeArt: String) -> Unit,
    onDelete: () -> Unit,
    onTerminAnlegen: () -> Unit,
    onDeleteListenTermine: (List<com.example.we2026_5.KundenTermin>) -> Unit,
    onWochentagAChange: (Int?) -> Unit,
    onTageAzuLChange: (Int) -> Unit,
    onRemoveKunde: (Customer) -> Unit,
    onAddKunde: (Customer) -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
    val surfaceWhite = Color(ContextCompat.getColor(context, R.color.surface_white))
    val textPrimary = Color(ContextCompat.getColor(context, R.color.text_primary))
    val textSecondary = Color(ContextCompat.getColor(context, R.color.text_secondary))
    val statusOverdue = Color(ContextCompat.getColor(context, R.color.status_overdue))
    val statusDone = Color(ContextCompat.getColor(context, R.color.status_done))
    val saveButtonColor = Color(ContextCompat.getColor(context, R.color.termin_regel_button_save))

    var editName by remember(state.liste?.name) { mutableStateOf(state.liste?.name ?: "") }
    var editListeArt by remember(state.liste?.listeArt) { mutableStateOf(state.liste?.listeArt ?: "Gewerbe") }
    var nameError by remember { mutableStateOf<String?>(null) }
    val validationNameMissing = stringResource(R.string.validation_name_missing)

    Scaffold(
        topBar = {
            ListeBearbeitenTopBar(
                listName = state.liste?.name,
                isInEditMode = state.isInEditMode,
                onBack = onBack,
                onRefresh = onRefresh,
                onDelete = onDelete,
                containerColor = primaryBlue,
                deleteTextColor = statusOverdue
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            ListeBearbeitenLoadingView(
                modifier = Modifier.fillMaxWidth().padding(paddingValues).padding(32.dp),
                textColor = textSecondary
            )
        } else if (state.isEmpty && state.kundenInListe.isEmpty() && state.verfuegbareKunden.isEmpty()) {
            ListeBearbeitenEmptyView(
                modifier = Modifier.fillMaxWidth().padding(paddingValues).padding(32.dp),
                textColor = textSecondary
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                ListeBearbeitenMetadatenBlock(
                    isInEditMode = state.isInEditMode,
                    displayName = state.liste?.name ?: "",
                    displayListeArt = state.liste?.listeArt ?: "",
                    onEdit = onEdit,
                    editName = editName,
                    editListeArt = editListeArt,
                    nameError = nameError,
                    onNameChange = { editName = it; nameError = null },
                    onListeArtChange = { editListeArt = it },
                    onSaveClick = {
                        val name = editName.trim()
                        if (name.isEmpty()) nameError = validationNameMissing
                        else { nameError = null; onSave(name, editListeArt) }
                    },
                    textPrimary = textPrimary,
                    statusOverdue = statusOverdue,
                    saveButtonContainerColor = saveButtonColor
                )

                if (state.isInEditMode && state.liste != null && (state.liste!!.wochentag ?: -1) !in 0..6) {
                    Spacer(modifier = Modifier.height(12.dp))
                    ListeBearbeitenListenTermineSection(
                        listenTermine = state.liste!!.listenTermine,
                        wochentagA = state.liste!!.wochentagA,
                        tageAzuL = state.liste!!.tageAzuL,
                        surfaceWhite = surfaceWhite,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        primaryBlue = primaryBlue,
                        onWochentagAChange = onWochentagAChange,
                        onTageAzuLChange = onTageAzuLChange,
                        onAddTermin = onTerminAnlegen,
                        onDeleteTermine = onDeleteListenTermine
                    )
                }

                ListeBearbeitenKundenSectionCollapsible(
                    isTourListe = (state.liste?.wochentag ?: -1) !in 0..6,
                    wochentag = state.liste?.wochentag,
                    kundenInListe = state.kundenInListe,
                    verfuegbareKunden = state.verfuegbareKunden,
                    onRemoveKunde = onRemoveKunde,
                    onAddKunde = onAddKunde,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    statusOverdue = statusOverdue,
                    statusDone = statusDone
                )
            }
        }
    }
}
