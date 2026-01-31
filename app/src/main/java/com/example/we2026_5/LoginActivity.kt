package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Prüfen ob bereits eingeloggt
        if (auth.currentUser != null) {
            startMainActivity()
            return
        }

        // Automatisch anonym anmelden (für Team-Nutzung)
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    startMainActivity()
                } else {
                    // Bei Fehler: Meldung anzeigen und beenden
                    android.widget.Toast.makeText(this, getString(R.string.error_login_failed), android.widget.Toast.LENGTH_LONG).show()
                    finish()
                }
            }
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        // Prüfen ob bereits eingeloggt
        if (auth.currentUser != null) {
            startMainActivity()
        }
    }
}
