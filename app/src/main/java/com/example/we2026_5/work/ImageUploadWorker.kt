package com.example.we2026_5.work

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.we2026_5.ImageUtils
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

            // Thumbnail erstellen und hochladen (Prio 3)
            var thumbUrl: String? = null
            val lastSlash = storagePath.lastIndexOf('/')
            val thumbPath = if (lastSlash >= 0)
                storagePath.substring(0, lastSlash + 1) + "thumb_" + storagePath.substring(lastSlash + 1)
            else
                "thumb_$storagePath"
            val thumbFile = ImageUtils.createThumbnailFile(file, 300)
            if (thumbFile != null) {
                try {
                    val thumbRef = storage.reference.child(thumbPath)
                    thumbRef.putFile(Uri.fromFile(thumbFile)).await()
                    thumbUrl = thumbRef.downloadUrl.await().toString()
                } finally {
                    thumbFile.delete()
                }
            }

            val storageRef = storage.reference.child(storagePath)
            storageRef.putFile(Uri.fromFile(file)).await()
            val fullUrl = storageRef.downloadUrl.await().toString()

            Log.d("ImageUploadWorker", "Upload successful; thumb=${thumbUrl != null}")

            val db = com.google.firebase.database.FirebaseDatabase.getInstance()
            val customerRef = db.reference.child("customers").child(customerId)
            val customerSnapshot = customerRef.get().await()
            val customer = customerSnapshot.getValue(com.example.we2026_5.Customer::class.java)

            if (customer != null) {
                val updatedUrls = customer.fotoUrls.toMutableList()
                val updatedThumbUrls = customer.fotoThumbUrls.toMutableList()
                if (!updatedUrls.contains(fullUrl)) {
                    updatedUrls.add(fullUrl)
                    updatedThumbUrls.add(thumbUrl ?: fullUrl)
                    customerRef.child("fotoUrls").setValue(updatedUrls).await()
                    customerRef.child("fotoThumbUrls").setValue(updatedThumbUrls).await()
                }
            }

            file.delete()
            Result.success()
        } catch (e: Exception) {
            Log.e("ImageUploadWorker", "Upload failed", e)
            Result.retry()
        }
    }

    companion object {
        const val KEY_IMAGE_PATH = "image_path"
        const val KEY_STORAGE_PATH = "storage_path"
        const val KEY_CUSTOMER_ID = "customer_id"
    }
}
