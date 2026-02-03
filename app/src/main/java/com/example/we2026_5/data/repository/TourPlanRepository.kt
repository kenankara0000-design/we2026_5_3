package com.example.we2026_5.data.repository

import com.example.we2026_5.TourSlot
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
        return try {
            val task = tourRef.child(slot.id).setValue(slot)
            withTimeout(2000) { task.await() }
            true
        } catch (e: Exception) {
            android.util.Log.e("TourPlanRepository", "Error saving tour slot", e)
            false
        }
    }

    suspend fun deleteTourSlot(slotId: String): Boolean {
        if (slotId.isBlank()) return true
        return try {
            val task = tourRef.child(slotId).removeValue()
            withTimeout(2000) { task.await() }
            true
        } catch (e: Exception) {
            android.util.Log.e("TourPlanRepository", "Error deleting tour slot", e)
            false
        }
    }

    private fun parseSlot(snapshot: DataSnapshot): TourSlot? {
        val slot = snapshot.getValue(TourSlot::class.java) ?: return null
        return slot.copy(id = snapshot.key ?: slot.id)
    }
}
