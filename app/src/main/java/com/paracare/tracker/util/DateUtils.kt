package com.paracare.tracker.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    const val DAY = 86_400_000L
    private val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

    fun format(ts: Long): String = fmt.format(Date(ts))

    fun endOfDay(ts: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ts
        c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
        c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    /** month : 0-11 (comme DatePickerDialog) */
    fun endOfDay(year: Int, month: Int, day: Int): Long {
        val c = Calendar.getInstance()
        c.clear(); c.set(year, month, day)
        return endOfDay(c.timeInMillis)
    }

    /** month : 1-12 */
    fun endOfMonth(year: Int, month: Int): Long {
        val c = Calendar.getInstance()
        c.clear(); c.set(year, month - 1, 1)
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
        return endOfDay(c.timeInMillis)
    }

    fun addMonths(ts: Long, months: Int): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = ts
        c.add(Calendar.MONTH, months)
        return endOfDay(c.timeInMillis)
    }

    /** Jours restants (négatif si périmé). */
    fun daysLeft(deadline: Long): Int =
        Math.floorDiv(deadline - System.currentTimeMillis(), DAY).toInt()

    fun describe(deadline: Long?): String {
        if (deadline == null) return "Pas d'échéance"
        val d = daysLeft(deadline)
        return when {
            d < 0 -> "Périmé depuis ${-d} j"
            d == 0 -> "Expire aujourd'hui"
            else -> "Expire dans $d j"
        }
    }

    fun price(v: Double): String = String.format(Locale.FRANCE, "%.2f €", v)
}
