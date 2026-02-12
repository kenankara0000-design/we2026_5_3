package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.ui.main.DataImportScreen

class DataImportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DataImportScreen(
                onSevDeskImport = { startActivity(Intent(this, SevDeskImportActivity::class.java)) },
                onBack = { finish() }
            )
        }
    }
}

