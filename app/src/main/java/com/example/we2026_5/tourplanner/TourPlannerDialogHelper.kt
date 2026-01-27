package com.example.we2026_5.tourplanner

import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.Customer
import com.example.we2026_5.databinding.DialogTerminDetailBinding
import java.util.Calendar

/**
 * Helper-Klasse für Dialog-Funktionen der TourPlannerActivity
 */
class TourPlannerDialogHelper(
    private val activity: AppCompatActivity,
    private val onKundeAnzeigen: ((Customer) -> Unit)?,
    private val onTerminLoeschen: ((Customer, Long) -> Unit)?
) {
    
    fun showTerminDetailDialog(customer: Customer, terminDatum: Long) {
        val dialogView = android.view.LayoutInflater.from(activity).inflate(
            com.example.we2026_5.R.layout.dialog_termin_detail,
            null
        )
        val binding = DialogTerminDetailBinding.bind(dialogView)
        
        // Kundeninfos anzeigen
        binding.tvKundenname.text = customer.name
        binding.tvAdresse.text = customer.adresse
        binding.tvTelefon.text = customer.telefon
        binding.tvNotizen.text = customer.notizen.ifEmpty { "Keine Notizen" }
        
        // Termin-Datum formatieren
        val dateStr = com.example.we2026_5.util.DateFormatter.formatDate(terminDatum)
        binding.tvTerminDatum.text = dateStr
        
        // Kunden-Typ Button (G/P/L) anzeigen
        com.example.we2026_5.ui.CustomerTypeButtonHelper.setupButton(binding.btnKundenTyp, customer, activity)
        
        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()
        
        binding.btnKundeAnzeigen.setOnClickListener {
            onKundeAnzeigen?.invoke(customer)
            dialog.dismiss()
        }
        
        binding.btnTerminLoeschen.setOnClickListener {
            com.example.we2026_5.util.DialogBaseHelper.showConfirmationDialog(
                context = activity,
                title = "Termin löschen",
                message = "Möchten Sie diesen Termin wirklich löschen?",
                positiveButtonText = "Löschen",
                onPositive = {
                    onTerminLoeschen?.invoke(customer, terminDatum)
                    dialog.dismiss()
                }
            )
        }
        
        dialog.show()
    }
}
