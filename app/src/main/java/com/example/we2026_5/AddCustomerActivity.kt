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
                        delay(800)
                        if (!isFinishing) {
                            startActivity(Intent(this@AddCustomerActivity, MainActivity::class.java).apply {
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
                    onNameChange = { viewModel.setName(it) },
                    onAdresseChange = { viewModel.setAdresse(it) },
                    onStadtChange = { viewModel.setStadt(it) },
                    onPlzChange = { viewModel.setPlz(it) },
                    onTelefonChange = { viewModel.setTelefon(it) },
                    onNotizenChange = { viewModel.setNotizen(it) },
                    onKundenArtChange = { viewModel.setKundenArt(it) },
                    onKundenTypChange = { viewModel.setKundenTyp(it) },
                    onListenWochentagChange = { viewModel.setListenWochentag(it) },
                    onIntervallTageChange = { viewModel.setIntervallTage(it) },
                    onKundennummerChange = { viewModel.setKundennummer(it) },
                    onAbholungTagChange = { viewModel.setAbholungWochentag(it) },
                    onAuslieferungTagChange = { viewModel.setAuslieferungWochentag(it) },
                    onDefaultUhrzeitChange = { viewModel.setDefaultUhrzeit(it) },
                    onTagsChange = { viewModel.setTagsInput(it) },
                    onTourWochentagChange = { viewModel.setTourWochentag(it) },
                    onTourStadtChange = { viewModel.setTourStadt(it) },
                    onTourZeitStartChange = { viewModel.setTourZeitStart(it) },
                    onTourZeitEndeChange = { viewModel.setTourZeitEnde(it) },
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
        return when {
            state.kundenTyp == KundenTyp.REGELMAESSIG && (state.abholungWochentag < 0 || state.auslieferungWochentag < 0) ->
                getString(R.string.validation_regelmaessig_al_required)
            state.kundenTyp == KundenTyp.UNREGELMAESSIG && state.abholungWochentag < 0 && state.auslieferungWochentag < 0 ->
                getString(R.string.validation_unregelmaessig_al_required)
            state.kundenTyp == KundenTyp.REGELMAESSIG && (state.intervallTage !in 1..365) ->
                getString(R.string.validation_intervall_required)
            state.kundenTyp == KundenTyp.REGELMAESSIG && state.listenWochentag < 0 ->
                getString(R.string.validation_listen_wochentag)
            else -> null
        }
    }

    private fun doSave(state: AddCustomerState) {
        viewModel.setSaving(true)
        viewModel.setError(null)
        val name = state.name.trim()
        val customerId = UUID.randomUUID().toString()
        val tags = state.tagsInput.split(",").mapNotNull { it.trim().ifEmpty { null } }
        val tourSlot = if (state.tourWochentag >= 0 || state.tourStadt.isNotBlank()) {
            TourSlot(
                id = "customer-$customerId",
                wochentag = state.tourWochentag,
                stadt = state.tourStadt.trim(),
                zeitfenster = Zeitfenster(state.tourZeitStart.trim(), state.tourZeitEnde.trim())
            )
        } else null
        val defaultZeitfenster = if (state.defaultUhrzeit.isNotBlank()) {
            Zeitfenster(state.defaultUhrzeit.trim(), state.defaultUhrzeit.trim())
        } else null
        val baseCustomer = Customer(
            id = customerId,
            name = name,
            adresse = state.adresse.trim(),
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
            intervallTage = if (state.kundenTyp == KundenTyp.REGELMAESSIG) state.intervallTage else 0,
            letzterTermin = 0,
            wochentag = "",
            kundenTyp = state.kundenTyp,
            listenWochentag = if (state.kundenTyp == KundenTyp.REGELMAESSIG) state.listenWochentag else -1,
            kundennummer = state.kundennummer.trim(),
            defaultAbholungWochentag = state.abholungWochentag,
            defaultAuslieferungWochentag = state.auslieferungWochentag,
            defaultUhrzeit = state.defaultUhrzeit.trim(),
            defaultZeitfenster = defaultZeitfenster,
            tags = tags,
            tourSlotId = tourSlot?.id ?: "",
            tourSlot = tourSlot,
            istImUrlaub = false
        )
        val intervalle = if (state.kundenTyp == KundenTyp.REGELMAESSIG) {
            TerminAusKundeUtils.erstelleIntervallAusKunde(baseCustomer)?.let { listOf(it) } ?: emptyList()
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
