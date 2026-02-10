package com.example.we2026_5.util

import android.app.DatePickerDialog
import android.content.Context
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.ListeIntervall
import java.util.Calendar

/**
 * Generischer Manager für Intervall-Verwaltung.
 * Unterstützt sowohl CustomerIntervall als auch ListeIntervall.
 */
object IntervallManager {
    
    /**
     * Zeigt einen DatePickerDialog für ein CustomerIntervall-Datum an.
     */
    fun showDatumPickerForCustomer(
        context: Context,
        intervalle: MutableList<CustomerIntervall>,
        position: Int,
        isAbholung: Boolean,
        onDatumSelected: (CustomerIntervall) -> Unit
    ) {
        val cal = AppTimeZone.newCalendar()
        val intervall = intervalle.getOrNull(position) ?: return
        
        val initialDatum = if (isAbholung && intervall.abholungDatum > 0) {
            cal.timeInMillis = intervall.abholungDatum
            intervall.abholungDatum
        } else if (!isAbholung && intervall.auslieferungDatum > 0) {
            cal.timeInMillis = intervall.auslieferungDatum
            intervall.auslieferungDatum
        } else {
            System.currentTimeMillis()
        }
        
        cal.timeInMillis = initialDatum
        
        DatePickerDialog(
            context,
            DatePickerDialog.OnDateSetListener { _, year: Int, month: Int, dayOfMonth: Int ->
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val selectedDatum = cal.timeInMillis
                
                val updatedIntervall = if (isAbholung) {
                    intervall.copy(abholungDatum = selectedDatum)
                } else {
                    intervall.copy(auslieferungDatum = selectedDatum)
                }
                
                intervalle[position] = updatedIntervall
                onDatumSelected(updatedIntervall)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    
    /**
     * Zeigt einen DatePickerDialog für ein ListeIntervall-Datum an.
     */
    fun showDatumPickerForListe(
        context: Context,
        intervalle: MutableList<ListeIntervall>,
        position: Int,
        isAbholung: Boolean,
        onDatumSelected: (ListeIntervall) -> Unit
    ) {
        val cal = AppTimeZone.newCalendar()
        val intervall = intervalle.getOrNull(position) ?: return
        
        val initialDatum = if (isAbholung && intervall.abholungDatum > 0) {
            cal.timeInMillis = intervall.abholungDatum
            intervall.abholungDatum
        } else if (!isAbholung && intervall.auslieferungDatum > 0) {
            cal.timeInMillis = intervall.auslieferungDatum
            intervall.auslieferungDatum
        } else {
            System.currentTimeMillis()
        }
        
        cal.timeInMillis = initialDatum
        
        DatePickerDialog(
            context,
            DatePickerDialog.OnDateSetListener { _, year: Int, month: Int, dayOfMonth: Int ->
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val selectedDatum = cal.timeInMillis
                
                val updatedIntervall = if (isAbholung) {
                    intervall.copy(abholungDatum = selectedDatum)
                } else {
                    intervall.copy(auslieferungDatum = selectedDatum)
                }
                
                intervalle[position] = updatedIntervall
                onDatumSelected(updatedIntervall)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    /**
     * Zeigt einen DatePickerDialog für Listen-Termin (A-Datum). L wird als A + tageAzuL berechnet.
     */
    fun showDatePickerForListenTermin(
        context: Context,
        onDateSelected: (Long) -> Unit
    ) {
        val cal = AppTimeZone.newCalendar()
        DatePickerDialog(
            context,
            DatePickerDialog.OnDateSetListener { _, year: Int, month: Int, dayOfMonth: Int ->
                cal.set(year, month, dayOfMonth, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                onDateSelected(cal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
