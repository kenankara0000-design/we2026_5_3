package com.example.we2026_5.data.repository

import com.example.we2026_5.TerminRegel
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

class TerminRegelRepository(
    private val database: FirebaseDatabase
) {
    private val regelnRef: DatabaseReference = database.reference.child("terminRegeln")
    
    /**
     * Lädt alle Regeln als Flow (für LiveData/StateFlow)
     */
    fun getAllRegelnFlow(): Flow<List<TerminRegel>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val regeln = mutableListOf<TerminRegel>()
                snapshot.children.forEach { child ->
                    val regel = child.getValue(TerminRegel::class.java)
                    regel?.let { regeln.add(it) }
                }
                // Sortieren nach Name
                regeln.sortBy { it.name }
                trySend(regeln)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        }
        
        regelnRef.addValueEventListener(listener)
        
        awaitClose { regelnRef.removeEventListener(listener) }
    }
    
    /**
     * Lädt alle Regeln einmalig
     */
    suspend fun getAllRegeln(): List<TerminRegel> {
        val snapshot = regelnRef.get().await()
        val regeln = mutableListOf<TerminRegel>()
        snapshot.children.forEach { child ->
            val regel = child.getValue(TerminRegel::class.java)
            regel?.let { regeln.add(it) }
        }
        // Sortieren nach Name
        return regeln.sortedBy { it.name }
    }
    
    /**
     * Lädt eine einzelne Regel
     */
    suspend fun getRegelById(regelId: String): TerminRegel? {
        val snapshot = regelnRef.child(regelId).get().await()
        return snapshot.getValue(TerminRegel::class.java)
    }
    
    /**
     * Speichert eine neue Regel
     */
    suspend fun saveRegel(regel: TerminRegel): Boolean {
        return try {
            val task = regelnRef.child(regel.id).setValue(regel)
            
            try {
                withTimeout(2000) {
                    task.await()
                }
                android.util.Log.d("TerminRegelRepository", "Save completed successfully")
                true
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                android.util.Log.d("TerminRegelRepository", "Save completed (timeout, but saved locally)")
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("TerminRegelRepository", "Error saving regel", e)
            false
        }
    }
    
    /**
     * Aktualisiert eine Regel
     */
    suspend fun updateRegel(regelId: String, updates: Map<String, Any>): Boolean {
        return try {
            val task = regelnRef.child(regelId).updateChildren(updates)
            
            try {
                withTimeout(2000) {
                    task.await()
                }
                android.util.Log.d("TerminRegelRepository", "Update completed successfully")
                true
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                android.util.Log.d("TerminRegelRepository", "Update completed (timeout, but updated locally)")
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("TerminRegelRepository", "Error updating regel", e)
            false
        }
    }
    
    /**
     * Löscht eine Regel
     */
    suspend fun deleteRegel(regelId: String): Boolean {
        return try {
            val task = regelnRef.child(regelId).removeValue()
            
            try {
                withTimeout(2000) {
                    task.await()
                }
                android.util.Log.d("TerminRegelRepository", "Delete completed successfully")
                true
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                android.util.Log.d("TerminRegelRepository", "Delete completed (timeout, but deleted locally)")
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("TerminRegelRepository", "Error deleting regel", e)
            false
        }
    }
    
    /**
     * Erhöht die Verwendungsanzahl einer Regel
     */
    suspend fun incrementVerwendungsanzahl(regelId: String): Boolean {
        return try {
            val regel = getRegelById(regelId)
            if (regel != null) {
                updateRegel(regelId, mapOf("verwendungsanzahl" to (regel.verwendungsanzahl + 1)))
            } else {
                false
            }
        } catch (e: Exception) {
            android.util.Log.e("TerminRegelRepository", "Error incrementing verwendungsanzahl", e)
            false
        }
    }
}
