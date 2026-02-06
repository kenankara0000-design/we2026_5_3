package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.we2026_5.ui.wasch.WaschenErfassungScreen
import com.example.we2026_5.ui.wasch.WaschenErfassungUiState
import com.example.we2026_5.ui.wasch.WaschenErfassungViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class WaschenErfassungActivity : AppCompatActivity() {

    private val viewModel: WaschenErfassungViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val state by viewModel.uiState.collectAsState()
                WaschenErfassungScreen(
                    state = state,
                    onBack = { finish() },
                    onNeueErfassung = { viewModel.startNeueErfassung() },
                    onSevDeskImport = { startActivity(Intent(this@WaschenErfassungActivity, SevDeskImportActivity::class.java)) },
                    onKundeWaehlen = { viewModel.kundeGewaehlt(it) },
                    onMengeChange = { id, menge -> viewModel.setMenge(id, menge) },
                    onNotizChange = { viewModel.setNotiz(it) },
                    onSpeichern = {
                        viewModel.speichern {
                            Toast.makeText(this@WaschenErfassungActivity, getString(R.string.wasch_erfassung_gespeichert), Toast.LENGTH_SHORT).show()
                            viewModel.backToAuswahl()
                        }
                    },
                    onBackToKundeWaehlen = { viewModel.backToKundeWaehlen() }
                )
            }
        }
    }
}
