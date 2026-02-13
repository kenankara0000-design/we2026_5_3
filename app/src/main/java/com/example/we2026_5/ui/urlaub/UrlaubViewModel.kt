package com.example.we2026_5.ui.urlaub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.Customer
import com.example.we2026_5.UrlaubEintrag
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UrlaubViewModel(
    private val repository: CustomerRepository,
    private val customerId: String
) : ViewModel() {

    private val _customer = MutableStateFlow<Customer?>(null)
    val customer: StateFlow<Customer?> = _customer.asStateFlow()

    /** Effektive Liste: urlaubEintraege, oder ein Eintrag aus urlaubVon/urlaubBis. */
    fun getEffectiveUrlaubEintraege(c: Customer?): List<UrlaubEintrag> {
        if (c == null) return emptyList()
        return if (c.urlaubEintraege.isNotEmpty()) c.urlaubEintraege
        else if (c.urlaubVon > 0L && c.urlaubBis > 0L) listOf(UrlaubEintrag(c.urlaubVon, c.urlaubBis))
        else emptyList()
    }

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadCustomer()
    }

    private fun loadCustomer() {
        viewModelScope.launch {
            repository.getCustomerFlow(customerId).collect {
                _customer.value = it
            }
        }
    }

    private fun buildUpdates(newList: List<UrlaubEintrag>): Map<String, Any> {
        val first = newList.firstOrNull()
        val urlaubVon = first?.von ?: 0L
        val urlaubBis = first?.bis ?: 0L
        val listForFirebase = newList.map { mapOf("von" to it.von, "bis" to it.bis) }
        return mapOf(
            "urlaubEintraege" to listForFirebase,
            "urlaubVon" to urlaubVon,
            "urlaubBis" to urlaubBis
        )
    }

    fun saveUrlaub(urlaubVon: Long, urlaubBis: Long, onComplete: ((Boolean) -> Unit)? = null) {
        val c = _customer.value ?: run { onComplete?.invoke(false); return }
        val current = getEffectiveUrlaubEintraege(c)
        val newList = current + UrlaubEintrag(urlaubVon, urlaubBis)
        saveUrlaubList(newList, onComplete)
    }

    fun updateUrlaub(index: Int, urlaubVon: Long, urlaubBis: Long, onComplete: ((Boolean) -> Unit)? = null) {
        val c = _customer.value ?: run { onComplete?.invoke(false); return }
        val current = getEffectiveUrlaubEintraege(c)
        if (index < 0 || index >= current.size) { onComplete?.invoke(false); return }
        val newList = current.toMutableList().apply { set(index, UrlaubEintrag(urlaubVon, urlaubBis)) }
        saveUrlaubList(newList, onComplete)
    }

    fun deleteUrlaub(index: Int, onComplete: ((Boolean) -> Unit)? = null) {
        val c = _customer.value ?: run { onComplete?.invoke(false); return }
        val current = getEffectiveUrlaubEintraege(c)
        if (index < 0 || index >= current.size) { onComplete?.invoke(false); return }
        val newList = current.toMutableList().apply { removeAt(index) }
        saveUrlaubList(newList, onComplete)
    }

    private fun saveUrlaubList(newList: List<UrlaubEintrag>, onComplete: ((Boolean) -> Unit)?) {
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            when (val result = repository.updateCustomerResult(customerId, buildUpdates(newList))) {
                is Result.Success -> onComplete?.invoke(result.data)
                is Result.Error -> _errorMessage.value = result.message
                is Result.Loading -> { /* Ignorieren */ }
            }
            _isSaving.value = false
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
