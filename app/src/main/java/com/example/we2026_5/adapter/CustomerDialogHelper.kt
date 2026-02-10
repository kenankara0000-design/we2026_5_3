package com.example.we2026_5.adapter

import android.content.Context
import android.widget.Toast
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.util.DialogBaseHelper
import java.util.Calendar

/**
 * Helper-Klasse für Verschieben-, Urlaub- und Rückgängig-Dialoge im TourPlanner.
 */
class CustomerDialogHelper(
    private val context: Context,
    private val onVerschieben: ((Customer, Long, Boolean, TerminTyp?) -> Unit)?,
    private val onUrlaub: ((Customer, Long, Long) -> Unit)?,
    private val onRueckgaengig: ((Customer) -> Unit)?,
    private val onButtonStateReset: ((String) -> Unit)? // customerId -> Unit
) {
    
    /**
     * @param terminTypForSingle Wenn A und L am gleichen Tag: vorher gewählter Typ (A oder L). Sonst null.
     */
    fun showVerschiebenDialog(customer: Customer, terminTypForSingle: TerminTyp? = null) {
        DialogBaseHelper.showDatePickerDialog(
            context = context,
            onDateSelected = { newDate ->
                androidx.appcompat.app.AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.dialog_verschieben_title))
                    .setMessage(context.getString(R.string.dialog_verschieben_message))
                    .setPositiveButton(context.getString(R.string.dialog_verschieben_single)) { _, _ ->
                        onVerschieben?.invoke(customer, newDate, false, terminTypForSingle)
                        onButtonStateReset?.invoke(customer.id)
                    }
                    .setNeutralButton(context.getString(R.string.dialog_verschieben_all)) { _, _ ->
                        onVerschieben?.invoke(customer, newDate, true, null)
                        onButtonStateReset?.invoke(customer.id)
                    }
                    .setNegativeButton(context.getString(R.string.btn_cancel)) { _, _ ->
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
            title = context.getString(R.string.dialog_urlaub_von),
            onDateSelected = { urlaubVon ->
                DialogBaseHelper.showDatePickerDialog(
                    context = context,
                    title = context.getString(R.string.dialog_urlaub_bis),
                    onDateSelected = { urlaubBis ->
                        val pickedBis = com.example.we2026_5.util.AppTimeZone.newCalendar().apply {
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
            title = context.getString(R.string.dialog_rueckgaengig_title),
            message = context.getString(R.string.dialog_rueckgaengig_message),
            positiveButtonText = context.getString(R.string.dialog_yes),
            onPositive = {
                onRueckgaengig?.invoke(customer)
            }
        )
    }
}
