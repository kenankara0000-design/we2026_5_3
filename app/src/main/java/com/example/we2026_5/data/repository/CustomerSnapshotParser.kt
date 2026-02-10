package com.example.we2026_5.data.repository

import com.example.we2026_5.AusnahmeTermin
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenTermin
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.KundenTyp
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.VerschobenerTermin
import com.example.we2026_5.util.migrateKundenTyp
import com.google.firebase.database.DataSnapshot

/**
 * Parst Customer aus Firebase DataSnapshot und serialisiert Listen (verschobeneTermine, ausnahmeTermine)
 * für das Schreiben in die Realtime DB. Alte Struktur-Felder werden optional gelesen (Rückwärtskompatibilität).
 */
object CustomerSnapshotParser {

    fun parseCustomerSnapshot(child: DataSnapshot, id: String): Customer? {
        val customer = child.getValue(Customer::class.java) ?: return null
        val verschobeneTermine = parseVerschobeneTermine(child.child("verschobeneTermine"))
        val ausnahmeTermine = parseAusnahmeTermine(child.child("ausnahmeTermine"))
        val kundenTermine = parseKundenTermine(child.child("kundenTermine"))
        val termineVonListe = parseKundenTermine(child.child("termineVonListe"))
        val abholungWochentage = parseIntListFromSnapshot(child.child("defaultAbholungWochentage"))
        val auslieferungWochentage = parseIntListFromSnapshot(child.child("defaultAuslieferungWochentage"))
        val kundenTypNode = child.child("kundenTyp")
        val kundenTypStr = when {
            !kundenTypNode.exists() -> null
            else -> kundenTypNode.getValue(String::class.java)
                ?: kundenTypNode.getValue(Any::class.java)?.toString()
        }
        val kundenTypParsed = kundenTypStr?.trim()?.let { s ->
            try { KundenTyp.valueOf(s) } catch (_: Exception) { null }
        }
        val erstelltAm = optionalLong(child, "erstelltAm").takeIf { it > 0 } ?: customer.erstelltAm
        val intervalle = parseIntervalleWithErstelltAm(child.child("intervalle"), customer.intervalle)
        val base = customer.copy(
            id = id,
            verschobeneTermine = verschobeneTermine,
            ausnahmeTermine = ausnahmeTermine,
            kundenTermine = kundenTermine,
            termineVonListe = termineVonListe,
            defaultAbholungWochentage = abholungWochentage.ifEmpty { customer.defaultAbholungWochentage },
            defaultAuslieferungWochentage = auslieferungWochentage.ifEmpty { customer.defaultAuslieferungWochentage },
            abholungDatum = optionalLong(child, "abholungDatum"),
            auslieferungDatum = optionalLong(child, "auslieferungDatum"),
            wiederholen = optionalBoolean(child, "wiederholen"),
            intervallTage = optionalInt(child, "intervallTage").coerceIn(1, 365).takeIf { it in 1..365 } ?: 7,
            letzterTermin = optionalLong(child, "letzterTermin"),
            kundenTyp = kundenTypParsed ?: customer.kundenTyp,
            erstelltAm = erstelltAm,
            intervalle = intervalle
        )
        return if (child.child("kundenTyp").exists()) base else base.migrateKundenTyp()
    }

    /** Intervalle mit explizit gelesenem Long-Feldern (Firebase liefert Long oft als Double). */
    private fun parseIntervalleWithErstelltAm(intervalleNode: DataSnapshot, fallback: List<CustomerIntervall>): List<CustomerIntervall> {
        if (!intervalleNode.exists()) return fallback
        return intervalleNode.children.mapNotNull { entry ->
            entry.getValue(CustomerIntervall::class.java)?.let { iv ->
                val erstelltAmIv = optionalLong(entry, "erstelltAm").takeIf { it > 0 } ?: iv.erstelltAm
                val abholungDatumIv = optionalLong(entry, "abholungDatum").takeIf { it > 0 } ?: iv.abholungDatum
                val auslieferungDatumIv = optionalLong(entry, "auslieferungDatum").takeIf { it > 0 } ?: iv.auslieferungDatum
                iv.copy(
                    erstelltAm = erstelltAmIv,
                    abholungDatum = abholungDatumIv,
                    auslieferungDatum = auslieferungDatumIv
                )
            }
        }.ifEmpty { fallback }
    }

    fun parseIntListFromSnapshot(snapshot: DataSnapshot): List<Int> {
        if (!snapshot.exists()) return emptyList()
        val list = mutableListOf<Int>()
        snapshot.children.forEach { entry ->
            val v = entry.getValue(Any::class.java)
            val i = when (v) {
                is Number -> v.toInt()
                else -> null
            }
            if (i != null && i in 0..6) list.add(i)
        }
        return list.sorted()
    }

    fun parseVerschobeneTermine(snapshot: DataSnapshot): List<VerschobenerTermin> {
        if (!snapshot.exists()) return emptyList()
        val list = mutableListOf<VerschobenerTermin>()
        snapshot.children.forEach { entry ->
            val od = snapshotValueToLong(entry.child("originalDatum").getValue())
            val vd = snapshotValueToLong(entry.child("verschobenAufDatum").getValue())
            val typStr = entry.child("typ").getValue(String::class.java)
            val typ = when (typStr) {
                "AUSLIEFERUNG" -> TerminTyp.AUSLIEFERUNG
                else -> TerminTyp.ABHOLUNG
            }
            if (od != 0L || vd != 0L) list.add(VerschobenerTermin(originalDatum = od, verschobenAufDatum = vd, typ = typ))
        }
        return list
    }

    fun parseAusnahmeTermine(snapshot: DataSnapshot): List<AusnahmeTermin> {
        if (!snapshot.exists()) return emptyList()
        val list = mutableListOf<AusnahmeTermin>()
        snapshot.children.forEach { entry ->
            val datum = snapshotValueToLong(entry.child("datum").getValue())
            val typ = entry.child("typ").getValue(String::class.java) ?: "A"
            if (datum != 0L && typ in listOf("A", "L")) list.add(AusnahmeTermin(datum = datum, typ = typ))
        }
        return list
    }

    fun serializeVerschobeneTermine(list: List<VerschobenerTermin>): Map<String, Map<String, Any>> =
        list.mapIndexed { index, it ->
            index.toString() to mapOf(
                "originalDatum" to it.originalDatum,
                "verschobenAufDatum" to it.verschobenAufDatum,
                "typ" to it.typ.name
            )
        }.toMap()

    fun serializeAusnahmeTermine(list: List<AusnahmeTermin>): Map<String, Map<String, Any>> =
        list.mapIndexed { index, it ->
            index.toString() to mapOf("datum" to it.datum, "typ" to it.typ)
        }.toMap()

    fun parseKundenTermine(snapshot: DataSnapshot): List<KundenTermin> {
        if (!snapshot.exists()) return emptyList()
        val list = mutableListOf<KundenTermin>()
        snapshot.children.forEach { entry ->
            val datum = snapshotValueToLong(entry.child("datum").getValue())
            val typ = entry.child("typ").getValue(String::class.java) ?: "A"
            if (datum != 0L && typ in listOf("A", "L")) list.add(KundenTermin(datum = datum, typ = typ))
        }
        return list
    }

    fun serializeKundenTermine(list: List<KundenTermin>): Map<String, Map<String, Any>> =
        list.mapIndexed { index, it ->
            index.toString() to mapOf("datum" to it.datum, "typ" to it.typ)
        }.toMap()

    private fun optionalLong(snapshot: DataSnapshot, key: String): Long =
        snapshotValueToLong(snapshot.child(key).getValue())

    private fun optionalInt(snapshot: DataSnapshot, key: String): Int =
        (snapshot.child(key).getValue(Any::class.java) as? Number)?.toInt() ?: 7

    private fun optionalBoolean(snapshot: DataSnapshot, key: String): Boolean =
        snapshot.child(key).getValue(Boolean::class.java) ?: false

    private fun snapshotValueToLong(value: Any?): Long = when (value) {
        is Number -> value.toLong()
        else -> 0L
    }
}
