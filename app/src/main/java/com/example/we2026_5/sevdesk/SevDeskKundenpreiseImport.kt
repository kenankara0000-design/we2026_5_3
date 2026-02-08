package com.example.we2026_5.sevdesk

import com.example.we2026_5.data.repository.ArticleRepository
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenPreiseRepository
import com.example.we2026_5.wasch.KundenPreis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Importiert Kundenpreise (PartContactPrice) von SevDesk in die App.
 * Nur Einträge, bei denen sowohl Kunde als auch Artikel bereits in der App existieren
 * (Kunde: kundennummer = "sevdesk_&lt;contactId&gt;", Artikel: sevDeskId = partId).
 */
class SevDeskKundenpreiseImport(
    private val customerRepository: CustomerRepository,
    private val articleRepository: ArticleRepository,
    private val kundenPreiseRepository: KundenPreiseRepository
) {

    /**
     * Holt PartContactPrice von der API und schreibt gültige Einträge nach kundenPreise (Firebase).
     * @return Anzahl der Kunden, für die mindestens ein Preis übernommen wurde
     */
    suspend fun importKundenpreise(token: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val prices = SevDeskApi.getPartContactPrices(token)
            val customers = customerRepository.getAllCustomers()
                .filter { it.kundennummer.startsWith("sevdesk_") }
            val sevdeskIdToCustomerId = customers.associate { c ->
                c.kundennummer.removePrefix("sevdesk_") to c.id
            }
            val articles = articleRepository.getAllArticles()
            val sevdeskIdToArticleId = articles.mapNotNull { a ->
                a.sevDeskId?.takeIf { it.isNotBlank() }?.let { it to a.id }
            }.toMap()

            val byCustomer = mutableMapOf<String, MutableList<KundenPreis>>()
            for (p in prices) {
                val appCustomerId = sevdeskIdToCustomerId[p.contactId] ?: continue
                val appArticleId = sevdeskIdToArticleId[p.partId] ?: continue
                byCustomer.getOrPut(appCustomerId) { mutableListOf() }.add(
                    KundenPreis(
                        customerId = appCustomerId,
                        articleId = appArticleId,
                        priceNet = p.priceNet,
                        priceGross = p.priceGross
                    )
                )
            }

            var count = 0
            for ((customerId, preise) in byCustomer) {
                if (kundenPreiseRepository.setKundenPreiseForCustomer(customerId, preise)) {
                    count++
                }
            }
            Result.success(count)
        } catch (e: SevDeskApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
