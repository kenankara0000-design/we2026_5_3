package com.example.we2026_5.util

import android.content.Context
import com.example.we2026_5.data.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREF_MIGRATION = "remove_deprecated_fields_migration"
private const val KEY_DONE = "v1_done"
private const val KEY_VERIFIED = "v1_verified"

/**
 * Einmalige Migration: Entfernt die alten Kunden-Felder (abholungDatum, auslieferungDatum,
 * wiederholen, intervallTage, letzterTermin) aus der Firebase-Datenbank.
 * Sollte nach runListeIntervalleMigration laufen, damit alle Kunden intervalle haben.
 * Nach der Migration bzw. einmalig danach: Prüfung, ob die Felder in Firebase weg sind (Log-Ausgabe).
 */
suspend fun runRemoveDeprecatedFieldsMigration(
    context: Context,
    customerRepository: CustomerRepository
) {
    val prefs = context.getSharedPreferences(PREF_MIGRATION, Context.MODE_PRIVATE)
    val alreadyDone = prefs.getBoolean(KEY_DONE, false)

    withContext(Dispatchers.IO) {
        if (!alreadyDone) {
            try {
                val count = customerRepository.removeDeprecatedFieldsFromFirebase()
                if (count > 0) {
                    android.util.Log.d("RemoveDeprecatedFieldsMigration", "Removed deprecated fields from $count customers")
                }
                prefs.edit().putBoolean(KEY_DONE, true).apply()
            } catch (e: Exception) {
                android.util.Log.e("RemoveDeprecatedFieldsMigration", "Migration failed", e)
                return@withContext
            }
        }

        // Einmalige Prüfung: Sind die Felder in Firebase wirklich weg? (nach Migration oder beim ersten Start nach Update)
        if (!prefs.getBoolean(KEY_VERIFIED, false)) {
            try {
                val stillPresent = customerRepository.getCustomerIdsWithDeprecatedFields()
                if (stillPresent.isEmpty()) {
                    android.util.Log.d("RemoveDeprecatedFieldsMigration", "Verification: all deprecated fields removed from Firebase")
                } else {
                    android.util.Log.w("RemoveDeprecatedFieldsMigration", "Verification: ${stillPresent.size} customers still have deprecated fields: ${stillPresent.take(20).joinToString()}" + if (stillPresent.size > 20) " ..." else "")
                }
                prefs.edit().putBoolean(KEY_VERIFIED, true).apply()
            } catch (_: Exception) { }
        }
    }
}
