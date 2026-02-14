package com.example.we2026_5

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
                onKundenpreise = { startActivity(com.example.we2026_5.util.AppNavigation.toKundenpreise(this)) },
                onListenPrivatKundenpreise = { startActivity(com.example.we2026_5.util.AppNavigation.toListenPrivatKundenpreise(this)) },
                onArtikelVerwalten = { startActivity(com.example.we2026_5.util.AppNavigation.toArtikelVerwaltung(this)) },
                onBack = { finish() }
            )
            }
        }
    }
}

