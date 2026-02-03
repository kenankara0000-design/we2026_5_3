package com.example.we2026_5.data.repository

import com.example.we2026_5.Customer
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.util.migrateKundenTyp
import com.example.we2026_5.VerschobenerTermin
import com.example.we2026_5.util.AppErrorMapper
import com.example.we2026_5.util.Result
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
) : CustomerRepositoryInterface {
    private val customersRef: DatabaseReference = database.reference.child("customers")
    
    /**
     * Lädt alle Kunden als Flow (für LiveData/StateFlow)
     */
    override fun getAllCustomersFlow(): Flow<List<Customer>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val customers = mutableListOf<Customer>()
                snapshot.children.forEach { child ->
                    val key = child.key ?: return@forEach
                    val customer = parseCustomerSnapshot(child, key)
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
     * Echtzeit-Updates für einen einzelnen Kunden (für Detail-Screen).
     */
    override fun getCustomerFlow(customerId: String): Flow<Customer?> = callbackFlow {
        val ref = customersRef.child(customerId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val customer = parseCustomerSnapshot(snapshot, customerId)
                trySend(customer)
            }
            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
    
    /**
     * Lädt alle Kunden einmalig
     */
    override suspend fun getAllCustomers(): List<Customer> {
        val snapshot = customersRef.get().await()
        val customers = mutableListOf<Customer>()
        snapshot.children.forEach { child ->
            val key = child.key ?: return@forEach
            val customer = parseCustomerSnapshot(child, key)
            customer?.let { customers.add(it) }
        }
        // Sortieren nach Name
        return customers.sortedBy { it.name }
    }

    /**
     * Parst einen Kunden aus dem Snapshot und liest verschobeneTermine manuell,
     * damit Long-Felder auch bei Firebase-Double-Werten korrekt gesetzt werden.
     */
    private fun parseCustomerSnapshot(child: DataSnapshot, id: String): Customer? {
        val customer = child.getValue(Customer::class.java) ?: return null
        val verschobeneTermine = parseVerschobeneTermine(child.child("verschobeneTermine"))
        return customer.copy(id = id, verschobeneTermine = verschobeneTermine).migrateKundenTyp()
    }

    private fun parseVerschobeneTermine(snapshot: DataSnapshot): List<VerschobenerTermin> {
        if (!snapshot.exists()) return emptyList()
        val list = mutableListOf<VerschobenerTermin>()
        snapshot.children.forEach { entry ->
            val od = snapshotValueToLong(entry.child("originalDatum").getValue())
            val vd = snapshotValueToLong(entry.child("verschobenAufDatum").getValue())
            val typStr = entry.child("typ").getValue(String::class.java)
            val typ = when (typStr) {
                "AUSLIEFERUNG" -> TerminTyp.AUSLIEFERUNG
                else -> TerminTyp.ABHOLUNG // auch bei null (alte Einträge ohne typ)
            }
            if (od != 0L || vd != 0L) list.add(VerschobenerTermin(originalDatum = od, verschobenAufDatum = vd, typ = typ))
        }
        return list
    }

    /** Firebase liefert Zahlen oft als Double; Number::class.java wird nicht unterstützt. Rohwert zu Long. */
    private fun snapshotValueToLong(value: Any?): Long = when (value) {
        is Number -> value.toLong()
        else -> 0L
    }

    /** Serialisiert verschobeneTermine für Firebase (Map mit Keys "0","1",…; originalDatum, verschobenAufDatum, typ). */
    private fun serializeVerschobeneTermine(list: List<VerschobenerTermin>): Map<String, Map<String, Any>> =
        list.mapIndexed { index, it ->
            index.toString() to mapOf(
                "originalDatum" to it.originalDatum,
                "verschobenAufDatum" to it.verschobenAufDatum,
                "typ" to it.typ.name
            )
        }.toMap()
    
    /**
     * Lädt einen einzelnen Kunden
     */
    override suspend fun getCustomerById(customerId: String): Customer? {
        val snapshot = customersRef.child(customerId).get().await()
        return parseCustomerSnapshot(snapshot, customerId)
    }
    
    /**
     * Speichert einen neuen Kunden
     */
    override suspend fun saveCustomer(customer: Customer): Boolean {
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
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                // Timeout: Realtime Database hat bereits lokal gespeichert (im Offline-Modus)
                android.util.Log.d("CustomerRepository", "Save completed (timeout, but saved locally)")
            }
            // verschobeneTermine ist @Exclude und wird bei setValue nicht geschrieben – separat schreiben
            if (customer.verschobeneTermine.isNotEmpty()) {
                updateCustomer(customer.id, mapOf("verschobeneTermine" to serializeVerschobeneTermine(customer.verschobeneTermine)))
            }
            true
        } catch (e: Exception) {
            // Nur echte Fehler werden als Fehler behandelt
            android.util.Log.e("CustomerRepository", "Error saving customer", e)
            false
        }
    }
    
    /**
     * Aktualisiert einen Kunden
     */
    override suspend fun updateCustomer(customerId: String, updates: Map<String, Any>): Boolean {
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

    override suspend fun updateCustomerResult(customerId: String, updates: Map<String, Any>): Result<Boolean> {
        return try {
            val task = customersRef.child(customerId).updateChildren(updates)
            try {
                withTimeout(2000) { task.await() }
                Result.Success(true)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Result.Success(true)
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomerRepository", "Error updating customer", e)
            Result.Error(AppErrorMapper.toSaveMessage(e))
        }
    }
    
    /**
     * Löscht einen Kunden
     */
    override suspend fun deleteCustomer(customerId: String): Boolean {
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

    override suspend fun deleteCustomerResult(customerId: String): Result<Boolean> {
        return try {
            val task = customersRef.child(customerId).removeValue()
            try {
                withTimeout(2000) { task.await() }
                Result.Success(true)
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Result.Success(true)
            }
        } catch (e: Exception) {
            android.util.Log.e("CustomerRepository", "Error deleting customer", e)
            Result.Error(AppErrorMapper.toDeleteMessage(e))
        }
    }
}
