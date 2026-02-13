package com.example.we2026_5.util

import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * Zentrale Extension-Funktionen für die gesamte App.
 */

// ── Intent Extensions ──

/**
 * Liest einen String-Extra aus dem Intent.
 * Wirft eine [IllegalArgumentException] wenn der Key fehlt oder leer ist.
 *
 * Nutzung:
 * ```
 * val customerId = intent.requireStringExtra("CUSTOMER_ID")
 * ```
 */
fun Intent.requireStringExtra(key: String): String {
    return getStringExtra(key)
        ?: throw IllegalArgumentException("Intent-Extra '$key' fehlt oder ist null")
}

/**
 * Liest einen String-Extra aus dem Intent, gibt Fallback zurück wenn nicht vorhanden.
 */
fun Intent.getStringExtraOrDefault(key: String, default: String = ""): String {
    return getStringExtra(key) ?: default
}

// ── Context / Toast Extensions ──

/**
 * Zeigt einen kurzen Toast.
 *
 * Nutzung:
 * ```
 * showToast("Gespeichert")
 * showToast(R.string.toast_saved)
 * ```
 */
fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

/**
 * Zeigt einen Toast mit String-Resource.
 */
fun Context.showToast(resId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, getString(resId), duration).show()
}
