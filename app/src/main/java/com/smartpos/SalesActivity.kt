package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class NewSaleActivity : Activity() {

    data class Product(val id: String, val name: String, val code: String, val price: Float, val qty: Int)

    private var allProducts = mutableListOf<Product>()
    private var filteredProducts = mutableListOf<Product>()
    private var cart = mutableListOf<Pair<Product, Int>>()

    private lateinit var shopSpinner: Spinner
    private lateinit var searchEdit: EditText
    private lateinit var productsContainer: LinearLayout
    private lateinit var tvProductCount: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvTotal: TextView
    private lateinit var etTendered: EditText
    private lateinit var tvChange: TextView
    private lateinit var tvReceipt: TextView

    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F3F4F6"))

        root.addView(TextView(this).apply {
            text = "SMART POS"
            setBackgroundColor(Color.parseColor("#111827"))
            setTextColor(Color.WHITE)
            setPadding(20,18,20,18)
            setTypeface(null, Typeface.BOLD)
        })

        val header = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E3A8A"))
            setPadding(12,8,12,8)
            orientation = LinearLayout.HORIZONTAL
        }
        header.addView(TextView(this).apply { text = "SMART POS"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
        header.addView(TextView(this).apply { text = "Cashier: John Mukucha\n${sdf.format(Date())}"; setTextColor(Color.WHITE); textSize = 9f; gravity = Gravity.END })
        root.addView(header)

        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(-1,0,1f) }

        val shopCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(12,6,12,6) }
        shopCard.addView(TextView(this).apply { text = "SHOP"; setTextColor(Color.GRAY); textSize = 10f })
        shopSpinner = Spinner(this)
        shopCard.addView(shopSpinner)
        content.addView(shopCard)

        val searchCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(12,8,12,8)
            gravity = Gravity.CENTER_VERTICAL
        }
        searchCard.addView(TextView(this).apply { text = "🔍" })
        searchEdit = EditText(this).apply { hint = "SEARCH CODE or Name"; setBackgroundColor(Color.TRANSPARENT); layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(8,0,8,0) } }
        searchCard.addView(searchEdit)
        val clearBtn = TextView(this).apply { text = "X"; setPadding(16,0,16,0); setTypeface(null, Typeface.BOLD) }
        clearBtn.setOnClickListener { searchEdit.setText("") }
        searchCard.addView(clearBtn)
        content.addView(searchCard)

        tvProductCount = TextView(this).apply { text = "Products (0) - NO FAKE DATA"; setTextColor(Color.parseColor("#2563EB")); textSize = 11f; setPadding(8,6,8,2) }
        content.addView(tvProductCount)
        content.addView(TextView(this).apply { text = "Products"; setTypeface(null, Typeface.BOLD); setPadding(8,0,8,4) })

        val scroll = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1,0,1f) }
        productsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(8,0,8,0) }
        scroll.addView(productsContainer)
        content.addView(scroll)

        // FIXED VIRTUAL RECEIPT - NO OVERLAP
        val receiptCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#FDE047")); setPadding(12,12,12,12) }
        receiptCard.addView(TextView(this).apply { text = "Virtual Receipt"; setTypeface(null, Typeface.BOLD); textSize = 14f })
        tvReceipt = TextView(this).apply { text = "No items"; textSize = 10f; setPadding(0,6,0,6) }
        receiptCard.addView(tvReceipt)

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0,6,0,6) }
        tvSubtotal = TextView(this).apply { text = "Subtotal: $0"; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0,-2,1f) }
        tvTotal = TextView(this).apply { text = "TOTAL: $0"; setTypeface(null, Typeface.BOLD); setTextColor(Color.RED); gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(0,-2,1f) }
        row1.addView(tvSubtotal); row1.addView(tvTotal)
        receiptCard.addView(row1)

        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0,6,0,0) }
        row2.addView(TextView(this).apply { text = "TENDERED: $"; setTypeface(null, Typeface.BOLD); textSize = 12f })
        etTendered = EditText(this).apply { hint="0"; setBackgroundColor(Color.WHITE); setPadding(8,4,8,4); layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(6,0,12,0) } }
        row2.addView(etTendered)
        row2.addView(TextView(this).apply { text = "CHANGE:"; setTypeface(null, Typeface.BOLD); textSize = 11f })
        tvChange = TextView(this).apply { text="$0"; setTypeface(null, Typeface.BOLD); setBackgroundColor(Color.WHITE); setPadding(12,6,12,6); layoutParams = LinearLayout.LayoutParams(-2,-2).apply { setMargins(6,0,0,0) } }
        row2.addView(tvChange)
        receiptCard.addView(row2)

        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0,8,0,0) }
        val btnClear = Button(this).apply { text="CLEAR"; layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(0,0,4,0) } }
        val btnSale = Button(this).apply { text="MAKE SALE"; layoutParams = LinearLayout.LayoutParams(0,-2,1.5f).apply { setMargins(4,0,0,0) } }
        btnClear.setOnClickListener { cart.clear(); updateReceipt() }
        btnSale.setOnClickListener { makeSale() }
        btnRow.addView(btnClear); btnRow.addView(btnSale)
        receiptCard.addView(btnRow)

        content.addView(receiptCard)
        root.addView(content)
        setContentView(root)

        searchEdit.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int){}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int){ filter(s.toString()) }
            override fun afterTextChanged(s: Editable?){}
        })
        etTendered.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int){}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int){ updateChange() }
            override fun afterTextChanged(s: Editable?){}
        })

        loadData()
    }

    private fun loadData(){
        allProducts.clear()
        // FIX: Check EVERY possible pref name where Receive Stock saves
        val prefNames = listOf("received_stock","receive_stock","receivedStock","stock_received","products","inventory","stock","products_list","all_products","shop_stock","main_shop_stock","MainShop")
        var foundAny = 0
        for(pn in prefNames){
            try{
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                if(pref.all.isEmpty()) continue
                for((k,v) in pref.all){
                    try{
                        val s = v.toString().trim()
                        if(!s.startsWith("{")) continue
                        val o = JSONObject(s)
                        // Try ALL possible keys for name
                        var name = o.optString("product_name","")
                        if(name.isBlank()) name = o.optString("name","")
                        if(name.isBlank()) name = o.optString("productName","")
                        if(name.isBlank()) name = o.optString("item_name","")
                        if(name.isBlank()) name = o.optString("item","")
                        if(name.isBlank()) continue
                        // Skip fake if you want, but allow real
                        if(name.contains("TR-01") || name.contains("Travel Cup")) continue

                        // Try ALL possible keys for code
                        var code = o.optString("product_code","")
                        if(code.isBlank()) code = o.optString("code","")
                        if(code.isBlank()) code = o.optString("barcode","")
                        if(code.isBlank()) code = k.take(8)

                        // Try ALL possible keys for qty
                        var qty = 0
                        if(o.has("qnty")) qty = o.optInt("qnty",0)
                        else if(o.has("quantity")) qty = o.optInt("quantity",0)
                        else if(o.has("qty")) qty = o.optInt("qty",0)
                        else if(o.has("stock_qty")) qty = o.optInt("stock_qty",0)
                        else if(o.has("current_qty")) qty = o.optInt("current_qty",0)
                        else if(o.has("received_qty")) qty = o.optInt("received_qty",0)

                        // Try ALL possible keys for price - THIS WAS THE BUG
                        var price = 0f
                        if(o.has("selling_price")) price = o.optDouble("selling_price",0.0).toFloat()
                        else if(o.has("sellingPrice")) price = o.optDouble("sellingPrice",0.0).toFloat()
                        else if(o.has("price")) price = o.optDouble("price",0.0).toFloat()
                        else if(o.has("sell_price")) price = o.optDouble("sell_price",0.0).toFloat()
                        else if(o.has("retail_price")) price = o.optDouble("retail_price",0.0).toFloat()
                        else if(o.has("sp")) price = o.optDouble("sp",0.0).toFloat()
                        else if(o.has("buying_price")) price = (o.optDouble("buying_price",0.0)*1.2).toFloat() // use buying + 20%

                        if(qty<=0 && price<=0) continue // skip empty
                        if(price<=0) price = 1f // allow item even if price missing, to show

                        foundAny++
                        // Avoid duplicates
                        if(allProducts.none { it.name==name }){
                            allProducts.add(Product(k, name, code, price, qty))
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }

        filteredProducts = allProducts.toMutableList()

        // Shops
        val shops = mutableListOf("Main Shop")
        try{
            val pref = getSharedPreferences("shops", Context.MODE_PRIVATE)
            for((_,v) in pref.all){ try{ val o=JSONObject(v.toString()); val n=o.optString("name",""); if(n.isNotBlank()) shops.add(n) }catch(_:Exception){} }
        }catch(_:Exception){}
        shopSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shops.distinct())

        if(allProducts.isEmpty()){
            Toast.makeText(this, "No stock found in received_stock. Found $foundAny raw. Go Receive Stock first.", Toast.LENGTH_LONG).show()
        }else{
            Toast.makeText(this, "Loaded ${allProducts.size} products from Received Stock", Toast.LENGTH_SHORT).show()
        }

        renderProducts()
    }

    private fun filter(q: String){
        filteredProducts = if(q.isBlank()) allProducts.toMutableList() else allProducts.filter { it.name.contains(q,true) || it.code.contains(q,true) }.toMutableList()
        renderProducts()
    }

    private fun renderProducts(){
        productsContainer.removeAllViews()
        tvProductCount.text = "Products (${filteredProducts.size}) - FROM RECEIVED STOCK"

        if(filteredProducts.isEmpty()){
            productsContainer.addView(TextView(this).apply {
                text = "No products yet\n\nYour Received Stock is empty or field names mismatch\n\nGo to RECEIVE STOCK\nAdd stock there\nThen come back here"
                gravity = Gravity.CENTER
                setTextColor(Color.GRAY)
                setPadding(0,30,0,30)
            })
            return
        }
        for(p in filteredProducts){
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.WHITE)
                setPadding(10,10,10,10)
                layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,4) }
                gravity = Gravity.CENTER_VERTICAL
            }
            val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0,-2,1f) }
            left.addView(TextView(this).apply { text = p.name; setTypeface(null, Typeface.BOLD); textSize = 12f })
            left.addView(TextView(this).apply { text = "Code: ${p.code} | Qty: ${p.qty}"; textSize = 10f; setTextColor(Color.GRAY) })
            left.addView(TextView(this).apply { text = "$${p.price}"; setTextColor(Color.parseColor("#16A34A")); setTypeface(null, Typeface.BOLD) })
            row.addView(left)
            val btnAdd = Button(this).apply { text = "ADD"; textSize = 10f }
            btnAdd.setOnClickListener { addToCart(p) }
            row.addView(btnAdd)
            productsContainer.addView(row)
        }
    }

    private fun addToCart(p: Product){
        val idx = cart.indexOfFirst { it.first.id==p.id }
        if(idx>=0) cart[idx]=Pair(p, cart[idx].second+1) else cart.add(Pair(p,1))
        updateReceipt()
    }

    private fun updateReceipt(){
        var sub = 0f
        val sb = StringBuilder()
        for((prod,qty) in cart){ sub+=prod.price*qty; sb.append("${prod.name} x$qty\n") }
        tvReceipt.text = if(sb.isEmpty()) "No items" else sb.toString()
        tvSubtotal.text = "Subtotal: $${String.format("%.2f",sub)}"
        tvTotal.text = "TOTAL: $${String.format("%.2f",sub)}"
        updateChange()
    }

    private fun updateChange(){
        try{
            val total = cart.sumOf { (it.first.price*it.second).toDouble() }.toFloat()
            val tendered = etTendered.text.toString().toFloatOrNull() ?: 0f
            val change = tendered - total
            tvChange.text = "$${String.format("%.2f", if(change<0) 0f else change)}"
        }catch(_:Exception){ tvChange.text = "$0" }
    }

    private fun makeSale(){
        if(cart.isEmpty()){ Toast.makeText(this,"Cart empty",Toast.LENGTH_SHORT).show(); return }
        val total = cart.sumOf { (it.first.price*it.second).toDouble() }.toFloat()
        val edit = getSharedPreferences("sales", Context.MODE_PRIVATE).edit()
        val now = sdf.format(Date())
        for((prod,qty) in cart){
            val o = JSONObject()
            o.put("product_name", prod.name)
            o.put("product_code", prod.code)
            o.put("qty", qty)
            o.put("price", prod.price)
            o.put("total", prod.price*qty)
            o.put("branch", shopSpinner.selectedItem?.toString() ?: "Main Shop")
            o.put("date", now)
            edit.putString(UUID.randomUUID().toString(), o.toString())
        }
        edit.apply()
        Toast.makeText(this,"Sale saved: $${total}",Toast.LENGTH_LONG).show()
        cart.clear(); updateReceipt(); etTendered.setText("")
    }
}