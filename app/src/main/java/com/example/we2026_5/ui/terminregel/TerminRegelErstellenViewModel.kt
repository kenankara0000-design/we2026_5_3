package com.example.we2026_5.ui.terminregel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.we2026_5.TerminRegel
import com.example.we2026_5.R
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.TerminRegelRepository
import com.example.we2026_5.util.TerminRegelManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI-State für Termin-Regel Erstellen/Bearbeiten. */
data class TerminRegelState(
    val currentRegelId: String? = null,
    val name: String = "",
    val beschreibung: String = "",
    val wiederholen: Boolean = false,
    val intervallTage: String = "7",
    val intervallAnzahl: String = "0",
    val wochentagBasiert: Boolean = false,
    val startDatum: Long = 0,
    val startDatumText: String = "",
    val abholungDatum: Long = 0,
    val abholungDatumText: String = "",
    val auslieferungDatum: Long = 0,
    val auslieferungDatumText: String = "",
    val abholungWochentag: Int = -1,
    val abholungWochentagText: String = "",
    val auslieferungWochentag: Int = -1,
    val auslieferungWochentagText: String = "",
    val isSaving: Boolean = false,
    val errorMessageResId: Int? = null,
    val success: Boolean = false
)

/**
 * ViewModel für Erstellen/Bearbeiten einer Termin-Regel.
 * Kapselt Laden, Speichern, Löschen und Prüfung der Regel.
 */
class TerminRegelErstellenViewModel(
    private val regelRepository: TerminRegelRepository,
    private val customerRepository: CustomerRepository
) : ViewModel() {

    private val _state = MutableLiveData(TerminRegelState())
    val state: LiveData<TerminRegelState> = _state

    fun setName(name: String) { updateState { copy(name = name, errorMessageResId = null) } }
    fun setBeschreibung(beschreibung: String) { updateState { copy(beschreibung = beschreibung) } }
    fun setWiederholen(wiederholen: Boolean) { updateState { copy(wiederholen = wiederholen) } }
    fun setIntervallTage(s: String) { updateState { copy(intervallTage = s) } }
    fun setIntervallAnzahl(s: String) { updateState { copy(intervallAnzahl = s) } }
    fun setWochentagBasiert(b: Boolean) { updateState { copy(wochentagBasiert = b) } }
    fun setStartDatum(timestamp: Long, text: String) { updateState { copy(startDatum = timestamp, startDatumText = text) } }
    fun setAbholungDatum(timestamp: Long, text: String) { updateState { copy(abholungDatum = timestamp, abholungDatumText = text) } }
    fun setAuslieferungDatum(timestamp: Long, text: String) { updateState { copy(auslieferungDatum = timestamp, auslieferungDatumText = text) } }
    fun setAbholungWochentag(weekday: Int, text: String) { updateState { copy(abholungWochentag = weekday, abholungWochentagText = text) } }
    fun setAuslieferungWochentag(weekday: Int, text: String) { updateState { copy(auslieferungWochentag = weekday, auslieferungWochentagText = text) } }

    private fun updateState(block: TerminRegelState.() -> TerminRegelState) {
        _state.value = (_state.value ?: TerminRegelState()).block()
    }

    fun loadRegel(regelId: String) {
        viewModelScope.launch {
            val regel = withContext(Dispatchers.IO) { regelRepository.getRegelById(regelId) }
            if (regel != null) {
                _state.value = (_state.value ?: TerminRegelState()).copy(
                    currentRegelId = regel.id,
                    name = regel.name,
                    beschreibung = regel.beschreibung,
                    wiederholen = regel.wiederholen,
                    intervallTage = regel.intervallTage.toString(),
                    intervallAnzahl = regel.intervallAnzahl.toString(),
                    wochentagBasiert = regel.wochentagBasiert,
                    startDatum = regel.startDatum,
                    startDatumText = if (regel.startDatum > 0) com.example.we2026_5.util.TerminRegelDatePickerHelper.formatDateFromMillis(regel.startDatum) else "",
                    abholungDatum = regel.abholungDatum,
                    abholungDatumText = if (regel.abholungDatum > 0) com.example.we2026_5.util.TerminRegelDatePickerHelper.formatDateFromMillis(regel.abholungDatum) else "",
                    auslieferungDatum = regel.auslieferungDatum,
                    auslieferungDatumText = if (regel.auslieferungDatum > 0) com.example.we2026_5.util.TerminRegelDatePickerHelper.formatDateFromMillis(regel.auslieferungDatum) else "",
                    abholungWochentag = regel.abholungWochentag,
                    abholungWochentagText = if (regel.abholungWochentag >= 0) WOCENTAGE[regel.abholungWochentag] else "",
                    auslieferungWochentag = regel.auslieferungWochentag,
                    auslieferungWochentagText = if (regel.auslieferungWochentag >= 0) WOCENTAGE[regel.auslieferungWochentag] else ""
                )
            }
        }
    }

    fun saveRegel() {
        val s = _state.value ?: return
        val name = s.name.trim()
        if (name.isEmpty()) {
            updateState { copy(errorMessageResId = R.string.error_regel_name_fehlt) }
            return
        }
        val intervallTage = s.intervallTage.toIntOrNull() ?: 7
        val intervallAnzahl = s.intervallAnzahl.toIntOrNull() ?: 0
        if (s.wiederholen && intervallTage < 1) {
            updateState { copy(errorMessageResId = R.string.validierung_intervall_min) }
            return
        }
        if (s.wochentagBasiert) {
            if (s.startDatum == 0L) { updateState { copy(errorMessageResId = R.string.error_startdatum_fehlt) }; return }
            if (s.abholungWochentag < 0) { updateState { copy(errorMessageResId = R.string.error_abholung_wochentag_fehlt) }; return }
            if (s.auslieferungWochentag < 0) { updateState { copy(errorMessageResId = R.string.error_auslieferung_wochentag_fehlt) }; return }
        }

        viewModelScope.launch {
            updateState { copy(isSaving = true, errorMessageResId = null) }
            val regel = TerminRegel(
                id = s.currentRegelId ?: java.util.UUID.randomUUID().toString(),
                name = name,
                beschreibung = s.beschreibung,
                abholungDatum = s.abholungDatum,
                auslieferungDatum = s.auslieferungDatum,
                wiederholen = s.wiederholen,
                intervallTage = intervallTage,
                intervallAnzahl = intervallAnzahl,
                wochentagBasiert = s.wochentagBasiert,
                startDatum = s.startDatum,
                abholungWochentag = s.abholungWochentag,
                auslieferungWochentag = s.auslieferungWochentag,
                geaendertAm = System.currentTimeMillis()
            )
            val success = withContext(Dispatchers.IO) { regelRepository.saveRegel(regel) }
            if (success) {
                if (s.currentRegelId != null) {
                    withContext(Dispatchers.IO) { aktualisiereBetroffeneKunden(regel) }
                }
                _state.value = (_state.value ?: s).copy(isSaving = false, success = true)
            } else {
                _state.value = (_state.value ?: s).copy(isSaving = false, errorMessageResId = R.string.error_save_failed)
            }
        }
    }

    fun deleteRegel(regelId: String, onSuccess: () -> Unit, onError: (Int) -> Unit) {
        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) { regelRepository.deleteRegel(regelId) }
            if (success) {
                _state.value = (_state.value ?: TerminRegelState()).copy(success = true)
                onSuccess()
            } else {
                onError(R.string.error_delete_failed)
            }
        }
    }

    companion object {
        val WOCENTAGE = arrayOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
    }

    suspend fun loadRegelById(regelId: String): TerminRegel? =
        regelRepository.getRegelById(regelId)

    /**
     * Prüft, ob eine Regel von mindestens einem Kunden verwendet wird.
     */
    suspend fun istRegelVerwendet(regelId: String): Boolean {
        return try {
            val allCustomers = customerRepository.getAllCustomers()
            allCustomers.any { customer ->
                customer.intervalle.any { intervall ->
                    intervall.terminRegelId == regelId
                }
            }
        } catch (e: Exception) {
            true // Bei Fehler sicher annehmen, dass Regel verwendet wird
        }
    }

    /**
     * Aktualisiert alle Kunden, die die bearbeitete Regel verwenden.
     */
    suspend fun aktualisiereBetroffeneKunden(regel: TerminRegel) {
        try {
            val allCustomers = customerRepository.getAllCustomers()
            allCustomers.forEach { customer ->
                val intervallIndex = customer.intervalle.indexOfFirst { it.terminRegelId == regel.id }
                if (intervallIndex != -1) {
                    val neuesIntervall = TerminRegelManager.wendeRegelAufKundeAn(regel, customer)
                    val aktualisierteIntervalle = customer.intervalle.toMutableList()
                    aktualisierteIntervalle[intervallIndex] = neuesIntervall.copy(id = customer.intervalle[intervallIndex].id)
                    val intervalleMap = aktualisierteIntervalle.mapIndexed { _, intervall ->
                        mapOf(
                            "id" to intervall.id,
                            "abholungDatum" to intervall.abholungDatum,
                            "auslieferungDatum" to intervall.auslieferungDatum,
                            "wiederholen" to intervall.wiederholen,
                            "intervallTage" to intervall.intervallTage,
                            "intervallAnzahl" to intervall.intervallAnzahl,
                            "erstelltAm" to intervall.erstelltAm,
                            "terminRegelId" to intervall.terminRegelId
                        )
                    }
                    customerRepository.updateCustomer(customer.id, mapOf("intervalle" to intervalleMap))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TerminRegelErstellenVM", "Fehler beim Aktualisieren der Kunden", e)
        }
    }
}
