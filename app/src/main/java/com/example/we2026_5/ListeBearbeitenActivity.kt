package com.example.we2026_5

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.ui.theme.AppTheme
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.liste.ListeBearbeitenCallbacks
import com.example.we2026_5.ui.listebearbeiten.ListeBearbeitenScreen
import com.example.we2026_5.ui.listebearbeiten.ListeBearbeitenViewModel
import com.example.we2026_5.util.IntervallManager
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ListeBearbeitenActivity : AppCompatActivity() {

    private val listeRepository: KundenListeRepository by inject()
    private val customerRepository: CustomerRepository by inject()
    private val viewModel: ListeBearbeitenViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val listeId = intent.getStringExtra("LISTE_ID")
        if (listeId == null) {
            Toast.makeText(this, getString(R.string.error_list_id_missing), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            AppTheme {
            val state by viewModel.state.collectAsState()
            val callbacks = remember(viewModel) {
                ListeBearbeitenCallbacks(
                    activity = this@ListeBearbeitenActivity,
                    customerRepository = customerRepository,
                    listeRepository = listeRepository,
                    onDataReload = { viewModel.loadDaten(null) }
                )
            }

            LaunchedEffect(listeId) {
                viewModel.loadDaten(listeId)
            }

            LaunchedEffect(state.errorMessageResId) {
                state.errorMessageResId?.let { resId ->
                    val msg = state.errorMessageArg?.let { getString(resId, it) } ?: getString(resId)
                    Toast.makeText(this@ListeBearbeitenActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearErrorMessage()
                    if (resId == R.string.error_list_not_found) finish()
                }
            }

            ListeBearbeitenScreen(
                state = state,
                onBack = { finish() },
                onEdit = { viewModel.setEditMode(true) },
                onSave = { name, listeArt ->
                    val liste = state.liste ?: return@ListeBearbeitenScreen
                    callbacks.handleSave(liste, name, listeArt, state.intervalle) { updatedListe ->
                        viewModel.updateListe(updatedListe)
                        viewModel.setEditMode(false)
                    }
                },
                onDelete = {
                    val liste = state.liste ?: return@ListeBearbeitenScreen
                    callbacks.showDeleteConfirmation {
                        callbacks.deleteListe(liste.id) { finish() }
                    }
                },
                onTerminAnlegen = {
                    val liste = state.liste ?: return@ListeBearbeitenScreen
                    val options = arrayOf(
                        getString(R.string.listen_termin_art_a_plus_l),
                        getString(R.string.listen_termin_art_nur_a_oder_l)
                    )
                    androidx.appcompat.app.AlertDialog.Builder(this@ListeBearbeitenActivity)
                        .setTitle(getString(R.string.dialog_listen_termin_art_title))
                        .setItems(options) { _, which ->
                            when (which) {
                                0 -> {
                                    val tageAzuL = liste.tageAzuL.coerceIn(1, 365)
                                    IntervallManager.showDatePickerForListenTermin(
                                        context = this@ListeBearbeitenActivity,
                                        onDateSelected = { dateMillis ->
                                            callbacks.addListenTermin(liste, dateMillis, tageAzuL) { updated ->
                                                viewModel.updateListe(updated)
                                            }
                                        }
                                    )
                                }
                                1 -> {
                                    IntervallManager.showDatePickerForListenTermin(
                                        context = this@ListeBearbeitenActivity,
                                        onDateSelected = { dateMillis ->
                                            val aOrL = arrayOf(
                                                getString(R.string.termin_anlegen_ausnahme_abholung),
                                                getString(R.string.termin_anlegen_ausnahme_auslieferung)
                                            )
                                            androidx.appcompat.app.AlertDialog.Builder(this@ListeBearbeitenActivity)
                                                .setTitle(getString(R.string.listen_termin_nur_a_oder_l_waehlen))
                                                .setItems(aOrL) { _, typWhich ->
                                                    val typ = if (typWhich == 0) "A" else "L"
                                                    callbacks.addSingleListenTermin(liste, dateMillis, typ) { updated ->
                                                        viewModel.updateListe(updated)
                                                    }
                                                }
                                                .show()
                                        }
                                    )
                                }
                            }
                        }
                        .show()
                },
                onDeleteListenTermine = { toRemove ->
                    val liste = state.liste ?: return@ListeBearbeitenScreen
                    callbacks.removeListenTermine(liste, toRemove) { updated ->
                        viewModel.updateListe(updated)
                    }
                },
                onRemoveKunde = { customer ->
                    val liste = state.liste ?: return@ListeBearbeitenScreen
                    callbacks.entferneKundeAusListe(customer, liste)
                },
                onAddKunde = { customer ->
                    val liste = state.liste ?: return@ListeBearbeitenScreen
                    callbacks.fuegeKundeZurListeHinzu(customer, liste)
                },
                onRefresh = { viewModel.loadDaten(null) },
                onWochentagAChange = { wochentagA ->
                    val liste = state.liste ?: return@ListeBearbeitenScreen
                    callbacks.updateListenTourEinstellungen(liste, wochentagA, liste.tageAzuL) { updated ->
                        viewModel.updateListe(updated)
                    }
                },
                onTageAzuLChange = { tageAzuL ->
                    val liste = state.liste ?: return@ListeBearbeitenScreen
                    callbacks.updateListenTourEinstellungen(liste, liste.wochentagA, tageAzuL) { updated ->
                        viewModel.updateListe(updated)
                    }
                }
            )
            }
        }
    }
}
