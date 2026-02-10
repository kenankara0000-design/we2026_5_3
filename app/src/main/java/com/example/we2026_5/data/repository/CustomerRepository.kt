package com.example.we2026_5.data.repository

import com.example.we2026_5.AusnahmeTermin
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenTermin
import com.example.we2026_5.tourplanner.TerminCache
import com.example.we2026_5.util.AppErrorMapper
import com.example.we2026_5.util.Result
import com.example.we2026_5.util.TerminBerechnungUtils
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class CustomerRepository(
    private val database: FirebaseDatabase,
    private val termincache: TerminCache
) : CustomerRepositoryInterface {
    private val customersRef: DatabaseReference = database.reference.child("customers")
    private val customersForTourRef: DatabaseReference = database.reference.child("customers_for_tour")
    private val tourIndexBackfillDone = AtomicBoolean(false)

    /** Wartet auf Task oder gibt bei Timeout (Offline) true zurück – Realtime DB speichert lokal. */
    private suspend fun awaitWithTimeout(ms: Long = 2000, block: suspend () -> Unit): Boolean {
        return try {
            withTimeout(ms) { block() }
            true
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            true
        }
    }

    private fun parseSnapshotToCustomers(snapshot: DataSnapshot): List<Customer> {
        val customers = mutableListOf<Customer>()
        snapshot.children.forEach { child ->
            val key = child.key ?: return@forEach
            val customer = CustomerSnapshotParser.parseCustomerSnapshot(child, key)
            customer?.let { customers.add(it) }
        }
        return customers.sortedBy { it.name }
    }

    /**
     * Lädt alle Kunden als Flow (für LiveData/StateFlow). Parsing auf Hintergrund-Thread.
     */
    override fun getAllCustomersFlow(): Flow<List<Customer>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(coroutineContext).launch(Dispatchers.Default) {
                    val customers = parseSnapshotToCustomers(snapshot)
                    trySend(customers)
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        }
        
        customersRef.addValueEventListener(listener)
        
        awaitClose { customersRef.removeEventListener(listener) }
    }

    /**
     * Füllt customers_for_tour einmalig aus allen Kunden (Migration für bestehende DBs).
     */
    private suspend fun ensureTourIndexFilled() {
        try {
            val all = getAllCustomers()
            for (c in all) {
                syncTourCustomer(c)
            }
        } catch (_: Exception) { }
    }

    /**
     * Lädt nur Tour-Kunden (ohneTour == false) für TourPlanner. Weniger Daten bei vielen Kunden (Punkt 5).
     */
    override fun getCustomersForTourFlow(): Flow<List<Customer>> = callbackFlow {
        kotlinx.coroutines.CoroutineScope(coroutineContext).launch(Dispatchers.Default) {
            if (tourIndexBackfillDone.compareAndSet(false, true)) {
                ensureTourIndexFilled()
            }
        }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(coroutineContext).launch(Dispatchers.Default) {
                    val customers = mutableListOf<Customer>()
                    snapshot.children.forEach { child ->
                        val key = child.key ?: return@forEach
                        CustomerSnapshotParser.parseCustomerSnapshot(child, key)?.let { customers.add(it) }
                    }
                    trySend(customers.sortedBy { it.name })
                }
            }
            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        }
        customersForTourRef.addValueEventListener(listener)
        awaitClose { customersForTourRef.removeEventListener(listener) }
    }

    /**
     * Synchronisiert einen Kunden in customers_for_tour: hinzufügen wenn !ohneTour, sonst entfernen.
     */
    private suspend fun syncTourCustomer(customer: Customer) {
        try {
            if (customer.ohneTour) {
                customersForTourRef.child(customer.id).removeValue().await()
            } else {
                customersForTourRef.child(customer.id).setValue(customer).await()
            }
        } catch (_: Exception) {
            // Offline/Fehler – Realtime DB puffert; kein Abbruch
        }
    }

    /**
     * Echtzeit-Updates für einen einzelnen Kunden (für Detail-Screen). Parsing auf Hintergrund-Thread.
     */
    override fun getCustomerFlow(customerId: String): Flow<Customer?> = callbackFlow {
        val ref = customersRef.child(customerId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                CoroutineScope(coroutineContext).launch(Dispatchers.Default) {
                    val customer = CustomerSnapshotParser.parseCustomerSnapshot(snapshot, customerId)
                    trySend(customer)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
    
    /**
     * Lädt alle Kunden einmalig. Parsing auf Hintergrund-Thread.
     */
    override suspend fun getAllCustomers(): List<Customer> {
        val snapshot = customersRef.get().await()
        return withContext(Dispatchers.Default) { parseSnapshotToCustomers(snapshot) }
    }

    /**
     * Lädt einen einzelnen Kunden. Parsing auf Hintergrund-Thread.
     */
    override suspend fun getCustomerById(customerId: String): Customer? {
        val snapshot = customersRef.child(customerId).get().await()
        return withContext(Dispatchers.Default) { CustomerSnapshotParser.parseCustomerSnapshot(snapshot, customerId) }
    }
    
    /**
     * Speichert einen neuen Kunden
     */
    override suspend fun saveCustomer(customer: Customer): Boolean {
        return try {
            awaitWithTimeout { customersRef.child(customer.id).setValue(customer).await() }
            syncTourCustomer(customer)
            if (customer.verschobeneTermine.isNotEmpty()) {
                updateCustomer(customer.id, mapOf("verschobeneTermine" to CustomerSnapshotParser.serializeVerschobeneTermine(customer.verschobeneTermine)))
            }
            if (customer.ausnahmeTermine.isNotEmpty()) {
                updateCustomer(customer.id, mapOf("ausnahmeTermine" to CustomerSnapshotParser.serializeAusnahmeTermine(customer.ausnahmeTermine)))
            }
            if (customer.kundenTermine.isNotEmpty()) {
                updateCustomer(customer.id, mapOf("kundenTermine" to CustomerSnapshotParser.serializeKundenTermine(customer.kundenTermine)))
            }
            if (customer.termineVonListe.isNotEmpty()) {
                updateCustomer(customer.id, mapOf("termineVonListe" to CustomerSnapshotParser.serializeKundenTermine(customer.termineVonListe)))
            }
            termincache.invalidate(customer.id)
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

    /** Felder, die die Tourenplaner-Ansicht beeinflussen – nach Update Index customers_for_tour mitsyncen. */
    private val tourRelevantKeys = setOf(
        "abholungErfolgt", "abholungErledigtAm", "auslieferungErfolgt", "auslieferungErledigtAm",
        "keinerWäscheErfolgt", "keinerWäscheErledigtAm", "geloeschteTermine", "verschobeneTermine",
        "urlaubVon", "urlaubBis", "ausnahmeTermine", "kundenTermine", "termineVonListe",
        "listeId", "intervalle",
        "defaultAbholungWochentag", "defaultAuslieferungWochentag", "defaultAbholungWochentage", "defaultAuslieferungWochentage"
    )

    /** Kunden, die der angegebenen Tour-Liste zugeordnet sind (für Sync der listenTermine → termineVonListe). */
    suspend fun getCustomersByListeId(listeId: String): List<Customer> =
        getAllCustomers().filter { it.listeId == listeId }

    /**
     * Aktualisiert einen Kunden
     */
    override suspend fun updateCustomer(customerId: String, updates: Map<String, Any>): Boolean {
        return try {
            val filtered = withoutDeprecatedCustomerFields(updates)
            if (filtered.isEmpty()) return true
            awaitWithTimeout { customersRef.child(customerId).updateChildren(filtered).await() }
            if ("ohneTour" in filtered || filtered.keys.any { it in tourRelevantKeys }) {
                getCustomerById(customerId)?.let { syncTourCustomer(it) }
            }
            termincache.invalidate(customerId)
            true
        } catch (e: Exception) {
            // Nur echte Fehler werden als Fehler behandelt
            android.util.Log.e("CustomerRepository", "Error updating customer", e)
            false
        }
    }

    /** Fügt einen Ausnahme-Termin hinzu (A oder L an einem Datum). */
    suspend fun addAusnahmeTermin(customerId: String, termin: AusnahmeTermin): Boolean {
        val customer = getCustomerById(customerId) ?: return false
        val newList = customer.ausnahmeTermine + termin
        return updateCustomer(customerId, mapOf("ausnahmeTermine" to CustomerSnapshotParser.serializeAusnahmeTermine(newList)))
    }

    /**
     * Fügt Ausnahme-Termin A und L hinzu. L-Datum = A-Datum + tageAzuL.
     */
    suspend fun addAusnahmeAbholungMitLieferung(customerId: String, abholungDatum: Long, tageAzuL: Int): Boolean {
        val customer = getCustomerById(customerId) ?: return false
        val aStart = TerminBerechnungUtils.getStartOfDay(abholungDatum)
        val tage = tageAzuL.coerceIn(0, 365)
        val lStart = TerminBerechnungUtils.getStartOfDay(aStart + TimeUnit.DAYS.toMillis(tage.toLong()))
        val newList = customer.ausnahmeTermine + AusnahmeTermin(datum = aStart, typ = "A") + AusnahmeTermin(datum = lStart, typ = "L")
        return updateCustomer(customerId, mapOf("ausnahmeTermine" to CustomerSnapshotParser.serializeAusnahmeTermine(newList)))
    }

    /** Entfernt einen Ausnahme-Termin (gleiches Datum + Typ). */
    suspend fun removeAusnahmeTermin(customerId: String, termin: AusnahmeTermin): Boolean {
        val customer = getCustomerById(customerId) ?: return false
        val newList = customer.ausnahmeTermine.filter { it.datum != termin.datum || it.typ != termin.typ }
        if (newList.size == customer.ausnahmeTermine.size) return false
        return updateCustomer(customerId, mapOf("ausnahmeTermine" to CustomerSnapshotParser.serializeAusnahmeTermine(newList)))
    }

    /**
     * Fügt einen Kunden-Termin Abholung (A) und den zugehörigen Liefertermin (L) hinzu.
     * L-Datum = A-Datum + tageAzuL. Pro Aufruf genau ein A und ein L.
     */
    suspend fun addKundenAbholungMitLieferung(customerId: String, abholungDatum: Long, tageAzuL: Int): Boolean {
        val customer = getCustomerById(customerId) ?: return false
        val aStart = TerminBerechnungUtils.getStartOfDay(abholungDatum)
        val tage = tageAzuL.coerceIn(0, 365)
        val lStart = TerminBerechnungUtils.getStartOfDay(aStart + TimeUnit.DAYS.toMillis(tage.toLong()))
        val newList = customer.kundenTermine + KundenTermin(datum = aStart, typ = "A") + KundenTermin(datum = lStart, typ = "L")
        return updateCustomer(customerId, mapOf("kundenTermine" to CustomerSnapshotParser.serializeKundenTermine(newList)))
    }

    /** Entfernt einen Kunden-Termin (gleiches Datum + Typ). */
    suspend fun removeKundenTermin(customerId: String, termin: KundenTermin): Boolean =
        removeKundenTermine(customerId, listOf(termin))

    /** Entfernt mehrere Kunden-Termine in einem Schritt (z. B. A+L-Paar). */
    suspend fun removeKundenTermine(customerId: String, termins: List<KundenTermin>): Boolean {
        if (termins.isEmpty()) return true
        val customer = getCustomerById(customerId) ?: return false
        val toRemove = termins.toSet()
        val newList = customer.kundenTermine.filter { t -> !toRemove.any { it.datum == t.datum && it.typ == t.typ } }
        if (newList.size == customer.kundenTermine.size) return false
        return updateCustomer(customerId, mapOf("kundenTermine" to CustomerSnapshotParser.serializeKundenTermine(newList)))
    }

    override suspend fun updateCustomerResult(customerId: String, updates: Map<String, Any>): Result<Boolean> {
        return try {
            val filtered = withoutDeprecatedCustomerFields(updates)
            if (filtered.isEmpty()) return Result.Success(true)
            awaitWithTimeout { customersRef.child(customerId).updateChildren(filtered).await() }
            if ("ohneTour" in filtered || filtered.keys.any { it in tourRelevantKeys }) {
                getCustomerById(customerId)?.let { syncTourCustomer(it) }
            }
            termincache.invalidate(customerId)
            Result.Success(true)
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
            awaitWithTimeout { customersRef.child(customerId).removeValue().await() }
            customersForTourRef.child(customerId).removeValue().await()
            true
        } catch (e: Exception) {
            // Nur echte Fehler werden als Fehler behandelt
            android.util.Log.e("CustomerRepository", "Error deleting customer", e)
            false
        }
    }

    override suspend fun deleteCustomerResult(customerId: String): Result<Boolean> {
        return try {
            awaitWithTimeout { customersRef.child(customerId).removeValue().await() }
            customersForTourRef.child(customerId).removeValue().await()
            Result.Success(true)
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
                awaitWithTimeout(3000) { customersRef.child(key).updateChildren(keysToRemove).await() }
                count++
            } catch (_: Exception) { }
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
