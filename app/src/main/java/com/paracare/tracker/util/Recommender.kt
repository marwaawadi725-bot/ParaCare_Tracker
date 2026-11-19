package com.paracare.tracker.util

import com.paracare.tracker.data.Product
import com.paracare.tracker.data.UserItem

/** Moteur de recommandations : analyse la trousse et suggère des soins complémentaires. */
object Recommender {

    data class Rec(val product: Product, val reason: String)

    private val complements = mapOf(
        Categories.VISAGE to listOf(Categories.SOLAIRE, Categories.HYDRATATION),
        Categories.HYDRATATION to listOf(Categories.SOLAIRE),
        Categories.SOLAIRE to listOf(Categories.HYDRATATION, Categories.VISAGE),
        Categories.HYGIENE to listOf(Categories.VISAGE),
        Categories.CAPILLAIRE to listOf(Categories.HYDRATATION),
        Categories.COMPLEMENTS to listOf(Categories.HYDRATATION),
        Categories.MEDICAMENT to listOf(Categories.COMPLEMENTS),
        Categories.AUTRE to listOf(Categories.VISAGE)
    )

    fun recommend(items: List<UserItem>, catalog: List<Product>): List<Rec> {
        if (items.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val available = catalog.filter { it.quantity > 0 && it.expiryDate >= now }
        val owned = items.map { it.name.lowercase() }.toSet()
        val recs = LinkedHashMap<Long, Rec>()

        // 1) Renouvellement : produit de la trousse proche de la péremption / périmé
        for (item in items) {
            val dl = item.deadline() ?: continue
            if (DateUtils.daysLeft(dl) <= Level.ORANGE_DAYS) {
                val alt = available.firstOrNull { it.category == item.category && it.name.lowercase() !in owned }
                if (alt != null) recs.putIfAbsent(alt.id, Rec(alt, "Remplace « ${item.name} » bientôt périmé"))
            }
        }

        // 2) Complémentarité (ex : sérum hydratant -> protection solaire)
        for (item in items) {
            val cats = (complements[item.category] ?: continue).toMutableList()
            if (item.name.contains("sérum", true) || item.name.contains("serum", true)) {
                cats.remove(Categories.SOLAIRE); cats.add(0, Categories.SOLAIRE)
            }
            for (cat in cats) {
                if (items.any { it.category == cat }) continue
                val p = available.firstOrNull { it.category == cat && it.name.lowercase() !in owned } ?: continue
                recs.putIfAbsent(p.id, Rec(p, "Complète votre « ${item.name} » : ${cat.lowercase()}"))
            }
        }
        return recs.values.take(5).toList()
    }
}
