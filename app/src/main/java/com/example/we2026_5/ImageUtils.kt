package com.example.we2026_5

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
        var inputStream: InputStream? = null
        var originalBitmap: Bitmap? = null
        var outputStream: FileOutputStream? = null
        
        try {
            // Uri öffnen
            inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                android.util.Log.e("ImageUtils", "Could not open input stream for URI: $uri")
                return@withContext null
            }
            
            // Bitmap dekodieren mit Optionen für bessere Fehlerbehandlung
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = 1
            }
            
            originalBitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()
            inputStream = null
            
            if (originalBitmap == null) {
                android.util.Log.e("ImageUtils", "Could not decode bitmap from URI: $uri")
                return@withContext null
            }
            
            // Prüfe ob Bitmap recycelt wurde
            if (originalBitmap.isRecycled) {
                android.util.Log.e("ImageUtils", "Original bitmap is already recycled")
                return@withContext null
            }
            
            // Skalierung berechnen
            val width = originalBitmap.width
            val height = originalBitmap.height
            val needsScaling = width > maxWidth
            val scale = if (needsScaling) maxWidth.toFloat() / width else 1.0f
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            
            // Komprimiertes File erstellen (vor der Bitmap-Verarbeitung)
            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: context.cacheDir
            
            // Stelle sicher, dass Verzeichnis existiert
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            val outputFile = File.createTempFile("compressed_${System.currentTimeMillis()}_", ".jpg", outputDir)
            outputStream = FileOutputStream(outputFile)
            
            // Bitmap für Komprimierung vorbereiten
            val bitmapToCompress: Bitmap = if (needsScaling) {
                // Skalierung nötig - erstelle skalierte Kopie
                try {
                    val scaled = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                    if (scaled == null || scaled.isRecycled) {
                        android.util.Log.e("ImageUtils", "Failed to create scaled bitmap")
                        outputStream?.close()
                        outputFile.delete()
                        if (originalBitmap != null && !originalBitmap.isRecycled) {
                            originalBitmap.recycle()
                        }
                        return@withContext null
                    }
                    // Original recyclen (skalierte Kopie ist unabhängig)
                    if (!originalBitmap.isRecycled) {
                        originalBitmap.recycle()
                    }
                    originalBitmap = null
                    scaled
                } catch (e: Exception) {
                    android.util.Log.e("ImageUtils", "Error creating scaled bitmap", e)
                    outputStream?.close()
                    outputFile.delete()
                    if (originalBitmap != null && !originalBitmap.isRecycled) {
                        originalBitmap.recycle()
                    }
                    return@withContext null
                }
            } else {
                // Keine Skalierung nötig - verwende Original direkt
                originalBitmap
            }
            
            // Prüfe ob Bitmap für Komprimierung gültig ist
            if (bitmapToCompress.isRecycled) {
                android.util.Log.e("ImageUtils", "Bitmap to compress is already recycled")
                outputStream?.close()
                outputFile.delete()
                if (bitmapToCompress != originalBitmap && !bitmapToCompress.isRecycled) {
                    bitmapToCompress.recycle()
                }
                if (originalBitmap != null && !originalBitmap.isRecycled) {
                    originalBitmap.recycle()
                }
                return@withContext null
            }
            
            // Komprimieren
            try {
                val success = bitmapToCompress.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
                if (!success) {
                    android.util.Log.e("ImageUtils", "Failed to compress bitmap")
                    outputStream?.close()
                    outputFile.delete()
                    if (bitmapToCompress != originalBitmap && !bitmapToCompress.isRecycled) {
                        bitmapToCompress.recycle()
                    }
                    if (originalBitmap != null && !originalBitmap.isRecycled) {
                        originalBitmap.recycle()
                    }
                    return@withContext null
                }
                outputStream?.flush()
                outputStream?.close()
                outputStream = null
            } catch (e: Exception) {
                android.util.Log.e("ImageUtils", "Error during compression", e)
                outputStream?.close()
                outputFile.delete()
                if (bitmapToCompress != originalBitmap && !bitmapToCompress.isRecycled) {
                    bitmapToCompress.recycle()
                }
                if (originalBitmap != null && !originalBitmap.isRecycled) {
                    originalBitmap.recycle()
                }
                return@withContext null
            }
            
            // Bitmap recyclen (nur wenn es eine skalierte Kopie war)
            if (bitmapToCompress != originalBitmap && !bitmapToCompress.isRecycled) {
                bitmapToCompress.recycle()
            } else if (originalBitmap != null && !originalBitmap.isRecycled) {
                originalBitmap.recycle()
            }
            originalBitmap = null
            
            android.util.Log.d("ImageUtils", "Image compressed successfully: ${outputFile.absolutePath}")
            outputFile
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "Error compressing image", e)
            e.printStackTrace()
            
            // Cleanup bei Fehler
            inputStream?.close()
            if (originalBitmap != null && !originalBitmap.isRecycled) {
                originalBitmap.recycle()
            }
            outputStream?.close()
            
            null
        }
    }

    /**
     * Erstellt eine Thumbnail-Datei aus einer Bilddatei (Prio 3 PLAN_PERFORMANCE_OFFLINE).
     * @param source Quelldatei
     * @param maxSizePx Maximale Kantenlänge in Pixel (Standard 300)
     * @return Thumbnail-File oder null bei Fehler
     */
    fun createThumbnailFile(source: File, maxSizePx: Int = 300): File? {
        var bitmap: Bitmap? = null
        var out: FileOutputStream? = null
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = false }
            bitmap = BitmapFactory.decodeFile(source.absolutePath, opts) ?: return null
            if (bitmap.isRecycled) return null
            val w = bitmap.width
            val h = bitmap.height
            val scale = if (w <= maxSizePx && h <= maxSizePx) 1f
                else maxSizePx.toFloat() / maxOf(w, h)
            val nw = (w * scale).toInt().coerceAtLeast(1)
            val nh = (h * scale).toInt().coerceAtLeast(1)
            val thumb = Bitmap.createScaledBitmap(bitmap, nw, nh, true)
            if (thumb != bitmap) bitmap.recycle()
            bitmap = null
            val dir = source.parentFile ?: source.absoluteFile.parentFile?.let { File(it) } ?: return null
            val thumbFile = File.createTempFile("thumb_${System.currentTimeMillis()}_", ".jpg", dir)
            out = FileOutputStream(thumbFile)
            if (!thumb.compress(Bitmap.CompressFormat.JPEG, 80, out)) {
                out.close()
                thumbFile.delete()
                if (!thumb.isRecycled) thumb.recycle()
                return null
            }
            out.flush()
            out.close()
            if (!thumb.isRecycled) thumb.recycle()
            thumbFile
        } catch (e: Exception) {
            android.util.Log.e("ImageUtils", "createThumbnailFile failed", e)
            out?.close()
            if (bitmap != null && !bitmap.isRecycled) bitmap.recycle()
            null
        }
    }
}
