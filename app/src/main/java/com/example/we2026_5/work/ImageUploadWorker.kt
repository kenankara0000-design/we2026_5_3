package com.example.we2026_5.work

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File

class ImageUploadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val imagePath = inputData.getString(KEY_IMAGE_PATH)
            val storagePath = inputData.getString(KEY_STORAGE_PATH)
            val customerId = inputData.getString(KEY_CUSTOMER_ID)

            if (imagePath == null || storagePath == null || customerId == null) {
                Log.e("ImageUploadWorker", "Missing required parameters")
                return Result.failure()
            }

            val file = File(imagePath)
            if (!file.exists()) {
                Log.e("ImageUploadWorker", "Image file does not exist: $imagePath")
                return Result.failure()
            }

            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference.child(storagePath)
            val uri = Uri.fromFile(file)

            // Upload
            storageRef.putFile(uri).await()
            
            // Download-URL abrufen
            val downloadUrl = storageRef.downloadUrl.await()
            val downloadUrlString = downloadUrl.toString()

            Log.d("ImageUploadWorker", "Upload successful: $downloadUrlString")

            // Download-URL zu Realtime Database hinzufügen
            val db = com.google.firebase.database.FirebaseDatabase.getInstance()
            val customerRef = db.reference.child("customers").child(customerId)
            
            // Aktuellen Kunden laden
            val customerSnapshot = customerRef.get().await()
            val customer = customerSnapshot.getValue(com.example.we2026_5.Customer::class.java)
            
            if (customer != null) {
                val updatedUrls = customer.fotoUrls.toMutableList()
                if (!updatedUrls.contains(downloadUrlString)) {
                    updatedUrls.add(downloadUrlString)
                    customerRef.child("fotoUrls").setValue(updatedUrls).await()
                }
            }

            Log.d("ImageUploadWorker", "Photo URL added to Realtime Database")

            // Temporäres File löschen
            file.delete()

            Result.success()
        } catch (e: Exception) {
            Log.e("ImageUploadWorker", "Upload failed", e)
            // Bei Fehler: Retry später
            Result.retry()
        }
    }

    companion object {
        const val KEY_IMAGE_PATH = "image_path"
        const val KEY_STORAGE_PATH = "storage_path"
        const val KEY_CUSTOMER_ID = "customer_id"
    }
}
