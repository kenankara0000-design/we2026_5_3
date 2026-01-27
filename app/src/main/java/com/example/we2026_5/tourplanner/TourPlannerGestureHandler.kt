package com.example.we2026_5.tourplanner

import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.GestureDetectorCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.we2026_5.databinding.ActivityTourPlannerBinding
import java.util.Calendar

/**
 * Helper-Klasse für Gesture-Handling in TourPlannerActivity
 */
class TourPlannerGestureHandler(
    private val binding: ActivityTourPlannerBinding,
    private val viewDate: Calendar,
    private val onDateChanged: () -> Unit
) {
    
    private lateinit var gestureDetector: GestureDetectorCompat
    
    /**
     * Initialisiert die Swipe-Gesten
     */
    fun setupSwipeGestures() {
        gestureDetector = GestureDetectorCompat(binding.root.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val deltaX = e2.x - e1.x
                val deltaY = e2.y - e1.y
                
                // Nur horizontale Swipes erkennen (nicht vertikale)
                if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > 100) {
                    if (deltaX > 0) {
                        // Swipe nach rechts = vorheriger Tag
                        viewDate.add(Calendar.DAY_OF_YEAR, -1)
                        onDateChanged()
                        return true
                    } else {
                        // Swipe nach links = nächster Tag
                        viewDate.add(Calendar.DAY_OF_YEAR, 1)
                        onDateChanged()
                        return true
                    }
                }
                return false
            }
        })
        
        // Swipe-Gesten auf dem RecyclerView aktivieren
        binding.rvTourList.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event) || false
        }
        
        // Auch auf dem gesamten Layout aktivieren (falls RecyclerView leer ist)
        binding.root.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event) || false
        }
    }
}
