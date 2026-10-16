package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject

class SalesActivity : Activity() {

    data class Product(val code:String, val name:String, val qty:Int, val cost:Float, val price:Float, val branch:String)
    data class CartItem(val product:Product, var cartQty:Int)

    var all = mutableListOf<Product>()
    var filtered = mutableListOf<Product>()
    var cart = mutableListOf<CartItem>()
    lateinit var tableProducts: LinearLayout
    lateinit var etSearch: EditText
    lateinit var tvProductsCount: TextView
    lateinit var tvSubtotal: TextView
    lateinit var tvTotal: TextView
    lateinit var tvTendered: TextView
    lateinit var tvChange: TextView
    lateinit var containerReceipt: LinearLayout
    lateinit var spinnerShop: Spinner
    var selectedShop = "Main Shop"
    var tendered = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)

        // HEADER
        root.addView(LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#111827"))
            setPadding(16,14,16,14)
            addView(TextView(this@POSActivity).apply{ text="SMART POS"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=16f })
        })

        val cashierRow = LinearLayout(this)
        cashierRow.setBackgroundColor(Color.parseColor("#1E3A8A"))
        cashierRow.setPadding(12,8,12,8)
        cashierRow.orientation=LinearLayout.HORIZONTAL
        cashierRow.addView(TextView(this).apply{ text="SMART POS"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=13f; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        cashierRow.addView(LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL; gravity=Gravity.END
            addView(TextView(this@POSActivity).apply{ text="Cashier: John Mukucha"; setTextColor(Color.WHITE); textSize=10f; gravity=Gravity.END })
            addView(TextView(this@POSActivity).apply{ text="17/10/2026 00:26"; setTextColor(Color.WHITE); textSize=10f; gravity=Gravity.END })
        })
        root.addView(cashierRow)

        // SHOP
        root.addView(TextView(this).apply{ text="SHOP"; textSize=10f; setTextColor(Color.GRAY); setPadding(12,6,0,2) })
        spinnerShop = Spinner(this)
        val shops = mutableListOf("Main Shop","Rukudzo","Rukudzo Shop","All Shops")
        try{
            val p=getSharedPreferences("shops", Context.MODE_PRIVATE)
            for((_,v) in p.all){ try{ val o=JSONObject(v.toString()); val n=o.optString("name",""); if(n.isNotBlank() && !shops.contains(n)) shops.add(n) }catch(_:Exception){} }
        }catch(_:Exception){}
        spinnerShop.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shops)
        spinnerShop.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,0,8,8)}
        root.addView(spinnerShop)
        selectedShop = shops[0]
        spinnerShop.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a:AdapterView<*>?, v:View?, p:Int, id:Long){ selectedShop=shops[p]; filter() }
            override fun onNothingSelected(a:AdapterView<*>?){}
        }

        // SEARCH
        val searchBox = LinearLayout(this)
        searchBox.setBackgroundColor(Color.WHITE)
        searchBox.setPadding(12,12,12,12)
        searchBox.orientation=LinearLayout.HORIZONTAL
        searchBox.gravity=Gravity.CENTER_VERTICAL
        searchBox.elevation=4f
        searchBox.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,0,8,4)}
        searchBox.addView(TextView(this).apply{ text="🔍"; textSize=14f })
        etSearch = EditText(this)
        etSearch.hint="SEARCH CODE or Name"
        etSearch.textSize=14f
        etSearch.setTypeface(null, Typeface.BOLD)
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(8,0,8,0)}
        searchBox.addView(etSearch)
        val btnClear = TextView(this)
        btnClear.text="X"; btnClear.textSize=16f; btnClear.setPadding(12,0,12,0)
        btnClear.setOnClickListener{ etSearch.setText("") }
        searchBox.addView(btnClear)
        root.addView(searchBox)

        // PRODUCTS COUNT - REAL
        tvProductsCount = TextView(this)
        tvProductsCount.text="Products (0) - NO FAKE DATA"
        tvProductsCount.textSize=11f
        tvProductsCount.setTextColor(Color.parseColor("#2563EB"))
        tvProductsCount.setPadding(12,4,0,2)
        root.addView(tvProductsCount)

        root.addView(TextView(this).apply{ text="Products"; textSize=12f; setTypeface(null, Typeface.BOLD); setPadding(12,2,0,4) })

        // PRODUCTS LIST
        val scrollProducts = ScrollView(this)
        scrollProducts.layoutParams=LinearLayout.LayoutParams(-1,0,1f)
        tableProducts = LinearLayout(this)
        tableProducts.orientation=LinearLayout.VERTICAL
        scrollProducts.addView(tableProducts)
        root.addView(scrollProducts)

        // VIRTUAL RECEIPT - FIXED LAYOUT - NOT OVERLAPPING
        val receiptCard = LinearLayout(this)
        receiptCard.orientation=LinearLayout.VERTICAL
        receiptCard.setBackgroundColor(Color.parseColor("#FDE047")) // yellow
        receiptCard.setPadding(12,10,12,10)
        receiptCard.layoutParams=LinearLayout.LayoutParams(-1,-2)

        // RECEIPT HEADER
        val receiptHeader = LinearLayout(this)
        receiptHeader.orientation=LinearLayout.HORIZONTAL
        receiptHeader.addView(TextView(this).apply{ text="Virtual Receipt"; setTypeface(null, Typeface.BOLD); textSize=13f; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        receiptHeader.addView(TextView(this).apply{ text="Subtotal"; textSize=11f; setPadding(0,0,8,0) })
        tvSubtotal = TextView(this).apply{ text="$0"; setTypeface(null, Typeface.BOLD); textSize=12f }
        receiptHeader.addView(tvSubtotal)
        receiptHeader.addView(TextView(this).apply{ text="TOTAL"; textSize=12f; setTypeface(null, Typeface.BOLD); setPadding(8,0,4,0) })
        tvTotal = TextView(this).apply{ text="$0"; setTypeface(null, Typeface.BOLD); textSize=14f; setTextColor(Color.parseColor("#111827")) }
        receiptHeader.addView(tvTotal)
        receiptCard.addView(receiptHeader)

        // RECEIPT ITEMS
        containerReceipt = LinearLayout(this)
        containerReceipt.orientation=LinearLayout.VERTICAL
        containerReceipt.setPadding(0,8,0,8)
        receiptCard.addView(containerReceipt)

        // TENDERED / CHANGE ROW
        val tenderRow = LinearLayout(this)
        tenderRow.orientation=LinearLayout.HORIZONTAL
        tenderRow.gravity=Gravity.CENTER_VERTICAL
        tenderRow.setPadding(0,8,0,0)

        tenderRow.addView(TextView(this).apply{ text="TENDERED: "; textSize=12f; setTypeface(null, Typeface.BOLD) })
        tvTendered = TextView(this).apply{
            text="$0"; textSize=14f; setTypeface(null, Typeface.BOLD); setBackgroundColor(Color.WHITE); setPadding(12,8,12,8)
            layoutParams=LinearLayout.LayoutParams(0,-2,0.6f).apply{setMargins(0,0,8,0)}
        }
        tenderRow.addView(tvTendered)

        tenderRow.addView(TextView(this).apply{ text="CHANGE: "; textSize=12f; setTypeface(null, Typeface.BOLD) })
        tvChange = TextView(this).apply{
            text="$0"; textSize=14f; setTypeface(null, Typeface.BOLD); setBackgroundColor(Color.WHITE); setPadding(12,8,12,8); setTextColor(Color.parseColor("#16A34A"))
            layoutParams=LinearLayout.LayoutParams(0,-2,0.6f)
        }
        tenderRow.addView(tvChange)

        // QUICK CASH BUTTONS
        val cashBtns = LinearLayout(this)
        cashBtns.orientation=LinearLayout.HORIZONTAL
        cashBtns.setPadding(0,8,0,0)
        val amounts = listOf("10","20","50","100")
        for(am in amounts){
            val b = Button(this)
            b.text="$$am"; b.textSize=10f; b.setPadding(0,12,0,12)
            b.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(2,0,2,0)}
            b.setBackgroundColor(Color.WHITE)
            b.setOnClickListener{ addTendered(am.toFloat()) }
            cashBtns.addView(b)
        }
        val bPay = Button(this)
        bPay.text="PAY"; bPay.setBackgroundColor(Color.parseColor("#16A34A")); bPay.setTextColor(Color.WHITE); bPay.setTypeface(null, Typeface.BOLD)
        bPay.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,0,0)}
        bPay.setOnClickListener{ completeSale() }
        cashBtns.addView(bPay)

        receiptCard.addView(tenderRow)
        receiptCard.addView(cashBtns)

        root.addView(receiptCard)
        setContentView(root)

        loadProductsReal()
        filter()

        etSearch.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    fun loadProductsReal(){
        all.clear()
        val prefs = listOf("received_stock","stock_received","products","products_main","products_db","inventory","stock","products_list","items","goods_received","product_list")

        for(pn in prefs){
            try{
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    try{
                        val raw = v.toString()
                        if(!raw.trim().startsWith("{")) continue
                        val o = JSONObject(raw)
                        val code = o.optString("code", o.optString("sku", k))
                        val name = o.optString("name", o.optString("product_name", o.optString("product","")))
                        if(name.isBlank() && code.isBlank()) continue

                        var qty = 0
                        for(qk in listOf("qnty","quantity","qty","on_hand","current_stock","received_qty","in_stock","stock")){
                            if(o.has(qk)){ qty = o.optString(qk,"0").toIntOrNull() ?: o.optInt(qk,0); if(qty==0) qty = o.optDouble(qk,0.0).toInt(); if(qty!=0) break }
                        }

                        var cost = 0f
                        for(ck in listOf("cost","buying_price","cost_price","purchase_price")){
                            if(o.has(ck)){ cost = o.optString(ck,"0").toFloatOrNull() ?: o.optDouble(ck,0.0).toFloat(); if(cost!=0f) break }
                        }

                        var price = 0f
                        for(sk in listOf("selling_price","price","sell_price","sale_price","retail_price")){
                            if(o.has(sk)){ price = o.optString(sk,"0").toFloatOrNull() ?: o.optDouble(sk,0.0).toFloat(); if(price!=0f) break }
                        }

                        val branch = o.optString("branch", o.optString("shop","Main Shop"))

                        // NO FAKE DATA FILTER - SKIP FAKE TR-01 / Travel Cup if marked fake
                        if(name.contains("TR-01", true) || name.contains("Travel Cup", true)){
                            // Allow only if real - check if qty>0 or cost>0 means real user added
                            if(qty==0 && cost==0f && price==0f) continue
                        }

                        if(all.none{ it.code==code }){
                            all.add(Product(code, if(name.isNotBlank()) name else code, qty, cost, price, branch))
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
    }

    fun filter(){
        val q = etSearch.text.toString().lowercase().trim()
        filtered = all.filter{
            val branchOk = selectedShop=="All Shops" || it.branch.contains(selectedShop, true) || selectedShop.contains(it.branch, true)
            val searchOk = q.isBlank() || it.name.lowercase().contains(q) || it.code.lowercase().contains(q)
            branchOk && searchOk && it.qty>0
        }.toMutableList()

        tvProductsCount.text = "Products (${filtered.size}) - NO FAKE DATA"
        renderProducts()
    }

    fun renderProducts(){
        tableProducts.removeAllViews()
        if(filtered.isEmpty()){
            val empty = LinearLayout(this)
            empty.orientation=LinearLayout.VERTICAL; empty.gravity=Gravity.CENTER; empty.setPadding(0,40,0,0)
            empty.addView(TextView(this).apply{ text="No products yet"; textSize=13f; setTextColor(Color.GRAY); gravity=Gravity.CENTER })
            empty.addView(TextView(this).apply{ text="Go to Add Product screen\nAdd your real products first\n\nNO FAKE TR-01 / Travel Cup\nNO FAKE DATA"; textSize=11f; setTextColor(Color.GRAY); gravity=Gravity.CENTER; setPadding(0,12,0,0) })
            tableProducts.addView(empty)
            return
        }

        for(p in filtered){
            val row = LinearLayout(this)
            row.orientation=LinearLayout.HORIZONTAL
            row.setPadding(12,12,12,12)
            row.setBackgroundColor(Color.WHITE)
            row.elevation=2f
            row.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,0,8,6)}
            row.gravity=Gravity.CENTER_VERTICAL

            val info = LinearLayout(this)
            info.orientation=LinearLayout.VERTICAL
            info.layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            info.addView(TextView(this).apply{ text=p.name; textSize=13f; setTypeface(null, Typeface.BOLD) })
            info.addView(TextView(this).apply{ text="${p.code} | Stock: ${p.qty} | $${p.price}"; textSize=11f; setTextColor(Color.GRAY) })
            row.addView(info)

            val btnAdd = Button(this)
            btnAdd.text="ADD"; btnAdd.textSize=11f; btnAdd.setBackgroundColor(Color.parseColor("#2563EB")); btnAdd.setTextColor(Color.WHITE)
            btnAdd.setPadding(20,10,20,10)
            btnAdd.setOnClickListener{ addToCart(p) }
            row.addView(btnAdd)

            tableProducts.addView(row)
        }
    }

    fun addToCart(p:Product){
        val existing = cart.find{ it.product.code==p.code }
        if(existing!=null){
            if(existing.cartQty < p.qty) existing.cartQty++
            else { Toast.makeText(this,"No more stock! Only ${p.qty}", Toast.LENGTH_SHORT).show(); return }
        }else{
            cart.add(CartItem(p, 1))
        }
        renderReceipt()
    }

    fun renderReceipt(){
        containerReceipt.removeAllViews()
        var subtotal = 0f
        for(item in cart){
            subtotal += item.product.price * item.cartQty
            val row = LinearLayout(this)
            row.orientation=LinearLayout.HORIZONTAL
            row.setPadding(4,4,4,4)
            row.addView(TextView(this).apply{ text="${item.product.name} x${item.cartQty}"; textSize=11f; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            row.addView(TextView(this).apply{ text="$${item.product.price * item.cartQty}"; textSize=11f; setTypeface(null, Typeface.BOLD) })
            val btnDel = TextView(this)
            btnDel.text=" X"; btnDel.setTextColor(Color.RED); btnDel.setTypeface(null, Typeface.BOLD); btnDel.setPadding(12,0,0,0)
            btnDel.setOnClickListener{ cart.remove(item); renderReceipt() }
            row.addView(btnDel)
            containerReceipt.addView(row)
        }

        tvSubtotal.text = "$${String.format("%.2f", subtotal)}"
        tvTotal.text = "$${String.format("%.2f", subtotal)}"
        val change = tendered - subtotal
        tvChange.text = "$${String.format("%.2f", if(change>0) change else 0f)}"
    }

    fun addTendered(amount:Float){
        tendered += amount
        tvTendered.text = "$${tendered}"
        renderReceipt()
    }

    fun completeSale(){
        if(cart.isEmpty()){ Toast.makeText(this,"Cart empty", Toast.LENGTH_SHORT).show(); return }
        val total = cart.sumOf{ (it.product.price * it.cartQty).toDouble() }.toFloat()
        if(tendered < total){ Toast.makeText(this,"Tendered $tendered < Total $total", Toast.LENGTH_SHORT).show(); return }

        // SAVE SALE TO PREFS - REAL DATA FOR REPORTS
        try{
            val salesPref = getSharedPreferences("sales", Context.MODE_PRIVATE)
            for(item in cart){
                val id = "sale_${System.currentTimeMillis()}_${item.product.code}"
                val o = JSONObject()
                o.put("code", item.product.code)
                o.put("product_name", item.product.name)
                o.put("name", item.product.name)
                o.put("qty", item.cartQty)
                o.put("quantity", item.cartQty)
                o.put("price", item.product.price)
                o.put("selling_price", item.product.price)
                o.put("cost", item.product.cost)
                o.put("buying_price", item.product.cost)
                o.put("total", item.product.price * item.cartQty)
                o.put("branch", selectedShop)
                o.put("department", "General")
                o.put("date", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))
                salesPref.edit().putString(id, o.toString()).apply()

                // DEDUCT STOCK FROM RECEIVED STOCK
                for(pn in listOf("received_stock","products","inventory")){
                    try{
                        val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                        for((k,v) in pref.all){
                            try{
                                val stockO = JSONObject(v.toString())
                                val scode = stockO.optString("code", k)
                                if(scode==item.product.code){
                                    var currentQty = 0
                                    for(qk in listOf("qnty","quantity","qty")){
                                        if(stockO.has(qk)){ currentQty = stockO.optInt(qk,0); if(currentQty!=0) break }
                                    }
                                    val newQty = currentQty - item.cartQty
                                    stockO.put("qnty", newQty)
                                    stockO.put("quantity", newQty)
                                    stockO.put("qty", newQty)
                                    pref.edit().putString(k, stockO.toString()).apply()
                                }
                            }catch(_:Exception){}
                        }
                    }catch(_:Exception){}
                }
            }
        }catch(_:Exception){}

        Toast.makeText(this,"Sale completed! Total $$total", Toast.LENGTH_LONG).show()
        cart.clear()
        tendered = 0f
        tvTendered.text="$0"
        tvChange.text="$0"
        loadProductsReal()
        filter()
        renderReceipt()
    }
}