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
        // Textfarbe immer weiß setzen
        button.setTextColor(android.graphics.Color.WHITE)
        // BackgroundTint deaktivieren, damit Drawable sichtbar ist
        button.backgroundTintList = null
        
        when (kundenArt) {
            "Gewerblich" -> {
                button.text = context.getString(R.string.label_type_g)
                // Kräftigeres, glänzenderes Blau mit Gradient-Hintergrund
                button.setBackgroundResource(R.drawable.button_gewerblich_glossy)
                button.visibility = View.VISIBLE
            }
            "Privat" -> {
                button.text = context.getString(R.string.label_type_p_letter)
                // Kräftigeres, glänzenderes Orange mit Gradient-Hintergrund
                button.setBackgroundResource(R.drawable.button_privat_glossy)
                button.visibility = View.VISIBLE
            }
            "Liste", "Tour" -> {
                button.text = context.getString(R.string.label_type_t_letter)
                // Kräftigeres, glänzenderes Braun mit Gradient-Hintergrund
                button.setBackgroundResource(R.drawable.button_liste_glossy)
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
