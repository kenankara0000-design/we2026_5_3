package com.example.we2026_5.util

import com.example.we2026_5.data.repository.ErfassungRepository
import com.example.we2026_5.wasch.WaschErfassung
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Startet das Sammeln der Erfassungs-Flows (offen + erledigt) für einen Kunden.
 * Reduziert Duplikat-Code in WaschenErfassungViewModel und BelegeViewModel.
 *
 * @return Pair: erstes Job = offene Erfassungen, zweites = erledigte.
 */
object ErfassungFlowCollector {

    fun startCollecting(
        scope: CoroutineScope,
        customerId: String,
        repository: ErfassungRepository,
        onOffen: (List<WaschErfassung>) -> Unit,
        onErledigt: (List<WaschErfassung>) -> Unit
    ): Pair<Job, Job> {
        val jobOffen = scope.launch {
            repository.getErfassungenByCustomerFlow(customerId).collect { onOffen(it) }
        }
        val jobErledigt = scope.launch {
            repository.getErfassungenByCustomerFlowErledigt(customerId).collect { onErledigt(it) }
        }
        return jobOffen to jobErledigt
    }
}
