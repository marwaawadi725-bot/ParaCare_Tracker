package com.paracare.tracker.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.paracare.tracker.R
import com.paracare.tracker.data.Product
import com.paracare.tracker.databinding.ActivityListBinding
import com.paracare.tracker.util.DateUtils
import com.paracare.tracker.util.Level
import kotlinx.coroutines.launch

class StockListActivity : BaseActivity() {

    private lateinit var b: ActivityListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityListBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.tvTitle.text = "Stock complet"
        b.tvEmpty.text = "Le stock est vide. Ajoutez un produit."
        b.btnBack.setOnClickListener { finish() }
        b.fab.visibility = View.VISIBLE
        b.fab.setOnClickListener { go(AddProductActivity::class.java) }

        val adapter = RowAdapter<Product>(R.layout.item_row) { v, p ->
            val lvl = Level.of(p.expiryDate)
            val c = color(lvl.colorRes)
            v.row(
                p.name,
                "${p.category} • ${DateUtils.price(p.finalPrice())} • Qté : ${p.quantity}",
                "${DateUtils.describe(p.expiryDate)} (${DateUtils.format(p.expiryDate)})", c
            )
            v.rowBadge(lvl.label.uppercase(), c)
            v.setOnClickListener { options(p) }
        }
        b.rv.layoutManager = LinearLayoutManager(this)
        b.rv.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.productDao().allFlow().collect { list ->
                    adapter.submit(list)
                    b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun options(p: Product) {
        MaterialAlertDialogBuilder(this)
            .setTitle(p.name)
            .setItems(arrayOf("Modifier la quantité", "Supprimer du stock")) { _, i ->
                if (i == 0) {
                    askNumber("Nouvelle quantité", p.quantity, 0, 100000) { q ->
                        lifecycleScope.launch { db.productDao().update(p.copy(quantity = q)) }
                    }
                } else {
                    MaterialAlertDialogBuilder(this)
                        .setMessage("Supprimer « ${p.name} » du stock ?")
                        .setPositiveButton("Supprimer") { _, _ -> lifecycleScope.launch { db.productDao().delete(p) } }
                        .setNegativeButton("Annuler", null).show()
                }
            }.show()
    }
}
