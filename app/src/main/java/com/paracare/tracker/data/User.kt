package com.paracare.tracker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val email: String,
    val phone: String,
    val passwordHash: String,
    val salt: String,
    val role: String,               // "CLIENT" ou "PHARMACIEN"
    val acceptsAlerts: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val ROLE_CLIENT = "CLIENT"
        const val ROLE_PHARMACIEN = "PHARMACIEN"
    }
}
