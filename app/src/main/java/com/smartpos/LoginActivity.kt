package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.content.Intent
import android.content.SharedPreferences

class LoginActivity : Activity() {
    var total = 0.0
    var salesText = ""
    lateinit var pref: SharedPreferences
    var stockMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("pos", 0)
        loadStock()
        showLogin()
    }

    fun loadStock() {
        val saved = pref.getString("stockMap", "")?: ""
        if (saved.isNotEmpty()) {
            saved.split(";").forEach {
                val parts = it.split(":")
                if (parts.size == 2) stockMap[parts[0]] = parts[1].toIntOrNull()?: 0
            }
        } else {
            // Starter stock for tuckshop
            stockMap = mutableMapOf("bread" to 20, "milk" to 15, "tea" to 30, "sugar" to 25)
        }
    }

    fun saveStock() {
        val str = stockMap.entries.joinToString(";") { "${it.key}:${it.value}" }
        pref.edit().putString("stockMap", str).putString("sales", salesText).putFloat("total", total.toFloat()).apply()
    }

    fun showLogin() {
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(60, 200, 60, 60); gravity = Gravity.CENTER }
        val t = TextView(this).apply { text = "SMART POS v2\nStock Count - \$12/mo\nSHOP001-HRE"; textSize = 20f; gravity = Gravity.CENTER; setPadding(0,0,0,40) }
        val biz = EditText(this).apply { hint = "BusinessID"; setText("SHOP001-HRE") }
        val pin = EditText(this).apply { hint = "PIN 1234"; inputType = 129 }
        val btn = Button(this).apply { text = "LOGIN" }
        btn.setOnClickListener {
            if (pin.text.toString() == "1234") {
                salesText = pref.getString("sales", "")?: ""
                total = pref.getFloat("total", 0f).toDouble()
                Toast.makeText(this, "ADMIN - Stock Control ON", Toast.LENGTH_SHORT).show()
                showSales(biz.text.toString())
            } else Toast.makeText(this, "PIN 1234", Toast.LENGTH_SHORT).show()
        }
        lay.addView(t); lay.addView(biz); lay.addView(pin); lay.addView(btn)
        setContentView(lay)
    }

    fun showSales(businessId: String) {
        val scroll = ScrollView(this)
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(30, 60, 30, 30) }

        val title = TextView(this).apply { text = "STOCK CONTROL DASHBOARD\n$businessId\nAUDIT ON ✅"; textSize = 16f; gravity = Gravity.CENTER; setPadding(0,0,0,15) }
        val item = EditText(this).apply { hint = "Item: bread/milk/tea/sugar" }
        val price = EditText(this).apply { hint = "Price"; inputType = 8194 }
        val btnAdd = Button(this).apply { text = "SELL 1 - Stock -1" }
        val tvTotal = TextView(this).apply { text = "TOTAL: \$${String.format("%.2f", total)}"; textSize = 20f; setPadding(0,15,0,5) }

        // STOCK VIEW
        val stockView = TextView(this).apply {
            text = getStockDisplay()
            textSize = 14f
            setPadding(0,10,0,10)
        }

        val log = TextView(this).apply { text = if(salesText.isEmpty()) "Sales:\n" else "Sales:\n$salesText"; setPadding(0,10,0,15) }
        val btnReceipt = Button(this).apply { text = "SHARE RECEIPT (WhatsApp)" }
        val btnRestock = Button(this).apply { text = "RESTOCK +10" }
        val btnLogout = Button(this).apply { text = "LOGOUT" }

        btnAdd.setOnClickListener {
            if (item.text.isEmpty() || price.text.isEmpty()) return@setOnClickListener
            val name = item.text.toString().lowercase().trim()
            val qty = stockMap[name]?: 10 // if new item, start with 10

            if (qty <= 0) {
                Toast.makeText(this, "$name OUT OF STOCK! Restock!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            try {
                val p = price.text.toString().toDouble()
                total += p
                salesText += "$name - \$$p (Stock: ${qty-1} left)\n"
                stockMap[name] = qty - 1

                log.text = "Sales:\n$salesText"
                tvTotal.text = "TOTAL: \$${String.format("%.2f", total)}"
                stockView.text = getStockDisplay()
                saveStock()

                if (qty-1 <= 3) Toast.makeText(this, "LOW STOCK: $name = ${qty-1} left!", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this, "Sold 1 $name - Stock -1", Toast.LENGTH_SHORT).show()

                item.text.clear(); price.text.clear()
            } catch (e: Exception) { Toast.makeText(this, "Price must be number", Toast.LENGTH_SHORT).show() }
        }

        btnRestock.setOnClickListener {
            val name = item.text.toString().lowercase().trim()
            if (name.isEmpty()) { Toast.makeText(this, "Enter item name to restock", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            stockMap[name] = (stockMap[name]?: 0) + 10
            stockView.text = getStockDisplay()
            saveStock()
            Toast.makeText(this, "$name restocked +10", Toast.LENGTH_SHORT).show()
        }

        btnReceipt.setOnClickListener {
            val receipt = "SMART POS v2 \$12/mo\n$businessId\nAUDIT ON - Stock Control\n\n$salesText\n${getStockDisplay()}\nTOTAL: \$${String.format("%.2f", total)}\nThank you!"
            val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, receipt); type = "text/plain" }
            startActivity(Intent.createChooser(sendIntent, "Receipt"))
        }

        btnLogout.setOnClickListener { showLogin() }

        lay.addView(title); lay.addView(item); lay.addView(price); lay.addView(btnAdd); lay.addView(tvTotal); lay.addView(stockView); lay.addView(log); lay.addView(btnReceipt); lay.addView(btnRestock); lay.addView(btnLogout)
        scroll.addView(lay)
        setContentView(scroll)
    }

    fun getStockDisplay(): String {
        return "STOCK LEVELS:\n" + stockMap.entries.joinToString("\n") { "${it.key}: ${it.value}" } + "\n"
    }
}
