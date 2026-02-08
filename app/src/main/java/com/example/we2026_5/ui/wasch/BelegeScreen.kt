package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.example.we2026_5.R
import com.example.we2026_5.wasch.Article

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BelegeScreen(
    state: BelegeUiState,
    belegMonate: List<BelegMonat>,
    articles: List<Article>,
    onBack: () -> Unit,
    onCustomerSearchQueryChange: (String) -> Unit,
    onKundeWaehlen: (com.example.we2026_5.Customer) -> Unit,
    onBackToKundeSuchen: () -> Unit,
    onBelegClick: (BelegMonat) -> Unit,
    onBackFromBelegDetail: () -> Unit,
    onNeueErfassungFromListe: () -> Unit,
    onDeleteErfassung: (com.example.we2026_5.wasch.WaschErfassung) -> Unit
) {
    val primaryBlue = colorResource(R.color.primary_blue)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val backgroundLight = colorResource(R.color.background_light)

    Scaffold(
        topBar = {
            WaschenErfassungTopBar(primaryBlue = primaryBlue, onBack = onBack)
        },
        containerColor = backgroundLight
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (state) {
                is BelegeUiState.KundeSuchen -> {
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
                is BelegeUiState.BelegListe -> {
                    WaschenErfassungBelegListeContent(
                        customer = state.customer,
                        belege = belegMonate,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onBackToKundeSuchen = onBackToKundeSuchen,
                        onNeueErfassungFromListe = onNeueErfassungFromListe,
                        onBelegClick = onBelegClick
                    )
                }
                is BelegeUiState.BelegDetail -> {
                    val articlesMap = articles.associateBy { it.id }
                    WaschenErfassungBelegDetailContent(
                        customerName = state.customer.displayName,
                        monthLabel = state.monthLabel,
                        erfassungen = state.erfassungen,
                        articlesMap = articlesMap,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onBack = onBackFromBelegDetail,
                        onDeleteErfassung = onDeleteErfassung
                    )
                }
            }
        }
    }
}
