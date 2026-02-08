package com.example.we2026_5.auth

import com.google.firebase.auth.FirebaseAuth

/**
 * Prüft, ob der aktuell angemeldete Nutzer volle Rechte hat.
 * Es gibt keine Admin/Anonymous-Trennung mehr: Alle angemeldeten Nutzer (nur anonym) haben volle Rechte.
 */
class AdminChecker(private val auth: FirebaseAuth) {

    fun isAdmin(): Boolean {
        return auth.currentUser != null
    }
}
