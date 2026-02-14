package com.example.we2026_5

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.ui.main.DataImportScreen
import com.example.we2026_5.ui.theme.AppTheme

class DataImportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
            DataImportScreen(
                onSevDeskImport = { startActivity(com.example.we2026_5.util.AppNavigation.toSevDeskImport(this)) },
                onBack = { finish() }
            )
            }
        }
    }
}

