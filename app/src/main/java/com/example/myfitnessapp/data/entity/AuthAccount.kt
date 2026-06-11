package com.example.myfitnessapp.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "auth_accounts",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["phone"], unique = true)
    ]
)
data class AuthAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "password_hash")
    val passwordHash: String,

    @ColumnInfo(name = "phone")
    val phone: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_login_at")
    val lastLoginAt: Long = System.currentTimeMillis()
)
