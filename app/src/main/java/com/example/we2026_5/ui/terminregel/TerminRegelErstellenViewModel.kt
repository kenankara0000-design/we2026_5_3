package com.example.we2026_5.ui.terminregel

import androidx.lifecycle.ViewModel
import com.example.we2026_5.TerminRegel
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.util.TerminRegelManager

/**
 * ViewModel für Erstellen/Bearbeiten einer Termin-Regel.
 * Kapselt Laden, Speichern, Löschen und Prüfung der Regel.
 */
class TerminRegelErstellenViewModel(
    private val regelRepository: TerminRegelRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    suspend fun loadRegel(regelId: String): TerminRegel? =
        regelRepository.getRegelById(regelId)

    suspend fun saveRegel(regel: TerminRegel): Boolean =
        regelRepository.saveRegel(regel)

    suspend fun deleteRegel(regelId: String): Boolean =
        regelRepository.deleteRegel(regelId)

    /**
     * Prüft, ob eine Regel von mindestens einem Kunden verwendet wird.
     */
    suspend fun istRegelVerwendet(regelId: String): Boolean {
        return try {
            val allCustomers = customerRepository.getAllCustomers()
            allCustomers.any { customer ->
                customer.intervalle.any { intervall ->
                    intervall.terminRegelId == regelId
                }
            }
        } catch (e: Exception) {
            true // Bei Fehler sicher annehmen, dass Regel verwendet wird
        }
    }

    /**
     * Aktualisiert alle Kunden, die die bearbeitete Regel verwenden.
     */
    suspend fun aktualisiereBetroffeneKunden(regel: TerminRegel) {
        try {
            val allCustomers = customerRepository.getAllCustomers()
            allCustomers.forEach { customer ->
                val intervallIndex = customer.intervalle.indexOfFirst { it.terminRegelId == regel.id }
                if (intervallIndex != -1) {
                    val neuesIntervall = TerminRegelManager.wendeRegelAufKundeAn(regel, customer)
                    val aktualisierteIntervalle = customer.intervalle.toMutableList()
                    aktualisierteIntervalle[intervallIndex] = neuesIntervall.copy(id = customer.intervalle[intervallIndex].id)
                    val intervalleMap = aktualisierteIntervalle.mapIndexed { _, intervall ->
                        mapOf(
                            "id" to intervall.id,
                            "abholungDatum" to intervall.abholungDatum,
                            "auslieferungDatum" to intervall.auslieferungDatum,
                            "wiederholen" to intervall.wiederholen,
                            "intervallTage" to intervall.intervallTage,
                            "intervallAnzahl" to intervall.intervallAnzahl,
                            "erstelltAm" to intervall.erstelltAm,
                            "terminRegelId" to intervall.terminRegelId
                        )
                    }
                    customerRepository.updateCustomer(customer.id, mapOf("intervalle" to intervalleMap))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TerminRegelErstellenVM", "Fehler beim Aktualisieren der Kunden", e)
        }
    }
}
