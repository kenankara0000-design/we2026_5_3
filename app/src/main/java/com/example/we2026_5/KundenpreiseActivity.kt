package com.example.we2026_5

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.we2026_5.ui.theme.AppTheme
import com.example.we2026_5.ui.wasch.KundenpreiseScreen
import com.example.we2026_5.ui.wasch.KundenpreiseUiState
import com.example.we2026_5.ui.wasch.KundenpreiseViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class KundenpreiseActivity : AppCompatActivity() {

    private val viewModel: KundenpreiseViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val state by viewModel.uiState.collectAsState()
                val kundenPreise by viewModel.kundenPreiseList.collectAsState(initial = emptyList())
                val articles by viewModel.articles.collectAsState(initial = emptyList())
                val isLoadingPreise by viewModel.isLoadingPreise.collectAsState()
                KundenpreiseScreen(
                    state = state,
                    kundenPreise = kundenPreise,
                    articles = articles,
                    isLoadingPreise = isLoadingPreise,
                    onBack = {
                        when (state) {
                            is KundenpreiseUiState.KundeSuchen -> finish()
                            is KundenpreiseUiState.KundenpreiseList -> viewModel.backToKundeSuchen()
                        }
                    },
                    onCustomerSearchQueryChange = { viewModel.setCustomerSearchQuery(it) },
                    onKundeWaehlen = { viewModel.kundeGewaehlt(it) },
                    onBackToKundeSuchen = { viewModel.backToKundeSuchen() }
                )
            }
        }
    }
}
