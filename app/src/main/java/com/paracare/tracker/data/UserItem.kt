package com.paracare.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paracare.tracker.util.DateUtils

/** Produit présent dans la trousse personnelle d'un client. */
@Entity(tableName = "user_items")
data class UserItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val category: String,
    val expiryDate: Long = 0,   // 0 = inconnue
    val openedDate: Long = 0,   // 0 = non ouvert
    val paoMonths: Int = 0,     // 0 = pas de PAO
    val alertLevel: Int = 0     // 0 aucune, 1 bientôt, 2 périmé (dernière alerte envoyée)
) {
    /** Échéance réelle = la plus proche entre date d'expiration et ouverture + PAO. */
    fun deadline(): Long? {
        val list = mutableListOf<Long>()
        if (expiryDate > 0) list.add(expiryDate)
        if (openedDate > 0 && paoMonths > 0) list.add(DateUtils.addMonths(openedDate, paoMonths))
        return list.minOrNull()
    }
}
