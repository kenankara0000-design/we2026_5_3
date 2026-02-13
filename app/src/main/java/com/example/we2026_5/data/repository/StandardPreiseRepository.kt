package com.example.we2026_5.data.repository

import com.example.we2026_5.wasch.StandardPreis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** Repository für die Standardpreisliste (Listenkunden + Privat). */
class StandardPreiseRepository(
    private val database: FirebaseDatabase
) {
    private val standardPreiseRef = database.reference.child("standardPreise")

    fun getStandardPreiseFlow(): Flow<List<StandardPreis>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<StandardPreis>()
                snapshot.children.forEach { child ->
                    parseStandardPreis(child)?.let { list.add(it) }
                }
                trySend(list)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(Exception(error.message))
            }
        }
        standardPreiseRef.addValueEventListener(listener)
        awaitClose { standardPreiseRef.removeEventListener(listener) }
    }

    suspend fun getStandardPreise(): List<StandardPreis> {
        val snapshot = standardPreiseRef.get().await()
        return snapshot.children.mapNotNull { parseStandardPreis(it) }
    }

    private fun parseStandardPreis(snapshot: DataSnapshot): StandardPreis? {
        val articleId = snapshot.key ?: return null
        val priceNet = (snapshot.child("priceNet").getValue(Any::class.java) as? Number)?.toDouble() ?: 0.0
        val priceGross = (snapshot.child("priceGross").getValue(Any::class.java) as? Number)?.toDouble() ?: 0.0
        return StandardPreis(articleId = articleId, priceNet = priceNet, priceGross = priceGross)
    }

    suspend fun setStandardPreis(preis: StandardPreis): Boolean {
        if (preis.articleId.isBlank()) return false
        return try {
            val ref = standardPreiseRef.child(preis.articleId)
            withTimeout(2000) {
                ref.setValue(mapOf(
                    "priceNet" to preis.priceNet,
                    "priceGross" to preis.priceGross
                )).await()
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("StandardPreiseRepository", "setStandardPreis failed", e)
            false
        }
    }

    suspend fun removeStandardPreis(articleId: String): Boolean {
        if (articleId.isBlank()) return false
        return try {
            withTimeout(2000) { standardPreiseRef.child(articleId).removeValue().await() }
            true
        } catch (e: Exception) {
            android.util.Log.e("StandardPreiseRepository", "removeStandardPreis failed", e)
            false
        }
    }
}
