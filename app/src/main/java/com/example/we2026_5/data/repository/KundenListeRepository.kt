package com.example.we2026_5.data.repository

import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.VerschobenerTermin
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

    /** Firebase liefert Zahlen teils als String; sicher nach Long parsen. */
    private fun safeLong(value: Any?): Long = when (value) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull() ?: 0L
        else -> 0L
    }

    /** Firebase liefert Zahlen teils als String; sicher nach Int parsen. */
    private fun safeInt(value: Any?): Int = when (value) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: 0
        else -> 0
    }

    private fun safeBoolean(value: Any?): Boolean = when (value) {
        is Boolean -> value
        is String -> value.toBooleanStrictOrNull() ?: false
        else -> false
    }

    private fun parseListeIntervall(s: DataSnapshot): ListeIntervall = ListeIntervall(
        abholungDatum = safeLong(s.child("abholungDatum").getValue()),
        auslieferungDatum = safeLong(s.child("auslieferungDatum").getValue()),
        wiederholen = safeBoolean(s.child("wiederholen").getValue()),
        intervallTage = safeInt(s.child("intervallTage").getValue()).coerceIn(1, 365).takeIf { it in 1..365 } ?: 7,
        intervallAnzahl = safeInt(s.child("intervallAnzahl").getValue()).coerceAtLeast(0)
    )

    private fun parseVerschobenerTermin(s: DataSnapshot): VerschobenerTermin {
        val typStr = s.child("typ").getValue(String::class.java)
        val typ = if (typStr == "AUSLIEFERUNG") TerminTyp.AUSLIEFERUNG else TerminTyp.ABHOLUNG
        return VerschobenerTermin(
            originalDatum = safeLong(s.child("originalDatum").getValue()),
            verschobenAufDatum = safeLong(s.child("verschobenAufDatum").getValue()),
            intervallId = s.child("intervallId").getValue(String::class.java),
            typ = typ
        )
    }

    private fun parseKundenListe(snapshot: DataSnapshot): KundenListe? {
        if (!snapshot.exists()) return null
        val intervalle = snapshot.child("intervalle").children.map { parseListeIntervall(it) }
        val verschobene = snapshot.child("verschobeneTermine").children.map { parseVerschobenerTermin(it) }
        val geloeschte = snapshot.child("geloeschteTermine").children.map { safeLong(it.getValue()) }
        return KundenListe(
            id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: "",
            name = snapshot.child("name").getValue(String::class.java) ?: "",
            listeArt = (snapshot.child("listeArt").getValue(String::class.java) ?: "Gewerbe").let { if (it == "Liste") "Tour" else it },
            wochentag = safeInt(snapshot.child("wochentag").getValue()).coerceIn(-1, 6),
            intervalle = intervalle.ifEmpty { listOf(ListeIntervall()) },
            erstelltAm = safeLong(snapshot.child("erstelltAm").getValue()).takeIf { it > 0 } ?: System.currentTimeMillis(),
            abholungErfolgt = safeBoolean(snapshot.child("abholungErfolgt").getValue()),
            auslieferungErfolgt = safeBoolean(snapshot.child("auslieferungErfolgt").getValue()),
            urlaubVon = safeLong(snapshot.child("urlaubVon").getValue()),
            urlaubBis = safeLong(snapshot.child("urlaubBis").getValue()),
            verschobeneTermine = verschobene,
            geloeschteTermine = geloeschte
        )
    }
    
    /**
     * Lädt alle Listen als Flow
     */
    fun getAllListenFlow(): Flow<List<KundenListe>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listen = mutableListOf<KundenListe>()
                snapshot.children.forEach { child ->
                    parseKundenListe(child)?.let { listen.add(it) }
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
     * Lädt alle Listen einmalig
     */
    suspend fun getAllListen(): List<KundenListe> {
        val snapshot = listenRef.get().await()
        val listen = mutableListOf<KundenListe>()
        snapshot.children.forEach { child ->
            parseKundenListe(child)?.let { listen.add(it) }
        }
        return listen.sortedBy { it.name }
    }
    
    /**
     * Lädt eine Liste nach ID
     */
    suspend fun getListeById(listeId: String): KundenListe? {
        val snapshot = listenRef.child(listeId).get().await()
        return parseKundenListe(snapshot)
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
