package com.example.we2026_5.data.repository

import android.content.Context

private const val PREFS_NAME = "tour_order_prefs"
private const val KEY_PREFIX = "order_"

class TourOrderRepositoryImpl(context: Context) : TourOrderRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getOrderForDate(dateKey: String): List<String> {
        val raw = prefs.getString(KEY_PREFIX + dateKey, null) ?: return emptyList()
        if (raw.isEmpty()) return emptyList()
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    }

    override fun setOrderForDate(dateKey: String, customerIds: List<String>) {
        prefs.edit()
            .putString(KEY_PREFIX + dateKey, customerIds.joinToString(","))
            .apply()
    }
}
