package com.example.we2026_5.tourplanner

import android.content.Context
import com.example.we2026_5.Customer
import com.example.we2026_5.R
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.VerschobenerTermin
import com.example.we2026_5.FirebaseRetryHelper
import com.example.we2026_5.KundenListe
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.util.TerminBerechnungUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Handler für Erledigungs-Callbacks im TourPlanner (Compose-UI).
 * getListen liefert Listen aus dem ViewModel (ohne runBlocking).
 */
class TourPlannerCallbackHandler(
    private val context: Context,
    private val repository: CustomerRepository,
    private val listeRepository: KundenListeRepository,
    private val getListen: () -> List<KundenListe>,
    private val dateUtils: TourPlannerDateUtils,
    private val viewDate: Calendar,
    private val reloadCurrentView: () -> Unit,
    private val resetTourCycle: (String) -> Unit,
    private val onError: ((String) -> Unit)? = null
) {

    private fun getAbholungDatum(customer: Customer): Long {
        val viewDateStart = dateUtils.getStartOfDay(viewDate.timeInMillis)
        return dateUtils.calculateAbholungDatum(customer, viewDateStart, dateUtils.getStartOfDay(System.currentTimeMillis()))
    }

    private fun getAuslieferungDatum(customer: Customer): Long {
        val viewDateStart = dateUtils.getStartOfDay(viewDate.timeInMillis)
        return dateUtils.calculateAuslieferungDatum(customer, viewDateStart, dateUtils.getStartOfDay(System.currentTimeMillis()))
    }

    /** Für Erledigungs-Bottom-Sheet: ruft handleAbholung auf. */
    fun handleAbholungPublic(customer: Customer) = handleAbholung(customer)
    /** Für Erledigungs-Bottom-Sheet: ruft handleAuslieferung auf. */
    fun handleAuslieferungPublic(customer: Customer) = handleAuslieferung(customer)
    /** Für Erledigungs-Bottom-Sheet: ruft handleKw auf. */
    fun handleKwPublic(customer: Customer) = handleKw(customer)
    /** Für Erledigungs-Bottom-Sheet: ruft handleRueckgaengig auf. */
    fun handleRueckgaengigPublic(customer: Customer) = handleRueckgaengig(customer)
    /** Für Erledigungs-Bottom-Sheet / Dialog: ruft handleVerschieben auf. */
    fun handleVerschiebenPublic(customer: Customer, newDate: Long, alleVerschieben: Boolean, typ: TerminTyp? = null) = handleVerschieben(customer, newDate, alleVerschieben, typ)

    /** True, wenn am angezeigten Tag sowohl Abholung als auch Auslieferung fällig sind (A und L am gleichen Tag). */
    fun hatSowohlAAlsAuchLAmViewTag(customer: Customer): Boolean {
        val viewDateStart = TerminBerechnungUtils.getStartOfDay(viewDate.timeInMillis)
        val abholungDatum = getAbholungDatum(customer)
        val auslieferungDatum = getAuslieferungDatum(customer)
        if (abholungDatum <= 0L || auslieferungDatum <= 0L) return false
        return TerminBerechnungUtils.getStartOfDay(abholungDatum) == viewDateStart &&
            TerminBerechnungUtils.getStartOfDay(auslieferungDatum) == viewDateStart
    }
    /** Für Erledigungs-Bottom-Sheet / Dialog: ruft handleUrlaub auf. */
    fun handleUrlaubPublic(customer: Customer, von: Long, bis: Long) = handleUrlaub(customer, von, bis)
    
    private fun handleAbholung(customer: Customer) {
        if (!customer.abholungErfolgt) {
            CoroutineScope(Dispatchers.Main).launch {
                val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
                val liste = withContext(Dispatchers.IO) {
                    if (customer.listeId.isNotBlank()) listeRepository.getListeById(customer.listeId) else null
                }
                val istAbholungHeute = istTerminHeuteFaellig(customer, com.example.we2026_5.TerminTyp.ABHOLUNG, heuteStart, liste)
                
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
    
    private fun handleKw(customer: Customer) {
        CoroutineScope(Dispatchers.Main).launch {
            val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
            val liste = withContext(Dispatchers.IO) {
                if (customer.listeId.isNotBlank()) listeRepository.getListeById(customer.listeId) else null
            }
            val hatAbholungHeute = istTerminHeuteFaellig(customer, com.example.we2026_5.TerminTyp.ABHOLUNG, heuteStart, liste)
            val hatAuslieferungHeute = istTerminHeuteFaellig(customer, com.example.we2026_5.TerminTyp.AUSLIEFERUNG, heuteStart, liste)
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

    private fun handleAuslieferung(customer: Customer) {
        if (!customer.auslieferungErfolgt) {
            // WICHTIG: Geschäftslogik - L darf nicht erledigt werden, solange A nicht erledigt ist
            if (!customer.abholungErfolgt) {
                Toast.makeText(context, context.getString(R.string.toast_auslieferung_nur_nach_abholung), Toast.LENGTH_LONG).show()
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
                    val liste = withContext(Dispatchers.IO) {
                        if (customer.listeId.isNotBlank()) listeRepository.getListeById(customer.listeId) else null
                    }
                    val istAuslieferungHeute = istTerminHeuteFaellig(customer, com.example.we2026_5.TerminTyp.AUSLIEFERUNG, heuteStart, liste)
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
    
    private fun handleVerschieben(customer: Customer, newDate: Long, alleVerschieben: Boolean, typ: TerminTyp? = null) {
        CoroutineScope(Dispatchers.Main).launch {
            val newDateNorm = TerminBerechnungUtils.getStartOfDay(newDate)
            val success = if (alleVerschieben) {
                val aktuellerFaelligAm = customer.getFaelligAm().takeIf { it > 0 }
                    ?: (customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())).takeIf { customer.letzterTermin > 0 } ?: 0L
                val diff = newDateNorm - aktuellerFaelligAm
                val neuerLetzterTermin = customer.letzterTermin + diff
                val serializedLeer = serializeVerschobeneTermine(emptyList())
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
                // Welcher Termin wird verschoben? Der, der am angezeigten Tag (viewDate) sichtbar ist.
                // Falls an viewDate bereits ein verschobener Termin liegt: dessen Originaldatum verwenden (Nachverschiebung).
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
                // Alle Einträge entfernen, die viewDate als Original oder als Ziel haben; dann neuen Eintrag anhängen.
                val newList = customer.verschobeneTermine.filterNot {
                    TerminBerechnungUtils.getStartOfDay(it.originalDatum) == viewDateStart ||
                    TerminBerechnungUtils.getStartOfDay(it.verschobenAufDatum) == viewDateStart
                } + newEntry
                val serialized = serializeVerschobeneTermine(newList)
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
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    reloadCurrentView()
                }, 2000)
            } else {
                onError?.invoke(context.getString(R.string.error_verschieben))
            }
        }
    }

    /** Serialisiert verschobeneTermine für Firebase (Map mit originalDatum, verschobenAufDatum, typ). */
    private fun serializeVerschobeneTermine(list: List<VerschobenerTermin>): Map<String, Map<String, Any>> = list.mapIndexed { index, it ->
        index.toString() to mapOf(
            "originalDatum" to it.originalDatum,
            "verschobenAufDatum" to it.verschobenAufDatum,
            "typ" to it.typ.name
        )
    }.toMap()

    private fun handleUrlaub(customer: Customer, von: Long, bis: Long) {
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
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    reloadCurrentView()
                }, 2000)
            } else {
                onError?.invoke(context.getString(R.string.error_urlaub))
            }
        }
    }
    
    private fun handleRueckgaengig(customer: Customer) {
        CoroutineScope(Dispatchers.Main).launch {
            // Prüfe welches am angezeigten Tag erledigt wurde
            val viewDateStart = TerminBerechnungUtils.getStartOfDay(viewDate.timeInMillis)
            
            // Prüfe welches am angezeigten Tag erledigt wurde ODER ob am angezeigten Tag ein Termin fällig ist
            val abholungDatumHeute = getAbholungDatum(customer)
            val hatAbholungHeute = abholungDatumHeute > 0 && 
                TerminBerechnungUtils.getStartOfDay(abholungDatumHeute) == viewDateStart
            val abholungErledigtAmTag = customer.abholungErfolgt && (
                (customer.abholungErledigtAm > 0 && 
                 TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm) == viewDateStart) ||
                hatAbholungHeute
            )
            
            val auslieferungDatumHeute = getAuslieferungDatum(customer)
            val hatAuslieferungHeute = auslieferungDatumHeute > 0 && 
                TerminBerechnungUtils.getStartOfDay(auslieferungDatumHeute) == viewDateStart
            val auslieferungErledigtAmTag = customer.auslieferungErfolgt && (
                (customer.auslieferungErledigtAm > 0 && 
                 TerminBerechnungUtils.getStartOfDay(customer.auslieferungErledigtAm) == viewDateStart) ||
                hatAuslieferungHeute
            )
            
            val kwErledigtAmTag = customer.keinerWäscheErfolgt && customer.keinerWäscheErledigtAm > 0 &&
                TerminBerechnungUtils.getStartOfDay(customer.keinerWäscheErledigtAm) == viewDateStart
            
            // Prüfe ob beide am gleichen Tag erledigt wurden
            val beideAmGleichenTagErledigt = abholungErledigtAmTag && auslieferungErledigtAmTag &&
                customer.abholungErledigtAm > 0 && customer.auslieferungErledigtAm > 0 &&
                TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm) == 
                TerminBerechnungUtils.getStartOfDay(customer.auslieferungErledigtAm)
            
            val updates = mutableMapOf<String, Any>()
            
            // KW (Keine Wäsche) am Tag zurücksetzen
            if (kwErledigtAmTag) {
                updates["keinerWäscheErfolgt"] = false
                updates["keinerWäscheErledigtAm"] = 0L
                // Wenn A/L am gleichen Tag durch KW gesetzt wurden, auch zurücksetzen
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
            }
            // WICHTIG: Nur das zurücksetzen, was am angezeigten Tag erledigt wurde (wenn nicht schon via KW)
            else if (beideAmGleichenTagErledigt && customer.abholungErfolgt && customer.auslieferungErfolgt) {
                // Beide am gleichen Tag erledigt: beide zurücksetzen
                updates["abholungErfolgt"] = false
                updates["auslieferungErfolgt"] = false
                updates["abholungErledigtAm"] = 0L
                updates["auslieferungErledigtAm"] = 0L
                updates["abholungZeitstempel"] = 0L
                updates["auslieferungZeitstempel"] = 0L
            } else {
                // Unterschiedliche Tage oder nicht beide erledigt: NUR das zurücksetzen, was am angezeigten Tag erledigt wurde
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
            
            // Fälligkeitsdatum zurücksetzen, wenn beide nicht mehr erledigt sind
            val wirdAbholungZurueckgesetzt = updates.containsKey("abholungErfolgt")
            val wirdAuslieferungZurueckgesetzt = updates.containsKey("auslieferungErfolgt")
            
            val abholungNochErledigt = if (wirdAbholungZurueckgesetzt) false else customer.abholungErfolgt
            val auslieferungNochErledigt = if (wirdAuslieferungZurueckgesetzt) false else customer.auslieferungErfolgt
            
            if (!abholungNochErledigt && !auslieferungNochErledigt && customer.faelligAmDatum > 0) {
                updates["faelligAmDatum"] = 0L
            }
            
            android.util.Log.d("TourPlanner", "Rückgängig für ${customer.name}: updates=$updates, beideAmGleichenTagErledigt=$beideAmGleichenTagErledigt, abholungErledigtAmTag=$abholungErledigtAmTag, auslieferungErledigtAmTag=$auslieferungErledigtAmTag")
            
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

    /**
     * Führt Erledigung aus: Firebase-Update mit Retry, bei Erfolg Toast und Reload.
     */
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
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                reloadCurrentView()
            }, 2000)
        } else {
            onError?.invoke(errorMsg)
        }
    }

    /**
     * Prüft, ob ein Termin heute erledigt werden kann (auch für überfällige Termine).
     * Nutzt direkte Prüfung: hatTerminAmDatum(customer, liste, heuteStart, typ).
     */
    private fun istTerminHeuteFaellig(customer: Customer, terminTyp: com.example.we2026_5.TerminTyp, heuteStart: Long, liste: KundenListe?): Boolean {
        // Direkte Prüfung: Hat der Kunde heute einen Termin dieses Typs?
        if (TerminBerechnungUtils.hatTerminAmDatum(customer, liste, heuteStart, terminTyp)) {
            return true
        }
        // Überfällig-Pfad: Termin war vor heute fällig, wird heute angezeigt, noch nicht erledigt
        val istErledigt = if (terminTyp == com.example.we2026_5.TerminTyp.ABHOLUNG) {
            customer.abholungErfolgt
        } else {
            customer.auslieferungErfolgt
        }
        if (istErledigt) return false
        val vergangeneTermine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
            customer = customer,
            liste = liste,
            startDatum = heuteStart - TimeUnit.DAYS.toMillis(60),
            tageVoraus = 60
        )
        val ueberfaelligeTermine = vergangeneTermine.filter {
            it.typ == terminTyp && TerminBerechnungUtils.getStartOfDay(it.datum) < heuteStart
        }
        return ueberfaelligeTermine.any { termin ->
            com.example.we2026_5.util.TerminFilterUtils.sollUeberfaelligAnzeigen(
                terminDatum = termin.datum,
                anzeigeDatum = heuteStart,
                aktuellesDatum = heuteStart
            )
        }
    }
    
    private fun checkSindAmGleichenTag(customer: Customer): Boolean {
        return if (customer.intervalle.isNotEmpty()) {
            // NEUE STRUKTUR: Prüfe alle Intervalle
            customer.intervalle.any { intervall ->
                val abholungStartDatum = intervall.abholungDatum
                val auslieferungStartDatum = intervall.auslieferungDatum
                if (abholungStartDatum > 0 && auslieferungStartDatum > 0) {
                    val abholungStart = TerminBerechnungUtils.getStartOfDay(abholungStartDatum)
                    val auslieferungStart = TerminBerechnungUtils.getStartOfDay(auslieferungStartDatum)
                    abholungStart == auslieferungStart
                } else {
                    false
                }
            }
        } else {
            // ALTE STRUKTUR: Prüfe Startdaten
            val abholungStartDatum = customer.abholungDatum
            val auslieferungStartDatum = customer.auslieferungDatum
            if (abholungStartDatum > 0 && auslieferungStartDatum > 0) {
                val abholungStart = TerminBerechnungUtils.getStartOfDay(abholungStartDatum)
                val auslieferungStart = TerminBerechnungUtils.getStartOfDay(auslieferungStartDatum)
                abholungStart == auslieferungStart
            } else {
                false
            }
        }
    }
}
