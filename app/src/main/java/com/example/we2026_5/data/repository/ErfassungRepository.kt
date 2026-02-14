package com.example.we2026_5.data.repository

import com.example.we2026_5.util.FirebaseRetryHelper
import com.example.we2026_5.util.Result
import com.example.we2026_5.wasch.WaschErfassung
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ErfassungRepository(
    private val database: FirebaseDatabase
) {
    private val erfassungenRef = database.reference.child("waschErfassungen")

    /** @param includeErledigt false = nur offene (erledigt==false), true = alle. */
    fun getErfassungenByCustomerFlow(customerId: String, includeErledigt: Boolean = false): Flow<List<WaschErfassung>> = callbackFlow {
        val ref = erfassungenRef.orderByChild("customerId").equalTo(customerId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var list = snapshot.children.mapNotNull { ErfassungSnapshotParser.parseErfassung(it) }
                if (!includeErledigt) list = list.filter { !it.erledigt }
                trySend(list.sortedByDescending { it.datum })
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(Exception(error.message))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Nur erledigte Erfassungen pro Kunde (für Erledigt-Bereich). */
    fun getErfassungenByCustomerFlowErledigt(customerId: String): Flow<List<WaschErfassung>> = callbackFlow {
        val ref = erfassungenRef.orderByChild("customerId").equalTo(customerId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { parseErfassung(it) }.filter { it.erledigt }
                trySend(list.sortedByDescending { it.datum })
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(Exception(error.message))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** @param includeErledigt false = nur offene, true = alle. */
    fun getAllErfassungenFlow(includeErledigt: Boolean = false): Flow<List<WaschErfassung>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var list = snapshot.children.mapNotNull { ErfassungSnapshotParser.parseErfassung(it) }
                if (!includeErledigt) list = list.filter { !it.erledigt }
                trySend(list.sortedByDescending { it.datum })
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(Exception(error.message))
            }
        }
        erfassungenRef.addValueEventListener(listener)
        awaitClose { erfassungenRef.removeEventListener(listener) }
    }

    /** Nur erledigte Erfassungen (für Erledigt-Bereich in Alle Belege). */
    fun getAllErfassungenFlowErledigt(): Flow<List<WaschErfassung>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { parseErfassung(it) }.filter { it.erledigt }
                trySend(list.sortedByDescending { it.datum })
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(Exception(error.message))
            }
        }
        erfassungenRef.addValueEventListener(listener)
        awaitClose { erfassungenRef.removeEventListener(listener) }
    }

    suspend fun getErfassungenByCustomer(customerId: String, includeErledigt: Boolean = false): List<WaschErfassung> {
        val snapshot = erfassungenRef.orderByChild("customerId").equalTo(customerId).get().await()
        var list = snapshot.children.mapNotNull { ErfassungSnapshotParser.parseErfassung(it) }
        if (!includeErledigt) list = list.filter { !it.erledigt }
        return list.sortedByDescending { it.datum }
    }

    suspend fun saveErfassung(erfassung: WaschErfassung): Boolean {
        val key = if (erfassung.id.isNotBlank()) erfassung.id else erfassungenRef.push().key ?: return false
        val posMap = erfassung.positionen.mapIndexed { i, p ->
            i.toString() to mapOf("articleId" to p.articleId, "menge" to p.menge, "einheit" to p.einheit)
        }.toMap()
        val map = mapOf<String, Any?>(
            "customerId" to erfassung.customerId,
            "datum" to erfassung.datum,
            "zeit" to erfassung.zeit,
            "notiz" to erfassung.notiz,
            "positionen" to posMap,
            "erledigt" to erfassung.erledigt
        )
        return when (val r = FirebaseRetryHelper.updateChildrenWithRetry(erfassungenRef.child(key), map)) {
            is Result.Success -> true
            is Result.Error -> {
                android.util.Log.e("ErfassungRepository", "Save erfassung failed", r.throwable)
                false
            }
            is Result.Loading -> false
        }
    }

    suspend fun saveErfassungNew(erfassung: WaschErfassung): Boolean {
        val key = erfassungenRef.push().key ?: return false
        val posMap = erfassung.positionen.mapIndexed { i, p ->
            i.toString() to mapOf("articleId" to p.articleId, "menge" to p.menge, "einheit" to p.einheit)
        }.toMap()
        val value = mapOf(
            "customerId" to erfassung.customerId,
            "datum" to erfassung.datum,
            "zeit" to erfassung.zeit,
            "notiz" to erfassung.notiz,
            "positionen" to posMap,
            "erledigt" to erfassung.erledigt
        )
        return when (val r = FirebaseRetryHelper.executeWithRetry {
            erfassungenRef.child(key).setValue(value).await()
            true
        }) {
            is Result.Success -> r.data
            is Result.Error -> {
                android.util.Log.e("ErfassungRepository", "Save erfassung failed", r.throwable)
                false
            }
            is Result.Loading -> false
        }
    }

    /** Alle Erfassungen eines Belegs als erledigt markieren. Nutzt FirebaseRetryHelper. */
    suspend fun markBelegErledigt(erfassungen: List<WaschErfassung>): Boolean {
        for (e in erfassungen) {
            when (val r = FirebaseRetryHelper.setValueWithRetry(erfassungenRef.child(e.id).child("erledigt"), true)) {
                is Result.Success -> { }
                is Result.Error -> {
                    android.util.Log.e("ErfassungRepository", "markBelegErledigt failed", r.throwable)
                    return false
                }
                is Result.Loading -> return false
            }
        }
        return true
    }

    suspend fun deleteErfassung(erfassungId: String): Boolean {
        return when (val r = FirebaseRetryHelper.removeValueWithRetry(erfassungenRef.child(erfassungId))) {
            is Result.Success -> true
            is Result.Error -> {
                android.util.Log.e("ErfassungRepository", "Delete erfassung failed", r.throwable)
                false
            }
            is Result.Loading -> false
        }
    }
}
