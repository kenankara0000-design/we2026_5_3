package com.example.we2026_5.adapter

import android.content.Context
import android.widget.Toast
import com.example.we2026_5.Customer
import com.example.we2026_5.util.DialogBaseHelper
import java.util.Calendar

/**
 * Helper-Klasse für Dialog-Funktionen des CustomerAdapter
 */
class CustomerDialogHelper(
    private val context: Context,
    private val onVerschieben: ((Customer, Long, Boolean) -> Unit)?,
    private val onUrlaub: ((Customer, Long, Long) -> Unit)?,
    private val onRueckgaengig: ((Customer) -> Unit)?,
    private val onButtonStateReset: ((String) -> Unit)? // customerId -> Unit
) {
    
    fun showVerschiebenDialog(customer: Customer) {
        DialogBaseHelper.showDatePickerDialog(
            context = context,
            onDateSelected = { newDate ->
                // Dialog: Nur diesen Termin oder alle restlichen Termine verschieben?
                // Verwende AlertDialog direkt, da wir einen Neutral-Button benötigen
                androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle("Termin verschieben")
                    .setMessage("Wie möchten Sie vorgehen?")
                    .setPositiveButton("Nur diesen Termin") { _, _ ->
                        onVerschieben?.invoke(customer, newDate, false)
                        onButtonStateReset?.invoke(customer.id)
                    }
                    .setNeutralButton("Alle zukünftigen Termine") { _, _ ->
                        onVerschieben?.invoke(customer, newDate, true)
                        onButtonStateReset?.invoke(customer.id)
                    }
                    .setNegativeButton("Abbrechen") { _, _ ->
                        onButtonStateReset?.invoke(customer.id)
                    }
                    .show()
            },
            onCancel = {
                onButtonStateReset?.invoke(customer.id)
            }
        )
    }

    fun showUrlaubDialog(customer: Customer) {
        DialogBaseHelper.showDatePickerDialog(
            context = context,
            title = "Urlaub von",
            onDateSelected = { urlaubVon ->
                DialogBaseHelper.showDatePickerDialog(
                    context = context,
                    title = "Urlaub bis",
                    onDateSelected = { urlaubBis ->
                        val pickedBis = Calendar.getInstance().apply {
                            timeInMillis = urlaubBis
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                        }
                        if (pickedBis.timeInMillis >= urlaubVon) {
                            onUrlaub?.invoke(customer, urlaubVon, pickedBis.timeInMillis)
                            onButtonStateReset?.invoke(customer.id)
                        } else {
                            Toast.makeText(context, context.getString(com.example.we2026_5.R.string.toast_enddatum_nach_startdatum), Toast.LENGTH_SHORT).show()
                            onButtonStateReset?.invoke(customer.id)
                        }
                    },
                    onCancel = {
                        onButtonStateReset?.invoke(customer.id)
                    }
                )
            },
            onCancel = {
                onButtonStateReset?.invoke(customer.id)
            }
        )
    }

    fun showRueckgaengigDialog(customer: Customer) {
        if (!customer.abholungErfolgt && !customer.auslieferungErfolgt) return
        
        DialogBaseHelper.showConfirmationDialog(
            context = context,
            title = "Rückgängig machen",
            message = "Möchten Sie die Erledigung wirklich rückgängig machen?",
            positiveButtonText = "Ja",
            onPositive = {
                onRueckgaengig?.invoke(customer)
            }
        )
    }
}
