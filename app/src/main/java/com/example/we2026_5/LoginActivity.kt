package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private var loginError by mutableStateOf<String?>(null)
    private var isLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Nur bei Admin (E-Mail-Login) direkt in die App; anonyme Nutzer sehen immer das Login-Panel
        if (auth.currentUser != null && !auth.currentUser!!.isAnonymous) {
            startMainActivity()
            return
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorResource(R.color.background_light)
                ) {
                    if (isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LoginScreenContent(
                            errorMessage = loginError,
                            onDismissError = { loginError = null },
                            onAdminLogin = { email, password -> signInAdmin(email, password) },
                            onAnonymContinue = { signInAnonymously() }
                        )
                    }
                }
            }
        }
    }

    private fun signInAdmin(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            loginError = getString(R.string.login_error_admin)
            return
        }
        isLoading = true
        loginError = null
        val emailTrim = email.trim()
        val signInEmail = if (emailTrim.contains("@")) emailTrim else "$emailTrim@tourplaner.test"
        auth.signInWithEmailAndPassword(signInEmail, password)
            .addOnCompleteListener(this) { task ->
                isLoading = false
                if (task.isSuccessful) {
                    startMainActivity()
                } else {
                    loginError = getString(R.string.login_error_admin)
                }
            }
    }

    private fun signInAnonymously() {
        isLoading = true
        loginError = null
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                isLoading = false
                if (task.isSuccessful) {
                    startMainActivity()
                } else {
                    loginError = getString(R.string.error_login_failed)
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
        if (auth.currentUser != null && !auth.currentUser!!.isAnonymous) {
            startMainActivity()
        }
    }
}

@androidx.compose.runtime.Composable
private fun LoginScreenContent(
    errorMessage: String?,
    onDismissError: () -> Unit,
    onAdminLogin: (String, String) -> Unit,
    onAnonymContinue: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colorResource(R.color.primary_blue_dark)
        )
        Spacer(Modifier.height(32.dp))

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = colorResource(R.color.status_overdue),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedButton(onClick = onDismissError) {
                Text(stringResource(R.string.login_retry))
            }
            Spacer(Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(stringResource(R.string.login_admin_email_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.login_admin_password_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done)
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onAdminLogin(email, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.login_admin_submit))
        }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(
            onClick = onAnonymContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.login_anonym_continue))
        }
    }
}
