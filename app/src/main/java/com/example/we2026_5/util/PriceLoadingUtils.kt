package com.example.we2026_5.util

import com.example.we2026_5.data.repository.KundenPreiseRepository
import com.example.we2026_5.data.repository.ListenPrivatKundenpreiseRepository

/**
 * Gemeinsame Logik zum Laden von Brutto-Preisen für Belege/Erfassung.
 * Bevorzugt Kundenpreise; falls keine vorhanden, Listen- und Privat-Kundenpreise.
 */
object PriceLoadingUtils {

    /**
     * Lädt Brutto-Preise (articleId → priceGross) für einen Kunden.
     * Zuerst Kundenpreise; wenn leer, Listen- und Privat-Kundenpreise.
     */
    suspend fun loadBruttoPreiseForCustomer(
        customerId: String,
        kundenPreiseRepository: KundenPreiseRepository,
        listenPrivatKundenpreiseRepository: ListenPrivatKundenpreiseRepository
    ): Map<String, Double> {
        val kunden = kundenPreiseRepository.getKundenPreiseForCustomer(customerId)
            .associate { it.articleId to it.priceGross }
        if (kunden.isNotEmpty()) return kunden
        return listenPrivatKundenpreiseRepository.getListenPrivatKundenpreise()
            .associate { it.articleId to it.priceGross }
    }
}
