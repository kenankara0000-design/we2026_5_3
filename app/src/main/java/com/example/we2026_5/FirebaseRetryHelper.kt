package com.example.we2026_5

import android.widget.Toast
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

object FirebaseRetryHelper {
    
    /**
     * Führt eine Firebase-Operation mit Retry-Logik aus
     * @param operation Die Firebase-Operation
     * @param maxRetries Maximale Anzahl Versuche (Standard: 3)
     * @param delayMs Verzögerung zwischen Versuchen in Millisekunden (Standard: 1000)
     * @return Task-Ergebnis oder null bei Fehler
     */
    suspend fun <T> executeWithRetry(
        operation: () -> Task<T>,
        maxRetries: Int = 3,
        delayMs: Long = 1000
    ): T? {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                val result = operation().await()
                return result
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    // Warten vor erneutem Versuch
                    kotlinx.coroutines.delay(delayMs * (attempt + 1)) // Exponential backoff
                }
            }
        }
        
        // Alle Versuche fehlgeschlagen
        lastException?.printStackTrace()
        return null
    }
    
    /**
     * Führt eine Firebase-Operation mit Retry-Logik aus und zeigt Toast bei Fehler
     */
    suspend fun <T> executeWithRetryAndToast(
        operation: () -> Task<T>,
        context: android.content.Context,
        errorMessage: String = "Fehler beim Speichern. Bitte erneut versuchen.",
        maxRetries: Int = 3
    ): T? {
        val result = executeWithRetry(operation, maxRetries)
        if (result == null) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
        return result
    }
    
    /**
     * Führt eine suspend-Funktion mit Retry-Logik aus und zeigt Toast bei Fehler
     */
    suspend fun <T> executeSuspendWithRetryAndToast(
        operation: suspend () -> T,
        context: android.content.Context,
        errorMessage: String = "Fehler beim Speichern. Bitte erneut versuchen.",
        maxRetries: Int = 3,
        delayMs: Long = 1000
    ): T? {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                val result = operation()
                return result
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    // Warten vor erneutem Versuch
                    kotlinx.coroutines.delay(delayMs * (attempt + 1)) // Exponential backoff
                }
            }
        }
        
        // Alle Versuche fehlgeschlagen
        lastException?.printStackTrace()
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
        return null
    }
}
