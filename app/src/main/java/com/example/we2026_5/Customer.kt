package com.example.we2026_5

data class Customer(
    val id: String = "",
    val name: String = "",
    val adresse: String = "",
    val telefon: String = "",
    val notizen: String = "",
    val intervallTage: Int = 7,
    val letzterTermin: Long = 0,
    val abholungErfolgt: Boolean = false,
    val auslieferungErfolgt: Boolean = false,
    val urlaubVon: Long = 0,
    val urlaubBis: Long = 0,
    val verschobenAufDatum: Long = 0,
    val fotoUrls: List<String> = listOf(),
    val istImUrlaub: Boolean = false // Das hat gefehlt!
)