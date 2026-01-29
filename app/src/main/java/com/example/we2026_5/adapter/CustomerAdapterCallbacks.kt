package com.example.we2026_5.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.we2026_5.Customer
import com.example.we2026_5.SectionType

/**
 * Helper-Klasse für Callback-Handler in CustomerAdapter
 */
class CustomerAdapterCallbacks(
    private val context: Context,
    private val onAbholung: ((Customer) -> Unit)?,
    private val onAuslieferung: ((Customer) -> Unit)?,
    private val onKw: ((Customer) -> Unit)?,
    private val onSectionToggle: ((SectionType) -> Unit)?
) {

    /**
     * Behandelt Abholung-Erledigung
     */
    fun handleAbholung(customer: Customer) {
        if (customer.abholungErfolgt) return
        onAbholung?.invoke(customer)
    }

    /**
     * Behandelt Auslieferung-Erledigung
     */
    fun handleAuslieferung(customer: Customer) {
        if (customer.auslieferungErfolgt) return
        onAuslieferung?.invoke(customer)
    }

    /**
     * Behandelt Keine Wäsche (KW): A+KW = erledigt Abholungstag, L+KW = erledigt Auslieferungstag
     */
    fun handleKw(customer: Customer) {
        onKw?.invoke(customer)
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
            Toast.makeText(context, "Google Maps ist nicht installiert.", Toast.LENGTH_SHORT).show()
        }
    }
}
