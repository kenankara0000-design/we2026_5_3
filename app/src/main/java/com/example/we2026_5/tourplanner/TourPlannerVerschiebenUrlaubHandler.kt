package com.example.we2026_5.tourplanner

import android.content.Context
import android.widget.Toast
import com.example.we2026_5.Customer
import com.example.we2026_5.FirebaseRetryHelper
import com.example.we2026_5.R
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.VerschobenerTermin
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.util.TerminBerechnungUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Handler für Verschieben und Urlaub-Aktionen.
 */
internal class TourPlannerVerschiebenUrlaubHandler(
    private val context: Context,
    private val repository: CustomerRepository,
    private val viewDate: Calendar,
    private val reloadCurrentView: () -> Unit,
    private val onError: ((String) -> Unit)?
) {

    fun handleVerschieben(customer: Customer, newDate: Long, alleVerschieben: Boolean, typ: TerminTyp? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            val newDateNorm = TerminBerechnungUtils.getStartOfDay(newDate)
            val success = if (alleVerschieben) {
                val aktuellerFaelligAm = TerminBerechnungUtils.naechstesFaelligAmDatum(customer).takeIf { it > 0 }
                    ?: (customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())).takeIf { customer.letzterTermin > 0 } ?: 0L
                val diff = newDateNorm - aktuellerFaelligAm
                val neuerLetzterTermin = customer.letzterTermin + diff
                val serializedLeer = TourPlannerCallbackHelpers.serializeVerschobeneTermine(emptyList())
                FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = {
                        repository.updateCustomer(customer.id, mapOf(
                            "letzterTermin" to neuerLetzterTermin,
                            "verschobeneTermine" to serializedLeer
                        ))
                    },
                    context = context,
                    errorMessage = context.getString(R.string.error_verschieben),
                    maxRetries = 3
                )
            } else {
                val viewDateStart = TerminBerechnungUtils.getStartOfDay(viewDate.timeInMillis)
                val existingForViewDate = customer.verschobeneTermine.firstOrNull {
                    TerminBerechnungUtils.getStartOfDay(it.verschobenAufDatum) == viewDateStart
                }
                val originalDatumRaw = if (existingForViewDate != null) existingForViewDate.originalDatum else viewDate.timeInMillis
                val originalDatumNorm = TerminBerechnungUtils.getStartOfDay(originalDatumRaw)
                val newEntry = VerschobenerTermin(
                    originalDatum = originalDatumNorm,
                    verschobenAufDatum = newDateNorm,
                    intervallId = null,
                    typ = typ ?: TerminTyp.ABHOLUNG
                )
                val newList = customer.verschobeneTermine.filterNot {
                    TerminBerechnungUtils.getStartOfDay(it.originalDatum) == viewDateStart ||
                    TerminBerechnungUtils.getStartOfDay(it.verschobenAufDatum) == viewDateStart
                } + newEntry
                val serialized = TourPlannerCallbackHelpers.serializeVerschobeneTermine(newList)
                FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = { repository.updateCustomer(customer.id, mapOf("verschobeneTermine" to serialized)) },
                    context = context,
                    errorMessage = context.getString(R.string.error_verschieben),
                    maxRetries = 3
                )
            }
            if (success == true) {
                Toast.makeText(context,
                    if (alleVerschieben) context.getString(R.string.toast_termine_verschoben) else context.getString(R.string.toast_termin_verschoben),
                    Toast.LENGTH_SHORT).show()
                reloadCurrentView()
            } else {
                onError?.invoke(context.getString(R.string.error_verschieben))
            }
        }
    }

    fun handleUrlaub(customer: Customer, von: Long, bis: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = {
                    repository.updateCustomer(customer.id, mapOf("urlaubVon" to von, "urlaubBis" to bis))
                },
                context = context,
                errorMessage = context.getString(R.string.error_urlaub),
                maxRetries = 3
            )
            if (success == true) {
                Toast.makeText(context, context.getString(R.string.toast_urlaub_eingetragen), Toast.LENGTH_SHORT).show()
                reloadCurrentView()
            } else {
                onError?.invoke(context.getString(R.string.error_urlaub))
            }
        }
    }
}
