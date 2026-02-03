package com.example.we2026_5.util

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.launch

/**
 * Helper für einheitliche Fehlerbehandlung in Compose Screens.
 * 
 * Verwendung:
 * ```
 * val snackbarHostState = rememberSnackbarHostState()
 * 
 * // Im ViewModel: errorMessage als StateFlow/LiveData
 * val errorMessage by viewModel.errorMessage.collectAsState(initial = null)
 * 
 * // Im Screen: Snackbar anzeigen
 * ShowErrorSnackbar(
 *     errorMessage = errorMessage,
 *     snackbarHostState = snackbarHostState,
 *     onErrorShown = { viewModel.clearErrorMessage() }
 * )
 * ```
 */
@Composable
fun ShowErrorSnackbar(
    errorMessage: String?,
    snackbarHostState: SnackbarHostState,
    onErrorShown: () -> Unit = {}
) {
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            launch {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = androidx.compose.material3.SnackbarDuration.Short
                )
                onErrorShown()
            }
        }
    }
}

/**
 * Erstellt einen SnackbarHostState für die Verwendung in Scaffold.
 */
@Composable
fun rememberSnackbarHostState(): SnackbarHostState {
    return remember { SnackbarHostState() }
}
