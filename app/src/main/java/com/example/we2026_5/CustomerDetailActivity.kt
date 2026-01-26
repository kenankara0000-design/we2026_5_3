package com.example.we2026_5

import android.Manifest
import android.app.DatePickerDialog
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
import com.example.we2026_5.databinding.ActivityCustomerDetailBinding
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class CustomerDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var customerListener: ListenerRegistration? = null
    private var currentCustomer: Customer? = null
    private lateinit var customerId: String
    private var isInEditMode = false

    private lateinit var photoAdapter: PhotoAdapter
    private var latestTmpUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let { uri -> uploadImage(uri) }
        }
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

        binding.btnDetailBack.setOnClickListener { finish() }
        binding.tvDetailAdresse.setOnClickListener { startNavigation() }
        binding.tvDetailTelefon.setOnClickListener { startPhoneCall() }

        binding.btnTakePhoto.setOnClickListener { checkCameraPermissionAndStart() }

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
            binding.etDetailIntervall.setText(currentCustomer?.intervallTage?.toString() ?: "7")
            binding.etDetailNotizen.setText(currentCustomer?.notizen)
        } else {
            binding.tvDetailName.text = currentCustomer?.name
        }
    }

    private fun handleSave() {
        val intervallInput = binding.etDetailIntervall.text.toString().toIntOrNull() ?: 7
        val intervall = when {
            intervallInput < 1 -> {
                binding.etDetailIntervall.error = "Intervall muss mindestens 1 Tag sein"
                return
            }
            intervallInput > 365 -> {
                binding.etDetailIntervall.error = "Intervall darf maximal 365 Tage sein"
                return
            }
            else -> intervallInput
        }

        val updatedData = mapOf(
            "name" to binding.etDetailName.text.toString(),
            "adresse" to binding.etDetailAdresse.text.toString(),
            "telefon" to binding.etDetailTelefon.text.toString(),
            "intervallTage" to intervall,
            "notizen" to binding.etDetailNotizen.text.toString()
        )
        updateCustomerData(updatedData, "Änderungen gespeichert")
        toggleEditMode(false)
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
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeWithRetryAndToast(
                operation = { 
                    db.collection("customers").document(customerId).delete()
                },
                context = this@CustomerDetailActivity,
                errorMessage = "Fehler beim Löschen. Bitte erneut versuchen.",
                maxRetries = 3
            )
            
            if (success != null) {
                Toast.makeText(this@CustomerDetailActivity, "Kunde gelöscht", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun checkCameraPermissionAndStart() {
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> startCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
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
            
            val storageRef = storage.reference.child("customer_photos/${customerId}/${System.currentTimeMillis()}.jpg")
            val compressedUri = Uri.fromFile(compressedFile)
            
            // Upload mit Retry-Logik
            val uploadTask = FirebaseRetryHelper.executeWithRetryAndToast(
                operation = { storageRef.putFile(compressedUri) },
                context = this@CustomerDetailActivity,
                errorMessage = "Upload fehlgeschlagen. Bitte erneut versuchen.",
                maxRetries = 3
            )
            
            if (uploadTask != null) {
                // Download-URL mit Retry-Logik abrufen
                val downloadUrl = FirebaseRetryHelper.executeWithRetryAndToast(
                    operation = { storageRef.downloadUrl },
                    context = this@CustomerDetailActivity,
                    errorMessage = "Fehler beim Abrufen der Download-URL.",
                    maxRetries = 3
                )
                
                downloadUrl?.let { url ->
                    addPhotoUrlToCustomer(url.toString())
                }
            }
            
            // Temporäres File löschen
            compressedFile.delete()
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun addPhotoUrlToCustomer(url: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeWithRetryAndToast(
                operation = { 
                    db.collection("customers").document(customerId)
                        .update("fotoUrls", FieldValue.arrayUnion(url))
                },
                context = this@CustomerDetailActivity,
                errorMessage = "Fehler beim Speichern der Foto-URL.",
                maxRetries = 3
            )
            
            if (success != null) {
                Toast.makeText(this@CustomerDetailActivity, "Foto hinzugefügt", Toast.LENGTH_SHORT).show()
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
        customerListener = db.collection("customers").document(customerId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Toast.makeText(this, "Fehler: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            snapshot?.toObject(Customer::class.java)?.let {
                currentCustomer = it
                if (!isInEditMode) updateUi(it)
            } ?: run {
                if (!isFinishing) {
                    Toast.makeText(this, "Kunde nicht mehr vorhanden.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun updateUi(customer: Customer) {
        binding.tvDetailName.text = customer.name
        binding.tvDetailAdresse.text = customer.adresse
        binding.tvDetailTelefon.text = customer.telefon
        binding.tvDetailIntervall.text = "${customer.intervallTage} Tage"
        binding.tvDetailNotizen.text = customer.notizen
        photoAdapter.updatePhotos(customer.fotoUrls)
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


    private fun updateCustomerData(data: Map<String, Any>, toastMessage: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeWithRetryAndToast(
                operation = { 
                    db.collection("customers").document(customerId).update(data)
                },
                context = this@CustomerDetailActivity,
                errorMessage = "Fehler beim Speichern. Bitte erneut versuchen.",
                maxRetries = 3
            )
            
            if (success != null) {
                Toast.makeText(this@CustomerDetailActivity, toastMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        customerListener?.remove()
    }
}
