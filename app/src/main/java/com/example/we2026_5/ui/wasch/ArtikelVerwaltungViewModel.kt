package com.example.we2026_5.ui.wasch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.data.repository.ArticleRepository
import com.example.we2026_5.wasch.Article
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArtikelVerwaltungViewModel(
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val articles: StateFlow<List<Article>> = articleRepository.getAllArticlesFlow()
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteArticle(article: Article, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = articleRepository.deleteArticle(article.id)
            onResult(ok)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
