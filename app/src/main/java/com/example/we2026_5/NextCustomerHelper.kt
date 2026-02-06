package com.example.we2026_5

import android.content.Intent

/**
 * Hilfs-Kontrakt für "Speichern und nächster Kunde" (einmalige Aktion aus dem Kundenmanager).
 * Zum Entfernen der Funktion: diese Datei löschen und alle Verwendungen entfernen.
 */
object NextCustomerHelper {
    const val EXTRA_CUSTOMER_IDS = "next_customer_ids"
    const val EXTRA_CURRENT_INDEX = "next_customer_index"
    const val RESULT_OPEN_NEXT = 2002
    const val RESULT_EXTRA_INDEX = "next_customer_index"

    fun putNextCustomerExtras(intent: Intent, customerIds: List<String>, currentIndex: Int) {
        intent.putExtra(EXTRA_CUSTOMER_IDS, ArrayList(customerIds))
        intent.putExtra(EXTRA_CURRENT_INDEX, currentIndex)
    }

    fun getNextCustomerIndex(intent: Intent?): Int =
        intent?.getIntExtra(EXTRA_CURRENT_INDEX, -1) ?: -1

    fun hasNextCustomerExtras(intent: Intent?): Boolean =
        intent != null && intent.hasExtra(EXTRA_CUSTOMER_IDS) && intent.hasExtra(EXTRA_CURRENT_INDEX)

    fun getCustomerIds(intent: Intent?): List<String>? =
        intent?.getStringArrayListExtra(EXTRA_CUSTOMER_IDS)
}
