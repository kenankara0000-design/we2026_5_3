package com.example.we2026_5

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.we2026_5.ui.wasch.StandardPreislisteScreen
import org.koin.androidx.viewmodel.ext.android.viewModel

class StandardPreislisteActivity : AppCompatActivity() {

    private val viewModel: com.example.we2026_5.ui.wasch.StandardPreislisteViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val state by viewModel.uiState.collectAsState()
                val articles by viewModel.articles.collectAsState(initial = emptyList())
                StandardPreislisteScreen(
                    state = state,
                    articles = articles,
                    onBack = { finish() },
                    onAddClick = { viewModel.openAddDialog() },
                    onCloseAddDialog = { viewModel.closeAddDialog() },
                    onSelectArticle = { viewModel.setSelectedArticleForAdd(it) },
                    onArticleSearchQueryChange = { viewModel.setAddArticleSearchQuery(it) },
                    onPriceNetChange = { viewModel.setAddPriceNet(it) },
                    onPriceGrossChange = { viewModel.setAddPriceGross(it) },
                    onSaveStandardPreis = { viewModel.saveStandardPreis() },
                    onRemoveStandardPreis = { viewModel.removeStandardPreis(it) }
                )
            }
        }
    }
}
