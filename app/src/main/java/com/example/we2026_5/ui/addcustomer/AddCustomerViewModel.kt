package com.example.we2026_5.ui.addcustomer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class AddCustomerState(
    val name: String = "",
    val adresse: String = "",
    val telefon: String = "",
    val notizen: String = "",
    val kundenArt: String = "Gewerblich",
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

    fun setTelefon(telefon: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(telefon = telefon)
    }

    fun setNotizen(notizen: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(notizen = notizen)
    }

    fun setKundenArt(kundenArt: String) {
        _state.value = (_state.value ?: AddCustomerState()).copy(kundenArt = kundenArt)
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
