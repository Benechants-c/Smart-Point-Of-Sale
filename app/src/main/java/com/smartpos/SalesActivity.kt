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
    private var allProducts = mutableListOf<Triple<String,String,Float>>() // code, name, price

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) REAL CASHIER NAME - FIX
        cashierName = intent.getStringExtra("USER_NAME") ?: intent.getStringExtra("USER") ?: ""
        cashierRole = intent.getStringExtra("USER_ROLE") ?: "Cashier"
        if(cashierName.isEmpty()){
            cashierName = getSharedPreferences("pos", Context.MODE_PRIVATE).getString("last_user","John") ?: "John"
        }
        selectedShop = intent.getStringExtra("SHOP_NAME") ?: "Main Shop"
        getSharedPreferences("pos", Context.MODE_PRIVATE).edit().putString("last_user", cashierName).apply()
        lastReceiptText = getSharedPreferences("pos", Context.MODE_PRIVATE).getString("last_receipt","") ?: ""

        loadAllProducts()

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F1F5F9")) }

        // HEADER
        val header = LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#1E293B")); setPadding(24,28,24,18); orientation=LinearLayout.HORIZONTAL }
        header.addView(TextView(this).apply { text="🖥️ SmartPOS"; textSize=18f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        val hRight = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.END }
        txtCashier = TextView(this).apply { text="👤 Cashier: $cashierName ($cashierRole)"; textSize=12f; setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD) }
        txtShopTop = TextView(this).apply { text="📍 Shop: $selectedShop"; textSize=11f; setTextColor(Color.parseColor("#93C5FD")); gravity=Gravity.END }
        hRight.addView(txtCashier); hRight.addView(txtShopTop)
        hRight.addView(TextView(this).apply { text="🕒 ${java.text.SimpleDateFormat("HH:mm").format(java.util.Date())}"; textSize=11f; setTextColor(Color.parseColor("#94A3B8")); gravity=Gravity.END })
        header.addView(hRight)
        root.addView(header)

        // 2) SELECT SHOP WITH BUTTONS - FIX
        val shopCard = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,12,16,12); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,8)} }
        shopCard.addView(TextView(this).apply { text="🏪 SELECT SHOP"; textSize=13f; setTypeface(null,Typeface.BOLD) })
        shopContainer = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,8,0,0) }
        shopCard.addView(shopContainer)
        loadShopsInto(shopContainer)
        root.addView(shopCard)

        // 3) QUICK ADD BY CODE / BARCODE / DESCRIPTION - FIX
        val searchCard = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(12,12,12,12); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,0,12,8)} }
        searchCard.addView(TextView(this).apply { text="⚡ QUICK ADD - Code / Barcode / Name + ENTER"; textSize=11f; setTextColor(Color.parseColor("#16A34A")); setTypeface(null,Typeface.BOLD) })
        val searchRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(0,6,0,0) }
        inputQuick = EditText(this).apply { hint="e.g. 001 or stove or 123456 barcode..."; setBackgroundColor(Color.parseColor("#F1F5F9")); setPadding(16,14,16,14); layoutParams=LinearLayout.LayoutParams(0,-2,1f) }
        val btnAdd = Button(this).apply { text="ADD"; setBackgroundColor(Color.parseColor("#22C55E")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(-2,-2).apply{setMargins(8,0,0,0)}; setOnClickListener{ quickAddProduct() } }
        searchRow.addView(inputQuick); searchRow.addView(btnAdd)
        searchCard.addView(searchRow)
        val btnSearchFull = Button(this).apply { text="🔍 SEARCH PRODUCTS LIST"; setBackgroundColor(Color.parseColor("#1D4ED8")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,8,0,0)}; setOnClickListener{ showProductPicker() } }
        searchCard.addView(btnSearchFull)
        root.addView(searchCard)

        // CART
        root.addView(TextView(this).apply { text="CART"; textSize=15f; setTypeface(null,Typeface.BOLD); setPadding(16,8,16,4) })
        val cartHead = LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#E2E8F0")); setPadding(10,6,10,6); orientation=LinearLayout.HORIZONTAL }
        cartHead.addView(TextView(this).apply { text="#"; layoutParams=LinearLayout.LayoutParams(30,-2); textSize=11f; setTypeface(null,Typeface.BOLD) })
        cartHead.addView(TextView(this).apply { text="ITEM"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); textSize=11f; setTypeface(null,Typeface.BOLD) })
        cartHead.addView(TextView(this).apply { text="QTY"; layoutParams=LinearLayout.LayoutParams(110,-2); gravity=Gravity.CENTER; textSize=11f; setTypeface(null,Typeface.BOLD) })
        cartHead.addView(TextView(this).apply { text="TOTAL"; layoutParams=LinearLayout.LayoutParams(80,-2); gravity=Gravity.END; textSize=11f; setTypeface(null,Typeface.BOLD) })
        root.addView(cartHead)
        cartContainer = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        root.addView(cartContainer)

        // RECEIPT
        val receiptCard = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(14,14,14,14); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,12)} }
        txtReceiptCount = TextView(this).apply { text="🧾 0 items"; textSize=12f }
        txtReceiptTotal = TextView(this).apply { text="TOTAL: $0.00"; textSize=18f; setTypeface(null,Typeface.BOLD) }
        val rRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        rRow.addView(txtReceiptCount.apply { layoutParams=LinearLayout.LayoutParams(0,-2,1f) }); rRow.addView(txtReceiptTotal)
        receiptCard.addView(rRow)
        val tRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,10,0,6); gravity=Gravity.CENTER_VERTICAL }
        tRow.addView(TextView(this).apply { text="TENDERED $:"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); setTypeface(null,Typeface.BOLD) })
        inputTendered = EditText(this).apply { hint="0"; inputType=8194; setBackgroundColor(Color.parseColor("#F1F5F9")); setPadding(16,10,16,10); layoutParams=LinearLayout.LayoutParams(120,-2) }
        tRow.addView(inputTendered); receiptCard.addView(tRow)
        txtChange = TextView(this).apply { text="CHANGE: $0.00"; gravity=Gravity.CENTER; setBackgroundColor(Color.parseColor("#22C55E")); setTextColor(Color.WHITE); setPadding(0,14,0,14); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,6,0,6)}; setTypeface(null,Typeface.BOLD) }
        receiptCard.addView(txtChange)
        receiptCard.addView(Button(this).apply { text="✅ COMPLETE SALE + PRINT"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); textSize=15f; setTypeface(null,Typeface.BOLD); setPadding(0,18,0,18); setOnClickListener{ completeSaleAndPrint() } })
        root.addView(receiptCard)

        scroll.addView(root)
        setContentView(scroll)

        inputQuick.setOnKeyListener { _, keyCode, event -> if(keyCode==KeyEvent.KEYCODE_ENTER && event.action==KeyEvent.ACTION_UP){ quickAddProduct(); true } else false }
        inputTendered.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ updateTotals() }
            override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}
            override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}
        })
        refreshCart()
    }

    private fun loadShopsInto(container: LinearLayout){
        container.removeAllViews()
        val shops = mutableListOf<String>("Main Shop")
        try{
            val pref = getSharedPreferences("shops_db", Context.MODE_PRIVATE)
            for((_,v) in pref.all){ val name=v.toString().split("|")[0]; if(name.isNotEmpty()) shops.add(name) }
            val pref2 = getSharedPreferences("branches_db", Context.MODE_PRIVATE)
            for((_,v) in pref2.all){ val name=v.toString().split("|")[0]; if(name.isNotEmpty()) shops.add(name) }
        }catch(_:Exception){}
        val group = RadioGroup(this)
        for(s in shops.distinct()){
            val rb = RadioButton(this).apply { text=s; isChecked=(s==selectedShop); setOnCheckedChangeListener{_,c-> if(c){ selectedShop=s; txtShopTop.text="📍 Shop: $selectedShop" } } }
            group.addView(rb)
        }
        container.addView(group)
    }

    private fun loadAllProducts(){
        allProducts.clear()
        try{
            for(db in listOf("stock_main","products_db
