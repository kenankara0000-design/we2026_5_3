package com.example.we2026_5

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.Lifecycle
import com.example.we2026_5.CustomerStatus
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.util.AppNavigation
import com.example.we2026_5.util.AppPreferences
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.DialogBaseHelper
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.we2026_5.auth.AdminChecker
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.detail.CustomerPhotoManager
import com.example.we2026_5.ui.theme.AppTheme
import com.example.we2026_5.ui.detail.CustomerDetailActions
import com.example.we2026_5.ui.detail.CustomerDetailScreen
import com.example.we2026_5.ui.detail.CustomerDetailUiState
import com.example.we2026_5.ui.detail.CustomerDetailViewModel
import com.example.we2026_5.ui.urlaub.UrlaubActivity
import com.example.we2026_5.sevdesk.SevDeskDeletedIds
import com.example.we2026_5.util.IntervallManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class CustomerDetailActivity : AppCompatActivity() {

    private val viewModel: CustomerDetailViewModel by viewModel()
    private val repository: CustomerRepository by inject()
    private val adminChecker: AdminChecker by inject()

    private lateinit var networkMonitor: NetworkMonitor
    private var photoManager: CustomerPhotoManager? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        photoManager?.handleCameraResult(success)
    }
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        photoManager?.handleGalleryResult(uri)
    }
    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        photoManager?.handleCameraPermissionResult(granted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val id = intent.getStringExtra("CUSTOMER_ID")
        if (id == null) {
            Toast.makeText(this, getString(R.string.error_customer_id_missing), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        viewModel.setCustomerId(id)

        networkMonitor = NetworkMonitor(this, lifecycleScope)
        networkMonitor.startMonitoring()

        // Phase 4: „Nächster Kunde" aus Einstellungen lesen (Standard: aus)
        val showSaveAndNext = AppPreferences(this).showSaveAndNext
        val nextCustomerIndex = intent.getIntExtra(NextCustomerHelper.EXTRA_CURRENT_INDEX, -1)

        photoManager = CustomerPhotoManager(
            activity = this,
            repository = repository,
            customerId = id,
            takePictureLauncher = takePictureLauncher,
            pickImageLauncher = pickImageLauncher,
            cameraPermissionLauncher = cameraPermissionLauncher,
            onProgressVisibilityChanged = { viewModel.setUploading(it) }
        )

        setContent {
            AppTheme {
            val customer by viewModel.currentCustomer.collectAsState(initial = null)
            val isInEditMode by viewModel.isInEditMode.collectAsState(initial = false)
            val editIntervalle by viewModel.editIntervalle.collectAsState(initial = emptyList())
            val editFormState by viewModel.editFormState.collectAsState(initial = null)
            val isLoading by viewModel.isLoading.collectAsState(initial = false)
            val isUploading by viewModel.isUploading.collectAsState(initial = false)
            val tourListenName by viewModel.tourListenName.collectAsState(initial = null)
            val terminePairs365 by viewModel.terminePairs365.collectAsState(initial = emptyList())
            val belegMonateForCustomer by viewModel.belegMonateForCustomer.collectAsState(initial = emptyList())
            val belegMonateErledigtForCustomer by viewModel.belegMonateErledigtForCustomer.collectAsState(initial = emptyList())
            val isOnline by networkMonitor.isOnline.observeAsState(initial = true)

            val detailState = CustomerDetailUiState(
                isAdmin = adminChecker.isAdmin(),
                customer = customer,
                isInEditMode = isInEditMode,
                editIntervalle = editIntervalle,
                editFormState = editFormState,
                isLoading = isLoading,
                isUploading = isUploading,
                isOffline = !isOnline,
                showSaveAndNext = showSaveAndNext,
                terminePairs365 = terminePairs365,
                tourListenName = tourListenName,
                belegMonateForCustomer = belegMonateForCustomer,
                belegMonateErledigtForCustomer = belegMonateErledigtForCustomer,
                regelNameByRegelId = emptyMap()
            )
            val detailActions = CustomerDetailActions(
                onUpdateEditFormState = { viewModel.updateEditFormState(it) },
                onBack = { finish() },
                onEdit = { viewModel.setEditMode(true, customer) },
                onPerformSave = { andNext ->
                    viewModel.performSave(getString(R.string.validation_name_missing)) { success ->
                        if (success) {
                            viewModel.setEditMode(false, null)
                            Toast.makeText(this@CustomerDetailActivity, getString(R.string.toast_gespeichert), Toast.LENGTH_SHORT).show()
                            if (andNext) {
                                setResult(NextCustomerHelper.RESULT_OPEN_NEXT, Intent().putExtra(NextCustomerHelper.RESULT_EXTRA_INDEX, nextCustomerIndex))
                                finish()
                            }
                        }
                    }
                },
                onDelete = {
                    AlertDialog.Builder(this@CustomerDetailActivity)
                        .setTitle(getString(R.string.dialog_delete_customer_title))
                        .setMessage(getString(R.string.dialog_delete_customer_message))
                        .setPositiveButton(getString(R.string.dialog_loeschen)) { _, _ ->
                            customer?.let { c ->
                                if (c.kundennummer.startsWith("sevdesk_")) {
                                    SevDeskDeletedIds.add(this@CustomerDetailActivity, c.kundennummer)
                                }
                            }
                            val resultIntent = Intent().apply { putExtra("DELETED_CUSTOMER_ID", id) }
                            setResult(com.example.we2026_5.CustomerManagerActivity.RESULT_CUSTOMER_DELETED, resultIntent)
                            viewModel.deleteCustomer()
                        }
                        .setNegativeButton(getString(R.string.btn_cancel), null)
                        .show()
                },
                onTerminAnlegen = {
                    customer?.let { handleTerminAnlegen(it, id) }
                },
                onPauseCustomer = { weeks ->
                    customer?.let { pauseCustomer(it, weeks) } ?: showCustomerActionError()
                },
                onResumeCustomer = {
                    customer?.let { resumeCustomer(it) } ?: showCustomerActionError()
                },
                onTakePhoto = { photoManager?.showPhotoOptionsDialog() },
                onAdresseClick = {
                    customer?.let { c ->
                        val dest = when {
                            c.latitude != null && c.longitude != null -> "${c.latitude},${c.longitude}"
                            c.adresse.isNotBlank() || c.plz.isNotBlank() || c.stadt.isNotBlank() -> {
                                buildString {
                                    if (c.adresse.isNotBlank()) append(c.adresse.trim())
                                    val plzStadt = listOf(c.plz.trim(), c.stadt.trim()).filter { it.isNotEmpty() }.joinToString(" ")
                                    if (plzStadt.isNotEmpty()) {
                                        if (isNotEmpty()) append(", ")
                                        append(plzStadt)
                                    }
                                    if (isNotEmpty()) append(", Deutschland")
                                }.trim().takeIf { it.isNotEmpty() }
                            }
                            else -> null
                        }
                        if (dest != null) {
                            AlertDialog.Builder(this@CustomerDetailActivity)
                                .setTitle(getString(R.string.dialog_navigation_title))
                                .setMessage(getString(R.string.dialog_navigation_message))
                                .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                                    try {
                                        val uri = if (c.latitude != null && c.longitude != null) {
                                            Uri.parse("google.navigation:q=${c.latitude},${c.longitude}")
                                        } else {
                                            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(dest)}&dir_action=navigate")
                                        }
                                        startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps"))
                                    } catch (_: android.content.ActivityNotFoundException) {
                                        Toast.makeText(this@CustomerDetailActivity, getString(R.string.error_maps_not_installed), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .setNegativeButton(getString(R.string.btn_cancel), null)
                                .show()
                        } else Toast.makeText(this@CustomerDetailActivity, getString(R.string.toast_keine_adresse), Toast.LENGTH_SHORT).show()
                    }
                },
                onTelefonClick = {
                    customer?.let { c ->
                        if (c.telefon.isNotBlank()) {
                            AlertDialog.Builder(this@CustomerDetailActivity)
                                .setTitle(getString(R.string.dialog_anrufen_title))
                                .setMessage(getString(R.string.dialog_anrufen_message))
                                .setPositiveButton(getString(R.string.dialog_yes)) { _, _ ->
                                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.telefon}")))
                                }
                                .setNegativeButton(getString(R.string.btn_cancel), null)
                                .show()
                        } else Toast.makeText(this@CustomerDetailActivity, getString(R.string.toast_keine_telefonnummer), Toast.LENGTH_SHORT).show()
                    }
                },
                onPhotoClick = { url -> photoManager?.showImageInDialog(url) },
                onDeletePhoto = { url ->
                    AlertDialog.Builder(this@CustomerDetailActivity)
                        .setTitle(getString(R.string.dialog_delete_photo_title))
                        .setMessage(getString(R.string.dialog_delete_photo_message))
                        .setPositiveButton(getString(R.string.dialog_loeschen)) { _, _ ->
                            viewModel.deletePhoto(url) { success ->
                                if (success) Toast.makeText(this@CustomerDetailActivity, getString(R.string.toast_gespeichert), Toast.LENGTH_SHORT).show()
                                else Toast.makeText(this@CustomerDetailActivity, getString(R.string.error_save_generic), Toast.LENGTH_SHORT).show()
                            }
                        }
                        .setNegativeButton(getString(R.string.btn_cancel), null)
                        .show()
                },
                onDatumSelected = { position, isAbholung ->
                    val intervalle = editIntervalle.toMutableList()
                    IntervallManager.showDatumPickerForCustomer(
                        context = this@CustomerDetailActivity,
                        intervalle = intervalle,
                        position = position,
                        isAbholung = isAbholung,
                        onDatumSelected = { viewModel.updateEditIntervalle(intervalle) }
                    )
                },
                onDeleteIntervall = { index -> viewModel.removeIntervallAt(index) },
                onRemoveRegel = { regelId ->
                    AlertDialog.Builder(this@CustomerDetailActivity)
                        .setTitle(R.string.dialog_regel_vom_kunden_entfernen_titel)
                        .setMessage(R.string.dialog_regel_vom_kunden_entfernen_message)
                        .setPositiveButton(R.string.dialog_loeschen) { _, _ -> viewModel.removeRegelFromEdit(regelId) }
                        .setNegativeButton(R.string.btn_cancel, null)
                        .show()
                },
                onResetToAutomatic = { viewModel.resetToAutomaticIntervall(customer, editFormState) },
                onRegelClick = { },
                onUrlaubStartActivity = { customerId ->
                    startActivity(AppNavigation.toUrlaub(this, customerId = customerId))
                },
                onAddMonthlyIntervall = { viewModel.addMonthlyIntervall(it) },
                onAddAbholungTermin = { c ->
                    startActivity(AppNavigation.toAusnahmeTermin(this, customerId = c.id).apply {
                        putExtra(AusnahmeTerminActivity.EXTRA_ADD_ABHOLUNG_MIT_LIEFERUNG, true)
                    })
                },
                onAddAusnahmeTermin = { c ->
                    startActivity(AppNavigation.toAusnahmeTermin(this, customerId = c.id))
                },
                onDeleteNextTermin = { terminDatum ->
                    viewModel.deleteNaechstenTermin(terminDatum) { success ->
                        if (success) Toast.makeText(this@CustomerDetailActivity, getString(R.string.toast_gespeichert), Toast.LENGTH_SHORT).show()
                    }
                },
                onDeleteAusnahmeTermin = { termin ->
                    viewModel.deleteAusnahmeTermin(termin) { success ->
                        if (success) Toast.makeText(this@CustomerDetailActivity, getString(R.string.toast_gespeichert), Toast.LENGTH_SHORT).show()
                        else Toast.makeText(this@CustomerDetailActivity, getString(R.string.error_save_generic), Toast.LENGTH_SHORT).show()
                    }
                },
                onDeleteKundenTermin = { termins ->
                    viewModel.deleteKundenTermine(termins) { success ->
                        if (success) Toast.makeText(this@CustomerDetailActivity, getString(R.string.toast_gespeichert), Toast.LENGTH_SHORT).show()
                        else Toast.makeText(this@CustomerDetailActivity, getString(R.string.error_save_generic), Toast.LENGTH_SHORT).show()
                    }
                },
                onNeueErfassungKameraFotoBelege = {
                    startActivity(AppNavigation.toWaschenErfassung(this@CustomerDetailActivity, customerId = id, openFormularWithCamera = true))
                },
                onNeueErfassungFormularBelege = {
                    startActivity(AppNavigation.toWaschenErfassung(this@CustomerDetailActivity, customerId = id, openFormular = true))
                },
                onNeueErfassungManuellBelege = {
                    startActivity(AppNavigation.toWaschenErfassung(this@CustomerDetailActivity, customerId = id, openErfassen = true))
                },
                onBelegClick = { beleg ->
                    startActivity(AppNavigation.toWaschenErfassung(this@CustomerDetailActivity, customerId = id, belegMonthKey = beleg.monthKey))
                }
            )
            CustomerDetailScreen(state = detailState, actions = detailActions)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                        viewModel.loadComplete,
                        viewModel.currentCustomer,
                        viewModel.isLoading
                    ) { loadComplete, customer, isLoading ->
                        Triple(loadComplete, customer, isLoading)
                    }.collectLatest { (loadComplete, customer, isLoading) ->
                        // Nur beenden, wenn das Laden wirklich abgeschlossen ist, kein Kunde gefunden wurde, 
                        // wir nicht gerade laden UND wir nicht gerade gelöscht haben.
                        if (loadComplete && customer == null && !isLoading && !viewModel.deleted.value) {
                            // Kurze Verzögerung, um Firebase Zeit zu geben (besonders offline)
                            kotlinx.coroutines.delay(1000)
                            // Erneute Prüfung nach Verzögerung
                            if (viewModel.currentCustomer.value == null) {
                                Toast.makeText(this@CustomerDetailActivity, getString(R.string.error_customer_not_found), Toast.LENGTH_SHORT).show()
                                if (!isFinishing) finish()
                            }
                        }
                    }
                }
                launch {
                    viewModel.deleted.collectLatest { deleted ->
                        if (deleted) {
                            setResult(CustomerManagerActivity.RESULT_CUSTOMER_DELETED, Intent().apply { putExtra("DELETED_CUSTOMER_ID", id) })
                            Toast.makeText(this@CustomerDetailActivity, getString(R.string.toast_customer_deleted), Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                }
                launch {
                    viewModel.errorMessage.collectLatest { msg ->
                        msg?.let {
                            Toast.makeText(this@CustomerDetailActivity, getString(R.string.error_message_generic, it), Toast.LENGTH_SHORT).show()
                            viewModel.clearErrorMessage()
                        }
                    }
                }
            }
        }
    }

    private fun pauseCustomer(customer: Customer, pauseEndeWochen: Int?) {
        val now = System.currentTimeMillis()
        val pauseEnde = if (pauseEndeWochen == null || pauseEndeWochen <= 0) 0L
        else now + java.util.concurrent.TimeUnit.DAYS.toMillis(pauseEndeWochen * 7L)
        val updates = mapOf(
            "status" to CustomerStatus.PAUSIERT.name,
            "pauseStart" to now,
            "pauseEnde" to pauseEnde,
            "reaktivierungsDatum" to 0L
        )
        viewModel.saveCustomer(updates) { success ->
            if (success) {
                Toast.makeText(this, getString(R.string.toast_customer_paused_simple), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resumeCustomer(customer: Customer) {
        val updates = mapOf(
            "status" to CustomerStatus.AKTIV.name,
            "pauseStart" to 0L,
            "pauseEnde" to 0L,
            "reaktivierungsDatum" to System.currentTimeMillis()
        )
        viewModel.saveCustomer(updates) { success ->
            if (success) {
                Toast.makeText(this, getString(R.string.toast_customer_resumed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showCustomerActionError() {
        Toast.makeText(this, getString(R.string.error_customer_not_found), Toast.LENGTH_SHORT).show()
    }

    private fun handleTerminAnlegen(customer: Customer, id: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.label_termine_anlegen))
            .setMessage(getString(R.string.termin_anlegen_option_regulaer) + " oder " + getString(R.string.termin_anlegen_option_ausnahme) + "?")
            .setPositiveButton(getString(R.string.termin_anlegen_option_regulaer)) { _, _ ->
                when (customer.kundenTyp) {
                    KundenTyp.UNREGELMAESSIG -> {
                        if (customer.effectiveAbholungWochentage.isEmpty() && customer.effectiveAuslieferungWochentage.isEmpty()) {
                            Toast.makeText(this, getString(R.string.validation_unregelmaessig_al_required), Toast.LENGTH_LONG).show()
                            return@setPositiveButton
                        }
                        startTerminAnlegenDialogUnregelmaessig(customer)
                    }
                    KundenTyp.AUF_ABRUF, KundenTyp.REGELMAESSIG -> startTerminAnlegenDialogUnregelmaessig(customer)
                }
            }
            .setNeutralButton(getString(R.string.termin_anlegen_option_ausnahme)) { _, _ ->
                startActivity(AppNavigation.toAusnahmeTermin(this, customerId = customer.id))
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun startTerminAnlegenDialogUnregelmaessig(customer: Customer) {
        startActivity(AppNavigation.toTerminAnlegenUnregelmaessig(this, customer.id, customer.name))
    }

    override fun onDestroy() {
        networkMonitor.stopMonitoring()
        photoManager = null
        super.onDestroy()
    }
}
