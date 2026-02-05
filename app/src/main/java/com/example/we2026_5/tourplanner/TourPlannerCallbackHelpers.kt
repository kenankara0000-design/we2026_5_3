package com.example.we2026_5.tourplanner

import com.example.we2026_5.VerschobenerTermin

/**
 * Hilfsfunktionen für TourPlanner-Callbacks.
 */
object TourPlannerCallbackHelpers {

    /** Serialisiert verschobeneTermine für Firebase (Map mit originalDatum, verschobenAufDatum, typ). */
    fun serializeVerschobeneTermine(list: List<VerschobenerTermin>): Map<String, Map<String, Any>> =
        list.mapIndexed { index, it ->
            index.toString() to mapOf(
                "originalDatum" to it.originalDatum,
                "verschobenAufDatum" to it.verschobenAufDatum,
                "typ" to it.typ.name
            )
        }.toMap()
}
