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

        binding.btnAbholung.setOnClickListener { handleAbholung() }
        binding.btnAuslieferung.setOnClickListener { handleAuslieferung() }
        binding.btnVerschieben.setOnClickListener { showVerschiebenDialog() }
        binding.btnUrlaub.setOnClickListener { showUrlaubDialog() }
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
            binding.etDetailNotizen.setText(currentCustomer?.notizen)
        } else {
            binding.tvDetailName.text = currentCustomer?.name
        }
    }

    private fun handleSave() {
        val updatedData = mapOf(
            "name" to binding.etDetailName.text.toString(),
            "adresse" to binding.etDetailAdresse.text.toString(),
            "telefon" to binding.etDetailTelefon.text.toString(),
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
        db.collection("customers").document(customerId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Kunde gelöscht", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener { e -> Toast.makeText(this, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show() }
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
        val storageRef = storage.reference.child("customer_photos/${customerId}/${System.currentTimeMillis()}.jpg")
        storageRef.putFile(uri)
            .addOnSuccessListener { 
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    addPhotoUrlToCustomer(downloadUri.toString())
                    binding.progressBar.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Upload fehlgeschlagen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addPhotoUrlToCustomer(url: String) {
        db.collection("customers").document(customerId)
            .update("fotoUrls", FieldValue.arrayUnion(url))
            .addOnSuccessListener { Toast.makeText(this, "Foto hinzugefügt", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(this, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show() }
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

    private fun handleAbholung() {
        currentCustomer?.takeIf { !it.abholungErfolgt }?.let {
            updateCustomerData(mapOf("abholungErfolgt" to true), "Abholung registriert")
        }
    }

    private fun handleAuslieferung() {
        val customer = currentCustomer ?: return
        if (customer.auslieferungErfolgt) return
        
        // Race Condition vermeiden: currentCustomer direkt verwenden
        val wasAbholungErfolgt = customer.abholungErfolgt
        updateCustomerData(mapOf("auslieferungErfolgt" to true), "Auslieferung registriert")
        
        // Wenn Abholung schon war, Tour-Zyklus zurücksetzen
        if (wasAbholungErfolgt) {
            resetTourCycle()
        }
    }

    private fun resetTourCycle() {
        val resetData = mapOf(
            "letzterTermin" to System.currentTimeMillis(),
            "abholungErfolgt" to false,
            "auslieferungErfolgt" to false,
            "verschobenAufDatum" to 0
        )
        updateCustomerData(resetData, "Tour für diesen Kunden abgeschlossen!")
    }

    private fun showVerschiebenDialog() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            val picked = Calendar.getInstance().apply { set(year, month, dayOfMonth, 0, 0, 0) }
            updateCustomerData(mapOf("verschobenAufDatum" to picked.timeInMillis), "Termin verschoben")
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showUrlaubDialog() {
        val cal = Calendar.getInstance()
        var urlaubVon: Long = 0

        val dateSetListenerVon = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val pickedVon = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) }
            urlaubVon = pickedVon.timeInMillis

            val dateSetListenerBis = DatePickerDialog.OnDateSetListener { _, y, m, d ->
                val pickedBis = Calendar.getInstance().apply { set(y, m, d, 23, 59, 59) }
                if (pickedBis.timeInMillis >= urlaubVon) {
                    updateCustomerData(mapOf("urlaubVon" to urlaubVon, "urlaubBis" to pickedBis.timeInMillis), "Urlaub eingetragen")
                } else {
                    Toast.makeText(this, "Enddatum muss nach Startdatum sein!", Toast.LENGTH_SHORT).show()
                }
            }

            DatePickerDialog(this, dateSetListenerBis, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
                setTitle("Urlaub bis")
                show()
            }
        }

        DatePickerDialog(this, dateSetListenerVon, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
            setTitle("Urlaub von")
            show()
        }
    }

    private fun updateCustomerData(data: Map<String, Any>, toastMessage: String) {
        db.collection("customers").document(customerId).update(data)
            .addOnSuccessListener { Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { e -> Toast.makeText(this, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    override fun onStop() {
        super.onStop()
        customerListener?.remove()
    }
}
