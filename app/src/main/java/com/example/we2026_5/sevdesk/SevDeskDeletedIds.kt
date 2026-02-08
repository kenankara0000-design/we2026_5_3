package com.example.we2026_5.sevdesk

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "sevdesk_deleted_ids"
private const val KEY_IDS = "ids"

/**
 * Speichert SevDesk-IDs, die der Nutzer gelöscht hat. Beim Re-Import werden diese nicht wieder angelegt.
 * Kleine Persistenz (SharedPreferences), eine Datei bleibt schlank.
 */
object SevDeskDeletedIds {

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun add(context: Context, sevdeskId: String) {
        val id = sevdeskId.trim().removePrefix("sevdesk_")
        if (id.isEmpty()) return
        val current = prefs(context).getString(KEY_IDS, "") ?: ""
        val set = current.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        set.add(id)
        prefs(context).edit().putString(KEY_IDS, set.joinToString(",")).apply()
    }

    fun addAll(context: Context, sevdeskIds: List<String>) {
        if (sevdeskIds.isEmpty()) return
        val current = prefs(context).getString(KEY_IDS, "") ?: ""
        val set = current.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        sevdeskIds.forEach { id ->
            val x = id.trim().removePrefix("sevdesk_")
            if (x.isNotEmpty()) set.add(x)
        }
        prefs(context).edit().putString(KEY_IDS, set.joinToString(",")).apply()
    }

    fun isDeleted(context: Context, sevdeskId: String): Boolean {
        val id = sevdeskId.trim().removePrefix("sevdesk_")
        if (id.isEmpty()) return false
        val current = prefs(context).getString(KEY_IDS, "") ?: ""
        return id in current.split(",").map { it.trim() }.toSet()
    }

    fun getDeletedIds(context: Context): Set<String> {
        val current = prefs(context).getString(KEY_IDS, "") ?: ""
        return current.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    /** Re-Import-Ignore-Liste leeren – beim nächsten Import werden alle SevDesk-Kontakte wieder berücksichtigt. */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_IDS).apply()
    }
}
