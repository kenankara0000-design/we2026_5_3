package com.example.we2026_5.tourplanner

import android.content.Context
import android.widget.Toast
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.R
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.TerminFilterUtils
import com.example.we2026_5.FirebaseRetryHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Handler für Erledigungs-Aktionen: Abholung, Auslieferung, Keine Wäsche, Rückgängig.
 */
internal class TourPlannerErledigungHandler(
    private val context: Context,
    private val repository: CustomerRepository,
    private val listeRepository: KundenListeRepository,
    private val dateUtils: TourPlannerDateUtils,
    private val viewDate: Calendar,
    private val reloadCurrentView: () -> Unit,
    private val onError: ((String) -> Unit)?
) {

    fun getAbholungDatum(customer: Customer): Long {
        val viewDateStart = dateUtils.getStartOfDay(viewDate.timeInMillis)
        return dateUtils.calculateAbholungDatum(customer, viewDateStart, dateUtils.getStartOfDay(System.currentTimeMillis()))
    }

    fun getAuslieferungDatum(customer: Customer): Long {
        val viewDateStart = dateUtils.getStartOfDay(viewDate.timeInMillis)
        return dateUtils.calculateAuslieferungDatum(customer, viewDateStart, dateUtils.getStartOfDay(System.currentTimeMillis()))
    }

    fun hatSowohlAAlsAuchLAmViewTag(customer: Customer): Boolean {
        val viewDateStart = TerminBerechnungUtils.getStartOfDay(viewDate.timeInMillis)
        val abholungDatum = getAbholungDatum(customer)
        val auslieferungDatum = getAuslieferungDatum(customer)
        if (abholungDatum <= 0L || auslieferungDatum <= 0L) return false
        return TerminBerechnungUtils.getStartOfDay(abholungDatum) == viewDateStart &&
            TerminBerechnungUtils.getStartOfDay(auslieferungDatum) == viewDateStart
    }

    fun handleAbholung(customer: Customer) {
        val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
        val hatAusnahmeAbholungHeute = hatAusnahmeTerminHeute(customer, heuteStart, "A")
        val hatKundenAbholungHeute = hatKundenTerminHeute(customer, heuteStart, "A")
        if (!customer.abholungErfolgt || hatAusnahmeAbholungHeute || hatKundenAbholungHeute) {
            CoroutineScope(Dispatchers.Main).launch {
                val istAbholungHeute = istTerminHeuteFaellig(customer, com.example.we2026_5.TerminTyp.ABHOLUNG, heuteStart, null) ||
                    hatAusnahmeTerminHeute(customer, heuteStart, "A") || hatKundenTerminHeute(customer, heuteStart, "A")

                if (!istAbholungHeute) {
                    Toast.makeText(context, context.getString(R.string.toast_abholung_nur_heute), Toast.LENGTH_LONG).show()
                    return@launch
                }
                val faelligAmDatum = dateUtils.getFaelligAmDatumFuerAbholung(customer, heuteStart)
                val jetzt = System.currentTimeMillis()
                val erledigtAm = TerminBerechnungUtils.getStartOfDay(jetzt)
                val updates = mutableMapOf<String, Any>()
                updates["abholungErfolgt"] = true
                updates["abholungErledigtAm"] = erledigtAm
                updates["abholungZeitstempel"] = jetzt
                if (faelligAmDatum > 0) updates["faelligAmDatum"] = faelligAmDatum
                android.util.Log.d("TourPlanner", "Abholung erledigen für ${customer.name}: updates=$updates")
                executeErledigung(customer.id, updates, R.string.error_abholung_registrieren, R.string.toast_abholung_dann_auslieferung)
            }
        }
    }

    fun handleAuslieferung(customer: Customer) {
        if (!customer.auslieferungErfolgt) {
            val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
            val hatAusnahmeLHeute = hatAusnahmeTerminHeute(customer, heuteStart, "L")
            val hatAusnahmeAHeute = hatAusnahmeTerminHeute(customer, heuteStart, "A")
            val hatKundenLHeute = hatKundenTerminHeute(customer, heuteStart, "L")
            val hatKundenAHeute = hatKundenTerminHeute(customer, heuteStart, "A")
            val nurAusnahmeLHeute = hatAusnahmeLHeute && !hatAusnahmeAHeute
            val nurKundenLHeute = hatKundenLHeute && !hatKundenAHeute
            val abholungNötig = !customer.abholungErfolgt && !nurAusnahmeLHeute && !nurKundenLHeute
            if (abholungNötig) {
                Toast.makeText(context, context.getString(R.string.toast_auslieferung_nur_nach_abholung), Toast.LENGTH_LONG).show()
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    val istAuslieferungHeute = istTerminHeuteFaellig(customer, com.example.we2026_5.TerminTyp.AUSLIEFERUNG, heuteStart, null) ||
                        hatAusnahmeTerminHeute(customer, heuteStart, "L") || hatKundenTerminHeute(customer, heuteStart, "L")
                    if (!istAuslieferungHeute) {
                        Toast.makeText(context, context.getString(R.string.toast_auslieferung_nur_heute), Toast.LENGTH_LONG).show()
                        return@launch
                    }
                    val faelligAmDatum = dateUtils.getFaelligAmDatumFuerAuslieferung(customer, heuteStart)
                    val jetzt = System.currentTimeMillis()
                    val erledigtAm = TerminBerechnungUtils.getStartOfDay(jetzt)
                    val updates = mutableMapOf<String, Any>()
                    updates["auslieferungErfolgt"] = true
                    updates["auslieferungErledigtAm"] = erledigtAm
                    updates["auslieferungZeitstempel"] = jetzt
                    if (faelligAmDatum > 0 && customer.faelligAmDatum == 0L) updates["faelligAmDatum"] = faelligAmDatum
                    android.util.Log.d("TourPlanner", "Auslieferung erledigen für ${customer.name}: updates=$updates")
                    executeErledigung(customer.id, updates, R.string.error_auslieferung_registrieren, R.string.toast_auslieferung_registriert)
                }
            }
        }
    }

    fun handleKw(customer: Customer) {
        CoroutineScope(Dispatchers.Main).launch {
            val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
            val hatAbholungHeute = istTerminHeuteFaellig(customer, com.example.we2026_5.TerminTyp.ABHOLUNG, heuteStart, null)
            val hatAuslieferungHeute = istTerminHeuteFaellig(customer, com.example.we2026_5.TerminTyp.AUSLIEFERUNG, heuteStart, null)
            if (!hatAbholungHeute && !hatAuslieferungHeute) {
                Toast.makeText(context, context.getString(R.string.toast_kw_nur_abholung_auslieferung), Toast.LENGTH_LONG).show()
                return@launch
            }
            val erledigtAm = heuteStart
            val updates = mutableMapOf<String, Any>()
            updates["keinerWäscheErfolgt"] = true
            updates["keinerWäscheErledigtAm"] = erledigtAm
            if (hatAbholungHeute && !customer.abholungErfolgt) {
                updates["abholungErfolgt"] = true
                updates["abholungErledigtAm"] = erledigtAm
                updates["abholungZeitstempel"] = System.currentTimeMillis()
                val faelligAmDatum = dateUtils.getFaelligAmDatumFuerAbholung(customer, heuteStart)
                if (faelligAmDatum > 0) updates["faelligAmDatum"] = faelligAmDatum
            }
            if (hatAuslieferungHeute && !customer.auslieferungErfolgt) {
                updates["auslieferungErfolgt"] = true
                updates["auslieferungErledigtAm"] = erledigtAm
                updates["auslieferungZeitstempel"] = System.currentTimeMillis()
                if (customer.faelligAmDatum == 0L) {
                    val faelligAmL = dateUtils.getFaelligAmDatumFuerAuslieferung(customer, heuteStart)
                    if (faelligAmL > 0) updates["faelligAmDatum"] = faelligAmL
                }
            }
            executeErledigung(customer.id, updates, R.string.error_kw_registrieren, R.string.toast_kw_registriert)
        }
    }

    fun handleRueckgaengig(customer: Customer) {
        CoroutineScope(Dispatchers.Main).launch {
            val viewDateStart = TerminBerechnungUtils.getStartOfDay(viewDate.timeInMillis)
            val abholungDatumHeute = getAbholungDatum(customer)
            val hatAbholungHeute = abholungDatumHeute > 0 &&
                TerminBerechnungUtils.getStartOfDay(abholungDatumHeute) == viewDateStart
            val abholungErledigtAmTag = customer.abholungErfolgt && (
                (customer.abholungErledigtAm > 0 &&
                 TerminBerechnungUtils.isTimestampInBerlinDay(customer.abholungErledigtAm, viewDateStart)) ||
                hatAbholungHeute
            )

            val auslieferungDatumHeute = getAuslieferungDatum(customer)
            val hatAuslieferungHeute = auslieferungDatumHeute > 0 &&
                TerminBerechnungUtils.getStartOfDay(auslieferungDatumHeute) == viewDateStart
            val auslieferungErledigtAmTag = customer.auslieferungErfolgt && (
                (customer.auslieferungErledigtAm > 0 &&
                 TerminBerechnungUtils.isTimestampInBerlinDay(customer.auslieferungErledigtAm, viewDateStart)) ||
                hatAuslieferungHeute
            )

            val kwErledigtAmTag = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                TerminBerechnungUtils.isTimestampInBerlinDay(customer.keinerWäscheErledigtAm, viewDateStart)

            val beideAmGleichenTagErledigt = abholungErledigtAmTag && auslieferungErledigtAmTag &&
                customer.abholungErledigtAm > 0 && customer.auslieferungErledigtAm > 0 &&
                TerminBerechnungUtils.isTimestampInBerlinDay(customer.abholungErledigtAm, viewDateStart) &&
                TerminBerechnungUtils.isTimestampInBerlinDay(customer.auslieferungErledigtAm, viewDateStart)

            val updates = mutableMapOf<String, Any>()

            if (kwErledigtAmTag) {
                updates["keinerWäscheErfolgt"] = false
                updates["keinerWäscheErledigtAm"] = 0L
                if (abholungErledigtAmTag) {
                    updates["abholungErfolgt"] = false
                    updates["abholungErledigtAm"] = 0L
                    updates["abholungZeitstempel"] = 0L
                }
                if (auslieferungErledigtAmTag) {
                    updates["auslieferungErfolgt"] = false
                    updates["auslieferungErledigtAm"] = 0L
                    updates["auslieferungZeitstempel"] = 0L
                }
            } else if (beideAmGleichenTagErledigt && customer.abholungErfolgt && customer.auslieferungErfolgt) {
                updates["abholungErfolgt"] = false
                updates["auslieferungErfolgt"] = false
                updates["abholungErledigtAm"] = 0L
                updates["auslieferungErledigtAm"] = 0L
                updates["abholungZeitstempel"] = 0L
                updates["auslieferungZeitstempel"] = 0L
            } else {
                if (abholungErledigtAmTag && customer.abholungErfolgt) {
                    updates["abholungErfolgt"] = false
                    updates["abholungErledigtAm"] = 0L
                    updates["abholungZeitstempel"] = 0L
                }
                if (auslieferungErledigtAmTag && customer.auslieferungErfolgt) {
                    updates["auslieferungErfolgt"] = false
                    updates["auslieferungErledigtAm"] = 0L
                    updates["auslieferungZeitstempel"] = 0L
                }
            }

            val wirdAbholungZurueckgesetzt = updates.containsKey("abholungErfolgt")
            val wirdAuslieferungZurueckgesetzt = updates.containsKey("auslieferungErfolgt")
            val abholungNochErledigt = if (wirdAbholungZurueckgesetzt) false else customer.abholungErfolgt
            val auslieferungNochErledigt = if (wirdAuslieferungZurueckgesetzt) false else customer.auslieferungErfolgt

            if (!abholungNochErledigt && !auslieferungNochErledigt && customer.faelligAmDatum > 0) {
                updates["faelligAmDatum"] = 0L
            }

            android.util.Log.d("TourPlanner", "Rückgängig für ${customer.name}: updates=$updates")

            if (updates.isNotEmpty()) {
                val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = { repository.updateCustomer(customer.id, updates) },
                    context = context,
                    errorMessage = context.getString(R.string.error_rueckgaengig),
                    maxRetries = 3
                )
                if (success == true) {
                    Toast.makeText(context, context.getString(R.string.toast_rueckgaengig_gemacht), Toast.LENGTH_SHORT).show()
                    reloadCurrentView()
                } else {
                    onError?.invoke(context.getString(R.string.error_rueckgaengig))
                }
            }
        }
    }

    private suspend fun executeErledigung(
        customerId: String,
        updates: Map<String, Any>,
        errorMessageResId: Int,
        successMessageResId: Int
    ) {
        val errorMsg = context.getString(errorMessageResId)
        val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
            operation = { repository.updateCustomer(customerId, updates) },
            context = context,
            errorMessage = errorMsg,
            maxRetries = 3
        )
        if (success == true) {
            Toast.makeText(context, context.getString(successMessageResId), Toast.LENGTH_SHORT).show()
            reloadCurrentView()
        } else {
            onError?.invoke(errorMsg)
        }
    }

    /** Ausnahme-Termin (A oder L) am angegebenen Tag – unabhängig von regulären Terminen. */
    private fun hatAusnahmeTerminHeute(customer: Customer, tagStart: Long, typ: String): Boolean =
        customer.ausnahmeTermine.any {
            TerminBerechnungUtils.getStartOfDay(it.datum) == tagStart && it.typ == typ
        }

    /** Kunden-Termin (A oder L) am angegebenen Tag. */
    private fun hatKundenTerminHeute(customer: Customer, tagStart: Long, typ: String): Boolean =
        customer.kundenTermine.any {
            TerminBerechnungUtils.getStartOfDay(it.datum) == tagStart && it.typ == typ
        }

    private fun istTerminHeuteFaellig(customer: Customer, terminTyp: com.example.we2026_5.TerminTyp, heuteStart: Long, liste: KundenListe?): Boolean {
        if (TerminBerechnungUtils.hatTerminAmDatum(customer, liste, heuteStart, terminTyp)) return true
        val istErledigt = if (terminTyp == com.example.we2026_5.TerminTyp.ABHOLUNG) customer.abholungErfolgt else customer.auslieferungErfolgt
        if (istErledigt) return false
        val vergangeneTermine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
            customer = customer,
            liste = null,
            startDatum = heuteStart - TimeUnit.DAYS.toMillis(60),
            tageVoraus = 60
        )
        val ueberfaelligeTermine = vergangeneTermine.filter {
            it.typ == terminTyp && TerminBerechnungUtils.getStartOfDay(it.datum) < heuteStart
        }
        return ueberfaelligeTermine.any { termin ->
            TerminFilterUtils.sollUeberfaelligAnzeigen(terminDatum = termin.datum, anzeigeDatum = heuteStart, aktuellesDatum = heuteStart)
        }
    }
}
