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
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.liste.ListeBearbeitenCallbacks
import com.example.we2026_5.ui.listebearbeiten.ListeBearbeitenScreen
import com.example.we2026_5.ui.listebearbeiten.ListeBearbeitenViewModel
import com.example.we2026_5.util.IntervallManager
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class ListeBearbeitenActivity : AppCompatActivity() {

    private val listeRepository: KundenListeRepository by inject()
    private val customerRepository: CustomerRepository by inject()
    private val regelRepository: TerminRegelRepository by inject()
    private val viewModel: ListeBearbeitenViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val listeId = intent.getStringExtra("LISTE_ID")
        if (listeId == null) {
            Toast.makeText(this, "Fehler: Keine Liste-ID übergeben", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            val state by viewModel.state.collectAsState()
            val callbacks = remember(viewModel) {
                ListeBearbeitenCallbacks(
                    activity = this@ListeBearbeitenActivity,
                    customerRepository = customerRepository,
                    listeRepository = listeRepository,
                    regelRepository = regelRepository,
                    onDataReload = { viewModel.loadDaten(null) }
                )
            }

            LaunchedEffect(listeId) {
                viewModel.loadDaten(listeId)
            }

            LaunchedEffect(state.errorMessage) {
                state.errorMessage?.let { msg ->
                    Toast.makeText(this@ListeBearbeitenActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearErrorMessage()
                    if (msg == "Liste nicht gefunden") finish()
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
                    callbacks.showRegelAuswahlDialog { regel ->
                        callbacks.wendeRegelAn(regel, liste) { updatedListe ->
                            viewModel.updateListe(updatedListe)
                        }
                    }
                },
                onRemoveKunde = { customer -> callbacks.entferneKundeAusListe(customer) },
                onAddKunde = { customer ->
                    val liste = state.liste ?: return@ListeBearbeitenScreen
                    callbacks.fuegeKundeZurListeHinzu(customer, liste.id)
                },
                onRefresh = { viewModel.loadDaten(null) },
                onDatumSelected = { position, isAbholung ->
                    val intervalle = state.intervalle.toMutableList()
                    IntervallManager.showDatumPickerForListe(
                        context = this@ListeBearbeitenActivity,
                        intervalle = intervalle,
                        position = position,
                        isAbholung = isAbholung,
                        onDatumSelected = { viewModel.updateIntervalle(intervalle) }
                    )
                }
            )
        }
    }
}
