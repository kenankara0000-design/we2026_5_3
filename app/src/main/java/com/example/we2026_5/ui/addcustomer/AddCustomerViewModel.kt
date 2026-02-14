package com.example.we2026_5.ui.addcustomer

import androidx.lifecycle.ViewModel
import com.example.we2026_5.KundenTyp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AddCustomerState(
    val name: String = "",
    /** Alias: Anzeigename in der App; für Rechnung wird name verwendet. */
    val alias: String = "",
    val adresse: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
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
    /** Wenn A- und L-Tage gleich: 0 = L am selben Tag, 7 = L eine Woche später. null = 0. */
    val sameDayLStrategy: Int? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false
)

class AddCustomerViewModel : ViewModel() {

    private val _state = MutableStateFlow(AddCustomerState())
    val state: StateFlow<AddCustomerState> = _state.asStateFlow()

    fun setInitialName(name: String) {
        if (_state.value.name.isEmpty()) {
            _state.value = _state.value.copy(name = name)
        }
    }

    fun setName(name: String) {
        _state.value = (_state.value).copy(name = name, errorMessage = null)
    }

    fun setAdresse(adresse: String) {
        _state.value = (_state.value).copy(adresse = adresse)
    }

    fun setStadt(stadt: String) {
        _state.value = (_state.value).copy(stadt = stadt)
    }

    fun setPlz(plz: String) {
        _state.value = (_state.value).copy(plz = plz)
    }

    fun setTelefon(telefon: String) {
        _state.value = (_state.value).copy(telefon = telefon)
    }

    fun setNotizen(notizen: String) {
        _state.value = (_state.value).copy(notizen = notizen)
    }

    fun setKundenArt(kundenArt: String) {
        _state.value = (_state.value).copy(kundenArt = kundenArt)
    }

    fun setKundenTyp(typ: KundenTyp) {
        _state.value = (_state.value).copy(kundenTyp = typ)
    }

    fun setTageAzuL(tage: Int?) {
        _state.value = (_state.value).copy(tageAzuL = tage?.coerceIn(0, 365))
    }

    fun setIntervallTage(tage: Int?) {
        _state.value = (_state.value).copy(intervallTage = tage?.coerceIn(1, 365))
    }

    fun setKundennummer(nummer: String) {
        _state.value = (_state.value).copy(kundennummer = nummer)
    }

    fun toggleAbholungWochentag(tag: Int) {
        val list = (_state.value?.abholungWochentage ?: emptyList()).toMutableList()
        if (tag in list) list.remove(tag) else list.add(tag)
        list.sort()
        _state.value = (_state.value).copy(abholungWochentage = list)
    }

    fun toggleAuslieferungWochentag(tag: Int) {
        val list = (_state.value?.auslieferungWochentage ?: emptyList()).toMutableList()
        if (tag in list) list.remove(tag) else list.add(tag)
        list.sort()
        _state.value = (_state.value).copy(auslieferungWochentage = list)
    }

    fun setDefaultUhrzeit(uhrzeit: String) {
        _state.value = (_state.value).copy(defaultUhrzeit = uhrzeit)
    }

    fun setTagsInput(tags: String) {
        _state.value = (_state.value).copy(tagsInput = tags)
    }

    fun setTourWochentag(tag: Int) {
        _state.value = (_state.value).copy(tourWochentag = tag)
    }

    fun setTourStadt(stadt: String) {
        _state.value = (_state.value).copy(tourStadt = stadt)
    }

    fun setTourZeitStart(start: String) {
        _state.value = (_state.value).copy(tourZeitStart = start)
    }

    fun setTourZeitEnde(ende: String) {
        _state.value = (_state.value).copy(tourZeitEnde = ende)
    }

    fun setOhneTour(ohneTour: Boolean) {
        _state.value = (_state.value).copy(ohneTour = ohneTour)
    }

    fun setSaving(isSaving: Boolean) {
        _state.value = (_state.value).copy(isSaving = isSaving)
    }

    fun setError(message: String?) {
        _state.value = (_state.value).copy(errorMessage = message, isSaving = false)
    }

    fun setSuccess() {
        _state.value = (_state.value).copy(success = true, isSaving = false)
    }

    /** Setzt nur die Formularfelder (für gemeinsame CustomerStammdatenForm). */
    fun setStateFromForm(newState: AddCustomerState) {
        val current = _state.value
        _state.value = newState.copy(
            isSaving = current.isSaving,
            errorMessage = current.errorMessage,
            success = current.success
        )
    }
}
