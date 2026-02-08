package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.ui.main.ErfassungMenuScreen

/**
 * Untermenü für Erfassung: „Erfassung starten“ und „Artikel verwalten“.
 * Wird vom Hauptbildschirm-Button „Erfassung“ geöffnet.
 */
class ErfassungMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ErfassungMenuScreen(
                onErfassungStarten = {
                    startActivity(Intent(this, WaschenErfassungActivity::class.java))
                    finish()
                },
                onBelege = {
                    startActivity(Intent(this, BelegeActivity::class.java))
                    finish()
                },
                onKundenpreise = {
                    startActivity(Intent(this, KundenpreiseActivity::class.java))
                    finish()
                },
                onArtikelVerwalten = {
                    startActivity(Intent(this, ArtikelVerwaltungActivity::class.java))
                    finish()
                },
                onBack = { finish() }
            )
        }
    }
}
