package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
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
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.TourSlot
import com.example.we2026_5.Zeitfenster
import com.example.we2026_5.util.TerminAusKundeUtils
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
        window.decorView.setBackgroundColor(ContextCompat.getColor(this, R.color.background_light))
        val initialName = intent.getStringExtra("CUSTOMER_NAME").orEmpty()

        setContent {
            MaterialTheme {
                val state by viewModel.state.observeAsState(initial = AddCustomerState())
                LaunchedEffect(Unit) {
                    if (initialName.isNotEmpty()) viewModel.setInitialName(initialName)
                }
                LaunchedEffect(state.success) {
                    if (state.success) {
                        delay(400)
                        if (!isFinishing) {
                            Toast.makeText(
                                this@AddCustomerActivity,
                                getString(R.string.toast_customer_created_hint),
                                Toast.LENGTH_LONG
                            ).show()
                            startActivity(Intent(this@AddCustomerActivity, CustomerManagerActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            })
                            finish()
                        }
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
                    onUpdate = { viewModel.setStateFromForm(it) },
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
        val errorMsg = validateAddCustomer(state)
        if (errorMsg != null) {
            viewModel.setError(errorMsg)
            return
        }
        lifecycleScope.launch {
            val similar = repository.getAllCustomers().any { c ->
                c.name.trim().lowercase().let { n -> n == name.lowercase() || n.contains(name.lowercase()) || name.lowercase().contains(n) }
            }
            if (similar) {
                androidx.appcompat.app.AlertDialog.Builder(this@AddCustomerActivity)
                    .setTitle(getString(R.string.dialog_duplikat_title))
                    .setMessage(getString(R.string.dialog_duplikat_message))
                    .setPositiveButton(getString(R.string.dialog_yes)) { _, _ -> doSave(state) }
                    .setNegativeButton(getString(R.string.btn_cancel), null)
                    .show()
            } else {
                doSave(state)
            }
        }
    }

    private fun validateAddCustomer(state: AddCustomerState): String? {
        // Nur Name ist Pflicht – A/L-Tag, Intervall etc. können in der Bearbeitung ergänzt werden
        return null
    }

    private fun doSave(state: AddCustomerState) {
        viewModel.setSaving(true)
        viewModel.setError(null)
        val name = state.name.trim()
        val customerId = UUID.randomUUID().toString()
        val hasTour = state.abholungWochentage.isNotEmpty() || state.tourStadt.isNotBlank() || state.tourZeitStart.isNotBlank() || state.tourZeitEnde.isNotBlank()
        val tourSlot = if (hasTour) {
            TourSlot(
                id = "customer-$customerId",
                wochentag = state.abholungWochentage.firstOrNull() ?: -1,
                stadt = state.tourStadt.trim(),
                zeitfenster = if (state.tourZeitStart.isNotBlank() || state.tourZeitEnde.isNotBlank())
                    Zeitfenster(state.tourZeitStart.trim(), state.tourZeitEnde.trim()) else null
            )
        } else null
        val tagsList = state.tagsInput.split(",").mapNotNull { it.trim().ifEmpty { null } }
        val baseCustomer = Customer(
            id = customerId,
            name = name,
            alias = state.alias.trim(),
            adresse = state.adresse.trim(),
            latitude = state.latitude,
            longitude = state.longitude,
            stadt = state.stadt.trim(),
            plz = state.plz.trim(),
            telefon = state.telefon.trim(),
            notizen = state.notizen.trim(),
            kundenArt = state.kundenArt,
            listeId = "",
            intervalle = emptyList(),
            abholungDatum = 0,
            auslieferungDatum = 0,
            wiederholen = false,
            intervallTage = if (state.kundenTyp == KundenTyp.REGELMAESSIG) (state.intervallTage ?: 7) else 0,
            letzterTermin = 0,
            wochentag = "",
            kundenTyp = state.kundenTyp,
            listenWochentag = -1,
            kundennummer = state.kundennummer.trim(),
            erstelltAm = state.erstelltAm.takeIf { it > 0 }?.let { TerminBerechnungUtils.getStartOfDay(it) } ?: TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis()),
            defaultAbholungWochentag = state.abholungWochentage.firstOrNull() ?: -1,
            defaultAuslieferungWochentag = state.auslieferungWochentage.firstOrNull() ?: -1,
            defaultAbholungWochentage = state.abholungWochentage,
            defaultAuslieferungWochentage = state.auslieferungWochentage,
            tageAzuL = state.tageAzuL?.coerceIn(0, 365) ?: 7,
            defaultUhrzeit = state.defaultUhrzeit.trim(),
            defaultZeitfenster = null,
            tags = tagsList,
            tourSlotId = tourSlot?.id ?: "",
            tourSlot = tourSlot,
            ohneTour = state.ohneTour,
            istImUrlaub = false
        )
        val startDatumA = state.erstelltAm.takeIf { it > 0 }?.let { TerminBerechnungUtils.getStartOfDay(it) } ?: TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
        val intervalle = if (state.kundenTyp == KundenTyp.REGELMAESSIG) {
            TerminAusKundeUtils.erstelleIntervalleAusKunde(baseCustomer, startDatumA, state.tageAzuL ?: 7, state.intervallTage)
        } else {
            emptyList()
        }
        val customer = baseCustomer.copy(intervalle = intervalle)
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
