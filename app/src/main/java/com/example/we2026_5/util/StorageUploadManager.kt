package com.example.we2026_5.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.we2026_5.ImageUtils
import com.example.we2026_5.work.ImageUploadWorker
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

/** Ergebnis eines Foto-Uploads (Prio 3: Thumbnail optional). */
data class PhotoUploadResult(val fullUrl: String, val thumbUrl: String?)

object StorageUploadManager {

    /**
     * Lädt ein Bild hoch (Vollbild + Thumbnail für Listen); bei Offline Queue.
     * onSuccess: fullUrl, thumbUrl (null wenn Thumbnail-Erstellung fehlschlug).
     */
    suspend fun uploadImage(
        context: Context,
        imageFile: File,
        customerId: String,
        onSuccess: (fullUrl: String, thumbUrl: String?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val ts = System.currentTimeMillis()
            val storagePath = "customer_photos/$customerId/$ts.jpg"
            val thumbPath = "customer_photos/$customerId/thumb_$ts.jpg"
            val storage = FirebaseStorage.getInstance()
            val uri = Uri.fromFile(imageFile)

            try {
                // Thumbnail erstellen und hochladen (Prio 3)
                var thumbUrl: String? = null
                val thumbFile = ImageUtils.createThumbnailFile(imageFile, 300)
                if (thumbFile != null) {
                    try {
                        val thumbRef = storage.reference.child(thumbPath)
                        thumbRef.putFile(Uri.fromFile(thumbFile)).await()
                        thumbUrl = thumbRef.downloadUrl.await().toString()
                    } finally {
                        thumbFile.delete()
                    }
                }
                // Vollbild hochladen
                val storageRef = storage.reference.child(storagePath)
                storageRef.putFile(uri).await()
                val fullUrl = storageRef.downloadUrl.await().toString()
                imageFile.delete()
                Log.d("StorageUploadManager", "Direct upload successful; thumb=${thumbUrl != null}")
                onSuccess(fullUrl, thumbUrl)
                return
            } catch (e: Exception) {
                Log.d("StorageUploadManager", "Direct upload failed, queuing: ${e.message}")
            }

            // Offline: in Queue (Worker lädt Vollbild + Thumbnail)
            val inputData = Data.Builder()
                .putString(ImageUploadWorker.KEY_IMAGE_PATH, imageFile.absolutePath)
                .putString(ImageUploadWorker.KEY_STORAGE_PATH, storagePath)
                .putString(ImageUploadWorker.KEY_CUSTOMER_ID, customerId)
                .build()
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val uploadWork = OneTimeWorkRequestBuilder<ImageUploadWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()
            WorkManager.getInstance(context).enqueue(uploadWork)
            Log.d("StorageUploadManager", "Upload queued for later execution")
        } catch (e: Exception) {
            Log.e("StorageUploadManager", "Error in uploadImage", e)
            onError(e)
        }
    }
}
