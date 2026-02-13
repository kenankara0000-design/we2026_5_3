package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe

/**
 * Füllt Listen ohne Wochentag (listeId): Kunden mit zugeordneter Liste, fällig oder erledigt am Tag.
 */
interface TourListenProcessor {
    fun fill(
        kundenNachListen: Map<String, List<Customer>>,
        allListen: List<KundenListe>,
        listenMitKunden: MutableMap<String, List<Customer>>,
        viewDateStart: Long,
        heuteStart: Long
    )
}
