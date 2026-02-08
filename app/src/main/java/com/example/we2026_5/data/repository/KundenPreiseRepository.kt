package com.example.we2026_5.data.repository

import com.example.we2026_5.wasch.KundenPreis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class KundenPreiseRepository(
    private val database: FirebaseDatabase
) {
    private val kundenPreiseRef = database.reference.child("kundenPreise")

    /** Kundenpreise nur für einen Kunden laden (Performance). */
    fun getKundenPreiseForCustomerFlow(customerId: String): Flow<List<KundenPreis>> = callbackFlow {
        if (customerId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val ref = kundenPreiseRef.child(customerId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<KundenPreis>()
                snapshot.children.forEach { child ->
                    parseKundenPreis(customerId, child)?.let { list.add(it) }
                }
                trySend(list)
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                close(Exception(error.message))
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun getKundenPreiseForCustomer(customerId: String): List<KundenPreis> {
        if (customerId.isBlank()) return emptyList()
        val snapshot = kundenPreiseRef.child(customerId).get().await()
        return snapshot.children.mapNotNull { parseKundenPreis(customerId, it) }
    }

    private fun parseKundenPreis(customerId: String, snapshot: DataSnapshot): KundenPreis? {
        val articleId = snapshot.key ?: return null
        val priceNet = (snapshot.child("priceNet").getValue(Any::class.java) as? Number)?.toDouble() ?: 0.0
        val priceGross = (snapshot.child("priceGross").getValue(Any::class.java) as? Number)?.toDouble() ?: 0.0
        return KundenPreis(customerId = customerId, articleId = articleId, priceNet = priceNet, priceGross = priceGross)
    }

    /** Einzelnen Kundenpreis speichern (für Import). */
    suspend fun setKundenPreis(kundenPreis: KundenPreis): Boolean = try {
        val ref = kundenPreiseRef.child(kundenPreis.customerId).child(kundenPreis.articleId)
        withTimeout(2000) {
            ref.setValue(mapOf(
                "priceNet" to kundenPreis.priceNet,
                "priceGross" to kundenPreis.priceGross
            )).await()
        }
        true
    } catch (e: Exception) {
        android.util.Log.e("KundenPreiseRepository", "setKundenPreis failed", e)
        false
    }

    /** Alle Kundenpreise eines Kunden ersetzen (z. B. nach SevDesk-Import). */
    suspend fun setKundenPreiseForCustomer(customerId: String, preise: List<KundenPreis>): Boolean = try {
        val ref = kundenPreiseRef.child(customerId)
        val map = preise.associate { it.articleId to mapOf("priceNet" to it.priceNet, "priceGross" to it.priceGross) }
        withTimeout(5000) { ref.setValue(map).await() }
        true
    } catch (e: Exception) {
        android.util.Log.e("KundenPreiseRepository", "setKundenPreiseForCustomer failed", e)
        false
    }
}
