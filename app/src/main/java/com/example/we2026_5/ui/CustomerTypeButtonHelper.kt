package com.example.we2026_5.ui

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.google.android.material.button.MaterialButton

/**
 * Helper-Klasse für das Setup von Kunden-Typ-Buttons (G/P/L).
 * Zentralisiert die Logik für die Anzeige von Gewerblich/Privat/Liste-Buttons.
 */
object CustomerTypeButtonHelper {
    
    /**
     * Setzt den Kunden-Typ-Button basierend auf der Kunden-Art.
     * 
     * @param button Der MaterialButton, der gesetzt werden soll
     * @param kundenArt Die Kunden-Art ("Gewerblich", "Privat", "Liste" oder andere)
     * @param context Der Context für Farb-Ressourcen
     */
    fun setupButton(button: MaterialButton, kundenArt: String, context: Context) {
        when (kundenArt) {
            "Gewerblich" -> {
                button.text = "G"
                button.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.primary_blue)
                    )
                )
                button.visibility = View.VISIBLE
            }
            "Privat" -> {
                button.text = "P"
                button.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.accent_orange)
                    )
                )
                button.visibility = View.VISIBLE
            }
            "Liste" -> {
                button.text = "L"
                button.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(context, R.color.accent_brown)
                    )
                )
                button.visibility = View.VISIBLE
            }
            else -> {
                button.visibility = View.GONE
            }
        }
    }
    
    /**
     * Setzt den Kunden-Typ-Button basierend auf einem Customer-Objekt.
     * 
     * @param button Der MaterialButton, der gesetzt werden soll
     * @param customer Das Customer-Objekt
     * @param context Der Context für Farb-Ressourcen
     */
    fun setupButton(button: MaterialButton, customer: Customer, context: Context) {
        setupButton(button, customer.kundenArt, context)
    }
}
