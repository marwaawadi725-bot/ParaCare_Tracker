package com.paracare.tracker.util

import com.paracare.tracker.R

/** Paliers de péremption : vert (valide), orange (proche), rouge (périmé). */
enum class Level(val label: String, val colorRes: Int) {
    GREEN("Valide", R.color.level_green),
    ORANGE("Proche", R.color.level_orange),
    RED("Périmé", R.color.level_red);

    companion object {
        const val ORANGE_DAYS = 30

        fun of(deadline: Long?): Level {
            if (deadline == null) return GREEN
            val d = DateUtils.daysLeft(deadline)
            return when {
                d < 0 -> RED
                d <= ORANGE_DAYS -> ORANGE
                else -> GREEN
            }
        }
    }
}
