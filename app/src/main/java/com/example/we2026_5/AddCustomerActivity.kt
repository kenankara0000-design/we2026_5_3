package com.example.we2026_5

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.lifecycleScope
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.ui.addcustomer.AddCustomerScreen
import com.example.we2026_5.ui.addcustomer.AddCustomerState
import com.example.we2026_5.ui.addcustomer.AddCustomerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.UUID

class AddCustomerActivity : AppCompatActivity() {

    private val viewModel: AddCustomerViewModel by viewModel()
    private val repository: CustomerRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialName = intent.getStringExtra("CUSTOMER_NAME").orEmpty()

        setContent {
            MaterialTheme {
                val state by viewModel.state.observeAsState(initial = AddCustomerState())
                LaunchedEffect(Unit) {
                    if (initialName.isNotEmpty()) viewModel.setInitialName(initialName)
                }
                LaunchedEffect(state.success) {
                    if (state.success) {
                        delay(800)
                        if (!isFinishing) finish()
                    }
                }
                LaunchedEffect(state.errorMessage) {
                    state.errorMessage?.let { msg ->
                        Toast.makeText(this@AddCustomerActivity, msg, Toast.LENGTH_SHORT).show()
                        viewModel.setError(null)
                    }
                }
                AddCustomerScreen(
                    state = state,
                    onBack = { finish() },
                    onNameChange = { viewModel.setName(it) },
                    onAdresseChange = { viewModel.setAdresse(it) },
                    onTelefonChange = { viewModel.setTelefon(it) },
                    onNotizenChange = { viewModel.setNotizen(it) },
                    onKundenArtChange = { viewModel.setKundenArt(it) },
                    onSave = { performSave(state) }
                )
            }
        }
    }

    private fun performSave(state: AddCustomerState) {
        val name = state.name.trim()
        if (name.isEmpty()) {
            viewModel.setError(getString(R.string.validation_name_missing))
            return
        }
        viewModel.setSaving(true)
        viewModel.setError(null)
        val customer = Customer(
            id = UUID.randomUUID().toString(),
            name = name,
            adresse = state.adresse.trim(),
            telefon = state.telefon.trim(),
            notizen = state.notizen.trim(),
            kundenArt = state.kundenArt,
            listeId = "",
            intervalle = emptyList(),
            abholungDatum = 0,
            auslieferungDatum = 0,
            wiederholen = false,
            intervallTage = 0,
            letzterTermin = 0,
            wochentag = 0,
            istImUrlaub = false
        )
        lifecycleScope.launch {
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = { repository.saveCustomer(customer) },
                context = this@AddCustomerActivity,
                errorMessage = getString(R.string.error_save_generic),
                maxRetries = 3
            )
            viewModel.setSaving(false)
            if (success == true) {
                viewModel.setSuccess()
            }
        }
    }
}
