package com.example.we2026_5.util

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.we2026_5.R

/**
 * Zentraler Helper für einheitliche Compose-Dialoge.
 * Reduziert Code-Duplikate und sorgt für konsistente UX (Buttons, Farben, Texte).
 */
object ComposeDialogHelper {

    /**
     * Standard-Bestätigungs-Dialog (z.B. Löschen, Abmelden, Reset).
     * 
     * @param title Titel des Dialogs
     * @param message Nachrichtentext
     * @param confirmText Text für Bestätigen-Button (default: "OK")
     * @param cancelText Text für Abbrechen-Button (default: "Abbrechen")
     * @param isDestructive true → Bestätigen-Button rot (für Löschen/Reset)
     * @param onDismiss wird bei Dismiss aufgerufen
     * @param onConfirm wird bei Bestätigung aufgerufen
     */
    @Composable
    fun ConfirmDialog(
        visible: Boolean,
        title: String,
        message: String,
        confirmText: String = stringResource(R.string.dialog_ok),
        cancelText: String = stringResource(R.string.btn_cancel),
        isDestructive: Boolean = false,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        if (!visible) return

        val confirmColor = if (isDestructive) {
            colorResource(R.color.status_overdue)
        } else {
            colorResource(R.color.primary_blue)
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = confirmColor)
                ) {
                    Text(confirmText)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(cancelText)
                }
            }
        )
    }

    /**
     * Info-Dialog (nur OK-Button, keine Bestätigung nötig).
     * 
     * @param title Titel
     * @param message Nachricht
     * @param okText Text für OK-Button (default: "OK")
     * @param onDismiss wird bei Schließen aufgerufen
     */
    @Composable
    fun InfoDialog(
        visible: Boolean,
        title: String,
        message: String,
        okText: String = stringResource(R.string.dialog_ok),
        onDismiss: () -> Unit
    ) {
        if (!visible) return

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(message) },
            confirmButton = {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(okText)
                }
            }
        )
    }

    /**
     * Custom-Dialog: Für komplexere Dialoge (z.B. mit eigenem @Composable Content).
     * 
     * @param title Titel
     * @param content Composable-Block für Dialog-Inhalt
     * @param confirmText Text für Bestätigen-Button
     * @param cancelText Text für Abbrechen-Button
     * @param confirmEnabled ob Bestätigen-Button aktiv ist
     * @param isDestructive true → Bestätigen-Button rot
     * @param onDismiss wird bei Dismiss aufgerufen
     * @param onConfirm wird bei Bestätigung aufgerufen
     */
    @Composable
    fun CustomDialog(
        visible: Boolean,
        title: String,
        content: @Composable () -> Unit,
        confirmText: String = stringResource(R.string.dialog_ok),
        cancelText: String = stringResource(R.string.btn_cancel),
        confirmEnabled: Boolean = true,
        isDestructive: Boolean = false,
        onDismiss: () -> Unit,
        onConfirm: () -> Unit
    ) {
        if (!visible) return

        val confirmColor = if (isDestructive) {
            colorResource(R.color.status_overdue)
        } else {
            colorResource(R.color.primary_blue)
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = content,
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = confirmColor),
                    enabled = confirmEnabled
                ) {
                    Text(confirmText)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(cancelText)
                }
            }
        )
    }
}

/**
 * State-Holder für Dialoge in Compose.
 * Vereinfacht die Verwaltung von Dialog-Sichtbarkeit + Daten.
 * 
 * Beispiel:
 * ```
 * val deleteDialog = rememberDialogState<String>()
 * 
 * Button(onClick = { deleteDialog.show("item_id_123") }) { Text("Löschen") }
 * 
 * ComposeDialogHelper.ConfirmDialog(
 *     visible = deleteDialog.isVisible,
 *     title = "Löschen?",
 *     message = "Item wirklich löschen?",
 *     isDestructive = true,
 *     onDismiss = { deleteDialog.hide() },
 *     onConfirm = {
 *         val id = deleteDialog.data
 *         viewModel.deleteItem(id)
 *     }
 * )
 * ```
 */
data class DialogState<T>(
    val isVisible: Boolean = false,
    val data: T? = null
)

@Composable
fun <T> rememberDialogState(): MutableState<DialogState<T>> {
    return remember { mutableStateOf(DialogState()) }
}

fun <T> MutableState<DialogState<T>>.show(data: T? = null) {
    this.value = DialogState(isVisible = true, data = data)
}

fun <T> MutableState<DialogState<T>>.hide() {
    this.value = DialogState(isVisible = false, data = null)
}
