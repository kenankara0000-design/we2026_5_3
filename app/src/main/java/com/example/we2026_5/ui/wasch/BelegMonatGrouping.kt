package com.example.we2026_5.ui.wasch

import com.example.we2026_5.wasch.WaschErfassung
import java.util.Calendar
import java.util.Locale

/**
 * Ein Beleg = ein Monat pro Kunde. Enthält alle Erfassungen dieses Monats,
 * sortiert absteigend nach Datum/Zeit.
 */
data class BelegMonat(
    val monthKey: String,
    val monthLabel: String,
    val erfassungen: List<WaschErfassung>
)

/**
 * Gruppiert Erfassungen nach Monat (Jahr-Monat). Neueste Monate zuerst,
 * innerhalb eines Monats Erfassungen absteigend nach datum (dann zeit).
 */
object BelegMonatGrouping {

    private val monthLabelFormat = java.text.SimpleDateFormat("MMMM yyyy", Locale.GERMANY)

    fun groupByMonth(erfassungen: List<WaschErfassung>): List<BelegMonat> {
        if (erfassungen.isEmpty()) return emptyList()
        val byKey = erfassungen
            .groupBy { monthKeyFromDatum(it.datum) }
            .mapValues { (_, list) -> list.sortedWith(compareByDescending<WaschErfassung> { it.datum }.thenByDescending { it.zeit }) }
        return byKey.keys.sortedDescending().map { key ->
            val label = monthLabelFromKey(key)
            BelegMonat(monthKey = key, monthLabel = label, erfassungen = byKey[key]!!)
        }
    }

    fun monthKeyFromDatum(datumMs: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = datumMs
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(year, month)
    }

    fun monthLabelFromKey(monthKey: String): String {
        val parts = monthKey.split("-")
        if (parts.size != 2) return monthKey
        val year = parts[0].toIntOrNull() ?: return monthKey
        val month = parts[1].toIntOrNull()?.minus(1) ?: return monthKey
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return monthLabelFormat.format(cal.time)
    }
}
