package com.paracare.tracker.data

import com.paracare.tracker.util.Categories
import com.paracare.tracker.util.DateUtils

/** Quelques produits de démonstration (vert / orange / rouge) au premier lancement. */
object DemoData {
    suspend fun seedIfEmpty(db: AppDatabase) {
        if (db.productDao().count() > 0) return
        val now = System.currentTimeMillis()
        fun d(days: Int) = DateUtils.endOfDay(now + days * DateUtils.DAY)
        db.productDao().insertAll(
            listOf(
                Product(name = "Sérum hydratant acide hyaluronique", category = Categories.VISAGE, price = 18.90, quantity = 12, expiryDate = d(400), paoMonths = 6),
                Product(name = "Crème solaire visage SPF50+", category = Categories.SOLAIRE, price = 14.50, quantity = 20, expiryDate = d(25), paoMonths = 12),
                Product(name = "Gel nettoyant doux", category = Categories.HYGIENE, price = 7.20, quantity = 30, expiryDate = d(200), paoMonths = 12),
                Product(name = "Lait corporel nourrissant", category = Categories.HYDRATATION, price = 9.90, quantity = 15, expiryDate = d(18), paoMonths = 12),
                Product(name = "Shampooing fortifiant", category = Categories.CAPILLAIRE, price = 8.50, quantity = 25, expiryDate = d(300), paoMonths = 12),
                Product(name = "Vitamine C 1000 mg", category = Categories.COMPLEMENTS, price = 11.00, quantity = 40, expiryDate = d(120)),
                Product(name = "Baume à lèvres SPF", category = Categories.SOLAIRE, price = 4.50, quantity = 50, expiryDate = d(500), paoMonths = 12),
                Product(name = "Crème contour des yeux", category = Categories.VISAGE, price = 22.00, quantity = 8, expiryDate = d(-5), paoMonths = 6),
                Product(name = "Spray nasal eau de mer", category = Categories.MEDICAMENT, price = 6.80, quantity = 10, expiryDate = d(-20))
            )
        )
    }
}
