package com.example.we2026_5.ui.wasch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.data.repository.ArticleRepository
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.ErfassungRepository
import com.example.we2026_5.wasch.Article
import com.example.we2026_5.wasch.ErfassungPosition
import com.example.we2026_5.wasch.WaschErfassung
import com.example.we2026_5.util.TerminBerechnungUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Einzelne Zeile in der Erfassung (Artikel + Menge + Einheit) für die UI. */
data class ErfassungZeile(
    val articleId: String,
    val artikelName: String,
    val einheit: String,
    val menge: Int
)

sealed class WaschenErfassungUiState {
    object Auswahl : WaschenErfassungUiState()
    data class KundeWaehlen(val customers: List<Customer>) : WaschenErfassungUiState()
    data class Erfassen(
        val customer: Customer,
        val datum: Long,
        val zeit: String,
        val zeilen: List<ErfassungZeile>,
        val notiz: String,
        val artikelSearchQuery: String = "",
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
            _uiState.value = WaschenErfassungUiState.Erfassen(
                customer = customer,
                datum = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis()),
                zeit = "",
                zeilen = emptyList(),
                notiz = ""
            )
        }
    }

    /** Startet Erfassen mit vorgewähltem Kunden (z. B. aus Kunden-Detail). */
    fun startErfassenFuerKunde(customer: Customer) {
        _uiState.value = WaschenErfassungUiState.Erfassen(
            customer = customer,
            datum = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis()),
            zeit = "",
            zeilen = emptyList(),
            notiz = ""
        )
    }

    fun setDatum(datum: Long) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Erfassen) {
            _uiState.value = s.copy(datum = datum)
        }
    }

    fun setZeit(zeit: String) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Erfassen) {
            _uiState.value = s.copy(zeit = zeit.trim())
        }
    }

    fun setArtikelSearchQuery(query: String) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Erfassen) {
            _uiState.value = s.copy(artikelSearchQuery = query)
        }
    }

    fun addPosition(article: Article) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Erfassen) {
            val newZeile = ErfassungZeile(
                articleId = article.id,
                artikelName = article.name,
                einheit = article.einheit.ifBlank { "Stk" },
                menge = 1
            )
            _uiState.value = s.copy(
                zeilen = s.zeilen + newZeile,
                artikelSearchQuery = ""
            )
        }
    }

    fun removePosition(index: Int) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Erfassen && index in s.zeilen.indices) {
            _uiState.value = s.copy(zeilen = s.zeilen.toMutableList().apply { removeAt(index) })
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

    fun setMengeByIndex(index: Int, menge: Int) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Erfassen && index in s.zeilen.indices) {
            val list = s.zeilen.toMutableList()
            list[index] = list[index].copy(menge = menge.coerceAtLeast(0))
            _uiState.value = s.copy(zeilen = list)
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
            ErfassungPosition(articleId = z.articleId, menge = z.menge, einheit = z.einheit)
        }
        if (positionen.isEmpty()) {
            _uiState.value = s.copy(errorMessage = "Bitte mindestens einen Artikel mit Menge > 0 eintragen.")
            return
        }
        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true, errorMessage = null)
            val ok = erfassungRepository.saveErfassungNew(
                WaschErfassung(
                    customerId = s.customer.id,
                    datum = s.datum,
                    zeit = s.zeit,
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
