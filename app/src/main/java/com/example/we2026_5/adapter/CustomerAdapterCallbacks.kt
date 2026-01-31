package com.example.we2026_5.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.we2026_5.Customer

/**
 * Helper-Klasse für Callback-Handler in CustomerAdapter.
 * Liest Callbacks aus der aktuellen CustomerAdapterCallbacksConfig.
 */
class CustomerAdapterCallbacks(
    private val context: Context,
    private val getConfig: () -> CustomerAdapterCallbacksConfig
) {

    /**
     * Behandelt Abholung-Erledigung
     */
    fun handleAbholung(customer: Customer) {
        if (customer.abholungErfolgt) return
        getConfig().onAbholung?.invoke(customer)
    }

    /**
     * Behandelt Auslieferung-Erledigung
     */
    fun handleAuslieferung(customer: Customer) {
        if (customer.auslieferungErfolgt) return
        getConfig().onAuslieferung?.invoke(customer)
    }

    /**
     * Behandelt Keine Wäsche (KW): A+KW = erledigt Abholungstag, L+KW = erledigt Auslieferungstag
     */
    fun handleKw(customer: Customer) {
        getConfig().onKw?.invoke(customer)
    }
    
    /**
     * Startet Navigation zu einer Adresse
     */
    fun startNavigation(adresse: String) {
        val gmmIntentUri = Uri.parse("google.navigation:q=$adresse")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(com.example.we2026_5.R.string.error_maps_not_installed), Toast.LENGTH_SHORT).show()
        }
    }
}
