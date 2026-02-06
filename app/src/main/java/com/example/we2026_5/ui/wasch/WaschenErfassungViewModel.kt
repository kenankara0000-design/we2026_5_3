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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Einzelne Zeile in der Erfassung (Artikel + Menge + Einheit) für die UI. */
data class ErfassungZeile(
    val articleId: String,
    val artikelName: String,
    val einheit: String,
    val menge: Int
)

/** Zeile für Beleg-Detail (Artikelname, Menge, Einheit). */
data class ErfassungPositionAnzeige(
    val artikelName: String,
    val menge: Int,
    val einheit: String
)

sealed class WaschenErfassungUiState {
    /** Start: Kunde suchen (Suchfeld + Liste). */
    data class KundeSuchen(
        val customerSearchQuery: String = "",
        val customers: List<Customer> = emptyList()
    ) : WaschenErfassungUiState()
    /** Erfassungen für einen Kunden (Tag für Tag Belege). */
    data class ErfassungenListe(val customer: Customer) : WaschenErfassungUiState()
    /** Ein Beleg im Detail (nur Anzeige). */
    data class ErfassungDetail(
        val erfassung: WaschErfassung,
        val positionenAnzeige: List<ErfassungPositionAnzeige>
    ) : WaschenErfassungUiState()
    /** Neue Erfassung anlegen (ohne Datum/Zeit-Felder – werden beim Speichern gesetzt). */
    data class Erfassen(
        val customer: Customer,
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

    private val _uiState = MutableStateFlow<WaschenErfassungUiState>(WaschenErfassungUiState.KundeSuchen())
    val uiState: StateFlow<WaschenErfassungUiState> = _uiState.asStateFlow()

    private val _erfassungenList = MutableStateFlow<List<WaschErfassung>>(emptyList())
    val erfassungenList: StateFlow<List<WaschErfassung>> = _erfassungenList.asStateFlow()

    private var erfassungenJob: Job? = null

    val articles = articleRepository.getAllArticlesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val customers = customerRepository.getAllCustomers().sortedBy { it.displayName }
            _uiState.value = WaschenErfassungUiState.KundeSuchen(customers = customers)
        }
    }

    fun startNeueErfassung() {
        viewModelScope.launch {
            val customers = customerRepository.getAllCustomers().sortedBy { it.displayName }
            _uiState.value = WaschenErfassungUiState.KundeSuchen(customerSearchQuery = "", customers = customers)
        }
    }

    fun setCustomerSearchQuery(query: String) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.KundeSuchen) {
            _uiState.value = s.copy(customerSearchQuery = query)
        }
    }

    fun kundeGewaehlt(customer: Customer) {
        erfassungenJob?.cancel()
        _uiState.value = WaschenErfassungUiState.ErfassungenListe(customer)
        erfassungenJob = viewModelScope.launch {
            erfassungRepository.getErfassungenByCustomerFlow(customer.id).collect {
                _erfassungenList.value = it
            }
        }
    }

    fun backToKundeSuchen() {
        erfassungenJob?.cancel()
        _erfassungenList.value = emptyList()
        viewModelScope.launch {
            val customers = customerRepository.getAllCustomers().sortedBy { it.displayName }
            _uiState.value = WaschenErfassungUiState.KundeSuchen(customerSearchQuery = "", customers = customers)
        }
    }

    fun openErfassungDetail(erfassung: WaschErfassung) {
        viewModelScope.launch {
            val articlesMap = articleRepository.getAllArticles().associateBy { it.id }
            val positionenAnzeige = erfassung.positionen.map { pos ->
                val name = articlesMap[pos.articleId]?.name ?: pos.articleId
                ErfassungPositionAnzeige(name, pos.menge, pos.einheit.ifBlank { "Stk" })
            }
            _uiState.value = WaschenErfassungUiState.ErfassungDetail(erfassung, positionenAnzeige)
        }
    }

    fun backFromDetail() {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.ErfassungDetail) {
            viewModelScope.launch {
                val customer = customerRepository.getCustomerById(s.erfassung.customerId)
                if (customer != null) {
                    _uiState.value = WaschenErfassungUiState.ErfassungenListe(customer)
                }
            }
        }
    }

    /** Von ErfassungenListe: Neue Erfassung für diesen Kunden. */
    fun neueErfassungClick(customer: Customer) {
        _uiState.value = WaschenErfassungUiState.Erfassen(
            customer = customer,
            zeilen = emptyList(),
            notiz = ""
        )
    }

    /** Startet Erfassen mit vorgewähltem Kunden (z. B. aus Kunden-Detail). */
    fun startErfassenFuerKunde(customer: Customer) {
        erfassungenJob?.cancel()
        _uiState.value = WaschenErfassungUiState.ErfassungenListe(customer)
        erfassungenJob = viewModelScope.launch {
            erfassungRepository.getErfassungenByCustomerFlow(customer.id).collect {
                _erfassungenList.value = it
            }
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
                menge = 0
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
            val now = System.currentTimeMillis()
            val datum = TerminBerechnungUtils.getStartOfDay(now)
            val zeit = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
            val ok = erfassungRepository.saveErfassungNew(
                WaschErfassung(
                    customerId = s.customer.id,
                    datum = datum,
                    zeit = zeit,
                    positionen = positionen,
                    notiz = s.notiz
                )
            )
            _uiState.value = s.copy(isSaving = false)
            if (ok) {
                _uiState.value = WaschenErfassungUiState.ErfassungenListe(s.customer)
                erfassungenJob?.cancel()
                erfassungenJob = viewModelScope.launch {
                    erfassungRepository.getErfassungenByCustomerFlow(s.customer.id).collect {
                        _erfassungenList.value = it
                    }
                }
                onSaved()
            } else {
                _uiState.value = s.copy(errorMessage = "Fehler beim Speichern.")
            }
        }
    }

    fun backToAuswahl() {
        backToKundeSuchen()
    }

    fun backFromErfassenToListe() {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Erfassen) {
            _uiState.value = WaschenErfassungUiState.ErfassungenListe(s.customer)
            erfassungenJob?.cancel()
            erfassungenJob = viewModelScope.launch {
                erfassungRepository.getErfassungenByCustomerFlow(s.customer.id).collect {
                    _erfassungenList.value = it
                }
            }
        }
    }

    /** Erfassung löschen; bei Erfolg aus Detail zurück zur Liste, Liste aktualisiert sich per Flow. */
    fun deleteErfassung(erfassung: WaschErfassung, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val ok = erfassungRepository.deleteErfassung(erfassung.id)
                if (ok) {
                val s = _uiState.value
                if (s is WaschenErfassungUiState.ErfassungDetail && s.erfassung.id == erfassung.id) {
                    val customer = customerRepository.getCustomerById(erfassung.customerId)
                    if (customer != null) {
                        _uiState.value = WaschenErfassungUiState.ErfassungenListe(customer)
                        erfassungenJob?.cancel()
                        erfassungenJob = viewModelScope.launch {
                            erfassungRepository.getErfassungenByCustomerFlow(customer.id).collect {
                                _erfassungenList.value = it
                            }
                        }
                    }
                }
                onDeleted()
            }
        }
    }
}
