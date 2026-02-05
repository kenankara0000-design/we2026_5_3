package com.example.we2026_5.tourplanner

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe

/**
 * Füllt Wochentagslisten (wochentag 0..6): Gruppierung nach Tag, nur G/P ohne listeId.
 */
interface WochentagslistenProcessor {
    fun fill(
        allCustomers: List<Customer>,
        allListen: List<KundenListe>,
        listenMitKunden: MutableMap<String, List<Customer>>,
        viewDateStart: Long,
        heuteStart: Long
    )
}
