package com.example.we2026_5.ui.customermanager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.we2026_5.Customer
import com.example.we2026_5.data.repository.CustomerRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerManagerViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var mockRepository: CustomerRepository
    private lateinit var viewModel: CustomerManagerViewModel
    
    @Before
    fun setup() {
        mockRepository = mock()
        viewModel = CustomerManagerViewModel(mockRepository)
    }
    
    @Test
    fun `loadCustomers updates customers LiveData`() = runTest {
        // Arrange
        val customers = listOf(
            Customer(id = "1", name = "Kunde 1"),
            Customer(id = "2", name = "Kunde 2")
        )
        whenever(mockRepository.getAllCustomers()).thenReturn(customers)
        
        // Act
        viewModel.loadCustomers()
        
        // Assert
        val result = viewModel.customers.value
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("Kunde 1", result[0].name)
    }
    
    @Test
    fun `filterCustomers filters by name`() = runTest {
        // Arrange
        val customers = listOf(
            Customer(id = "1", name = "Max Mustermann"),
            Customer(id = "2", name = "Anna Schmidt"),
            Customer(id = "3", name = "Peter Müller")
        )
        whenever(mockRepository.getAllCustomers()).thenReturn(customers)
        viewModel.loadCustomers()
        
        // Act
        viewModel.filterCustomers("Max")
        
        // Assert
        val result = viewModel.filteredCustomers.value
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("Max Mustermann", result[0].name)
    }
    
    @Test
    fun `filterCustomers filters by address`() = runTest {
        // Arrange
        val customers = listOf(
            Customer(id = "1", name = "Kunde 1", adresse = "Berlin"),
            Customer(id = "2", name = "Kunde 2", adresse = "München"),
            Customer(id = "3", name = "Kunde 3", adresse = "Hamburg")
        )
        whenever(mockRepository.getAllCustomers()).thenReturn(customers)
        viewModel.loadCustomers()
        
        // Act
        viewModel.filterCustomers("Berlin")
        
        // Assert
        val result = viewModel.filteredCustomers.value
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("Berlin", result[0].adresse)
    }
    
    @Test
    fun `filterCustomers returns all when query is empty`() = runTest {
        // Arrange
        val customers = listOf(
            Customer(id = "1", name = "Kunde 1"),
            Customer(id = "2", name = "Kunde 2")
        )
        whenever(mockRepository.getAllCustomers()).thenReturn(customers)
        viewModel.loadCustomers()
        
        // Act
        viewModel.filterCustomers("")
        
        // Assert
        val result = viewModel.filteredCustomers.value
        assertNotNull(result)
        assertEquals(2, result.size)
    }
}
