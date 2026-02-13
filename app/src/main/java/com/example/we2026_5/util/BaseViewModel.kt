package com.example.we2026_5.util

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Basis-ViewModel mit gemeinsamen Funktionen für Fehlerbehandlung und Loading-States.
 * 
 * Vorteile:
 * - Reduziert Code-Duplikate (jedes ViewModel hatte eigene _isLoading, _errorMessage)
 * - Einheitliche Error-Handling-Logik
 * - Zentrale Stelle für Loading/Error-State-Management
 * - Einfacher zu testen
 * 
 * Verwendung:
 * ```
 * class MyViewModel : BaseViewModel() {
 *     fun loadData() {
 *         viewModelScope.launch {
 *             executeWithLoading {
 *                 val result = repository.getData()
 *                 result.onError { showError(it) }
 *             }
 *         }
 *     }
 * }
 * ```
 */
abstract class BaseViewModel : ViewModel() {
    
    // =============================
    // LOADING STATE
    // =============================
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Setzt den Loading-Status.
     */
    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    /**
     * Führt einen Code-Block mit automatischem Loading-State aus.
     * 
     * Beispiel:
     * ```
     * executeWithLoading {
     *     repository.saveData()
     * }
     * ```
     */
    protected suspend fun <T> executeWithLoading(block: suspend () -> T): T {
        setLoading(true)
        return try {
            block()
        } finally {
            setLoading(false)
        }
    }
    
    // =============================
    // ERROR HANDLING
    // =============================
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Zeigt eine Fehlermeldung an (wird im UI als Toast/Snackbar angezeigt).
     */
    protected fun showError(message: String) {
        _errorMessage.value = message
    }
    
    /**
     * Löscht die aktuelle Fehlermeldung.
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Führt einen Code-Block mit automatischem Error-Handling aus.
     * 
     * Beispiel:
     * ```
     * executeWithErrorHandling("Fehler beim Laden") {
     *     repository.loadData()
     * }
     * ```
     */
    protected suspend fun <T> executeWithErrorHandling(
        errorPrefix: String = "Fehler",
        block: suspend () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            showError("$errorPrefix: ${e.message ?: "Unbekannter Fehler"}")
            AppLogger.e(this::class.simpleName ?: "BaseViewModel", errorPrefix, e)
            null
        }
    }
    
    /**
     * Kombiniert Loading + Error-Handling.
     * 
     * Beispiel:
     * ```
     * executeWithLoadingAndErrorHandling("Fehler beim Speichern") {
     *     repository.saveData()
     * }
     * ```
     */
    protected suspend fun <T> executeWithLoadingAndErrorHandling(
        errorPrefix: String = "Fehler",
        block: suspend () -> T
    ): T? {
        setLoading(true)
        return try {
            block()
        } catch (e: Exception) {
            showError("$errorPrefix: ${e.message ?: "Unbekannter Fehler"}")
            AppLogger.e(this::class.simpleName ?: "BaseViewModel", errorPrefix, e)
            null
        } finally {
            setLoading(false)
        }
    }
}
