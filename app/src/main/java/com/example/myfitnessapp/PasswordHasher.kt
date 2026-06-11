package com.example.myfitnessapp

import java.security.MessageDigest

object PasswordHasher {
    fun sha256(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        return bytes.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
