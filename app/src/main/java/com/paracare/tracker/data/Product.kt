package com.paracare.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Produit en stock de la parapharmacie (géré par le pharmacien). */
@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val price: Double,
    val quantity: Int,
    val expiryDate: Long,            // fin du jour d'expiration (ms)
    val paoMonths: Int = 0,          // durée d'utilisation après ouverture
    val discountPercent: Int = 0,    // remise promotionnelle
    val destocked: Boolean = false,  // déstockage immédiat
    val alertSent: Boolean = false   // alerte "stock critique" déjà envoyée
) {
    fun finalPrice(): Double = price * (1 - discountPercent / 100.0)
}
