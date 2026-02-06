package com.example.we2026_5.util

import com.example.we2026_5.CustomerStatus
import com.example.we2026_5.data.repository.CustomerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Beim App-Start: Kunden, deren Pause abgelaufen ist (pauseEnde > 0 und jetzt > pauseEnde),
 * werden automatisch wieder auf AKTIV gesetzt (status, pauseStart, pauseEnde, reaktivierungsDatum).
 */
suspend fun runPauseExpiredReset(repository: CustomerRepository) {
    withContext(Dispatchers.IO) {
        try {
            val now = System.currentTimeMillis()
            val customers = repository.getAllCustomers()
            val toReset = customers.filter { c ->
                c.status == CustomerStatus.PAUSIERT && c.pauseEnde > 0L && now > c.pauseEnde
            }
            toReset.forEach { customer ->
                repository.updateCustomer(
                    customer.id,
                    mapOf(
                        "status" to CustomerStatus.AKTIV.name,
                        "pauseStart" to 0L,
                        "pauseEnde" to 0L,
                        "reaktivierungsDatum" to now
                    )
                )
            }
        } catch (_: Exception) {
            // Beim nächsten App-Start erneut versuchen
        }
    }
}
