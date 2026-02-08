package com.example.we2026_5.auth

import com.google.firebase.auth.FirebaseAuth

/**
 * Prüft, ob der aktuell angemeldete Nutzer Admin-Rechte hat.
 * Test-Phase: Admin = E-Mail/Passwort-Account mit dieser E-Mail.
 * Später: Admin = Geräte-ID in Firebase (nur dein Handy).
 */
class AdminChecker(private val auth: FirebaseAuth) {

    /** Test-Admin: E-Mail admin@tourplaner.test, Passwort „test23“ (Firebase min. 6 Zeichen). In Firebase Console (Authentication → E-Mail/Passwort) anlegen. */
    companion object {
        const val TEST_ADMIN_EMAIL = "admin@tourplaner.test"
    }

    fun isAdmin(): Boolean {
        val user = auth.currentUser ?: return false
        if (user.isAnonymous) return false
        return user.email?.equals(TEST_ADMIN_EMAIL, ignoreCase = true) == true
    }
}
