package com.example.we2026_5.util

import android.app.DatePickerDialog
import android.content.Context
import androidx.appcompat.app.AlertDialog
import java.util.Calendar

/**
 * Basis-Helper für gemeinsame Dialog-Funktionalität.
 * Bietet wiederverwendbare Dialog-Erstellungs-Methoden.
 */
object DialogBaseHelper {
    
    /**
     * Erstellt einen einfachen Bestätigungs-Dialog.
     */
    fun createConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String = "Ja",
        negativeButtonText: String = "Abbrechen",
        onPositive: () -> Unit,
        onNegative: (() -> Unit)? = null
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveButtonText) { _, _ -> onPositive() }
            .setNegativeButton(negativeButtonText) { _, _ -> onNegative?.invoke() }
        
        return builder.create()
    }
    
    /**
     * Zeigt einen einfachen Bestätigungs-Dialog.
     */
    fun showConfirmationDialog(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String = "Ja",
        negativeButtonText: String = "Abbrechen",
        onPositive: () -> Unit,
        onNegative: (() -> Unit)? = null
    ) {
        createConfirmationDialog(
            context = context,
            title = title,
            message = message,
            positiveButtonText = positiveButtonText,
            negativeButtonText = negativeButtonText,
            onPositive = onPositive,
            onNegative = onNegative
        ).show()
    }
    
    /**
     * Erstellt einen DatePickerDialog mit Standard-Konfiguration.
     */
    fun createDatePickerDialog(
        context: Context,
        initialDate: Long = System.currentTimeMillis(),
        title: String? = null,
        onDateSelected: (Long) -> Unit,
        onCancel: (() -> Unit)? = null
    ): DatePickerDialog {
        val cal = AppTimeZone.newCalendar()
        cal.timeInMillis = initialDate
        
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = AppTimeZone.newCalendar().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onDateSelected(selected.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        
        title?.let { dialog.setTitle(it) }
        onCancel?.let { dialog.setOnCancelListener { it() } }
        
        return dialog
    }
    
    /**
     * Zeigt einen DatePickerDialog mit Standard-Konfiguration.
     */
    fun showDatePickerDialog(
        context: Context,
        initialDate: Long = System.currentTimeMillis(),
        title: String? = null,
        onDateSelected: (Long) -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        createDatePickerDialog(
            context = context,
            initialDate = initialDate,
            title = title,
            onDateSelected = onDateSelected,
            onCancel = onCancel
        ).show()
    }
    
    /**
     * Erstellt einen Auswahl-Dialog mit mehreren Optionen.
     */
    fun createSelectionDialog(
        context: Context,
        title: String,
        items: Array<String>,
        onItemSelected: (Int) -> Unit,
        negativeButtonText: String = "Abbrechen"
    ): AlertDialog {
        val builder = AlertDialog.Builder(context)
            .setTitle(title)
            .setItems(items) { _, which -> onItemSelected(which) }
            .setNegativeButton(negativeButtonText, null)
        
        return builder.create()
    }
    
    /**
     * Zeigt einen Auswahl-Dialog mit mehreren Optionen.
     */
    fun showSelectionDialog(
        context: Context,
        title: String,
        items: Array<String>,
        onItemSelected: (Int) -> Unit,
        negativeButtonText: String = "Abbrechen"
    ) {
        createSelectionDialog(
            context = context,
            title = title,
            items = items,
            onItemSelected = onItemSelected,
            negativeButtonText = negativeButtonText
        ).show()
    }
}
