package com.example.we2026_5.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.AusnahmeTermin
import com.example.we2026_5.KundenTermin
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.CustomerSnapshotParser
import com.example.we2026_5.util.TerminAusKundeUtils
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.ui.addcustomer.AddCustomerState
import com.example.we2026_5.util.Result
import com.example.we2026_5.util.intervallTageOrDefault
import com.example.we2026_5.util.tageAzuLOrDefault
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * ViewModel für CustomerDetailActivity.
 * Hält Kunden-State und Geschäftslogik (Laden, Speichern, Löschen).
 * Activity beobachtet nur State und leitet Klicks weiter.
 */
class CustomerDetailViewModel(
    private val repository: CustomerRepository
) : ViewModel() {

    private val _customerId = MutableStateFlow<String?>(null)

    /** true, sobald getCustomerFlow mindestens einmal emittiert hat (dann ist null = wirklich nicht vorhanden). */
    private val _loadComplete = MutableStateFlow(false)
    val loadComplete: StateFlow<Boolean> = _loadComplete.asStateFlow()

    /** Echtzeit-Kundendaten aus Repository. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentCustomer: StateFlow<Customer?> = _customerId
        .flatMapLatest { id ->
            if (id == null) {
                _loadComplete.value = false
                flowOf(null)
            } else {
                repository.getCustomerFlow(id).map { customer ->
                    _loadComplete.value = true
                    customer
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** true nach erfolgreichem Löschen → Activity beendet sich mit Result. */
    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    /** Kurzzeitig true während Speichern/Löschen. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** true während Foto-Upload. */
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    fun setUploading(uploading: Boolean) {
        _isUploading.value = uploading
    }

    /** Fehlermeldung für Toast/Snackbar. */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Bearbeitungsmodus (für Compose-Screen). */
    private val _isInEditMode = MutableStateFlow(false)
    val isInEditMode: StateFlow<Boolean> = _isInEditMode.asStateFlow()

    /** Intervalle im Bearbeitungsmodus (für Compose). */
    private val _editIntervalle = MutableStateFlow<List<CustomerIntervall>>(emptyList())
    val editIntervalle: StateFlow<List<CustomerIntervall>> = _editIntervalle.asStateFlow()

    /** Formular-State im Bearbeitungsmodus (überlebt Recomposition/Konfigurationswechsel, damit z. B. KundenTyp-Regelmaessig→Unregelmaessig erhalten bleibt). */
    private val _editFormState = MutableStateFlow<AddCustomerState?>(null)
    val editFormState: StateFlow<AddCustomerState?> = _editFormState.asStateFlow()

    fun setCustomerId(id: String) {
        _customerId.value = id
    }

    /**
     * Speichert Kunden-Updates. Fehler werden über [errorMessage] gemeldet (zentrale Fehlerbehandlung).
     * Wenn [newIntervalle] mitgegeben wird und sich Regel-Verwendungen geändert haben, wird die
     * Verwendungsanzahl der entfernten Regeln dekrementiert.
     * [newTageAzuLFromForm] wird bei Änderung von „Tage A zu L“ im Formular mitgegeben (wichtig für
     * unregelmäßige Kunden, da dort der Wert nicht aus den Intervallen ableitbar ist).
     * Nach Abschluss wird onComplete(success) auf dem Main-Thread aufgerufen.
     */
    fun saveCustomer(updates: Map<String, Any>, newIntervalle: List<CustomerIntervall>? = null, newTageAzuLFromForm: Int? = null, onComplete: ((Boolean) -> Unit)? = null) {
        val id = _customerId.value ?: return
        val oldCustomer = currentCustomer.value
        var finalUpdates = updates
        if (oldCustomer != null && oldCustomer.kundenTermine.isNotEmpty()) {
            val newTageAzuL = when {
                newTageAzuLFromForm != null -> newTageAzuLFromForm.coerceIn(0, 365)
                newIntervalle != null -> {
                    val firstNew = newIntervalle.firstOrNull()
                    if (firstNew != null && firstNew.abholungDatum > 0 && firstNew.auslieferungDatum > 0)
                        TimeUnit.MILLISECONDS.toDays(firstNew.auslieferungDatum - firstNew.abholungDatum).toInt().coerceIn(0, 365)
                    else 7
                }
                else -> 7
            }
            val oldTageAzuL = oldCustomer.tageAzuLOrDefault(7)
            if (newTageAzuL != oldTageAzuL) {
                val newKundenTermine = oldCustomer.kundenTermine
                    .filter { it.typ == "A" }
                    .sortedBy { it.datum }
                    .flatMap { a ->
                        val lDatum = TerminBerechnungUtils.getStartOfDay(a.datum + TimeUnit.DAYS.toMillis(newTageAzuL.toLong()))
                        listOf(KundenTermin(datum = a.datum, typ = "A"), KundenTermin(datum = lDatum, typ = "L"))
                    }
                finalUpdates = finalUpdates + ("kundenTermine" to CustomerSnapshotParser.serializeKundenTermine(newKundenTermine))
            }
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = repository.updateCustomerResult(id, finalUpdates)) {
                is Result.Success -> onComplete?.invoke(result.data)
                is Result.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun deleteCustomer() {
        val id = _customerId.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = repository.deleteCustomerResult(id)) {
                is Result.Success -> _deleted.value = true
                is Result.Error -> _errorMessage.value = result.message
            }
            _isLoading.value = false
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun setEditMode(editing: Boolean, customer: Customer? = null) {
        _isInEditMode.value = editing
        if (editing && customer != null) {
            _editIntervalle.value = customer.intervalle
            _editFormState.value = buildFormStateFromCustomer(customer)
        } else {
            _editFormState.value = null
        }
    }

    fun updateEditFormState(state: AddCustomerState) {
        _editFormState.value = state
    }

    private fun buildFormStateFromCustomer(c: Customer): AddCustomerState {
        val tageAzuL = c.tageAzuLOrDefault(7)
        val intervallTage = c.intervallTageOrDefault(7)
        return AddCustomerState(
            name = c.name,
            alias = c.alias,
            adresse = c.adresse,
            latitude = c.latitude,
            longitude = c.longitude,
            stadt = c.stadt,
            plz = c.plz,
            telefon = c.telefon,
            notizen = c.notizen,
            kundenArt = c.kundenArt,
            kundenTyp = c.kundenTyp,
            tageAzuL = tageAzuL,
            intervallTage = intervallTage,
            kundennummer = c.kundennummer,
            abholungWochentage = c.effectiveAbholungWochentage,
            auslieferungWochentage = c.effectiveAuslieferungWochentage,
            defaultUhrzeit = c.defaultUhrzeit,
            tagsInput = c.tags.joinToString(", "),
            tourStadt = c.tourSlot?.stadt ?: "",
            tourZeitStart = c.tourSlot?.zeitfenster?.start ?: "",
            tourZeitEnde = c.tourSlot?.zeitfenster?.ende ?: "",
            ohneTour = c.ohneTour,
            erstelltAm = c.erstelltAm,
            sameDayLStrategy = c.sameDayLStrategy
        )
    }

    fun updateEditIntervalle(intervalle: List<CustomerIntervall>) {
        _editIntervalle.value = intervalle
    }

    /** Hängt ein Intervall (z. B. monatlicher Wochentag) an die Bearbeitungsliste an. */
    fun addMonthlyIntervall(intervall: CustomerIntervall) {
        _editIntervalle.value = _editIntervalle.value + intervall
    }

    /** Entfernt ein Intervall aus der Bearbeitungsliste (Index 0-basiert). */
    fun removeIntervallAt(index: Int) {
        val list = _editIntervalle.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _editIntervalle.value = list
        }
    }

    /**
     * Löscht den nächsten Termin (fügt Datum zu geloeschteTermine hinzu).
     * Für Kunden mit Monats-Wochentag-Intervall: „Nächster Termin“ wird löschbar; nach Löschen zeigt die Anzeige den übernächsten Termin.
     */
    fun deleteNaechstenTermin(terminDatum: Long, onComplete: (Boolean) -> Unit) {
        val customer = currentCustomer.value ?: run {
            onComplete(false)
            return
        }
        val terminDatumStart = TerminBerechnungUtils.getStartOfDay(terminDatum)
        val aktuelleGeloeschteTermine = customer.geloeschteTermine.toMutableList()
        if (!aktuelleGeloeschteTermine.contains(terminDatumStart)) {
            aktuelleGeloeschteTermine.add(terminDatumStart)
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val result = repository.updateCustomerResult(customer.id, mapOf("geloeschteTermine" to aktuelleGeloeschteTermine))) {
                is Result.Success -> onComplete(result.data)
                is Result.Error -> {
                    _errorMessage.value = result.message
                    onComplete(false)
                }
            }
            _isLoading.value = false
        }
    }

    /** Entfernt einen Ausnahme-Termin (A oder L an einem Datum). */
    fun deleteAusnahmeTermin(termin: AusnahmeTermin, onComplete: (Boolean) -> Unit) {
        val customer = currentCustomer.value ?: run {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val ok = repository.removeAusnahmeTermin(customer.id, termin)
            _isLoading.value = false
            onComplete(ok)
        }
    }

    /** Entfernt einen oder mehrere Kunden-Termine (z. B. A+L-Paar zusammen). */
    fun deleteKundenTermine(termins: List<KundenTermin>, onComplete: (Boolean) -> Unit) {
        if (termins.isEmpty()) {
            onComplete(true)
            return
        }
        val customer = currentCustomer.value ?: run {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val ok = repository.removeKundenTermine(customer.id, termins)
            _isLoading.value = false
            onComplete(ok)
        }
    }

    /** Entfernt ein Foto aus der Kundenliste (fotoUrls, fotoThumbUrls). */
    fun deletePhoto(photoUrl: String, onComplete: (Boolean) -> Unit) {
        val customer = currentCustomer.value ?: run {
            onComplete(false)
            return
        }
        val index = customer.fotoUrls.indexOf(photoUrl)
        if (index < 0) {
            onComplete(true)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val updatedUrls = customer.fotoUrls.toMutableList().apply { removeAt(index) }
            val updatedThumbUrls = customer.fotoThumbUrls.toMutableList().apply {
                if (index < size) removeAt(index)
            }
            val ok = repository.updateCustomer(customer.id, mapOf(
                "fotoUrls" to updatedUrls,
                "fotoThumbUrls" to updatedThumbUrls
            ))
            _isLoading.value = false
            onComplete(ok)
        }
    }

    /** Entfernt alle Intervalle mit der angegebenen Regel-ID aus der Bearbeitungsliste (z. B. ganze Regel „täglich“). */
    fun removeRegelFromEdit(regelId: String) {
        _editIntervalle.value = _editIntervalle.value.filter { it.terminRegelId != regelId }
    }

    /**
     * Setzt die Intervalle auf genau ein automatisches Intervall aus den Formulardaten (A-Tag, Intervall, L-Termin).
     * Entfernt manuell angelegte Termine (z. B. aus „Termin anlegen“). Nur bei REGELMAESSIG und gültigem A-Tag.
     */
    fun resetToAutomaticIntervall(customer: Customer?, formState: AddCustomerState?) {
        if (customer == null || formState == null) return
        if (customer.kundenTyp != KundenTyp.REGELMAESSIG || formState.abholungWochentage.isEmpty()) return
        val slotId = customer.tourSlot?.id ?: "customer-${customer.id}"
        val customerForIntervall = customer.copy(
            defaultAbholungWochentag = formState.abholungWochentage.firstOrNull() ?: -1,
            defaultAuslieferungWochentag = formState.auslieferungWochentage.firstOrNull() ?: -1,
            defaultAbholungWochentage = formState.abholungWochentage,
            defaultAuslieferungWochentage = formState.auslieferungWochentage,
            tourSlotId = slotId
        )
        val startDatum = formState.erstelltAm.takeIf { it > 0 } ?: TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
        val intervalle = TerminAusKundeUtils.erstelleIntervalleAusKunde(
            customerForIntervall,
            startDatum,
            formState.tageAzuL ?: 7,
            formState.intervallTage ?: 7
        )
        if (intervalle.isEmpty()) return
        _editIntervalle.value = intervalle
    }
}
