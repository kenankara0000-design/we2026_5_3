package com.example.we2026_5.data.repository

import com.example.we2026_5.Customer
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CustomerRepository(
    private val db: FirebaseFirestore
) {
    
    /**
     * Lädt alle Kunden als Flow (für LiveData/StateFlow)
     */
    fun getAllCustomersFlow(): Flow<List<Customer>> = callbackFlow {
        val listener = db.collection("customers")
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val customers = snapshot.toObjects(Customer::class.java)
                    trySend(customers)
                }
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Lädt alle Kunden einmalig
     */
    suspend fun getAllCustomers(): List<Customer> {
        val snapshot = db.collection("customers")
            .orderBy("name")
            .get()
            .await()
        return snapshot.toObjects(Customer::class.java)
    }
    
    /**
     * Lädt einen einzelnen Kunden
     */
    suspend fun getCustomerById(customerId: String): Customer? {
        val document = db.collection("customers")
            .document(customerId)
            .get()
            .await()
        return document.toObject(Customer::class.java)
    }
    
    /**
     * Speichert einen neuen Kunden
     */
    suspend fun saveCustomer(customer: Customer): Boolean {
        return try {
            db.collection("customers")
                .document(customer.id)
                .set(customer)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Aktualisiert einen Kunden
     */
    suspend fun updateCustomer(customerId: String, updates: Map<String, Any>): Boolean {
        return try {
            db.collection("customers")
                .document(customerId)
                .update(updates)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Löscht einen Kunden
     */
    suspend fun deleteCustomer(customerId: String): Boolean {
        return try {
            db.collection("customers")
                .document(customerId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Erstellt einen Snapshot-Listener für Echtzeit-Updates
     */
    fun addCustomersListener(
        onUpdate: (List<Customer>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("customers")
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val customers = snapshot.toObjects(Customer::class.java)
                    onUpdate(customers)
                }
            }
    }
    
    /**
     * Erstellt einen Snapshot-Listener für einen einzelnen Kunden (Echtzeit-Updates)
     */
    fun addCustomerListener(
        customerId: String,
        onUpdate: (Customer?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("customers")
            .document(customerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }
                
                val customer = snapshot?.toObject(Customer::class.java)
                onUpdate(customer)
            }
    }
}
