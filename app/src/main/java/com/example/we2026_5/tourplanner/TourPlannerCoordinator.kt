package com.example.we2026_5.tourplanner

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerDetailActivity
import com.example.we2026_5.MapViewActivity
import com.example.we2026_5.R
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.adapter.CustomerDialogHelper
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.ui.tourplanner.TourPlannerViewModel
import com.example.we2026_5.util.Result
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Kapselt alle Helper der TourPlannerActivity (DateUtils, DialogHelper, CallbackHandler, SheetHelper).
 * Activity hält nur Compose-State und ruft Coordinator-Methoden auf.
 */
class TourPlannerCoordinator(
    private val activity: AppCompatActivity,
    private val viewModel: TourPlannerViewModel,
    private val repository: CustomerRepository,
    private val listeRepository: KundenListeRepository,
    private val regelRepository: TerminRegelRepository
) {
    private val viewDate = Calendar.getInstance()

    val dateUtils: TourPlannerDateUtils = TourPlannerDateUtils { viewModel.getListen() }
    val dialogHelper: TourPlannerDialogHelper = TourPlannerDialogHelper(
        activity = activity,
        onKundeAnzeigen = { customer ->
            activity.startActivity(Intent(activity, CustomerDetailActivity::class.java).apply {
                putExtra("CUSTOMER_ID", customer.id)
            })
        },
        onTerminLoeschen = { customer, terminDatum -> deleteTermin(customer, terminDatum) }
    )
    val callbackHandler: TourPlannerCallbackHandler = TourPlannerCallbackHandler(
        context = activity,
        repository = repository,
        listeRepository = listeRepository,
        getListen = { viewModel.getListen() },
        dateUtils = dateUtils,
        viewDate = viewDate,
        reloadCurrentView = { reloadCurrentView() },
        resetTourCycle = { customerId -> resetTourCycle(customerId) },
        onError = { msg -> viewModel.setError(msg) }
    )
    val sheetDialogHelper: CustomerDialogHelper = CustomerDialogHelper(
        context = activity,
        onVerschieben = { c, newDate, alle, typ -> callbackHandler.handleVerschiebenPublic(c, newDate, alle, typ) },
        onUrlaub = { c, von, bis -> callbackHandler.handleUrlaubPublic(c, von, bis) },
        onRueckgaengig = { c -> callbackHandler.handleRueckgaengigPublic(c) },
        onButtonStateReset = { reloadCurrentView() }
    )

    /** Zeigt Verschieben-Dialog. Wenn A und L am gleichen Tag: zuerst „A oder L verschieben?“ abfragen. */
    fun showVerschiebenDialog(customer: Customer) {
        if (callbackHandler.hatSowohlAAlsAuchLAmViewTag(customer)) {
            androidx.appcompat.app.AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.dialog_verschieben_choose_a_or_l))
                .setPositiveButton(activity.getString(R.string.dialog_verschieben_abholung)) { _, _ ->
                    sheetDialogHelper.showVerschiebenDialog(customer, TerminTyp.ABHOLUNG)
                }
                .setNegativeButton(activity.getString(R.string.dialog_verschieben_auslieferung)) { _, _ ->
                    sheetDialogHelper.showVerschiebenDialog(customer, TerminTyp.AUSLIEFERUNG)
                }
                .setNeutralButton(activity.getString(R.string.btn_cancel)) { _, _ -> }
                .show()
        } else {
            sheetDialogHelper.showVerschiebenDialog(customer, null)
        }
    }

    init {
        viewModel.selectedTimestamp.observe(activity) { ts ->
            ts?.let { viewDate.timeInMillis = it }
        }
    }

    fun getViewDateMillis(): Long = viewDate.timeInMillis

    fun reloadCurrentView() {
        viewModel.getSelectedTimestamp()?.let { ts ->
            viewModel.loadTourData(ts) { viewModel.isSectionExpanded(it) }
        }
    }

    fun deleteTermin(customer: Customer, terminDatum: Long) {
        activity.lifecycleScope.launch {
            when (val result = viewModel.deleteTerminFromCustomer(customer, terminDatum)) {
                is Result.Success -> {
                    viewModel.clearError()
                    android.widget.Toast.makeText(activity, activity.getString(com.example.we2026_5.R.string.toast_termin_deleted), android.widget.Toast.LENGTH_SHORT).show()
                    com.example.we2026_5.util.DialogBaseHelper.showConfirmationDialog(
                        context = activity,
                        title = activity.getString(com.example.we2026_5.R.string.dialog_undo_termin_title),
                        message = activity.getString(com.example.we2026_5.R.string.dialog_undo_termin_message),
                        positiveButtonText = activity.getString(com.example.we2026_5.R.string.btn_undo),
                        negativeButtonText = activity.getString(com.example.we2026_5.R.string.dialog_close),
                        onPositive = {
                            activity.lifecycleScope.launch {
                                when (val undo = viewModel.restoreTerminForCustomer(customer, terminDatum)) {
                                    is Result.Success -> reloadCurrentView()
                                    is Result.Error -> viewModel.setError(undo.message)
                                }
                            }
                        }
                    )
                    reloadCurrentView()
                }
                is Result.Error -> {
                    viewModel.setError(result.message)
                    android.widget.Toast.makeText(activity, result.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun resetTourCycle(customerId: String) {
        activity.lifecycleScope.launch {
            when (val result = viewModel.resetTourCycle(customerId)) {
                is Result.Success -> {
                    viewModel.clearError()
                    android.widget.Toast.makeText(activity, activity.getString(com.example.we2026_5.R.string.toast_tour_completed), android.widget.Toast.LENGTH_SHORT).show()
                    reloadCurrentView()
                }
                is Result.Error -> {
                    viewModel.setError(result.message)
                    android.widget.Toast.makeText(activity, result.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
