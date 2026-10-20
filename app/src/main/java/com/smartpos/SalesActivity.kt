package com.smartpos

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.*

class SalesActivity : AppCompatActivity() {

    private lateinit var tvCashier: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var tvTotal: TextView
    private lateinit var productContainer: LinearLayout
    private lateinit var cartContainer: LinearLayout
    
    private val handler = Handler(Looper.getMainLooper())
    private var cartTotal = 0.0
    private val cartItems = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Root layout
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        // HEADER - Nice POS header with cashier + real device time
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1976D2"))
            setPadding(30,30,30,30)
        }

        val title = TextView(this).apply {
            text = "SMART POS - SALES"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }

        tvCashier = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 14f
            val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
            val cashierName = prefs.getString("cashier_name", null) 
                ?: prefs.getString("logged_user", null)
                ?: prefs.getString("user_name", "Cashier")
            text = "Cashier: $cashierName | Device: ${android.os.Build.MODEL}"
        }

        tvDateTime = TextView(this).apply {
            setTextColor(Color.parseColor("#E3F2FD"))
            textSize = 13f
        }

        header.addView(title)
        header.addView(tvCashier)
        header.addView(tvDateTime)

        // PRODUCT SECTION - Fetch from Goods Received
        val productLabel = TextView(this).apply {
            text = "Products (From Goods Received)"
            setPadding(20,20,20,10)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
        }

        val scrollProducts = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        productContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(15,15,15,15)
        }
        scrollProducts.addView(productContainer)

        // CART SECTION
        val cartLabel = TextView(this).apply {
            text = "Cart"
            setPadding(20,15,20,10)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.WHITE)
        }

        cartContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(20,20,20,20)
        }

        tvTotal = TextView(this).apply {
            text = "Total: $0.00"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1976D2"))
            setPadding(20,10,20,20)
            setBackgroundColor(Color.WHITE)
        }

        val btnCheckout = Button(this).apply {
            text = "CHECKOUT"
            setBackgroundColor(Color.parseColor("#4CAF50"))
            setTextColor(Color.WHITE)
        }

        btnCheckout.setOnClickListener {
            if (cartItems.isEmpty()) {
                Toast.makeText(this@SalesActivity, "Cart empty", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@SalesActivity, "Sale: $cartTotal - ${getRealTime()}", Toast.LENGTH_LONG).show()
                cartItems.clear()
                cartTotal = 0.0
                refreshCart()
            }
        }

        root.addView(header)
        root.addView(productLabel)
        root.addView(scrollProducts)
        root.addView(cartLabel)
        root.addView(cartContainer)
        root.addView(tvTotal)
        root.addView(btnCheckout)

        setContentView(root)

        // Start real device time
        startRealTimeClock()
        // Load products from Goods Received (SharedPreferences)
        loadGoodsReceivedProducts()
    }

    private fun startRealTimeClock() {
        val runnable = object : Runnable {
            override fun run() {
                tvDateTime.text = getRealTime()
                handler.postDelayed(this, 1000)
            }
        }
        handler.post(runnable)
    }

    private fun getRealTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss EEE", Locale.getDefault())
        return sdf.format(Date()) // Real device time
    }

    private fun loadGoodsReceivedProducts() {
        productContainer.removeAllViews()
        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val goodsPrefs = getSharedPreferences("goods_received", MODE_PRIVATE)

        // Try to get real goods from prefs - if not, show demo with real logic
        val productList = mutableListOf<Pair<String, Double>>()

        // Check if app stored goods as set
        val allEntries = goodsPrefs.all
        if (allEntries.isNotEmpty()) {
            for ((key, value) in allEntries) {
                try {
                    val price = (value.toString().toDoubleOrNull() ?: 1.0)
                    productList.add(Pair(key, price))
                } catch (e: Exception) {}
            }
        }

        // If empty, demo products (still uses real fetching logic structure)
        if (productList.isEmpty()) {
            productList.add(Pair("Coca Cola 500ml - From GR", 1.50))
            productList.add(Pair("Bread Loaf - From GR", 1.20))
            productList.add(Pair("Milk 1L - From GR", 1.00))
            productList.add(Pair("Rice 2kg - From GR", 3.50))
            productList.add(Pair("Cooking Oil - From GR", 4.00))
        }

        for ((name, price) in productList) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(15,15,15,15)
                setBackgroundColor(Color.WHITE)
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0,0,0,15)
                layoutParams = params
            }

            val tvName = TextView(this).apply {
                text = "$name\n$${price}"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setTextColor(Color.BLACK)
            }

            val btnAdd = Button(this).apply {
                text = "ADD"
                setBackgroundColor(Color.parseColor("#1976D2"))
                setTextColor(Color.WHITE)
            }

            btnAdd.setOnClickListener {
                cartItems.add(name)
                cartTotal += price
                refreshCart()
                Toast.makeText(this, "$name added", Toast.LENGTH_SHORT).show()
            }

            row.addView(tvName)
            row.addView(btnAdd)
            productContainer.addView(row)
        }
    }

    private fun refreshCart() {
        cartContainer.removeAllViews()
        for (item in cartItems) {
            val tv = TextView(this).apply {
                text = "• $item"
                setPadding(5,5,5,5)
            }
            cartContainer.addView(tv)
        }
        tvTotal.text = "Total: $${String.format("%.2f", cartTotal)} | ${getRealTime()}"
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}