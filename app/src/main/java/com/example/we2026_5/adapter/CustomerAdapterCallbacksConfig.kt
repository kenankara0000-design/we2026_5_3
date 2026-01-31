package com.example.we2026_5.adapter

import com.example.we2026_5.Customer
import com.example.we2026_5.SectionType
import com.example.we2026_5.tourplanner.ErledigungSheetState
import com.example.we2026_5.util.TerminInfo

/**
 * Gebündelte Callbacks für den CustomerAdapter.
 * Statt vieler einzelner var-Callbacks wird eine einzige Konfiguration verwendet.
 * Erhöht Lesbarkeit und Testbarkeit (z. B. ein Mock-Objekt für Tests).
 */
data class CustomerAdapterCallbacksConfig(
    val onAbholung: ((Customer) -> Unit)? = null,
    val onAuslieferung: ((Customer) -> Unit)? = null,
    val onKw: ((Customer) -> Unit)? = null,
    val onResetTourCycle: ((String) -> Unit)? = null,
    val onVerschieben: ((Customer, Long, Boolean) -> Unit)? = null,
    val onUrlaub: ((Customer, Long, Long) -> Unit)? = null,
    val onRueckgaengig: ((Customer) -> Unit)? = null,
    val onBulkMarkDone: ((List<Customer>) -> Unit)? = null,
    val onTerminClick: ((Customer, Long) -> Unit)? = null,
    val onAktionenClick: ((Customer, ErledigungSheetState) -> Unit)? = null,
    val onSectionToggle: ((SectionType) -> Unit)? = null,
    val getAbholungDatum: ((Customer) -> Long)? = null,
    val getAuslieferungDatum: ((Customer) -> Long)? = null,
    val getNaechstesTourDatum: ((Customer) -> Long)? = null,
    val getTermineFuerKunde: ((Customer, Long, Int) -> List<TerminInfo>)? = null
)
