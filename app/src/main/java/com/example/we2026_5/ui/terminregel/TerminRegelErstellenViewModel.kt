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
import com.example.we2026_5.TerminRegelTyp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** UI-State für Termin-Regel Erstellen/Bearbeiten. Mehrere Abhol- und Auslieferungswochentage; optional täglich. */
data class TerminRegelState(
    val currentRegelId: String? = null,
    val name: String = "",
    val beschreibung: String = "",
    val wiederholen: Boolean = false,
    val intervallTage: String = "7",
    val intervallAnzahl: String = "0",
    val regelTyp: TerminRegelTyp = TerminRegelTyp.WEEKLY,
    val zyklusTage: String = "7",
    val startDatum: Long = 0,
    val startDatumText: String = "",
    val abholungWochentage: List<Int> = emptyList(), // 0=Mo..6=So, mehrere erlaubt
    val auslieferungWochentage: List<Int> = emptyList(),
    val taeglich: Boolean = false, // Täglich: Termine jeden Tag ab Startdatum
    val aktiv: Boolean = true,
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
    fun setZyklusTage(s: String) { updateState { copy(zyklusTage = s) } }
    fun setStartDatum(timestamp: Long, text: String) { updateState { copy(startDatum = timestamp, startDatumText = text) } }
    fun setTaeglich(taeglich: Boolean) { updateState { copy(taeglich = taeglich) } }
    fun setRegelTyp(typ: TerminRegelTyp) { updateState { copy(regelTyp = typ) } }
    fun setAktiv(aktiv: Boolean) { updateState { copy(aktiv = aktiv) } }
    fun toggleAbholungWochentag(weekday: Int) {
        updateState {
            val list = if (weekday in abholungWochentage) abholungWochentage - weekday else abholungWochentage + weekday
            copy(abholungWochentage = list.sorted().distinct())
        }
    }
    fun toggleAuslieferungWochentag(weekday: Int) {
        updateState {
            val list = if (weekday in auslieferungWochentage) auslieferungWochentage - weekday else auslieferungWochentage + weekday
            copy(auslieferungWochentage = list.sorted().distinct())
        }
    }

    private fun updateState(block: TerminRegelState.() -> TerminRegelState) {
        _state.value = (_state.value ?: TerminRegelState()).block()
    }

    fun loadRegel(regelId: String) {
        viewModelScope.launch {
            val regel = withContext(Dispatchers.IO) { regelRepository.getRegelById(regelId) }
            if (regel != null) {
                val abholList = regel.abholungWochentage?.filter { it in 0..6 }?.distinct()?.sorted()
                    ?: (if (regel.abholungWochentag in 0..6) listOf(regel.abholungWochentag) else emptyList())
                val auslList = regel.auslieferungWochentage?.filter { it in 0..6 }?.distinct()?.sorted()
                    ?: (if (regel.auslieferungWochentag in 0..6) listOf(regel.auslieferungWochentag) else emptyList())
                _state.value = (_state.value ?: TerminRegelState()).copy(
                    currentRegelId = regel.id,
                    name = regel.name,
                    beschreibung = regel.beschreibung,
                    wiederholen = regel.wiederholen,
                    intervallTage = regel.intervallTage.toString(),
                    intervallAnzahl = regel.intervallAnzahl.toString(),
                    regelTyp = regel.regelTyp,
                    zyklusTage = regel.zyklusTage.toString(),
                    startDatum = regel.startDatum,
                    startDatumText = if (regel.startDatum > 0) com.example.we2026_5.util.TerminRegelDatePickerHelper.formatDateFromMillis(regel.startDatum) else "",
                    abholungWochentage = abholList,
                    auslieferungWochentage = auslList,
                    taeglich = regel.taeglich,
                    aktiv = regel.aktiv
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
        val zyklusTage = s.zyklusTage.toIntOrNull() ?: intervallTage
        if (s.wiederholen && intervallTage < 1) {
            updateState { copy(errorMessageResId = R.string.validierung_intervall_min) }
            return
        }
        if (s.taeglich) {
            if (s.startDatum <= 0) {
                updateState { copy(errorMessageResId = R.string.validierung_startdatum) }; return
            }
        } else {
            if (s.abholungWochentage.isEmpty()) {
                updateState { copy(errorMessageResId = R.string.error_abholung_wochentag_fehlt) }; return
            }
            if (s.auslieferungWochentage.isEmpty()) {
                updateState { copy(errorMessageResId = R.string.error_auslieferung_wochentag_fehlt) }; return
            }
        }

        viewModelScope.launch {
            updateState { copy(isSaving = true, errorMessageResId = null) }
            val regel = TerminRegel(
                id = s.currentRegelId ?: java.util.UUID.randomUUID().toString(),
                name = name,
                beschreibung = s.beschreibung,
                abholungDatum = 0,
                auslieferungDatum = 0,
                wiederholen = s.wiederholen,
                intervallTage = intervallTage,
                intervallAnzahl = intervallAnzahl,
                regelTyp = s.regelTyp,
                zyklusTage = zyklusTage,
                wochentagBasiert = !s.taeglich,
                startDatum = s.startDatum,
                abholungWochentag = s.abholungWochentage.firstOrNull() ?: -1,
                auslieferungWochentag = s.auslieferungWochentage.firstOrNull() ?: -1,
                abholungWochentage = s.abholungWochentage,
                auslieferungWochentage = s.auslieferungWochentage,
                taeglich = s.taeglich,
                geaendertAm = System.currentTimeMillis(),
                aktiv = s.aktiv
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
                val betroffeneIndices = customer.intervalle.mapIndexed { index, intervall -> if (intervall.terminRegelId == regel.id) index else -1 }.filter { it >= 0 }
                if (betroffeneIndices.isNotEmpty()) {
                    val neueIntervalle = TerminRegelManager.wendeRegelAufKundeAn(regel, customer)
                    val aktualisierteIntervalle = customer.intervalle.filterIndexed { index, _ -> index !in betroffeneIndices.toSet() }.toMutableList()
                    aktualisierteIntervalle.addAll(neueIntervalle)
                    val intervalleMap = aktualisierteIntervalle.map { intervall ->
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
