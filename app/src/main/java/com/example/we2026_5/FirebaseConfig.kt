package com.example.we2026_5

import android.app.Application
import com.example.we2026_5.di.appModule
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FirebaseConfig : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Firebase Firestore Offline-Konfiguration
        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Offline-Modus aktiv
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED) // Unbegrenzter Cache
            .build()
        db.firestoreSettings = settings
        
        // Koin starten
        startKoin {
            androidContext(this@FirebaseConfig)
            modules(appModule)
        }
    }
}