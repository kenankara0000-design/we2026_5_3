package com.example.we2026_5

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {
    
    /**
     * Komprimiert ein Bild von einem Uri
     * @param context Context
     * @param uri Original-Uri
     * @param maxWidth Maximale Breite in Pixel (Standard: 1920)
     * @param quality Kompressionsqualität 0-100 (Standard: 85)
     * @return Komprimiertes File oder null bei Fehler
     */
    suspend fun compressImage(
        context: Context,
        uri: Uri,
        maxWidth: Int = 1920,
        quality: Int = 85
    ): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap == null) return@withContext null
            
            // Skalierung berechnen
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > maxWidth) maxWidth.toFloat() / width else 1.0f
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            
            // Bild skalieren
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            originalBitmap.recycle()
            
            // Komprimiertes File erstellen
            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: context.cacheDir
            val outputFile = File.createTempFile("compressed_${System.currentTimeMillis()}_", ".jpg", outputDir)
            
            val outputStream = FileOutputStream(outputFile)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream.flush()
            outputStream.close()
            scaledBitmap.recycle()
            
            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
