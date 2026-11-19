package com.paracare.tracker.util

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object PasswordUtils {
    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hash(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest((salt + password).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** 0 = vide, 1 = faible, 2 = moyen, 3 = fort */
    fun strength(p: String): Int {
        if (p.isEmpty()) return 0
        var s = 0
        if (p.length >= 8) s++
        if (p.any { it.isDigit() } && p.any { it.isLetter() }) s++
        if (p.any { !it.isLetterOrDigit() } || p.length >= 12) s++
        return s.coerceAtLeast(1)
    }
}
