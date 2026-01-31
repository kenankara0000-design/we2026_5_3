package com.example.we2026_5.util

import java.io.IOException
import java.net.UnknownHostException

/**
 * Zentrale Zuordnung von Exceptions zu nutzerlesbaren Fehlermeldungen.
 * Texte entsprechen inhaltlich strings.xml (error_firebase_allgemein, error_save_generic,
 * error_delete_generic, error_load_generic). ViewModels/Repositories nutzen diese Meldungen
 * ohne Context; UI kann bei Bedarf getString(R.string.xxx) für Lokalisierung verwenden.
 */
object AppErrorMapper {

    /**
     * Mappt eine Exception auf eine anzeigbare Fehlermeldung.
     * Firebase/Netzwerk-Fehler → Verbindungsfehler; sonst generische Meldung.
     */
    fun toMessage(throwable: Throwable?): String {
        if (throwable == null) return "Ein Fehler ist aufgetreten. Bitte erneut versuchen."
        return when {
            throwable is UnknownHostException || throwable is IOException ->
                "Verbindungsfehler. Bitte prüfen Sie die Internetverbindung und versuchen Sie es erneut."
            throwable.message?.contains("network", ignoreCase = true) == true ||
                throwable.message?.contains("connection", ignoreCase = true) == true ->
                "Verbindungsfehler. Bitte prüfen Sie die Internetverbindung und versuchen Sie es erneut."
            else ->
                throwable.message?.takeIf { it.isNotBlank() } ?: "Ein Fehler ist aufgetreten. Bitte erneut versuchen."
        }
    }

    /** Für Speicher-Fehler (z. B. updateCustomer). */
    fun toSaveMessage(throwable: Throwable?): String =
        toMessage(throwable).takeIf { it.isNotBlank() } ?: "Fehler beim Speichern. Bitte erneut versuchen."

    /** Für Lösch-Fehler (z. B. deleteCustomer). */
    fun toDeleteMessage(throwable: Throwable?): String =
        toMessage(throwable).takeIf { it.isNotBlank() } ?: "Fehler beim Löschen. Bitte erneut versuchen."

    /** Für Lade-Fehler (z. B. loadStatistics). */
    fun toLoadMessage(throwable: Throwable?): String =
        toMessage(throwable).takeIf { it.isNotBlank() } ?: "Fehler beim Laden. Bitte erneut versuchen."
}
