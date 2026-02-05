package com.example.we2026_5.ui.listebearbeiten

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.R
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ListeBearbeitenState(
    val liste: KundenListe? = null,
    val kundenInListe: List<Customer> = emptyList(),
    val verfuegbareKunden: List<Customer> = emptyList(),
    val intervalle: List<ListeIntervall> = emptyList(),
    val isInEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessageResId: Int? = null,
    val errorMessageArg: String? = null,
    val isEmpty: Boolean = false
)

class ListeBearbeitenViewModel(
    private val listeRepository: KundenListeRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ListeBearbeitenState())
    val state: StateFlow<ListeBearbeitenState> = _state.asStateFlow()

    fun loadDaten(listeId: String?) {
        val targetId = listeId ?: _state.value.liste?.id ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessageResId = null, errorMessageArg = null)
            try {
                val geladeneListe = withContext(Dispatchers.IO) { listeRepository.getListeById(targetId) }
                if (geladeneListe == null) {
                    _state.value = _state.value.copy(isLoading = false, errorMessageResId = R.string.error_list_not_found)
                    return@launch
                }
                val alleKunden = withContext(Dispatchers.IO) { customerRepository.getAllCustomers() }
                val inListe = if (geladeneListe.wochentag in 0..6) {
                    alleKunden.filter { k ->
                        geladeneListe.wochentag in k.effectiveAbholungWochentage ||
                        geladeneListe.wochentag in k.effectiveAuslieferungWochentage
                    }.sortedBy { it.name }
                } else {
                    alleKunden.filter { it.listeId == geladeneListe.id }.sortedBy { it.name }
                }
                val verfuegbar = if (geladeneListe.wochentag in 0..6) {
                    alleKunden.filter { k ->
                        geladeneListe.wochentag !in k.effectiveAbholungWochentage &&
                        geladeneListe.wochentag !in k.effectiveAuslieferungWochentage
                    }.sortedBy { it.name }
                } else {
                    alleKunden.filter { it.listeId.isEmpty() && it.kundenArt == "Tour" }.sortedBy { it.name }
                }
                val intervalle = if (_state.value.isInEditMode) _state.value.intervalle else geladeneListe.intervalle
                _state.value = _state.value.copy(
                    liste = geladeneListe,
                    kundenInListe = inListe,
                    verfuegbareKunden = verfuegbar,
                    intervalle = if (_state.value.isInEditMode) intervalle else geladeneListe.intervalle,
                    isLoading = false,
                    isEmpty = inListe.isEmpty() && verfuegbar.isEmpty()
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessageResId = R.string.error_load_generic,
                    errorMessageArg = e.message
                )
            }
        }
    }

    fun setEditMode(isEditing: Boolean) {
        val liste = _state.value.liste ?: return
        _state.value = _state.value.copy(
            isInEditMode = isEditing,
            intervalle = if (isEditing) liste.intervalle else _state.value.intervalle
        )
    }

    fun updateListe(updatedListe: KundenListe) {
        _state.value = _state.value.copy(
            liste = updatedListe,
            intervalle = updatedListe.intervalle
        )
    }

    fun updateIntervalle(newIntervalle: List<ListeIntervall>) {
        _state.value = _state.value.copy(intervalle = newIntervalle)
    }

    fun clearErrorMessage() {
        _state.value = _state.value.copy(errorMessageResId = null, errorMessageArg = null)
    }
}
