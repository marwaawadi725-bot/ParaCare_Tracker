package com.paracare.tracker.ui

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.paracare.tracker.data.Order
import com.paracare.tracker.databinding.ActivityHubBinding
import com.paracare.tracker.util.AlertManager
import com.paracare.tracker.util.Level
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Hub Pharmacien : 3 cartes (ajout, stock, commandes) + dashboard anti-gaspillage. */
class PharmacistActivity : BaseActivity() {

    private lateinit var b: ActivityHubBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHubBinding.inflate(layoutInflater)
        setContentView(b.root)
        requestNotifPermission()

        b.tvHubTitle.text = "Espace Pharmacien"
        b.btnLogout.setOnClickListener { logout() }
        addHubCard(b.cards, "➕", "Ajouter un produit", "Saisie manuelle ou scan IA") { go(AddProductActivity::class.java) }
        addHubCard(b.cards, "📋", "Stock complet", "Tous les produits et quantités") { go(StockListActivity::class.java) }
        addHubCard(b.cards, "🧾", "Commandes clients", "Valider les commandes en attente") { go(PharmacistOrdersActivity::class.java) }
        addHubCard(b.cards, "📊", "Dashboard anti-gaspillage", "Vert / orange / rouge, remises, déstockage") { go(ExpiryDashboardActivity::class.java) }

        lifecycleScope.launch { b.tvWelcome.text = "Bonjour, ${currentUser()?.fullName ?: ""}" }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(db.productDao().allFlow(), db.orderDao().allFlow()) { p, o -> p to o }
                    .collect { (products, orders) ->
                        val orange = products.count { Level.of(it.expiryDate) == Level.ORANGE }
                        val red = products.count { Level.of(it.expiryDate) == Level.RED }
                        val pending = orders.count { it.status == Order.EN_ATTENTE }
                        b.tvSummary.text = "📦 ${products.size} produit(s)  •  🟠 $orange proche(s)  •  🔴 $red périmé(s)  •  🧾 $pending en attente"
                        AlertManager.checkPharmacist(this@PharmacistActivity)
                    }
            }
        }
    }
}
