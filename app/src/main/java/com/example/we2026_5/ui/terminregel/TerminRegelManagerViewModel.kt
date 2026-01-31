package com.example.we2026_5.ui.terminregel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.TerminRegel
import com.example.we2026_5.data.repository.TerminRegelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class TerminRegelManagerViewModel(
    private val regelRepository: TerminRegelRepository
) : ViewModel() {

    private val _regeln = MutableStateFlow<List<TerminRegel>>(emptyList())
    val regeln: StateFlow<List<TerminRegel>> = _regeln.asStateFlow()

    init {
        regelRepository.getAllRegelnFlow()
            .onEach { list ->
                _regeln.value = list
            }
            .launchIn(viewModelScope)
    }
}
