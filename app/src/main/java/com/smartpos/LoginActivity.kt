package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.content.SharedPreferences
import android.content.Intent

class LoginActivity : Activity() {
    lateinit var pref: SharedPreferences
    var totalSales = 0.0
    var salesLog = ""
    var products = mutableMapOf<String, Product>()
    var cartList = ""
    var cartTotal = 0.0

    data class Product(var code: String, var name: String, var qty: Int, var price: Double, var minPrice: Double)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("pos_v4", 0)
        loadAll()
        showLogin()
    }

    fun loadAll() {
        totalSales = pref.getFloat("total", 0f).toDouble()
        salesLog = pref.getString("sales", "")?: ""
        val saved = pref.getString("products", "")?: ""
        if (saved.isNotEmpty()) {
            for (item in saved.split(";;")) {
                val p = item.split("|")
                if (p.size == 5) {
                    products[p[0]] = Product(p[0], p[1], p[2].toIntOrNull()?:0, p[3].toDoubleOrNull()?:0.0, p[4].toDoubleOrNull()?:0.0)
                }
            }
        }
    }

    fun saveAll() {
        var prodStr = ""
        var first = true
        for (pr in products.values) {
            if (!first) prodStr += ";;"
            prodStr += pr.code + "|" + pr.name + "|" + pr.qty + "|" + pr.price + "|" + pr.minPrice
            first = false
        }
        pref.edit().putString("products", prodStr).putString("sales", salesLog).putFloat("total", totalSales.toFloat()).apply()
    }

    fun showLogin() {
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(60,200,60,60); gravity = Gravity.CENTER }
        val t = TextView(this).apply { text = "SMART POS v4.3\nSHOP001-HRE"; textSize = 18f; gravity = Gravity.CENTER; setPadding(0,0,0,30) }
        val pin = EditText(this).apply { hint = "PIN 1234"; inputType = 129 }
        val btn = Button(this).apply { text = "LOGIN" }
        btn.setOnClickListener { if(pin.text.toString()=="1234") showDashboard() else Toast.makeText(this,"PIN 1234",Toast.LENGTH_SHORT).show() }
        lay.addView(t); lay.addView(pin); lay.addView(btn)
        setContentView(lay)
    }

    fun showDashboard() {
        val scroll = ScrollView(this)
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20,40,20,20) }

        val title = TextView(this).apply { text = "POS v4.3 RECEIVING + CART\nSHOP001-HRE"; textSize = 16f; gravity = Gravity.CENTER; setPadding(0,0,0,15) }

        // === RECEIVING ===
        val eName = EditText(this).apply { hint = "Product name............" }
        val eCode = EditText(this).apply { hint = "Product code.............." }
        val ePrice = EditText(this).apply { hint = "Selling price...."; inputType = 8194 }
        val eCodeMin = EditText(this).apply { hint = "Code (Min Price)........"; inputType = 8194 }
        val eQty = EditText(this).apply { hint = "Quantity........"; inputType = 2 }
        val btnRec = Button(this).apply { text = "RECEIVE" }
        val tvStock = TextView(this).apply { text = getStockDisplay() }

        // === CART ===
        val eScan = EditText(this).apply { hint = "Scan Product Code" }
        val eScanQty = EditText(this).apply { hint = "Qty"; inputType = 2 }
        val btnAdd = Button(this).apply { text = "ADD TO CART" }
        val tvCart = TextView(this).apply { text = "Product Qty Price Total\n----------------\n"; setPadding(0,10,0,5) }
        val tvTotal = TextView(this).apply { text = "Total.... 0.00"; textSize = 18f }
        val eTender = EditText(this).apply { hint = "Tender....."; inputType = 8194 }
        val tvChange = TextView(this).apply { text = "Change...."; textSize = 18f }
        val btnCalc = Button(this).apply { text = "CALC CHANGE" }
        val btnPay = Button(this).apply { text = "PAY" }

        btnRec.setOnClickListener {
            try {
                val code = eCode.text.toString().trim().uppercase()
                if(code.isEmpty()) return@setOnClickListener
                val price = ePrice.text.toString().toDouble()
                val minP = if(eCodeMin.text.isEmpty()) price else eCodeMin.text.toString().toDouble()
                val qty = eQty.text.toString().toInt()
                if(price < minP){ Toast.makeText(this,"Price < MIN",Toast.LENGTH_LONG).show(); return@setOnClickListener }
                val ex = products[code]
                if(ex!=null){ ex.qty = ex.qty + qty; ex.price = price; ex.minPrice = minP; ex.name = eName.text.toString() }
                else { products[code] = Product(code, eName.text.toString(), qty, price, minP) }
                saveAll()
                tvStock.text = getStockDisplay()
                Toast.makeText(this,"Received",Toast.LENGTH_SHORT).show()
            } catch(ex: Exception){ Toast.makeText(this,"Fill correctly",Toast.LENGTH_SHORT).show() }
        }

        btnAdd.setOnClickListener {
            val code = eScan.text.toString().trim().uppercase()
            val prod = products[code]
            if(prod==null){ Toast.makeText(this,"Code not found",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val qty = if(eScanQty.text.isEmpty()) 1 else eScanQty.text.toString().toIntOrNull()?:1
            if(qty > prod.qty){ Toast.makeText(this,"Only "+prod.qty+" left",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            if(prod.price < prod.minPrice){ Toast.makeText(this,"Below MIN - DENIED",Toast.LENGTH_LONG).show(); return@setOnClickListener }
            val totalLine = qty * prod.price
            cartList = cartList + prod.name + " " + qty + " " + prod.price + " " + totalLine + "\n"
            cartTotal = cartTotal + totalLine
            tvCart.text = "Product Qty Price Total\n----------------\n" + cartList
            tvTotal.text = "Total.... " + cartTotal
            prod.qty = prod.qty - qty
            tvStock.text = getStockDisplay()
            saveAll()
        }

        btnCalc.setOnClickListener {
            val tender = eTender.text.toString().toDoubleOrNull()?:0.0
            val change = tender - cartTotal
            tvChange.text = "Change.... " + change
        }

        btnPay.setOnClickListener {
            totalSales = totalSales + cartTotal
            salesLog = salesLog + cartList + "Total " + cartTotal + "\n"
            Toast.makeText(this,"PAID",Toast.LENGTH_SHORT).show()
            cartList = ""
            cartTotal = 0.0
            tvCart.text = "Product Qty Price Total\n----------------\n"
            tvTotal.text = "Total.... 0.00"
            saveAll()
        }

        lay.addView(title)
        lay.addView(eName); lay.addView(eCode); lay.addView(ePrice); lay.addView(eCodeMin); lay.addView(eQty); lay.addView(btnRec); lay.addView(tvStock)
        lay.addView(eScan); lay.addView(eScanQty); lay.addView(btnAdd); lay.addView(tvCart); lay.addView(tvTotal); lay.addView(eTender); lay.addView(tvChange); lay.addView(btnCalc); lay.addView(btnPay)

        scroll.addView(lay)
        setContentView(scroll)
    }

    fun getStockDisplay(): String {
        if(products.isEmpty()) return "STOCK Empty"
        var txt = "STOCK:\n"
        for(pr in products.values){ txt += pr.code + " " + pr.name + " Qty:" + pr.qty + " Price:" + pr.price + " MIN:" + pr.minPrice + "\n" }
        return txt
    }
}
