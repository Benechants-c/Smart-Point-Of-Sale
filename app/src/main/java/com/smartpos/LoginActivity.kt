package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.content.Intent
import android.content.SharedPreferences

class LoginActivity : Activity() {
    lateinit var pref: SharedPreferences
    var total = 0.0
    var salesText = ""
    // Product format: CODE|NAME|QTY|PRICE|MIN_PRICE
    var products = mutableMapOf<String, Product>() // key = CODE

    data class Product(var code: String, var name: String, var qty: Int, var price: Double, var minPrice: Double)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("pos_big", 0)
        loadAll()
        showLogin()
    }

    fun loadAll() {
        total = pref.getFloat("total", 0f).toDouble()
        salesText = pref.getString("sales", "")?: ""
        val saved = pref.getString("products", "")?: ""
        if (saved.isNotEmpty()) {
            saved.split(";;").forEach {
                val p = it.split("|")
                if (p.size == 5) products[p[0]] = Product(p[0], p[1], p[2].toIntOrNull()?:0, p[3].toDoubleOrNull()?:0.0, p[4].toDoubleOrNull()?:0.0)
            }
        }
    }

    fun saveAll() {
        val prodStr = products.values.joinToString(";;") { "${it.code}|${it.name}|${it.qty}|${it.price}|${it.minPrice}" }
        pref.edit().putString("products", prodStr).putString("sales", salesText).putFloat("total", total.toFloat()).apply()
    }

    fun showLogin() {
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(60,200,60,60); gravity = Gravity.CENTER }
        val t = TextView(this).apply { text = "SMART POS v3\nBIG SHOPS EDITION\nSHOP001-HRE\n\$12/mo COMMERCIAL"; textSize = 18f; gravity = Gravity.CENTER; setPadding(0,0,0,30) }
        val pin = EditText(this).apply { hint = "PIN 1234"; inputType = 129 }
        val btn = Button(this).apply { text = "LOGIN ADMIN" }
        btn.setOnClickListener {
            if (pin.text.toString() == "1234") { showDashboard() }
            else Toast.makeText(this, "PIN 1234", Toast.LENGTH_SHORT).show()
        }
        lay.addView(t); lay.addView(pin); lay.addView(btn)
        setContentView(lay)
    }

    fun showDashboard() {
        val scroll = ScrollView(this)
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(25,50,25,25) }

        val title = TextView(this).apply { text = "BIG SHOP POS v3\nSHOP001-HRE | AUDIT ON ✅\nProducts: ${products.size} | TOTAL: \$${String.format("%.2f", total)}"; textSize = 15f; gravity = Gravity.CENTER; setPadding(0,0,0,15) }

        // --- CREATE PRODUCT ---
        val lblCreate = TextView(this).apply { text = "--- CREATE PRODUCT ---"; textSize = 16f; setPadding(0,10,0,5) }
        val eCode = EditText(this).apply { hint = "Product Code (e.g. 001 or BREAD01)" }
        val eName = EditText(this).apply { hint = "Product Name (e.g. Mazoe Orange 2L)" }
        val eQty = EditText(this).apply { hint = "Quantity (e.g. 50)"; inputType = 2 }
        val ePrice = EditText(this).apply { hint = "Selling Price (e.g. 3.50)"; inputType = 8194 }
        val eMinPrice = EditText(this).apply { hint = "MIN Price (DENY if lower, e.g. 3.00)"; inputType = 8194 }
        val btnCreate = Button(this).apply { text = "CREATE / UPDATE PRODUCT" }

        // --- SELL ---
        val lblSell = TextView(this).apply { text = "--- SELL PRODUCT ---"; textSize = 16f; setPadding(0,20,0,5) }
        val eSellCode = EditText(this).apply { hint = "Enter CODE to sell (e.g. 001)" }
        val eSellPrice = EditText(this).apply { hint = "Price customer pays"; inputType = 8194 }
        val btnSell = Button(this).apply { text = "SELL - CHECK MIN PRICE" }

        val tvProducts = TextView(this).apply { text = getProductDisplay(); setPadding(0,15,0,10) }
        val tvLog = TextView(this).apply { text = "Sales:\n$salesText"; setPadding(0,10,0,10) }
        val tvTotal = TextView(this).apply { text = "TOTAL: \$${String.format("%.2f", total)}"; textSize = 20f; setPadding(0,10,0,10) }

        val btnReceipt = Button(this).apply { text = "SHARE RECEIPT" }
        val btnClear = Button(this).apply { text = "CLEAR DAY" }
        val btnLogout = Button(this).apply { text = "LOGOUT" }

        btnCreate.setOnClickListener {
            if (eCode.text.isEmpty() || eName.text.isEmpty() || eQty.text.isEmpty() || ePrice.text.isEmpty() || eMinPrice.text.isEmpty()) {
                Toast.makeText(this, "Fill ALL fields", Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            try {
                val code = eCode.text.toString().trim().uppercase()
                val prod = Product(code, eName.text.toString(), eQty.text.toString().toInt(), ePrice.text.toString().toDouble(), eMinPrice.text.toString().toDouble())
                if (prod.minPrice > prod.price) { Toast.makeText(this, "MIN Price cannot be > Selling Price!", Toast.LENGTH_LONG).show(); return@setOnClickListener }
                products[code] = prod
                saveAll()
                tvProducts.text = getProductDisplay()
                title.text = "BIG SHOP POS v3\nSHOP001-HRE | AUDIT ON ✅\nProducts: ${products.size} | TOTAL: \$${String.format("%.2f", total)}"
                Toast.makeText(this, "Product $code Saved ✅", Toast.LENGTH_SHORT).show()
                eCode.text.clear(); eName.text.clear(); eQty.text.clear(); ePrice.text.clear(); eMinPrice.text.clear()
            } catch (e: Exception) { Toast.makeText(this, "Check numbers", Toast.LENGTH_SHORT).show() }
        }

        btnSell.setOnClickListener {
            val code = eSellCode.text.toString().trim().uppercase()
            if (code.isEmpty() || eSellPrice.text.isEmpty()) { Toast.makeText(this, "Enter Code & Price", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val prod = products[code]
            if (prod == null) { Toast.makeText(this, "Product CODE not found!", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if (prod.qty <= 0) { Toast.makeText(this, "${prod.name} OUT OF STOCK!", Toast.LENGTH_LONG).show(); return@setOnClickListener }
            try {
                val payPrice = eSellPrice.text.toString().toDouble()
                // *** MIN PRICE CHECK - DENY SALE ***
                if (payPrice < prod.minPrice) {
                    Toast.makeText(this, "❌ DENIED! Price \$${payPrice} < MIN \$${prod.minPrice} for ${prod.name}", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                if (payPrice < prod.price) {
                    Toast.makeText(this, "⚠️ Warning: Selling below Price but above MIN", Toast.LENGTH_SHORT).show()
                }
                // Sell OK
                prod.qty -= 1
                total += payPrice
                salesText += "${prod.code} ${prod.name} - \$${payPrice} (Stock ${prod.qty})\n"
                saveAll()
                tvProducts.text = getProductDisplay()
                tvLog.text = "Sales:\n$salesText"
                tvTotal.text = "TOTAL: \$${String.format("%.2f", total)}"
                title.text = "BIG SHOP POS v3\nSHOP001-HRE | AUDIT ON ✅\nProducts: ${products.size} | TOTAL: \$${String.format("%.2f", total)}"
                Toast.makeText(this, "Sold ${prod.name} ✅ Stock ${prod.qty}", Toast.LENGTH_SHORT).show()
                eSellCode.text.clear(); eSellPrice.text.clear()
            } catch (e: Exception) { Toast.makeText(this, "Invalid price", Toast.LENGTH_SHORT).show() }
        }

        btnReceipt.setOnClickListener {
            val receipt = "SMART POS v3 BIG SHOPS \$12/mo\nSHOP001-HRE AUDIT ON\n\n$salesText\nTOTAL: \$${String.format("%.2f", total)}\nProducts:\n${getProductDisplay()}"
            val i = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, receipt); type = "text/plain" }
            startActivity(Intent.createChooser(i, "Receipt"))
        }

        btnClear.setOnClickListener { salesText = ""; total = 0.0; saveAll(); tvLog.text = "Sales:\n"; tvTotal.text = "TOTAL: \$0.00"; title.text = "BIG SHOP POS v3\nSHOP001-HRE | AUDIT ON ✅\nProducts: ${products.size} | TOTAL: \$0.00"; Toast.makeText(this, "Day cleared", Toast.LENGTH_SHORT).show() }
        btnLogout.setOnClickListener { showLogin() }

        lay.addView(title)
        lay.addView(lblCreate); lay.addView(eCode); lay.addView(eName); lay.addView(eQty); lay.addView(ePrice); lay.addView(eMinPrice); lay.addView(btnCreate)
        lay.addView(lblSell); lay.addView(eSellCode); lay.addView(eSellPrice); lay.addView(btnSell)
        lay.addView(tvTotal); lay.addView(tvProducts); lay.addView(tvLog); lay.addView(btnReceipt); lay.addView(btnClear); lay.addView(btnLogout)
        scroll.addView(lay)
        setContentView(scroll)
    }

    fun getProductDisplay(): String {
        if (products.isEmpty()) return "No Products yet - Create above"
        return "STOCK (Code | Name | Qty | Price | MIN):\n" + products.values.joinToString("\n") { "${it.code} | ${it.name} | Qty:${it.qty} | \$${it.price} | MIN:\$${it.minPrice}" }
    }
}
