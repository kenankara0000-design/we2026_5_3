package com.example.we2026_5

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.detail.CustomerPhotoManager
import com.example.we2026_5.ui.detail.CustomerDetailScreen
import com.example.we2026_5.ui.detail.CustomerDetailViewModel
import com.example.we2026_5.util.IntervallManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class CustomerDetailActivity : AppCompatActivity() {

    private val viewModel: CustomerDetailViewModel by viewModel()
    private val repository: CustomerRepository by inject()
    private val regelRepository: TerminRegelRepository by inject()

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
            val customer by viewModel.currentCustomer.collectAsState(initial = null)
            val isInEditMode by viewModel.isInEditMode.collectAsState(initial = false)
            val editIntervalle by viewModel.editIntervalle.collectAsState(initial = emptyList())
            val isLoading by viewModel.isLoading.collectAsState(initial = false)
            val isUploading by viewModel.isUploading.collectAsState(initial = false)

            CustomerDetailScreen(
                customer = customer,
                isInEditMode = isInEditMode,
                editIntervalle = editIntervalle,
                isLoading = isLoading,
                isUploading = isUploading,
                onBack = { finish() },
                onEdit = { viewModel.setEditMode(true, customer) },
                onSave = { updates ->
                    viewModel.saveCustomer(updates) { success ->
                        if (success) {
                            viewModel.setEditMode(false, null)
                            Toast.makeText(this@CustomerDetailActivity, getString(R.string.toast_gespeichert), Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDelete = {
                    AlertDialog.Builder(this@CustomerDetailActivity)
                        .setTitle("Kunde löschen?")
                        .setMessage("Möchten Sie diesen Kunden wirklich löschen? Alle Termine dieses Kunden werden ebenfalls gelöscht.")
                        .setPositiveButton("Löschen") { _, _ ->
                            val resultIntent = Intent().apply { putExtra("DELETED_CUSTOMER_ID", id) }
                            setResult(com.example.we2026_5.CustomerManagerActivity.RESULT_CUSTOMER_DELETED, resultIntent)
                            viewModel.deleteCustomer()
                        }
                        .setNegativeButton("Abbrechen", null)
                        .show()
                },
                onTerminAnlegen = {
                    startActivity(Intent(this@CustomerDetailActivity, TerminRegelManagerActivity::class.java).apply {
                        putExtra("CUSTOMER_ID", id)
                    })
                },
                onTakePhoto = { photoManager?.showPhotoOptionsDialog() },
                onAdresseClick = {
                    customer?.let { c ->
                        if (c.adresse.isNotBlank()) {
                            val gmmUri = Uri.parse("google.navigation:q=${Uri.encode(c.adresse)}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmUri).setPackage("com.google.android.apps.maps")
                            if (mapIntent.resolveActivity(packageManager) != null) startActivity(mapIntent)
                            else Toast.makeText(this@CustomerDetailActivity, getString(R.string.error_maps_not_installed), Toast.LENGTH_SHORT).show()
                        } else Toast.makeText(this@CustomerDetailActivity, getString(R.string.toast_keine_adresse), Toast.LENGTH_SHORT).show()
                    }
                },
                onTelefonClick = {
                    customer?.let { c ->
                        if (c.telefon.isNotBlank()) {
                            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${c.telefon}")))
                        } else Toast.makeText(this@CustomerDetailActivity, getString(R.string.toast_keine_telefonnummer), Toast.LENGTH_SHORT).show()
                    }
                },
                onPhotoClick = { url -> photoManager?.showImageInDialog(url) },
                onDatumSelected = { position, isAbholung ->
                    val intervalle = editIntervalle.toMutableList()
                    IntervallManager.showDatumPickerForCustomer(
                        context = this@CustomerDetailActivity,
                        intervalle = intervalle,
                        position = position,
                        isAbholung = isAbholung,
                        onDatumSelected = { viewModel.updateEditIntervalle(intervalle) }
                    )
                }
            )
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

    override fun onDestroy() {
        photoManager = null
        super.onDestroy()
    }
}
