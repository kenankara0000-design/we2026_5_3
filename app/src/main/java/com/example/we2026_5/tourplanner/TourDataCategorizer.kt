package com.example.we2026_5.tourplanner

import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Helper-Klasse für Kategorisierungs-Logik in TourDataProcessor.
 * Extrahiert alle Kategorisierungs-Funktionen aus TourDataProcessor.
 */
class TourDataCategorizer {
    
    /**
     * Berechnet den Start des Tages (00:00:00) für einen Timestamp.
     */
    fun getStartOfDay(ts: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = ts
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    /**
     * Berechnet das nächste Datum für eine Liste basierend auf ihren Intervallen.
     */
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
}
