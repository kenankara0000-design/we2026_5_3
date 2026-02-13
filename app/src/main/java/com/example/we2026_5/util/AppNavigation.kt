package com.example.we2026_5.util

import android.content.Context
import android.content.Intent
import com.example.we2026_5.*
import com.example.we2026_5.ui.urlaub.UrlaubActivity

/**
 * Zentrale Intent-Factory für typ-sichere Activity-Navigation.
 * 
 * Vorteile:
 * - Keine Tippfehler bei Intent-Keys (z.B. "CUSTOMER_ID" vs "customer_id")
 * - IDE-Autovervollständigung für alle verfügbaren Extras
 * - Einfaches Refactoring (nur 1 Stelle ändern statt 50x suchen/ersetzen)
 * - Klare Dokumentation, welche Activity welche Parameter erwartet
 * 
 * Verwendung:
 * ```
 * // Alt (unsicher):
 * startActivity(Intent(this, CustomerDetailActivity::class.java).apply {
 *     putExtra("CUSTOMER_ID", "abc123")
 * })
 * 
 * // Neu (typ-sicher):
 * startActivity(AppNavigation.toCustomerDetail(this, customerId = "abc123"))
 * ```
 */
object AppNavigation {

    // =============================
    // INTENT EXTRA KEYS (zentral)
    // =============================
    
    object Keys {
        const val CUSTOMER_ID = "CUSTOMER_ID"
        const val TOUR_ID = "TOUR_ID"
        const val LISTE_ID = "LISTE_ID"
        const val BELEG_MONTH_KEY = "BELEG_MONTH_KEY"
        const val OPEN_FORMULAR = "OPEN_FORMULAR"
        const val OPEN_FORMULAR_WITH_CAMERA = "OPEN_FORMULAR_WITH_CAMERA"
        const val OPEN_ERFASSEN = "OPEN_ERFASSEN"
        const val SELECTED_TAB = "SELECTED_TAB"
        const val RETURN_FROM_DETAIL = "RETURN_FROM_DETAIL"
    }

    // =============================
    // MAIN FLOWS
    // =============================

    /** Hauptbildschirm (Dashboard) */
    fun toMain(context: Context): Intent {
        return Intent(context, MainActivity::class.java)
    }

    /** Login-Bildschirm (mit Task-Clear für Logout) */
    fun toLogin(context: Context, clearTask: Boolean = false): Intent {
        return Intent(context, LoginActivity::class.java).apply {
            if (clearTask) {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        }
    }

    /** Einstellungen */
    fun toSettings(context: Context): Intent {
        return Intent(context, SettingsActivity::class.java)
    }

    // =============================
    // KUNDEN
    // =============================

    /** Kundenverwaltung (Kundenübersicht mit 3 Tabs) */
    fun toCustomerManager(context: Context, selectedTab: Int? = null, returnFromDetail: Boolean = false): Intent {
        return Intent(context, CustomerManagerActivity::class.java).apply {
            selectedTab?.let { putExtra(Keys.SELECTED_TAB, it) }
            if (returnFromDetail) putExtra(Keys.RETURN_FROM_DETAIL, true)
        }
    }

    /** Neuen Kunden anlegen */
    fun toAddCustomer(context: Context): Intent {
        return Intent(context, AddCustomerActivity::class.java)
    }

    /** Kundendetails anzeigen */
    fun toCustomerDetail(context: Context, customerId: String): Intent {
        return Intent(context, CustomerDetailActivity::class.java).apply {
            putExtra(Keys.CUSTOMER_ID, customerId)
        }
    }

    // =============================
    // TOUREN
    // =============================

    /** Tourenplaner */
    fun toTourPlanner(context: Context): Intent {
        return Intent(context, TourPlannerActivity::class.java)
    }

    /** Kartenansicht (mit Tour-IDs) */
    fun toMapView(context: Context, tourIds: ArrayList<String>? = null): Intent {
        return Intent(context, MapViewActivity::class.java).apply {
            tourIds?.let { putStringArrayListExtra("TOUR_IDS", it) }
        }
    }

    // =============================
    // TERMINE
    // =============================

    /** Ausnahme-Termin (Feiertag/Urlaub/Sonderfall) */
    fun toAusnahmeTermin(context: Context, customerId: String? = null): Intent {
        return Intent(context, AusnahmeTerminActivity::class.java).apply {
            customerId?.let { putExtra(Keys.CUSTOMER_ID, it) }
        }
    }

    /** Unregelmäßigen Termin anlegen */
    fun toTerminAnlegenUnregelmaessig(context: Context, customerId: String, name: String): Intent {
        return Intent(context, TerminAnlegenUnregelmaessigActivity::class.java).apply {
            putExtra(Keys.CUSTOMER_ID, customerId)
            putExtra("CUSTOMER_NAME", name)
        }
    }

    /** Urlaub verwalten */
    fun toUrlaub(context: Context, customerId: String? = null): Intent {
        return Intent(context, UrlaubActivity::class.java).apply {
            customerId?.let { putExtra(Keys.CUSTOMER_ID, it) }
        }
    }

    // =============================
    // ERFASSUNG
    // =============================

    /** Erfassungs-Menü (Waschen/Belege) */
    fun toErfassungMenu(context: Context): Intent {
        return Intent(context, ErfassungMenuActivity::class.java)
    }

    /** Wäsche erfassen (mit optionalen Shortcuts) */
    fun toWaschenErfassung(
        context: Context,
        customerId: String? = null,
        openFormularWithCamera: Boolean = false,
        openFormular: Boolean = false,
        openErfassen: Boolean = false,
        belegMonthKey: String? = null
    ): Intent {
        return Intent(context, WaschenErfassungActivity::class.java).apply {
            customerId?.let { putExtra(Keys.CUSTOMER_ID, it) }
            if (openFormularWithCamera) putExtra(Keys.OPEN_FORMULAR_WITH_CAMERA, true)
            if (openFormular) putExtra(Keys.OPEN_FORMULAR, true)
            if (openErfassen) putExtra(Keys.OPEN_ERFASSEN, true)
            belegMonthKey?.let { putExtra(Keys.BELEG_MONTH_KEY, it) }
        }
    }

    /** Belege anzeigen (Monatsübersicht) */
    fun toBelege(context: Context): Intent {
        return Intent(context, BelegeActivity::class.java)
    }

    // =============================
    // LISTEN
    // =============================

    /** Kundenlisten-Verwaltung */
    fun toKundenListen(context: Context): Intent {
        return Intent(context, KundenListenActivity::class.java)
    }

    /** Neue Liste erstellen */
    fun toListeErstellen(context: Context): Intent {
        return Intent(context, ListeErstellenActivity::class.java)
    }

    /** Bestehende Liste bearbeiten */
    fun toListeBearbeiten(context: Context, listeId: String): Intent {
        return Intent(context, ListeBearbeitenActivity::class.java).apply {
            putExtra(Keys.LISTE_ID, listeId)
        }
    }

    // =============================
    // PREISE & ARTIKEL
    // =============================

    /** Preis-Menü (Kundenpreise/Tourpreisliste/Artikel) */
    fun toPreise(context: Context): Intent {
        return Intent(context, PreiseActivity::class.java)
    }

    /** Kundenpreise verwalten */
    fun toKundenpreise(context: Context): Intent {
        return Intent(context, KundenpreiseActivity::class.java)
    }

    /** Tourpreisliste verwalten */
    fun toTourPreisliste(context: Context): Intent {
        return Intent(context, TourPreislisteActivity::class.java)
    }

    /** Artikelverwaltung */
    fun toArtikelVerwaltung(context: Context): Intent {
        return Intent(context, ArtikelVerwaltungActivity::class.java)
    }

    // =============================
    // IMPORT & STATS
    // =============================

    /** Datenimport-Menü */
    fun toDataImport(context: Context): Intent {
        return Intent(context, DataImportActivity::class.java)
    }

    /** SevDesk-Import */
    fun toSevDeskImport(context: Context): Intent {
        return Intent(context, SevDeskImportActivity::class.java)
    }

    /** Statistiken */
    fun toStatistics(context: Context): Intent {
        return Intent(context, StatisticsActivity::class.java)
    }
}
