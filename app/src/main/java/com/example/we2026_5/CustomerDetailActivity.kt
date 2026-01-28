package com.example.we2026_5

import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.databinding.ActivityCustomerDetailBinding
import com.example.we2026_5.detail.CustomerPhotoManager
import com.example.we2026_5.detail.CustomerEditManager
import com.example.we2026_5.detail.CustomerDetailUISetup
import com.example.we2026_5.detail.CustomerDetailCallbacks
import com.example.we2026_5.util.IntervallManager
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import org.koin.android.ext.android.inject

class CustomerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerDetailBinding
    private val repository: CustomerRepository by inject()
    private val regelRepository: TerminRegelRepository by inject()
    private val storage: FirebaseStorage by inject()
    private var customerListener: ValueEventListener? = null
    private var currentCustomer: Customer? = null
    private lateinit var customerId: String

    private lateinit var photoManager: CustomerPhotoManager
    private lateinit var editManager: CustomerEditManager
    private lateinit var uiSetup: CustomerDetailUISetup
    private lateinit var callbacks: CustomerDetailCallbacks
    
    // Intervalle-Verwaltung
    private val intervalle = mutableListOf<CustomerIntervall>()
    private var aktuellesIntervallPosition: Int = -1
    private var aktuellesDatumTyp: Boolean = true // true = Abholung, false = Auslieferung

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        photoManager.handleCameraResult(isSuccess)
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        photoManager.handleGalleryResult(uri)
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        photoManager.handleCameraPermissionResult(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getStringExtra("CUSTOMER_ID")
        if (id == null) {
            Toast.makeText(this, "Kunden-ID fehlt!", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        customerId = id

        // PhotoManager initialisieren
        photoManager = CustomerPhotoManager(
            activity = this,
            repository = repository,
            customerId = customerId,
            takePictureLauncher = takePictureLauncher,
            pickImageLauncher = pickImageLauncher,
            cameraPermissionLauncher = cameraPermissionLauncher,
            onProgressVisibilityChanged = { isVisible ->
                binding.progressBar.visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
            }
        )

        // UI-Setup initialisieren
        uiSetup = CustomerDetailUISetup(
            activity = this,
            binding = binding,
            photoManager = photoManager,
            intervalle = intervalle
        )

        // Callbacks VOR setupUI initialisieren (für temporäre Verwendung)
        var tempCallbacks: CustomerDetailCallbacks? = null
        
        // UI-Setup ZUERST durchführen, damit Adapter initialisiert werden
        uiSetup.setupUI(
            onTerminAnlegenClick = { tempCallbacks?.showRegelAuswahlDialog() },
            onBackClick = { finish() },
            onAdresseClick = { tempCallbacks?.startNavigation() },
            onTelefonClick = { tempCallbacks?.startPhoneCall() },
            onTakePhotoClick = { photoManager.showPhotoOptionsDialog() },
            onEditClick = { editManager.toggleEditMode(true, currentCustomer) },
            onSaveClick = { 
                editManager.handleSave(currentCustomer) {
                    // Nach erfolgreichem Speichern
                }
            },
            onDeleteClick = { tempCallbacks?.showDeleteConfirmation() },
            onDatumSelected = { position, isAbholung ->
                aktuellesIntervallPosition = position
                aktuellesDatumTyp = isAbholung
                IntervallManager.showDatumPickerForCustomer(
                    context = this@CustomerDetailActivity,
                    intervalle = intervalle,
                    position = position,
                    isAbholung = isAbholung,
                    onDatumSelected = { updatedIntervall ->
                        uiSetup.intervallAdapter.updateIntervalle(intervalle.toList())
                    }
                )
            }
        )
        
        // Callbacks NACH setupUI initialisieren (benötigt intervallAdapter)
        callbacks = CustomerDetailCallbacks(
            activity = this,
            binding = binding,
            repository = repository,
            regelRepository = regelRepository,
            customerId = customerId,
            intervalle = intervalle,
            intervallAdapter = uiSetup.intervallAdapter,
            currentCustomer = currentCustomer
        )
        tempCallbacks = callbacks
        
        // IntervallViewAdapter mit Callback für Regel-Klick initialisieren
        uiSetup.intervallViewAdapter = IntervallViewAdapter(emptyList()) { regelId ->
            callbacks.showRegelInfoDialog(regelId)
        }
        binding.rvDetailIntervalleView.adapter = uiSetup.intervallViewAdapter
        
        // Click-Listener mit echten Callbacks aktualisieren
        binding.btnTerminAnlegen.setOnClickListener { callbacks.showRegelAuswahlDialog() }
        binding.btnTerminAnlegenView.setOnClickListener { callbacks.showRegelAuswahlDialog() }
        binding.tvDetailAdresse.setOnClickListener { callbacks.startNavigation() }
        binding.tvDetailTelefon.setOnClickListener { callbacks.startPhoneCall() }
        binding.btnEditCustomer.setOnClickListener { editManager.toggleEditMode(true, currentCustomer) }
        binding.btnSaveCustomer.setOnClickListener { 
            editManager.handleSave(currentCustomer) {
                // Nach erfolgreichem Speichern
            }
        }
        binding.btnDeleteCustomer.setOnClickListener { callbacks.showDeleteConfirmation() }
        
        // EditManager initialisieren
        editManager = CustomerEditManager(
            activity = this,
            binding = binding,
            repository = repository,
            customerId = customerId,
            intervalle = intervalle,
            intervallAdapter = uiSetup.intervallAdapter,
            onEditModeChanged = { isEditing -> /* Kann für weitere Logik verwendet werden */ },
            onCustomerUpdated = { updatedCustomer -> 
                currentCustomer = updatedCustomer
                callbacks.updateCurrentCustomer(updatedCustomer)
            },
            onMapsLocationRequested = { callbacks.openMapsForLocationSelection() }
        )
        
        loadCustomer()
        editManager.toggleEditMode(false, null)
    }

    private fun loadCustomer() {
        customerListener = repository.addCustomerListener(
            customerId = customerId,
            onUpdate = { customer ->
                customer?.let {
                    currentCustomer = it
                    callbacks.updateCurrentCustomer(it)
                    // UI immer aktualisieren, auch wenn im Edit-Mode (für Echtzeit-Updates)
                    // Wenn im Edit-Mode, werden die EditTexts nicht überschrieben, aber andere Felder schon
                    if (!editManager.isInEditMode()) {
                        uiSetup.updateUi(it)
                    } else {
                        // Im Edit-Mode: Nur nicht-editierbare Felder aktualisieren (z.B. Fotos)
                        uiSetup.photoAdapter.updatePhotos(it.fotoUrls)
                    }
                } ?: run {
                    if (!isFinishing) {
                        Toast.makeText(this, "Kunde nicht mehr vorhanden.", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            },
            onError = { error ->
                Toast.makeText(this, "Fehler: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onStop() {
        super.onStop()
        customerListener?.let { repository.removeListener(it) }
    }
}
