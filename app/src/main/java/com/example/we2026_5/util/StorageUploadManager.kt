package com.example.we2026_5.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.we2026_5.work.ImageUploadWorker
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

object StorageUploadManager {
    
    /**
     * Lädt ein Bild hoch - versucht direkt, falls offline wird es in die Queue eingereiht
     */
    suspend fun uploadImage(
        context: Context,
        imageFile: File,
        customerId: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val storagePath = "customer_photos/$customerId/${System.currentTimeMillis()}.jpg"
            val storage = FirebaseStorage.getInstance()
            val storageRef = storage.reference.child(storagePath)
            val uri = Uri.fromFile(imageFile)

            // Versuche direkten Upload
            try {
                val uploadTask = storageRef.putFile(uri).await()
                val downloadUrl = storageRef.downloadUrl.await()
                
                Log.d("StorageUploadManager", "Direct upload successful: $downloadUrl")
                imageFile.delete()
                onSuccess(downloadUrl.toString())
                return
            } catch (e: Exception) {
                // Upload fehlgeschlagen - in Queue einreihen
                Log.d("StorageUploadManager", "Direct upload failed, queuing: ${e.message}")
            }

            // Offline: In WorkManager-Queue einreihen
            val inputData = Data.Builder()
                .putString(ImageUploadWorker.KEY_IMAGE_PATH, imageFile.absolutePath)
                .putString(ImageUploadWorker.KEY_STORAGE_PATH, storagePath)
                .putString(ImageUploadWorker.KEY_CUSTOMER_ID, customerId)
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val uploadWork = OneTimeWorkRequestBuilder<ImageUploadWorker>()
                .setConstraints(constraints)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context).enqueue(uploadWork)
            
            Log.d("StorageUploadManager", "Upload queued for later execution")
            
            // Für Offline-Uploads: Datei NICHT löschen, wird vom Worker gelöscht
            // onSuccess wird später aufgerufen, wenn Upload erfolgreich
            
        } catch (e: Exception) {
            Log.e("StorageUploadManager", "Error in uploadImage", e)
            onError(e)
        }
    }
}
