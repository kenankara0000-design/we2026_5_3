package com.example.we2026_5

import android.app.Application
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class FirebaseConfig : Application() {
    override fun onCreate() {
        super.onCreate()
        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Offline-Modus aktiv
            .build()
        db.firestoreSettings = settings
    }
}