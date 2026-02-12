package com.example.we2026_5

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.sevdesk.SevDeskDeletedIds
import com.example.we2026_5.ui.main.SettingsScreen
import com.google.firebase.auth.FirebaseAuth

/**
 * Einstellungen: Preise, Data Import, App-Daten zurücksetzen, Abmelden.
 */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen(
                onOpenPreise = {
                    startActivity(Intent(this, PreiseActivity::class.java))
                },
                onOpenDataImport = {
                    startActivity(Intent(this, DataImportActivity::class.java))
                },
                onResetAppData = {
                    resetAppData(applicationContext)
                    Toast.makeText(this, getString(R.string.settings_reset_app_data_done), Toast.LENGTH_SHORT).show()
                },
                onAbmelden = {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK })
                    finish()
                },
                onBack = { finish() }
            )
        }
    }

    private fun resetAppData(context: Context) {
        SevDeskDeletedIds.clear(context)
        context.getSharedPreferences("sevdesk_prefs", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("tourplanner_prefs", Context.MODE_PRIVATE).edit().clear().apply()
    }
}
