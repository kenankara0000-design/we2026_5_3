package com.example.we2026_5

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.ui.terminregel.TerminRegelManagerScreen
import com.example.we2026_5.ui.terminregel.TerminRegelManagerViewModel
import com.example.we2026_5.util.TerminRegelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class TerminRegelManagerActivity : AppCompatActivity() {

    private val viewModel: TerminRegelManagerViewModel by viewModel()
    private val regelRepository: TerminRegelRepository by inject()
    private val customerRepository: CustomerRepository by inject()
    private var customerId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        customerId = intent.getStringExtra("CUSTOMER_ID")

        setContent {
            MaterialTheme {
                val regeln by viewModel.regeln.collectAsState(initial = emptyList())
                TerminRegelManagerScreen(
                    regeln = regeln,
                    onBack = { finish() },
                    onNewRegel = {
                        startActivity(Intent(this@TerminRegelManagerActivity, TerminRegelErstellenActivity::class.java))
                    },
                    onRegelClick = { regel ->
                        if (customerId != null) {
                            wendeRegelAufKundeAn(regel)
                        } else {
                            showRegelInfoDialog(regel)
                        }
                    }
                )
            }
        }
    }

    private fun showRegelInfoDialog(regel: TerminRegel) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val aktuelleRegel = regelRepository.getRegelById(regel.id) ?: regel
                showRegelInfoDialogInternal(aktuelleRegel)
            } catch (e: Exception) {
                showRegelInfoDialogInternal(regel)
            }
        }
    }

    private fun showRegelInfoDialogInternal(regel: TerminRegel) {
        val wochentage = arrayOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")

        val infoText = buildString {
            append("Name: ${regel.name}\n\n")

            if (regel.beschreibung.isNotEmpty()) {
                append("Beschreibung: ${regel.beschreibung}\n\n")
            }

            if (regel.wochentagBasiert) {
                append("Typ: Wochentag-basiert\n\n")

                if (regel.startDatum > 0) {
                    val startDateText = com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(regel.startDatum)
                    append("Startdatum: $startDateText\n")
                }

                if (regel.abholungWochentag >= 0) {
                    append("Abholung: ${wochentage[regel.abholungWochentag]}\n")
                }

                if (regel.auslieferungWochentag >= 0) {
                    append("Auslieferung: ${wochentage[regel.auslieferungWochentag]}\n")
                }
                append("\n")
            } else {
                append("Typ: Datum-basiert\n\n")

                val abholungText = if (regel.abholungDatum > 0) {
                    com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(regel.abholungDatum)
                } else "Heute"
                append("Abholung: $abholungText\n")

                val auslieferungText = if (regel.auslieferungDatum > 0) {
                    com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(regel.auslieferungDatum)
                } else "Heute"
                append("Auslieferung: $auslieferungText\n\n")
            }

            if (regel.wiederholen) {
                append("Wiederholen: Ja\n")
                append("Intervall: Alle ${regel.intervallTage} Tage\n")
                if (regel.intervallAnzahl > 0) {
                    append("Anzahl: ${regel.intervallAnzahl} Wiederholungen\n")
                } else {
                    append("Anzahl: Unbegrenzt\n")
                }
            } else {
                append("Wiederholen: Nein\n")
            }

            append("\nVerwendungsanzahl: ${regel.verwendungsanzahl}x")
        }

        AlertDialog.Builder(this)
            .setTitle("Regel-Informationen")
            .setMessage(infoText)
            .setPositiveButton("Bearbeiten") { _, _ ->
                val intent = Intent(this, TerminRegelErstellenActivity::class.java).apply {
                    putExtra("REGEL_ID", regel.id)
                }
                startActivity(intent)
            }
            .setNegativeButton("Schließen", null)
            .show()
    }

    private fun wendeRegelAufKundeAn(regel: TerminRegel) {
        val id = customerId ?: return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val customer = customerRepository.getCustomerById(id)
                if (customer == null) {
                    Toast.makeText(this@TerminRegelManagerActivity, "Kunde nicht gefunden", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val hatBereitsIntervalle = customer.intervalle.isNotEmpty()

                if (hatBereitsIntervalle) {
                    val bestehendeRegelnText = customer.intervalle.joinToString("\n") { intervall ->
                        val abholungText = if (intervall.abholungDatum > 0) {
                            com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(intervall.abholungDatum)
                        } else "Heute"
                        val auslieferungText = if (intervall.auslieferungDatum > 0) {
                            com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(intervall.auslieferungDatum)
                        } else "Heute"
                        "• Abholung: $abholungText, Auslieferung: $auslieferungText"
                    }

                    AlertDialog.Builder(this@TerminRegelManagerActivity)
                        .setTitle("Regel anwenden")
                        .setMessage("Der Kunde hat bereits ${customer.intervalle.size} Termin-Regel(n):\n\n$bestehendeRegelnText\n\nNeue Regel: ${regel.name}\n\nMöchten Sie diese Regel hinzufügen oder die bestehenden Regeln ersetzen?")
                        .setPositiveButton("Hinzufügen") { _, _ ->
                            regelHinzufuegenMitBestaetigung(customer, regel)
                        }
                        .setNeutralButton("Ersetzen") { _, _ ->
                            regelErsetzenMitBestaetigung(customer, regel, bestehendeRegelnText)
                        }
                        .setNegativeButton("Abbrechen", null)
                        .show()
                } else {
                    regelHinzufuegenMitBestaetigung(customer, regel)
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelManagerActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun regelHinzufuegenMitBestaetigung(customer: Customer, regel: TerminRegel) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val neuesIntervall = TerminRegelManager.wendeRegelAufKundeAn(regel, customer)
                val abholungText = if (neuesIntervall.abholungDatum > 0) {
                    com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(neuesIntervall.abholungDatum)
                } else "Heute"
                val auslieferungText = if (neuesIntervall.auslieferungDatum > 0) {
                    com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(neuesIntervall.auslieferungDatum)
                } else "Heute"

                val hinzufuegenText = if (customer.intervalle.isNotEmpty()) {
                    "Diese Regel wird zu den bestehenden ${customer.intervalle.size} Regel(n) hinzugefügt:\n\n"
                } else {
                    "Diese Regel wird als erste Regel hinzugefügt:\n\n"
                }

                AlertDialog.Builder(this@TerminRegelManagerActivity)
                    .setTitle("Regel hinzufügen - Bestätigung")
                    .setMessage("$hinzufuegenText" +
                            "Regel: ${regel.name}\n" +
                            "Abholung: $abholungText\n" +
                            "Auslieferung: $auslieferungText\n" +
                            (if (neuesIntervall.wiederholen) "Wiederholen: Alle ${neuesIntervall.intervallTage} Tage\n" else "Wiederholen: Nein\n") +
                            "\nMöchten Sie fortfahren?")
                    .setPositiveButton("Ja, hinzufügen") { _, _ ->
                        regelHinzufuegen(customer, regel, neuesIntervall)
                    }
                    .setNegativeButton("Abbrechen", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelManagerActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun regelErsetzenMitBestaetigung(customer: Customer, regel: TerminRegel, bestehendeRegelnText: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val neuesIntervall = TerminRegelManager.wendeRegelAufKundeAn(regel, customer)
                val abholungText = if (neuesIntervall.abholungDatum > 0) {
                    com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(neuesIntervall.abholungDatum)
                } else "Heute"
                val auslieferungText = if (neuesIntervall.auslieferungDatum > 0) {
                    com.example.we2026_5.util.DateFormatter.formatDateWithLeadingZeros(neuesIntervall.auslieferungDatum)
                } else "Heute"

                AlertDialog.Builder(this@TerminRegelManagerActivity)
                    .setTitle("Regeln ersetzen - Bestätigung")
                    .setMessage("ACHTUNG: Alle bestehenden Regeln werden gelöscht!\n\n" +
                            "Wird gelöscht (${customer.intervalle.size} Regel(n)):\n$bestehendeRegelnText\n\n" +
                            "Wird hinzugefügt:\n" +
                            "Regel: ${regel.name}\n" +
                            "Abholung: $abholungText\n" +
                            "Auslieferung: $auslieferungText\n" +
                            (if (neuesIntervall.wiederholen) "Wiederholen: Alle ${neuesIntervall.intervallTage} Tage\n" else "Wiederholen: Nein\n") +
                            "\nMöchten Sie wirklich fortfahren?")
                    .setPositiveButton("Ja, ersetzen") { _, _ ->
                        regelErsetzen(customer, regel, neuesIntervall)
                    }
                    .setNegativeButton("Abbrechen", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelManagerActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun regelHinzufuegen(customer: Customer, regel: TerminRegel, neuesIntervall: CustomerIntervall) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val neueIntervalle = customer.intervalle.toMutableList()
                neueIntervalle.add(neuesIntervall)

                val updates = mapOf("intervalle" to neueIntervalle)
                val success = customerRepository.updateCustomer(customer.id, updates)

                if (success) {
                    regelRepository.incrementVerwendungsanzahl(regel.id)
                    Toast.makeText(this@TerminRegelManagerActivity, "Regel '${regel.name}' hinzugefügt", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@TerminRegelManagerActivity, "Fehler beim Hinzufügen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelManagerActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun regelErsetzen(customer: Customer, regel: TerminRegel, neuesIntervall: CustomerIntervall) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val neueIntervalle = listOf(neuesIntervall)

                val updates = mapOf("intervalle" to neueIntervalle)
                val success = customerRepository.updateCustomer(customer.id, updates)

                if (success) {
                    regelRepository.incrementVerwendungsanzahl(regel.id)
                    Toast.makeText(this@TerminRegelManagerActivity, "Regel '${regel.name}' ersetzt alle bestehenden Regeln", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@TerminRegelManagerActivity, "Fehler beim Ersetzen", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TerminRegelManagerActivity, "Fehler: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
