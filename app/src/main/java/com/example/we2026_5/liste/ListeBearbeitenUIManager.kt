package com.example.we2026_5.liste

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.Customer
import com.example.we2026_5.KundenListe
import com.example.we2026_5.ListeIntervall
import com.example.we2026_5.ListeIntervallAdapter
import com.example.we2026_5.ListeIntervallViewAdapter
import com.example.we2026_5.databinding.ActivityListeBearbeitenBinding

/**
 * Helper-Klasse für UI-Management in ListeBearbeitenActivity
 */
class ListeBearbeitenUIManager(
    private val activity: AppCompatActivity,
    private val binding: ActivityListeBearbeitenBinding,
    private val intervallAdapter: ListeIntervallAdapter,
    private val intervallViewAdapter: ListeIntervallViewAdapter
) {
    
    /**
     * Initialisiert die RecyclerViews
     */
    fun setupRecyclerViews(
        kundenInListeAdapter: RecyclerView.Adapter<*>,
        verfuegbareKundenAdapter: RecyclerView.Adapter<*>
    ) {
        binding.rvKundenInListe.layoutManager = LinearLayoutManager(activity)
        binding.rvKundenInListe.adapter = kundenInListeAdapter

        binding.rvVerfuegbareKunden.layoutManager = LinearLayoutManager(activity)
        binding.rvVerfuegbareKunden.adapter = verfuegbareKundenAdapter
        
        binding.rvListeIntervalle.layoutManager = LinearLayoutManager(activity)
        binding.rvListeIntervalle.adapter = intervallAdapter
        
        binding.rvListeIntervalleView.layoutManager = LinearLayoutManager(activity)
        binding.rvListeIntervalleView.adapter = intervallViewAdapter
    }
    
    /**
     * Wechselt zwischen Edit- und View-Mode
     */
    fun toggleEditMode(
        isEditing: Boolean,
        liste: KundenListe,
        intervalle: MutableList<ListeIntervall>
    ) {
        val viewModeVisibility = if (isEditing) View.GONE else View.VISIBLE
        val editModeVisibility = if (isEditing) View.VISIBLE else View.GONE
        
        binding.groupListView.visibility = viewModeVisibility
        binding.groupListEdit.visibility = editModeVisibility
        
        binding.btnEditListe.visibility = viewModeVisibility
        binding.btnSaveListe.visibility = editModeVisibility
        binding.btnDeleteListe.visibility = editModeVisibility
        
        if (isEditing) {
            // Edit-Felder mit aktuellen Werten füllen
            binding.etListeNameEdit.setText(liste.name)
            
            // Liste-Art RadioButton setzen
            when (liste.listeArt) {
                "Gewerbe" -> binding.rgListeArtEdit.check(binding.rbGewerbeEdit.id)
                "Privat" -> binding.rgListeArtEdit.check(binding.rbPrivatEdit.id)
                "Liste" -> binding.rgListeArtEdit.check(binding.rbListeEdit.id)
                else -> binding.rgListeArtEdit.check(binding.rbGewerbeEdit.id)
            }
            
            // Intervalle laden und anzeigen
            intervalle.clear()
            intervalle.addAll(liste.intervalle)
            intervallAdapter.updateIntervalle(intervalle.toList())
        }
    }
    
    /**
     * Aktualisiert die UI im View-Mode
     */
    fun updateUi(liste: KundenListe) {
        binding.tvListeNameView.text = liste.name
        binding.tvListeArtView.text = liste.listeArt
        
        // Intervalle im View-Mode anzeigen
        if (liste.intervalle.isNotEmpty()) {
            binding.cardListeIntervallView.visibility = View.VISIBLE
            intervallViewAdapter.updateIntervalle(liste.intervalle)
        } else {
            binding.cardListeIntervallView.visibility = View.GONE
        }
    }
    
    /**
     * Aktualisiert die Empty-State-Sichtbarkeit
     */
    fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }
}
