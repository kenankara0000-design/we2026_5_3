package com.example.we2026_5.ui.wasch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.data.repository.ArticleRepository
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.ErfassungRepository
import com.example.we2026_5.wasch.ErfassungPosition
import com.example.we2026_5.wasch.WaschErfassung
import com.example.we2026_5.util.TerminBerechnungUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Einzelne Zeile in der Erfassung (Artikel + Menge) für die UI. */
data class ErfassungZeile(
    val articleId: String,
    val artikelName: String,
    var menge: Int
)

sealed class WaschenErfassungUiState {
    object Auswahl : WaschenErfassungUiState()  // Start: Neue Erfassung oder Liste
    data class KundeWaehlen(val customers: List<Customer>) : WaschenErfassungUiState()
    data class Erfassen(
        val customer: Customer,
        val zeilen: List<ErfassungZeile>,
        val notiz: String,
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    ) : WaschenErfassungUiState()
}

class WaschenErfassungViewModel(
    private val customerRepository: CustomerRepository,
    private val articleRepository: ArticleRepository,
    private val erfassungRepository: ErfassungRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WaschenErfassungUiState>(WaschenErfassungUiState.Auswahl)
    val uiState: StateFlow<WaschenErfassungUiState> = _uiState.asStateFlow()

    val articles = articleRepository.getAllArticlesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startNeueErfassung() {
        viewModelScope.launch {
            val customers = customerRepository.getAllCustomers().sortedBy { it.displayName }
            _uiState.value = WaschenErfassungUiState.KundeWaehlen(customers)
        }
    }

    fun kundeGewaehlt(customer: Customer) {
        viewModelScope.launch {
            val artList = articleRepository.getAllArticles()
            val zeilen = artList.sortedBy { it.name }.map { a ->
                ErfassungZeile(articleId = a.id, artikelName = a.name, menge = 0)
            }
            _uiState.value = WaschenErfassungUiState.Erfassen(
                customer = customer,
                zeilen = zeilen,
                notiz = ""
            )
        }
    }

    fun setMenge(articleId: String, menge: Int) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Erfassen) {
            val updated = s.zeilen.map { z ->
                if (z.articleId == articleId) z.copy(menge = menge.coerceAtLeast(0)) else z
            }
            _uiState.value = s.copy(zeilen = updated)
        }
    }

    fun setNotiz(notiz: String) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Erfassen) {
            _uiState.value = s.copy(notiz = notiz)
        }
    }

    fun speichern(onSaved: () -> Unit) {
        val s = _uiState.value
        if (s !is WaschenErfassungUiState.Erfassen) return
        val positionen = s.zeilen.filter { it.menge > 0 }.map { z ->
            ErfassungPosition(articleId = z.articleId, menge = z.menge)
        }
        if (positionen.isEmpty()) {
            _uiState.value = s.copy(errorMessage = "Bitte mindestens einen Artikel mit Menge > 0 eintragen.")
            return
        }
        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true, errorMessage = null)
            val datum = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
            val ok = erfassungRepository.saveErfassungNew(
                WaschErfassung(
                    customerId = s.customer.id,
                    datum = datum,
                    positionen = positionen,
                    notiz = s.notiz
                )
            )
            _uiState.value = s.copy(isSaving = false)
            if (ok) onSaved()
            else _uiState.value = s.copy(errorMessage = "Fehler beim Speichern.")
        }
    }

    fun backToAuswahl() {
        _uiState.value = WaschenErfassungUiState.Auswahl
    }

    fun backToKundeWaehlen() {
        viewModelScope.launch {
            val customers = customerRepository.getAllCustomers().sortedBy { it.displayName }
            _uiState.value = WaschenErfassungUiState.KundeWaehlen(customers)
        }
    }
}
