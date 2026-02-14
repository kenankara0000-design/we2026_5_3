package com.example.we2026_5.ui.detail

import com.example.we2026_5.AusnahmeTermin
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.KundenTermin
import com.example.we2026_5.ui.addcustomer.AddCustomerState
import com.example.we2026_5.ui.wasch.BelegMonat

/**
 * Phase C2: Gebündelter UI-State für [CustomerDetailScreen].
 * Reduziert die vielen Einzelparameter auf ein State-Objekt.
 */
data class CustomerDetailUiState(
    val isAdmin: Boolean,
    val customer: Customer?,
    val isInEditMode: Boolean,
    val editIntervalle: List<CustomerIntervall>,
    val editFormState: AddCustomerState?,
    val isLoading: Boolean,
    val isUploading: Boolean = false,
    val isOffline: Boolean = false,
    val showSaveAndNext: Boolean = false,
    val terminePairs365: List<Pair<Long, Long>> = emptyList(),
    val tourListenName: String? = null,
    val belegMonateForCustomer: List<BelegMonat> = emptyList(),
    val belegMonateErledigtForCustomer: List<BelegMonat> = emptyList(),
    val regelNameByRegelId: Map<String, String> = emptyMap()
)

/**
 * Phase C2: Gebündelte Callbacks für [CustomerDetailScreen].
 * Reduziert die vielen Lambda-Parameter auf ein Actions-Objekt.
 */
data class CustomerDetailActions(
    val onUpdateEditFormState: (AddCustomerState) -> Unit,
    val onBack: () -> Unit,
    val onEdit: () -> Unit,
    /** Phase C3: Speichern ausführen (Payload + save im ViewModel). [andNext] true = „Speichern & Nächster“. */
    val onPerformSave: (andNext: Boolean) -> Unit,
    val onDelete: () -> Unit,
    val onTerminAnlegen: () -> Unit,
    val onPauseCustomer: (pauseEndeWochen: Int?) -> Unit,
    val onResumeCustomer: () -> Unit,
    val onTakePhoto: () -> Unit,
    val onAdresseClick: () -> Unit,
    val onTelefonClick: () -> Unit,
    val onPhotoClick: (String) -> Unit,
    val onDeletePhoto: ((String) -> Unit)? = null,
    val onDatumSelected: (Int, Boolean) -> Unit,
    val onDeleteIntervall: ((Int) -> Unit)? = null,
    val onRemoveRegel: ((String) -> Unit)? = null,
    val onResetToAutomatic: () -> Unit = {},
    val onRegelClick: (String) -> Unit = {},
    val onUrlaubStartActivity: (String) -> Unit = {},
    val onAddMonthlyIntervall: ((CustomerIntervall) -> Unit)? = null,
    val onAddAbholungTermin: (Customer) -> Unit = {},
    val onAddAusnahmeTermin: (Customer) -> Unit = {},
    val onDeleteNextTermin: (Long) -> Unit = {},
    val onDeleteAusnahmeTermin: (AusnahmeTermin) -> Unit = {},
    val onDeleteKundenTermin: (List<KundenTermin>) -> Unit = {},
    val onNeueErfassungKameraFotoBelege: () -> Unit = {},
    val onNeueErfassungFormularBelege: () -> Unit = {},
    val onNeueErfassungManuellBelege: () -> Unit = {},
    val onBelegClick: (BelegMonat) -> Unit = {}
)
