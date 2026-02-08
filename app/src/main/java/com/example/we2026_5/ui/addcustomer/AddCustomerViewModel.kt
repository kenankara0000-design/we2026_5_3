package com.example.we2026_5.ui.addcustomer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.we2026_5.KundenTyp

data class AddCustomerState(
    val name: String = "",
    /** Alias: Anzeigename in der App; für Rechnung wird name verwendet. */
    val alias: String = "",
    val adresse: String = "",
    val stadt: String = "",
    val plz: String = "",
    val telefon: String = "",
    val notizen: String = "",
    val kundenArt: String = "Gewerblich",
    val kundenTyp: KundenTyp = KundenTyp.REGELMAESSIG,
    /** Tage A→L; null = Feld gelöscht, beim Speichern wird 7 verwendet. */
    val tageAzuL: Int? = 7,
    /** Intervall-Tage; null = Feld gelöscht, beim Speichern wird 7 verwendet. */
    val intervallTage: Int? = 7,
    val kundennummer: String = "",
    val abholungWochentage: List<Int> = emptyList(),
    val auslieferungWochentage: List<Int> = emptyList(),
    val defaultUhrzeit: String = "",
    val tagsInput: String = "",
    val tourWochentag: Int = -1,
    val tourStadt: String = "",
    val tourZeitStart: String = "",
    val tourZeitEnde: String = "",
    val ohneTour: Boolean = false,
    /** A-Termin Startdatum (Tagesanfang). 0 = beim Speichern „heute“ verwenden. */
    val erstelltAm: Long = 0L,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false
)

class AddCustomerViewModel : ViewModel() {

    private val _state = MutableLiveData(AddCustomerState())
    val state: LiveData<AddCustomerState> = _state

    fun setInitialName(name: String) {
        if (_state.value?.name?.isEmpty() != false) {
            _state.value = (_state.value ?: AddCustomerState()).copy(name = name)
        }
    }

    fun setName(name: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(name = name, errorMessage = null)
    }

    fun setAdresse(adresse: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(adresse = adresse)
    }

    fun setStadt(stadt: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(stadt = stadt)
    }

    fun setPlz(plz: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(plz = plz)
    }

    fun setTelefon(telefon: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(telefon = telefon)
    }

    fun setNotizen(notizen: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(notizen = notizen)
    }

    fun setKundenArt(kundenArt: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(kundenArt = kundenArt)
    }

    fun setKundenTyp(typ: KundenTyp) {
        _state.value = (_state.value ?: AddCustomerState()).copy(kundenTyp = typ)
    }

    fun setTageAzuL(tage: Int?) {
        _state.value = (_state.value ?: AddCustomerState()).copy(tageAzuL = tage?.coerceIn(0, 365))
    }

    fun setIntervallTage(tage: Int?) {
        _state.value = (_state.value ?: AddCustomerState()).copy(intervallTage = tage?.coerceIn(1, 365))
    }

    fun setKundennummer(nummer: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(kundennummer = nummer)
    }

    fun toggleAbholungWochentag(tag: Int) {
        val list = (_state.value?.abholungWochentage ?: emptyList()).toMutableList()
        if (tag in list) list.remove(tag) else list.add(tag)
        list.sort()
        _state.value = (_state.value ?: AddCustomerState()).copy(abholungWochentage = list)
    }

    fun toggleAuslieferungWochentag(tag: Int) {
        val list = (_state.value?.auslieferungWochentage ?: emptyList()).toMutableList()
        if (tag in list) list.remove(tag) else list.add(tag)
        list.sort()
        _state.value = (_state.value ?: AddCustomerState()).copy(auslieferungWochentage = list)
    }

    fun setDefaultUhrzeit(uhrzeit: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(defaultUhrzeit = uhrzeit)
    }

    fun setTagsInput(tags: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(tagsInput = tags)
    }

    fun setTourWochentag(tag: Int) {
        _state.value = (_state.value ?: AddCustomerState()).copy(tourWochentag = tag)
    }

    fun setTourStadt(stadt: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(tourStadt = stadt)
    }

    fun setTourZeitStart(start: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(tourZeitStart = start)
    }

    fun setTourZeitEnde(ende: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(tourZeitEnde = ende)
    }

    fun setOhneTour(ohneTour: Boolean) {
        _state.value = (_state.value ?: AddCustomerState()).copy(ohneTour = ohneTour)
    }

    fun setSaving(isSaving: Boolean) {
        _state.value = (_state.value ?: AddCustomerState()).copy(isSaving = isSaving)
    }

    fun setError(message: String?) {
        _state.value = (_state.value ?: AddCustomerState()).copy(errorMessage = message, isSaving = false)
    }

    fun setSuccess() {
        _state.value = (_state.value ?: AddCustomerState()).copy(success = true, isSaving = false)
    }

    /** Setzt nur die Formularfelder (für gemeinsame CustomerStammdatenForm). */
    fun setStateFromForm(newState: AddCustomerState) {
        val current = _state.value ?: AddCustomerState()
        _state.value = newState.copy(
            isSaving = current.isSaving,
            errorMessage = current.errorMessage,
            success = current.success
        )
    }
}
