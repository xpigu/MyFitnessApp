package com.example.myfitnessapp

import android.content.Context

object AuthSessionManager {
    private const val PREFS_NAME = "auth_session_prefs"
    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_USERNAME = "username"

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_LOGGED_IN, false)
    }

    fun getUsername(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USERNAME, "").orEmpty()
    }

    fun saveLogin(context: Context, username: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
