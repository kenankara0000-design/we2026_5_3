package com.example.we2026_5.data.repository

import com.example.we2026_5.KundenListe
import com.example.we2026_5.KundenTermin
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.VerschobenerTermin
import com.google.firebase.database.DataSnapshot

/**
 * Parst KundenListe und zugehörige Strukturen aus Firebase DataSnapshot.
 * Ausgelagert aus KundenListeRepository (Phase 7.03).
 */
object KundenListeSnapshotParser {

    fun parseKundenListe(snapshot: DataSnapshot): KundenListe? {
        if (!snapshot.exists()) return null
        val intervalle = snapshot.child("intervalle").children.map { parseListeIntervall(it) }
        val verschobene = snapshot.child("verschobeneTermine").children.map { parseVerschobenerTermin(it) }
        val geloeschte = snapshot.child("geloeschteTermine").children.map { safeLong(it.getValue()) }
        val listenTermine = parseListenTermine(snapshot.child("listenTermine"))
        val wochentagASnap = snapshot.child("wochentagA")
        val wochentagAVal = if (wochentagASnap.exists()) safeInt(wochentagASnap.getValue()).takeIf { it in 0..6 } else null
        val tageAzuLVal = if (snapshot.child("tageAzuL").exists()) safeInt(snapshot.child("tageAzuL").getValue()).coerceIn(1, 365).takeIf { it in 1..365 } ?: 7 else 7
        return KundenListe(
            id = snapshot.child("id").getValue(String::class.java) ?: snapshot.key ?: "",
            name = snapshot.child("name").getValue(String::class.java) ?: "",
            listeArt = (snapshot.child("listeArt").getValue(String::class.java) ?: "Gewerbe").let { raw ->
                when (raw) { "Liste", "Tour" -> "Listenkunden"; else -> raw }
            },
            wochentag = safeInt(snapshot.child("wochentag").getValue()).coerceIn(-1, 6),
            intervalle = intervalle.ifEmpty { listOf(ListeIntervall()) },
            erstelltAm = safeLong(snapshot.child("erstelltAm").getValue()).takeIf { it > 0 } ?: System.currentTimeMillis(),
            abholungErfolgt = safeBoolean(snapshot.child("abholungErfolgt").getValue()),
            auslieferungErfolgt = safeBoolean(snapshot.child("auslieferungErfolgt").getValue()),
            urlaubVon = safeLong(snapshot.child("urlaubVon").getValue()),
            urlaubBis = safeLong(snapshot.child("urlaubBis").getValue()),
            verschobeneTermine = verschobene,
            geloeschteTermine = geloeschte,
            listenTermine = listenTermine,
            wochentagA = wochentagAVal,
            tageAzuL = tageAzuLVal
        )
    }

    fun parseListeIntervall(s: DataSnapshot): ListeIntervall = ListeIntervall(
        abholungDatum = safeLong(s.child("abholungDatum").getValue()),
        auslieferungDatum = safeLong(s.child("auslieferungDatum").getValue()),
        wiederholen = safeBoolean(s.child("wiederholen").getValue()),
        intervallTage = safeInt(s.child("intervallTage").getValue()).coerceIn(1, 365).takeIf { it in 1..365 } ?: 7,
        intervallAnzahl = safeInt(s.child("intervallAnzahl").getValue()).coerceAtLeast(0)
    )

    fun parseListenTermine(snapshot: DataSnapshot): List<KundenTermin> {
        if (!snapshot.exists()) return emptyList()
        return snapshot.children.map { parseKundenTermin(it) }
    }

    private fun parseKundenTermin(s: DataSnapshot): KundenTermin = KundenTermin(
        datum = safeLong(s.child("datum").getValue()),
        typ = s.child("typ").getValue(String::class.java) ?: "A"
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

    private fun safeLong(value: Any?): Long = when (value) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull() ?: 0L
        else -> 0L
    }

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
}
