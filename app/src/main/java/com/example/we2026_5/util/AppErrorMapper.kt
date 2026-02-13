package com.example.we2026_5.util

import android.content.Context
import com.example.we2026_5.R
import java.io.IOException
import java.net.UnknownHostException

/**
 * Zentrale Zuordnung von Exceptions zu nutzerlesbaren Fehlermeldungen.
 *
 * **Bevorzugt:** `toMessage(context, throwable)` für lokalisierte String-Ressourcen.
 * **Fallback:** `toMessage(throwable)` ohne Context (hardcodierte deutsche Texte).
 *
 * ViewModels/Repositories ohne Context nutzen die Variante ohne Context.
 * UI-Code (Activities, Composables) sollte die Variante mit Context bevorzugen.
 */
object AppErrorMapper {

    /** Prüft, ob es ein Netzwerk-/Verbindungsfehler ist. */
    fun isNetworkError(throwable: Throwable?): Boolean {
        if (throwable == null) return false
        return throwable is UnknownHostException ||
            throwable is IOException ||
            throwable.message?.contains("network", ignoreCase = true) == true ||
            throwable.message?.contains("connection", ignoreCase = true) == true
    }

    /**
     * Mappt eine Exception auf eine lokalisierte Fehlermeldung (mit Context).
     */
    fun toMessage(context: Context, throwable: Throwable?): String {
        if (throwable == null) return context.getString(R.string.error_daten_laden)
        return when {
            isNetworkError(throwable) -> context.getString(R.string.error_netzwerk)
            else -> throwable.message?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.error_daten_laden)
        }
    }

    /** Für Speicher-Fehler (mit Context). */
    fun toSaveMessage(context: Context, throwable: Throwable?): String = when {
        isNetworkError(throwable) -> context.getString(R.string.error_netzwerk)
        throwable?.message != null -> context.getString(R.string.error_speichern_detail, throwable.message)
        else -> context.getString(R.string.error_save_generic)
    }

    /** Für Lösch-Fehler (mit Context). */
    fun toDeleteMessage(context: Context, throwable: Throwable?): String = when {
        isNetworkError(throwable) -> context.getString(R.string.error_netzwerk)
        throwable?.message != null -> context.getString(R.string.error_loeschen_detail, throwable.message)
        else -> context.getString(R.string.error_delete_generic)
    }

    /** Für Lade-Fehler (mit Context). */
    fun toLoadMessage(context: Context, throwable: Throwable?): String = when {
        isNetworkError(throwable) -> context.getString(R.string.error_netzwerk)
        throwable?.message != null -> context.getString(R.string.error_laden, throwable.message)
        else -> context.getString(R.string.error_load_generic)
    }

    // ── Varianten ohne Context (Fallback für ViewModels/Repositories) ──

    /**
     * Mappt eine Exception auf eine anzeigbare Fehlermeldung (ohne Context).
     */
    fun toMessage(throwable: Throwable?): String {
        if (throwable == null) return "Ein Fehler ist aufgetreten. Bitte erneut versuchen."
        return when {
            isNetworkError(throwable) ->
                "Verbindungsfehler. Bitte prüfen Sie die Internetverbindung und versuchen Sie es erneut."
            else ->
                throwable.message?.takeIf { it.isNotBlank() } ?: "Ein Fehler ist aufgetreten. Bitte erneut versuchen."
        }
    }

    /** Für Speicher-Fehler (ohne Context). */
    fun toSaveMessage(throwable: Throwable?): String =
        toMessage(throwable).takeIf { it.isNotBlank() } ?: "Fehler beim Speichern. Bitte erneut versuchen."

    /** Für Lösch-Fehler (ohne Context). */
    fun toDeleteMessage(throwable: Throwable?): String =
        toMessage(throwable).takeIf { it.isNotBlank() } ?: "Fehler beim Löschen. Bitte erneut versuchen."

    /** Für Lade-Fehler (ohne Context). */
    fun toLoadMessage(throwable: Throwable?): String =
        toMessage(throwable).takeIf { it.isNotBlank() } ?: "Fehler beim Laden. Bitte erneut versuchen."
}
