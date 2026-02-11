package com.example.we2026_5

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.we2026_5.R
import com.example.we2026_5.util.DateFormatter
import com.example.we2026_5.util.TerminBerechnungUtils
import com.example.we2026_5.util.tageAzuLOrDefault
import com.example.we2026_5.ui.common.TerminDatumKalenderContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import androidx.lifecycle.lifecycleScope

@OptIn(ExperimentalMaterial3Api::class)
class AusnahmeTerminActivity : AppCompatActivity() {

    /** Intent-Extra: Wenn true, wird bei Datumswahl ein A-Termin angelegt und L automatisch (A + tageAzuL). */
    companion object {
        const val EXTRA_ADD_ABHOLUNG_MIT_LIEFERUNG = "ADD_ABHOLUNG_MIT_LIEFERUNG"
    }

    private val repository: com.example.we2026_5.data.repository.CustomerRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val customerId = intent.getStringExtra("CUSTOMER_ID") ?: run {
            Toast.makeText(this, getString(R.string.error_customer_id_missing), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val addAbholungMitLieferung = intent.getBooleanExtra(EXTRA_ADD_ABHOLUNG_MIT_LIEFERUNG, false)
        val titleStr = if (addAbholungMitLieferung) getString(R.string.label_neu_termin)
        else getString(R.string.termin_anlegen_option_ausnahme)
        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val primaryBlue = Color(ContextCompat.getColor(context, R.color.primary_blue))
                var customer by remember { mutableStateOf<Customer?>(null) }

                LaunchedEffect(customerId) {
                    customer = withContext(Dispatchers.IO) { repository.getCustomerById(customerId) }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    titleStr,
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = { },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryBlue)
                        )
                    }
                ) { paddingValues ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp)
                    ) {
                        TerminDatumKalenderContent(
                            onDismiss = { finish() },
                            onDateSelected = { datum ->
                                if (addAbholungMitLieferung) {
                                    confirmThenSaveAbholungMitLieferung(customerId, datum)
                                } else {
                                    AlertDialog.Builder(this@AusnahmeTerminActivity)
                                        .setTitle(getString(R.string.termin_anlegen_ausnahme_typ_waehlen))
                                        .setItems(
                                            arrayOf(
                                                getString(R.string.termin_anlegen_ausnahme_abholung),
                                                getString(R.string.termin_anlegen_ausnahme_auslieferung),
                                                getString(R.string.termin_anlegen_ausnahme_a_plus_l)
                                            )
                                        ) { _, which ->
                                            when (which) {
                                                0 -> confirmThenSaveAusnahmeNurA(customerId, datum)
                                                1 -> confirmThenSaveAusnahmeNurL(customerId, datum)
                                                2 -> confirmThenSaveAusnahmeAbholungMitLieferung(customerId, datum)
                                            }
                                        }
                                        .setNegativeButton(getString(R.string.btn_cancel), null)
                                        .show()
                                }
                            },
                            aWochentage = emptyList(),
                            lWochentage = emptyList(),
                            initialDate = TerminBerechnungUtils.getStartOfDay(System.currentTimeMillis()),
                            dismissOnDateSelected = false
                        )
                    }
                }
            }
        }
    }

    private fun confirmThenSaveAbholungMitLieferung(customerId: String, datum: Long) {
        val dateStr = DateFormatter.formatDate(datum)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.label_termine_anlegen))
            .setMessage(getString(R.string.dialog_ausnahme_bestaetigen, dateStr))
            .setPositiveButton(getString(android.R.string.ok)) { _, _ -> saveAbholungMitLieferung(customerId, datum) }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun confirmThenSaveAusnahmeAbholungMitLieferung(customerId: String, datum: Long) {
        val dateStr = DateFormatter.formatDate(datum)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.label_termine_anlegen))
            .setMessage(getString(R.string.dialog_ausnahme_bestaetigen, dateStr))
            .setPositiveButton(getString(android.R.string.ok)) { _, _ -> saveAusnahmeAbholungMitLieferung(customerId, datum) }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun confirmThenSaveAusnahmeNurA(customerId: String, datum: Long) {
        val dateStr = DateFormatter.formatDate(datum)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.label_termine_anlegen))
            .setMessage(getString(R.string.dialog_ausnahme_bestaetigen, dateStr))
            .setPositiveButton(getString(android.R.string.ok)) { _, _ -> saveAusnahme(customerId, datum, "A") }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun confirmThenSaveAusnahmeNurL(customerId: String, datum: Long) {
        val dateStr = DateFormatter.formatDate(datum)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.label_termine_anlegen))
            .setMessage(getString(R.string.dialog_ausnahme_bestaetigen, dateStr))
            .setPositiveButton(getString(android.R.string.ok)) { _, _ -> saveAusnahme(customerId, datum, "L") }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun saveAbholungMitLieferung(customerId: String, datum: Long) {
        lifecycleScope.launch {
            val customer = withContext(Dispatchers.IO) { repository.getCustomerById(customerId) }
            if (customer == null) {
                Toast.makeText(this@AusnahmeTerminActivity, getString(R.string.error_save_generic), Toast.LENGTH_SHORT).show()
                return@launch
            }
            val tageAzuL = customer.tageAzuLOrDefault(7)
            val ok = withContext(Dispatchers.IO) {
                repository.addKundenAbholungMitLieferung(customerId, datum, tageAzuL)
            }
            if (ok) {
                Toast.makeText(this@AusnahmeTerminActivity, getString(R.string.toast_abholungstermin_angelegt), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@AusnahmeTerminActivity, getString(R.string.error_save_generic), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAusnahmeAbholungMitLieferung(customerId: String, datum: Long) {
        lifecycleScope.launch {
            val customer = withContext(Dispatchers.IO) { repository.getCustomerById(customerId) }
            if (customer == null) {
                Toast.makeText(this@AusnahmeTerminActivity, getString(R.string.error_save_generic), Toast.LENGTH_SHORT).show()
                return@launch
            }
            val tageAzuL = customer.tageAzuLOrDefault(7)
            val ok = withContext(Dispatchers.IO) {
                repository.addAusnahmeAbholungMitLieferung(customerId, datum, tageAzuL)
            }
            if (ok) {
                Toast.makeText(this@AusnahmeTerminActivity, getString(R.string.toast_gespeichert), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@AusnahmeTerminActivity, getString(R.string.error_save_generic), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAusnahme(customerId: String, datum: Long, typ: String) {
        val dayStart = TerminBerechnungUtils.getStartOfDay(datum)
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                repository.addAusnahmeTermin(customerId, AusnahmeTermin(datum = dayStart, typ = typ))
            }
            if (ok) {
                Toast.makeText(this@AusnahmeTerminActivity, getString(R.string.toast_gespeichert), Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this@AusnahmeTerminActivity, getString(R.string.error_save_generic), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
