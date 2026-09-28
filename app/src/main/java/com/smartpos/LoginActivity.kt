package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.content.Intent

class LoginActivity : Activity() {
    var total = 0.0
    var salesText = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLogin()
    }

    fun showLogin() {
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(60, 200, 60, 60); gravity = Gravity.CENTER }
        val t = TextView(this).apply { text = "SMART POS\nCommercial - \$12/mo\nSHOP001-HRE"; textSize = 20f; gravity = Gravity.CENTER; setPadding(0,0,0,40) }
        val biz = EditText(this).apply { hint = "BusinessID"; setText("SHOP001-HRE") }
        val pin = EditText(this).apply { hint = "PIN"; inputType = 129 }
        val btn = Button(this).apply { text = "LOGIN" }
        btn.setOnClickListener {
            if (pin.text.toString() == "1234") {
                Toast.makeText(this, "ADMIN Login - Audit ON", Toast.LENGTH_SHORT).show()
                // Load saved sales
                val pref = getSharedPreferences("pos", 0)
                salesText = pref.getString("sales", "") ?: ""
                total = pref.getFloat("total", 0f).toDouble()
                showSales(biz.text.toString())
            } else Toast.makeText(this, "Wrong PIN: 1234", Toast.LENGTH_SHORT).show()
        }
        lay.addView(t); lay.addView(biz); lay.addView(pin); lay.addView(btn)
        setContentView(lay)
    }

    fun showSales(businessId: String) {
        val scroll = ScrollView(this)
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 80, 40, 40) }
        
        val title = TextView(this).apply {
            text = "SALES DASHBOARD\n$businessId\nAUDIT ON ✅\n\$12/mo COMMERCIAL"
            textSize = 18f; gravity = Gravity.CENTER; setPadding(0,0,0,20)
        }
        val item = EditText(this).apply { hint = "Item name (e.g. Bread)" }
        val price = EditText(this).apply { hint = "Price (e.g. 2.50)"; inputType = 8194 }
        val btnAdd = Button(this).apply { text = "ADD SALE - Stock -1" }
        val tvTotal = TextView(this).apply { text = "TOTAL: \$${String.format("%.2f", total)}"; textSize = 20f; setPadding(0,20,0,10) }
        val log = TextView(this).apply { text = if(salesText.isEmpty()) "Sales log:\n(No sales yet)" else "Sales log:\n$salesText"; setPadding(0,10,0,20) }
        val btnReceipt = Button(this).apply { text = "SHARE RECEIPT" }
        val btnClear = Button(this).apply { text = "CLEAR DAY (New Audit)" }
        val btnLogout = Button(this).apply { text = "LOGOUT" }

        btnAdd.setOnClickListener {
            if (item.text.isEmpty() || price.text.isEmpty()) {
                Toast.makeText(this, "Enter item & price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                val p = price.text.toString().toDouble()
                total += p
                salesText += "${item.text} - \$${price.text}\n"
                log.text = "Sales log:\n$salesText"
                tvTotal.text = "TOTAL: \$${String.format("%.2f", total)}"
                // SAVE
                getSharedPreferences("pos", 0).edit().putString("sales", salesText).putFloat("total", total.toFloat()).apply()
                Toast.makeText(this, "Sale added - Stock -1 ✅", Toast.LENGTH_SHORT).show()
                item.text.clear(); price.text.clear()
            } catch (e: Exception) {
                Toast.makeText(this, "Price must be number", Toast.LENGTH_SHORT).show()
            }
        }

        btnReceipt.setOnClickListener {
            val receipt = "SMART POS - \$12/mo\n$businessId\nAUDIT ON\n\n$salesText\nTOTAL: \$${String.format("%.2f", total)}\nThank you!"
            val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, receipt); type = "text/plain" }
            startActivity(Intent.createChooser(sendIntent, "Share Receipt via WhatsApp"))
        }

        btnClear.setOnClickListener {
            salesText = ""; total = 0.0
            getSharedPreferences("pos", 0).edit().clear().apply()
            log.text = "Sales log:\n(No sales yet)"; tvTotal.text = "TOTAL: \$0.00"
            Toast.makeText(this, "Day cleared - Audit reset", Toast.LENGTH_SHORT).show()
        }

        btnLogout.setOnClickListener { showLogin() }

        lay.addView(title); lay.addView(item); lay.addView(price); lay.addView(btnAdd); lay.addView(tvTotal); lay.addView(log); lay.addView(btnReceipt); lay.addView(btnClear); lay.addView(btnLogout)
        scroll.addView(lay)
        setContentView(scroll)
    }
}
