package com.example.we2026_5

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.databinding.ActivityCustomerDetailBinding
import com.example.we2026_5.detail.CustomerPhotoManager
import com.example.we2026_5.detail.CustomerEditManager
import com.example.we2026_5.util.IntervallManager
import com.example.we2026_5.util.TerminRegelManager
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import org.koin.android.ext.android.inject

class CustomerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerDetailBinding
    private val repository: CustomerRepository by inject()
    private val regelRepository: TerminRegelRepository by inject()
    private val storage: FirebaseStorage by inject()
    private var customerListener: ValueEventListener? = null
    private var currentCustomer: Customer? = null
    private lateinit var customerId: String

    private lateinit var photoAdapter: PhotoAdapter
    private lateinit var photoManager: CustomerPhotoManager
    private lateinit var editManager: CustomerEditManager
    
    // Intervalle-Verwaltung
    private val intervalle = mutableListOf<CustomerIntervall>()
    private lateinit var intervallAdapter: IntervallAdapter
    private lateinit var intervallViewAdapter: IntervallViewAdapter // Read-Only für View-Mode
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
                binding.progressBar.visibility = if (isVisible) View.VISIBLE else View.GONE
            }
        )

        setupUI()
        
        // EditManager initialisieren
        editManager = CustomerEditManager(
            activity = this,
            binding = binding,
            repository = repository,
            customerId = customerId,
            intervalle = intervalle,
            intervallAdapter = intervallAdapter,
            onEditModeChanged = { isEditing -> /* Kann für weitere Logik verwendet werden */ },
            onCustomerUpdated = { updatedCustomer -> currentCustomer = updatedCustomer },
            onMapsLocationRequested = { openMapsForLocationSelection() }
        )
        
        loadCustomer()
        editManager.toggleEditMode(false, null)
    }

    private fun setupUI() {
        photoAdapter = PhotoAdapter(listOf()) { photoUrl ->
            photoManager.showImageInDialog(photoUrl)
        }
        binding.rvPhotoThumbnails.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPhotoThumbnails.adapter = photoAdapter

        // Intervall-Adapter initialisieren
        intervallAdapter = IntervallAdapter(
            intervalle = intervalle.toMutableList(),
            onIntervallChanged = { neueIntervalle ->
                intervalle.clear()
                intervalle.addAll(neueIntervalle)
            },
            onDatumSelected = { position, isAbholung ->
                aktuellesIntervallPosition = position
                aktuellesDatumTyp = isAbholung
                IntervallManager.showDatumPickerForCustomer(
                    context = this@CustomerDetailActivity,
                    intervalle = intervalle,
                    position = position,
                    isAbholung = isAbholung,
                    onDatumSelected = { updatedIntervall ->
                        intervallAdapter.updateIntervalle(intervalle.toList())
                    }
                )
            }
        )
        binding.rvDetailIntervalle.layoutManager = LinearLayoutManager(this)
        binding.rvDetailIntervalle.adapter = intervallAdapter

        // Intervall-View-Adapter für Read-Only-Anzeige im View-Mode
        intervallViewAdapter = IntervallViewAdapter(emptyList())
        binding.rvDetailIntervalleView.layoutManager = LinearLayoutManager(this)
        binding.rvDetailIntervalleView.adapter = intervallViewAdapter

        // Termin Anlegen Button
        binding.btnTerminAnlegen.setOnClickListener {
            showRegelAuswahlDialog()
        }

        binding.btnDetailBack.setOnClickListener { finish() }
        binding.tvDetailAdresse.setOnClickListener { startNavigation() }
        binding.tvDetailTelefon.setOnClickListener { startPhoneCall() }

        binding.btnTakePhoto.setOnClickListener { photoManager.showPhotoOptionsDialog() }

        binding.btnEditCustomer.setOnClickListener { 
            editManager.toggleEditMode(true, currentCustomer) 
        }
        binding.btnSaveCustomer.setOnClickListener { 
            editManager.handleSave(currentCustomer) {
                // Nach erfolgreichem Speichern
            }
        }
        binding.btnDeleteCustomer.setOnClickListener { showDeleteConfirmation() }
    }

    // toggleEditMode und handleSave Funktionen entfernt - jetzt in CustomerEditManager

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Kunde löschen")
            .setMessage("Bist du sicher, dass du diesen Kunden endgültig löschen möchtest?")
            .setPositiveButton("Löschen") { _, _ -> handleDelete() }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun handleDelete() {
        AlertDialog.Builder(this)
            .setTitle("Kunde löschen?")
            .setMessage("Möchten Sie diesen Kunden wirklich löschen? Alle Termine dieses Kunden werden ebenfalls gelöscht.")
            .setPositiveButton("Löschen") { _, _ ->
                // Optimistische UI-Aktualisierung: Sofort benachrichtigen, dass Kunde gelöscht wurde
                // Damit die Liste in CustomerManagerActivity sofort aktualisiert wird
                val resultIntent = Intent().apply {
                    putExtra("DELETED_CUSTOMER_ID", customerId)
                }
                setResult(CustomerManagerActivity.RESULT_CUSTOMER_DELETED, resultIntent)
                
                CoroutineScope(Dispatchers.Main).launch {
                    val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                        operation = { 
                            // Kunde löschen - alle Termine werden automatisch gelöscht, da sie Teil des Kunden-Objekts sind
                            repository.deleteCustomer(customerId)
                        },
                        context = this@CustomerDetailActivity,
                        errorMessage = "Fehler beim Löschen. Bitte erneut versuchen.",
                        maxRetries = 3
                    )
                    
                    if (success == true) {
                        Toast.makeText(this@CustomerDetailActivity, "Kunde und alle Termine gelöscht", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        // Bei Fehler: Result zurücksetzen
                        setResult(RESULT_CANCELED)
                    }
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }
    
    // Photo-Management-Funktionen entfernt - jetzt in CustomerPhotoManager

    private fun startNavigation() {
        currentCustomer?.let {
            if (it.adresse.isNotBlank()) {
                val gmmIntentUri = Uri.parse("google.navigation:q=${it.adresse}")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    Toast.makeText(this, "Google Maps ist nicht installiert.", Toast.LENGTH_SHORT).show()
                }
            } else {
                 Toast.makeText(this, "Keine Adresse vorhanden.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun openMapsForLocationSelection() {
        val currentAddress = binding.etDetailAdresse.text.toString().trim()
        val query = if (currentAddress.isNotBlank()) currentAddress else "Deutschland"
        
        // Google Maps öffnen mit Suchfunktion
        val gmmIntentUri = Uri.parse("geo:0,0?q=$query")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        
        if (mapIntent.resolveActivity(packageManager) != null) {
            // Starte Maps - Benutzer kann dann Adresse auswählen und kopieren
            startActivity(mapIntent)
            Toast.makeText(this, "Wählen Sie einen Ort in Google Maps aus und kopieren Sie die Adresse hierher ein.", Toast.LENGTH_LONG).show()
        } else {
            // Fallback: Browser öffnen
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$query"))
            if (webIntent.resolveActivity(packageManager) != null) {
                startActivity(webIntent)
            } else {
                Toast.makeText(this, "Google Maps ist nicht verfügbar.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun startPhoneCall() {
        currentCustomer?.let {
            if (it.telefon.isNotBlank()) {
                val dialIntent = Intent(Intent.ACTION_DIAL)
                dialIntent.data = Uri.parse("tel:${it.telefon}")
                startActivity(dialIntent)
            } else {
                Toast.makeText(this, "Keine Telefonnummer vorhanden.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadCustomer() {
        customerListener = repository.addCustomerListener(
            customerId = customerId,
            onUpdate = { customer ->
                customer?.let {
                    currentCustomer = it
                    // UI immer aktualisieren, auch wenn im Edit-Mode (für Echtzeit-Updates)
                    // Wenn im Edit-Mode, werden die EditTexts nicht überschrieben, aber andere Felder schon
                    if (!editManager.isInEditMode()) {
                        updateUi(it)
                    } else {
                        // Im Edit-Mode: Nur nicht-editierbare Felder aktualisieren (z.B. Fotos)
                        photoAdapter.updatePhotos(it.fotoUrls)
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

    private fun updateUi(customer: Customer) {
        binding.tvDetailName.text = customer.name
        binding.tvDetailKundenArt.text = customer.kundenArt
        binding.tvDetailAdresse.text = customer.adresse
        binding.tvDetailTelefon.text = customer.telefon
        binding.tvDetailNotizen.text = customer.notizen
        photoAdapter.updatePhotos(customer.fotoUrls)

        // Kunden-Typ Button (G/P/L) anzeigen
        com.example.we2026_5.ui.CustomerTypeButtonHelper.setupButton(binding.btnKundenTyp, customer, this)
        
        // Intervalle im View-Mode anzeigen (nur für Gewerblich und Liste)
        val sollIntervallAnzeigen = customer.kundenArt == "Gewerblich" || customer.kundenArt == "Liste"
        if (sollIntervallAnzeigen && customer.intervalle.isNotEmpty()) {
            binding.cardDetailIntervallView.visibility = View.VISIBLE
            intervallViewAdapter.updateIntervalle(customer.intervalle)
        } else {
            binding.cardDetailIntervallView.visibility = View.GONE
        }
    }
    
    private fun showRegelAuswahlDialog() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val regeln = regelRepository.getAllRegeln()
                
                if (regeln.isEmpty()) {
                    Toast.makeText(this@CustomerDetailActivity, "Keine Regeln vorhanden. Bitte erstellen Sie zuerst eine Regel.", Toast.LENGTH_LONG).show()
                    return@launch
                }
                
                val regelNamen = regeln.map { it.name }.toTypedArray()
                
                AlertDialog.Builder(this@CustomerDetailActivity)
                    .setTitle("Termin-Regel auswählen")
                    .setItems(regelNamen) { _, which ->
                        val ausgewaehlteRegel = regeln[which]
                        wendeRegelAn(ausgewaehlteRegel)
                    }
                    .setNegativeButton("Abbrechen", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@CustomerDetailActivity, "Fehler beim Laden der Regeln: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun wendeRegelAn(regel: TerminRegel) {
        val customer = currentCustomer ?: return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Regel auf Kunden anwenden
                val neuesIntervall = TerminRegelManager.wendeRegelAufKundeAn(regel, customer)
                
                // Intervall zur Liste hinzufügen
                val neueIntervalle = customer.intervalle.toMutableList()
                neueIntervalle.add(neuesIntervall)
                
                // Kunden aktualisieren
                val updates = mapOf(
                    "intervalle" to neueIntervalle
                )
                
                val success = repository.updateCustomer(customer.id, updates)
                if (success) {
                    // Verwendungsanzahl erhöhen
                    regelRepository.incrementVerwendungsanzahl(regel.id)
                    
                    Toast.makeText(this@CustomerDetailActivity, "Regel '${regel.name}' angewendet", Toast.LENGTH_SHORT).show()
                    
                    // UI aktualisieren
                    intervalle.clear()
                    intervalle.addAll(neueIntervalle)
                    intervallAdapter.updateIntervalle(intervalle.toList())
                } else {
                    Toast.makeText(this@CustomerDetailActivity, "Fehler beim Anwenden der Regel", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CustomerDetailActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // showDatumPicker Funktion entfernt - jetzt in IntervallManager
    
    // showImageInDialog Funktion entfernt - jetzt in CustomerPhotoManager


    // updateCustomerData Funktion entfernt - jetzt in CustomerEditManager

    override fun onStop() {
        super.onStop()
        customerListener?.let { repository.removeListener(it) }
    }
}
