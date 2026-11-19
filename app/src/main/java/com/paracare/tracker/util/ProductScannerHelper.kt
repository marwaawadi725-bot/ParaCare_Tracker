package com.paracare.tracker.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class ScanResult(
    val rawText: String,
    val name: String? = null,
    val category: String? = null,
    val expiry: Long? = null,
    val paoMonths: Int? = null
)

/** Scan IA (Google ML Kit) : extrait nom, catégorie, date d'expiration et PAO depuis une photo. */
class ProductScannerHelper(private val context: Context) {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun scan(uri: Uri): ScanResult {
        val text = recognize(uri)
        return if (text.isBlank()) ScanResult("") else parse(text)
    }

    private suspend fun recognize(uri: Uri): String = suspendCancellableCoroutine { cont ->
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { if (cont.isActive) cont.resume(it.text) }
                .addOnFailureListener { if (cont.isActive) cont.resume("") }
        } catch (e: Exception) {
            if (cont.isActive) cont.resume("")
        }
    }

    // ------------------------------------------------------------------ parsing

    private val fullDate = Regex("""(\d{1,2})\s*[/.\-]\s*(\d{1,2})\s*[/.\-]\s*(\d{4}|\d{2})""")
    private val monthYear = Regex("""(?<!\d)(0?[1-9]|1[0-2])\s*[/.\-]\s*(\d{4}|\d{2})(?!\d)""")
    private val keywords = listOf("EXP", "BBE", "UTILISER AVANT", "USE BY", "PER:", "PÉR", "A UTILISER", "À UTILISER")

    private fun parse(text: String): ScanResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return ScanResult(
            rawText = text,
            name = guessName(lines),
            category = guessCategory(text),
            expiry = findExpiry(lines),
            paoMonths = findPao(text)
        )
    }

    private fun fixYear(y: Int) = if (y < 100) 2000 + y else y

    private fun parseDate(s: String): Long? {
        fullDate.find(s)?.let { m ->
            val d = m.groupValues[1].toInt(); val mo = m.groupValues[2].toInt(); val y = fixYear(m.groupValues[3].toInt())
            if (d in 1..31 && mo in 1..12 && y in 2000..2100) return DateUtils.endOfDay(y, mo - 1, d)
        }
        monthYear.find(s)?.let { m ->
            val mo = m.groupValues[1].toInt(); val y = fixYear(m.groupValues[2].toInt())
            if (y in 2000..2100) return DateUtils.endOfMonth(y, mo)
        }
        return null
    }

    private fun findExpiry(lines: List<String>): Long? {
        // 1) ligne contenant un mot-clé (EXP, BBE...) puis la suivante
        for ((i, line) in lines.withIndex()) {
            val up = line.uppercase()
            if (keywords.any { up.contains(it) }) {
                parseDate(line)?.let { return it }
                if (i + 1 < lines.size) parseDate(lines[i + 1])?.let { return it }
            }
        }
        // 2) sinon la date la plus lointaine trouvée dans le texte
        return lines.mapNotNull { parseDate(it) }.maxOrNull()
    }

    private fun findPao(text: String): Int? {
        val up = text.uppercase()
        val pats = listOf(
            Regex("""PAO\D{0,6}(\d{1,2})"""),
            Regex("""(\d{1,2})\s*(MOIS|MONTHS)"""),
            Regex("""(?<![\d/.])(\d{1,2})\s*M(?![A-Z])""")
        )
        for (p in pats) {
            val v = p.find(up)?.groupValues?.get(1)?.toIntOrNull()
            if (v != null && v in 1..60) return v
        }
        return null
    }

    private fun guessName(lines: List<String>): String? {
        val bad = listOf("EXP", "LOT", "BBE", "PAO", "WWW", "TEL", "MADE", "FABRIQU", "ML", "FL OZ")
        return lines.firstOrNull { l ->
            val letters = l.count { it.isLetter() }
            letters >= 4 && l.length <= 45 && letters * 2 >= l.length &&
                bad.none { l.uppercase().contains(it) } && parseDate(l) == null
        }?.lowercase()?.replaceFirstChar { it.uppercase() }
    }

    private fun guessCategory(text: String): String? {
        val t = text.lowercase()
        fun has(vararg k: String) = k.any { t.contains(it) }
        return when {
            has("solaire", "spf", "sun ", "uva", "uvb", "indice") -> Categories.SOLAIRE
            has("shampo", "cheveux", "hair", "après-shampoing", "apres-shampoing") -> Categories.CAPILLAIRE
            has("gel douche", "savon", "dentifrice", "déodorant", "deodorant", "hygi") -> Categories.HYGIENE
            has("vitamin", "complément", "complement", "gélule", "gelule", "oméga", "omega", "magnésium") -> Categories.COMPLEMENTS
            has("comprimé", "comprime", "sirop", "paracétamol", "paracetamol", "ibuprof", " mg") -> Categories.MEDICAMENT
            has("hydrat", "moistur", "lait corp", "body", "corps", "baume") -> Categories.HYDRATATION
            has("sérum", "serum", "visage", "face", "anti-âge", "anti-age", "crème", "creme", "cream", "contour") -> Categories.VISAGE
            else -> null
        }
    }
}
