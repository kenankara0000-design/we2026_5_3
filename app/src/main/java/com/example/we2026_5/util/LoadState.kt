package com.example.we2026_5.util

/**
 * Einheitlicher Lade-/Fehler-State für UI.
 * ViewModel setzt LoadState; Activity/Fragment zeigt nur an (Loading-Indikator, Inhalt, Fehler).
 */
sealed class LoadState<out T> {
    data object Loading : LoadState<Nothing>()
    data class Success<T>(val data: T) : LoadState<T>()
    data class Error(val message: String) : LoadState<Nothing>()
}
