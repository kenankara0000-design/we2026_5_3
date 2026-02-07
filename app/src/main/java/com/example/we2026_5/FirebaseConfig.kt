package com.example.we2026_5

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.we2026_5.di.appModule
import com.example.we2026_5.util.AgentDebugLog
import com.google.firebase.database.FirebaseDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.io.File

class FirebaseConfig : Application() {
    override fun onCreate() {
        super.onCreate()
        AgentDebugLog.setLogFile(File(filesDir, "agent_debug.ndjson"))
        
        // Dunkelmodus global deaktivieren
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        
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