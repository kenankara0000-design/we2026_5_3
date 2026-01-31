package com.example.we2026_5.ui.customermanager

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.core.view.GestureDetectorCompat

/**
 * FrameLayout, das horizontale Swipe-Gesten erkennt und den Tab-Wechsel auslöst.
 * Vertikales Scrollen (z. B. RecyclerView) wird nicht blockiert.
 */
class TabSwipeFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val tabCount = 3

    private var currentTabIndex: Int = 0
    private var onTabChange: ((Int) -> Unit)? = null

    private val gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            val deltaX = e2.x - e1.x
            val minVelocity = 100
            val minDistance = 80
            if (kotlin.math.abs(deltaX) < minDistance) return false
            if (kotlin.math.abs(velocityX) < minVelocity) return false
            val newIndex = if (deltaX < 0) {
                (currentTabIndex + 1).coerceAtMost(tabCount - 1)
            } else {
                (currentTabIndex - 1).coerceAtLeast(0)
            }
            if (newIndex != currentTabIndex) {
                currentTabIndex = newIndex
                onTabChange?.invoke(newIndex)
                return true
            }
            return false
        }
    })

    private var startX = 0f
    private var startY = 0f
    private var intercepted = false
    private var hasSentSyntheticDown = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = ev.x
                startY = ev.y
                intercepted = false
                hasSentSyntheticDown = false
                return false
            }
            MotionEvent.ACTION_MOVE -> {
                if (intercepted) return true
                val dx = kotlin.math.abs(ev.x - startX)
                val dy = kotlin.math.abs(ev.y - startY)
                val touchSlop = 40
                if (dx > touchSlop || dy > touchSlop) {
                    if (dx > dy) {
                        intercepted = true
                        return true
                    }
                }
                return false
            }
        }
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (intercepted) {
            if (!hasSentSyntheticDown) {
                hasSentSyntheticDown = true
                val down = MotionEvent.obtain(event.downTime, event.eventTime, MotionEvent.ACTION_DOWN, startX, startY, 0)
                gestureDetector.onTouchEvent(down)
                down.recycle()
            }
            val handled = gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                intercepted = false
                hasSentSyntheticDown = false
            }
            return handled || super.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    fun setCurrentTab(index: Int) {
        currentTabIndex = index.coerceIn(0, tabCount - 1)
    }

    fun setOnTabChangeListener(listener: (Int) -> Unit) {
        onTabChange = listener
    }
}
