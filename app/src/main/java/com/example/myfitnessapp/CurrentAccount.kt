package com.example.myfitnessapp

import android.content.Context

object CurrentAccount {
    fun requireUsername(context: Context): String {
        return AuthSessionManager.getUsername(context).ifBlank {
            error("No logged in account found for user-scoped data access.")
        }
    }
}
