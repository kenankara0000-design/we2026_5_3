package com.example.we2026_5.ui.wasch

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.we2026_5.R
import com.example.we2026_5.wasch.Article
import com.example.we2026_5.wasch.KundenPreis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KundenpreiseScreen(
    state: KundenpreiseUiState,
    kundenPreise: List<KundenPreis>,
    articles: List<Article>,
    onBack: () -> Unit,
    onCustomerSearchQueryChange: (String) -> Unit,
    onKundeWaehlen: (com.example.we2026_5.Customer) -> Unit,
    onBackToKundeSuchen: () -> Unit
) {
    val primaryBlue = colorResource(R.color.primary_blue)
    val textPrimary = colorResource(R.color.text_primary)
    val textSecondary = colorResource(R.color.text_secondary)
    val backgroundLight = colorResource(R.color.background_light)
    val articlesMap = articles.associateBy { it.id }

    Scaffold(
        topBar = {
            WaschenErfassungTopBar(primaryBlue = primaryBlue, onBack = onBack)
        },
        containerColor = backgroundLight
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (state) {
                is KundenpreiseUiState.KundeSuchen -> {
                    WaschenErfassungKundeSuchenContent(
                        customerSearchQuery = state.customerSearchQuery,
                        onSearchQueryChange = onCustomerSearchQueryChange,
                        filteredCustomers = state.customers,
                        textSecondary = textSecondary,
                        onKundeWaehlen = onKundeWaehlen,
                        searchHintWhenEmpty = state.customerSearchQuery.isBlank()
                    )
                }
                is KundenpreiseUiState.KundenpreiseList -> {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text(
                            state.customer.displayName,
                            fontSize = 18.sp,
                            color = textPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            stringResource(R.string.erfassung_menu_kundenpreise),
                            fontSize = 14.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        if (kundenPreise.isEmpty()) {
                            Text(
                                stringResource(R.string.wasch_keine_kundenpreise),
                                fontSize = 14.sp,
                                color = textSecondary,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            LazyColumn {
                                items(kundenPreise) { preis ->
                                    val name = articlesMap[preis.articleId]?.name ?: preis.articleId
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text(name, fontSize = 16.sp, color = textPrimary)
                                            Text(
                                                stringResource(R.string.format_netto_brutto, preis.priceNet, preis.priceGross),
                                                fontSize = 14.sp,
                                                color = textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
