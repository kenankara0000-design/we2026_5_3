package com.example.we2026_5.tourplanner

import android.content.Context
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerAdapter
import com.example.we2026_5.FirebaseRetryHelper
import com.example.we2026_5.KundenListe
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.util.TerminBerechnungUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Handler für alle Callbacks des CustomerAdapters im TourPlanner.
 * getListen liefert Listen aus dem ViewModel (ohne runBlocking).
 */
class TourPlannerCallbackHandler(
    private val context: Context,
    private val repository: CustomerRepository,
    private val listeRepository: KundenListeRepository,
    private val getListen: () -> List<KundenListe>,
    private val dateUtils: TourPlannerDateUtils,
    private val viewDate: Calendar,
    private val adapter: CustomerAdapter,
    private val reloadCurrentView: () -> Unit,
    private val resetTourCycle: (String) -> Unit
) {
    
    fun setupCallbacks() {
        // Abholung
        adapter.onAbholung = { customer ->
            handleAbholung(customer)
        }
        
        // Auslieferung
        adapter.onAuslieferung = { customer ->
            handleAuslieferung(customer)
        }
        
        // Keine Wäsche (KW): A+KW = erledigt Abholungstag, L+KW = erledigt Auslieferungstag
        adapter.onKw = { customer -> handleKw(customer) }
        
        // Tour-Zyklus zurücksetzen
        adapter.onResetTourCycle = { customerId ->
            resetTourCycle(customerId)
        }
        
        // Verschieben
        adapter.onVerschieben = { customer, newDate, alleVerschieben ->
            handleVerschieben(customer, newDate, alleVerschieben)
        }
        
        // Urlaub
        adapter.onUrlaub = { customer, von, bis ->
            handleUrlaub(customer, von, bis)
        }
        
        // Rückgängig
        adapter.onRueckgaengig = { customer ->
            handleRueckgaengig(customer)
        }
        
        // Callbacks für Datum-Berechnung (für A/L Button-Aktivierung)
        adapter.getAbholungDatum = { customer ->
            val viewDateStart = dateUtils.getStartOfDay(viewDate.timeInMillis)
            dateUtils.calculateAbholungDatum(customer, viewDateStart, dateUtils.getStartOfDay(System.currentTimeMillis()))
        }
        
        adapter.getAuslieferungDatum = { customer ->
            val viewDateStart = dateUtils.getStartOfDay(viewDate.timeInMillis)
            dateUtils.calculateAuslieferungDatum(customer, viewDateStart, dateUtils.getStartOfDay(System.currentTimeMillis()))
        }
        
        // Nächstes Tour-Datum (für "Nächste Tour" auf der Karte; Listen-Kunden: Termin-Regel der Liste)
        adapter.getNaechstesTourDatum = { customer -> dateUtils.getNaechstesTourDatum(customer) }

        // Termine für Kunde (mit Liste bei Listen-Kunden – einheitliche A/L/KW/Ü-Logik)
        adapter.getTermineFuerKunde = { customer, startDatum, tageVoraus ->
            val liste = if (customer.listeId.isNotBlank()) getListen().find { it.id == customer.listeId } else null
            TerminBerechnungUtils.berechneAlleTermineFuerKunde(customer, liste, startDatum, tageVoraus)
        }
    }
    
    private fun handleAbholung(customer: Customer) {
        if (!customer.abholungErfolgt) {
            CoroutineScope(Dispatchers.Main).launch {
                val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
                val liste = withContext(Dispatchers.IO) {
                    if (customer.listeId.isNotBlank()) listeRepository.getListeById(customer.listeId) else null
                }
                val istAbholungHeute = istTerminHeuteFaellig(customer, com.example.we2026_5.TerminTyp.ABHOLUNG, heuteStart, liste)
                
                if (!istAbholungHeute) {
                    android.widget.Toast.makeText(
                        context,
                        "Abholung kann nur erledigt werden, wenn das Datum heute ist.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    adapter.clearPressedButtons()
                    return@launch
                }
                
                // Prüfe ob A und L am gleichen Tag sind
                val sindAmGleichenTag = checkSindAmGleichenTag(customer)
                
                // Berechne Fälligkeitsdatum (wenn überfällig)
                val faelligAmDatum = dateUtils.getFaelligAmDatumFuerAbholung(customer, heuteStart)
                
                // Aktuelles Datum und Zeitstempel
                val jetzt = System.currentTimeMillis()
                val erledigtAm = TerminBerechnungUtils.getStartOfDay(jetzt)
                
                val updates = mutableMapOf<String, Any>()
                updates["abholungErfolgt"] = true
                updates["abholungErledigtAm"] = erledigtAm
                updates["abholungZeitstempel"] = jetzt
                
                // Fälligkeitsdatum speichern, wenn überfällig war
                if (faelligAmDatum > 0) {
                    updates["faelligAmDatum"] = faelligAmDatum
                }
                
                android.util.Log.d("TourPlanner", "Abholung erledigen für ${customer.name}: updates=$updates, sindAmGleichenTag=$sindAmGleichenTag")
                
                val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = { 
                        repository.updateCustomer(customer.id, updates)
                    },
                    context = context,
                    errorMessage = "Fehler beim Registrieren der Abholung. Bitte erneut versuchen.",
                    maxRetries = 3
                )
                if (success == true) {
                    android.widget.Toast.makeText(context, "Abholung registriert", android.widget.Toast.LENGTH_SHORT).show()
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        adapter.clearPressedButtons()
                        reloadCurrentView()
                    }, 2000)
                }
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
                android.widget.Toast.makeText(context, "KW (Keine Wäsche) nur an Abholungs- oder Auslieferungstag.", android.widget.Toast.LENGTH_LONG).show()
                adapter.clearPressedButtons()
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
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = { repository.updateCustomer(customer.id, updates) },
                context = context,
                errorMessage = "Fehler beim Registrieren „Keine Wäsche“. Bitte erneut versuchen.",
                maxRetries = 3
            )
            if (success == true) {
                android.widget.Toast.makeText(context, "Keine Wäsche registriert", android.widget.Toast.LENGTH_SHORT).show()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    adapter.clearPressedButtons()
                    reloadCurrentView()
                }, 2000)
            }
        }
    }

    private fun handleAuslieferung(customer: Customer) {
        if (!customer.auslieferungErfolgt) {
            // WICHTIG: Geschäftslogik - L darf nicht erledigt werden, solange A nicht erledigt ist
            if (!customer.abholungErfolgt) {
                android.widget.Toast.makeText(
                    context,
                    "Auslieferung kann nicht erledigt werden, solange die Abholung nicht erledigt ist.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                adapter.clearPressedButtons()
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    val heuteStart = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis())
                    val liste = withContext(Dispatchers.IO) {
                        if (customer.listeId.isNotBlank()) listeRepository.getListeById(customer.listeId) else null
                    }
                    val istAuslieferungHeute = istTerminHeuteFaellig(customer, com.example.we2026_5.TerminTyp.AUSLIEFERUNG, heuteStart, liste)
                    
                    if (!istAuslieferungHeute) {
                        android.widget.Toast.makeText(
                            context,
                            "Auslieferung kann nur erledigt werden, wenn das Datum heute ist.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        adapter.clearPressedButtons()
                        return@launch
                    }
                    
                    // Prüfe ob A und L am gleichen Tag sind
                    val sindAmGleichenTag = checkSindAmGleichenTag(customer)
                    
                    // Berechne Fälligkeitsdatum (wenn überfällig)
                    val faelligAmDatum = dateUtils.getFaelligAmDatumFuerAuslieferung(customer, heuteStart)
                    
                    // Aktuelles Datum und Zeitstempel
                    val jetzt = System.currentTimeMillis()
                    val erledigtAm = TerminBerechnungUtils.getStartOfDay(jetzt)
                    
                    val updates = mutableMapOf<String, Any>()
                    updates["auslieferungErfolgt"] = true
                    updates["auslieferungErledigtAm"] = erledigtAm
                    updates["auslieferungZeitstempel"] = jetzt
                    
                    // Fälligkeitsdatum speichern, wenn überfällig war (nur wenn noch nicht gesetzt)
                    if (faelligAmDatum > 0 && customer.faelligAmDatum == 0L) {
                        updates["faelligAmDatum"] = faelligAmDatum
                    }
                    
                    android.util.Log.d("TourPlanner", "Auslieferung erledigen für ${customer.name}: updates=$updates, sindAmGleichenTag=$sindAmGleichenTag")
                    
                    val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                        operation = { 
                            repository.updateCustomer(customer.id, updates)
                        },
                        context = context,
                        errorMessage = "Fehler beim Registrieren der Auslieferung. Bitte erneut versuchen.",
                        maxRetries = 3
                    )
                    if (success == true) {
                        android.widget.Toast.makeText(context, "Auslieferung registriert", android.widget.Toast.LENGTH_SHORT).show()
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            adapter.clearPressedButtons()
                            reloadCurrentView()
                        }, 2000)
                    }
                }
            }
        }
    }
    
    private fun handleVerschieben(customer: Customer, newDate: Long, alleVerschieben: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = if (alleVerschieben) {
                // Alle zukünftigen Termine verschieben
                val aktuellerFaelligAm = if (customer.verschobenAufDatum > 0) customer.verschobenAufDatum
                                         else customer.letzterTermin + TimeUnit.DAYS.toMillis(customer.intervallTage.toLong())
                val diff = newDate - aktuellerFaelligAm
                val neuerLetzterTermin = customer.letzterTermin + diff
                FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = { 
                        repository.updateCustomer(customer.id, mapOf(
                            "letzterTermin" to neuerLetzterTermin,
                            "verschobenAufDatum" to 0
                        ))
                    },
                    context = context,
                    errorMessage = "Fehler beim Verschieben. Bitte erneut versuchen.",
                    maxRetries = 3
                )
            } else {
                // Nur diesen Termin verschieben
                FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                    operation = { 
                        repository.updateCustomer(customer.id, mapOf("verschobenAufDatum" to newDate))
                    },
                    context = context,
                    errorMessage = "Fehler beim Verschieben. Bitte erneut versuchen.",
                    maxRetries = 3
                )
            }
            if (success == true) {
                android.widget.Toast.makeText(context, 
                    if (alleVerschieben) "Alle zukünftigen Termine verschoben" else "Termin verschoben", 
                    android.widget.Toast.LENGTH_SHORT).show()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    adapter.clearPressedButtons()
                    reloadCurrentView()
                }, 2000)
            }
        }
    }
    
    private fun handleUrlaub(customer: Customer, von: Long, bis: Long) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = FirebaseRetryHelper.executeSuspendWithRetryAndToast(
                operation = { 
                    repository.updateCustomer(customer.id, mapOf(
                        "urlaubVon" to von, 
                        "urlaubBis" to bis
                    ))
                },
                context = context,
                errorMessage = "Fehler beim Eintragen des Urlaubs. Bitte erneut versuchen.",
                maxRetries = 3
            )
            if (success == true) {
                android.widget.Toast.makeText(context, "Urlaub eingetragen", android.widget.Toast.LENGTH_SHORT).show()
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    adapter.clearPressedButtons()
                    reloadCurrentView()
                }, 2000)
            }
        }
    }
    
    private fun handleRueckgaengig(customer: Customer) {
        CoroutineScope(Dispatchers.Main).launch {
            // Prüfe welches am angezeigten Tag erledigt wurde
            val viewDateStart = TerminBerechnungUtils.getStartOfDay(viewDate.timeInMillis)
            
            // Prüfe welches am angezeigten Tag erledigt wurde ODER ob am angezeigten Tag ein Termin fällig ist
            val abholungDatumHeute = adapter.getAbholungDatum?.invoke(customer) ?: 0L
            val hatAbholungHeute = abholungDatumHeute > 0 && 
                TerminBerechnungUtils.getStartOfDay(abholungDatumHeute) == viewDateStart
            val abholungErledigtAmTag = customer.abholungErfolgt && (
                (customer.abholungErledigtAm > 0 && 
                 TerminBerechnungUtils.getStartOfDay(customer.abholungErledigtAm) == viewDateStart) ||
                hatAbholungHeute
            )
            
            val auslieferungDatumHeute = adapter.getAuslieferungDatum?.invoke(customer) ?: 0L
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
                    operation = { 
                        repository.updateCustomer(customer.id, updates)
                    },
                    context = context,
                    errorMessage = "Fehler beim Rückgängigmachen. Bitte erneut versuchen.",
                    maxRetries = 3
                )
                if (success == true) {
                    android.widget.Toast.makeText(context, "Rückgängig gemacht", android.widget.Toast.LENGTH_SHORT).show()
                    adapter.clearPressedButtons()
                    reloadCurrentView()
                }
            }
        }
    }
    
    /**
     * Prüft, ob ein Termin heute erledigt werden kann (auch für überfällige Termine).
     * Liste wird vom Aufrufer mit withContext(IO) geladen.
     */
    private fun istTerminHeuteFaellig(customer: Customer, terminTyp: com.example.we2026_5.TerminTyp, heuteStart: Long, liste: KundenListe?): Boolean {
        val termine = TerminBerechnungUtils.berechneAlleTermineFuerKunde(
            customer = customer,
            liste = liste,
            startDatum = heuteStart - TimeUnit.DAYS.toMillis(1),
            tageVoraus = 2
        )
        
        // Prüfe ob heute ein Termin des gewünschten Typs fällig ist (normal fällig)
        val terminHeute = termine.firstOrNull { 
            it.typ == terminTyp &&
            TerminBerechnungUtils.getStartOfDay(it.datum) == heuteStart
        }
        
        if (terminHeute != null) {
            return true
        }
        
        // Prüfe ob ein überfälliger Termin heute angezeigt werden soll (Container 2)
        // Überfällige Termine werden angezeigt, wenn:
        // 1. Am tatsächlichen Fälligkeitstag (Container 1) - zeigt überfällig an
        // 2. Am heutigen Tag, wenn noch überfällig (Container 2) - HIER kann erledigt werden
        val ueberfaelligeTermine = termine.filter { 
            it.typ == terminTyp &&
            TerminBerechnungUtils.getStartOfDay(it.datum) < heuteStart
        }
        
        // Prüfe ob ein überfälliger Termin heute angezeigt werden soll (Container 2)
        // WICHTIG: Nur wenn der Termin noch nicht erledigt ist
        val istErledigt = if (terminTyp == com.example.we2026_5.TerminTyp.ABHOLUNG) {
            customer.abholungErfolgt
        } else {
            customer.auslieferungErfolgt
        }
        
        if (!istErledigt) {
            val ueberfaelligerTerminHeuteAnzeigen = ueberfaelligeTermine.any { termin ->
                com.example.we2026_5.util.TerminFilterUtils.sollUeberfaelligAnzeigen(
                    terminDatum = termin.datum,
                    anzeigeDatum = heuteStart,
                    aktuellesDatum = heuteStart
                )
            }
            
            if (ueberfaelligerTerminHeuteAnzeigen) {
                return true
            }
        }
        
        return false
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
