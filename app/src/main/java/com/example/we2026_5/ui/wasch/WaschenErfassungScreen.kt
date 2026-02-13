package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    belegMonate: List<BelegMonat>,
    onBelegClick: (BelegMonat) -> Unit,
    onBackFromBelegDetail: () -> Unit,
    onBackFromDetail: () -> Unit,
    onMengeChangeByIndex: (index: Int, menge: Double) -> Unit,
    onNotizChange: (String) -> Unit,
    onSpeichern: () -> Unit,
    onBackFromErfassen: () -> Unit,
    onArtikelSearchQueryChange: (String) -> Unit,
    erfassungArticles: List<ArticleDisplay>,
    showAllgemeinePreiseHint: Boolean,
    onAddPosition: (ArticleDisplay) -> Unit,
    onRemovePosition: (Int) -> Unit,
    onDeleteErfassung: (com.example.we2026_5.wasch.WaschErfassung) -> Unit = {},
    onDeleteBeleg: () -> Unit = {},
    onErledigtBeleg: () -> Unit = {},
    belegMonateErledigt: List<BelegMonat> = emptyList(),
    onBelegListeShowErledigtTabChange: (Boolean) -> Unit = {},
    /** Brutto-Preise pro Artikel für Beleg-Detail (Gesamtpreis-Anzeige). */
    belegPreiseGross: Map<String, Double> = emptyMap(),
    onNeueErfassungKameraFotoFromListe: () -> Unit = {},
    onNeueErfassungFormularFromListe: () -> Unit = {},
    onNeueErfassungManuellFromListe: () -> Unit = {},
    onFormularNameChange: (String) -> Unit = {},
    onFormularAdresseChange: (String) -> Unit = {},
    onFormularTelefonChange: (String) -> Unit = {},
    onFormularMengeChange: (String, Int) -> Unit = { _, _ -> },
    onFormularSonstigesChange: (String) -> Unit = {},
    onFormularKameraFoto: () -> Unit = {},
    onFormularAbbrechen: () -> Unit = {},
    onFormularSpeichern: () -> Unit = {},
    isOffline: Boolean = false
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
            if (isOffline) {
                val offlineYellow = colorResource(R.color.status_offline_yellow)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(offlineYellow.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_offline),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = offlineYellow
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        stringResource(R.string.offline_hinweis_daten_sync),
                        color = colorResource(R.color.text_secondary),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            when (state) {
            is WaschenErfassungUiState.KundeSuchen -> {
                WaschenErfassungKundeSuchenContent(
                    customerSearchQuery = state.customerSearchQuery,
                    onSearchQueryChange = onCustomerSearchQueryChange,
                    filteredCustomers = state.customers,
                    textSecondary = textSecondary,
                    onKundeWaehlen = onKundeWaehlen,
                    searchHintWhenEmpty = state.customerSearchQuery.isBlank()
                )
            }
            is WaschenErfassungUiState.ErfassungenListe -> {
                WaschenErfassungBelegListeContent(
                    customer = state.customer,
                    belege = if (state.showErledigtTab) belegMonateErledigt else belegMonate,
                    showErledigtTab = state.showErledigtTab,
                    onShowErledigtTabChange = onBelegListeShowErledigtTabChange,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onBackToKundeSuchen = onBackToKundeSuchen,
                    onNeueErfassungKameraFoto = onNeueErfassungKameraFotoFromListe,
                    onNeueErfassungFormular = onNeueErfassungFormularFromListe,
                    onNeueErfassungManuell = onNeueErfassungManuellFromListe,
                    onBelegClick = onBelegClick
                )
            }
            is WaschenErfassungUiState.Formular -> {
                WaeschelisteFormularContent(
                    customer = state.customer,
                    formularState = state.formularState,
                    onNameChange = onFormularNameChange,
                    onAdresseChange = onFormularAdresseChange,
                    onTelefonChange = onFormularTelefonChange,
                    onMengeChange = onFormularMengeChange,
                    onSonstigesChange = onFormularSonstigesChange,
                    onKameraFoto = onFormularKameraFoto,
                    onAbbrechen = onFormularAbbrechen,
                    onSpeichern = onFormularSpeichern,
                    isSaving = state.isSaving,
                    isScanning = state.isScanning,
                    errorMessage = state.errorMessage,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )
            }
            is WaschenErfassungUiState.BelegDetail -> {
                val articlesMap = articles.associateBy { it.id }
                WaschenErfassungBelegDetailContent(
                    customerName = state.customer.displayName,
                    monthLabel = state.monthLabel,
                    erfassungen = state.erfassungen,
                    articlesMap = articlesMap,
                    preiseGross = belegPreiseGross,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onBack = onBackFromBelegDetail,
                    onDeleteBeleg = onDeleteBeleg,
                    onErledigt = onErledigtBeleg
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
                val searchResults = erfassungArticles.filter { it.name.contains(state.artikelSearchQuery, ignoreCase = true) }
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
                    showAllgemeinePreiseHint = showAllgemeinePreiseHint,
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
