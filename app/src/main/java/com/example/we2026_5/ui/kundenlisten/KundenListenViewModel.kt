package com.example.we2026_5.ui.kundenlisten

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.KundenListe
import com.example.we2026_5.R
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class KundenListenState {
    object Loading : KundenListenState()
    object Empty : KundenListenState()
    data class Error(val messageResId: Int, val messageArg: String? = null) : KundenListenState()
    data class Success(
        val listen: List<KundenListe>,
        val kundenProListe: Map<String, Int>
    ) : KundenListenState()
}

class KundenListenViewModel(
    private val listeRepository: KundenListeRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableStateFlow<KundenListenState>(KundenListenState.Loading)
    val state: StateFlow<KundenListenState> = _state.asStateFlow()

    fun loadListen() {
        viewModelScope.launch {
            _state.value = KundenListenState.Loading
            try {
                val listen = withContext(Dispatchers.IO) { listeRepository.getAllListen() }
                val allCustomers = withContext(Dispatchers.IO) { customerRepository.getAllCustomers() }
                val kundenProListe = allCustomers
                    .filter { it.listeId.isNotEmpty() }
                    .groupBy { it.listeId }
                    .mapValues { it.value.size }
                if (listen.isEmpty()) {
                    _state.value = KundenListenState.Empty
                } else {
                    _state.value = KundenListenState.Success(listen, kundenProListe)
                }
            } catch (e: Exception) {
                _state.value = KundenListenState.Error(R.string.error_load_lists, e.message)
            }
        }
    }
}
