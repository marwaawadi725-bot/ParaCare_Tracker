package com.paracare.tracker.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.paracare.tracker.R
import com.paracare.tracker.data.Product
import com.paracare.tracker.databinding.ActivityDashboardBinding
import com.paracare.tracker.util.DateUtils
import com.paracare.tracker.util.Level
import kotlinx.coroutines.launch

/** Dashboard anti-gaspillage : compteurs par palier, filtre, tri, remises & déstockage. */
class ExpiryDashboardActivity : BaseActivity() {

    private lateinit var b: ActivityDashboardBinding
    private lateinit var adapter: RowAdapter<Product>
    private var all: List<Product> = emptyList()
    private var filter: Level? = null
    private var urgentFirst = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.btnBack.setOnClickListener { finish() }

        adapter = RowAdapter(R.layout.item_row) { v, p ->
            val lvl = Level.of(p.expiryDate)
            val c = color(lvl.colorRes)
            v.row(
                p.name,
                "${p.category} • Qté : ${p.quantity} • ${DateUtils.price(p.finalPrice())}",
                "${DateUtils.describe(p.expiryDate)} (${DateUtils.format(p.expiryDate)})", c
            )
            when {
                p.destocked -> v.rowBadge("DÉSTOCKAGE", c)
                p.discountPercent > 0 -> v.rowBadge("-${p.discountPercent} %", color(R.color.primary))
                else -> v.rowBadge(lvl.label.uppercase(), c)
            }
            if (lvl == Level.ORANGE) {
                v.rowAction("Agir") { decide(p) }
                v.setOnClickListener { decide(p) }
            } else if (lvl == Level.RED) {
                v.rowAction("Retirer") { removeExpired(p) }
                v.setOnClickListener { removeExpired(p) }
            }
        }
        b.rv.layoutManager = LinearLayoutManager(this)
        b.rv.adapter = adapter

        b.cardGreen.setOnClickListener { toggle(Level.GREEN) }
        b.cardOrange.setOnClickListener { toggle(Level.ORANGE) }
        b.cardRed.setOnClickListener { toggle(Level.RED) }
        b.btnSort.setOnClickListener { urgentFirst = !urgentFirst; render() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                db.productDao().allFlow().collect { all = it; render() }
            }
        }
    }

    private fun toggle(l: Level) { filter = if (filter == l) null else l; render() }

    private fun render() {
        val g = all.count { Level.of(it.expiryDate) == Level.GREEN }
        val o = all.count { Level.of(it.expiryDate) == Level.ORANGE }
        val r = all.count { Level.of(it.expiryDate) == Level.RED }
        b.tvCountGreen.text = g.toString()
        b.tvCountOrange.text = o.toString()
        b.tvCountRed.text = r.toString()

        mark(b.cardGreen, filter == Level.GREEN, R.color.level_green)
        mark(b.cardOrange, filter == Level.ORANGE, R.color.level_orange)
        mark(b.cardRed, filter == Level.RED, R.color.level_red)

        var list = all.filter { filter == null || Level.of(it.expiryDate) == filter }.sortedBy { it.expiryDate }
        if (!urgentFirst) list = list.reversed()
        adapter.submit(list)
        b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        b.tvFilterInfo.text = if (filter == null) "Tous les produits (${list.size})" else "Filtre : ${filter!!.label} (${list.size})"
        b.btnSort.text = if (urgentFirst) "Tri : plus urgent d'abord" else "Tri : plus tard d'abord"
    }

    private fun mark(card: MaterialCardView, selected: Boolean, colorRes: Int) {
        card.strokeColor = color(colorRes)
        card.strokeWidth = if (selected) 6 else 0
    }

    // ---- Outils de décision anti-gaspillage (produits orange) ----
    private fun decide(p: Product) {
        MaterialAlertDialogBuilder(this)
            .setTitle(p.name)
            .setItems(arrayOf("Appliquer une remise promotionnelle", "Déstockage immédiat (-70 %)")) { _, i ->
                if (i == 0) askDiscount(p) else destock(p)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun askDiscount(p: Product) {
        val pct = intArrayOf(10, 20, 30, 50)
        MaterialAlertDialogBuilder(this)
            .setTitle("Remise sur ${p.name}")
            .setItems(pct.map { "-$it %" }.toTypedArray()) { _, i ->
                lifecycleScope.launch {
                    db.productDao().update(p.copy(discountPercent = pct[i], destocked = false))
                    toast("Remise de ${pct[i]} % appliquée")
                }
            }.show()
    }

    private fun destock(p: Product) {
        lifecycleScope.launch {
            db.productDao().update(p.copy(discountPercent = 70, destocked = true))
            toast("« ${p.name} » mis au déstockage (-70 %)")
        }
    }

    private fun removeExpired(p: Product) {
        MaterialAlertDialogBuilder(this)
            .setMessage("Retirer le lot périmé « ${p.name} » du stock ?")
            .setPositiveButton("Retirer") { _, _ -> lifecycleScope.launch { db.productDao().delete(p) } }
            .setNegativeButton("Annuler", null)
            .show()
    }
}
