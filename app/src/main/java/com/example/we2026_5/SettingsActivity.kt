package com.example.we2026_5

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.we2026_5.sevdesk.SevDeskDeletedIds
import com.example.we2026_5.ui.main.SettingsScreen
import com.example.we2026_5.ui.theme.AppTheme
import com.example.we2026_5.util.AppNavigation
import com.example.we2026_5.util.AppPreferences
import com.google.firebase.auth.FirebaseAuth

/**
 * Einstellungen: Preise, Data Import, App-Daten zurücksetzen, Abmelden.
 */
class SettingsActivity : AppCompatActivity() {
    private val appPrefs by lazy { AppPreferences(this) }

    // Phase 4: Reactive state für Toggles
    private var showAddressOnCard by mutableStateOf(false)
    private var showPhoneOnCard by mutableStateOf(false)
    private var showNotesOnCard by mutableStateOf(false)
    private var showSaveAndNext by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showAddressOnCard = appPrefs.showAddressOnCard
        showPhoneOnCard = appPrefs.showPhoneOnCard
        showNotesOnCard = appPrefs.showNotesOnCard
        showSaveAndNext = appPrefs.showSaveAndNext
        setContent {
            AppTheme {
            SettingsScreen(
                onOpenPreise = {
                    startActivity(AppNavigation.toPreise(this))
                },
                onOpenDataImport = {
                    startActivity(AppNavigation.toDataImport(this))
                },
                onResetAppData = {
                    resetAppData(applicationContext)
                    Toast.makeText(this, getString(R.string.settings_reset_app_data_done), Toast.LENGTH_SHORT).show()
                },
                onAbmelden = {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(AppNavigation.toLogin(this, clearTask = true))
                    finish()
                },
                onBack = { finish() },
                showAddressOnCard = showAddressOnCard,
                onShowAddressOnCardChange = { showAddressOnCard = it; appPrefs.showAddressOnCard = it },
                showPhoneOnCard = showPhoneOnCard,
                onShowPhoneOnCardChange = { showPhoneOnCard = it; appPrefs.showPhoneOnCard = it },
                showNotesOnCard = showNotesOnCard,
                onShowNotesOnCardChange = { showNotesOnCard = it; appPrefs.showNotesOnCard = it },
                showSaveAndNext = showSaveAndNext,
                onShowSaveAndNextChange = { showSaveAndNext = it; appPrefs.showSaveAndNext = it }
            )
            }
        }
    }

    private fun resetAppData(context: Context) {
        SevDeskDeletedIds.clear(context)
        context.getSharedPreferences("sevdesk_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("tourplanner_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().clear().apply()
        // UI-State nach Reset zurücksetzen
        showAddressOnCard = true
        showPhoneOnCard = false
        showNotesOnCard = false
        showSaveAndNext = false
    }
}
