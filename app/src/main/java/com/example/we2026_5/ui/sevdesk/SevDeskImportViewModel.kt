package com.example.we2026_5.ui.sevdesk

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.data.repository.ArticleRepository
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenPreiseRepository
import com.example.we2026_5.util.Result
import com.example.we2026_5.sevdesk.SevDeskDeletedIds
import com.example.we2026_5.sevdesk.getSevDeskToken
import com.example.we2026_5.sevdesk.setSevDeskToken
import com.example.we2026_5.sevdesk.SevDeskImport
import com.example.we2026_5.sevdesk.SevDeskKundenpreiseImport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

data class SevDeskImportState(
    val token: String = "",
    val isImportingContacts: Boolean = false,
    val isImportingArticles: Boolean = false,
    val isImportingPrices: Boolean = false,
    val isDeletingSevDeskContacts: Boolean = false,
    val isDeletingSevDeskArticles: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class SevDeskImportViewModel(
    private val context: Context,
    private val customerRepository: CustomerRepository,
    private val articleRepository: ArticleRepository,
    private val kundenPreiseRepository: KundenPreiseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SevDeskImportState(token = getSevDeskToken(context)))
    val state: StateFlow<SevDeskImportState> = _state.asStateFlow()

    fun setToken(token: String) {
        _state.value = _state.value.copy(token = token, error = null)
    }

    fun saveToken() {
        setSevDeskToken(context, _state.value.token.trim())
        _state.value = _state.value.copy(message = "Token gespeichert")
    }

    /** Nur Kontakte importieren (keine Kundenpreise). Preise separat über „Preise importieren“. */
    fun importContacts() {
        val token = _state.value.token.trim()
        if (token.isEmpty()) {
            _state.value = _state.value.copy(error = "Bitte API-Token eingeben.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isImportingContacts = true, error = null, message = null)
            val result = withContext(Dispatchers.IO) {
                SevDeskImport(context, customerRepository, articleRepository).importContacts(token)
            }
            _state.value = _state.value.copy(isImportingContacts = false)
            result.fold(
                onSuccess = { (created, updated) ->
                    val msg = buildString {
                        if (created > 0) append("$created neu angelegt. ")
                        if (updated > 0) append("$updated aktualisiert (nur Name/Adresse aus SevDesk). ")
                        if (created == 0 && updated == 0) append("Keine Änderungen. ")
                        else append("Alias/Termine etc. bleiben unverändert.")
                    }
                    _state.value = _state.value.copy(message = msg.trim())
                }
            ) { e -> _state.value = _state.value.copy(error = e.message ?: "Fehler beim Import.") }
        }
    }

    /** Kundenpreise (PartContactPrice) von SevDesk importieren. Kontakte und Artikel sollten bereits importiert sein. */
    fun importKundenpreise() {
        val token = _state.value.token.trim()
        if (token.isEmpty()) {
            _state.value = _state.value.copy(error = "Bitte API-Token eingeben.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isImportingPrices = true, error = null, message = null)
            val result = withContext(Dispatchers.IO) {
                SevDeskKundenpreiseImport(customerRepository, articleRepository, kundenPreiseRepository)
                    .importKundenpreise(token)
            }
            _state.value = _state.value.copy(isImportingPrices = false)
            result.fold(
                onSuccess = { count ->
                    _state.value = _state.value.copy(
                        message = if (count > 0) "$count Kunden mit Kundenpreisen übernommen."
                        else "Keine Kundenpreise zugeordnet. Zuerst Kontakte und Artikel importieren."
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(error = e.message ?: "Fehler beim Preise-Import.")
                }
            )
        }
    }

    fun importArticles() {
        val token = _state.value.token.trim()
        if (token.isEmpty()) {
            _state.value = _state.value.copy(error = "Bitte API-Token eingeben.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isImportingArticles = true, error = null, message = null)
            val result = withContext(Dispatchers.IO) {
                SevDeskImport(context, customerRepository, articleRepository).importArticles(token)
            }
            _state.value = _state.value.copy(isImportingArticles = false)
            result.fold(
                onSuccess = { count -> _state.value = _state.value.copy(message = "$count Artikel importiert.") }
            ) { e -> _state.value = _state.value.copy(error = e.message ?: "Fehler beim Import.") }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }

    fun deleteAllSevDeskContacts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeletingSevDeskContacts = true, error = null, message = null)
            val result = withContext(Dispatchers.IO) { customerRepository.deleteAllSevDeskContacts() }
            _state.value = _state.value.copy(isDeletingSevDeskContacts = false)
            when (result) {
                is Result.Success -> {
                    val (count, kundennummern) = result.data
                    if (kundennummern.isNotEmpty()) {
                        SevDeskDeletedIds.addAll(context, kundennummern)
                    }
                    _state.value = _state.value.copy(message = "$count SevDesk-Kontakte gelöscht (werden beim Re-Import nicht wieder angelegt).")
                }
                is Result.Error -> _state.value = _state.value.copy(error = result.message)
            }
        }
    }

    fun deleteAllSevDeskArticles() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isDeletingSevDeskArticles = true, error = null, message = null)
            val count = withContext(Dispatchers.IO) { articleRepository.deleteAllSevDeskArticles() }
            _state.value = _state.value.copy(isDeletingSevDeskArticles = false)
            _state.value = _state.value.copy(message = "$count SevDesk-Artikel gelöscht.")
        }
    }

    /** Re-Import-Ignore-Liste leeren – beim nächsten Kontakte-Import werden alle SevDesk-Kontakte wieder angelegt. */
    fun clearReimportIgnoreList() {
        SevDeskDeletedIds.clear(context)
        _state.value = _state.value.copy(message = "Re-Import-Liste geleert. Beim nächsten Import werden alle Kontakte aus SevDesk wieder berücksichtigt.")
    }
}
