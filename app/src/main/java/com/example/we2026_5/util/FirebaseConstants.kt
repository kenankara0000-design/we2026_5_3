package com.example.we2026_5.util

/**
 * Zentrale Konstanten für Firebase Realtime Database Pfade.
 * 
 * Vorteile:
 * - Tippfehler-Sicherheit (z.B. "customer" vs "customers")
 * - Einheitliche Benennung im gesamten Projekt
 * - Einfaches Refactoring (nur 1 Stelle ändern)
 * - Dokumentation der DB-Struktur
 */
object FirebaseConstants {
    
    // Haupt-Collections
    const val CUSTOMERS = "customers"
    const val CUSTOMERS_FOR_TOUR = "customers_for_tour"
    const val TOUR_PLAENE = "tourPlaene"
    const val KUNDEN_LISTEN = "kundenListen"
    const val ARTICLES = "articles"
    const val KUNDEN_PREISE = "kundenPreise"
    const val TOUR_PREISE = "tourPreise"
    /** Listen- und Privat-Kundenpreise (Firebase-Pfad unverändert für Kompatibilität). */
    const val LISTEN_PRIVAT_KUNDENPREISE = "standardPreise"
    const val WASCH_ERFASSUNGEN = "waschErfassungen"
    
    // Customer-Felder (für Updates)
    const val FIELD_NAME = "name"
    const val FIELD_KUNDENNUMMER = "kundennummer"
    const val FIELD_KUNDEN_TYP = "kundenTyp"
    const val FIELD_ADRESSE = "adresse"
    const val FIELD_PLZ = "plz"
    const val FIELD_STADT = "stadt"
    const val FIELD_TELEFON = "telefon"
    const val FIELD_EMAIL = "email"
    const val FIELD_LATITUDE = "latitude"
    const val FIELD_LONGITUDE = "longitude"
    const val FIELD_NOTIZ = "notiz"
    const val FIELD_KUNDEN_TERMINE = "kundenTermine"
    const val FIELD_OHNE_TOUR = "ohneTour"
    const val FIELD_INTERVALLE = "intervalle"
    const val FIELD_KUNDE_PAUSIERT = "kundePausiert"
    const val FIELD_PAUSE_BIS = "pauseBis"
    const val FIELD_TOUR_ID = "tourId"
    const val FIELD_TOUR_SLOT_ID = "tourSlotId"
    const val FIELD_FOTOS = "fotos"
    const val FIELD_TAGE_ABHOLUNG_ZU_LIEFERUNG = "tageAbholungZuLieferung"
    
    // Tour-Felder
    const val FIELD_TOUR_NAME = "tourName"
    const val FIELD_TOUR_SLOTS = "tourSlots"
    const val FIELD_SLOT_ID = "slotId"
    const val FIELD_WOCHENTAG = "wochentag"
    const val FIELD_ZEITFENSTER = "zeitfenster"
    
    // Liste-Felder
    const val FIELD_LISTEN_NAME = "listenName"
    const val FIELD_LISTE_KUNDEN_IDS = "kundenIds"
    const val FIELD_TOUR_ART = "tourArt"
}
