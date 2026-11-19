package com.paracare.tracker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.paracare.tracker.R
import com.paracare.tracker.data.AppDatabase
import com.paracare.tracker.data.OrderService
import com.paracare.tracker.data.Product
import com.paracare.tracker.data.User
import com.paracare.tracker.util.SessionManager
import kotlinx.coroutines.launch

open class BaseActivity : AppCompatActivity() {

    val db: AppDatabase by lazy { AppDatabase.get(this) }
    val session: SessionManager by lazy { SessionManager(this) }

    protected suspend fun currentUser(): User? = db.userDao().findById(session.userId)

    protected fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    protected fun color(res: Int) = ContextCompat.getColor(this, res)

    protected fun go(cls: Class<*>) = startActivity(Intent(this, cls))

    protected fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
        }
    }

    protected fun logout() {
        session.logout()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    protected fun addHubCard(container: LinearLayout, emoji: String, title: String, sub: String, onClick: () -> Unit) {
        val v = layoutInflater.inflate(R.layout.item_hub_card, container, false)
        v.findViewById<TextView>(R.id.tvIcon).text = emoji
        v.findViewById<TextView>(R.id.tvCardTitle).text = title
        v.findViewById<TextView>(R.id.tvCardSub).text = sub
        v.setOnClickListener { onClick() }
        container.addView(v)
    }

    /** Boîte de dialogue pour saisir un nombre entre min et max. */
    protected fun askNumber(title: String, def: Int, min: Int, max: Int, onOk: (Int) -> Unit) {
        val et = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(def.toString())
            setSelection(text.length)
        }
        val box = FrameLayout(this).apply { setPadding(56, 16, 56, 0); addView(et) }
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(box)
            .setPositiveButton("OK") { _, _ ->
                val n = et.text.toString().toIntOrNull()
                if (n == null || n < min || n > max) toast("Entrez une valeur entre $min et $max") else onOk(n)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    /** Commande un produit (statut initial EN_ATTENTE) pour l'utilisateur connecté. */
    protected fun placeOrder(p: Product, qty: Int) {
        lifecycleScope.launch {
            val u = currentUser() ?: return@launch
            val err = OrderService.place(db, u.id, u.fullName, p.id, qty)
            toast(err ?: "Commande envoyée — statut : EN_ATTENTE")
        }
    }
}
