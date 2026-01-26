package com.example.we2026_5.data.repository

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

/**
 * Repository für Firebase Realtime Database
 * Kann für Echtzeit-Updates verwendet werden, wenn Firestore nicht ausreicht
 */
class RealtimeDatabaseRepository(
    private val database: FirebaseDatabase
) {
    
    /**
     * Speichert Daten in Realtime Database
     */
    suspend fun setValue(path: String, value: Any): Boolean {
        return try {
            val ref = database.reference.child(path)
            ref.setValue(value).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("RealtimeDatabaseRepository", "Error setting value", e)
            false
        }
    }
    
    /**
     * Lädt Daten aus Realtime Database
     */
    suspend fun <T> getValue(path: String, valueType: Class<T>): T? {
        return try {
            val ref = database.reference.child(path)
            val snapshot = ref.get().await()
            snapshot.getValue(valueType)
        } catch (e: Exception) {
            android.util.Log.e("RealtimeDatabaseRepository", "Error getting value", e)
            null
        }
    }
    
    /**
     * Aktualisiert Daten in Realtime Database
     */
    suspend fun updateValue(path: String, updates: Map<String, Any>): Boolean {
        return try {
            val ref = database.reference.child(path)
            ref.updateChildren(updates).await()
            true
        } catch (e: Exception) {
            android.util.Log.e("RealtimeDatabaseRepository", "Error updating value", e)
            false
        }
    }
    
    /**
     * Löscht Daten aus Realtime Database
     */
    suspend fun deleteValue(path: String): Boolean {
        return try {
            val ref = database.reference.child(path)
            ref.removeValue().await()
            true
        } catch (e: Exception) {
            android.util.Log.e("RealtimeDatabaseRepository", "Error deleting value", e)
            false
        }
    }
    
    /**
     * Gibt eine DatabaseReference zurück für Echtzeit-Listener
     */
    fun getReference(path: String): DatabaseReference {
        return database.reference.child(path)
    }
}
