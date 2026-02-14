package com.example.we2026_5

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.we2026_5.ui.liste.ListeErstellenScreen
import com.example.we2026_5.ui.theme.AppTheme
import com.example.we2026_5.ui.liste.ListeErstellenState
import com.example.we2026_5.ui.liste.ListeErstellenViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ListeErstellenActivity : AppCompatActivity() {

    private val viewModel: ListeErstellenViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("UNUSED_EXPRESSION")
        viewModel
        setContent {
            AppTheme {
                val state by viewModel.state.collectAsState(initial = ListeErstellenState())
                LaunchedEffect(state.errorMessage) {
                    state.errorMessage?.let { msg ->
                        Toast.makeText(this@ListeErstellenActivity, msg, Toast.LENGTH_SHORT).show()
                        viewModel.clearErrorMessage()
                    }
                }
                ListeErstellenScreen(
                    state = state,
                    onListNameChange = { viewModel.setListName(it) },
                    onTypeChange = { viewModel.setSelectedType(it) },
                    onWochentagListeChange = { viewModel.setWochentagListe(it) },
                    onWochentagChange = { viewModel.setWochentag(it) },
                    onSave = { viewModel.save() },
                    onBack = { finish() },
                    onFinish = { finish() }
                )
            }
        }
    }
}
