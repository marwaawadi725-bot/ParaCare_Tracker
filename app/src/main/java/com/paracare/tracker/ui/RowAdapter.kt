package com.paracare.tracker.ui

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.paracare.tracker.R

class RowVH(v: View) : RecyclerView.ViewHolder(v)

/** Adaptateur générique : un layout + une fonction de liaison. */
class RowAdapter<T>(
    private val layout: Int,
    private val bind: (View, T) -> Unit
) : RecyclerView.Adapter<RowVH>() {

    private var items: List<T> = emptyList()

    fun submit(list: List<T>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        RowVH(LayoutInflater.from(parent.context).inflate(layout, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RowVH, position: Int) = bind(holder.itemView, items[position])
}

// ---- Helpers pour item_row.xml ----

/** Remplit une ligne et réinitialise badge / bouton / clic (les vues sont recyclées). */
fun View.row(title: String, sub: String, extra: String, dotColor: Int) {
    findViewById<TextView>(R.id.tvTitle).text = title
    findViewById<TextView>(R.id.tvSub).text = sub
    findViewById<TextView>(R.id.tvExtra).text = extra
    findViewById<View>(R.id.vDot).backgroundTintList = ColorStateList.valueOf(dotColor)
    findViewById<TextView>(R.id.tvBadge).visibility = View.GONE
    findViewById<Button>(R.id.btnAction).visibility = View.GONE
    setOnClickListener(null)
    isClickable = false
}

fun View.rowBadge(text: String, color: Int) {
    val tv = findViewById<TextView>(R.id.tvBadge)
    tv.text = text
    tv.setTextColor(color)
    tv.background = GradientDrawable().apply {
        cornerRadius = 40f
        setColor(ColorUtils.setAlphaComponent(color, 40))
    }
    tv.visibility = View.VISIBLE
}

fun View.rowAction(text: String, onClick: () -> Unit) {
    val b = findViewById<Button>(R.id.btnAction)
    b.text = text
    b.setOnClickListener { onClick() }
    b.visibility = View.VISIBLE
}
