package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.ui.main.SettingsScreen
import com.google.firebase.auth.FirebaseAuth

/**
 * Einstellungen: SevDesk Import (nur Lesen), Abmelden.
 */
class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen(
                onSevDeskImport = {
                    startActivity(Intent(this, SevDeskImportActivity::class.java))
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
}
