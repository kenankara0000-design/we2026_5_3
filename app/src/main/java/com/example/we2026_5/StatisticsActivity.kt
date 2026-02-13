package com.example.we2026_5

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.we2026_5.ui.statistics.StatisticsScreen
import com.example.we2026_5.ui.theme.AppTheme
import com.example.we2026_5.ui.statistics.StatisticsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class StatisticsActivity : AppCompatActivity() {

    private val viewModel: StatisticsViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fehler sichtbar machen: Bei Absturz Log + Toast, dann Standard-Handler
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("StatisticsActivity", "Uncaught exception", throwable)
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Fehler: ${throwable.javaClass.simpleName}\n${throwable.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }

        // ViewModel vor setContent anfordern (Koin Activity-Kontext)
        @Suppress("UNUSED_EXPRESSION")
        viewModel

        setContent {
            AppTheme {
                val state by viewModel.state.observeAsState(initial = null)
                StatisticsScreen(
                    state = state,
                    onBack = { finish() }
                )
            }
        }
    }
}
