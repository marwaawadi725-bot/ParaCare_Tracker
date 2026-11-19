package com.paracare.tracker.ui

import android.content.Intent
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.paracare.tracker.R
import com.paracare.tracker.data.AppDatabase
import com.paracare.tracker.data.User
import com.paracare.tracker.databinding.ActivityRegisterBinding
import com.paracare.tracker.util.PasswordUtils
import com.paracare.tracker.util.SessionManager
import kotlinx.coroutines.launch

/** Écran d'inscription (Client / Pharmacien). */
class RegisterActivity : AppCompatActivity() {

    private lateinit var b: ActivityRegisterBinding
    private lateinit var db: AppDatabase
    private var selectedRole = User.ROLE_CLIENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(b.root)
        db = AppDatabase.get(this)

        b.btnBack.setOnClickListener { finish() }
        b.tvLogin.setOnClickListener { finish() }

        b.cardClient.setOnClickListener { selectRole(User.ROLE_CLIENT) }
        b.cardPharmacien.setOnClickListener { selectRole(User.ROLE_PHARMACIEN) }
        selectRole(User.ROLE_CLIENT)

        b.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b4: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = updateStrength(s?.toString().orEmpty())
        })
        updateStrength("")

        b.btnCreate.setOnClickListener { register() }
    }

    private fun selectRole(role: String) {
        selectedRole = role
        val on = ContextCompat.getColor(this, R.color.primary)
        val off = ContextCompat.getColor(this, R.color.stroke)
        val isClient = role == User.ROLE_CLIENT
        b.cardClient.strokeColor = if (isClient) on else off
        b.cardClient.strokeWidth = if (isClient) 4 else 2
        b.cardPharmacien.strokeColor = if (!isClient) on else off
        b.cardPharmacien.strokeWidth = if (!isClient) 4 else 2
        b.tvRoleHint.setText(if (isClient) R.string.hint_client else R.string.hint_pharmacist)
    }

    private fun updateStrength(p: String) {
        val level = PasswordUtils.strength(p)
        val (labelRes, colorRes) = when (level) {
            0 -> R.string.strength_none to R.color.text_secondary
            1 -> R.string.strength_weak to R.color.danger
            2 -> R.string.strength_medium to R.color.warning
            else -> R.string.strength_strong to R.color.primary
        }
        b.tvStrength.setText(labelRes)
        b.tvStrength.setTextColor(ContextCompat.getColor(this, colorRes))
        b.strengthBar.max = 3
        b.strengthBar.progress = level
        b.strengthBar.setIndicatorColor(ContextCompat.getColor(this, colorRes))
    }

    private fun register() {
        listOf(b.tilName, b.tilEmail, b.tilPhone, b.tilPassword, b.tilConfirm).forEach { it.error = null }

        val name = b.etName.text.toString().trim()
        val email = b.etEmail.text.toString().trim().lowercase()
        val phone = b.etPhone.text.toString().trim()
        val pass = b.etPassword.text.toString()
        val confirm = b.etConfirm.text.toString()

        var ok = true
        if (name.length < 2) { b.tilName.error = getString(R.string.err_name_required); ok = false }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { b.tilEmail.error = getString(R.string.err_email_invalid); ok = false }
        if (phone.filter { it.isDigit() }.length < 8) { b.tilPhone.error = getString(R.string.err_phone_invalid); ok = false }
        if (pass.length < 8) { b.tilPassword.error = getString(R.string.err_password_short); ok = false }
        if (confirm != pass) { b.tilConfirm.error = getString(R.string.err_password_mismatch); ok = false }
        if (!b.cbTerms.isChecked) {
            Toast.makeText(this, R.string.err_terms, Toast.LENGTH_LONG).show(); ok = false
        }
        if (!ok) return

        lifecycleScope.launch {
            if (db.userDao().findByEmail(email) != null) {
                b.tilEmail.error = getString(R.string.err_email_taken)
                return@launch
            }
            val salt = PasswordUtils.newSalt()
            val user = User(
                fullName = name, email = email, phone = phone,
                passwordHash = PasswordUtils.hash(pass, salt), salt = salt,
                role = selectedRole, acceptsAlerts = b.cbAlerts.isChecked
            )
            try {
                val id = db.userDao().insert(user)
                SessionManager(this@RegisterActivity).login(id, user.role)
                Toast.makeText(this@RegisterActivity, R.string.account_created, Toast.LENGTH_SHORT).show()
                val target = if (user.role == User.ROLE_PHARMACIEN) PharmacistActivity::class.java
                else HomeActivity::class.java
                startActivity(Intent(this@RegisterActivity, target)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            } catch (e: SQLiteConstraintException) {
                b.tilEmail.error = getString(R.string.err_email_taken)
            }
        }
    }
}
