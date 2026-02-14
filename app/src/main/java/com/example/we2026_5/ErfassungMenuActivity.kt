package com.example.we2026_5

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.ui.main.ErfassungMenuScreen
import com.example.we2026_5.ui.theme.AppTheme

/**
 * Untermenü für Erfassung: „Erfassung starten“ und „Artikel verwalten“.
 * Wird vom Hauptbildschirm-Button „Erfassung“ geöffnet.
 */
class ErfassungMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
            ErfassungMenuScreen(
                onErfassungStarten = {
                    startActivity(com.example.we2026_5.util.AppNavigation.toWaschenErfassung(this))
                },
                onBelege = {
                    startActivity(com.example.we2026_5.util.AppNavigation.toBelege(this))
                },
                onBack = { finish() }
            )
            }
        }
    }
}
