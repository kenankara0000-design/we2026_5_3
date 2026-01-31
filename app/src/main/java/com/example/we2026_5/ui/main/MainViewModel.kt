package com.example.we2026_5.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.tourplanner.TourDataProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel für MainActivity.
 * Tour-Count (fällig) über Flow; Repository nur über ViewModel.
 */
class MainViewModel(
    private val repository: CustomerRepository,
    private val listeRepository: KundenListeRepository
) : ViewModel() {

    private val dataProcessor = TourDataProcessor()

    private val _tourFälligCount = MutableLiveData<Int>(0)
    /** Anzahl fälliger/überfälliger Termine für heute (für Tour-Button-Text). */
    val tourFälligCount: LiveData<Int> = _tourFälligCount

    init {
        viewModelScope.launch {
            combine(
                repository.getAllCustomersFlow(),
                listeRepository.getAllListenFlow()
            ) { customers, listen ->
                Pair(customers, listen)
            }.collect { (customers, listen) ->
                val count = withContext(Dispatchers.IO) {
                    dataProcessor.getFälligCount(customers, listen, System.currentTimeMillis())
                }
                _tourFälligCount.value = count
            }
        }
    }
}
