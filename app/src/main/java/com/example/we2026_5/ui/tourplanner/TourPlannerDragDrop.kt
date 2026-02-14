package com.example.we2026_5.ui.tourplanner

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.example.we2026_5.ListItem

/** val LazyListItemInfo.offsetEnd - untere Kante des Items */
private val LazyListItemInfo.offsetEnd: Int
    get() = offset + size

/** Sucht LazyListItemInfo für absoluten Index (unter sichtbaren Items). */
private fun LazyListState.getVisibleItemInfoFor(absoluteIndex: Int): LazyListItemInfo? =
    layoutInfo.visibleItemsInfo.firstOrNull { it.index == absoluteIndex }

/**
 * State für Drag & Drop im Tourenplaner. Nur CustomerItems sind ziehbar.
 * Long-Press auf Kundenkarte startet den Drag – keine Symbole.
 */
class TourPlannerDragDropState(
    val lazyListState: LazyListState,
    private val isCustomerItem: (Int) -> Boolean,
    private val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    private val onDragEnd: () -> Unit
) {
    var draggedDistance by mutableStateOf(0f)
    private set

    var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)
    private set

    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)

    private val initialOffsets: Pair<Float, Float>?
        get() = initiallyDraggedElement?.let { it.offset.toFloat() to it.offsetEnd.toFloat() }

    val elementDisplacement: Float?
        get() = currentIndexOfDraggedItem
            ?.let { lazyListState.getVisibleItemInfoFor(it) }
            ?.let { item ->
                (initiallyDraggedElement?.offset ?: 0).toFloat() + draggedDistance - item.offset
            }

    fun onDragStart(offset: Offset) {
        val viewportY = offset.y.toInt()
        val hit = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            viewportY in item.offset..(item.offset + item.size)
        }
        hit?.takeIf { isCustomerItem(hit.index) }?.let {
            currentIndexOfDraggedItem = it.index
            initiallyDraggedElement = it
        }
    }

    /** Startet Drag direkt mit bekanntem Index (z. B. wenn Geste auf dem Item liegt). */
    fun onDragStartWithIndex(index: Int) {
        if (isCustomerItem(index)) {
            currentIndexOfDraggedItem = index
            initiallyDraggedElement = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
        }
    }

    fun onDragInterrupted() {
        draggedDistance = 0f
        currentIndexOfDraggedItem = null
        initiallyDraggedElement = null
        onDragEnd()
    }

    fun onDrag(dragAmount: Offset) {
        draggedDistance += dragAmount.y

        initialOffsets?.let { (topOffset, bottomOffset) ->
            val startOffset = topOffset + draggedDistance
            val endOffset = bottomOffset + draggedDistance

            currentIndexOfDraggedItem?.let { _ ->
                val currentElement = lazyListState.getVisibleItemInfoFor(currentIndexOfDraggedItem!!)
                currentElement?.let { hovered ->
                    val swapTarget = lazyListState.layoutInfo.visibleItemsInfo
                        .filterNot { item ->
                            item.offsetEnd < startOffset || item.offset > endOffset || hovered.index == item.index
                        }
                        .filter { isCustomerItem(it.index) }
                        .firstOrNull { item ->
                            val delta = startOffset - hovered.offset
                            when {
                                delta > 0 -> endOffset > item.offsetEnd
                                else -> startOffset < item.offset
                            }
                        }

                    swapTarget?.let { target ->
                        if (hovered.index != target.index) {
                            onMove(hovered.index, target.index)
                            currentIndexOfDraggedItem = target.index
                        }
                    }
                }
            }
        }
    }

    fun checkForOverScroll(): Float {
        return initiallyDraggedElement?.let {
            val startOffset = it.offset + draggedDistance
            val endOffset = it.offsetEnd + draggedDistance
            when {
                draggedDistance > 0 -> (endOffset - lazyListState.layoutInfo.viewportEndOffset)
                    .takeIf { diff -> diff > 0 }
                draggedDistance < 0 -> (startOffset - lazyListState.layoutInfo.viewportStartOffset)
                    .takeIf { diff -> diff < 0 }
                else -> null
            }
        } ?: 0f
    }
}
