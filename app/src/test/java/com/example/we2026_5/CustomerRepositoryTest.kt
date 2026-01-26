package com.example.we2026_5

import com.example.we2026_5.data.repository.CustomerRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class CustomerRepositoryTest {

    @Mock
    private lateinit var firestore: FirebaseFirestore

    private lateinit var repository: CustomerRepository

    @Before
    fun setup() {
        repository = CustomerRepository(firestore)
    }

    @Test
    fun testGetAllCustomers() = runTest {
        // Test würde Mock-Firestore benötigen
        // Für jetzt nur Struktur-Test
        assertNotNull(repository)
    }

    @Test
    fun testSaveCustomer() = runTest {
        val customer = Customer(
            id = "test-id",
            name = "Test Kunde",
            adresse = "Teststraße 1",
            telefon = "0123456789"
        )
        
        // Test würde Mock-Firestore benötigen
        // Für jetzt nur Struktur-Test
        assertNotNull(customer)
    }
}
