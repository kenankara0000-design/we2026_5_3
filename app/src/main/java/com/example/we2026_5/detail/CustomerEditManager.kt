package com.example.we2026_5.detail

import android.view.View
import android.widget.Toast
import com.example.we2026_5.Customer
import com.example.we2026_5.CustomerIntervall
import com.example.we2026_5.ValidationHelper
import com.example.we2026_5.databinding.ActivityCustomerDetailBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manager für Edit-Mode-Logik in CustomerDetailActivity.
 * Speichern erfolgt über Callback (z. B. ViewModel.saveCustomer), nicht direkt über Repository.
 */
class CustomerEditManager(
    private val activity: android.app.Activity,
    private val binding: ActivityCustomerDetailBinding,
    private val customerId: String,
    private val intervalle: MutableList<CustomerIntervall>,
    private val intervallAdapter: com.example.we2026_5.IntervallAdapter,
    private val onSaveUpdates: (Map<String, Any>, (Boolean) -> Unit) -> Unit,
    private val onEditModeChanged: (Boolean) -> Unit,
    private val onCustomerUpdated: (Customer) -> Unit,
    private val onMapsLocationRequested: () -> Unit
) {
    
    private var isInEditMode = false
    
    fun toggleEditMode(isEditing: Boolean, currentCustomer: Customer?) {
        isInEditMode = isEditing
        val viewModeVisibility = if (isEditing) View.GONE else View.VISIBLE
        val editModeVisibility = if (isEditing) View.VISIBLE else View.GONE

        binding.groupTextViews.visibility = viewModeVisibility
        binding.groupEditTexts.visibility = editModeVisibility

        binding.btnEditCustomer.visibility = viewModeVisibility
        binding.btnSaveCustomer.visibility = editModeVisibility
        binding.btnDeleteCustomer.visibility = editModeVisibility

        if (isEditing) {
            populateEditFields(currentCustomer)
        } else {
            binding.tvDetailName.text = currentCustomer?.name
        }
        
        onEditModeChanged(isEditing)
    }
    
    private fun populateEditFields(customer: Customer?) {
        customer?.let {
            binding.etDetailName.setText(it.name)
            binding.etDetailAdresse.setText(it.adresse)
            binding.etDetailTelefon.setText(it.telefon)
            binding.etDetailNotizen.setText(it.notizen)
            
            // Kunden-Art RadioButton setzen
            when (it.kundenArt) {
                "Gewerblich" -> binding.rgDetailKundenArt.check(binding.rbDetailGewerblich.id)
                "Privat" -> binding.rgDetailKundenArt.check(binding.rbDetailPrivat.id)
                "Liste" -> binding.rgDetailKundenArt.check(binding.rbDetailListe.id)
                else -> binding.rgDetailKundenArt.check(binding.rbDetailGewerblich.id)
            }
            
            // Intervall-Card Sichtbarkeit basierend auf Kunden-Art
            val sollIntervallAnzeigen = it.kundenArt == "Gewerblich" || it.kundenArt == "Liste"
            binding.cardDetailIntervall.visibility = if (sollIntervallAnzeigen) View.VISIBLE else View.GONE
            
            // Listener für Kunden-Art-Änderung
            binding.rgDetailKundenArt.setOnCheckedChangeListener { _, checkedId ->
                val neueKundenArt = when (checkedId) {
                    binding.rbDetailGewerblich.id -> "Gewerblich"
                    binding.rbDetailPrivat.id -> "Privat"
                    binding.rbDetailListe.id -> "Liste"
                    else -> "Gewerblich"
                }
                // Intervall-Card anzeigen/ausblenden basierend auf neuer Kunden-Art
                binding.cardDetailIntervall.visibility = if (neueKundenArt == "Gewerblich" || neueKundenArt == "Liste") {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
            
            // Intervalle laden und anzeigen
            intervalle.clear()
            intervalle.addAll(it.intervalle)
            intervallAdapter.updateIntervalle(intervalle.toList())
        }
        
        // Google Maps Button für Adress-Auswahl
        binding.btnSelectLocation.setOnClickListener {
            onMapsLocationRequested()
        }
    }
    
    fun handleSave(currentCustomer: Customer?, onSaveComplete: () -> Unit) {
        val name = binding.etDetailName.text.toString().trim()
        if (name.isEmpty()) {
            binding.etDetailName.error = activity.getString(com.example.we2026_5.R.string.validation_name_missing)
            return
        }

        // Adresse-Validierung
        val adresse = binding.etDetailAdresse.text.toString().trim()
        if (adresse.isNotEmpty() && !ValidationHelper.isValidAddress(adresse)) {
            binding.etDetailAdresse.error = activity.getString(com.example.we2026_5.R.string.validation_adresse)
            return
        }

        // Telefon-Validierung
        val telefon = binding.etDetailTelefon.text.toString().trim()
        if (telefon.isNotEmpty() && !ValidationHelper.isValidPhoneNumber(telefon)) {
            binding.etDetailTelefon.error = activity.getString(com.example.we2026_5.R.string.validation_telefon)
            return
        }

        // Button sofort deaktivieren und visuelles Feedback geben
        CoroutineScope(Dispatchers.Main).launch {
            activity.runOnUiThread {
                binding.btnSaveCustomer.visibility = View.VISIBLE
                binding.btnSaveCustomer.isEnabled = false
                binding.btnSaveCustomer.text = activity.getString(com.example.we2026_5.R.string.save_in_progress)
                binding.btnSaveCustomer.alpha = 0.6f
            }
            
            // Kunden-Art bestimmen
            val kundenArt = when (binding.rgDetailKundenArt.checkedRadioButtonId) {
                binding.rbDetailGewerblich.id -> "Gewerblich"
                binding.rbDetailPrivat.id -> "Privat"
                binding.rbDetailListe.id -> "Liste"
                else -> "Gewerblich"
            }
            
            // Intervalle aktualisieren (nur für Gewerblich und Liste)
            val customerIntervalle = if ((kundenArt == "Gewerblich" || kundenArt == "Liste") && intervalle.isNotEmpty()) {
                intervalle.toList()
            } else {
                currentCustomer?.intervalle ?: emptyList()
            }
            
            val updatedData = mapOf(
                "name" to name,
                "adresse" to adresse,
                "telefon" to telefon,
                "notizen" to binding.etDetailNotizen.text.toString().trim(),
                "kundenArt" to kundenArt,
                "wochentag" to 0,
                "intervalle" to customerIntervalle.map { 
                    mapOf(
                        "id" to it.id,
                        "abholungDatum" to it.abholungDatum,
                        "auslieferungDatum" to it.auslieferungDatum,
                        "wiederholen" to it.wiederholen,
                        "intervallTage" to it.intervallTage,
                        "intervallAnzahl" to it.intervallAnzahl,
                        "erstelltAm" to it.erstelltAm
                    )
                }
            )
            
            val updatedCustomerOptimistic = currentCustomer?.copy(
                name = name,
                adresse = adresse,
                telefon = telefon,
                notizen = binding.etDetailNotizen.text.toString().trim(),
                kundenArt = kundenArt,
                wochentag = 0,
                intervalle = customerIntervalle
            )
            updatedCustomerOptimistic?.let { onCustomerUpdated(it) }

            onSaveUpdates(updatedData) { success ->
                activity.runOnUiThread {
                    if (success) {
                        Toast.makeText(activity, activity.getString(com.example.we2026_5.R.string.toast_gespeichert), Toast.LENGTH_SHORT).show()
                        binding.btnSaveCustomer.visibility = View.VISIBLE
                        binding.btnSaveCustomer.text = activity.getString(com.example.we2026_5.R.string.toast_gespeichert)
                        binding.btnSaveCustomer.backgroundTintList = android.content.res.ColorStateList.valueOf(
                            activity.resources.getColor(com.example.we2026_5.R.color.status_done, activity.theme)
                        )
                        binding.btnSaveCustomer.alpha = 1.0f
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            toggleEditMode(false, updatedCustomerOptimistic)
                            binding.btnSaveCustomer.isEnabled = true
                            binding.btnSaveCustomer.text = activity.getString(com.example.we2026_5.R.string.btn_save)
                            binding.btnSaveCustomer.alpha = 1.0f
                            onSaveComplete()
                        }, 1500)
                    } else {
                        binding.btnSaveCustomer.isEnabled = true
                        binding.btnSaveCustomer.text = activity.getString(com.example.we2026_5.R.string.btn_save)
                        binding.btnSaveCustomer.alpha = 1.0f
                    }
                }
            }
        }
    }
    
    fun isInEditMode(): Boolean = isInEditMode
}
