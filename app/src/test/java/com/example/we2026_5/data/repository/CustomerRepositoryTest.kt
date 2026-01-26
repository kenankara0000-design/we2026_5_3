package com.example.we2026_5.data.repository

import com.example.we2026_5.Customer
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CustomerRepositoryTest {
    
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockCollection: com.google.firebase.firestore.CollectionReference
    private lateinit var mockQuery: Query
    private lateinit var mockQuerySnapshot: QuerySnapshot
    private lateinit var repository: CustomerRepository
    
    @Before
    fun setup() {
        mockFirestore = mock()
        mockCollection = mock()
        mockQuery = mock()
        mockQuerySnapshot = mock()
        
        whenever(mockFirestore.collection("customers")).thenReturn(mockCollection)
        whenever(mockCollection.orderBy("name")).thenReturn(mockQuery)
        
        repository = CustomerRepository(mockFirestore)
    }
    
    @Test
    fun `getAllCustomers returns list of customers`() = runTest {
        // Arrange
        val mockTask: Task<QuerySnapshot> = mock()
        val mockDocument1 = mock<com.google.firebase.firestore.DocumentSnapshot>()
        val mockDocument2 = mock<com.google.firebase.firestore.DocumentSnapshot>()
        
        val customer1 = Customer(id = "1", name = "Kunde 1")
        val customer2 = Customer(id = "2", name = "Kunde 2")
        
        whenever(mockQuery.get()).thenReturn(mockTask)
        whenever(mockTask.isComplete).thenReturn(true)
        whenever(mockTask.isSuccessful).thenReturn(true)
        whenever(mockTask.result).thenReturn(mockQuerySnapshot)
        whenever(mockQuerySnapshot.documents).thenReturn(listOf(mockDocument1, mockDocument2))
        whenever(mockDocument1.toObject(Customer::class.java)).thenReturn(customer1)
        whenever(mockDocument2.toObject(Customer::class.java)).thenReturn(customer2)
        
        // Act
        val result = repository.getAllCustomers()
        
        // Assert
        assertEquals(2, result.size)
        assertEquals("Kunde 1", result[0].name)
        assertEquals("Kunde 2", result[1].name)
    }
    
    @Test
    fun `saveCustomer returns true on success`() = runTest {
        // Arrange
        val customer = Customer(id = "1", name = "Test Kunde")
        val mockDocument = mock<com.google.firebase.firestore.DocumentReference>()
        val mockTask: Task<Void> = mock()
        
        whenever(mockCollection.document("1")).thenReturn(mockDocument)
        whenever(mockDocument.set(customer)).thenReturn(mockTask)
        whenever(mockTask.isComplete).thenReturn(true)
        whenever(mockTask.isSuccessful).thenReturn(true)
        
        // Act
        val result = repository.saveCustomer(customer)
        
        // Assert
        assertTrue(result)
        verify(mockDocument).set(customer)
    }
    
    @Test
    fun `deleteCustomer returns true on success`() = runTest {
        // Arrange
        val customerId = "1"
        val mockDocument = mock<com.google.firebase.firestore.DocumentReference>()
        val mockTask: Task<Void> = mock()
        
        whenever(mockCollection.document(customerId)).thenReturn(mockDocument)
        whenever(mockDocument.delete()).thenReturn(mockTask)
        whenever(mockTask.isComplete).thenReturn(true)
        whenever(mockTask.isSuccessful).thenReturn(true)
        
        // Act
        val result = repository.deleteCustomer(customerId)
        
        // Assert
        assertTrue(result)
        verify(mockDocument).delete()
    }
}
