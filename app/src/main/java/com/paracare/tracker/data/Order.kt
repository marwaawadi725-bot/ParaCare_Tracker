package com.paracare.tracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val clientName: String,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val status: String = EN_ATTENTE,
    val createdAt: Long = System.currentTimeMillis(),
    val clientNotified: Boolean = false
) {
    fun total(): Double = unitPrice * quantity

    companion object {
        const val EN_ATTENTE = "EN_ATTENTE"
        const val PREPAREE = "PREPAREE"
    }
}
