package com.example.we2026_5

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.ui.theme.AppTheme
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
            AppTheme {
                val state by viewModel.state.collectAsState(initial = KundenListenState.Loading)
                KundenListenScreen(
                    state = state,
                    onBack = { finish() },
                    onNewListe = {
                        startActivity(com.example.we2026_5.util.AppNavigation.toListeErstellen(this@KundenListenActivity))
                    },
                    onRefresh = { viewModel.loadListen() },
                    onListeClick = { liste ->
                        startActivity(com.example.we2026_5.util.AppNavigation.toListeBearbeiten(this@KundenListenActivity, liste.id))
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
                        .setTitle(getString(R.string.dialog_list_cannot_delete_title))
                        .setMessage(getString(R.string.dialog_list_has_customers_message, kundenInListe.size))
                        .setPositiveButton(getString(R.string.dialog_ok), null)
                        .show()
                    return@launch
                }

                AlertDialog.Builder(this@KundenListenActivity)
                    .setTitle(getString(R.string.dialog_delete_list_title))
                    .setMessage(getString(R.string.dialog_delete_list_message, liste.name))
                    .setPositiveButton(getString(R.string.dialog_loeschen)) { _, _ ->
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
.setNegativeButton(getString(R.string.btn_cancel), null)
                        .show()
            } catch (e: Exception) {
                Toast.makeText(this@KundenListenActivity, getString(R.string.error_message_generic, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
