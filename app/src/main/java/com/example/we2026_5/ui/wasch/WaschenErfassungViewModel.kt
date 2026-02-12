package com.example.we2026_5.ui.wasch

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.we2026_5.R
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.data.repository.ArticleRepository
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.ErfassungRepository
import com.example.we2026_5.data.repository.KundenPreiseRepository
import com.example.we2026_5.data.repository.TourPreiseRepository
import com.example.we2026_5.wasch.Article
import com.example.we2026_5.wasch.ErfassungPosition
import com.example.we2026_5.wasch.WaschErfassung
import com.example.we2026_5.wasch.WaeschelisteFormularState
import com.example.we2026_5.util.TerminBerechnungUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Einzelne Zeile in der Erfassung (Artikel + Menge + Einheit) für die UI. Menge als Double für kg (z. B. 5,5). */
data class ErfassungZeile(
    val articleId: String,
    val artikelName: String,
    val einheit: String,
    val menge: Double
)

/** Artikel-Option in der Erfassung (mit optionalem Preis-Label bei Kundenpreisen). */
data class ArticleDisplay(
    val id: String,
    val name: String,
    val einheit: String,
    val priceLabel: String? = null
)

/** Zeile für Beleg-Detail (Artikelname, Menge, Einheit). */
data class ErfassungPositionAnzeige(
    val artikelName: String,
    val menge: Double,
    val einheit: String
)

sealed class WaschenErfassungUiState {
    /** Start: Kunde suchen (Suchfeld + Liste). */
    data class KundeSuchen(
        val customerSearchQuery: String = "",
        val customers: List<Customer> = emptyList()
    ) : WaschenErfassungUiState()
    /** Erfassungen für einen Kunden – gruppiert nach Monat (Belege). */
    data class ErfassungenListe(val customer: Customer, val showErledigtTab: Boolean = false) : WaschenErfassungUiState()
    /** Beleg-Detail: ein Monat, alle Erfassungen mit Datum/Uhrzeit. */
    data class BelegDetail(
        val customer: Customer,
        val monthKey: String,
        val monthLabel: String,
        val erfassungen: List<WaschErfassung>
    ) : WaschenErfassungUiState()
    /** Ein Beleg im Detail (nur Anzeige – einzelne Erfassung). */
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
    /** Wäscheliste-Formular: Kundendaten + Artikl-Mengen + Sonstiges. */
    data class Formular(
        val customer: Customer,
        val formularState: WaeschelisteFormularState,
        val isSaving: Boolean = false,
        val errorMessage: String? = null
    ) : WaschenErfassungUiState()
}

class WaschenErfassungViewModel(
    private val context: Context,
    private val customerRepository: CustomerRepository,
    private val articleRepository: ArticleRepository,
    private val erfassungRepository: ErfassungRepository,
    private val kundenPreiseRepository: KundenPreiseRepository,
    private val tourPreiseRepository: TourPreiseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<WaschenErfassungUiState>(WaschenErfassungUiState.KundeSuchen())
    val uiState: StateFlow<WaschenErfassungUiState> = _uiState.asStateFlow()

    private val _erfassungenList = MutableStateFlow<List<WaschErfassung>>(emptyList())
    val erfassungenList: StateFlow<List<WaschErfassung>> = _erfassungenList.asStateFlow()

    /** Belege (nach Monat gruppiert) – nur offene. */
    val belegMonate: StateFlow<List<BelegMonat>> = _erfassungenList
        .map { BelegMonatGrouping.groupByMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _erfassungenListErledigt = MutableStateFlow<List<WaschErfassung>>(emptyList())
    val belegMonateErledigt: StateFlow<List<BelegMonat>> = _erfassungenListErledigt
        .map { BelegMonatGrouping.groupByMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var erfassungenErledigtJob: Job? = null

    private val _kundenPreiseForErfassung = MutableStateFlow<List<com.example.we2026_5.wasch.KundenPreis>>(emptyList())
    /** Brutto-Preise pro Artikel für Beleg-Detail (Tour- oder Kundenpreise), für Gesamtpreis-Anzeige. */
    private val _belegPreiseGross = MutableStateFlow<Map<String, Double>>(emptyMap())
    val belegPreiseGross: StateFlow<Map<String, Double>> = _belegPreiseGross.asStateFlow()
    private var kundenPreiseJob: Job? = null
    private var erfassungenJob: Job? = null
    /** Cache für Kunde-Suche: nur bei Eingabe Treffer anzeigen, nicht alle Kunden beim Start. */
    private var customersSearchCache: List<Customer>? = null

    val articles = articleRepository.getAllArticlesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Artikel für Erfassung: bei Kundenpreisen nur diese mit Preis, sonst alle mit Hinweis „Allgemeine Preise“. */
    val erfassungArticles: StateFlow<List<ArticleDisplay>> = combine(
        articles,
        _kundenPreiseForErfassung
    ) { arts, preise ->
        if (preise.isEmpty()) {
            arts.map { ArticleDisplay(it.id, it.name, it.einheit.ifBlank { "Stk" }, null) }
        } else {
            val artsMap = arts.associateBy { it.id }
            preise.map { p ->
                val a = artsMap[p.articleId]
                ArticleDisplay(
                    p.articleId,
                    a?.name ?: p.articleId,
                    (a?.einheit).orEmpty().ifBlank { "Stk" },
                    "%.2f €".format(p.priceGross)
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val showAllgemeinePreiseHint: StateFlow<Boolean> = _kundenPreiseForErfassung
        .map { it.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        // Keine Kundenliste beim Start – nur Treffer bei Suche anzeigen
    }

    fun startNeueErfassung() {
        customersSearchCache = null
        _uiState.value = WaschenErfassungUiState.KundeSuchen(customerSearchQuery = "", customers = emptyList())
    }

    fun setCustomerSearchQuery(query: String) {
        val s = _uiState.value
        if (s !is WaschenErfassungUiState.KundeSuchen) return
        // Sofort Query aktualisieren, damit der Cursor nicht zurückspringt (Recomposition)
        if (query.isBlank()) {
            _uiState.value = s.copy(customerSearchQuery = query, customers = emptyList())
            return
        }
        _uiState.value = s.copy(customerSearchQuery = query)
        viewModelScope.launch {
            if (customersSearchCache == null) {
                customersSearchCache = customerRepository.getAllCustomers().sortedBy { it.displayName }
            }
            val filtered = customersSearchCache!!.filter {
                it.displayName.contains(query, ignoreCase = true)
            }
            val now = _uiState.value
            if (now is WaschenErfassungUiState.KundeSuchen && now.customerSearchQuery == query) {
                _uiState.value = now.copy(customers = filtered)
            }
        }
    }

    fun kundeGewaehlt(customer: Customer) {
        erfassungenJob?.cancel()
        erfassungenErledigtJob?.cancel()
        _uiState.value = WaschenErfassungUiState.ErfassungenListe(customer, showErledigtTab = false)
        erfassungenJob = viewModelScope.launch {
            erfassungRepository.getErfassungenByCustomerFlow(customer.id).collect {
                _erfassungenList.value = it
            }
        }
        erfassungenErledigtJob = viewModelScope.launch {
            erfassungRepository.getErfassungenByCustomerFlowErledigt(customer.id).collect {
                _erfassungenListErledigt.value = it
            }
        }
    }

    fun backToKundeSuchen() {
        erfassungenJob?.cancel()
        erfassungenErledigtJob?.cancel()
        _erfassungenList.value = emptyList()
        _erfassungenListErledigt.value = emptyList()
        customersSearchCache = null
        _uiState.value = WaschenErfassungUiState.KundeSuchen(customerSearchQuery = "", customers = emptyList())
    }

    /** Von ErfassungenListe: Beleg (Monat) öffnen. Lädt Brutto-Preise für Gesamtpreis-Anzeige (Tour- oder Kundenpreise). */
    fun openBelegDetail(beleg: BelegMonat) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.ErfassungenListe) {
            _uiState.value = WaschenErfassungUiState.BelegDetail(
                customer = s.customer,
                monthKey = beleg.monthKey,
                monthLabel = beleg.monthLabel,
                erfassungen = beleg.erfassungen
            )
            viewModelScope.launch {
                val customer = s.customer
                val isTourKunde = customer.tourSlotId.isNotEmpty() && !customer.ohneTour
                val map = withContext(Dispatchers.IO) {
                    if (isTourKunde) {
                        tourPreiseRepository.getTourPreise().associate { it.articleId to it.priceGross }
                    } else {
                        kundenPreiseRepository.getKundenPreiseForCustomer(customer.id).associate { it.articleId to it.priceGross }
                    }
                }
                _belegPreiseGross.value = map
            }
        }
    }

    fun backFromBelegDetail() {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.BelegDetail) {
            _belegPreiseGross.value = emptyMap()
            _uiState.value = WaschenErfassungUiState.ErfassungenListe(s.customer)
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
        kundenPreiseJob?.cancel()
        _kundenPreiseForErfassung.value = emptyList()
        val isTourKunde = customer.tourSlotId.isNotEmpty() && !customer.ohneTour
        kundenPreiseJob = viewModelScope.launch {
            if (isTourKunde) {
                tourPreiseRepository.getTourPreiseFlow().collect { tourPreise ->
                    val asKundenPreis = tourPreise.map { p ->
                        com.example.we2026_5.wasch.KundenPreis(
                            customerId = customer.id,
                            articleId = p.articleId,
                            priceNet = p.priceNet,
                            priceGross = p.priceGross
                        )
                    }
                    _kundenPreiseForErfassung.value = asKundenPreis
                }
            } else {
                kundenPreiseRepository.getKundenPreiseForCustomerFlow(customer.id).collect {
                    _kundenPreiseForErfassung.value = it
                }
            }
        }
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

    fun setBelegListeShowErledigtTab(showErledigt: Boolean) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.ErfassungenListe) _uiState.value = s.copy(showErledigtTab = showErledigt)
    }

    fun setArtikelSearchQuery(query: String) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Erfassen) {
            _uiState.value = s.copy(artikelSearchQuery = query)
        }
    }

    fun addPosition(article: Article) {
        addPositionFromDisplay(ArticleDisplay(article.id, article.name, article.einheit.ifBlank { "Stk" }, null))
    }

    fun addPositionFromDisplay(display: ArticleDisplay) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Erfassen) {
            val newZeile = ErfassungZeile(
                articleId = display.id,
                artikelName = display.name,
                einheit = display.einheit,
                menge = 0.0
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

    fun setMenge(articleId: String, menge: Double) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Erfassen) {
            val updated = s.zeilen.map { z ->
                if (z.articleId == articleId) z.copy(menge = menge.coerceAtLeast(0.0)) else z
            }
            _uiState.value = s.copy(zeilen = updated)
        }
    }

    fun setMengeByIndex(index: Int, menge: Double) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Erfassen && index in s.zeilen.indices) {
            val list = s.zeilen.toMutableList()
            list[index] = list[index].copy(menge = menge.coerceAtLeast(0.0))
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
        val positionen = s.zeilen.filter { it.menge > 0.0 }.map { z ->
            ErfassungPosition(articleId = z.articleId, menge = z.menge, einheit = z.einheit)
        }
        if (positionen.isEmpty()) {
            _uiState.value = s.copy(errorMessage = context.getString(R.string.error_erfassen_min_artikel))
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
                _uiState.value = s.copy(errorMessage = context.getString(R.string.wasch_fehler_speichern))
            }
        }
    }

    fun backToAuswahl() {
        backToKundeSuchen()
    }

    fun backFromErfassenToListe() {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Erfassen) {
            kundenPreiseJob?.cancel()
            _kundenPreiseForErfassung.value = emptyList()
            _uiState.value = WaschenErfassungUiState.ErfassungenListe(s.customer)
            erfassungenJob?.cancel()
            erfassungenJob = viewModelScope.launch {
                erfassungRepository.getErfassungenByCustomerFlow(s.customer.id).collect {
                    _erfassungenList.value = it
                }
            }
        }
    }

    /** Öffnet Wäscheliste-Formular für den Kunden (Kundendaten vorbelegt). */
    fun openFormular(customer: Customer) {
        val adresse = listOf(customer.adresse, customer.plz, customer.stadt).filter { it.isNotBlank() }.joinToString(", ")
        val formState = WaeschelisteFormularState(
            name = customer.displayName,
            adresse = adresse,
            telefon = customer.telefon
        )
        _uiState.value = WaschenErfassungUiState.Formular(customer = customer, formularState = formState)
    }

    fun backFromFormularToListe() {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Formular) {
            _uiState.value = WaschenErfassungUiState.ErfassungenListe(s.customer)
            erfassungenJob?.cancel()
            erfassungenJob = viewModelScope.launch {
                erfassungRepository.getErfassungenByCustomerFlow(s.customer.id).collect {
                    _erfassungenList.value = it
                }
            }
        }
    }

    fun setFormularKundendaten(name: String, adresse: String, telefon: String) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Formular) {
            _uiState.value = s.copy(formularState = s.formularState.withKundendaten(name, adresse, telefon))
        }
    }

    fun setFormularName(name: String) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Formular) {
            _uiState.value = s.copy(formularState = s.formularState.copy(name = name))
        }
    }

    fun setFormularAdresse(adresse: String) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Formular) {
            _uiState.value = s.copy(formularState = s.formularState.copy(adresse = adresse))
        }
    }

    fun setFormularTelefon(telefon: String) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Formular) {
            _uiState.value = s.copy(formularState = s.formularState.copy(telefon = telefon))
        }
    }

    fun setFormularMenge(key: String, value: Int) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Formular) {
            _uiState.value = s.copy(formularState = s.formularState.withMenge(key, value.coerceAtLeast(0)))
        }
    }

    fun setFormularSonstiges(sonstiges: String) {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Formular) {
            _uiState.value = s.copy(formularState = s.formularState.withSonstiges(sonstiges))
        }
    }

    private var pendingFormularOnSaved: (() -> Unit)? = null

    /** Speichert Wäscheliste-Formular als eine WaschErfassung (nur Positionen mit Menge > 0). Optional: Stammdaten-Vorschlag. */
    fun saveFormular(
        onSaved: () -> Unit,
        onSuggestStammdatenUpdate: ((Customer, String, String) -> Unit)? = null
    ) {
        val s = _uiState.value
        if (s !is WaschenErfassungUiState.Formular) return
        val posList = s.formularState.mengen.filter { it.value > 0 }.map { (key, menge) ->
            ErfassungPosition(articleId = key, menge = menge.toDouble(), einheit = "Stk")
        }
        if (posList.isEmpty()) {
            _uiState.value = s.copy(errorMessage = context.getString(R.string.error_waescheliste_min_menge))
            return
        }
        viewModelScope.launch {
            _uiState.value = s.copy(isSaving = true, errorMessage = null)
            val now = System.currentTimeMillis()
            val datum = TerminBerechnungUtils.getStartOfDay(now)
            val zeit = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(now))
            val notiz = s.formularState.sonstiges.takeIf { it.isNotBlank() }.orEmpty()
            val ok = erfassungRepository.saveErfassungNew(
                WaschErfassung(
                    customerId = s.customer.id,
                    datum = datum,
                    zeit = zeit,
                    positionen = posList,
                    notiz = notiz
                )
            )
            _uiState.value = s.copy(isSaving = false)
            if (ok) {
                val suggestAdresse = s.customer.adresse.isBlank() && s.formularState.adresse.isNotBlank()
                val suggestTelefon = s.customer.telefon.isBlank() && s.formularState.telefon.isNotBlank()
                if ((suggestAdresse || suggestTelefon) && onSuggestStammdatenUpdate != null) {
                    pendingFormularOnSaved = onSaved
                    onSuggestStammdatenUpdate(s.customer, s.formularState.adresse, s.formularState.telefon)
                    return@launch
                }
                _uiState.value = WaschenErfassungUiState.ErfassungenListe(s.customer)
                erfassungenJob?.cancel()
                erfassungenJob = viewModelScope.launch {
                    erfassungRepository.getErfassungenByCustomerFlow(s.customer.id).collect {
                        _erfassungenList.value = it
                    }
                }
                onSaved()
            } else {
                _uiState.value = s.copy(errorMessage = context.getString(R.string.wasch_fehler_speichern))
            }
        }
    }

    /** Nach Bestätigung im Stammdaten-Dialog: zurück zur Liste und onSaved ausführen. */
    fun finishFormularAfterStammdatenConfirm() {
        val s = _uiState.value
        if (s is WaschenErfassungUiState.Formular) {
            _uiState.value = WaschenErfassungUiState.ErfassungenListe(s.customer)
            erfassungenJob?.cancel()
            erfassungenJob = viewModelScope.launch {
                erfassungRepository.getErfassungenByCustomerFlow(s.customer.id).collect {
                    _erfassungenList.value = it
                }
            }
            pendingFormularOnSaved?.invoke()
            pendingFormularOnSaved = null
        }
    }

    /** Stammdaten-Dialog abgelehnt: trotzdem zur Liste wechseln und onSaved ausführen. */
    fun finishFormularWithoutStammdatenUpdate() {
        pendingFormularOnSaved?.let { cb ->
            val s = _uiState.value
            if (s is WaschenErfassungUiState.Formular) {
                _uiState.value = WaschenErfassungUiState.ErfassungenListe(s.customer)
                erfassungenJob?.cancel()
                erfassungenJob = viewModelScope.launch {
                    erfassungRepository.getErfassungenByCustomerFlow(s.customer.id).collect {
                        _erfassungenList.value = it
                    }
                }
                cb()
            }
            pendingFormularOnSaved = null
        }
    }

    /**
     * Stub: Bild von Kamera/Galerie für spätere OCR-Erkennung.
     * Aufruf aus Activity nach Foto-Auswahl (Kamera: temporäre Datei-URI, Galerie: Content-URI).
     * Später: URI hier entgegennehmen (z. B. als String), OCR ausführen, Ergebnis in
     * [WaschenErfassungUiState.Formular].formularState schreiben (Kundendaten, Mengen, Sonstiges).
     */
    fun onFormularScanImageStub() {
        // Platzhalter – OCR später einbauen.
    }

    /** Erfassung löschen; bei Erfolg aus Detail zurück zur Liste, Liste aktualisiert sich per Flow. */
    fun deleteErfassung(erfassung: WaschErfassung, onDeleted: () -> Unit) {
        viewModelScope.launch {
            val ok = erfassungRepository.deleteErfassung(erfassung.id)
                if (ok) {
                val s = _uiState.value
                val customer = customerRepository.getCustomerById(erfassung.customerId)
                if (customer != null) {
                    when (s) {
                        is WaschenErfassungUiState.ErfassungDetail -> if (s.erfassung.id == erfassung.id) {
                            _uiState.value = WaschenErfassungUiState.ErfassungenListe(customer)
                            erfassungenJob?.cancel()
                            erfassungenJob = viewModelScope.launch {
                                erfassungRepository.getErfassungenByCustomerFlow(customer.id).collect {
                                    _erfassungenList.value = it
                                }
                            }
                        }
                        is WaschenErfassungUiState.BelegDetail -> {
                            _uiState.value = WaschenErfassungUiState.ErfassungenListe(customer)
                            erfassungenJob?.cancel()
                            erfassungenJob = viewModelScope.launch {
                                erfassungRepository.getErfassungenByCustomerFlow(customer.id).collect {
                                    _erfassungenList.value = it
                                }
                            }
                        }
                        else -> { }
                    }
                }
                onDeleted()
            }
        }
    }

    /** Beleg als erledigt markieren; danach zurück zur Beleg-Liste (Beleg erscheint im Erledigt-Bereich). */
    fun markBelegErledigt(erfassungen: List<WaschErfassung>, onMarked: () -> Unit) {
        viewModelScope.launch {
            val ok = erfassungRepository.markBelegErledigt(erfassungen)
            if (ok && erfassungen.isNotEmpty()) {
                val s = _uiState.value
                if (s is WaschenErfassungUiState.BelegDetail) {
                    _uiState.value = WaschenErfassungUiState.ErfassungenListe(s.customer)
                    erfassungenJob?.cancel()
                    erfassungenJob = viewModelScope.launch {
                        erfassungRepository.getErfassungenByCustomerFlow(s.customer.id).collect {
                            _erfassungenList.value = it
                        }
                    }
                }
                onMarked()
            }
        }
    }

    /** Alle Erfassungen des geöffneten Belegs (Monat) löschen; danach zurück zur Beleg-Liste. */
    fun deleteBeleg(erfassungen: List<WaschErfassung>, onDeleted: () -> Unit) {
        viewModelScope.launch {
            for (e in erfassungen) {
                erfassungRepository.deleteErfassung(e.id)
            }
            if (erfassungen.isNotEmpty()) {
                val s = _uiState.value
                if (s is WaschenErfassungUiState.BelegDetail) {
                    _uiState.value = WaschenErfassungUiState.ErfassungenListe(s.customer)
                    erfassungenJob?.cancel()
                    erfassungenJob = viewModelScope.launch {
                        erfassungRepository.getErfassungenByCustomerFlow(s.customer.id).collect {
                            _erfassungenList.value = it
                        }
                    }
                }
                onDeleted()
            }
        }
    }
}
