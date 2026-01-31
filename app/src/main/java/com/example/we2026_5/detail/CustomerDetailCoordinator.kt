package com.example.we2026_5.detail

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.IntervallViewAdapter
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.databinding.ActivityCustomerDetailBinding
import com.example.we2026_5.util.IntervallManager
import com.example.we2026_5.ui.detail.CustomerDetailViewModel

/**
 * Ein Coordinator pro Bildschirm: kapselt UISetup, Callbacks, EditManager, PhotoManager.
 * Die Activity spricht nur mit dem Coordinator (setupUi, updateCustomer) und dem ViewModel (State beobachten).
 */
class CustomerDetailCoordinator(
    private val activity: AppCompatActivity,
    private val binding: ActivityCustomerDetailBinding,
    private val viewModel: CustomerDetailViewModel,
    private val repository: CustomerRepository,
    private val regelRepository: TerminRegelRepository,
    private val customerId: String,
    private val takePictureLauncher: ActivityResultLauncher<android.net.Uri>,
    private val pickImageLauncher: ActivityResultLauncher<String>,
    private val cameraPermissionLauncher: ActivityResultLauncher<String>,
    private val onProgressVisibilityChanged: (Boolean) -> Unit
) {
    private val intervalle = mutableListOf<CustomerIntervall>()

    private lateinit var photoManager: CustomerPhotoManager
    private lateinit var uiSetup: CustomerDetailUISetup
    private lateinit var callbacks: CustomerDetailCallbacks
    private lateinit var editManager: CustomerEditManager

    /**
     * Einmaliges Setup: erstellt alle Helper und verbindet sie.
     */
    fun setupUi() {
        photoManager = CustomerPhotoManager(
            activity = activity,
            repository = repository,
            customerId = customerId,
            takePictureLauncher = takePictureLauncher,
            pickImageLauncher = pickImageLauncher,
            cameraPermissionLauncher = cameraPermissionLauncher,
            onProgressVisibilityChanged = onProgressVisibilityChanged
        )

        uiSetup = CustomerDetailUISetup(
            activity = activity,
            binding = binding,
            photoManager = photoManager,
            intervalle = intervalle
        )

        callbacks = CustomerDetailCallbacks(
            activity = activity,
            binding = binding,
            repository = repository,
            regelRepository = regelRepository,
            customerId = customerId,
            intervalle = intervalle,
            intervallAdapter = uiSetup.intervallAdapter,
            currentCustomer = null,
            onDeleteRequested = { viewModel.deleteCustomer() }
        )

        editManager = CustomerEditManager(
            activity = activity,
            binding = binding,
            customerId = customerId,
            intervalle = intervalle,
            intervallAdapter = uiSetup.intervallAdapter,
            onSaveUpdates = { data, onResult ->
                viewModel.saveCustomer(data, onResult)
            },
            onEditModeChanged = { },
            onCustomerUpdated = { callbacks.updateCurrentCustomer(it) },
            onMapsLocationRequested = { callbacks.openMapsForLocationSelection() }
        )

        uiSetup.setupUI(
            onTerminAnlegenClick = { callbacks.showRegelAuswahlDialog() },
            onBackClick = { activity.finish() },
            onAdresseClick = { callbacks.startNavigation() },
            onTelefonClick = { callbacks.startPhoneCall() },
            onTakePhotoClick = { photoManager.showPhotoOptionsDialog() },
            onEditClick = { editManager.toggleEditMode(true, callbacks.currentCustomerForEdit) },
            onSaveClick = {
                editManager.handleSave(callbacks.currentCustomerForEdit) { }
            },
            onDeleteClick = { callbacks.showDeleteConfirmation() },
            onDatumSelected = { position, isAbholung ->
                IntervallManager.showDatumPickerForCustomer(
                    context = activity,
                    intervalle = intervalle,
                    position = position,
                    isAbholung = isAbholung,
                    onDatumSelected = {
                        uiSetup.intervallAdapter.updateIntervalle(intervalle.toList())
                    }
                )
            }
        )

        uiSetup.intervallViewAdapter = IntervallViewAdapter(emptyList()) { regelId ->
            callbacks.showRegelInfoDialog(regelId)
        }
        binding.rvDetailIntervalleView.adapter = uiSetup.intervallViewAdapter

        binding.btnTerminAnlegen.setOnClickListener { callbacks.showRegelAuswahlDialog() }
        binding.btnTerminAnlegenView.setOnClickListener { callbacks.showRegelAuswahlDialog() }
        binding.tvDetailAdresse.setOnClickListener { callbacks.startNavigation() }
        binding.tvDetailTelefon.setOnClickListener { callbacks.startPhoneCall() }
        binding.btnEditCustomer.setOnClickListener { editManager.toggleEditMode(true, callbacks.currentCustomerForEdit) }
        binding.btnSaveCustomer.setOnClickListener {
            editManager.handleSave(callbacks.currentCustomerForEdit) { }
        }
        binding.btnDeleteCustomer.setOnClickListener { callbacks.showDeleteConfirmation() }

        editManager.toggleEditMode(false, null)
    }

    /**
     * Wird von der Activity aufgerufen, wenn das ViewModel einen neuen Kunden emittiert.
     */
    fun updateCustomer(customer: Customer?) {
        callbacks.updateCurrentCustomer(customer)
        if (!editManager.isInEditMode()) {
            customer?.let { uiSetup.updateUi(it) }
        } else {
            uiSetup.photoAdapter.updatePhotos(customer?.fotoUrls ?: emptyList())
        }
    }

    /**
     * Liefert den aktuellen Kunden für EditManager (z. B. handleSave braucht currentCustomer).
     * Callbacks hält die Referenz; wir müssen sie von außen abfragbar machen.
     */
    fun getCurrentCustomerForEdit(): Customer? = callbacks.currentCustomerForEdit

    /** Launcher-Results von der Activity an PhotoManager weiterreichen. */
    fun onTakePictureResult(success: Boolean) {
        if (::photoManager.isInitialized) photoManager.handleCameraResult(success)
    }

    fun onPickImageResult(uri: Uri?) {
        if (::photoManager.isInitialized) photoManager.handleGalleryResult(uri)
    }

    fun onCameraPermissionResult(granted: Boolean) {
        if (::photoManager.isInitialized) photoManager.handleCameraPermissionResult(granted)
    }
}
