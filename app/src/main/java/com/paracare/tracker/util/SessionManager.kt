package com.paracare.tracker.util

import android.content.Context

class SessionManager(context: Context) {
    private val prefs = context.getSharedPreferences("paracare_session", Context.MODE_PRIVATE)

    fun login(userId: Long, role: String) {
        prefs.edit()
            .putLong(KEY_USER, userId)
            .putString(KEY_ROLE, role)
            .putLong(KEY_LAST_USER, userId)   // sert à la connexion par empreinte
            .apply()
    }

    fun logout() {
        prefs.edit().remove(KEY_USER).remove(KEY_ROLE).apply()
    }

    val userId: Long get() = prefs.getLong(KEY_USER, -1L)
    val role: String? get() = prefs.getString(KEY_ROLE, null)
    val lastUserId: Long get() = prefs.getLong(KEY_LAST_USER, -1L)
    val isLoggedIn: Boolean get() = userId != -1L

    companion object {
        private const val KEY_USER = "user_id"
        private const val KEY_ROLE = "role"
        private const val KEY_LAST_USER = "last_user_id"
    }
}
