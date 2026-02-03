package com.example.we2026_5

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.we2026_5.ui.liste.ListeErstellenScreen
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
            MaterialTheme {
                val state by viewModel.state.observeAsState(initial = ListeErstellenState())
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
