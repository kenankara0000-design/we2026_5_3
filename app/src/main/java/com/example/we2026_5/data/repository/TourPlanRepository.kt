package com.example.we2026_5.data.repository

import com.example.we2026_5.TourSlot
import com.example.we2026_5.util.FirebaseRetryHelper
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

/**
 * Repository zur Verwaltung fester Tour-Tage (Wochentag -> Stadt + Zeitfenster).
 */
class TourPlanRepository(
    database: FirebaseDatabase
) {
    private val tourRef: DatabaseReference = database.reference.child("tourPlaene")

    fun getTourSlotsFlow(): Flow<List<TourSlot>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val slots = snapshot.children.mapNotNull { child ->
                    parseSlot(child)
                }.sortedBy { it.wochentag }
                trySend(slots)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        tourRef.addValueEventListener(listener)
        awaitClose { tourRef.removeEventListener(listener) }
    }

    suspend fun saveTourSlot(slot: TourSlot): Boolean {
        return when (val r = FirebaseRetryHelper.setValueWithRetry(tourRef.child(slot.id), slot)) {
            is Result.Success -> true
            is Result.Error -> {
                android.util.Log.e("TourPlanRepository", "Error saving tour slot", r.throwable)
                false
            }
            is Result.Loading -> false
        }
    }

    suspend fun deleteTourSlot(slotId: String): Boolean {
        if (slotId.isBlank()) return true
        return when (val r = FirebaseRetryHelper.removeValueWithRetry(tourRef.child(slotId))) {
            is Result.Success -> true
            is Result.Error -> {
                android.util.Log.e("TourPlanRepository", "Error deleting tour slot", r.throwable)
                false
            }
            is Result.Loading -> false
        }
    }

    private fun parseSlot(snapshot: DataSnapshot): TourSlot? {
        val slot = snapshot.getValue(TourSlot::class.java) ?: return null
        return slot.copy(id = snapshot.key ?: slot.id)
    }
}
