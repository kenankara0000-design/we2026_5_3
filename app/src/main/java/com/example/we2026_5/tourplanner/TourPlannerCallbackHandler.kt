package com.example.we2026_5.tourplanner

import android.content.Context
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.TerminTyp
import com.example.we2026_5.data.repository.CustomerRepository
import com.example.we2026_5.data.repository.KundenListeRepository

/**
 * Fassade für Erledigungs- und Verschieben/Urlaub-Callbacks im TourPlanner (Compose-UI).
 * Delegiert an TourPlannerErledigungHandler und TourPlannerVerschiebenUrlaubHandler.
 */
class TourPlannerCallbackHandler(
    private val context: Context,
    private val repository: CustomerRepository,
    private val listeRepository: KundenListeRepository,
    private val getListen: () -> List<KundenListe>,
    private val dateUtils: TourPlannerDateUtils,
    private val viewDate: java.util.Calendar,
    private val reloadCurrentView: () -> Unit,
    private val onError: ((String) -> Unit)? = null
) {

    private val erledigungHandler = TourPlannerErledigungHandler(
        context = context,
        repository = repository,
        listeRepository = listeRepository,
        dateUtils = dateUtils,
        viewDate = viewDate,
        reloadCurrentView = reloadCurrentView,
        onError = onError
    )

    private val verschiebenUrlaubHandler = TourPlannerVerschiebenUrlaubHandler(
        context = context,
        repository = repository,
        viewDate = viewDate,
        reloadCurrentView = reloadCurrentView,
        onError = onError
    )

    fun handleAbholungPublic(customer: Customer) = erledigungHandler.handleAbholung(customer)
    fun handleAuslieferungPublic(customer: Customer) = erledigungHandler.handleAuslieferung(customer)
    fun handleKwPublic(customer: Customer) = erledigungHandler.handleKw(customer)
    fun handleRueckgaengigPublic(customer: Customer) = erledigungHandler.handleRueckgaengig(customer)
    fun handleVerschiebenPublic(customer: Customer, newDate: Long, alleVerschieben: Boolean, typ: TerminTyp? = null) =
        verschiebenUrlaubHandler.handleVerschieben(customer, newDate, alleVerschieben, typ)
    fun handleUrlaubPublic(customer: Customer, von: Long, bis: Long) =
        verschiebenUrlaubHandler.handleUrlaub(customer, von, bis)

    fun hatSowohlAAlsAuchLAmViewTag(customer: Customer): Boolean =
        erledigungHandler.hatSowohlAAlsAuchLAmViewTag(customer)
}
