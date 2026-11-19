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

/** Accueil Client : trousse, catalogue, suivi des commandes. */
class HomeActivity : BaseActivity() {

    private lateinit var b: ActivityHubBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityHubBinding.inflate(layoutInflater)
        setContentView(b.root)
        requestNotifPermission()

        b.tvHubTitle.text = "Espace Client"
        b.btnLogout.setOnClickListener { logout() }
        addHubCard(b.cards, "🧴", "Ma trousse", "Mes produits, PAO & recommandations IA") { go(TrousseActivity::class.java) }
        addHubCard(b.cards, "🛍️", "Catalogue", "Commander en parapharmacie") { go(ProductsActivity::class.java) }
        addHubCard(b.cards, "📦", "Mes commandes", "Suivi en temps réel du statut") { go(MyOrdersActivity::class.java) }

        val uid = session.userId
        lifecycleScope.launch { b.tvWelcome.text = "Bonjour, ${currentUser()?.fullName ?: ""}" }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(db.userItemDao().itemsFlow(uid), db.orderDao().forClientFlow(uid)) { items, orders ->
                    items to orders
                }.collect { (items, orders) ->
                    val watch = items.count { Level.of(it.deadline()) != Level.GREEN }
                    val pending = orders.count { it.status == Order.EN_ATTENTE }
                    b.tvSummary.text = "🧴 ${items.size} produit(s)  •  ⚠️ $watch à surveiller  •  ⏳ $pending commande(s) en attente"
                    AlertManager.checkClient(this@HomeActivity, uid)
                    AlertManager.checkOrders(this@HomeActivity, uid)
                }
            }
        }
    }
}
