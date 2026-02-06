package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.ui.wasch.WaschenErfassungScreen
import com.example.we2026_5.ui.wasch.WaschenErfassungUiState
import com.example.we2026_5.ui.wasch.WaschenErfassungViewModel
import com.example.we2026_5.util.DialogBaseHelper
import com.example.we2026_5.util.TerminBerechnungUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class WaschenErfassungActivity : AppCompatActivity() {

    private val viewModel: WaschenErfassungViewModel by viewModel()
    private val customerRepository: CustomerRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val customerId = intent.getStringExtra("CUSTOMER_ID")
        if (customerId != null) {
            CoroutineScope(Dispatchers.Main).launch {
                val customer = customerRepository.getCustomerById(customerId)
                customer?.let { viewModel.startErfassenFuerKunde(it) }
            }
        }
        setContent {
            MaterialTheme {
                val state by viewModel.uiState.collectAsState()
                val articles by viewModel.articles.collectAsState(initial = emptyList())
                WaschenErfassungScreen(
                    state = state,
                    articles = articles,
                    onBack = { finish() },
                    onNeueErfassung = { viewModel.startNeueErfassung() },
                    onSevDeskImport = { startActivity(Intent(this@WaschenErfassungActivity, SevDeskImportActivity::class.java)) },
                    onKundeWaehlen = { viewModel.kundeGewaehlt(it) },
                    onMengeChange = { id, menge -> viewModel.setMenge(id, menge) },
                    onMengeChangeByIndex = { index, menge -> viewModel.setMengeByIndex(index, menge) },
                    onZeitChange = { viewModel.setZeit(it) },
                    onNotizChange = { viewModel.setNotiz(it) },
                    onSpeichern = {
                        viewModel.speichern {
                            Toast.makeText(this@WaschenErfassungActivity, getString(R.string.wasch_erfassung_gespeichert), Toast.LENGTH_SHORT).show()
                            viewModel.backToAuswahl()
                        }
                    },
                    onBackToKundeWaehlen = { viewModel.backToKundeWaehlen() },
                    onDatumClick = { current ->
                        DialogBaseHelper.showDatePickerDialog(
                            context = this@WaschenErfassungActivity,
                            initialDate = current,
                            title = getString(R.string.wasch_erfassung_datum),
                            onDateSelected = { viewModel.setDatum(TerminBerechnungUtils.getStartOfDay(it)) }
                        )
                    },
                    onArtikelSearchQueryChange = { viewModel.setArtikelSearchQuery(it) },
                    onAddPosition = { viewModel.addPosition(it) },
                    onRemovePosition = { viewModel.removePosition(it) }
                )
            }
        }
    }
}
