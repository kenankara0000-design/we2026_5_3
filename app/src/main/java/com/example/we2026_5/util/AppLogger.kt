package com.example.we2026_5.util

import android.util.Log

/**
 * Zentraler Logger für die App.
 * 
 * Vorteile:
 * - Einheitliches Logging im gesamten Projekt
 * - Zentrale Kontrolle über Log-Level (z.B. Produktion vs. Debug)
 * - Einfaches Deaktivieren von Logs in Release-Builds
 * - Strukturierte Log-Tags (automatisch aus Klasse)
 * - Optional: Crash-Reporting-Integration (Firebase Crashlytics)
 * 
 * Verwendung:
 * ```
 * AppLogger.e("CustomerRepository", "Error saving customer", exception)
 * AppLogger.d("TourPlanner", "Updates: $updates")
 * AppLogger.w("SevDeskImport", "Token expired")
 * ```
 */
object AppLogger {
    
    /**
     * Logging aktiviert (true für Debug-Builds, false für Release).
     * In der Release-Variante sollte dies auf `false` gesetzt werden.
     */
    private val ENABLED = com.example.we2026_5.BuildConfig.DEBUG
    
    /**
     * App-weiter Log-Tag Prefix.
     */
    private const val TAG_PREFIX = "WE2026"
    
    /**
     * Error-Log (höchste Priorität für Fehler).
     * 
     * @param tag Kontext/Klasse (z.B. "CustomerRepository")
     * @param message Fehlernachricht
     * @param throwable Optional: Exception
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (!ENABLED) return
        val fullTag = "$TAG_PREFIX/$tag"
        if (throwable != null) {
            Log.e(fullTag, message, throwable)
        } else {
            Log.e(fullTag, message)
        }
    }
    
    /**
     * Warning-Log (mittlere Priorität für Warnungen).
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (!ENABLED) return
        val fullTag = "$TAG_PREFIX/$tag"
        if (throwable != null) {
            Log.w(fullTag, message, throwable)
        } else {
            Log.w(fullTag, message)
        }
    }
    
    /**
     * Info-Log (für wichtige Info-Events).
     */
    fun i(tag: String, message: String) {
        if (!ENABLED) return
        Log.i("$TAG_PREFIX/$tag", message)
    }
    
    /**
     * Debug-Log (niedrige Priorität für Debug-Infos).
     * In Release-Builds werden diese Logs automatisch weggelassen.
     */
    fun d(tag: String, message: String) {
        if (!ENABLED) return
        Log.d("$TAG_PREFIX/$tag", message)
    }
    
    /**
     * Verbose-Log (niedrigste Priorität für sehr detaillierte Logs).
     */
    fun v(tag: String, message: String) {
        if (!ENABLED) return
        Log.v("$TAG_PREFIX/$tag", message)
    }
    
    /**
     * Loggt eine Exception mit minimaler Ausgabe (für catch-Blöcke).
     * Äquivalent zu `e.printStackTrace()`, aber strukturiert.
     */
    fun logException(tag: String, throwable: Throwable, context: String = "Exception occurred") {
        if (!ENABLED) return
        e(tag, "$context: ${throwable.message}", throwable)
    }
}
