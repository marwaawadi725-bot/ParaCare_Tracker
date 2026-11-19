package com.paracare.tracker.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.paracare.tracker.R
import com.paracare.tracker.data.UserItem
import com.paracare.tracker.databinding.ActivityTrousseBinding
import com.paracare.tracker.util.AlertManager
import com.paracare.tracker.util.DateUtils
import com.paracare.tracker.util.Level
import com.paracare.tracker.util.Recommender
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Trousse personnelle : produits, calcul automatique de la PAO, recommandations IA. */
class TrousseActivity : BaseActivity() {

    private lateinit var b: ActivityTrousseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityTrousseBinding.inflate(layoutInflater)
        setContentView(b.root)
        b.btnBack.setOnClickListener { finish() }
        b.fab.setOnClickListener {
            startActivity(Intent(this, AddProductActivity::class.java)
                .putExtra(AddProductActivity.EXTRA_MODE, AddProductActivity.MODE_TROUSSE))
        }

        val itemsAdapter = RowAdapter<UserItem>(R.layout.item_row) { v, it ->
            val dl = it.deadline()
            val lvl = Level.of(dl)
            val c = color(lvl.colorRes)
            val opened = if (it.openedDate > 0) "Ouvert le ${DateUtils.format(it.openedDate)}" else "Non ouvert"
            val pao = if (it.paoMonths > 0) " • PAO ${it.paoMonths} mois" else ""
            val extra = if (dl != null) "${DateUtils.describe(dl)} (échéance ${DateUtils.format(dl)})" else "Pas d'échéance"
            v.row(it.name, "${it.category} • $opened$pao", extra, c)
            v.rowBadge(lvl.label.uppercase(), c)
            v.setOnClickListener { _ -> itemOptions(it) }
        }
        b.rvItems.layoutManager = LinearLayoutManager(this)
        b.rvItems.adapter = itemsAdapter

        val recAdapter = RowAdapter<Recommender.Rec>(R.layout.item_row) { v, r ->
            val p = r.product
            v.row(p.name, r.reason, "${DateUtils.price(p.finalPrice())} • Stock : ${p.quantity}", color(R.color.primary))
            v.rowAction("Acheter") {
                MaterialAlertDialogBuilder(this)
                    .setMessage("Commander 1 × ${p.name} pour ${DateUtils.price(p.finalPrice())} ?")
                    .setPositiveButton("Commander") { _, _ -> placeOrder(p, 1) }
                    .setNegativeButton("Annuler", null)
                    .show()
            }
        }
        b.rvRecs.layoutManager = LinearLayoutManager(this)
        b.rvRecs.adapter = recAdapter

        val uid = session.userId
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(db.userItemDao().itemsFlow(uid), db.productDao().allFlow()) { items, products ->
                    items to products
                }.collect { (items, products) ->
                    val sorted = items.sortedBy { it.deadline() ?: Long.MAX_VALUE }
                    itemsAdapter.submit(sorted)
                    b.tvEmptyItems.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                    val recs = Recommender.recommend(items, products)
                    recAdapter.submit(recs)
                    b.tvEmptyRecs.visibility = if (recs.isEmpty()) View.VISIBLE else View.GONE
                    AlertManager.checkClient(this@TrousseActivity, uid)
                }
            }
        }
    }

    private fun itemOptions(item: UserItem) {
        val canOpen = item.openedDate == 0L && item.paoMonths > 0
        val labels = mutableListOf<String>()
        if (canOpen) labels.add("Marquer comme ouvert aujourd'hui")
        labels.add("Supprimer de ma trousse")
        MaterialAlertDialogBuilder(this)
            .setTitle(item.name)
            .setItems(labels.toTypedArray()) { _, i ->
                lifecycleScope.launch {
                    if (canOpen && i == 0) {
                        db.userItemDao().update(item.copy(openedDate = System.currentTimeMillis(), alertLevel = 0))
                        toast("PAO calculée : ${item.paoMonths} mois à partir d'aujourd'hui")
                    } else {
                        db.userItemDao().delete(item)
                    }
                }
            }.show()
    }
}
