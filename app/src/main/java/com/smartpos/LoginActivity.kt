package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.content.Intent
import android.content.SharedPreferences

class LoginActivity : Activity() {
    lateinit var pref: SharedPreferences
    var totalSales = 0.0
    var salesLog = ""
    var products = mutableMapOf<String, Product>()
    var cart = mutableListOf<CartItem>()

    data class Product(var code: String, var name: String, var qty: Int, var price: Double, var minPrice: Double)
    data class CartItem(var code: String, var name: String, var qty: Int, var price: Double) {
        fun getTotal(): Double { return qty * price }
    }

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
            val parts = saved.split(";;")
            for (item in parts) {
                val p = item.split("|")
                if (p.size == 5) {
                    val qty = p[2].toIntOrNull()?: 0
                    val price = p[3].toDoubleOrNull()?: 0.0
                    val minP = p[4].toDoubleOrNull()?: 0.0
                    products[p[0]] = Product(p[0], p[1], qty, price, minP)
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
        val t = TextView(this).apply { text = "SMART POS v4.1\nBIG SHOPS RECEIVING\nSHOP001-HRE"; textSize = 18f; gravity = Gravity.CENTER; setPadding(0,0,0,30) }
        val pin = EditText(this).apply { hint = "PIN 1234"; inputType = 129 }
        val btn = Button(this).apply { text = "LOGIN" }
        btn.setOnClickListener {
            if(pin.text.toString()=="1234") showDashboard()
            else Toast.makeText(this,"PIN 1234",Toast.LENGTH_SHORT).show()
        }
        lay.addView(t); lay.addView(pin); lay.addView(btn)
        setContentView(lay)
    }

    fun showDashboard() {
        val scroll = ScrollView(this)
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20,40,20,20) }

        val title = TextView(this).apply { text = "SMART POS v4.1 \$12/mo\nSHOP001-HRE | AUDIT ON"; textSize = 16f; gravity = Gravity.CENTER; setPadding(0,0,0,15) }

        // 1. RECEIVING
        val lblRec = TextView(this).apply { text = "=== PRODUCT RECEIVING MENU ==="; textSize = 16f; setPadding(0,10,0,5) }
        val eName = EditText(this).apply { hint = "Product name............" }
        val eCode = EditText(this).apply { hint = "Product code............" }
        val eSellPrice = EditText(this).apply { hint = "Selling price...."; inputType = 8194 }
        val eMinPrice = EditText(this).apply { hint = "Code (Min Price)........"; inputType = 8194 }
        val eQty = EditText(this).apply { hint = "Quantity........"; inputType = 2 }
        val btnReceive = Button(this).apply { text = "RECEIVE STOCK" }
        val tvStock = TextView(this).apply { text = getStockDisplay(); setPadding(0,10,0,10); textSize = 13f }

        // 2. CART
        val lblCart = TextView(this).apply { text = "=== CART RECEIPT ==="; textSize = 16f; setPadding(0,20,0,5) }
        val eCartCode = EditText(this).apply { hint = "Scan / Enter Product Code" }
        val eCartQty = EditText(this).apply { hint = "Qty (default 1)"; inputType = 2 }
        val eCartPrice = EditText(this).apply { hint = "Price (blank = default)"; inputType = 8194 }
        val btnAddCart = Button(this).apply { text = "ADD TO CART" }

        val tvCart = TextView(this).apply { text = "Cart:\nProduct Qty Price Total\n----------------\n(empty)"; setPadding(0,10,0,10); textSize = 13f }
        val tvTotal = TextView(this).apply { text = "Total.... \$0.00"; textSize = 18f; setPadding(0,10,0,5) }
        val eTender = EditText(this).apply { hint = "Tender..... (cash given)"; inputType = 8194 }
        val tvChange = TextView(this).apply { text = "Change.... \$0.00"; textSize = 18f; setPadding(0,5,0,10) }
        val btnTender = Button(this).apply { text = "CALCULATE CHANGE" }
        val btnPay = Button(this).apply { text = "PAY & PRINT RECEIPT" }
        val btnClearCart = Button(this).apply { text = "CLEAR CART" }

        btnReceive.setOnClickListener {
            if(eName.text.isEmpty() || eCode.text.isEmpty() || eSellPrice.text.isEmpty() || eQty.text.isEmpty()){
                Toast.makeText(this,"Fill name, code, price, qty",Toast.LENGTH_SHORT).show(); return@setOnClickListener
            }
            try {
                val code = eCode.text.toString().trim().uppercase()
                val minP = if(eMinPrice.text.isEmpty()) eSellPrice.text.toString().toDouble() else eMinPrice.text.toString().toDouble()
                val price = eSellPrice.text.toString().toDouble()
                if(price < minP){ Toast.makeText(this,"Selling price cannot be < MIN!",Toast.LENGTH_LONG).show(); return@setOnClickListener }
                val qty = eQty.text.toString().toInt()
                val existing = products[code]
                if(existing!= null){
                    existing.qty = existing.qty + qty
                    existing.name = eName.text.toString()
                    existing.price = price
                    existing.minPrice = minP
                } else {
                    products[code] = Product(code, eName.text.toString(), qty, price, minP)
                }
                saveAll()
                tvStock.text = getStockDisplay()
                Toast.makeText(this,"Received $qty x ${eName.text}",Toast.LENGTH_SHORT).show()
                eName.text.clear(); eCode.text.clear(); eSellPrice.text.clear(); eMinPrice.text.clear(); eQty.text.clear()
            } catch(ex: Exception){ Toast.makeText(this,"Check numbers",Toast.LENGTH_SHORT).show() }
        }

        fun getCartTotal(): Double {
            var sum = 0.0
            for(c in cart){ sum = sum + c.getTotal() }
            return sum
        }

        fun refreshCart(){
            if(cart.isEmpty()){
                tvCart.text = "Cart:\nProduct Qty Price Total\n----------------\n(empty)"
                tvTotal.text = "Total.... \$0.00"
                return
            }
            var txt = "Cart:\nProduct Qty Price Total\n----------------\n"
            var sum = 0.0
            for(it in cart){
                txt += it.name + " " + it.qty + " \$" + it.price + " \$" + String.format("%.2f",it.getTotal()) + "\n"
                sum = sum + it.getTotal()
            }
            tvCart.text = txt
            tvTotal.text = "Total.... \$" + String.format("%.2f",sum)
        }

        btnAddCart.setOnClickListener {
            val code = eCartCode.text.toString().trim().uppercase()
            if(code.isEmpty()){ Toast.makeText(this,"Enter Product Code",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val prod = products[code]
            if(prod==null){ Toast.makeText(this,"Code $code not found!",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val qty = if(eCartQty.text.isEmpty()) 1 else eCartQty.text.toString().toIntOrNull()?:1
            if(qty > prod.qty){ Toast.makeText(this,"Not enough stock! Only ${prod.qty}",Toast.LENGTH_LONG).show(); return@setOnClickListener }
            val sellPrice = if(eCartPrice.text.isEmpty()) prod.price else eCartPrice.text.toString().toDoubleOrNull()?:prod.price
            if(sellPrice < prod.minPrice){ Toast.makeText(this,"DENIED! \$"+sellPrice+" < MIN \$"+prod.minPrice,Toast.LENGTH_LONG).show(); return@setOnClickListener }
            cart.add(CartItem(code, prod.name, qty, sellPrice))
            refreshCart()
            eCartCode.text.clear(); eCartQty.text.clear(); eCartPrice.text.clear()
        }

        btnTender.setOnClickListener {
            val sum = getCartTotal()
            val tender = eTender.text.toString().toDoubleOrNull()?:0.0
            val change = tender - sum
            tvChange.text = "Change.... \$" + String.format("%.2f",change)
        }

        btnPay.setOnClickListener {
            if(cart.isEmpty()){ Toast.makeText(this,"Cart empty",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val sum = getCartTotal()
            val tender = eTender.text.toString().toDoubleOrNull()?:0.0
            if(tender < sum && tender!= 0.0){ Toast.makeText(this,"Tender less than Total!",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            for(item in cart){ val p = products[item.code]; if(p!=null){ p.qty = p.qty - item.qty } }
            val change = tender - sum
            totalSales = totalSales + sum
            var receipt = "SMART POS v4.1\nSHOP001-HRE\n----------------\nProduct Qty Price Total\n"
            for(it in cart){ receipt += it.name + " " + it.qty + " \$" + it.price + " \$" + String.format("%.2f",it.getTotal()) + "\n" }
            receipt += "----------------\nTotal.... \$" + String
