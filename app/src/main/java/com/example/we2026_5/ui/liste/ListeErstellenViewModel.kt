package com.example.we2026_5.ui.liste

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.we2026_5.KundenListe
import com.example.we2026_5.util.Result
import com.example.we2026_5.R
import com.example.we2026_5.ui.common.getWochentagFullResIds
import com.example.we2026_5.data.repository.KundenListeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ListeErstellenState(
    val listName: String = "",
    val selectedType: String = "Gewerbe",
    val isWochentagListe: Boolean = false,
    val wochentag: Int = -1,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val success: Boolean = false
)

class ListeErstellenViewModel(
    context: Context,
    private val repository: KundenListeRepository
) : AndroidViewModel(context.applicationContext as Application) {

    private val appContext: Context = context.applicationContext

    private val _state = MutableStateFlow(ListeErstellenState())
    val state: StateFlow<ListeErstellenState> = _state.asStateFlow()

    fun setListName(name: String) {
        _state.value = _state.value.copy(listName = name, errorMessage = null)
    }

    fun setSelectedType(type: String) {
        _state.value = _state.value.copy(selectedType = type)
    }

    fun setWochentagListe(isWochentagListe: Boolean) {
        val current = _state.value
        _state.value = current.copy(
            isWochentagListe = isWochentagListe,
            wochentag = if (isWochentagListe && current.wochentag < 0) 0 else current.wochentag
        )
    }

    fun setWochentag(tag: Int) {
        _state.value = _state.value.copy(wochentag = tag)
    }

    fun clearErrorMessage() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun save() {
        val current = _state.value
        val name = current.listName.trim()
        val wochentag = if (current.isWochentagListe) current.wochentag else -1
        val weekdayNames = getWochentagFullResIds().map { appContext.getString(it) }
        val finalName = if (current.isWochentagListe && name.isEmpty() && wochentag in 0..6) {
            weekdayNames[wochentag]
        } else name
        if (finalName.isEmpty()) {
            _state.value = current.copy(errorMessage = appContext.getString(R.string.validation_list_name_missing))
            return
        }
        if (current.isWochentagListe && wochentag !in 0..6) {
            _state.value = current.copy(errorMessage = appContext.getString(R.string.validation_list_wochentag))
            return
        }

        viewModelScope.launch {
            _state.value = current.copy(isSaving = true, errorMessage = null)
            val listeId = UUID.randomUUID().toString()
            val neueListe = KundenListe(
                id = listeId,
                name = finalName,
                listeArt = current.selectedType,
                wochentag = wochentag,
                intervalle = emptyList(),
                erstelltAm = System.currentTimeMillis()
            )

            val result = withContext(Dispatchers.IO) { repository.saveListe(neueListe) }
            val updated = _state.value
            when (result) {
                is Result.Success -> _state.value = updated.copy(isSaving = false, success = true)
                is Result.Error -> _state.value = updated.copy(isSaving = false, errorMessage = result.message)
                is Result.Loading -> _state.value = updated.copy(isSaving = false)
            }
        }
    }
}
