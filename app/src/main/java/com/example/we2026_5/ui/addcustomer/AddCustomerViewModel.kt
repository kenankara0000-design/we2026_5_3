package com.example.we2026_5.ui.addcustomer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.we2026_5.KundenTyp

data class AddCustomerState(
    val name: String = "",
    val adresse: String = "",
    val stadt: String = "",
    val plz: String = "",
    val telefon: String = "",
    val notizen: String = "",
    val kundenArt: String = "Gewerblich",
    val kundenTyp: KundenTyp = KundenTyp.REGELMAESSIG,
    val listenWochentag: Int = -1,
    val intervallTage: Int = 7,
    val kundennummer: String = "",
    val abholungWochentag: Int = -1,
    val auslieferungWochentag: Int = -1,
    val defaultUhrzeit: String = "",
    val tagsInput: String = "",
    val tourWochentag: Int = -1,
    val tourStadt: String = "",
    val tourZeitStart: String = "",
    val tourZeitEnde: String = "",
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
        val s = _state.value ?: AddCustomerState()
        _state.value = s.copy(
            kundenTyp = typ,
            listenWochentag = if (typ == KundenTyp.REGELMAESSIG && s.listenWochentag < 0 && s.abholungWochentag >= 0) s.abholungWochentag else s.listenWochentag
        )
    }

    fun setListenWochentag(tag: Int) {
        _state.value = (_state.value ?: AddCustomerState()).copy(listenWochentag = tag)
    }

    fun setIntervallTage(tage: Int) {
        _state.value = (_state.value ?: AddCustomerState()).copy(intervallTage = tage.coerceIn(1, 365))
    }

    fun setKundennummer(nummer: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(kundennummer = nummer)
    }

    fun setAbholungWochentag(tag: Int) {
        val s = _state.value ?: AddCustomerState()
        _state.value = s.copy(
            abholungWochentag = tag,
            listenWochentag = if (s.kundenTyp == KundenTyp.REGELMAESSIG && s.listenWochentag < 0 && tag >= 0) tag else s.listenWochentag
        )
    }

    fun setAuslieferungWochentag(tag: Int) {
        _state.value = (_state.value ?: AddCustomerState()).copy(auslieferungWochentag = tag)
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

    fun setSaving(isSaving: Boolean) {
        _state.value = (_state.value ?: AddCustomerState()).copy(isSaving = isSaving)
    }

    fun setError(message: String?) {
        _state.value = (_state.value ?: AddCustomerState()).copy(errorMessage = message, isSaving = false)
    }

    fun setSuccess() {
        _state.value = (_state.value ?: AddCustomerState()).copy(success = true, isSaving = false)
    }
}
