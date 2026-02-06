package com.example.we2026_5.data.repository

import com.example.we2026_5.wasch.ErfassungPosition
import com.example.we2026_5.wasch.WaschErfassung
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class ErfassungRepository(
    private val database: FirebaseDatabase
) {
    private val erfassungenRef = database.reference.child("waschErfassungen")

    fun getErfassungenByCustomerFlow(customerId: String): Flow<List<WaschErfassung>> = callbackFlow {
        val ref = erfassungenRef.orderByChild("customerId").equalTo(customerId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { parseErfassung(it) }
                trySend(list.sortedByDescending { it.datum })
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(Exception(error.message))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun getAllErfassungenFlow(): Flow<List<WaschErfassung>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { parseErfassung(it) }
                trySend(list.sortedByDescending { it.datum })
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(Exception(error.message))
            }
        }
        erfassungenRef.addValueEventListener(listener)
        awaitClose { erfassungenRef.removeEventListener(listener) }
    }

    suspend fun getErfassungenByCustomer(customerId: String): List<WaschErfassung> {
        val snapshot = erfassungenRef.orderByChild("customerId").equalTo(customerId).get().await()
        return snapshot.children.mapNotNull { parseErfassung(it) }.sortedByDescending { it.datum }
    }

    private fun parseErfassung(snapshot: DataSnapshot): WaschErfassung? {
        val id = snapshot.key ?: return null
        val customerId = snapshot.child("customerId").getValue(String::class.java) ?: ""
        val datum = (snapshot.child("datum").getValue(Any::class.java) as? Number)?.toLong() ?: 0L
        val notiz = snapshot.child("notiz").getValue(String::class.java) ?: ""
        val posSnap = snapshot.child("positionen")
        val positionen = mutableListOf<ErfassungPosition>()
        posSnap.children.forEach { child ->
            val articleId = child.child("articleId").getValue(String::class.java) ?: ""
            val menge = (child.child("menge").getValue(Any::class.java) as? Number)?.toInt() ?: 0
            val einheit = child.child("einheit").getValue(String::class.java) ?: ""
            if (articleId.isNotBlank()) positionen.add(ErfassungPosition(articleId = articleId, menge = menge, einheit = einheit))
        }
        val zeit = snapshot.child("zeit").getValue(String::class.java) ?: ""
        return WaschErfassung(id = id, customerId = customerId, datum = datum, zeit = zeit, positionen = positionen, notiz = notiz)
    }

    suspend fun saveErfassung(erfassung: WaschErfassung): Boolean {
        return try {
            val key = if (erfassung.id.isNotBlank()) erfassung.id else erfassungenRef.push().key ?: return false
            val posMap = erfassung.positionen.mapIndexed { i, p ->
                i.toString() to mapOf("articleId" to p.articleId, "menge" to p.menge, "einheit" to p.einheit)
            }.toMap()
            val map = mapOf(
                "customerId" to erfassung.customerId,
                "datum" to erfassung.datum,
                "zeit" to erfassung.zeit,
                "notiz" to erfassung.notiz,
                "positionen" to posMap
            )
            withTimeout(2000) { erfassungenRef.child(key).updateChildren(map).let { t -> t.await() } }
            true
        } catch (e: Exception) {
            android.util.Log.e("ErfassungRepository", "Save erfassung failed", e)
            false
        }
    }

    suspend fun saveErfassungNew(erfassung: WaschErfassung): Boolean {
        val key = erfassungenRef.push().key ?: return false
        val posMap = erfassung.positionen.mapIndexed { i, p ->
            i.toString() to mapOf("articleId" to p.articleId, "menge" to p.menge, "einheit" to p.einheit)
        }.toMap()
        return try {
            withTimeout(2000) {
                erfassungenRef.child(key).setValue(mapOf(
                    "customerId" to erfassung.customerId,
                    "datum" to erfassung.datum,
                    "zeit" to erfassung.zeit,
                    "notiz" to erfassung.notiz,
                    "positionen" to posMap
                )).await()
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("ErfassungRepository", "Save erfassung failed", e)
            false
        }
    }

    suspend fun deleteErfassung(erfassungId: String): Boolean {
        return try {
            withTimeout(2000) { erfassungenRef.child(erfassungId).removeValue().await() }
            true
        } catch (e: Exception) {
            android.util.Log.e("ErfassungRepository", "Delete erfassung failed", e)
            false
        }
    }
}
