package com.example.we2026_5.data.repository

import com.example.we2026_5.Customer
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class CustomerRepository(
    private val database: FirebaseDatabase
) {
    private val customersRef: DatabaseReference = database.reference.child("customers")
    
    /**
     * Lädt alle Kunden als Flow (für LiveData/StateFlow)
     */
    fun getAllCustomersFlow(): Flow<List<Customer>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val customers = mutableListOf<Customer>()
                snapshot.children.forEach { child ->
                    val customer = child.getValue(Customer::class.java)
                    customer?.let { customers.add(it) }
                }
                // Sortieren nach Name
                customers.sortBy { it.name }
                trySend(customers)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        }
        
        customersRef.addValueEventListener(listener)
        
        awaitClose { customersRef.removeEventListener(listener) }
    }
    
    /**
     * Lädt alle Kunden einmalig
     */
    suspend fun getAllCustomers(): List<Customer> {
        val snapshot = customersRef.get().await()
        val customers = mutableListOf<Customer>()
        snapshot.children.forEach { child ->
            val customer = child.getValue(Customer::class.java)
            customer?.let { customers.add(it) }
        }
        // Sortieren nach Name
        return customers.sortedBy { it.name }
    }
    
    /**
     * Lädt einen einzelnen Kunden
     */
    suspend fun getCustomerById(customerId: String): Customer? {
        val snapshot = customersRef.child(customerId).get().await()
        return snapshot.getValue(Customer::class.java)
    }
    
    /**
     * Speichert einen neuen Kunden
     */
    suspend fun saveCustomer(customer: Customer): Boolean {
        return try {
            // Realtime Database speichert sofort lokal im Offline-Modus
            val task = customersRef.child(customer.id).setValue(customer)
            
            // Versuchen, auf Abschluss zu warten, aber mit Timeout (2 Sekunden)
            // Im Offline-Modus ist die lokale Speicherung bereits sofort erfolgt
            try {
                withTimeout(2000) {
                    task.await()
                }
                android.util.Log.d("CustomerRepository", "Save completed successfully")
                true
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // Timeout: Realtime Database hat bereits lokal gespeichert (im Offline-Modus)
                // Die Daten sind sicher lokal gespeichert, auch wenn Server-Verbindung fehlt
                android.util.Log.d("CustomerRepository", "Save completed (timeout, but saved locally)")
                true
            }
        } catch (e: Exception) {
            // Nur echte Fehler werden als Fehler behandelt
            android.util.Log.e("CustomerRepository", "Error saving customer", e)
            false
        }
    }
    
    /**
     * Aktualisiert einen Kunden
     */
    suspend fun updateCustomer(customerId: String, updates: Map<String, Any>): Boolean {
        return try {
            // Realtime Database speichert sofort lokal im Offline-Modus
            val task = customersRef.child(customerId).updateChildren(updates)
            
            // Versuchen, auf Abschluss zu warten, aber mit Timeout (2 Sekunden)
            // Im Offline-Modus ist die lokale Aktualisierung bereits sofort erfolgt
            try {
                withTimeout(2000) {
                    task.await()
                }
                android.util.Log.d("CustomerRepository", "Update completed successfully")
                true
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // Timeout: Realtime Database hat bereits lokal aktualisiert (im Offline-Modus)
                // Die Daten sind sicher lokal aktualisiert, auch wenn Server-Verbindung fehlt
                android.util.Log.d("CustomerRepository", "Update completed (timeout, but updated locally)")
                true
            }
        } catch (e: Exception) {
            // Nur echte Fehler werden als Fehler behandelt
            android.util.Log.e("CustomerRepository", "Error updating customer", e)
            false
        }
    }
    
    /**
     * Löscht einen Kunden
     */
    suspend fun deleteCustomer(customerId: String): Boolean {
        return try {
            // Realtime Database speichert sofort lokal im Offline-Modus
            val task = customersRef.child(customerId).removeValue()
            
            // Versuchen, auf Abschluss zu warten, aber mit Timeout (2 Sekunden)
            // Im Offline-Modus ist die lokale Löschung bereits sofort erfolgt
            try {
                withTimeout(2000) {
                    task.await()
                }
                android.util.Log.d("CustomerRepository", "Delete completed successfully")
                true
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // Timeout: Realtime Database hat bereits lokal gelöscht (im Offline-Modus)
                // Die Daten sind sicher lokal gelöscht, auch wenn Server-Verbindung fehlt
                android.util.Log.d("CustomerRepository", "Delete completed (timeout, but deleted locally)")
                true
            }
        } catch (e: Exception) {
            // Nur echte Fehler werden als Fehler behandelt
            android.util.Log.e("CustomerRepository", "Error deleting customer", e)
            false
        }
    }
    
    /**
     * Erstellt einen ValueEventListener für Echtzeit-Updates
     */
    fun addCustomersListener(
        onUpdate: (List<Customer>) -> Unit,
        onError: (Exception) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val customers = mutableListOf<Customer>()
                snapshot.children.forEach { child ->
                    val customer = child.getValue(Customer::class.java)
                    customer?.let { customers.add(it) }
                }
                // Sortieren nach Name
                customers.sortBy { it.name }
                onUpdate(customers)
            }
            
            override fun onCancelled(error: DatabaseError) {
                onError(Exception(error.message))
            }
        }
        
        customersRef.addValueEventListener(listener)
        return listener
    }
    
    /**
     * Erstellt einen ValueEventListener für einen einzelnen Kunden (Echtzeit-Updates)
     */
    fun addCustomerListener(
        customerId: String,
        onUpdate: (Customer?) -> Unit,
        onError: (Exception) -> Unit
    ): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val customer = snapshot.getValue(Customer::class.java)
                onUpdate(customer)
            }
            
            override fun onCancelled(error: DatabaseError) {
                onError(Exception(error.message))
            }
        }
        
        customersRef.child(customerId).addValueEventListener(listener)
        return listener
    }
    
    /**
     * Entfernt einen Listener
     */
    fun removeListener(listener: ValueEventListener) {
        customersRef.removeEventListener(listener)
    }
}
