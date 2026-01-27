package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.ListItem
import com.example.we2026_5.SectionType
import com.example.we2026_5.util.TerminFilterUtils
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Prozessor für Tour-Datenverarbeitung.
 * Extrahiert die komplexe Datenverarbeitungslogik aus TourPlannerViewModel.
 */
class TourDataProcessor {
    
    fun processTourData(
        allCustomers: List<Customer>,
        allListen: List<KundenListe>,
        selectedTimestamp: Long,
        expandedSections: Set<SectionType>
    ): List<ListItem> {
        val viewDateStart = getStartOfDay(selectedTimestamp)
        val heuteStart = getStartOfDay(System.currentTimeMillis())
        
        // Alle Kunden nach Listen gruppieren
        val kundenNachListen = allCustomers.filter { it.listeId.isNotEmpty() }.groupBy { it.listeId }
        val kundenOhneListe = allCustomers.filter { it.listeId.isEmpty() }
        
        // Kunden in Listen filtern
        val listenMitKunden = mutableMapOf<String, List<Customer>>()
        kundenNachListen.forEach { (listeId, kunden) ->
            if (listeId.isEmpty()) return@forEach
            val liste = allListen.find { it.id == listeId } ?: return@forEach
            
            val istFaellig = liste.intervalle.any { intervall ->
                isIntervallFaelligAm(intervall, viewDateStart) || 
                isIntervallFaelligInZukunft(intervall, viewDateStart)
            }
            
            if (istFaellig) {
                val fälligeKunden = kunden.filter { customer ->
                    val faelligAm = customerFaelligAm(customer, liste, viewDateStart)
                    val customerImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                           faelligAm in customer.urlaubVon..customer.urlaubBis
                    val listeImUrlaub = liste?.let { 
                        it.urlaubVon > 0 && it.urlaubBis > 0 && faelligAm in it.urlaubVon..it.urlaubBis 
                    } ?: false
                    if (customerImUrlaub || listeImUrlaub) return@filter false
                    
                    val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt
                    
                    val warUeberfaelligUndErledigtAmDatum = if (isDone && customer.faelligAmDatum > 0) {
                        val faelligAmStart = getStartOfDay(customer.faelligAmDatum)
                        val erledigtAmStart = if (customer.abholungErledigtAm > 0) {
                            getStartOfDay(customer.abholungErledigtAm)
                        } else if (customer.auslieferungErledigtAm > 0) {
                            getStartOfDay(customer.auslieferungErledigtAm)
                        } else {
                            0L
                        }
                        viewDateStart == faelligAmStart || (erledigtAmStart > 0 && viewDateStart == erledigtAmStart)
                    } else {
                        false
                    }
                    
                    if (warUeberfaelligUndErledigtAmDatum) {
                        return@filter true
                    }
                    
                    val isOverdue = istKundeUeberfaellig(customer, liste, viewDateStart, heuteStart)
                    if (isOverdue) {
                        return@filter true
                    }
                    
                    hatKundeTerminAmDatum(customer, liste, viewDateStart)
                }
                
                if (fälligeKunden.isNotEmpty()) {
                    listenMitKunden[listeId] = fälligeKunden.sortedBy { it.name }
                }
            }
        }
        
        // Gewerblich-Kunden ohne Liste filtern
        val gewerblichKundenOhneListe = kundenOhneListe.filter { it.kundenArt == "Gewerblich" }
        val filteredGewerblich = gewerblichKundenOhneListe.filter { customer ->
            val isDone = customer.abholungErfolgt || customer.auslieferungErfolgt
            
            val warUeberfaelligUndErledigtAmDatum = if (isDone && customer.faelligAmDatum > 0) {
                val faelligAmStart = getStartOfDay(customer.faelligAmDatum)
                val erledigtAmStart = if (customer.abholungErledigtAm > 0) {
                    getStartOfDay(customer.abholungErledigtAm)
                } else if (customer.auslieferungErledigtAm > 0) {
                    getStartOfDay(customer.auslieferungErledigtAm)
                } else {
                    0L
                }
                viewDateStart == faelligAmStart || (erledigtAmStart > 0 && viewDateStart == erledigtAmStart)
            } else {
                false
            }
            
            if (warUeberfaelligUndErledigtAmDatum) {
                return@filter true
            }
            
            val faelligAm = customerFaelligAm(customer, null, viewDateStart)
            val faelligAmImUrlaub = customer.urlaubVon > 0 && customer.urlaubBis > 0 && 
                                   faelligAm in customer.urlaubVon..customer.urlaubBis
            if (faelligAmImUrlaub) return@filter false
            
            val isOverdue = istKundeUeberfaellig(customer, null, viewDateStart, heuteStart)
            if (isOverdue) {
                return@filter true
            }
            
            hatKundeTerminAmDatum(customer, null, viewDateStart)
        }
        
        // Liste mit Items erstellen
        val items = mutableListOf<ListItem>()
        val kundenInListenIds = listenMitKunden.values.flatten().map { it.id }.toSet()
        
        // Gewerblich-Kunden OHNE Liste kategorisieren
        val overdueGewerblich = mutableListOf<Customer>()
        val normalGewerblich = mutableListOf<Customer>()
        val doneGewerblich = mutableListOf<Customer>()
        
        filteredGewerblich.forEach { customer ->
            if (customer.id in kundenInListenIds) return@forEach
            
            val isOverdue = istKundeUeberfaellig(customer, null, viewDateStart, heuteStart)
            
            val warUeberfaelligUndErledigt = if ((customer.abholungErfolgt || customer.auslieferungErfolgt) && customer.faelligAmDatum > 0) {
                val faelligAmStart = getStartOfDay(customer.faelligAmDatum)
                val erledigtAmStart = if (customer.abholungErledigtAm > 0) {
                    getStartOfDay(customer.abholungErledigtAm)
                } else if (customer.auslieferungErledigtAm > 0) {
                    getStartOfDay(customer.auslieferungErledigtAm)
                } else {
                    0L
                }
                viewDateStart == faelligAmStart || (erledigtAmStart > 0 && viewDateStart == erledigtAmStart)
            } else {
                false
            }
            
            val hatErledigtenTerminAmDatum = if (customer.abholungErfolgt || customer.auslieferungErfolgt) {
                val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                    customer = customer,
                    startDatum = viewDateStart - TimeUnit.DAYS.toMillis(365),
                    tageVoraus = 730
                )
                
                val hatErledigtenATermin = if (customer.abholungErfolgt) {
                    val abholungErledigtAmStart = if (customer.abholungErledigtAm > 0) getStartOfDay(customer.abholungErledigtAm) else 0L
                    if (abholungErledigtAmStart > 0 && viewDateStart == abholungErledigtAmStart) {
                        true
                    } else {
                        termine.any { termin ->
                            termin.typ == com.example.we2026_5.TerminTyp.ABHOLUNG &&
                            getStartOfDay(termin.datum) == viewDateStart
                        }
                    }
                } else {
                    false
                }
                
                val hatErledigtenLTermin = if (customer.auslieferungErfolgt) {
                    val auslieferungErledigtAmStart = if (customer.auslieferungErledigtAm > 0) getStartOfDay(customer.auslieferungErledigtAm) else 0L
                    if (auslieferungErledigtAmStart > 0 && viewDateStart == auslieferungErledigtAmStart) {
                        true
                    } else {
                        termine.any { termin ->
                            termin.typ == com.example.we2026_5.TerminTyp.AUSLIEFERUNG &&
                            getStartOfDay(termin.datum) == viewDateStart
                        }
                    }
                } else {
                    false
                }
                
                hatErledigtenATermin || hatErledigtenLTermin
            } else {
                false
            }
            
            when {
                warUeberfaelligUndErledigt -> doneGewerblich.add(customer)
                hatErledigtenTerminAmDatum -> doneGewerblich.add(customer)
                isOverdue -> overdueGewerblich.add(customer)
                else -> normalGewerblich.add(customer)
            }
        }
        
        // REIHENFOLGE: 1. Überfällig, 2. Listen, 3. Normal, 4. Erledigt
        
        // 1. Überfällige Kunden
        val overdueOhneListen = overdueGewerblich.sortedBy { it.name }
        if (overdueOhneListen.isNotEmpty()) {
            items.add(ListItem.SectionHeader("ÜBERFÄLLIG", overdueOhneListen.size, 0, SectionType.OVERDUE))
            if (expandedSections.contains(SectionType.OVERDUE)) {
                overdueOhneListen.forEach { items.add(ListItem.CustomerItem(it)) }
            }
        }
        
        // 2. Kunden nach Listen gruppiert
        allListen.sortedBy { it.name }.forEach { liste ->
            val kundenInListe = listenMitKunden[liste.id] ?: return@forEach
            if (kundenInListe.isNotEmpty()) {
                val erledigteKunden = kundenInListe.count { customer ->
                    val customerDone = customer.abholungErfolgt && customer.auslieferungErfolgt
                    val listeDone = liste.abholungErfolgt && liste.auslieferungErfolgt
                    customerDone || listeDone
                }
                items.add(ListItem.ListeHeader(liste.name, kundenInListe.size, erledigteKunden, liste.id))
                kundenInListe.forEach { items.add(ListItem.CustomerItem(it)) }
            }
        }
        
        // 3. Normale Kunden
        val normalOhneListen = normalGewerblich.sortedBy { it.name }
        normalOhneListen.forEach { items.add(ListItem.CustomerItem(it)) }
        
        // 4. Erledigt-Bereich
        val doneOhneListen = doneGewerblich.sortedBy { it.name }
        items.add(ListItem.SectionHeader("ERLEDIGT", doneOhneListen.size, doneOhneListen.size, SectionType.DONE))
        if (expandedSections.contains(SectionType.DONE)) {
            doneOhneListen.forEach { items.add(ListItem.CustomerItem(it)) }
        }
        
        return items
    }
    
    fun customerFaelligAm(c: Customer, liste: KundenListe? = null, abDatum: Long = System.currentTimeMillis()): Long {
        // NEUE STRUKTUR: Verwende Intervalle-Liste wenn vorhanden
        if (c.intervalle.isNotEmpty()) {
            val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = c,
                liste = liste,
                startDatum = abDatum,
                tageVoraus = 365
            )
            val naechstesTermin = termine.firstOrNull { 
                it.datum >= getStartOfDay(abDatum)
            }
            return naechstesTermin?.datum ?: 0L
        }
        
        // Für Kunden in Listen: Daten der Liste verwenden
        if (c.listeId.isNotEmpty() && liste != null) {
            if (c.verschobenAufDatum > 0) {
                val verschobenStart = getStartOfDay(c.verschobenAufDatum)
                if (c.geloeschteTermine.contains(verschobenStart)) {
                    val naechstesDatum = getNaechstesListeDatum(liste, verschobenStart + TimeUnit.DAYS.toMillis(1))
                    return naechstesDatum ?: abDatum
                }
                return c.verschobenAufDatum
            }
            
            val naechstesDatum = getNaechstesListeDatum(liste, abDatum, c.geloeschteTermine)
            return naechstesDatum ?: 0L
        }
        
        // Für Kunden ohne Liste: Normale Logik
        if (!c.wiederholen) {
            val abholungStart = getStartOfDay(c.abholungDatum)
            val auslieferungStart = getStartOfDay(c.auslieferungDatum)
            val abDatumStart = getStartOfDay(abDatum)
            
            if (c.verschobenAufDatum > 0) {
                val verschobenStart = getStartOfDay(c.verschobenAufDatum)
                if (c.geloeschteTermine.contains(verschobenStart)) {
                    return 0L
                }
                if (abDatumStart == verschobenStart) return c.verschobenAufDatum
                if (abDatumStart < verschobenStart) return c.verschobenAufDatum
                return 0L
            }
            
            val abholungGeloescht = c.geloeschteTermine.contains(abholungStart)
            val auslieferungGeloescht = c.geloeschteTermine.contains(auslieferungStart)
            
            if (abholungGeloescht && auslieferungGeloescht) return 0L
            
            if (abDatumStart == abholungStart && !abholungGeloescht) {
                return c.abholungDatum
            }
            
            if (abDatumStart == auslieferungStart && !auslieferungGeloescht) {
                return c.auslieferungDatum
            }
            
            if (abDatumStart > abholungStart && abDatumStart < auslieferungStart) {
                return if (!auslieferungGeloescht) c.auslieferungDatum else 0L
            }
            if (abDatumStart > auslieferungStart && abDatumStart < abholungStart) {
                return if (!abholungGeloescht) c.abholungDatum else 0L
            }
            
            if (abDatumStart < abholungStart && abDatumStart < auslieferungStart) {
                return if (!abholungGeloescht) c.abholungDatum else 
                       if (!auslieferungGeloescht) c.auslieferungDatum else 0L
            }
            
            if (abDatumStart > abholungStart && abDatumStart > auslieferungStart) {
                return 0L
            }
            
            if (!abholungGeloescht && !auslieferungGeloescht) {
                return minOf(c.abholungDatum, c.auslieferungDatum)
            }
            if (!abholungGeloescht) return c.abholungDatum
            if (!auslieferungGeloescht) return c.auslieferungDatum
            return 0L
        }
        
        // Wiederholender Termin: Alte Logik
        val faelligAm = c.getFaelligAm()
        val faelligAmStart = getStartOfDay(faelligAm)
        if (c.geloeschteTermine.contains(faelligAmStart)) {
            if (c.wiederholen && c.letzterTermin > 0) {
                return c.letzterTermin + TimeUnit.DAYS.toMillis(c.intervallTage.toLong())
            }
            return faelligAm + TimeUnit.DAYS.toMillis(1)
        }
        return faelligAm
    }
    
    fun hatKundeTerminAmDatum(
        customer: Customer,
        liste: KundenListe? = null,
        viewDateStart: Long
    ): Boolean {
        // NEUE STRUKTUR: Verwende Intervalle-Liste
        if (customer.intervalle.isNotEmpty() || (customer.listeId.isNotEmpty() && liste != null)) {
            val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                liste = liste,
                startDatum = viewDateStart - TimeUnit.DAYS.toMillis(1),
                tageVoraus = 2
            )
            val alleGeloeschteTermine = if (liste != null) {
                (customer.geloeschteTermine + liste.geloeschteTermine).distinct()
            } else {
                customer.geloeschteTermine
            }
            return termine.any { termin ->
                val terminStart = com.example.we2026_5.util.TerminBerechnungUtils.getStartOfDay(termin.datum)
                terminStart == viewDateStart &&
                !TerminFilterUtils.istTerminGeloescht(termin.datum, alleGeloeschteTermine)
            }
        }
        
        // ALTE STRUKTUR: Rückwärtskompatibilität
        val faelligAm = customerFaelligAm(customer, liste, viewDateStart)
        val faelligAmStart = getStartOfDay(faelligAm)
        return faelligAmStart == viewDateStart && faelligAm > 0
    }
    
    fun istKundeUeberfaellig(
        customer: Customer,
        liste: KundenListe? = null,
        viewDateStart: Long,
        heuteStart: Long
    ): Boolean {
        val customerDone = customer.abholungErfolgt || customer.auslieferungErfolgt
        val listeDone = liste?.let { it.abholungErfolgt || it.auslieferungErfolgt } ?: false
        val isDone = customerDone || listeDone
        if (isDone) return false
        
        // NEUE STRUKTUR: Verwende Intervalle-Liste
        if (customer.intervalle.isNotEmpty() || (customer.listeId.isNotEmpty() && liste != null)) {
            if (viewDateStart > heuteStart) {
                return false
            }
            
            val termine = com.example.we2026_5.util.TerminBerechnungUtils.berechneAlleTermineFuerKunde(
                customer = customer,
                liste = liste,
                startDatum = heuteStart - TimeUnit.DAYS.toMillis(365),
                tageVoraus = 730
            )
            
            return termine.any { termin ->
                val terminStart = getStartOfDay(termin.datum)
                val istUeberfaellig = terminStart < heuteStart
                if (terminStart == viewDateStart) return@any false
                val sollAnzeigen = com.example.we2026_5.util.TerminFilterUtils.sollUeberfaelligAnzeigen(
                    terminDatum = termin.datum,
                    anzeigeDatum = viewDateStart,
                    aktuellesDatum = heuteStart
                )
                istUeberfaellig && sollAnzeigen
            }
        }
        
        // ALTE STRUKTUR: Rückwärtskompatibilität
        val faelligAm = customerFaelligAm(customer, liste, viewDateStart)
        val abholungStart = getStartOfDay(customer.abholungDatum)
        val auslieferungStart = getStartOfDay(customer.auslieferungDatum)
        val abholungUeberfaellig = !customer.abholungErfolgt && customer.abholungDatum > 0 && 
                                   abholungStart < heuteStart
        val auslieferungUeberfaellig = !customer.auslieferungErfolgt && customer.auslieferungDatum > 0 && 
                                      auslieferungStart < heuteStart
        val wiederholendUeberfaellig = customer.wiederholen && faelligAm < heuteStart && faelligAm > 0
        
        return (abholungUeberfaellig && viewDateStart >= abholungStart && viewDateStart <= heuteStart) ||
               (auslieferungUeberfaellig && viewDateStart >= auslieferungStart && viewDateStart <= heuteStart) ||
               (wiederholendUeberfaellig && viewDateStart >= faelligAm && viewDateStart <= heuteStart)
    }
    
    fun isIntervallFaelligAm(intervall: ListeIntervall, datum: Long): Boolean {
        val datumStart = getStartOfDay(datum)
        val abholungStart = getStartOfDay(intervall.abholungDatum)
        val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
        
        if (!intervall.wiederholen) {
            return datumStart == abholungStart || datumStart == auslieferungStart
        }
        
        val intervallTage = intervall.intervallTage.coerceIn(1, 365)
        
        if (datumStart >= abholungStart) {
            val tageSeitAbholung = TimeUnit.MILLISECONDS.toDays(datumStart - abholungStart)
            if (tageSeitAbholung <= 365 && tageSeitAbholung % intervallTage == 0L) {
                val erwartetesDatum = abholungStart + TimeUnit.DAYS.toMillis(tageSeitAbholung)
                if (datumStart == erwartetesDatum) {
                    return true
                }
            }
        } else {
            val tageBisAbholung = TimeUnit.MILLISECONDS.toDays(abholungStart - datumStart)
            if (tageBisAbholung <= 365 && datumStart == abholungStart) {
                return true
            }
        }
        
        if (datumStart >= auslieferungStart) {
            val tageSeitAuslieferung = TimeUnit.MILLISECONDS.toDays(datumStart - auslieferungStart)
            if (tageSeitAuslieferung <= 365 && tageSeitAuslieferung % intervallTage == 0L) {
                val erwartetesDatum = auslieferungStart + TimeUnit.DAYS.toMillis(tageSeitAuslieferung)
                if (datumStart == erwartetesDatum) {
                    return true
                }
            }
        } else {
            val tageBisAuslieferung = TimeUnit.MILLISECONDS.toDays(auslieferungStart - datumStart)
            if (tageBisAuslieferung <= 365 && datumStart == auslieferungStart) {
                return true
            }
        }
        
        return false
    }
    
    fun isIntervallFaelligInZukunft(intervall: ListeIntervall, abDatum: Long): Boolean {
        val abDatumStart = getStartOfDay(abDatum)
        val maxZukunft = abDatumStart + TimeUnit.DAYS.toMillis(365)
        
        if (!intervall.wiederholen) {
            val abholungStart = getStartOfDay(intervall.abholungDatum)
            val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
            return (abholungStart >= abDatumStart && abholungStart <= maxZukunft) ||
                   (auslieferungStart >= abDatumStart && auslieferungStart <= maxZukunft)
        }
        
        val intervallTage = intervall.intervallTage.coerceIn(1, 365)
        val abholungStart = getStartOfDay(intervall.abholungDatum)
        val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
        
        if (abDatumStart <= maxZukunft) {
            var naechsteAbholung = if (abDatumStart >= abholungStart) {
                val tageSeitAbholung = TimeUnit.MILLISECONDS.toDays(abDatumStart - abholungStart)
                val naechsterZyklus = ((tageSeitAbholung / intervallTage) + 1) * intervallTage
                abholungStart + TimeUnit.DAYS.toMillis(naechsterZyklus)
            } else {
                abholungStart
            }
            
            if (naechsteAbholung <= maxZukunft) {
                return true
            }
        }
        
        if (abDatumStart <= maxZukunft) {
            var naechsteAuslieferung = if (abDatumStart >= auslieferungStart) {
                val tageSeitAuslieferung = TimeUnit.MILLISECONDS.toDays(abDatumStart - auslieferungStart)
                val naechsterZyklus = ((tageSeitAuslieferung / intervallTage) + 1) * intervallTage
                auslieferungStart + TimeUnit.DAYS.toMillis(naechsterZyklus)
            } else {
                auslieferungStart
            }
            
            if (naechsteAuslieferung <= maxZukunft) {
                return true
            }
        }
        
        return false
    }
    
    fun getNaechstesListeDatum(liste: KundenListe, abDatum: Long = System.currentTimeMillis(), geloeschteTermine: List<Long> = emptyList()): Long? {
        val abDatumStart = getStartOfDay(abDatum)
        var naechstesDatum: Long? = null
        
        liste.intervalle.forEach { intervall ->
            if (!intervall.wiederholen) {
                val abholungStart = getStartOfDay(intervall.abholungDatum)
                val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
                
                if (abholungStart >= abDatumStart && !geloeschteTermine.contains(abholungStart) && (naechstesDatum == null || abholungStart < naechstesDatum!!)) {
                    naechstesDatum = abholungStart
                }
                if (auslieferungStart >= abDatumStart && !geloeschteTermine.contains(auslieferungStart) && (naechstesDatum == null || auslieferungStart < naechstesDatum!!)) {
                    naechstesDatum = auslieferungStart
                }
            } else {
                val intervallTage = intervall.intervallTage.coerceIn(1, 365)
                val abholungStart = getStartOfDay(intervall.abholungDatum)
                val auslieferungStart = getStartOfDay(intervall.auslieferungDatum)
                
                // Nächstes Abholungsdatum
                var naechsteAbholung: Long? = null
                if (abDatumStart >= abholungStart) {
                    val tageSeitAbholung = TimeUnit.MILLISECONDS.toDays(abDatumStart - abholungStart)
                    val zyklusAktuell = tageSeitAbholung / intervallTage
                    val aktuellesDatum = abholungStart + TimeUnit.DAYS.toMillis(zyklusAktuell * intervallTage)
                    val aktuellesDatumStart = getStartOfDay(aktuellesDatum)
                    
                    if (aktuellesDatumStart == abDatumStart && !geloeschteTermine.contains(aktuellesDatumStart)) {
                        naechsteAbholung = aktuellesDatumStart
                    } else {
                        var zyklus = (tageSeitAbholung / intervallTage + 1).toInt()
                        var versuche = 0
                        while (versuche < 100) {
                            val kandidat = abholungStart + TimeUnit.DAYS.toMillis((zyklus * intervallTage).toLong())
                            val kandidatStart = getStartOfDay(kandidat)
                            if (kandidatStart >= abDatumStart && !geloeschteTermine.contains(kandidatStart)) {
                                naechsteAbholung = kandidatStart
                                break
                            }
                            zyklus++
                            versuche++
                        }
                    }
                } else {
                    if (!geloeschteTermine.contains(abholungStart)) {
                        naechsteAbholung = abholungStart
                    } else {
                        var zyklus = 1
                        var versuche = 0
                        while (versuche < 100) {
                            val kandidat = abholungStart + TimeUnit.DAYS.toMillis((zyklus * intervallTage).toLong())
                            val kandidatStart = getStartOfDay(kandidat)
                            if (kandidatStart >= abDatumStart && !geloeschteTermine.contains(kandidatStart)) {
                                naechsteAbholung = kandidatStart
                                break
                            }
                            zyklus++
                            versuche++
                        }
                    }
                }
                
                if (naechsteAbholung != null && (naechstesDatum == null || naechsteAbholung < naechstesDatum!!)) {
                    naechstesDatum = naechsteAbholung
                }
                
                // Nächstes Auslieferungsdatum
                var naechsteAuslieferung: Long? = null
                if (abDatumStart >= auslieferungStart) {
                    val tageSeitAuslieferung = TimeUnit.MILLISECONDS.toDays(abDatumStart - auslieferungStart)
                    val zyklusAktuell = tageSeitAuslieferung / intervallTage
                    val aktuellesDatum = auslieferungStart + TimeUnit.DAYS.toMillis(zyklusAktuell * intervallTage)
                    val aktuellesDatumStart = getStartOfDay(aktuellesDatum)
                    
                    if (aktuellesDatumStart == abDatumStart && !geloeschteTermine.contains(aktuellesDatumStart)) {
                        naechsteAuslieferung = aktuellesDatumStart
                    } else {
                        var zyklus = (tageSeitAuslieferung / intervallTage + 1).toInt()
                        var versuche = 0
                        while (versuche < 100) {
                            val kandidat = auslieferungStart + TimeUnit.DAYS.toMillis((zyklus * intervallTage).toLong())
                            val kandidatStart = getStartOfDay(kandidat)
                            if (kandidatStart >= abDatumStart && !geloeschteTermine.contains(kandidatStart)) {
                                naechsteAuslieferung = kandidatStart
                                break
                            }
                            zyklus++
                            versuche++
                        }
                    }
                } else {
                    if (!geloeschteTermine.contains(auslieferungStart)) {
                        naechsteAuslieferung = auslieferungStart
                    } else {
                        var zyklus = 1
                        var versuche = 0
                        while (versuche < 100) {
                            val kandidat = auslieferungStart + TimeUnit.DAYS.toMillis((zyklus * intervallTage).toLong())
                            val kandidatStart = getStartOfDay(kandidat)
                            if (kandidatStart >= abDatumStart && !geloeschteTermine.contains(kandidatStart)) {
                                naechsteAuslieferung = kandidatStart
                                break
                            }
                            zyklus++
                            versuche++
                        }
                    }
                }
                
                if (naechsteAuslieferung != null && (naechstesDatum == null || naechsteAuslieferung < naechstesDatum!!)) {
                    naechstesDatum = naechsteAuslieferung
                }
            }
        }
        
        return naechstesDatum
    }
    
    fun getStartOfDay(ts: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
