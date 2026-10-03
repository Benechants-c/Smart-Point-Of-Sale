package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.widget.*

class SalesActivity : Activity() {

    data class CartItem(var name:String, var price:Float, var qty:Int, var code:String="")

    private val cart = mutableListOf<CartItem>()
    private lateinit var cartContainer: LinearLayout
    private lateinit var txtReceiptTotal: TextView
    private lateinit var txtReceiptCount: TextView
    private lateinit var txtChange: TextView
    private lateinit var inputTendered: EditText
    private lateinit var inputQuick: EditText
    private lateinit var txtCashier: TextView
    private lateinit var txtShopTop: TextView
    private lateinit var shopContainer: LinearLayout
    private var selectedShop = "Main Shop"
    private var lastReceiptText = ""
    private var cashierName = "Cashier"
    private var cashierRole = "Cashier"
    private var allProducts = mutableListOf<Triple<String,String,Float>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cashierName = intent.getStringExtra("USER_NAME") ?: intent.getStringExtra("USER") ?: "John"
        cashierRole = intent.getStringExtra("USER_ROLE") ?: "Cashier"
        if(cashierName.trim().isEmpty()) cashierName = "John"
        selectedShop = intent.getStringExtra("SHOP_NAME") ?: "Main Shop"
        getSharedPreferences("pos", Context.MODE_PRIVATE).edit().putString("last_user", cashierName).apply()
        lastReceiptText = getSharedPreferences("pos", Context.MODE_PRIVATE).getString("last_receipt","") ?: ""

        loadAllProducts()

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F1F5F9")) }

        val header = LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#1E293B")); setPadding(24,28,24,18); orientation=LinearLayout.HORIZONTAL }
        header.addView(TextView(this).apply { text="🖥️ SmartPOS"; textSize=18f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        val hRight = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.END }
        txtCashier = TextView(this).apply { text="👤 Cashier: $cashierName ($cashierRole)"; textSize=12f; setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD) }
        txtShopTop = TextView(this).apply { text="📍 Shop: $selectedShop"; textSize=11f; setTextColor(Color.parseColor("#93C5FD")); gravity=Gravity.END }
        hRight.addView(txtCashier); hRight.addView(txtShopTop)
        header.addView(hRight)
        root.addView(header)

        val shopCard = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,12,16,12); layoutParams=Linear
