package com.paracare.tracker.ui

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.paracare.tracker.data.Product
import com.paracare.tracker.data.UserItem
import com.paracare.tracker.databinding.ActivityAddProductBinding
import com.paracare.tracker.util.AlertManager
import com.paracare.tracker.util.Categories
import com.paracare.tracker.util.DateUtils
import com.paracare.tracker.util.ProductScannerHelper
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

/**
 * Saisie + Scan IA d'un produit.
 *  - mode STOCK (pharmacien) : ajoute au stock
 *  - mode TROUSSE (client)   : ajoute à la trousse personnelle
 */
class AddProductActivity : BaseActivity() {

    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_STOCK = "STOCK"
        const val MODE_TROUSSE = "TROUSSE"
    }

    private lateinit var b: ActivityAddProductBinding
    private lateinit var photoUri: Uri
    private val scanner by lazy { ProductScannerHelper(this) }
    private var trousse = false
    private var expiryMs = 0L
    private var openedMs = 0L

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) processImage(photoUri)
    }
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) processImage(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(b.root)

        trousse = intent.getStringExtra(EXTRA_MODE) == MODE_TROUSSE
        b.tvTitle.text = if (trousse) "Ajouter à ma trousse" else "Ajouter un produit"
        b.groupStock.visibility = if (trousse) View.GONE else View.VISIBLE
        b.groupTrousse.visibility = if (trousse) View.VISIBLE else View.GONE
        b.btnBack.setOnClickListener { finish() }

        b.spCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, Categories.ALL)

        val dir = File(cacheDir, "images").apply { mkdirs() }
        photoUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", File(dir, "scan.jpg"))

        b.etExpiry.setOnClickListener {
            pickDate(false) { expiryMs = it; b.etExpiry.setText(DateUtils.format(it)) }
        }
        b.etOpened.setOnClickListener {
            pickDate(true) { openedMs = it; b.etOpened.setText(DateUtils.format(it)) }
        }
        b.btnScanCamera.setOnClickListener {
            try { takePicture.launch(photoUri) } catch (e: Exception) { toast("Caméra indisponible : utilisez la galerie") }
        }
        b.btnScanGallery.setOnClickListener { pickImage.launch("image/*") }
        b.btnSave.setOnClickListener { save() }
    }

    private fun pickDate(maxToday: Boolean, onPicked: (Long) -> Unit) {
        val c = Calendar.getInstance()
        val dlg = DatePickerDialog(this, { _, y, m, d -> onPicked(DateUtils.endOfDay(y, m, d)) },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
        if (maxToday) dlg.datePicker.maxDate = System.currentTimeMillis()
        dlg.show()
    }

    private fun processImage(uri: Uri) {
        b.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val r = scanner.scan(uri)
            b.progress.visibility = View.GONE
            if (r.rawText.isBlank()) {
                toast("Aucun texte détecté — essayez une photo plus nette")
                return@launch
            }
            r.name?.let { b.etName.setText(it) }
            r.category?.let { val i = Categories.ALL.indexOf(it); if (i >= 0) b.spCategory.setSelection(i) }
            r.expiry?.let { expiryMs = it; b.etExpiry.setText(DateUtils.format(it)) }
            r.paoMonths?.let { b.etPao.setText(it.toString()) }
            b.tvScanInfo.visibility = View.VISIBLE
            b.tvScanInfo.text = "Scan IA terminé : nom, catégorie, expiration et PAO remplis quand ils ont été détectés. Vérifiez les champs avant d'enregistrer."
        }
    }

    private fun save() {
        b.tilName.error = null; b.tilPrice.error = null; b.tilQty.error = null
        val name = b.etName.text.toString().trim()
        val cat = b.spCategory.selectedItem as String
        val pao = b.etPao.text.toString().toIntOrNull() ?: 0
        if (name.isEmpty()) { b.tilName.error = "Nom requis"; return }

        lifecycleScope.launch {
            if (trousse) {
                if (expiryMs == 0L && !(openedMs > 0 && pao > 0)) {
                    toast("Indiquez une date d'expiration, ou une date d'ouverture avec PAO")
                    return@launch
                }
                db.userItemDao().insert(
                    UserItem(userId = session.userId, name = name, category = cat,
                        expiryDate = expiryMs, openedDate = openedMs, paoMonths = pao)
                )
                AlertManager.checkClient(this@AddProductActivity, session.userId)
            } else {
                val price = b.etPrice.text.toString().replace(',', '.').toDoubleOrNull()
                val qty = b.etQty.text.toString().toIntOrNull()
                if (expiryMs == 0L) { toast("Date d'expiration requise"); return@launch }
                if (price == null || price < 0) { b.tilPrice.error = "Prix invalide"; return@launch }
                if (qty == null || qty < 0) { b.tilQty.error = "Quantité invalide"; return@launch }
                db.productDao().insert(
                    Product(name = name, category = cat, price = price, quantity = qty,
                        expiryDate = expiryMs, paoMonths = pao)
                )
                AlertManager.checkPharmacist(this@AddProductActivity)
            }
            toast("Enregistré ✓")
            finish()
        }
    }
}
