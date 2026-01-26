package com.example.we2026_5

import android.app.Application
import com.example.we2026_5.di.appModule
import com.google.firebase.database.FirebaseDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class FirebaseConfig : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Firebase Realtime Database Offline-Konfiguration
        val realtimeDb = FirebaseDatabase.getInstance()
        realtimeDb.setPersistenceEnabled(true) // Offline-Persistence aktivieren
        
        // Koin starten
        startKoin {
            androidContext(this@FirebaseConfig)
            modules(appModule)
        }
    }
}