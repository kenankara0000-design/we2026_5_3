package com.example.we2026_5

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.we2026_5.ui.theme.AppTheme
import com.example.we2026_5.ui.wasch.ListenPrivatKundenpreiseScreen
import org.koin.androidx.viewmodel.ext.android.viewModel

class ListenPrivatKundenpreiseActivity : AppCompatActivity() {

    private val viewModel: com.example.we2026_5.ui.wasch.ListenPrivatKundenpreiseViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val state by viewModel.uiState.collectAsState()
                val articles by viewModel.articles.collectAsState(initial = emptyList())
                ListenPrivatKundenpreiseScreen(
                    state = state,
                    articles = articles,
                    onBack = { finish() },
                    onAddClick = { viewModel.openAddDialog() },
                    onCloseAddDialog = { viewModel.closeAddDialog() },
                    onSelectArticle = { viewModel.setSelectedArticleForAdd(it) },
                    onArticleSearchQueryChange = { viewModel.setAddArticleSearchQuery(it) },
                    onPriceNetChange = { viewModel.setAddPriceNet(it) },
                    onPriceGrossChange = { viewModel.setAddPriceGross(it) },
                    onSaveListenPrivatKundenpreis = { viewModel.saveListenPrivatKundenpreis() },
                    onRemoveListenPrivatKundenpreis = { viewModel.removeListenPrivatKundenpreis(it) }
                )
            }
        }
    }
}
