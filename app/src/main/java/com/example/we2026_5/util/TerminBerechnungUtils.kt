package com.example.we2026_5.util

import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.VerschobenerTermin
import com.example.we2026_5.TerminTyp
import java.util.concurrent.TimeUnit

/**
 * Utility-Klasse für Termin-Berechnungen
 * Unterstützt sowohl alte (einzelne Felder) als auch neue (intervalle-Liste) Struktur
 */
object TerminBerechnungUtils {
    
    /**
     * Normalisiert ein Datum auf Tagesanfang (00:00:00)
     */
    fun getStartOfDay(timestamp: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
    
    /**
     * Prüft ob ein Termin verschoben wurde
     */
    fun istTerminVerschoben(
        terminDatum: Long,
        verschobeneTermine: List<VerschobenerTermin>,
        intervallId: String? = null
    ): VerschobenerTermin? {
        val terminStart = getStartOfDay(terminDatum)
        return verschobeneTermine.firstOrNull { verschoben ->
            val originalStart = getStartOfDay(verschoben.originalDatum)
            originalStart == terminStart && 
            (verschoben.intervallId == null || verschoben.intervallId == intervallId)
        }
    }
    
    /**
     * Prüft ob ein Termin gelöscht wurde
     */
    fun istTerminGeloescht(terminDatum: Long, geloeschteTermine: List<Long>): Boolean {
        val terminStart = getStartOfDay(terminDatum)
        return geloeschteTermine.contains(terminStart)
    }
    
    /**
     * Prüft ob ein Termin im Urlaub liegt
     */
    fun istTerminImUrlaub(terminDatum: Long, urlaubVon: Long, urlaubBis: Long): Boolean {
        if (urlaubVon == 0L || urlaubBis == 0L) return false
        return terminDatum in urlaubVon..urlaubBis
    }
    
    /**
     * Berechnet alle Termine für einen Zeitraum (365 Tage) für ein CustomerIntervall
     */
    fun berechneTermineFuerIntervall(
        intervall: CustomerIntervall,
        startDatum: Long = System.currentTimeMillis(),
        tageVoraus: Int = 365,
        geloeschteTermine: List<Long> = emptyList(),
        verschobeneTermine: List<VerschobenerTermin> = emptyList()
    ): List<TerminInfo> {
        val termine = mutableListOf<TerminInfo>()
        val startDatumStart = getStartOfDay(startDatum)
        val endDatum = startDatumStart + TimeUnit.DAYS.toMillis(tageVoraus.toLong())
        
        // Abholungstermine
        if (intervall.abholungDatum > 0) {
            val abholungTermine = berechneWiederholendeTermine(
                startDatum = intervall.abholungDatum,
                wiederholen = intervall.wiederholen,
                intervallTage = intervall.intervallTage,
                startDatumStart = startDatumStart,
                endDatum = endDatum,
                geloeschteTermine = geloeschteTermine,
                verschobeneTermine = verschobeneTermine.filter { it.typ == TerminTyp.ABHOLUNG },
                intervallId = intervall.id,
                typ = TerminTyp.ABHOLUNG
            )
            termine.addAll(abholungTermine)
        }
        
        // Auslieferungstermine
        if (intervall.auslieferungDatum > 0) {
            val auslieferungTermine = berechneWiederholendeTermine(
                startDatum = intervall.auslieferungDatum,
                wiederholen = intervall.wiederholen,
                intervallTage = intervall.intervallTage,
                startDatumStart = startDatumStart,
                endDatum = endDatum,
                geloeschteTermine = geloeschteTermine,
                verschobeneTermine = verschobeneTermine.filter { it.typ == TerminTyp.AUSLIEFERUNG },
                intervallId = intervall.id,
                typ = TerminTyp.AUSLIEFERUNG
            )
            termine.addAll(auslieferungTermine)
        }
        
        return termine.sortedBy { it.datum }
    }
    
    /**
     * Berechnet wiederholende Termine für ein Intervall
     */
    private fun berechneWiederholendeTermine(
        startDatum: Long,
        wiederholen: Boolean,
        intervallTage: Int,
        startDatumStart: Long,
        endDatum: Long,
        geloeschteTermine: List<Long>,
        verschobeneTermine: List<VerschobenerTermin>,
        intervallId: String,
        typ: TerminTyp
    ): List<TerminInfo> {
        val termine = mutableListOf<TerminInfo>()
        val startDatumStartNormalized = getStartOfDay(startDatum)
        
        if (!wiederholen) {
            // Einmaliger Termin
            if (startDatumStartNormalized >= startDatumStart && startDatumStartNormalized <= endDatum) {
                // Prüfe ob verschoben
                val verschoben = istTerminVerschoben(startDatumStartNormalized, verschobeneTermine, intervallId)
                val finalDatum = verschoben?.verschobenAufDatum ?: startDatumStartNormalized
                
                // Prüfe ob gelöscht
                if (!istTerminGeloescht(finalDatum, geloeschteTermine)) {
                    termine.add(TerminInfo(
                        datum = finalDatum,
                        typ = typ,
                        intervallId = intervallId,
                        verschoben = verschoben != null,
                        originalDatum = if (verschoben != null) startDatumStartNormalized else null
                    ))
                }
            }
        } else {
            // Wiederholender Termin
            var aktuellesDatum = startDatumStartNormalized
            val intervallTageSafe = intervallTage.coerceIn(1, 365)
            var versuche = 0
            val maxVersuche = 1000 // Sicherheit gegen Endlosschleifen
            
            while (aktuellesDatum <= endDatum && versuche < maxVersuche) {
                // Prüfe ob verschoben
                val verschoben = istTerminVerschoben(aktuellesDatum, verschobeneTermine, intervallId)
                val finalDatum = verschoben?.verschobenAufDatum ?: aktuellesDatum
                
                // Prüfe ob gelöscht
                if (!istTerminGeloescht(finalDatum, geloeschteTermine)) {
                    if (finalDatum >= startDatumStart && finalDatum <= endDatum) {
                        termine.add(TerminInfo(
                            datum = finalDatum,
                            typ = typ,
                            intervallId = intervallId,
                            verschoben = verschoben != null,
                            originalDatum = if (verschoben != null) aktuellesDatum else null
                        ))
                    }
                }
                
                aktuellesDatum += TimeUnit.DAYS.toMillis(intervallTageSafe.toLong())
                versuche++
            }
        }
        
        return termine
    }
    
    /**
     * Berechnet alle Termine für einen Kunden (365 Tage)
     * Unterstützt sowohl alte als auch neue Struktur
     */
    fun berechneAlleTermineFuerKunde(
        customer: Customer,
        liste: KundenListe? = null,
        startDatum: Long = System.currentTimeMillis(),
        tageVoraus: Int = 365
    ): List<TerminInfo> {
        val alleTermine = mutableListOf<TerminInfo>()
        
        // NEUE STRUKTUR: Intervalle-Liste
        if (customer.intervalle.isNotEmpty()) {
            customer.intervalle.forEach { intervall ->
                val termine = berechneTermineFuerIntervall(
                    intervall = intervall,
                    startDatum = startDatum,
                    tageVoraus = tageVoraus,
                    geloeschteTermine = customer.geloeschteTermine,
                    verschobeneTermine = customer.verschobeneTermine
                )
                alleTermine.addAll(termine)
            }
        } else if (customer.listeId.isNotEmpty() && liste != null) {
            // Kunde in Liste: Verwende Listen-Intervalle
            // Kombiniere gelöschte und verschobene Termine von Kunde UND Liste
            val alleGeloeschteTermine = (customer.geloeschteTermine + liste.geloeschteTermine).distinct()
            val alleVerschobeneTermine = (customer.verschobeneTermine + liste.verschobeneTermine).distinctBy { 
                it.originalDatum to it.verschobenAufDatum to it.typ
            }
            
            liste.intervalle.forEach { listeIntervall ->
                // Konvertiere ListeIntervall zu CustomerIntervall für Berechnung
                val customerIntervall = CustomerIntervall(
                    id = "", // Liste-Intervalle haben keine ID
                    abholungDatum = listeIntervall.abholungDatum,
                    auslieferungDatum = listeIntervall.auslieferungDatum,
                    wiederholen = listeIntervall.wiederholen,
                    intervallTage = listeIntervall.intervallTage
                )
                val termine = berechneTermineFuerIntervall(
                    intervall = customerIntervall,
                    startDatum = startDatum,
                    tageVoraus = tageVoraus,
                    geloeschteTermine = alleGeloeschteTermine,
                    verschobeneTermine = alleVerschobeneTermine
                )
                alleTermine.addAll(termine)
            }
        } else {
            // ALTE STRUKTUR: Einzelne Felder (Rückwärtskompatibilität)
            if (customer.abholungDatum > 0 || customer.auslieferungDatum > 0) {
                val altesIntervall = CustomerIntervall(
                    id = "legacy",
                    abholungDatum = customer.abholungDatum,
                    auslieferungDatum = customer.auslieferungDatum,
                    wiederholen = customer.wiederholen,
                    intervallTage = customer.intervallTage
                )
                val termine = berechneTermineFuerIntervall(
                    intervall = altesIntervall,
                    startDatum = startDatum,
                    tageVoraus = tageVoraus,
                    geloeschteTermine = customer.geloeschteTermine,
                    verschobeneTermine = if (customer.verschobenAufDatum > 0) {
                        // Alte verschobenAufDatum Logik konvertieren
                        listOf(VerschobenerTermin(
                            originalDatum = customer.abholungDatum,
                            verschobenAufDatum = customer.verschobenAufDatum,
                            intervallId = null, // Alle Intervalle
                            typ = TerminTyp.ABHOLUNG
                        ))
                    } else {
                        customer.verschobeneTermine
                    }
                )
                alleTermine.addAll(termine)
            }
        }
        
        return alleTermine.sortedBy { it.datum }
    }
    
    /**
     * Prüft ob ein Termin überfällig ist
     */
    fun istUeberfaellig(
        terminDatum: Long,
        aktuellesDatum: Long = System.currentTimeMillis(),
        erledigt: Boolean
    ): Boolean {
        if (erledigt) return false
        val terminStart = getStartOfDay(terminDatum)
        val aktuellesStart = getStartOfDay(aktuellesDatum)
        return terminStart < aktuellesStart
    }
    
    /**
     * Prüft ob ein überfälliger Termin an einem bestimmten Datum angezeigt werden soll
     */
    fun sollUeberfaelligAnzeigen(
        terminDatum: Long,
        anzeigeDatum: Long,
        aktuellesDatum: Long = System.currentTimeMillis()
    ): Boolean {
        val terminStart = getStartOfDay(terminDatum)
        val anzeigeStart = getStartOfDay(anzeigeDatum)
        val aktuellesStart = getStartOfDay(aktuellesDatum)
        
        // Überfällig nur anzeigen: Ab Termin-Datum bis zum aktuellen Datum (nicht in Zukunft)
        return terminStart < aktuellesStart && anzeigeStart >= terminStart && anzeigeStart <= aktuellesStart
    }
}

/**
 * Repräsentiert einen Termin mit allen relevanten Informationen
 */
data class TerminInfo(
    val datum: Long,
    val typ: TerminTyp,
    val intervallId: String,
    val verschoben: Boolean = false,
    val originalDatum: Long? = null // Nur gesetzt wenn verschoben
)
