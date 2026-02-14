package com.example.we2026_5.data.repository

import com.example.we2026_5.KundenListe
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.example.we2026_5.util.FirebaseRetryHelper
import com.example.we2026_5.util.Result
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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
                    KundenListeSnapshotParser.KundenListeSnapshotParser.parseKundenListe(child)?.let { listen.add(it) }
                }
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
     * Liefert Listen-IDs, bei denen in Firebase noch listeArt "Liste" steht (für Migration Liste→Tour).
     */
    suspend fun getListenIdsWithListeArtListe(): List<String> {
        val snapshot = listenRef.get().await()
        val ids = mutableListOf<String>()
        snapshot.children.forEach { child ->
            val id = child.key ?: return@forEach
            if (child.child("listeArt").getValue(String::class.java) == "Liste") ids.add(id)
        }
        return ids
    }

    /**
     * Liefert Listen-IDs, bei denen in Firebase noch listeArt "Tour" steht (für Migration Tour→Listenkunden).
     */
    suspend fun getListenIdsWithListeArtTour(): List<String> {
        val snapshot = listenRef.get().await()
        val ids = mutableListOf<String>()
        snapshot.children.forEach { child ->
            val id = child.key ?: return@forEach
            if (child.child("listeArt").getValue(String::class.java) == "Tour") ids.add(id)
        }
        return ids
    }

    /**
     * Lädt alle Listen einmalig
     */
    suspend fun getAllListen(): List<KundenListe> {
        val snapshot = listenRef.get().await()
        val listen = mutableListOf<KundenListe>()
        snapshot.children.forEach { child ->
            KundenListeSnapshotParser.parseKundenListe(child)?.let { listen.add(it) }
        }
        return listen.sortedBy { it.name }
    }
    
    /**
     * Lädt eine Liste nach ID
     */
    suspend fun getListeById(listeId: String): KundenListe? {
        val snapshot = listenRef.child(listeId).get().await()
        return KundenListeSnapshotParser.parseKundenListe(snapshot)
    }
    
    /**
     * Speichert eine neue Liste. Nutzt FirebaseRetryHelper. Return Result für einheitliches Fehler-Handling (7.01).
     */
    suspend fun saveListe(liste: KundenListe): Result<Unit> {
        if (liste.id.isEmpty()) return Result.Error("Liste ID darf nicht leer sein", IllegalArgumentException("Liste ID darf nicht leer sein"))
        return FirebaseRetryHelper.executeWithRetry(timeoutMs = 5000) {
            listenRef.child(liste.id).setValue(liste).await()
            Unit
        }
    }

    /**
     * Aktualisiert eine Liste. Return Result für einheitliches Fehler-Handling (7.01).
     */
    suspend fun updateListe(listeId: String, updates: Map<String, Any>): Result<Unit> {
        return FirebaseRetryHelper.updateChildrenWithRetry(listenRef.child(listeId), updates).let { r ->
            when (r) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> r
                is Result.Loading -> r
            }
        }
    }

    /**
     * Löscht eine Liste. Return Result für einheitliches Fehler-Handling (7.01).
     */
    suspend fun deleteListe(listeId: String): Result<Unit> {
        return FirebaseRetryHelper.removeValueWithRetry(listenRef.child(listeId)).let { r ->
            when (r) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> r
                is Result.Loading -> r
            }
        }
    }
}
