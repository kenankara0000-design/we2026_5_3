package com.example.we2026_5.data.repository

import com.example.we2026_5.KundenListe
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

class KundenListeRepository(
    private val database: FirebaseDatabase
) {
    private val listenRef: DatabaseReference = database.reference.child("kundenListen")
    
    /**
     * Lädt alle Listen als Flow
     */
    fun getAllListenFlow(): Flow<List<KundenListe>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listen = mutableListOf<KundenListe>()
                snapshot.children.forEach { child ->
                    val liste = child.getValue(KundenListe::class.java)
                    liste?.let { listen.add(it) }
                }
                // Sortieren nach Name
                listen.sortBy { it.name }
                trySend(listen)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        }
        
        listenRef.addValueEventListener(listener)
        
        awaitClose { listenRef.removeEventListener(listener) }
    }
    
    /**
     * Lädt alle Listen einmalig
     */
    suspend fun getAllListen(): List<KundenListe> {
        val snapshot = listenRef.get().await()
        val listen = mutableListOf<KundenListe>()
        snapshot.children.forEach { child ->
            val liste = child.getValue(KundenListe::class.java)
            liste?.let { listen.add(it) }
        }
        // Sortieren nach Name
        return listen.sortedBy { it.name }
    }
    
    /**
     * Lädt eine Liste nach ID
     */
    suspend fun getListeById(listeId: String): KundenListe? {
        val snapshot = listenRef.child(listeId).get().await()
        return snapshot.getValue(KundenListe::class.java)
    }
    
    /**
     * Speichert eine neue Liste
     */
    suspend fun saveListe(liste: KundenListe) {
        if (liste.id.isEmpty()) {
            throw IllegalArgumentException("Liste ID darf nicht leer sein")
        }
        
        // Realtime Database speichert sofort lokal im Offline-Modus
        val task = listenRef.child(liste.id).setValue(liste)
        
        // Versuchen, auf Abschluss zu warten, aber mit Timeout (2 Sekunden)
        // Im Offline-Modus ist die lokale Speicherung bereits sofort erfolgt
        try {
            withTimeout(2000) {
                task.await()
            }
            android.util.Log.d("KundenListeRepository", "Save completed successfully")
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            // Timeout: Realtime Database hat bereits lokal gespeichert (im Offline-Modus)
            // Die Daten sind sicher lokal gespeichert, auch wenn Server-Verbindung fehlt
            android.util.Log.d("KundenListeRepository", "Save completed (timeout, but saved locally)")
            // Weiter machen, da lokale Speicherung bereits erfolgt ist
        }
    }
    
    /**
     * Aktualisiert eine Liste
     */
    suspend fun updateListe(listeId: String, updates: Map<String, Any>) {
        listenRef.child(listeId).updateChildren(updates).await()
    }
    
    /**
     * Löscht eine Liste
     */
    suspend fun deleteListe(listeId: String) {
        val task = listenRef.child(listeId).removeValue()
        
        try {
            withTimeout(2000) {
                task.await()
            }
            android.util.Log.d("KundenListeRepository", "Delete completed successfully")
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            // Timeout: Realtime Database hat bereits lokal gelöscht (im Offline-Modus)
            android.util.Log.d("KundenListeRepository", "Delete completed (timeout, but deleted locally)")
        }
    }
}
