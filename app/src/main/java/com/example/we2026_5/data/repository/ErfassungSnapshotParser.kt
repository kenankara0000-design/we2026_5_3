package com.example.we2026_5.data.repository

import com.example.we2026_5.wasch.ErfassungPosition
import com.example.we2026_5.wasch.WaschErfassung
import com.google.firebase.database.DataSnapshot

/**
 * Parst WaschErfassung aus Firebase DataSnapshot.
 * Ausgelagert aus ErfassungRepository (Phase 7.03).
 */
object ErfassungSnapshotParser {

    fun parseErfassung(snapshot: DataSnapshot): WaschErfassung? {
        val id = snapshot.key ?: return null
        val customerId = snapshot.child("customerId").getValue(String::class.java) ?: ""
        val datum = (snapshot.child("datum").getValue(Any::class.java) as? Number)?.toLong() ?: 0L
        val notiz = snapshot.child("notiz").getValue(String::class.java) ?: ""
        val posSnap = snapshot.child("positionen")
        val positionen = mutableListOf<ErfassungPosition>()
        posSnap.children.forEach { child ->
            val articleId = child.child("articleId").getValue(String::class.java) ?: ""
            val menge = (child.child("menge").getValue(Any::class.java) as? Number)?.toDouble() ?: 0.0
            val einheit = child.child("einheit").getValue(String::class.java) ?: ""
            if (articleId.isNotBlank()) positionen.add(ErfassungPosition(articleId = articleId, menge = menge, einheit = einheit))
        }
        val zeit = snapshot.child("zeit").getValue(String::class.java) ?: ""
        val erledigt = (snapshot.child("erledigt").getValue(Any::class.java) as? Boolean) ?: false
        return WaschErfassung(id = id, customerId = customerId, datum = datum, zeit = zeit, positionen = positionen, notiz = notiz, erledigt = erledigt)
    }
}
