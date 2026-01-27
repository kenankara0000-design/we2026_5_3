package com.example.we2026_5.detail

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.we2026_5.FirebaseRetryHelper
import com.example.we2026_5.ImageUtils
import com.example.we2026_5.R
import com.example.we2026_5.data.repository.CustomerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Manager für Photo-Upload, -Anzeige und -Verwaltung.
 * Extrahiert die Photo-Management-Logik aus CustomerDetailActivity.
 */
class CustomerPhotoManager(
    private val activity: android.app.Activity,
    private val repository: CustomerRepository,
    private val customerId: String,
    private val takePictureLauncher: ActivityResultLauncher<Uri>,
    private val pickImageLauncher: ActivityResultLauncher<String>,
    private val cameraPermissionLauncher: ActivityResultLauncher<String>,
    private val onProgressVisibilityChanged: (Boolean) -> Unit
) {
    
    private var latestTmpUri: Uri? = null
    
    fun showPhotoOptionsDialog() {
        AlertDialog.Builder(activity)
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
        when (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> startCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }
    
    private fun startCamera() {
        val picturesDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (picturesDir == null) {
            Toast.makeText(activity, "Speicherort nicht verfügbar", Toast.LENGTH_SHORT).show()
            return
        }
        
        val tmpFile = File.createTempFile("IMG_${System.currentTimeMillis()}_", ".jpg", picturesDir)
        latestTmpUri = FileProvider.getUriForFile(
            activity,
            "${activity.applicationContext.packageName}.fileprovider",
            tmpFile
        )
        latestTmpUri?.let { takePictureLauncher.launch(it) }
    }
    
    fun handleCameraResult(isSuccess: Boolean) {
        if (isSuccess) {
            latestTmpUri?.let { uri -> uploadImage(uri) }
        }
    }
    
    fun handleGalleryResult(uri: Uri?) {
        uri?.let { uploadImage(it) }
    }
    
    fun handleCameraPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(activity, "Kamera-Berechtigung verweigert", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun uploadImage(uri: Uri) {
        onProgressVisibilityChanged(true)
        
        CoroutineScope(Dispatchers.Main).launch {
            val compressedFile = ImageUtils.compressImage(activity, uri)
            
            if (compressedFile == null) {
                onProgressVisibilityChanged(false)
                Toast.makeText(activity, "Fehler beim Komprimieren des Bildes", Toast.LENGTH_SHORT).show()
                return@launch
            }
            
            com.example.we2026_5.util.StorageUploadManager.uploadImage(
                context = activity,
                imageFile = compressedFile,
                customerId = customerId,
                onSuccess = { downloadUrl ->
                    addPhotoUrlToCustomer(downloadUrl)
                    onProgressVisibilityChanged(false)
                    Toast.makeText(activity, "Bild erfolgreich hochgeladen", Toast.LENGTH_SHORT).show()
                },
                onError = { exception ->
                    onProgressVisibilityChanged(false)
                    val isOffline = !isNetworkAvailable()
                    if (isOffline) {
                        Toast.makeText(activity, "Bild wird hochgeladen, sobald Internet verfügbar ist", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(activity, "Upload fehlgeschlagen: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        return capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
               capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    private fun addPhotoUrlToCustomer(url: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val customer = repository.getCustomerById(customerId)
            if (customer != null) {
                val updatedUrls = customer.fotoUrls.toMutableList()
                if (!updatedUrls.contains(url)) {
                    updatedUrls.add(url)
                    val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                        operation = { 
                            repository.updateCustomer(customerId, mapOf("fotoUrls" to updatedUrls))
                        },
                        context = activity,
                        errorMessage = "Fehler beim Speichern der Foto-URL.",
                        maxRetries = 3
                    )
                    
                    if (success == true) {
                        Toast.makeText(activity, "Foto hinzugefügt", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    
    fun showImageInDialog(url: String) {
        val builder = AlertDialog.Builder(activity)
        val inflater = LayoutInflater.from(activity)
        val dialogView = inflater.inflate(R.layout.dialog_fullscreen_image, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.fullscreen_image)
        
        Glide.with(activity)
            .load(url)
            .into(imageView)
        
        builder.setView(dialogView)
        builder.create().show()
    }
}
