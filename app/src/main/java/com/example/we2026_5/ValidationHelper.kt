package com.example.we2026_5

import android.text.TextUtils
import java.util.regex.Pattern

object ValidationHelper {
    
    /**
     * Validiert Telefonnummer (Deutsches Format)
     * Akzeptiert: +49..., 0049..., 0..., oder nur Ziffern
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        if (phone.isBlank()) return true // Optional
        
        // Entferne Leerzeichen, Bindestriche, Klammern
        val cleaned = phone.replace(Regex("[\\s\\-\\(\\)]"), "")
        
        // Pattern: +49..., 0049..., 0..., oder nur Ziffern (mindestens 5)
        val pattern = Pattern.compile("^(\\+49|0049|0)?[1-9]\\d{4,}$")
        return pattern.matcher(cleaned).matches()
    }
    
    /**
     * Validiert Adresse (mindestens Straße + PLZ)
     */
    fun isValidAddress(address: String): Boolean {
        if (address.isBlank()) return true // Optional
        
        // Mindestens 5 Zeichen (kurze Adresse)
        if (address.length < 5) return false
        
        // Sollte mindestens eine Zahl enthalten (für Hausnummer oder PLZ)
        val hasNumber = address.any { it.isDigit() }
        if (!hasNumber) return false
        
        return true
    }
    
    /**
     * Prüft ob ein Kunde mit gleichem Wochentag + Reihenfolge bereits existiert
     */
    suspend fun checkDuplicateReihenfolge(
        repository: com.example.we2026_5.data.repository.CustomerRepository,
        wochentag: Int,
        reihenfolge: Int,
        excludeCustomerId: String? = null
    ): Customer? {
        val allCustomers = repository.getAllCustomers()
        return allCustomers.firstOrNull { customer ->
            customer.wochentag == wochentag && 
            customer.reihenfolge == reihenfolge &&
            customer.id != excludeCustomerId
        }
    }
    
    /**
     * Formatiert Telefonnummer für Anzeige
     */
    fun formatPhoneNumber(phone: String): String {
        if (phone.isBlank()) return phone
        
        val cleaned = phone.replace(Regex("[^0-9+]"), "")
        
        // Deutsche Formatierung
        return when {
            cleaned.startsWith("+49") -> {
                val rest = cleaned.substring(3)
                if (rest.length >= 10) {
                    "+49 ${rest.substring(0, rest.length - 8)} ${rest.substring(rest.length - 8, rest.length - 4)} ${rest.substring(rest.length - 4)}"
                } else {
                    cleaned
                }
            }
            cleaned.startsWith("0049") -> {
                val rest = cleaned.substring(4)
                if (rest.length >= 10) {
                    "+49 ${rest.substring(0, rest.length - 8)} ${rest.substring(rest.length - 8, rest.length - 4)} ${rest.substring(rest.length - 4)}"
                } else {
                    cleaned
                }
            }
            cleaned.startsWith("0") && cleaned.length >= 10 -> {
                "${cleaned.substring(0, cleaned.length - 8)} ${cleaned.substring(cleaned.length - 8, cleaned.length - 4)} ${cleaned.substring(cleaned.length - 4)}"
            }
            else -> phone
        }
    }
}
