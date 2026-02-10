package com.example.we2026_5.data.repository

import com.example.we2026_5.wasch.TourPreis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** Repository für die einheitliche Tour-Preisliste (alle Kunden mit Tour nutzen diese Preise). */
class TourPreiseRepository(
    private val database: FirebaseDatabase
) {
    private val tourPreiseRef = database.reference.child("tourPreise")

    fun getTourPreiseFlow(): Flow<List<TourPreis>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<TourPreis>()
                snapshot.children.forEach { child ->
                    parseTourPreis(child)?.let { list.add(it) }
                }
                trySend(list)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(Exception(error.message))
            }
        }
        tourPreiseRef.addValueEventListener(listener)
        awaitClose { tourPreiseRef.removeEventListener(listener) }
    }

    suspend fun getTourPreise(): List<TourPreis> {
        val snapshot = tourPreiseRef.get().await()
        return snapshot.children.mapNotNull { parseTourPreis(it) }
    }

    private fun parseTourPreis(snapshot: DataSnapshot): TourPreis? {
        val articleId = snapshot.key ?: return null
        val priceNet = (snapshot.child("priceNet").getValue(Any::class.java) as? Number)?.toDouble() ?: 0.0
        val priceGross = (snapshot.child("priceGross").getValue(Any::class.java) as? Number)?.toDouble() ?: 0.0
        return TourPreis(articleId = articleId, priceNet = priceNet, priceGross = priceGross)
    }

    suspend fun setTourPreis(preis: TourPreis): Boolean {
        if (preis.articleId.isBlank()) return false
        return try {
            val ref = tourPreiseRef.child(preis.articleId)
            withTimeout(2000) {
                ref.setValue(mapOf(
                    "priceNet" to preis.priceNet,
                    "priceGross" to preis.priceGross
                )).await()
            }
            true
        } catch (e: Exception) {
            android.util.Log.e("TourPreiseRepository", "setTourPreis failed", e)
            false
        }
    }

    suspend fun removeTourPreis(articleId: String): Boolean {
        if (articleId.isBlank()) return false
        return try {
            withTimeout(2000) { tourPreiseRef.child(articleId).removeValue().await() }
            true
        } catch (e: Exception) {
            android.util.Log.e("TourPreiseRepository", "removeTourPreis failed", e)
            false
        }
    }
}
