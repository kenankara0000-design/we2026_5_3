package com.example.we2026_5.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Zentrale App-Einstellungen (SharedPreferences "app_settings").
 * Phase 4: Kartenanzeige-Konfiguration + optionale Features.
 */
class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // -- Kartenanzeige: Was auf Tour-/Kundenkarten angezeigt wird --

    var showAddressOnCard: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ADDRESS, true)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_ADDRESS, value).apply()

    var showPhoneOnCard: Boolean
        get() = prefs.getBoolean(KEY_SHOW_PHONE, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_PHONE, value).apply()

    var showNotesOnCard: Boolean
        get() = prefs.getBoolean(KEY_SHOW_NOTES, false)
        set(value) = prefs.edit().putBoolean(KEY_SHOW_NOTES, value).apply()

    // -- Optionale Features --

    var showSaveAndNext: Boolean
        get() = prefs.getBoolean(KEY_SAVE_AND_NEXT, false)
        set(value) = prefs.edit().putBoolean(KEY_SAVE_AND_NEXT, value).apply()

    companion object {
        private const val KEY_SHOW_ADDRESS = "show_address_on_card"
        private const val KEY_SHOW_PHONE = "show_phone_on_card"
        private const val KEY_SHOW_NOTES = "show_notes_on_card"
        private const val KEY_SAVE_AND_NEXT = "show_save_and_next"
    }
}
