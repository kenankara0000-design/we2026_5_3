package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.we2026_5.ui.wasch.BelegeScreen
import com.example.we2026_5.ui.wasch.BelegeUiState
import com.example.we2026_5.ui.wasch.BelegeViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class BelegeActivity : AppCompatActivity() {

    private val viewModel: BelegeViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val state by viewModel.uiState.collectAsState()
                val belegMonate by viewModel.belegMonate.collectAsState(initial = emptyList())
                val alleBelegEintraege by viewModel.alleBelegEintraege.collectAsState(initial = emptyList())
                val articles by viewModel.articles.collectAsState(initial = emptyList())
                val belegPreiseGross by viewModel.belegPreiseGross.collectAsState(initial = emptyMap())
                val alleBelegEintraegeErledigt by viewModel.alleBelegEintraegeErledigt.collectAsState(initial = emptyList())
                val belegMonateErledigt by viewModel.belegMonateErledigt.collectAsState(initial = emptyList())
                BelegeScreen(
                    state = state,
                    belegMonate = belegMonate,
                    alleBelegEintraege = alleBelegEintraege,
                    articles = articles,
                    belegPreiseGross = belegPreiseGross,
                    alleBelegEintraegeErledigt = alleBelegEintraegeErledigt,
                    belegMonateErledigt = belegMonateErledigt,
                    onBack = {
                        when (state) {
                            is BelegeUiState.AlleBelege -> finish()
                            is BelegeUiState.KundeSuchen -> viewModel.backToKundeSuchen()
                            is BelegeUiState.BelegListe -> viewModel.backToAlleBelege()
                            is BelegeUiState.BelegDetail -> viewModel.backFromBelegDetail()
                        }
                    },
                    onCustomerSearchQueryChange = { viewModel.setCustomerSearchQuery(it) },
                    onKundeWaehlen = { viewModel.kundeGewaehlt(it) },
                    onBackToAlleBelege = { viewModel.backToAlleBelege() },
                    onBelegClick = { viewModel.openBelegDetail(it) },
                    onBelegEintragClick = { viewModel.openBelegDetailFromAlle(it) },
                    onBackFromBelegDetail = { viewModel.backFromBelegDetail() },
                    onNeueErfassungFromListe = {
                        (state as? BelegeUiState.BelegListe)?.customer?.let { customer ->
                            startActivity(Intent(this@BelegeActivity, WaschenErfassungActivity::class.java).putExtra("CUSTOMER_ID", customer.id))
                        }
                    },
                    onDeleteBeleg = {
                        (state as? BelegeUiState.BelegDetail)?.let { detail ->
                            AlertDialog.Builder(this@BelegeActivity)
                                .setTitle(R.string.dialog_beleg_loeschen_title)
                                .setMessage(getString(R.string.dialog_beleg_loeschen_message, detail.monthLabel, detail.erfassungen.size))
                                .setPositiveButton(R.string.dialog_loeschen) { _, _ ->
                                    viewModel.deleteBeleg(detail.erfassungen) {
                                        Toast.makeText(this@BelegeActivity, getString(R.string.beleg_geloescht), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show()
                        }
                    },
                    onErledigtBeleg = {
                        (state as? BelegeUiState.BelegDetail)?.let { detail ->
                            if (detail.erfassungen.any { it.erledigt }) return@let
                            AlertDialog.Builder(this@BelegeActivity)
                                .setTitle(R.string.dialog_beleg_erledigt_title)
                                .setMessage(R.string.dialog_beleg_erledigt_message)
                                .setPositiveButton(R.string.dialog_ok) { _, _ ->
                                    viewModel.markBelegErledigt(detail.erfassungen) {
                                        Toast.makeText(this@BelegeActivity, R.string.beleg_erledigt_toast, Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .setNegativeButton(R.string.btn_cancel, null)
                                .show()
                        }
                    },
                    onAlleBelegeNameFilterChange = { viewModel.setAlleBelegeNameFilter(it) },
                    onAlleBelegeShowErledigtTabChange = { viewModel.setAlleBelegeShowErledigtTab(it) },
                    onBelegListeShowErledigtTabChange = { viewModel.setBelegListeShowErledigtTab(it) }
                )
            }
        }
    }
}
