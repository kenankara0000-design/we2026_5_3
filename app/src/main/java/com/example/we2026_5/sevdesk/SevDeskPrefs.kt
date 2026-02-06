package com.example.we2026_5.sevdesk

import android.content.Context
import android.content.SharedPreferences

private const val PREFS_NAME = "sevdesk_prefs"
private const val KEY_API_TOKEN = "api_token"

fun getSevDeskToken(context: Context): String =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_API_TOKEN, "") ?: ""

fun setSevDeskToken(context: Context, token: String) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_API_TOKEN, token).apply()
}
