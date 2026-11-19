package com.paracare.tracker.data

import androidx.room.withTransaction

object OrderService {

    /** Crée une commande EN_ATTENTE et réserve le stock. Retourne un message d'erreur ou null. */
    suspend fun place(db: AppDatabase, clientId: Long, clientName: String, productId: Long, qty: Int): String? =
        db.withTransaction<String?> {
            val p = db.productDao().findById(productId)
            if (p == null) {
                "Produit introuvable"
            } else if (p.quantity < qty) {
                "Stock insuffisant (${p.quantity} disponible(s))"
            } else if (p.expiryDate < System.currentTimeMillis()) {
                "Ce produit est périmé"
            } else {
                db.productDao().update(p.copy(quantity = p.quantity - qty))
                db.orderDao().insert(
                    Order(
                        clientId = clientId, clientName = clientName,
                        productId = p.id, productName = p.name,
                        quantity = qty, unitPrice = p.finalPrice()
                    )
                )
                null
            }
        }

    /** Le pharmacien valide : EN_ATTENTE -> PREPAREE, et le produit rejoint la trousse du client. */
    suspend fun validate(db: AppDatabase, orderId: Long) {
        db.withTransaction<Unit> {
            val o = db.orderDao().findById(orderId)
            if (o != null && o.status != Order.PREPAREE) {
                db.orderDao().update(o.copy(status = Order.PREPAREE, clientNotified = false))
                val p = db.productDao().findById(o.productId)
                if (p != null) {
                    db.userItemDao().insert(
                        UserItem(
                            userId = o.clientId, name = o.productName, category = p.category,
                            expiryDate = p.expiryDate, paoMonths = p.paoMonths
                        )
                    )
                }
            }
        }
    }
}
