package com.example.we2026_5

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.example.we2026_5.ui.terminregel.TerminRegelErstellenScreen
import com.example.we2026_5.ui.terminregel.TerminRegelErstellenViewModel
import com.example.we2026_5.ui.terminregel.TerminRegelState
import com.example.we2026_5.util.TerminRegelDatePickerHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar

class TerminRegelErstellenActivity : AppCompatActivity() {

    private val viewModel: TerminRegelErstellenViewModel by viewModel()
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("UNUSED_EXPRESSION")
        viewModel
        val regelId = intent.getStringExtra("REGEL_ID")
        if (regelId != null) viewModel.loadRegel(regelId)

        setContent {
            MaterialTheme {
                val state by viewModel.state.observeAsState(initial = TerminRegelState())
                TerminRegelErstellenScreen(
                    state = state,
                    onNameChange = { viewModel.setName(it) },
                    onBeschreibungChange = { viewModel.setBeschreibung(it) },
                    onWiederholenChange = { viewModel.setWiederholen(it) },
                    onIntervallTageChange = { viewModel.setIntervallTage(it) },
                    onIntervallAnzahlChange = { viewModel.setIntervallAnzahl(it) },
                    onWochentagBasiertChange = { viewModel.setWochentagBasiert(it) },
                    onStartDateClick = { showStartDatePicker(viewModel) },
                    onAbholungDateClick = { showDatePicker(true, viewModel) },
                    onAuslieferungDateClick = { showDatePicker(false, viewModel) },
                    onAbholungWochentagClick = { showWochentagPicker(viewModel, true) },
                    onAuslieferungWochentagClick = { showWochentagPicker(viewModel, false) },
                    onSave = { viewModel.saveRegel() },
                    onDelete = { state.currentRegelId?.let { showDeleteConfirmation(it, viewModel) } },
                    onBack = { finish() },
                    onFinish = { finish() }
                )
            }
        }
    }

    private fun showStartDatePicker(viewModel: TerminRegelErstellenViewModel) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        TerminRegelDatePickerHelper.showDatePicker(this, year, month, day) { timestamp ->
            viewModel.setStartDatum(timestamp, TerminRegelDatePickerHelper.formatDateFromMillis(timestamp))
        }
    }

    private fun showDatePicker(isAbholung: Boolean, viewModel: TerminRegelErstellenViewModel) {
        AlertDialog.Builder(this)
            .setTitle(if (isAbholung) R.string.termin_regel_abholung else R.string.termin_regel_auslieferung)
            .setItems(arrayOf(getString(R.string.termin_regel_heute_verwenden), getString(R.string.termin_regel_datum_waehlen))) { _, which ->
                when (which) {
                    0 -> {
                        val text = if (isAbholung) getString(R.string.termin_regel_abholung_heute) else getString(R.string.termin_regel_auslieferung_heute)
                        if (isAbholung) viewModel.setAbholungDatum(0, text) else viewModel.setAuslieferungDatum(0, text)
                    }
                    1 -> {
                        val year = calendar.get(Calendar.YEAR)
                        val month = calendar.get(Calendar.MONTH)
                        val day = calendar.get(Calendar.DAY_OF_MONTH)
                        TerminRegelDatePickerHelper.showDatePicker(this, year, month, day) { timestamp ->
                            val dateText = TerminRegelDatePickerHelper.formatDateFromMillis(timestamp)
                            if (isAbholung) viewModel.setAbholungDatum(timestamp, dateText) else viewModel.setAuslieferungDatum(timestamp, dateText)
                        }
                    }
                }
            }
            .setNegativeButton(R.string.termin_regel_abbrechen, null)
            .show()
    }

    private fun showWochentagPicker(viewModel: TerminRegelErstellenViewModel, isAbholung: Boolean) {
        val wochentage = TerminRegelErstellenViewModel.WOCENTAGE
        AlertDialog.Builder(this)
            .setTitle(R.string.termin_regel_wochentag_waehlen)
            .setItems(wochentage) { _, which ->
                val text = wochentage[which]
                if (isAbholung) viewModel.setAbholungWochentag(which, text) else viewModel.setAuslieferungWochentag(which, text)
            }
            .setNegativeButton(R.string.termin_regel_abbrechen, null)
            .show()
    }

    private fun showDeleteConfirmation(regelId: String, viewModel: TerminRegelErstellenViewModel) {
        CoroutineScope(Dispatchers.Main).launch {
            val wirdVerwendet = viewModel.istRegelVerwendet(regelId)
            if (wirdVerwendet) {
                AlertDialog.Builder(this@TerminRegelErstellenActivity)
                    .setTitle(R.string.dialog_regel_loeschen_titel)
                    .setMessage(R.string.dialog_regel_loeschen_message)
                    .setPositiveButton(R.string.dialog_ok, null)
                    .show()
            } else {
                AlertDialog.Builder(this@TerminRegelErstellenActivity)
                    .setTitle(R.string.dialog_regel_loeschen_confirm_titel)
                    .setMessage(R.string.dialog_regel_loeschen_confirm_message)
                    .setPositiveButton(R.string.dialog_loeschen) { _, _ ->
                        viewModel.deleteRegel(regelId,
                            onSuccess = {
                                Toast.makeText(this@TerminRegelErstellenActivity, getString(R.string.toast_regel_geloescht), Toast.LENGTH_SHORT).show()
                                finish()
                            },
                            onError = { msg -> Toast.makeText(this@TerminRegelErstellenActivity, msg, Toast.LENGTH_SHORT).show() }
                        )
                    }
                    .setNegativeButton(R.string.termin_regel_abbrechen, null)
                    .show()
            }
        }
    }
}
