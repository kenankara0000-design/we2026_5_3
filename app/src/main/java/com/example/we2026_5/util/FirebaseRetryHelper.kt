package com.example.we2026_5.util

import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Zentraler Helper für Firebase-Operationen mit Retry-Logik und Timeout.
 * 
 * Vorteile:
 * - Einheitliches Fehler-Handling (keine `try/catch` Duplikate in Repositories)
 * - Standardisierte Timeout-Handling (Offline-Support)
 * - Retry-Logik für transiente Fehler
 * - Mapping auf `Result<T>` für konsistente Fehlerbehandlung im ViewModel
 */
object FirebaseRetryHelper {

    /**
     * Standard-Timeout für Firebase-Operationen (ms).
     * Bei Offline-Nutzung speichert Realtime DB lokal und synchronisiert später.
     */
    private const val DEFAULT_TIMEOUT_MS = 5000L

    /**
     * Standard-Anzahl Retry-Versuche bei transienten Fehlern.
     */
    private const val DEFAULT_RETRY_COUNT = 2

    /**
     * Delay zwischen Retry-Versuchen (ms).
     */
    private const val RETRY_DELAY_MS = 500L

    /**
     * Führt eine Firebase-Task mit Timeout und Retry aus.
     * 
     * @param timeoutMs Timeout in Millisekunden (default: 5000ms)
     * @param retryCount Anzahl der Retry-Versuche bei transienten Fehlern (default: 2)
     * @param block Der auszuführende Firebase-Task
     * @return Result<T> mit Erfolg oder Fehler
     */
    suspend fun <T> executeWithRetry(
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        retryCount: Int = DEFAULT_RETRY_COUNT,
        block: suspend () -> T
    ): Result<T> {
        var lastException: Exception? = null
        
        for (attempt in 0..retryCount) {
            try {
                val result = withTimeout(timeoutMs) {
                    block()
                }
                return Result.Success(result)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // Timeout ist bei Offline-Nutzung OK (Realtime DB puffert lokal)
                // Bei write-Operationen können wir davon ausgehen, dass sie lokal gespeichert wurden
                lastException = e
                if (attempt < retryCount) {
                    delay(RETRY_DELAY_MS)
                }
            } catch (e: Exception) {
                lastException = e
                // Nur bei transienten Fehlern erneut versuchen
                if (attempt < retryCount && isTransientError(e)) {
                    delay(RETRY_DELAY_MS)
                } else {
                    break
                }
            }
        }
        
        return Result.Error(
            message = mapErrorMessage(lastException),
            throwable = lastException
        )
    }

    /**
     * Führt eine Firebase-setValue-Operation mit Retry aus.
     * 
     * @param ref Die DatabaseReference
     * @param value Der zu schreibende Wert
     * @param timeoutMs Timeout in Millisekunden
     * @param retryCount Anzahl der Retry-Versuche
     * @return Result<Boolean> (true bei Erfolg oder Offline-Pufferung)
     */
    suspend fun setValueWithRetry(
        ref: DatabaseReference,
        value: Any?,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        retryCount: Int = DEFAULT_RETRY_COUNT
    ): Result<Boolean> {
        return executeWithRetry(timeoutMs, retryCount) {
            ref.setValue(value).await()
            true
        }
    }

    /**
     * Führt eine Firebase-updateChildren-Operation mit Retry aus.
     * 
     * @param ref Die DatabaseReference
     * @param updates Map mit Updates
     * @param timeoutMs Timeout in Millisekunden
     * @param retryCount Anzahl der Retry-Versuche
     * @return Result<Boolean> (true bei Erfolg oder Offline-Pufferung)
     */
    suspend fun updateChildrenWithRetry(
        ref: DatabaseReference,
        updates: Map<String, Any?>,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        retryCount: Int = DEFAULT_RETRY_COUNT
    ): Result<Boolean> {
        return executeWithRetry(timeoutMs, retryCount) {
            ref.updateChildren(updates).await()
            true
        }
    }

    /**
     * Führt eine Firebase-removeValue-Operation mit Retry aus.
     * 
     * @param ref Die DatabaseReference
     * @param timeoutMs Timeout in Millisekunden
     * @param retryCount Anzahl der Retry-Versuche
     * @return Result<Boolean> (true bei Erfolg oder Offline-Pufferung)
     */
    suspend fun removeValueWithRetry(
        ref: DatabaseReference,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        retryCount: Int = DEFAULT_RETRY_COUNT
    ): Result<Boolean> {
        return executeWithRetry(timeoutMs, retryCount) {
            ref.removeValue().await()
            true
        }
    }

    /**
     * Prüft, ob ein Fehler transient ist (d.h. Retry sinnvoll).
     * 
     * Transiente Fehler:
     * - Netzwerk-Timeouts
     * - Firebase-Unavailable
     * - Throttling/Rate-Limiting
     * 
     * Nicht transient:
     * - Permission-Errors
     * - Invalid-Data
     * - Not-Found (bei Delete-Operationen OK)
     */
    private fun isTransientError(e: Exception): Boolean {
        val message = e.message?.lowercase() ?: ""
        return when {
            e is kotlinx.coroutines.TimeoutCancellationException -> true
            message.contains("network") -> true
            message.contains("unavailable") -> true
            message.contains("timeout") -> true
            message.contains("throttle") -> true
            else -> false
        }
    }

    /**
     * Mappt Firebase-Exceptions auf benutzerfreundliche Fehlermeldungen.
     * 
     * Optional: Kann mit AppErrorMapper kombiniert werden für i18n.
     */
    private fun mapErrorMessage(e: Exception?): String {
        if (e == null) return "Unbekannter Fehler"
        
        return when {
            e is kotlinx.coroutines.TimeoutCancellationException -> 
                "Zeitüberschreitung – Änderung wird synchronisiert, sobald Verbindung besteht"
            e.message?.contains("permission", ignoreCase = true) == true ->
                "Keine Berechtigung für diese Aktion"
            e.message?.contains("network", ignoreCase = true) == true ->
                "Netzwerkfehler – Bitte Verbindung prüfen"
            else -> e.message ?: "Fehler bei Firebase-Operation"
        }
    }

    /**
     * Mappt Firebase DatabaseError auf benutzerfreundliche Nachricht.
     */
    fun mapDatabaseError(error: DatabaseError): String {
        return when (error.code) {
            DatabaseError.PERMISSION_DENIED -> "Keine Berechtigung"
            DatabaseError.UNAVAILABLE -> "Dienst nicht verfügbar"
            DatabaseError.NETWORK_ERROR -> "Netzwerkfehler"
            DatabaseError.DISCONNECTED -> "Verbindung unterbrochen"
            else -> error.message
        }
    }
}
