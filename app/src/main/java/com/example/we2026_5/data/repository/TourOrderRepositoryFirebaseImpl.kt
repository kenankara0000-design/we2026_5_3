package com.example.we2026_5.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap

/**
 * Speichert die Tour-Reihenfolge in Firebase (geräteübergreifend).
 * Key = Wochentag (1=Sonntag … 7=Samstag), damit die Order pro Wochentag stabil bleibt.
 */
class TourOrderRepositoryFirebaseImpl(
    private val database: FirebaseDatabase
) : TourOrderRepository {

    private val tourReihenfolgeRef = database.reference.child("tourReihenfolge")

    private val cache = ConcurrentHashMap<String, List<String>>()

    init {
        tourReihenfolgeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cache.clear()
                snapshot.children.forEach { child ->
                    val key = child.key ?: return@forEach
                    val raw = child.getValue(String::class.java) ?: return@forEach
                    cache[key] = if (raw.isEmpty()) emptyList() else raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun getOrderForDate(dateKey: String): List<String> {
        val weekdayKey = dateKeyToWeekdayKey(dateKey) ?: return emptyList()
        return cache[weekdayKey] ?: emptyList()
    }

    override fun setOrderForDate(dateKey: String, customerIds: List<String>) {
        val weekdayKey = dateKeyToWeekdayKey(dateKey) ?: return
        val value = customerIds.joinToString(",")
        tourReihenfolgeRef.child(weekdayKey).setValue(value)
        cache[weekdayKey] = customerIds
    }

    /** "yyyyMMdd" -> "wochentag_1" … "wochentag_7" (Calendar.SUNDAY=1, MONDAY=2, …). */
    private fun dateKeyToWeekdayKey(dateKey: String): String? {
        if (dateKey.length != 8) return null
        val year = dateKey.substring(0, 4).toIntOrNull() ?: return null
        val month = dateKey.substring(4, 6).toIntOrNull()?.minus(1) ?: return null
        val day = dateKey.substring(6, 8).toIntOrNull() ?: return null
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        return "wochentag_$dayOfWeek"
    }
}
