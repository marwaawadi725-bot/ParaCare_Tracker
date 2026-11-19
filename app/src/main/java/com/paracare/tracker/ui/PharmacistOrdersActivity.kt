package com.paracare.tracker.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.paracare.tracker.R
import com.paracare.tracker.data.Order
import com.paracare.tracker.data.OrderService
import com.paracare.tracker.databinding.ActivityListBinding
import com.paracare.tracker.util.DateUtils
import kotlinx.coroutines.launch

class PharmacistOrdersActivity : BaseActivity() {

    private lateinit var b: ActivityListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityListBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.tvTitle.text = "Commandes clients"
        b.tvEmpty.text = "Aucune commande pour le moment."
        b.btnBack.setOnClickListener { finish() }

        val adapter = RowAdapter<Order>(R.layout.item_row) { v, o ->
            val ready = o.status == Order.PREPAREE
            val c = color(if (ready) R.color.level_green else R.color.level_orange)
            v.row(
                "#${o.id} • ${o.productName} ×${o.quantity}",
                "Client : ${o.clientName} • ${DateUtils.price(o.total())}",
                "Passée le ${DateUtils.format(o.createdAt)}", c
            )
            v.rowBadge(if (ready) "PRÉPARÉE" else "EN ATTENTE", c)
            if (!ready) v.rowAction("Valider") {
                lifecycleScope.launch {
                    OrderService.validate(db, o.id)
                    toast("Commande #${o.id} validée — le client sera notifié")
                }
            }
        }
        b.rv.layoutManager = LinearLayoutManager(this)
        b.rv.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.orderDao().allFlow().collect { list ->
                    adapter.submit(list)
                    b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}
