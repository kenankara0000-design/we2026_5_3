package com.example.we2026_5.data.repository

import com.example.we2026_5.Customer
import com.example.we2026_5.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Fake-Implementierung von CustomerRepositoryInterface für Unit-Tests.
 * Hält Kunden im Speicher; keine Firebase-Abhängigkeit.
 */
class FakeCustomerRepository : CustomerRepositoryInterface {

    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    override fun getAllCustomersFlow(): Flow<List<Customer>> = _customers

    override fun getCustomerFlow(customerId: String): Flow<Customer?> =
        _customers.map { list -> list.find { it.id == customerId } }

    override suspend fun getAllCustomers(): List<Customer> = _customers.value

    override suspend fun getCustomerById(customerId: String): Customer? =
        _customers.value.find { it.id == customerId }

    override suspend fun saveCustomer(customer: Customer): Boolean {
        _customers.value = _customers.value + customer
        return true
    }

    override suspend fun updateCustomer(customerId: String, updates: Map<String, Any>): Boolean {
        val index = _customers.value.indexOfFirst { it.id == customerId }
        if (index < 0) return false
        // Vereinfachte Aktualisierung für Tests
        _customers.value = _customers.value.toMutableList().apply { set(index, get(index)) }
        return true
    }

    override suspend fun deleteCustomer(customerId: String): Boolean {
        _customers.value = _customers.value.filter { it.id != customerId }
        return true
    }

    override suspend fun updateCustomerResult(customerId: String, updates: Map<String, Any>): Result<Boolean> {
        return if (updateCustomer(customerId, updates)) Result.Success(true)
        else Result.Error("Fehler beim Speichern. Bitte erneut versuchen.")
    }

    override suspend fun deleteCustomerResult(customerId: String): Result<Boolean> {
        return if (deleteCustomer(customerId)) Result.Success(true)
        else Result.Error("Fehler beim Löschen. Bitte erneut versuchen.")
    }

    /** Test-Helfer: Kunden setzen. */
    fun setCustomers(customers: List<Customer>) {
        _customers.value = customers.sortedBy { it.name }
    }
}
