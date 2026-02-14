package com.example.we2026_5.util

import android.content.Context
import com.example.we2026_5.data.repository.KundenListeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREF_MIGRATION = "liste_art_tour_to_listenkunden_migration"
private const val KEY_DONE = "v1_done"

/**
 * Einmalige Migration (Option B): listeArt "Tour" → "Listenkunden" in Firebase (kundenListen).
 */
suspend fun runListeArtTourToListenkundenMigration(context: Context, listeRepository: KundenListeRepository) {
    val prefs = context.getSharedPreferences(PREF_MIGRATION, Context.MODE_PRIVATE)
    if (prefs.getBoolean(KEY_DONE, false)) return

    withContext(Dispatchers.IO) {
        try {
            val ids = listeRepository.getListenIdsWithListeArtTour()
            ids.forEach { id ->
                when (val r = listeRepository.updateListe(id, mapOf("listeArt" to "Listenkunden"))) {
                    is Result.Success -> { }
                    is Result.Error -> throw r.throwable ?: Exception(r.message)
                    is Result.Loading -> { }
                }
            }
            prefs.edit().putBoolean(KEY_DONE, true).apply()
        } catch (_: Exception) {
            // Bei Fehler: nächstes Mal erneut versuchen
        }
    }
}
