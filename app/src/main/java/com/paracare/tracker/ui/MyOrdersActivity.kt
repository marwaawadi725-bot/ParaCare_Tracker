package com.paracare.tracker.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.paracare.tracker.R
import com.paracare.tracker.data.Order
import com.paracare.tracker.databinding.ActivityListBinding
import com.paracare.tracker.util.AlertManager
import com.paracare.tracker.util.DateUtils
import kotlinx.coroutines.launch

/** Suivi en temps réel : badge orange EN_ATTENTE, badge vert PREPAREE. */
class MyOrdersActivity : BaseActivity() {

    private lateinit var b: ActivityListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityListBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.tvTitle.text = "Mes commandes"
        b.tvEmpty.text = "Vous n'avez pas encore passé de commande."
        b.btnBack.setOnClickListener { finish() }

        val adapter = RowAdapter<Order>(R.layout.item_row) { v, o ->
            val ready = o.status == Order.PREPAREE
            val c = color(if (ready) R.color.level_green else R.color.level_orange)
            v.row(
                "#${o.id} • ${o.productName} ×${o.quantity}",
                "Total : ${DateUtils.price(o.total())}",
                "Passée le ${DateUtils.format(o.createdAt)}", c
            )
            v.rowBadge(if (ready) "✅ PRÉPARÉE" else "⏳ EN ATTENTE", c)
        }
        b.rv.layoutManager = LinearLayoutManager(this)
        b.rv.adapter = adapter

        val uid = session.userId
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.orderDao().forClientFlow(uid).collect { list ->
                    adapter.submit(list)                       // mise à jour dynamique du statut
                    b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    AlertManager.checkOrders(this@MyOrdersActivity, uid)  // notification dès validation
                }
            }
        }
    }
}
