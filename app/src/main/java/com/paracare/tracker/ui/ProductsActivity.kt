package com.paracare.tracker.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.paracare.tracker.R
import com.paracare.tracker.data.Product
import com.paracare.tracker.databinding.ActivityListBinding
import com.paracare.tracker.util.DateUtils
import com.paracare.tracker.util.Level
import kotlinx.coroutines.launch

/** Catalogue client : articles en stock + commande (statut initial EN_ATTENTE). */
class ProductsActivity : BaseActivity() {

    private lateinit var b: ActivityListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityListBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.tvTitle.text = "Catalogue"
        b.tvEmpty.text = "Aucun produit disponible pour le moment."
        b.btnBack.setOnClickListener { finish() }

        val adapter = RowAdapter<Product>(R.layout.item_row) { v, p ->
            val dot = color(Level.of(p.expiryDate).colorRes)
            val price = if (p.discountPercent > 0)
                "${DateUtils.price(p.finalPrice())} (au lieu de ${DateUtils.price(p.price)})"
            else DateUtils.price(p.price)
            v.row(p.name, "${p.category} • $price", "Exp. ${DateUtils.format(p.expiryDate)} • Stock : ${p.quantity}", dot)
            if (p.destocked) v.rowBadge("DÉSTOCKAGE", color(R.color.level_red))
            else if (p.discountPercent > 0) v.rowBadge("-${p.discountPercent} %", color(R.color.primary))
            v.rowAction("Commander") {
                askNumber("Quantité à commander", 1, 1, p.quantity) { q -> placeOrder(p, q) }
            }
        }
        b.rv.layoutManager = LinearLayoutManager(this)
        b.rv.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.productDao().allFlow().collect { list ->
                    val now = System.currentTimeMillis()
                    val available = list.filter { it.quantity > 0 && it.expiryDate >= now }
                    adapter.submit(available)
                    b.tvEmpty.visibility = if (available.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}
