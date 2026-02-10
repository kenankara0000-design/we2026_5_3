package com.example.we2026_5

import android.text.TextUtils
import com.example.we2026_5.util.AppTimeZone
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
    
    /**
     * Validiert E-Mail-Adresse
     * @param email E-Mail-Adresse zum Validieren
     * @return true wenn gültig, false sonst
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$",
            Pattern.CASE_INSENSITIVE
        )
        return emailPattern.matcher(email).matches()
    }
    
    /**
     * Validiert Name (mindestens 2 Zeichen, keine Sonderzeichen außer Leerzeichen, Bindestrich, Apostroph)
     * @param name Name zum Validieren
     * @return true wenn gültig, false sonst
     */
    fun isValidName(name: String): Boolean {
        if (name.isBlank()) return false
        
        // Mindestens 2 Zeichen
        if (name.trim().length < 2) return false
        
        // Erlaubt: Buchstaben, Leerzeichen, Bindestrich, Apostroph, Umlaute
        val namePattern = Pattern.compile("^[\\p{L}\\s'-]+$", Pattern.UNICODE_CASE)
        return namePattern.matcher(name.trim()).matches()
    }
    
    /**
     * Validiert Postleitzahl (Deutsches Format: 5-stellig)
     * @param plz Postleitzahl zum Validieren
     * @return true wenn gültig, false sonst
     */
    fun isValidPostalCode(plz: String): Boolean {
        if (plz.isBlank()) return true // Optional
        
        val cleaned = plz.replace(Regex("[^0-9]"), "")
        return cleaned.length == 5 && cleaned.toIntOrNull() != null
    }
    
    /**
     * Validiert Datum (muss in der Zukunft oder heute sein für Termine)
     * @param timeInMillis Zeitstempel in Millisekunden
     * @param allowToday true wenn heute erlaubt ist, false sonst
     * @return true wenn gültig, false sonst
     */
    fun isValidFutureDate(timeInMillis: Long, allowToday: Boolean = true): Boolean {
        if (timeInMillis <= 0) return false
        
        val today = AppTimeZone.newCalendar()
        today.set(java.util.Calendar.HOUR_OF_DAY, 0)
        today.set(java.util.Calendar.MINUTE, 0)
        today.set(java.util.Calendar.SECOND, 0)
        today.set(java.util.Calendar.MILLISECOND, 0)
        
        val target = AppTimeZone.newCalendar()
        target.timeInMillis = timeInMillis
        target.set(java.util.Calendar.HOUR_OF_DAY, 0)
        target.set(java.util.Calendar.MINUTE, 0)
        target.set(java.util.Calendar.SECOND, 0)
        target.set(java.util.Calendar.MILLISECOND, 0)
        
        return if (allowToday) {
            target.timeInMillis >= today.timeInMillis
        } else {
            target.timeInMillis > today.timeInMillis
        }
    }
    
    /**
     * Validiert Datumsbereich (von-Datum muss vor bis-Datum sein)
     * @param vonDatum Startdatum in Millisekunden
     * @param bisDatum Enddatum in Millisekunden
     * @return true wenn gültig, false sonst
     */
    fun isValidDateRange(vonDatum: Long, bisDatum: Long): Boolean {
        if (vonDatum <= 0 || bisDatum <= 0) return false
        return vonDatum <= bisDatum
    }
    
    /**
     * Validiert Intervall-Tage (muss zwischen 1 und 365 sein)
     * @param tage Anzahl der Tage
     * @return true wenn gültig, false sonst
     */
    fun isValidIntervalDays(tage: Int): Boolean {
        return tage in 1..365
    }
    
    /**
     * Validiert Intervall-Anzahl (muss positiv sein, 0 = unbegrenzt)
     * @param anzahl Anzahl der Wiederholungen
     * @return true wenn gültig, false sonst
     */
    fun isValidIntervalCount(anzahl: Int): Boolean {
        return anzahl >= 0
    }
    
    /**
     * Bereinigt und normalisiert Text-Eingaben
     * @param text Text zum Bereinigen
     * @return Bereinigter Text
     */
    fun sanitizeText(text: String): String {
        return text.trim()
            .replace(Regex("\\s+"), " ") // Mehrfache Leerzeichen zu einem
            .replace(Regex("^\\s+|\\s+$"), "") // Leerzeichen am Anfang/Ende entfernen
    }
    
    /**
     * Validiert Notizen (maximale Länge)
     * @param notes Notizen zum Validieren
     * @param maxLength Maximale Länge (Standard: 1000 Zeichen)
     * @return true wenn gültig, false sonst
     */
    fun isValidNotes(notes: String, maxLength: Int = 1000): Boolean {
        return notes.length <= maxLength
    }
}
