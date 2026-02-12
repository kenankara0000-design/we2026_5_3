package com.example.we2026_5.ui.wasch

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.we2026_5.R
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.data.repository.ArticleRepository
import com.example.we2026_5.data.repository.TourPreiseRepository
import com.example.we2026_5.wasch.Article
import com.example.we2026_5.wasch.TourPreis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TourPreislisteUiState(
    val tourPreise: List<TourPreis> = emptyList(),
    val addDialogOpen: Boolean = false,
    val selectedArticleForAdd: Article? = null,
    val addArticleSearchQuery: String = "",
    val addPriceNet: String = "",
    val addPriceGross: String = "",
    val isSaving: Boolean = false,
    val message: String? = null
)

class TourPreislisteViewModel(
    private val context: Context,
    private val tourPreiseRepository: TourPreiseRepository,
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TourPreislisteUiState())
    val uiState: StateFlow<TourPreislisteUiState> = _uiState.asStateFlow()

    val articles: StateFlow<List<Article>> = articleRepository.getAllArticlesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            tourPreiseRepository.getTourPreiseFlow().collect { preise ->
                _uiState.value = _uiState.value.copy(tourPreise = preise)
            }
        }
    }

    fun openAddDialog() {
        _uiState.value = _uiState.value.copy(
            addDialogOpen = true,
            selectedArticleForAdd = null,
            addArticleSearchQuery = "",
            addPriceNet = "",
            addPriceGross = "",
            message = null
        )
    }

    fun closeAddDialog() {
        _uiState.value = _uiState.value.copy(
            addDialogOpen = false,
            selectedArticleForAdd = null,
            addArticleSearchQuery = "",
            addPriceNet = "",
            addPriceGross = "",
            message = null
        )
    }

    fun setSelectedArticleForAdd(article: Article?) {
        _uiState.value = _uiState.value.copy(
            selectedArticleForAdd = article,
            addArticleSearchQuery = if (article != null) "" else _uiState.value.addArticleSearchQuery
        )
    }

    fun setAddArticleSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(addArticleSearchQuery = query)
    }

    fun setAddPriceNet(value: String) {
        _uiState.value = _uiState.value.copy(addPriceNet = value)
    }

    fun setAddPriceGross(value: String) {
        _uiState.value = _uiState.value.copy(addPriceGross = value)
    }

    fun saveTourPreis() {
        val s = _uiState.value
        val article = s.selectedArticleForAdd ?: return
        val net = s.addPriceNet.toDoubleOrNull() ?: 0.0
        val gross = s.addPriceGross.toDoubleOrNull() ?: 0.0
        if (net <= 0 && gross <= 0) {
            _uiState.value = _uiState.value.copy(message = context.getString(R.string.error_tourpreis_netto_brutto))
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, message = null)
            val preis = TourPreis(articleId = article.id, priceNet = net, priceGross = gross)
            val ok = tourPreiseRepository.setTourPreis(preis)
            _uiState.value = _uiState.value.copy(
                isSaving = false,
                addDialogOpen = !ok,
                message = if (ok) null else context.getString(R.string.wasch_fehler_speichern),
                addPriceNet = "",
                addPriceGross = "",
                selectedArticleForAdd = null
            )
            if (ok) closeAddDialog()
        }
    }

    fun removeTourPreis(articleId: String) {
        viewModelScope.launch {
            tourPreiseRepository.removeTourPreis(articleId)
        }
    }
}
