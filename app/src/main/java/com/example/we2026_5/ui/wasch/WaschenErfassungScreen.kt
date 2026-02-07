package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.wasch.Article
import com.example.we2026_5.ui.wasch.WaschenErfassungUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaschenErfassungScreen(
    state: WaschenErfassungUiState,
    articles: List<Article>,
    erfassungen: List<com.example.we2026_5.wasch.WaschErfassung>,
    onBack: () -> Unit,
    onCustomerSearchQueryChange: (String) -> Unit,
    onKundeWaehlen: (Customer) -> Unit,
    onBackToKundeSuchen: () -> Unit,
    onErfassungClick: (com.example.we2026_5.wasch.WaschErfassung) -> Unit,
    onNeueErfassungFromListe: () -> Unit,
    onBackFromDetail: () -> Unit,
    onMengeChangeByIndex: (index: Int, menge: Int) -> Unit,
    onNotizChange: (String) -> Unit,
    onSpeichern: () -> Unit,
    onBackFromErfassen: () -> Unit,
    onArtikelSearchQueryChange: (String) -> Unit,
    onAddPosition: (Article) -> Unit,
    onRemovePosition: (Int) -> Unit,
    onDeleteErfassung: (com.example.we2026_5.wasch.WaschErfassung) -> Unit = {}
) {
    val primaryBlue = colorResource(R.color.primary_blue)
    val backgroundLight = colorResource(R.color.background_light)
    val textSecondary = colorResource(R.color.text_secondary)

    val textPrimary = colorResource(R.color.text_primary)
    Scaffold(
        topBar = {
            WaschenErfassungTopBar(primaryBlue = primaryBlue, onBack = onBack)
        },
        containerColor = backgroundLight
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (state) {
            is WaschenErfassungUiState.KundeSuchen -> {
                val filtered = state.customers.filter {
                    state.customerSearchQuery.isBlank() || it.displayName.contains(state.customerSearchQuery, ignoreCase = true)
                }
                WaschenErfassungKundeSuchenContent(
                    customerSearchQuery = state.customerSearchQuery,
                    onSearchQueryChange = onCustomerSearchQueryChange,
                    filteredCustomers = filtered,
                    textSecondary = textSecondary,
                    onKundeWaehlen = onKundeWaehlen
                )
            }
            is WaschenErfassungUiState.ErfassungenListe -> {
                WaschenErfassungErfassungenListeContent(
                    customer = state.customer,
                    erfassungen = erfassungen,
                    primaryBlue = primaryBlue,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onBackToKundeSuchen = onBackToKundeSuchen,
                    onNeueErfassungFromListe = onNeueErfassungFromListe,
                    onErfassungClick = onErfassungClick,
                    onDeleteErfassung = onDeleteErfassung
                )
            }
            is WaschenErfassungUiState.ErfassungDetail -> {
                WaschenErfassungDetailContent(
                    erfassung = state.erfassung,
                    positionenAnzeige = state.positionenAnzeige,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onDeleteErfassung = onDeleteErfassung
                )
            }
            is WaschenErfassungUiState.Erfassen -> {
                val searchResults = articles.filter { it.name.contains(state.artikelSearchQuery, ignoreCase = true) }
                WaschenErfassungErfassenContent(
                    customer = state.customer,
                    notiz = state.notiz,
                    onNotizChange = onNotizChange,
                    artikelSearchQuery = state.artikelSearchQuery,
                    onArtikelSearchQueryChange = onArtikelSearchQueryChange,
                    searchResults = searchResults,
                    zeilen = state.zeilen,
                    onMengeChangeByIndex = onMengeChangeByIndex,
                    onAddPosition = onAddPosition,
                    onRemovePosition = onRemovePosition,
                    errorMessage = state.errorMessage,
                    isSaving = state.isSaving,
                    onSpeichern = onSpeichern,
                    onBackFromErfassen = onBackFromErfassen,
                    primaryBlue = primaryBlue,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }
        }
        }
    }
}
