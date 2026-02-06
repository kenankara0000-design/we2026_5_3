package com.example.we2026_5.util

import android.util.Log

/**
 * Debug-Instrumentierung nur für Logcat.
 * Filter: adb logcat -s We2026Debug:D
 * Oder in Android Studio Logcat: Tag = We2026Debug
 */
object AgentDebugLog {
    private const val TAG = "We2026Debug"

    fun log(location: String, message: String, data: Map<String, Any?>, hypothesisId: String) {
        val dataStr = data.entries.joinToString(" ") { "${it.key}=${it.value}" }
        val line = "[$hypothesisId] $location | $message | $dataStr"
        Log.d(TAG, line)
    }
}
