package com.example.we2026_5.data.repository

import com.example.we2026_5.util.FirebaseConstants
import com.example.we2026_5.util.FirebaseRetryHelper
import com.example.we2026_5.util.Result
import com.example.we2026_5.wasch.ListenPrivatKundenpreis
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** Repository für Listen- und Privat-Kundenpreise (Listenkunden + Privat). */
class ListenPrivatKundenpreiseRepository(
    private val database: FirebaseDatabase
) {
    private val ref = database.reference.child(FirebaseConstants.LISTEN_PRIVAT_KUNDENPREISE)

    fun getListenPrivatKundenpreiseFlow(): Flow<List<ListenPrivatKundenpreis>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ListenPrivatKundenpreis>()
                snapshot.children.forEach { child ->
                    parseListenPrivatKundenpreis(child)?.let { list.add(it) }
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

    suspend fun getListenPrivatKundenpreise(): List<ListenPrivatKundenpreis> {
        val snapshot = ref.get().await()
        return snapshot.children.mapNotNull { parseListenPrivatKundenpreis(it) }
    }

    private fun parseListenPrivatKundenpreis(snapshot: DataSnapshot): ListenPrivatKundenpreis? {
        val articleId = snapshot.key ?: return null
        val priceNet = (snapshot.child("priceNet").getValue(Any::class.java) as? Number)?.toDouble() ?: 0.0
        val priceGross = (snapshot.child("priceGross").getValue(Any::class.java) as? Number)?.toDouble() ?: 0.0
        return ListenPrivatKundenpreis(articleId = articleId, priceNet = priceNet, priceGross = priceGross)
    }

    suspend fun setListenPrivatKundenpreis(preis: ListenPrivatKundenpreis): Boolean {
        if (preis.articleId.isBlank()) return false
        val value = mapOf("priceNet" to preis.priceNet, "priceGross" to preis.priceGross)
        return when (val r = FirebaseRetryHelper.setValueWithRetry(ref.child(preis.articleId), value)) {
            is Result.Success -> true
            is Result.Error -> {
                android.util.Log.e("ListenPrivatKundenpreiseRepository", "setListenPrivatKundenpreis failed", r.throwable)
                false
            }
            is Result.Loading -> false
        }
    }

    suspend fun removeListenPrivatKundenpreis(articleId: String): Boolean {
        if (articleId.isBlank()) return false
        return when (val r = FirebaseRetryHelper.removeValueWithRetry(ref.child(articleId))) {
            is Result.Success -> true
            is Result.Error -> {
                android.util.Log.e("ListenPrivatKundenpreiseRepository", "removeListenPrivatKundenpreis failed", r.throwable)
                false
            }
            is Result.Loading -> false
        }
    }
}
