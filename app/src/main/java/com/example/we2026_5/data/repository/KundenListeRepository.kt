package com.example.we2026_5.data.repository

import com.example.we2026_5.KundenListe
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class KundenListeRepository(
    private val db: FirebaseFirestore
) {
    
    /**
     * Lädt alle Listen als Flow
     */
    fun getAllListenFlow(): Flow<List<KundenListe>> = callbackFlow {
        val listener = db.collection("kundenListen")
            .orderBy("name")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val listen = snapshot.toObjects(KundenListe::class.java)
                    trySend(listen)
                }
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Lädt alle Listen einmalig
     */
    suspend fun getAllListen(): List<KundenListe> {
        val snapshot = db.collection("kundenListen")
            .orderBy("name")
            .get()
            .await()
        return snapshot.toObjects(KundenListe::class.java)
    }
    
    /**
     * Lädt eine Liste nach ID
     */
    suspend fun getListeById(listeId: String): KundenListe? {
        val doc = db.collection("kundenListen")
            .document(listeId)
            .get()
            .await()
        return doc.toObject(KundenListe::class.java)
    }
    
    /**
     * Speichert eine neue Liste
     */
    suspend fun saveListe(liste: KundenListe) {
        if (liste.id.isEmpty()) {
            throw IllegalArgumentException("Liste ID darf nicht leer sein")
        }
        db.collection("kundenListen")
            .document(liste.id)
            .set(liste)
            .await()
    }
    
    /**
     * Aktualisiert eine Liste
     */
    suspend fun updateListe(listeId: String, updates: Map<String, Any>) {
        db.collection("kundenListen")
            .document(listeId)
            .update(updates)
            .await()
    }
    
    /**
     * Löscht eine Liste
     */
    suspend fun deleteListe(listeId: String) {
        db.collection("kundenListen")
            .document(listeId)
            .delete()
            .await()
    }
}
