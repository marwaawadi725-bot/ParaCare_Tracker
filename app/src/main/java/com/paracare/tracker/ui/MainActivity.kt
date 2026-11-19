package com.paracare.tracker.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.paracare.tracker.R
import com.paracare.tracker.data.AppDatabase
import com.paracare.tracker.data.DemoData
import com.paracare.tracker.data.User
import com.paracare.tracker.databinding.ActivityMainBinding
import com.paracare.tracker.util.PasswordUtils
import com.paracare.tracker.util.SessionManager
import kotlinx.coroutines.launch

/** Écran de connexion (Client / Pharmacien Pro). */
class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var session: SessionManager
    private lateinit var db: AppDatabase
    private var selectedRole = User.ROLE_CLIENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        session = SessionManager(this)
        db = AppDatabase.get(this)
        lifecycleScope.launch { DemoData.seedIfEmpty(db) }

        // Session déjà ouverte -> redirection directe
        if (session.isLoggedIn) {
            redirect(session.role ?: User.ROLE_CLIENT)
            return
        }

        b.roleToggle.check(R.id.btnClient)
        b.roleToggle.addOnButtonCheckedListener { _, id, checked ->
            if (checked) selectedRole =
                if (id == R.id.btnPharmacien) User.ROLE_PHARMACIEN else User.ROLE_CLIENT
        }

        b.btnLogin.setOnClickListener { attemptLogin() }
        b.btnFingerprint.setOnClickListener { fingerprintLogin() }
        b.tvCreateAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        b.tvForgot.setOnClickListener {
            Toast.makeText(this, R.string.forgot_info, Toast.LENGTH_LONG).show()
        }
    }

    private fun attemptLogin() {
        b.tilEmail.error = null
        b.tilPassword.error = null
        val email = b.etEmail.text.toString().trim().lowercase()
        val password = b.etPassword.text.toString()

        if (email.isEmpty()) { b.tilEmail.error = getString(R.string.err_email_required); return }
        if (password.isEmpty()) { b.tilPassword.error = getString(R.string.err_password_required); return }

        lifecycleScope.launch {
            val user = db.userDao().findByEmail(email)
            when {
                user == null || user.passwordHash != PasswordUtils.hash(password, user.salt) ->
                    b.tilPassword.error = getString(R.string.err_bad_credentials)
                user.role != selectedRole ->
                    Toast.makeText(
                        this@MainActivity,
                        if (user.role == User.ROLE_CLIENT) R.string.err_wrong_role_client
                        else R.string.err_wrong_role_pharmacist,
                        Toast.LENGTH_LONG
                    ).show()
                else -> {
                    session.login(user.id, user.role)
                    redirect(user.role)
                }
            }
        }
    }

    private fun fingerprintLogin() {
        val last = session.lastUserId
        if (last == -1L) {
            Toast.makeText(this, R.string.fp_first_login, Toast.LENGTH_LONG).show()
            return
        }
        val can = BiometricManager.from(this).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, R.string.fp_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        val prompt = BiometricPrompt(
            this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    lifecycleScope.launch {
                        val user = db.userDao().findById(last)
                        if (user != null) {
                            session.login(user.id, user.role)
                            redirect(user.role)
                        }
                    }
                }
            })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.fp_title))
            .setSubtitle(getString(R.string.fp_subtitle))
            .setNegativeButtonText(getString(R.string.cancel))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()
        prompt.authenticate(info)
    }

    private fun redirect(role: String) {
        val target = if (role == User.ROLE_PHARMACIEN) PharmacistActivity::class.java
        else HomeActivity::class.java
        startActivity(Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
