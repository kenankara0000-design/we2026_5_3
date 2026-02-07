package com.example.we2026_5.data.repository

import com.example.we2026_5.Customer
import com.example.we2026_5.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Interface für Kunden-Datenzugriff.
 * Ermöglicht Fakes/Mocks für Unit-Tests ohne Firebase.
 */
interface CustomerRepositoryInterface {

    fun getAllCustomersFlow(): Flow<List<Customer>>
    /** Nur Kunden mit ohneTour == false (für TourPlanner, skaliert bei vielen Kunden). */
    fun getCustomersForTourFlow(): Flow<List<Customer>>
    /** Echtzeit-Updates für einen einzelnen Kunden (für Detail-Screen). */
    fun getCustomerFlow(customerId: String): Flow<Customer?>
    suspend fun getAllCustomers(): List<Customer>
    suspend fun getCustomerById(customerId: String): Customer?
    suspend fun saveCustomer(customer: Customer): Boolean
    suspend fun updateCustomer(customerId: String, updates: Map<String, Any>): Boolean
    suspend fun deleteCustomer(customerId: String): Boolean

    /** Wie [updateCustomer], gibt aber [Result] für einheitliche Fehlerbehandlung im ViewModel zurück. */
    suspend fun updateCustomerResult(customerId: String, updates: Map<String, Any>): Result<Boolean>
    /** Wie [deleteCustomer], gibt aber [Result] für einheitliche Fehlerbehandlung im ViewModel zurück. */
    suspend fun deleteCustomerResult(customerId: String): Result<Boolean>

    /** Löscht alle Kunden, die aus SevDesk importiert wurden. @return (Anzahl gelöscht, Kundennummern der gelöschten) oder Fehler. */
    suspend fun deleteAllSevDeskContacts(): Result<Pair<Int, List<String>>>
}
