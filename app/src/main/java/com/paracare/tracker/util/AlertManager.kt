package com.paracare.tracker.util

import android.content.Context
import com.paracare.tracker.data.AppDatabase
import com.paracare.tracker.data.Order
import com.paracare.tracker.data.User

/** Vérifie les alertes (péremption/PAO, stock critique, commandes) et envoie les notifications locales. */
object AlertManager {

    /** Client : produit de la trousse bientôt à échéance (<= 7 j) ou périmé. */
    suspend fun checkClient(ctx: Context, userId: Long) {
        val dao = AppDatabase.get(ctx).userItemDao()
        for (item in dao.getForUser(userId)) {
            val dl = item.deadline() ?: continue
            val days = DateUtils.daysLeft(dl)
            val level = when {
                days < 0 -> 2
                days <= 7 -> 1
                else -> 0
            }
            if (level > item.alertLevel) {
                val text = if (level == 2) "« ${item.name} » est arrivé à sa date limite d'utilisation. Ne l'utilisez plus."
                else "« ${item.name} » arrive à sa date limite d'utilisation : ${DateUtils.describe(dl).lowercase()}."
                NotificationHelper.notify(
                    ctx, NotificationHelper.CH_EXPIRY, 10_000 + item.id.toInt(),
                    if (level == 2) "Produit périmé" else "Péremption proche", text
                )
                dao.update(item.copy(alertLevel = level))
            }
        }
    }

    /** Pharmacien : lot ayant atteint le seuil rouge (périmé). */
    suspend fun checkPharmacist(ctx: Context) {
        val dao = AppDatabase.get(ctx).productDao()
        val now = System.currentTimeMillis()
        for (p in dao.getAll()) {
            if (p.expiryDate < now && !p.alertSent) {
                NotificationHelper.notify(
                    ctx, NotificationHelper.CH_STOCK, 20_000 + p.id.toInt(),
                    "Stock critique 🔴",
                    "Le lot « ${p.name} » (${p.quantity} unité(s)) a atteint le seuil d'expiration rouge."
                )
                dao.update(p.copy(alertSent = true))
            }
        }
    }

    /** Client : notification dès que sa commande est validée (PREPAREE). */
    suspend fun checkOrders(ctx: Context, clientId: Long) {
        val dao = AppDatabase.get(ctx).orderDao()
        for (o in dao.forClient(clientId)) {
            if (o.status == Order.PREPAREE && !o.clientNotified) {
                NotificationHelper.notify(
                    ctx, NotificationHelper.CH_ORDERS, 30_000 + o.id.toInt(),
                    "Commande prête ✅",
                    "Votre commande #${o.id} (${o.productName} ×${o.quantity}) est préparée."
                )
                dao.update(o.copy(clientNotified = true))
            }
        }
    }

    /** Appelé par le Worker en arrière-plan selon l'utilisateur connecté. */
    suspend fun runForSession(ctx: Context) {
        val s = SessionManager(ctx)
        if (!s.isLoggedIn) return
        if (s.role == User.ROLE_PHARMACIEN) {
            checkPharmacist(ctx)
        } else {
            checkClient(ctx, s.userId)
            checkOrders(ctx, s.userId)
        }
    }
}
