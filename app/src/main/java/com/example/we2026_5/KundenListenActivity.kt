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
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.ui.kundenlisten.KundenListenScreen
import com.example.we2026_5.ui.kundenlisten.KundenListenState
import com.example.we2026_5.ui.kundenlisten.KundenListenViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class KundenListenActivity : AppCompatActivity() {

    private val viewModel: KundenListenViewModel by viewModel()
    private val listeRepository: KundenListeRepository by inject()
    private val customerRepository: CustomerRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val state by viewModel.state.collectAsState(initial = KundenListenState.Loading)
                KundenListenScreen(
                    state = state,
                    onBack = { finish() },
                    onNewListe = {
                        startActivity(Intent(this@KundenListenActivity, ListeErstellenActivity::class.java))
                    },
                    onRefresh = { viewModel.loadListen() },
                    onListeClick = { liste ->
                        startActivity(Intent(this@KundenListenActivity, ListeBearbeitenActivity::class.java).apply {
                            putExtra("LISTE_ID", liste.id)
                        })
                    },
                    onListeLoeschen = { liste -> loescheListe(liste) }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadListen()
    }

    private fun loescheListe(liste: KundenListe) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val allCustomers = customerRepository.getAllCustomers()
                val kundenInListe = allCustomers.filter { it.listeId == liste.id }

                if (kundenInListe.isNotEmpty()) {
                    AlertDialog.Builder(this@KundenListenActivity)
                        .setTitle("Liste kann nicht gelöscht werden")
                        .setMessage("Es sind noch ${kundenInListe.size} Kunde(n) in dieser Liste. Bitte entfernen Sie zuerst alle Kunden aus der Liste.")
                        .setPositiveButton("OK", null)
                        .show()
                    return@launch
                }

                AlertDialog.Builder(this@KundenListenActivity)
                    .setTitle("Liste löschen?")
                    .setMessage("Möchten Sie die Liste '${liste.name}' wirklich löschen?")
                    .setPositiveButton("Löschen") { _, _ ->
                        CoroutineScope(Dispatchers.Main).launch {
                            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                                operation = { listeRepository.deleteListe(liste.id) },
                                context = this@KundenListenActivity,
                                errorMessage = getString(R.string.error_delete_generic),
                                maxRetries = 3
                            )
                            if (success != null) {
                                viewModel.loadListen()
                                Toast.makeText(this@KundenListenActivity, getString(R.string.toast_liste_geloescht), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Abbrechen", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@KundenListenActivity, getString(R.string.error_message_generic, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
