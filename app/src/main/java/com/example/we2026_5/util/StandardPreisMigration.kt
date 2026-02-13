package com.example.we2026_5.util

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val PREF_MIGRATION = "standard_preis_migration"
private const val KEY_DONE = "v1_done"

/**
 * Einmalige Migration: Daten von tourPreise/ nach standardPreise/ kopieren.
 * Wenn standardPreise/ bereits Einträge hat, wird nichts gemacht.
 */
suspend fun runStandardPreisMigration(context: Context, database: FirebaseDatabase) {
    val prefs = context.getSharedPreferences(PREF_MIGRATION, Context.MODE_PRIVATE)
    if (prefs.getBoolean(KEY_DONE, false)) return

    withContext(Dispatchers.IO) {
        try {
            val standardRef = database.reference.child("standardPreise")
            val standardSnap = standardRef.get().await()
            if (standardSnap.childrenCount > 0) {
                prefs.edit().putBoolean(KEY_DONE, true).apply()
                return@withContext
            }
            val tourRef = database.reference.child("tourPreise")
            val tourSnap = tourRef.get().await()
            for (child in tourSnap.children) {
                val key = child.key ?: continue
                val priceNet = (child.child("priceNet").getValue(Any::class.java) as? Number)?.toDouble() ?: 0.0
                val priceGross = (child.child("priceGross").getValue(Any::class.java) as? Number)?.toDouble() ?: 0.0
                val data: Map<String, Any> = mapOf(
                    "priceNet" to priceNet,
                    "priceGross" to priceGross
                )
                standardRef.child(key).setValue(data).await()
            }
            prefs.edit().putBoolean(KEY_DONE, true).apply()
        } catch (_: Exception) {
            // Bei Fehler: nächstes Mal erneut versuchen
        }
    }
}
