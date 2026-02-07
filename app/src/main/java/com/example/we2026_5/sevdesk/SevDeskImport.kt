package com.example.we2026_5.sevdesk

import android.content.Context
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.data.repository.ArticleRepository
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.wasch.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Importiert Kontakte und Artikel von SevDesk in die App.
 * Namen von SevDesk → Customer.name; Alias bleibt leer (Nutzer ergänzt).
 */
class SevDeskImport(
    private val context: Context,
    private val customerRepository: CustomerRepository,
    private val articleRepository: ArticleRepository
) {

    /**
     * Importiert Kontakte von SevDesk: Neue werden angelegt, bestehende (gleiche sevdesk_<id>) werden
     * nur in Name, Adresse, PLZ, Stadt aktualisiert (Merge). Alias, Notizen, Termine etc. bleiben unverändert.
     */
    suspend fun importContacts(token: String): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val contacts = SevDeskApi.getContacts(token)
            val existing = customerRepository.getAllCustomers()
            val byKundennummer = existing.filter { it.kundennummer.startsWith("sevdesk_") }.map { it.kundennummer to it }.toMap()
            val deletedIds = SevDeskDeletedIds.getDeletedIds(context)
            var created = 0
            var updated = 0
            for (c in contacts) {
                val key = "sevdesk_${c.id}"
                val safeName = c.name.takeIf { it.isNotBlank() && it.lowercase() != "null" } ?: ""
                val existingCustomer = byKundennummer[key]
                if (existingCustomer != null) {
                    val updates = mutableMapOf<String, Any>(
                        "adresse" to c.adresse,
                        "plz" to c.plz,
                        "stadt" to c.stadt
                    )
                    if (safeName.isNotBlank()) updates["name"] = safeName
                    val ok = customerRepository.updateCustomer(existingCustomer.id, updates)
                    if (ok) updated++
                } else if (c.id in deletedIds) {
                    // Vom Nutzer gelöscht – nicht wieder anlegen
                } else if (safeName.isNotBlank()) {
                    val customer = Customer(
                        id = UUID.randomUUID().toString(),
                        name = safeName,
                        alias = "",
                        adresse = c.adresse,
                        plz = c.plz,
                        stadt = c.stadt,
                        kundennummer = key,
                        erstelltAm = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis()),
                        kundenTyp = KundenTyp.REGELMAESSIG
                    )
                    if (customerRepository.saveCustomer(customer)) created++
                }
            }
            Result.success(created to updated)
        } catch (e: SevDeskApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importArticles(token: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val parts = SevDeskApi.getParts(token)
            val existing = articleRepository.getAllArticles()
            val bySevDeskId = existing.mapNotNull { it.sevDeskId?.takeIf { id -> id.isNotBlank() }?.let { id -> id to it } }.toMap()
            var imported = 0
            for (p in parts) {
                if (p.id in bySevDeskId) continue
                val article = Article(
                    id = "",
                    name = p.name,
                    preis = p.price,
                    einheit = p.unit,
                    sevDeskId = p.id
                )
                if (articleRepository.saveArticle(article)) imported++
            }
            Result.success(imported)
        } catch (e: SevDeskApiException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
