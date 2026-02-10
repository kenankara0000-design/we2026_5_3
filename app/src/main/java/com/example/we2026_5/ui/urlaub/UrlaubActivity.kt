package com.example.we2026_5.ui.urlaub

import com.example.we2026_5.Customer
import com.example.we2026_5.UrlaubEintrag

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.we2026_5.R
import com.example.we2026_5.util.DialogBaseHelper
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class UrlaubActivity : AppCompatActivity() {

    private val viewModel: UrlaubViewModel by viewModel {
        parametersOf(intent.getStringExtra("CUSTOMER_ID") ?: "")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val customerId = intent.getStringExtra("CUSTOMER_ID")
        if (customerId == null) {
            Toast.makeText(this, getString(R.string.error_customer_id_missing), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContent {
            val customer by viewModel.customer.collectAsState(initial = null)
            val isSaving by viewModel.isSaving.collectAsState(initial = false)
            val errorMessage by viewModel.errorMessage.collectAsState(initial = null)
            val urlaubEintraege = viewModel.getEffectiveUrlaubEintraege(customer)

            LaunchedEffect(errorMessage) {
                errorMessage?.let { msg ->
                    Toast.makeText(this@UrlaubActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearErrorMessage()
                }
            }

            UrlaubScreen(
                customer = customer,
                urlaubEintraege = urlaubEintraege,
                isSaving = isSaving,
                onBack = { finish() },
                onNeuerUrlaub = { customer?.let { openUrlaubDatePicker(it, null) } },
                onUrlaubAendern = { index -> customer?.let { openUrlaubDatePicker(it, index) } },
                onUrlaubEintragLoeschen = { index -> confirmDeleteUrlaub(index) }
            )
        }
    }

    private fun openUrlaubDatePicker(customer: Customer, editIndex: Int?) {
        val eintrag = editIndex?.let { i ->
            viewModel.getEffectiveUrlaubEintraege(customer).getOrNull(i)
        }
        val initialVon = eintrag?.von?.takeIf { it > 0 } ?: System.currentTimeMillis()
        val initialBis = eintrag?.bis?.takeIf { it > 0 } ?: initialVon

        DialogBaseHelper.showDatePickerDialog(
            context = this,
            initialDate = initialVon,
            title = getString(R.string.dialog_urlaub_von),
            onDateSelected = { urlaubVon ->
                val vonStartOfDay = urlaubVon
                DialogBaseHelper.showDatePickerDialog(
                    context = this,
                    initialDate = if (initialBis >= vonStartOfDay) initialBis else vonStartOfDay,
                    title = getString(R.string.dialog_urlaub_bis),
                    onDateSelected = { urlaubBis ->
                        val bisCal = com.example.we2026_5.util.AppTimeZone.newCalendar().apply {
                            timeInMillis = urlaubBis
                            set(java.util.Calendar.HOUR_OF_DAY, 23)
                            set(java.util.Calendar.MINUTE, 59)
                            set(java.util.Calendar.SECOND, 59)
                        }
                        if (bisCal.timeInMillis >= vonStartOfDay) {
                            if (editIndex != null) {
                                viewModel.updateUrlaub(editIndex, vonStartOfDay, bisCal.timeInMillis) { success ->
                                    if (success) Toast.makeText(this, getString(R.string.toast_urlaub_eingetragen), Toast.LENGTH_SHORT).show()
                                    else Toast.makeText(this, getString(R.string.error_urlaub), Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                val vonStr = com.example.we2026_5.util.DateFormatter.formatDate(vonStartOfDay)
                                val bisStr = com.example.we2026_5.util.DateFormatter.formatDate(bisCal.timeInMillis)
                                AlertDialog.Builder(this@UrlaubActivity)
                                    .setTitle(getString(R.string.dialog_urlaub_von))
                                    .setMessage(getString(R.string.dialog_urlaub_bestaetigen, vonStr, bisStr))
                                    .setPositiveButton(getString(android.R.string.ok)) { _, _ ->
                                        viewModel.saveUrlaub(vonStartOfDay, bisCal.timeInMillis) { success ->
                                            if (success) Toast.makeText(this@UrlaubActivity, getString(R.string.toast_urlaub_eingetragen), Toast.LENGTH_SHORT).show()
                                            else Toast.makeText(this@UrlaubActivity, getString(R.string.error_urlaub), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .setNegativeButton(getString(R.string.btn_cancel), null)
                                    .show()
                            }
                        } else {
                            Toast.makeText(this, getString(R.string.toast_enddatum_nach_startdatum), Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCancel = { }
                )
            },
            onCancel = { }
        )
    }

    private fun confirmDeleteUrlaub(index: Int) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_urlaub_delete_entry))
            .setMessage(getString(R.string.dialog_urlaub_delete))
            .setPositiveButton(getString(R.string.dialog_loeschen)) { _, _ ->
                viewModel.deleteUrlaub(index) { success ->
                    if (success) Toast.makeText(this, getString(R.string.toast_urlaub_eingetragen), Toast.LENGTH_SHORT).show()
                    else Toast.makeText(this, getString(R.string.error_urlaub), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }
}
