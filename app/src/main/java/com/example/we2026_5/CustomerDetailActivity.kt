package com.example.we2026_5

import android.Manifest
import android.app.DatePickerDialog
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
import com.example.we2026_5.databinding.ActivityCustomerDetailBinding
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import org.koin.android.ext.android.inject

class CustomerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerDetailBinding
    private val repository: CustomerRepository by inject()
    private val storage: FirebaseStorage by inject()
    private var customerListener: ValueEventListener? = null
    private var currentCustomer: Customer? = null
    private lateinit var customerId: String
    private var isInEditMode = false

    private lateinit var photoAdapter: PhotoAdapter
    private var latestTmpUri: Uri? = null
    
    // Intervalle-Verwaltung
    private val intervalle = mutableListOf<CustomerIntervall>()
    private lateinit var intervallAdapter: IntervallAdapter
    private lateinit var intervallViewAdapter: IntervallViewAdapter // Read-Only für View-Mode
    private var aktuellesIntervallPosition: Int = -1
    private var aktuellesDatumTyp: Boolean = true // true = Abholung, false = Auslieferung

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let { uri -> uploadImage(uri) }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { uploadImage(it) }
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Kamera-Berechtigung verweigert", Toast.LENGTH_SHORT).show()
        }
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

        setupUI()
        loadCustomer()
        toggleEditMode(false)
    }

    private fun setupUI() {
        photoAdapter = PhotoAdapter(listOf()) { photoUrl ->
            showImageInDialog(photoUrl)
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
                showDatumPicker(position, isAbholung)
            }
        )
        binding.rvDetailIntervalle.layoutManager = LinearLayoutManager(this)
        binding.rvDetailIntervalle.adapter = intervallAdapter

        // Intervall-View-Adapter für Read-Only-Anzeige im View-Mode
        intervallViewAdapter = IntervallViewAdapter(emptyList())
        binding.rvDetailIntervalleView.layoutManager = LinearLayoutManager(this)
        binding.rvDetailIntervalleView.adapter = intervallViewAdapter

        // Intervall hinzufügen Button
        binding.btnDetailIntervallHinzufuegen.setOnClickListener {
            val neuesIntervall = CustomerIntervall()
            intervallAdapter.addIntervall(neuesIntervall)
        }

        binding.btnDetailBack.setOnClickListener { finish() }
        binding.tvDetailAdresse.setOnClickListener { startNavigation() }
        binding.tvDetailTelefon.setOnClickListener { startPhoneCall() }

        binding.btnTakePhoto.setOnClickListener { showPhotoOptionsDialog() }

        binding.btnEditCustomer.setOnClickListener { toggleEditMode(true) }
        binding.btnSaveCustomer.setOnClickListener { handleSave() }
        binding.btnDeleteCustomer.setOnClickListener { showDeleteConfirmation() }
    }

    private fun toggleEditMode(isEditing: Boolean) {
        isInEditMode = isEditing
        val viewModeVisibility = if (isEditing) View.GONE else View.VISIBLE
        val editModeVisibility = if (isEditing) View.VISIBLE else View.GONE

        binding.groupTextViews.visibility = viewModeVisibility
        binding.groupEditTexts.visibility = editModeVisibility

        binding.btnEditCustomer.visibility = viewModeVisibility
        binding.btnSaveCustomer.visibility = editModeVisibility
        binding.btnDeleteCustomer.visibility = editModeVisibility

        if (isEditing) {
            binding.etDetailName.setText(currentCustomer?.name)
            binding.etDetailAdresse.setText(currentCustomer?.adresse)
            binding.etDetailTelefon.setText(currentCustomer?.telefon)
            binding.etDetailNotizen.setText(currentCustomer?.notizen)
            
            // Kunden-Art RadioButton setzen
            currentCustomer?.let { customer ->
                when (customer.kundenArt) {
                    "Gewerblich" -> binding.rgDetailKundenArt.check(binding.rbDetailGewerblich.id)
                    "Privat" -> binding.rgDetailKundenArt.check(binding.rbDetailPrivat.id)
                    "Liste" -> binding.rgDetailKundenArt.check(binding.rbDetailListe.id)
                    else -> binding.rgDetailKundenArt.check(binding.rbDetailGewerblich.id)
                }
                
                // Intervall-Card Sichtbarkeit basierend auf Kunden-Art
                val sollIntervallAnzeigen = customer.kundenArt == "Gewerblich" || customer.kundenArt == "Liste"
                binding.cardDetailIntervall.visibility = if (sollIntervallAnzeigen) View.VISIBLE else View.GONE
                
                // Listener für Kunden-Art-Änderung
                binding.rgDetailKundenArt.setOnCheckedChangeListener { _, checkedId ->
                    val neueKundenArt = when (checkedId) {
                        binding.rbDetailGewerblich.id -> "Gewerblich"
                        binding.rbDetailPrivat.id -> "Privat"
                        binding.rbDetailListe.id -> "Liste"
                        else -> "Gewerblich"
                    }
                    // Intervall-Card anzeigen/ausblenden basierend auf neuer Kunden-Art
                    binding.cardDetailIntervall.visibility = if (neueKundenArt == "Gewerblich" || neueKundenArt == "Liste") {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
                
                // Intervalle laden und anzeigen
                intervalle.clear()
                intervalle.addAll(customer.intervalle)
                intervallAdapter.updateIntervalle(intervalle.toList())
            }
            
            // Google Maps Button für Adress-Auswahl
            binding.btnSelectLocation.setOnClickListener {
                openMapsForLocationSelection()
            }
        } else {
            binding.tvDetailName.text = currentCustomer?.name
        }
    }

    private fun handleSave() {
        val name = binding.etDetailName.text.toString().trim()
        if (name.isEmpty()) {
            binding.etDetailName.error = "Name fehlt"
            return
        }

        // Adresse-Validierung
        val adresse = binding.etDetailAdresse.text.toString().trim()
        if (adresse.isNotEmpty() && !ValidationHelper.isValidAddress(adresse)) {
            binding.etDetailAdresse.error = "Adresse sollte Straße und Hausnummer enthalten"
            return
        }

        // Telefon-Validierung
        val telefon = binding.etDetailTelefon.text.toString().trim()
        if (telefon.isNotEmpty() && !ValidationHelper.isValidPhoneNumber(telefon)) {
            binding.etDetailTelefon.error = "Ungültiges Telefonnummer-Format"
            return
        }

        // Button sofort deaktivieren und visuelles Feedback geben
        CoroutineScope(Dispatchers.Main).launch {
            // WICHTIG: Button muss sichtbar bleiben, damit das Feedback sichtbar ist!
            runOnUiThread {
                binding.btnSaveCustomer.visibility = View.VISIBLE  // Explizit sichtbar machen
                binding.btnSaveCustomer.isEnabled = false
                binding.btnSaveCustomer.text = "Speichere..."
                binding.btnSaveCustomer.alpha = 0.6f
            }
            
            // Kunden-Art bestimmen
            val kundenArt = when (binding.rgDetailKundenArt.checkedRadioButtonId) {
                binding.rbDetailGewerblich.id -> "Gewerblich"
                binding.rbDetailPrivat.id -> "Privat"
                binding.rbDetailListe.id -> "Liste"
                else -> "Gewerblich"
            }
            
            // Intervalle aktualisieren (nur für Gewerblich und Liste)
            val customerIntervalle = if ((kundenArt == "Gewerblich" || kundenArt == "Liste") && intervalle.isNotEmpty()) {
                intervalle.toList()
            } else {
                currentCustomer?.intervalle ?: emptyList()
            }
            
            val updatedData = mapOf(
                "name" to name,
                "adresse" to adresse,
                "telefon" to telefon,
                "notizen" to binding.etDetailNotizen.text.toString().trim(),
                "kundenArt" to kundenArt,
                "wochentag" to 0, // Wochentag wird nicht mehr verwendet
                "intervalle" to customerIntervalle.map { 
                    mapOf(
                        "id" to it.id,
                        "abholungDatum" to it.abholungDatum,
                        "auslieferungDatum" to it.auslieferungDatum,
                        "wiederholen" to it.wiederholen,
                        "intervallTage" to it.intervallTage,
                        "intervallAnzahl" to it.intervallAnzahl,
                        "erstelltAm" to it.erstelltAm
                    )
                }
            )
            
            // Optimistische UI-Aktualisierung: UI sofort aktualisieren
            // ABER: Button sichtbar lassen für visuelles Feedback!
            currentCustomer?.let { customer ->
                val updatedCustomer = customer.copy(
                    name = name,
                    adresse = adresse,
                    telefon = telefon,
                    notizen = binding.etDetailNotizen.text.toString().trim(),
                    kundenArt = kundenArt,
                    wochentag = 0, // Wochentag wird nicht mehr verwendet
                    intervalle = customerIntervalle
                )
                currentCustomer = updatedCustomer
                // UI sofort aktualisieren (optimistisch), aber Button sichtbar lassen
                // toggleEditMode(false) wird NACH dem visuellen Feedback aufgerufen
            }
            
            updateCustomerData(updatedData, "Änderungen gespeichert") {
                // Visuelles Feedback nach erfolgreichem Update
                runOnUiThread {
                    // Button explizit sichtbar machen (falls durch toggleEditMode versteckt wurde)
                    binding.btnSaveCustomer.visibility = View.VISIBLE
                    binding.btnSaveCustomer.text = "✓ Gespeichert!"
                    binding.btnSaveCustomer.backgroundTintList = android.content.res.ColorStateList.valueOf(
                        resources.getColor(com.example.we2026_5.R.color.status_done, theme)
                    )
                    binding.btnSaveCustomer.alpha = 1.0f
                    
                    // Nach kurzer Verzögerung: Edit-Mode beenden und Button verstecken
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        // Jetzt erst Edit-Mode beenden (Button wird dadurch versteckt)
                        toggleEditMode(false)
                        // Button zurücksetzen für nächstes Mal
                        binding.btnSaveCustomer.isEnabled = true
                        binding.btnSaveCustomer.text = "Speichern"
                        binding.btnSaveCustomer.alpha = 1.0f
                    }, 1500)
                }
            }
        }
    }

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
    
    private fun showPhotoOptionsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Foto hinzufügen")
            .setItems(arrayOf("Kamera", "Galerie")) { _, which ->
                when (which) {
                    0 -> checkCameraPermissionAndStart() // Kamera
                    1 -> pickImageFromGallery() // Galerie
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun checkCameraPermissionAndStart() {
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> startCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun startCamera() {
        val picturesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (picturesDir == null) {
            Toast.makeText(this, "Speicherort nicht verfügbar", Toast.LENGTH_SHORT).show()
            return
        }
        
        val tmpFile = File.createTempFile("IMG_${System.currentTimeMillis()}_", ".jpg", picturesDir)
        // FileProvider Authority muss mit AndroidManifest.xml übereinstimmen
        latestTmpUri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.fileprovider", tmpFile)
        latestTmpUri?.let { takePictureLauncher.launch(it) }
    }

    private fun uploadImage(uri: Uri) {
        binding.progressBar.visibility = View.VISIBLE
        
        // Bild komprimieren und dann hochladen
        CoroutineScope(Dispatchers.Main).launch {
            val compressedFile = ImageUtils.compressImage(this@CustomerDetailActivity, uri)
            
            if (compressedFile == null) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@CustomerDetailActivity, "Fehler beim Komprimieren des Bildes", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            // Verwende StorageUploadManager für Offline-Unterstützung
            com.example.we2026_5.util.StorageUploadManager.uploadImage(
                context = this@CustomerDetailActivity,
                imageFile = compressedFile,
                customerId = customerId,
                onSuccess = { downloadUrl ->
                    addPhotoUrlToCustomer(downloadUrl)
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@CustomerDetailActivity, "Bild erfolgreich hochgeladen", Toast.LENGTH_SHORT).show()
                },
                onError = { exception ->
                    binding.progressBar.visibility = View.GONE
                    // Prüfe ob offline - dann in Queue
                    val isOffline = !isNetworkAvailable()
                    if (isOffline) {
                        Toast.makeText(this@CustomerDetailActivity, "Bild wird hochgeladen, sobald Internet verfügbar ist", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@CustomerDetailActivity, "Upload fehlgeschlagen: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        return capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
               capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun addPhotoUrlToCustomer(url: String) {
        CoroutineScope(Dispatchers.Main).launch {
            // Realtime Database: Liste laden, erweitern und zurückschreiben
            val customer = repository.getCustomerById(customerId)
            if (customer != null) {
                val updatedUrls = customer.fotoUrls.toMutableList()
                if (!updatedUrls.contains(url)) {
                    updatedUrls.add(url)
                    val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                        operation = { 
                            repository.updateCustomer(customerId, mapOf("fotoUrls" to updatedUrls))
                        },
                        context = this@CustomerDetailActivity,
                        errorMessage = "Fehler beim Speichern der Foto-URL.",
                        maxRetries = 3
                    )
                    
                    if (success == true) {
                        Toast.makeText(this@CustomerDetailActivity, "Foto hinzugefügt", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

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
                    if (!isInEditMode) {
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
        
        // Intervalle im View-Mode anzeigen (nur für Gewerblich und Liste)
        val sollIntervallAnzeigen = customer.kundenArt == "Gewerblich" || customer.kundenArt == "Liste"
        if (sollIntervallAnzeigen && customer.intervalle.isNotEmpty()) {
            binding.cardDetailIntervallView.visibility = View.VISIBLE
            intervallViewAdapter.updateIntervalle(customer.intervalle)
        } else {
            binding.cardDetailIntervallView.visibility = View.GONE
        }
    }
    
    private fun showDatumPicker(position: Int, isAbholung: Boolean) {
        val cal = Calendar.getInstance()
        val intervall = intervalle.getOrNull(position) ?: return
        
        // Aktuelles Datum oder Intervall-Datum verwenden
        val initialDatum = if (isAbholung && intervall.abholungDatum > 0) {
            cal.timeInMillis = intervall.abholungDatum
            intervall.abholungDatum
        } else if (!isAbholung && intervall.auslieferungDatum > 0) {
            cal.timeInMillis = intervall.auslieferungDatum
            intervall.auslieferungDatum
        } else {
            System.currentTimeMillis()
        }
        
        cal.timeInMillis = initialDatum
        
        DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { _, year: Int, month: Int, dayOfMonth: Int ->
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val selectedDatum = cal.timeInMillis
                
                // Intervall aktualisieren
                val updatedIntervall = if (isAbholung) {
                    intervall.copy(abholungDatum = selectedDatum)
                } else {
                    intervall.copy(auslieferungDatum = selectedDatum)
                }
                
                intervalle[position] = updatedIntervall
                intervallAdapter.updateIntervalle(intervalle.toList())
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    private fun showImageInDialog(url: String) {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_fullscreen_image, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.fullscreen_image)

        Glide.with(this)
            .load(url)
            .into(imageView)

        builder.setView(dialogView)
        builder.create().show()
    }


    private fun updateCustomerData(data: Map<String, Any>, toastMessage: String, onSuccess: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = { 
                    repository.updateCustomer(customerId, data)
                },
                context = this@CustomerDetailActivity,
                errorMessage = "Fehler beim Speichern. Bitte erneut versuchen.",
                maxRetries = 3
            )
            
            if (success == true) {
                Toast.makeText(this@CustomerDetailActivity, toastMessage, Toast.LENGTH_SHORT).show()
                // Callback nach erfolgreichem Update aufrufen
                onSuccess?.invoke()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        customerListener?.let { repository.removeListener(it) }
    }
}
