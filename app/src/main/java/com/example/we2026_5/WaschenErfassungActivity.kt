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
                val erfassungen by viewModel.erfassungenList.collectAsState(initial = emptyList())
                WaschenErfassungScreen(
                    state = state,
                    articles = articles,
                    erfassungen = erfassungen,
                    onBack = {
                        when (state) {
                            is WaschenErfassungUiState.KundeSuchen -> finish()
                            is WaschenErfassungUiState.ErfassungenListe -> viewModel.backToKundeSuchen()
                            is WaschenErfassungUiState.ErfassungDetail -> viewModel.backFromDetail()
                            is WaschenErfassungUiState.Erfassen -> viewModel.backFromErfassenToListe()
                        }
                    },
                    onCustomerSearchQueryChange = { viewModel.setCustomerSearchQuery(it) },
                    onKundeWaehlen = { viewModel.kundeGewaehlt(it) },
                    onBackToKundeSuchen = { viewModel.backToKundeSuchen() },
                    onErfassungClick = { viewModel.openErfassungDetail(it) },
                    onNeueErfassungFromListe = {
                        (state as? WaschenErfassungUiState.ErfassungenListe)?.let { viewModel.neueErfassungClick(it.customer) }
                    },
                    onBackFromDetail = { viewModel.backFromDetail() },
                    onMengeChangeByIndex = { index, menge -> viewModel.setMengeByIndex(index, menge) },
                    onNotizChange = { viewModel.setNotiz(it) },
                    onSpeichern = {
                        viewModel.speichern {
                            Toast.makeText(this@WaschenErfassungActivity, getString(R.string.wasch_erfassung_gespeichert), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBackFromErfassen = { viewModel.backFromErfassenToListe() },
                    onArtikelSearchQueryChange = { viewModel.setArtikelSearchQuery(it) },
                    onAddPosition = { viewModel.addPosition(it) },
                    onRemovePosition = { viewModel.removePosition(it) }
                )
            }
        }
    }
}
