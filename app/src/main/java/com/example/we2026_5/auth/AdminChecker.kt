package com.example.we2026_5.auth

import com.google.firebase.auth.FirebaseAuth

/**
 * Prüft, ob der aktuell angemeldete Nutzer volle Rechte hat.
 * Admin-Prüfung deaktiviert: Alle Nutzer haben volle Rechte (u. a. Drag-and-Drop, Termine bearbeiten).
 */
class AdminChecker(private val auth: FirebaseAuth) {

    fun isAdmin(): Boolean {
        return true
    }
}
