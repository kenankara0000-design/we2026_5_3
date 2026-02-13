package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.ui.main.PreiseScreen
import com.example.we2026_5.ui.theme.AppTheme

class PreiseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
            PreiseScreen(
                onKundenpreise = { startActivity(Intent(this, KundenpreiseActivity::class.java)) },
                onListenPrivatKundenpreise = { startActivity(Intent(this, ListenPrivatKundenpreiseActivity::class.java)) },
                onArtikelVerwalten = { startActivity(Intent(this, ArtikelVerwaltungActivity::class.java)) },
                onBack = { finish() }
            )
            }
        }
    }
}

