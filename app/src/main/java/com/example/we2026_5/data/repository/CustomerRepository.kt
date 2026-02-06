package com.example.we2026_5.data.repository

import com.example.we2026_5.Customer
import com.example.we2026_5.KundenTyp
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

    /** Alte Struktur-Felder werden nicht mehr geschrieben; optional aus DB lesen (Rückwärtskompatibilität). */
    private fun optionalLong(snapshot: DataSnapshot, key: String): Long =
        snapshotValueToLong(snapshot.child(key).getValue())

    private fun optionalInt(snapshot: DataSnapshot, key: String): Int =
        (snapshot.child(key).getValue(Any::class.java) as? Number)?.toInt() ?: 7

    private fun optionalBoolean(snapshot: DataSnapshot, key: String): Boolean =
        snapshot.child(key).getValue(Boolean::class.java) ?: false

    /**
     * Parst einen Kunden aus dem Snapshot und liest verschobeneTermine sowie
     * Wochentags-Listen manuell (Firebase Realtime DB liefert Listen oft als Map "0","1",…
     * und füllt dann List&lt;Int&gt; nicht zuverlässig).
     * Alte Struktur-Felder (abholungDatum, …) werden optional aus dem Snapshot gelesen (@Exclude-Felder).
     */
    private fun parseCustomerSnapshot(child: DataSnapshot, id: String): Customer? {
        val customer = child.getValue(Customer::class.java) ?: return null
        val verschobeneTermine = parseVerschobeneTermine(child.child("verschobeneTermine"))
        val abholungWochentage = parseIntListFromSnapshot(child.child("defaultAbholungWochentage"))
        val auslieferungWochentage = parseIntListFromSnapshot(child.child("defaultAuslieferungWochentage"))
        // kundenTyp explizit aus Snapshot lesen (Firebase setzt Enum aus String oft nicht zuverlässig)
        val kundenTypNode = child.child("kundenTyp")
        val kundenTypStr = when {
            !kundenTypNode.exists() -> null
            else -> kundenTypNode.getValue(String::class.java)
                ?: kundenTypNode.getValue(Any::class.java)?.toString()
        }
        val kundenTypParsed = kundenTypStr?.trim()?.let { s ->
            try { KundenTyp.valueOf(s) } catch (_: Exception) { null }
        }
        val base = customer.copy(
            id = id,
            verschobeneTermine = verschobeneTermine,
            defaultAbholungWochentage = abholungWochentage.ifEmpty { customer.defaultAbholungWochentage },
            defaultAuslieferungWochentage = auslieferungWochentage.ifEmpty { customer.defaultAuslieferungWochentage },
            abholungDatum = optionalLong(child, "abholungDatum"),
            auslieferungDatum = optionalLong(child, "auslieferungDatum"),
            wiederholen = optionalBoolean(child, "wiederholen"),
            intervallTage = optionalInt(child, "intervallTage").coerceIn(1, 365).takeIf { it in 1..365 } ?: 7,
            letzterTermin = optionalLong(child, "letzterTermin"),
            kundenTyp = kundenTypParsed ?: customer.kundenTyp
        )
        // Migration nur, wenn kundenTyp in Firebase fehlt (Legacy). Sonst gespeicherten Typ beibehalten.
        return if (child.child("kundenTyp").exists()) base else base.migrateKundenTyp()
    }

    /** Liest eine Liste von Int aus einem Snapshot (Realtime DB speichert Listen als Map "0", "1", …). */
    private fun parseIntListFromSnapshot(snapshot: DataSnapshot): List<Int> {
        if (!snapshot.exists()) return emptyList()
        val list = mutableListOf<Int>()
        snapshot.children.forEach { entry ->
            val v = entry.getValue(Any::class.java)
            val i = when (v) {
                is Number -> v.toInt()
                else -> null
            }
            if (i != null && i in 0..6) list.add(i)
        }
        return list.sorted()
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
    
    /** Deprecated-Felder werden nicht mehr geschrieben (Phase 3.7/3.8). */
    private fun withoutDeprecatedCustomerFields(updates: Map<String, Any>): Map<String, Any> =
        updates.filterKeys { it !in setOf("abholungDatum", "auslieferungDatum", "wiederholen", "intervallTage", "letzterTermin") }

    /**
     * Aktualisiert einen Kunden
     */
    override suspend fun updateCustomer(customerId: String, updates: Map<String, Any>): Boolean {
        return try {
            val filtered = withoutDeprecatedCustomerFields(updates)
            if (filtered.isEmpty()) return true
            // Realtime Database speichert sofort lokal im Offline-Modus
            val task = customersRef.child(customerId).updateChildren(filtered)
            
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
            val filtered = withoutDeprecatedCustomerFields(updates)
            if (filtered.isEmpty()) return Result.Success(true)
            val task = customersRef.child(customerId).updateChildren(filtered)
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

    override suspend fun deleteAllSevDeskContacts(): Result<Pair<Int, List<String>>> {
        return try {
            val all = getAllCustomers()
            val toDelete = all.filter { it.kundennummer.startsWith("sevdesk_") }
            val kundennummern = mutableListOf<String>()
            var deleted = 0
            for (c in toDelete) {
                if (deleteCustomer(c.id)) {
                    deleted++
                    kundennummern.add(c.kundennummer)
                }
            }
            Result.Success(deleted to kundennummern)
        } catch (e: Exception) {
            android.util.Log.e("CustomerRepository", "Error deleting SevDesk contacts", e)
            Result.Error(AppErrorMapper.toDeleteMessage(e))
        }
    }

    /**
     * Einmalige Bereinigung: Entfernt die alten Struktur-Felder (abholungDatum, auslieferungDatum,
     * wiederholen, intervallTage, letzterTermin) aus allen Kunden in Firebase.
     * In Firebase Realtime DB entfernt updateChildren mit Wert null den Key.
     * @return Anzahl der Kunden, bei denen die Felder entfernt wurden
     */
    suspend fun removeDeprecatedFieldsFromFirebase(): Int {
        val snapshot = customersRef.get().await()
        val keysToRemove = mapOf<String, Any?>(
            "abholungDatum" to null,
            "auslieferungDatum" to null,
            "wiederholen" to null,
            "intervallTage" to null,
            "letzterTermin" to null
        )
        var count = 0
        snapshot.children.forEach { child ->
            val key = child.key ?: return@forEach
            try {
                withTimeout(3000) {
                    customersRef.child(key).updateChildren(keysToRemove).await()
                }
                count++
            } catch (_: Exception) {
                // Einzelner Fehler: überspringen, nächsten Kunden versuchen
            }
        }
        return count
    }

    private val deprecatedFieldKeys = listOf("abholungDatum", "auslieferungDatum", "wiederholen", "intervallTage", "letzterTermin")

    /**
     * Prüft in Firebase, ob noch Kunden existieren, bei denen eines der veralteten Felder gesetzt ist.
     * @return Liste der Kunden-IDs, die mindestens eines der Felder noch haben
     */
    suspend fun getCustomerIdsWithDeprecatedFields(): List<String> {
        val snapshot = customersRef.get().await()
        val ids = mutableListOf<String>()
        snapshot.children.forEach { child ->
            val id = child.key ?: return@forEach
            val hasAny = deprecatedFieldKeys.any { child.child(it).exists() }
            if (hasAny) ids.add(id)
        }
        return ids
    }
}
