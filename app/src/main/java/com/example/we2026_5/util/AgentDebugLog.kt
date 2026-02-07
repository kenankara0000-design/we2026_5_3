package com.example.we2026_5.util

import android.util.Log
import java.io.File

/**
 * Debug-Instrumentierung für Logcat und optional NDJSON-Datei.
 * Filter: adb logcat -s We2026Debug:D
 * Datei: nach Reproduktion per adb pull holen (siehe Reproduktionsschritte).
 */
object AgentDebugLog {
    private const val TAG = "We2026Debug"
    @Volatile private var logFile: File? = null

    fun setLogFile(file: File) {
        logFile = file
        try {
            file.parentFile?.mkdirs()
            if (!file.exists()) file.createNewFile()
        } catch (_: Exception) { }
    }

    fun log(location: String, message: String, data: Map<String, Any?>, hypothesisId: String) {
        val dataStr = data.entries.joinToString(" ") { "${it.key}=${it.value}" }
        val line = "[$hypothesisId] $location | $message | $dataStr"
        Log.d(TAG, line)
        // #region agent log
        logFile?.let { f ->
            try {
                val ts = System.currentTimeMillis()
                val dataJson = data.entries.joinToString(",") { "\"${it.key}\":${jsonVal(it.value)}" }
                val ndjson = "{\"hypothesisId\":\"$hypothesisId\",\"location\":\"${esc(location)}\",\"message\":\"${esc(message)}\",\"data\":{$dataJson},\"timestamp\":$ts}\n"
                f.appendText(ndjson)
            } catch (_: Exception) { }
        }
        // #endregion
    }

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")
    private fun jsonVal(v: Any?): String = when (v) {
        is Number -> v.toString()
        is Boolean -> v.toString()
        null -> "null"
        else -> "\"${esc(v.toString())}\""
    }
}
