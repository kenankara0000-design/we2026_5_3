package com.example.we2026_5.tourplanner

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.CustomerAdapter
import com.example.we2026_5.CustomerDetailActivity
import com.example.we2026_5.databinding.ActivityTourPlannerBinding

/**
 * Helper-Klasse für UI-Setup in TourPlannerActivity
 */
class TourPlannerUISetup(
    private val activity: AppCompatActivity,
    private val binding: ActivityTourPlannerBinding,
    private val setupAdapterCallbacks: (CustomerAdapter) -> Unit,
    private val setupAdapterCallbacksForAdapter: (CustomerAdapter) -> Unit
) {
    
    /**
     * Initialisiert den Adapter für die Tagesansicht
     */
    fun setupAdapters(
        adapter: CustomerAdapter
    ) {
        binding.rvTourList.layoutManager = LinearLayoutManager(activity)
        binding.rvTourList.adapter = adapter
        
        // Drag & Drop für Kunden
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                return adapter.onItemMove(fromPosition, toPosition)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // Nicht verwendet
            }
            
            override fun isLongPressDragEnabled(): Boolean {
                return true // Lang drücken zum Verschieben
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.rvTourList)
    }
    
    /**
     * Stellt sicher, dass die Tagesansicht aktiv ist.
     */
    fun updateViewMode() {
        binding.rvTourList.visibility = View.VISIBLE
        binding.btnPrevDay.contentDescription = "Vorheriger Tag"
        binding.btnNextDay.contentDescription = "Nächster Tag"
    }
    
    /**
     * Aktualisiert die Header-Button-Zustände
     */
    fun updateHeaderButtonStates(pressedHeaderButton: String?) {
        // Farben definieren
        val activeBackgroundColor = ContextCompat.getColor(activity, com.example.we2026_5.R.color.status_warning) // Orange
        val inactiveBackgroundColor = ContextCompat.getColor(activity, com.example.we2026_5.R.color.button_blue) // Blau
        val textColor = ContextCompat.getColor(activity, com.example.we2026_5.R.color.white)
        
        // Button-Zeile bleibt immer blau
        val barBackgroundColor = ContextCompat.getColor(activity, com.example.we2026_5.R.color.primary_blue)
        binding.buttonBar.setBackgroundColor(barBackgroundColor)
        
        when (pressedHeaderButton) {
            "Karte" -> {
                // Aktiver Button: Orange Hintergrund
                binding.btnMapView.setBackgroundColor(activeBackgroundColor)
                binding.btnMapView.setTextColor(textColor)
                binding.btnMapView.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                // Inaktiver Button: Blau Hintergrund
                binding.btnToday.setBackgroundColor(inactiveBackgroundColor)
                binding.btnToday.setTextColor(textColor)
                binding.btnToday.iconTint = android.content.res.ColorStateList.valueOf(textColor)
            }
            "Heute" -> {
                // Aktiver Button: Orange Hintergrund
                binding.btnToday.setBackgroundColor(activeBackgroundColor)
                binding.btnToday.setTextColor(textColor)
                binding.btnToday.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                // Inaktiver Button: Blau Hintergrund
                binding.btnMapView.setBackgroundColor(inactiveBackgroundColor)
                binding.btnMapView.setTextColor(textColor)
                binding.btnMapView.iconTint = android.content.res.ColorStateList.valueOf(textColor)
            }
            else -> {
                // Kein Button gedrückt: Alle blau
                binding.btnMapView.setBackgroundColor(inactiveBackgroundColor)
                binding.btnMapView.setTextColor(textColor)
                binding.btnMapView.iconTint = android.content.res.ColorStateList.valueOf(textColor)
                
                binding.btnToday.setBackgroundColor(inactiveBackgroundColor)
                binding.btnToday.setTextColor(textColor)
                binding.btnToday.iconTint = android.content.res.ColorStateList.valueOf(textColor)
            }
        }
    }
    
    /**
     * Zeigt den Error-State an
     */
    fun showErrorState(message: String, onRetry: () -> Unit) {
        binding.errorStateLayout.visibility = View.VISIBLE
        binding.rvTourList.visibility = View.GONE
        binding.emptyStateLayout.visibility = View.GONE
        binding.tvErrorMessage.text = message
        
        binding.btnRetry.setOnClickListener {
            onRetry()
        }
    }
    
    /**
     * Aktualisiert die Empty-State-Sichtbarkeit
     */
    fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.rvTourList.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.rvTourList.visibility = View.VISIBLE
        }
    }
}
