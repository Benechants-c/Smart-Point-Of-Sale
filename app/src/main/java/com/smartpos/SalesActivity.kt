package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class SalesActivity : Activity() {
    data class Product(val code: String, val name: String, val sell: Double, val cost: Double, var qty: Int, val shopId: String, val dept: String = "General")
    data class CartItem(var product: Product, var qty: Int)

    private val allProducts = mutableListOf<Product>()
    private val filtered = mutableListOf<Product>()
    private val cart = mutableListOf<CartItem>()
    private lateinit var spinnerShop: Spinner
    private lateinit var spinnerDept: Spinner
    private lateinit var searchInput: EditText
    private lateinit var productsLayout: LinearLayout
    private lateinit var cartLayout: LinearLayout
    private lateinit var totalView: TextView
    private lateinit var tenderedInput: EditText
    private lateinit var changeView: TextView
    private val shops = mutableListOf<Pair<String,String>>()
    private val depts = mutableListOf<String>()

    private fun getSelectedShopId(): String = if(spinnerShop.selectedItemPosition in shops.indices) shops[spinnerShop.selectedItemPosition].first else "main"
    private fun getSelectedShopName(): String = if(spinnerShop.selectedItemPosition in shops.indices) shops[spinnerShop.selectedItemPosition].second else "Main Shop"
    private fun getTotal(): Double { var t=0.0; for(i in cart) t+= i.product.sell * i.qty; return t }

    private fun calcChange() {
        val total = getTotal()
        val tendered = tenderedInput.text.toString().toDoubleOrNull() ?: 0.0
        val change = tendered - total
        if(tendered==0.0) changeView.text = "CHANGE: $0.00"
        else if(change<0) changeView.text = "CHANGE: NEED $${String.format("%.2f", -change)} MORE"
        else changeView.text = "CHANGE: $${String.format("%.2f", change)}"
        changeView.setTextColor(if(change<0) Color.RED else Color.parseColor("#16A34A"))
    }

    private fun printReceipt(tendered: Double, change: Double) {
        if(cart.isEmpty()) return
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        var total = 0.0
        var receipt = "============================\n"
        receipt += "${getSelectedShopName().uppercase()}\n"
        receipt += "POS SALES RECEIPT\n"
        receipt += "$date\n"
        receipt += "============================\n"
        for(item in cart){
            val lineTotal = item.product.sell * item.qty
            total += lineTotal
            receipt += "${item.product.name}\n"
            receipt += " ${item.product.code} x${item.qty} @ $${item.product.sell} = $${String.format("%.2f", lineTotal)}\n"
        }
        receipt += "----------------------------\n"
        receipt += "TOTAL: $${String.format("%.2f", total)}\n"
        receipt += "TENDERED: $${String.format("%.2f", tendered)}\n"
        receipt += "CHANGE: $${String.format("%.2f", change)}\n"
        receipt += "============================\n"
        receipt += "Thank you! Come again\n"
        receipt += "Powered by SmartPOS\n"
        try {
            val i = android.content.Intent(android.content.Intent.ACTION_SEND)
            i.type = "text/plain"
            i.putExtra(android.content.Intent.EXTRA_TEXT, receipt)
            startActivity(android.content.Intent.createChooser(i, "PRINT RECEIPT"))
        } catch(e:Exception){
            Toast.makeText(this, receipt, Toast.LENGTH_LONG).show()
        }
    }

    private fun loadRealStock() {
        allProducts.clear()
        try {
            val shopId = getSelectedShopId()
            val stockPref = getSharedPreferences("stock_$shopId", Context.MODE_PRIVATE)
            stockPref.all.forEach { (code, v) ->
                try {
                    val parts = v.toString().split("|")
                    if(parts.size >= 4) {
                        val name = parts[0]; val cost = parts[1].toDoubleOrNull()?:0.0
                        val sell = parts[2].toDoubleOrNull()?:0.0; val qty = parts[3].toIntOrNull()?:0
                        val dept = if(parts.size>5) parts[5] else "General"
                        if(qty>0) allProducts.add(Product(code, name, sell, cost, qty, shopId, dept))
                    }
                }catch(_:Exception){}
            }
            val inv = getSharedPreferences("inventory_db", Context.MODE_PRIVATE)
            inv.all.forEach { (key, v) ->
                if(key.startsWith("${shopId}_")) {
                    try{
                        val code = key.removePrefix("${shopId}_")
                        if(allProducts.none{it.code==code}) {
                            val parts = v.toString().split("|")
                            if(parts.size>=4) {
                                val name = parts[0]; val cost = parts[1].toDoubleOrNull()?:0.0
                                val sell = parts[2].toDoubleOrNull()?:0.0; val qty = parts[3].toIntOrNull()?:0
                                val dept = if(parts.size>5) parts[5] else "General"
                                if(qty>0) allProducts.add(Product(code,name,sell,cost,qty,shopId,dept))
                            }
                        }
                    }catch(_:Exception){}
                }
            }
        }catch(_:Exception){}
        depts.clear(); depts.add("ALL DEPARTMENTS")
        allProducts.map{it.dept.ifEmpty{"General"}}.distinct().forEach{ depts.add(it) }
        spinnerDept.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, depts)
        applyFilter()
    }

    private fun applyFilter() {
        filtered.clear()
        val q = searchInput.text.toString().trim().lowercase()
        val deptSel = if(spinnerDept.selectedItemPosition>=0) depts.getOrNull(spinnerDept.selectedItemPosition) else "ALL DEPARTMENTS"
        for(p in allProducts){
            val matchSearch = q.isEmpty() || p.name.lowercase().contains(q) || p.code.lowercase().contains(q)
            val matchDept = deptSel=="ALL DEPARTMENTS" || p.dept==deptSel
            if(matchSearch && matchDept) filtered.add(p)
        }
        refreshProducts()
    }

    private fun refreshProducts() {
        productsLayout.removeAllViews()
        if(filtered.isEmpty()){
            productsLayout.addView(TextView(this).apply{
                text = "No stock in ${getSelectedShopName()}\nGo to RECEIVE STOCK first"; setPadding(20,30,20,30); setTextColor(Color.GRAY)
            }); return
        }
        for(p in filtered){
            val row = LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setPadding(12,12,12,12); setBackgroundColor(Color.WHITE) }
            row.addView(TextView(this).apply{
                text = "${p.name}\n${p.code} | ${p.dept} | Stock:${p.qty} | $${p.sell}"; textSize=13f; layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            })
            row.addView(Button(this).apply{
                text="ADD"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)
                setOnClickListener{ addToCart(p) }
            })
            productsLayout.addView(row)
            productsLayout.addView(View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,2); setBackgroundColor(Color.parseColor("#E5E7EB")) })
        }
    }

    private fun addToCart(p: Product){
        val exist = cart.find{ it.product.code==p.code }
        if(exist!=null){
            if(exist.qty < p.qty) exist.qty++ else { Toast.makeText(this,"No more stock",Toast.LENGTH_SHORT).show(); return }
        } else {
            cart.add(CartItem(p,1))
        }
        refreshCart()
    }

    private fun refreshCart(){
        cartLayout.removeAllViews()
        var total = 0.0; var tq = 0
        for(item in cart){
            total += item.product.sell * item.qty; tq += item.qty
            val r = LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setPadding(12,8,12,8) }
            r.addView(TextView(this).apply{ text="${item.product.name} x${item.qty} = $${item.product.sell*item.qty}"; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            r.addView(Button(this).apply{ text="-"; setOnClickListener{ if(item.qty>1) item.qty-- else cart.remove(item); refreshCart() }})
            cartLayout.addView(r)
        }
        totalView.text = "TOTAL: $${String.format("%.2f", total)} | $tq items | ${getSelectedShopName()}"
        calcChange()
    }

    private fun completeSale(){
        if(cart.isEmpty()){ Toast.makeText(this,"Cart empty",Toast.LENGTH_SHORT).show(); return }
        val total = getTotal()
        val tendered = tenderedInput.text.toString().toDoubleOrNull() ?: 0.0
        if(tendered < total){ Toast.makeText(this,"Tendered $${tendered} less than TOTAL $${total}",Toast.LENGTH_LONG).show(); return }
        val change = tendered - total
        printReceipt(tendered, change)
        try{
            val shopId = getSelectedShopId()
            val inv = getSharedPreferences("inventory_db", Context.MODE_PRIVATE)
            val invEdit = inv.edit()
            for(item in cart){
                val key = "${shopId}_${item.product.code}".lowercase()
                val oldStr = inv.getString(key,"")?: getSharedPreferences("stock_$shopId", Context.MODE_PRIVATE).getString(item.product.code,"")?:""
                if(oldStr.contains("|")){
                    val parts = oldStr.split("|").toMutableList()
                    val oldQty = parts.getOrNull(3)?.toIntOrNull()?:0
                    val newQty = oldQty - item.qty
                    parts[3] = newQty.toString()
                    val newStr = parts.joinToString("|")
                    invEdit.putString(key,newStr)
                    getSharedPreferences("stock_$shopId", Context.MODE_PRIVATE).edit().putString(item.product.code,newStr).apply()
                }
            }
            invEdit.apply()
        }catch(e:Exception){}
        Toast.makeText(this,"SALE COMPLETE | Change: $${String.format("%.2f", change)}",Toast.LENGTH_LONG).show()
        cart.clear(); tenderedInput.setText(""); refreshCart(); loadRealStock()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try{
            val prefsShops = getSharedPreferences("shops_db", Context.MODE_PRIVATE)
            prefsShops.all.forEach { (k,v) ->
                try{
                    val str = v.toString()
                    if(str.startsWith("{")){ val j=JSONObject(str); shops.add(Pair(j.optString("id",k), "${j.optString("name",k)} ($k)")) }
                    else shops.add(Pair(k,k))
                }catch(_:Exception){ shops.add(Pair(k,k)) }
            }
        }catch(_:Exception){}
        if(shops.isEmpty()) shops.add(Pair("main","Main Shop"))

        val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundColor(Color.parseColor("#F8FAFC")) }
        root.addView(TextView(this).apply{ text="POS SALES"; textSize=18f; setTypeface(null,Typeface.BOLD); setPadding(0,0,0,10) })
        root.addView(TextView(this).apply{ text="SELECT SHOP *"; textSize=11f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1E293B")); setPadding(12,6,12,6) })
        spinnerShop = Spinner(this); spinnerShop.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shops.map{it.second}); root.addView(spinnerShop)
        root.addView(TextView(this).apply{ text="FILTER BY DEPARTMENT (Auto)"; textSize=11f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1E293B")); setPadding(12,6,12,6) })
        spinnerDept = Spinner(this); depts.add("ALL DEPARTMENTS"); spinnerDept.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, depts); root.addView(spinnerDept)
        searchInput = EditText(this).apply{ hint="Search product - REAL stock"; setPadding(20,14,20,14); setBackgroundColor(Color.WHITE) }; root.addView(searchInput)
        root.addView(TextView(this).apply{ text="PRODUCTS - REAL STOCK ONLY"; setBackgroundColor(Color.parseColor("#DBEAFE")); setPadding(12,6,12,6); setTypeface(null,Typeface.BOLD) })
        productsLayout = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,0,1.2f); addView(productsLayout) })
        root.addView(TextView(this).apply{ text="CART"; setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(12,6,12,6); setTypeface(null,Typeface.BOLD) })
        cartLayout = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        root.addView(ScrollView(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,0,0.8f); addView(cartLayout) })
        totalView = TextView(this).apply{ text="TOTAL: $0.00"; setBackgroundColor(Color.parseColor("#DBEAFE")); setPadding(16,12,16,12); setTypeface(null,Typeface.BOLD); textSize=15f }; root.addView(totalView)

        // TENDERED + CHANGE - NEW FOR SALE
        val tenderRow = LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setPadding(0,8,0,0) }
        tenderRow.addView(TextView(this).apply{ text="TENDERED $: "; setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(-2,-2) })
        tenderedInput = EditText(this).apply{ hint="Amount given by customer"; inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; layoutParams=LinearLayout.LayoutParams(0,-2,1f); setBackgroundColor(Color.WHITE); setPadding(16,12,16,12) }
        tenderRow.addView(tenderedInput)
        root.addView(tenderRow)

        changeView = TextView(this).apply{ text="CHANGE: $0.00"; setBackgroundColor(Color.parseColor("#FEF3C7")); setPadding(16,12,16,12); setTypeface(null,Typeface.BOLD); textSize=16f; setTextColor(Color.parseColor("#16A34A")) }
        root.addView(changeView)

        root.addView(Button(this).apply{ text="COMPLETE SALE + PRINT"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); setOnClickListener{ completeSale() } })

        spinnerShop.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a: AdapterView<*>?, v: android.view.View?, p: Int, id: Long){ loadRealStock() }
            override fun onNothingSelected(a: AdapterView<*>?){}
        }
        spinnerDept.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a: AdapterView<*>?, v: android.view.View?, p: Int, id: Long){ applyFilter() }
            override fun onNothingSelected(a: AdapterView<*>?){}
        }
        searchInput.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?){ applyFilter() }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int){}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int){}
        })
        tenderedInput.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?){ calcChange() }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int){}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int){}
        })
        setContentView(root)
        loadRealStock()
    }
}
