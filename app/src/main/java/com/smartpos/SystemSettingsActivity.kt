package com.smartpos

import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.*

class SystemSettingActivity : AppCompatActivity() {

    private lateinit var etShopName: EditText
    private lateinit var etShopAddress: EditText
    private lateinit var etShopPhone: EditText
    private lateinit var etCurrency: EditText
    private lateinit var etTax: EditText
    private lateinit var etReceiptFooter: EditText
    private lateinit var etCashier: EditText
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D47A1"))
            setPadding(30,25,30,25)
        }
        val title = TextView(this).apply {
            text = "SYSTEM SETTINGS"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val tvTime = TextView(this).apply {
            text = "Real Time: ${sdf.format(Date())} | ${android.os.Build.MODEL}"
            setTextColor(Color.parseColor("#BBDEFB"))
            textSize = 11f
        }
        header.addView(title)
        header.addView(tvTime)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20,20,20,20)
        }

        // SHOP INFO
        val shopBox = createSection("🏪 SHOP INFORMATION")
        etShopName = createEdit("Shop Name")
        etShopAddress = createEdit("Shop Address")
        etShopPhone = createEdit("Shop Phone")
        shopBox.addView(etShopName)
        shopBox.addView(etShopAddress)
        shopBox.addView(etShopPhone)

        // CURRENCY & TAX
        val financeBox = createSection("💰 CURRENCY & TAX")
        etCurrency = createEdit("Currency e.g. USD, ZiG, Rand")
        etTax = createEdit("Tax Rate % e.g. 15")
        financeBox.addView(etCurrency)
        financeBox.addView(etTax)

        // RECEIPT
        val receiptBox = createSection("🧾 RECEIPT SETTINGS")
        etReceiptFooter = createEdit("Receipt Footer e.g. Thank you, Come Again")
        receiptBox.addView(etReceiptFooter)

        // USER / CASHIER
        val userBox = createSection("👤 DEFAULT CASHIER")
        etCashier = createEdit("Default Cashier Name")
        userBox.addView(etCashier)

        // PRINTER & LANGUAGE
        val otherBox = createSection("⚙️ OTHER SETTINGS")
        val spinnerPrinter = Spinner(this).apply {
            val options = arrayOf("Internal Printer", "Bluetooth Printer", "No Printer")
            adapter = ArrayAdapter(this@SystemSettingActivity, android.R.layout.simple_spinner_dropdown_item, options)
        }
        val spinnerLang = Spinner(this).apply {
            val langs = arrayOf("English", "Shona", "Ndebele")
            adapter = ArrayAdapter(this@SystemSettingActivity, android.R.layout.simple_spinner_dropdown_item, langs)
        }
        val tvPrinter = TextView(this).apply { text = "Printer Type"; setPadding(0,10,0,5) }
        val tvLang = TextView(this).apply { text = "Language"; setPadding(0,15,0,5) }
        otherBox.addView(tvPrinter)
        otherBox.addView(spinnerPrinter)
        otherBox.addView(tvLang)
        otherBox.addView(spinnerLang)

        tvStatus = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(0,20,0,0)
        }

        val btnSave = Button(this).apply {
            text = "💾 SAVE ALL SETTINGS"
            setBackgroundColor(Color.parseColor("#0D47A1"))
            setTextColor(Color.WHITE)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0,30,0,10)
            layoutParams = lp
        }

        val btnReset = Button(this).apply {
            text = "RESET TO DEFAULT"
            setBackgroundColor(Color.parseColor("#616161"))
            setTextColor(Color.WHITE)
        }

        container.addView(shopBox)
        container.addView(financeBox)
        container.addView(receiptBox)
        container.addView(userBox)
        container.addView(otherBox)
        container.addView(btnSave)
        container.addView(btnReset)
        container.addView(tvStatus)

        scroll.addView(container)
        root.addView(header)
        root.addView(scroll)
        setContentView(root)

        loadSettings()

        btnSave.setOnClickListener {
            saveSettings(
                spinnerPrinter.selectedItem.toString(),
                spinnerLang.selectedItem.toString()
            )
        }
        btnReset.setOnClickListener { resetSettings() }
    }

    private fun createSection(title: String): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(20,20,20,20)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0,0,0,20)
            layoutParams = lp
        }
        val tv = TextView(this).apply {
            text = title
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            setPadding(0,0,0,15)
        }
        box.addView(tv)
        return box
    }

    private fun createEdit(hintText: String): EditText {
        return EditText(this).apply {
            hint = ""
            setBackgroundResource(android.R.drawable.editbox_background)
            setPadding(25,25,25,25)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0,8,0,8)
            layoutParams = lp
        }.also { it.hint = hintText }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        etShopName.setText(prefs.getString("shop_name", ""))
        etShopAddress.setText(prefs.getString("shop_address", ""))
        etShopPhone.setText(prefs.getString("shop_phone", ""))
        etCurrency.setText(prefs.getString("currency", "USD"))
        etTax.setText(prefs.getString("tax_rate", "15"))
        etReceiptFooter.setText(prefs.getString("receipt_footer", "Thank you - Come Again"))
        etCashier.setText(prefs.getString("cashier_name", prefs.getString("logged_user", "")))

        tvStatus.text = "Loaded | Real Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())} | Device: ${android.os.Build.MODEL}"
    }

    private fun saveSettings(printer: String, lang: String) {
        val shopName = etShopName.text.toString().trim()
        val address = etShopAddress.text.toString().trim()
        val phone = etShopPhone.text.toString().trim()
        val currency = etCurrency.text.toString().trim()
        val tax = etTax.text.toString().trim()
        val footer = etReceiptFooter.text.toString().trim()
        val cashier = etCashier.text.toString().trim()

        if (shopName.isEmpty()) {
            etShopName.error = "Enter shop name"
            return
        }

        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("shop_name", shopName)
            .putString("shop_address", address)
            .putString("shop_phone", phone)
            .putString("currency", if (currency.isEmpty()) "USD" else currency)
            .putString("tax_rate", if (tax.isEmpty()) "15" else tax)
            .putString("receipt_footer", footer)
            .putString("cashier_name", cashier)
            .putString("printer_type", printer)
            .putString("language", lang)
            .putString("last_settings_update", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            .apply()

        Toast.makeText(this, "Settings Saved - $shopName - $currency - Tax $tax% - $printer", Toast.LENGTH_LONG).show()
        tvStatus.text = "Saved: $shopName | $currency | Tax $tax% | $printer | $lang | Real Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
    }

    private fun resetSettings() {
        getSharedPreferences("pos_prefs", MODE_PRIVATE).edit()
            .putString("shop_name", "")
            .putString("shop_address", "")
            .putString("shop_phone", "")
            .putString("currency", "USD")
            .putString("tax_rate", "15")
            .putString("receipt_footer", "Thank you")
            .putString("printer_type", "Internal Printer")
            .putString("language", "English")
            .apply()
        loadSettings()
        Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show()
    }
}