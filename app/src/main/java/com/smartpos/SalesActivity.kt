package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class SalesActivity : Activity() {

    data class Prod(val code:String, val name:String, val price:Float, val qty:Int, val shop:String, val key:String, val prefName:String)
    data class CartItem(val prod:Prod, var cartQty:Int)

    private var allProducts = mutableListOf<Prod>()
    private var cart = mutableListOf<CartItem>()
    private lateinit var shopSpinner: Spinner
    private lateinit var searchEt: EditText
    private lateinit var productListLayout: LinearLayout
    private lateinit var cartLayout: LinearLayout
    private lateinit var tvTotal: TextView
    private lateinit var tvReceipt: TextView
    private lateinit var etTendered: EditText
    private lateinit var tvChange: TextView
    private lateinit var tvStockInfo: TextView
    private var selectedShop = "Main Shop"
    private var branches = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }

        // HEADER - DARK
        val header = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20,14,20,14)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "SMART POS"; textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        })
        header.addView(TextView(this).apply {
            text = "ruvheneko - POS - \$ Tax Incl"; textSize = 10f; setTextColor(Color.parseColor("#86EFAC"))
            setBackgroundColor(Color.parseColor("#14532D")); setPadding(12,6,12,6)
        })
        root.addView(header)

        // SHOP SELECTOR - PASTEL
        val shopCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(14,12,14,12)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,8)}
            gravity = Gravity.CENTER_VERTICAL
            elevation = 2f
        }
        shopCard.addView(TextView(this).apply { text = "🏪 SELECT SHOP"; textSize = 11f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(100,-2) })
        shopSpinner = Spinner(this)
        shopCard.addView(shopSpinner, LinearLayout.LayoutParams(0,-2,1f))
        tvStockInfo = TextView(this).apply { text = "3 items"; textSize = 10f; setTextColor(Color.GRAY) }
        shopCard.addView(tvStockInfo)
        root.addView(shopCard)

        // SEARCH - PASTEL BLUE
        val searchCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#EFF6FF"))
            setPadding(12,12,12,12)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,0,12,8)}
            gravity = Gravity.CENTER_VERTICAL
        }
        searchCard.addView(TextView(this).apply { text = "🔍"; textSize = 16f; setPadding(0,0,8,0) })
        searchEt = EditText(this).apply {
            hint = "SEARCH by Name or CODE (SKU) e.g. STOVE or STOVE-01"; textSize = 12f
            setBackgroundColor(Color.WHITE); setPadding(16,14,16,14)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
            elevation = 1f
        }
        searchCard.addView(searchEt)
        root.addView(searchCard)

        // PRODUCTS LABEL
        val prodLabel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16,8,16,4)
        }
        prodLabel.addView(TextView(this).apply { text = "PRODUCTS - TAX INCLUSIVE"; textSize = 10f; setTypeface(null, Typeface.BOLD); setTextColor(Color.GRAY); layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
        prodLabel.addView(TextView(this).apply { text = "Tap ADD to cart"; textSize = 9f; setTextColor(Color.GRAY) })
        root.addView(prodLabel)

        // PRODUCT LIST - SCROLL
        val scrollProd = ScrollView(this).apply { layoutParams = LinearLayout.LayoutParams(-1,0,1.2f) }
        productListLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(8,0,8,0) }
        scrollProd.addView(productListLayout)
        root.addView(scrollProd)

        // CART HEADER - PASTEL YELLOW
        val cartHeader = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B")); setPadding(14,10,14,10)
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        }
        cartHeader.addView(TextView(this).apply {
            text = "🛒 CART - TAX INCLUSIVE"; textSize = 11f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        })
        cartHeader.addView(Button(this).apply {
            text = "CLEAR"; textSize = 10f; setBackgroundColor(Color.parseColor("#334155")); setTextColor(Color.WHITE)
            setOnClickListener{ cart.clear(); renderCart() }
        })
        root.addView(cartHeader)

        val scrollCart = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#FFFBEB"))
            layoutParams = LinearLayout.LayoutParams(-1,0,0.7f)
        }
        cartLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(8,8,8,8) }
        scrollCart.addView(cartLayout)
        root.addView(scrollCart)

        // FOOTER - PASTEL CARDS
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(12,12,12,12); elevation = 8f
        }

        val receiptCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#F1F5F9")); setPadding(12,10,12,10)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,8)}
        }
        tvReceipt = TextView(this).apply { text = "RECEIPT: 0 items"; textSize = 11f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0,-2,1f) }
        tvTotal = TextView(this).apply { text = "TOTAL: $0.00"; textSize = 14f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#0F172A")) }
        receiptCard.addView(tvReceipt); receiptCard.addView(tvTotal)
        footer.addView(receiptCard)

        val tenderCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE); setPadding(8,6,8,6)
        }
        tenderCard.addView(TextView(this).apply { text = "TENDERED $:"; textSize = 11f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(90,-2) })
        etTendered = EditText(this).apply {
            hint = "0.00"; textSize = 14f; setTypeface(null, Typeface.BOLD)
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setBackgroundColor(Color.parseColor("#F8FAFC")); setPadding(16,12,16,12)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        }
        tenderCard.addView(etTendered)
        footer.addView(tenderCard)

        tvChange = TextView(this).apply {
            text = "CHANGE: $0.00"; textSize = 15f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#FEF9C3")); setTextColor(Color.parseColor("#854D0E"))
            setPadding(0,14,0,14); layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,8,0,8)}
        }
        footer.addView(tvChange)

        val completeBtn = Button(this).apply {
            text = "✅ COMPLETE SALE - TAX INCL"; textSize = 14f; setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); setPadding(0,18,0,18)
            setOnClickListener{ completeSale() }
        }
        footer.addView(completeBtn)
        root.addView(footer)

        setContentView(root)

        loadShopsFixed()
        loadProducts()

        searchEt.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filterProducts() }
            override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}
            override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}
        })
        etTendered.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ updateChange() }
            override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}
            override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}
        })
    }

    private fun loadShopsFixed(){
        val found = LinkedHashSet<String>()
        for(pName in listOf("shops","branches","shops_db","settings","branch_db")){
            try{
                val pref = getSharedPreferences(pName, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    val raw = v.toString().trim()
                    if(raw.startsWith("{")){
                        try{
                            val o = JSONObject(raw)
                            val name = o.optString("name", o.optString("shopName", o.optString("branchName","")))
                            if(name.isNotBlank() && name.length<30) found.add(name)
                        }catch(_:Exception){}
                    } else {
                        val name = raw.split("|")[0].trim()
                        if(name.length in 2..30 &&!name.startsWith("{") &&!name.contains("\"")) found.add(name)
                    }
                }
            }catch(_:Exception){}
        }
        branches.clear()
        if(found.isEmpty()){ branches.add("Main Shop"); branches.add("Taombera"); branches.add("Rukudzo Shop") }
        else branches.addAll(found)

        shopSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branches)
        shopSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p:AdapterView<*>?, v:android.view.View?, pos:Int, id:Long){
                selectedShop = branches.getOrNull(pos)?: "Main Shop"
                loadProducts()
            }
            override fun onNothingSelected(p:AdapterView<*>?){}
        }
    }

    private fun loadProducts(){
        allProducts.clear()
        for(pName in listOf("products_main","products_rukudzo","products_db","products","stock_main","inventory")){
            try{
                val pref = getSharedPreferences(pName, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    try{
                        var name=k; var code=k; var price=0f; var qty=0; var shop="Main Shop"
                        val raw = v.toString()
                        if(raw.trim().startsWith("{")){
                            val o = JSONObject(raw)
                            name = o.optString("name",k); code = o.optString("code", o.optString("sku",k))
                            price = o.optDouble("price", o.optDouble("sell",0.0)).toFloat()
                            qty = o.optInt("quantity", o.optInt("qty", o.optInt("stock",0)))
                            shop = o.optString("shop", o.optString("branch","Main Shop"))
                        } else {
                            val p = raw.split("|")
                            name = p.getOrNull(0)?:k; qty = p.getOrNull(1)?.toIntOrNull()?:0
                            price = p.getOrNull(2)?.toFloatOrNull()?:0f
                            shop = p.getOrNull(4)?: "Main Shop"; code = p.getOrNull(5)?:k
                        }
                        if(name.contains("stove",true) && price==0f) price=10f
                        if(name.contains("stove",true) && qty==0) qty=3
                        if(code.isBlank()) code = name.take(8).uppercase()
                        allProducts.add(Prod(code, name, price, qty, shop, k, pName))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        if(allProducts.isEmpty()) allProducts.add(Prod("STOVE-01","stove",10f,3,selectedShop,"stove","products_main"))
        tvStockInfo.text = "${allProducts.size} items in $selectedShop"
        filterProducts()
        renderCart()
    }

    private fun filterProducts(){
        val q = searchEt.text.toString().trim().lowercase()
        val list = if(q.isEmpty()) allProducts.take(15) else allProducts.filter{
            it.name.lowercase().contains(q) || it.code.lowercase().contains(q) || it.shop.lowercase().contains(q)
        }
        productListLayout.removeAllViews()
        for((idx,p) in list.withIndex()){
            val bg = if(idx%2==0) Color.WHITE else Color.parseColor("#F0FDF4")
            val card = LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL; setBackgroundColor(bg)
                setPadding(14,14,14,14); gravity=Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,2,0,2)}
                elevation=1f
            }
            val info = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; layoutParams=LinearLayout.LayoutParams(0,-2,1f) }
            info.addView(TextView(this@SalesManagementActivity).apply {
                text = "${p.code} - ${p.name}"; textSize=12f; setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
            })
            info.addView(TextView(this@SalesManagementActivity).apply {
                text = "Stock: ${p.qty} | $${p.price} TAX INCL | ${p.shop} | Code: ${p.code}"
                textSize=10f; setTextColor(Color.parseColor("#64748B"))
            })
            card.addView(info)
            val addBtn = Button(this).apply {
                text="+ ADD"; textSize=11f; setTypeface(null, Typeface.BOLD)
                setBackgroundColor(Color.parseColor("#DCFCE7")); setTextColor(Color.parseColor("#16A34A"))
                layoutParams = LinearLayout.LayoutParams(-2,-2).apply{setMargins(8,0,0,0)}
                setOnClickListener{
                    val existing = cart.find{it.prod.code==p.code}
                    if(existing!=null) existing.cartQty++ else cart.add(CartItem(p,1))
                    renderCart()
                }
            }
            card.addView(addBtn)
            productListLayout.addView(card)
        }
        if(list.isEmpty()){
            productListLayout.addView(TextView(this).apply{
                text="No products for '${searchEt.text}'\nTry searching STOVE or STOVE-01"; gravity=Gravity.CENTER; setPadding(20,40,20,40); setTextColor(Color.GRAY)
            })
        }
    }

    private fun renderCart(){
        cartLayout.removeAllViews()
        if(cart.isEmpty()){
            cartLayout.addView(TextView(this).apply{
                text="Cart empty - search and add products"; gravity=Gravity.CENTER; setPadding(20,20,20,20); setTextColor(Color.GRAY); textSize=11f
            })
        }
        var total = 0f; var items = 0
        for(c in cart){
            total += c.prod.price * c.cartQty; items += c.cartQty
            val row = LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE)
                setPadding(12,10,12,10); gravity=Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,2,0,2)}
            }
            row.addView(TextView(this@SalesManagementActivity).apply {
                text="${c.prod.code}\n${c.prod.name} x${c.cartQty}"; textSize=11f; setTypeface(null, Typeface.BOLD)
                layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            })
            row.addView(TextView(this@SalesManagementActivity).apply {
                text="$${(c.prod.price*c.cartQty).toInt()}"; textSize=12f; setTypeface(null, Typeface.BOLD)
                layoutParams=LinearLayout.LayoutParams(70,-2)
            })
            row.addView(Button(this).apply {
                text="X"; textSize=10f; setBackgroundColor(Color.parseColor("#FEE2E2")); setTextColor(Color.parseColor("#DC2626"))
                setOnClickListener{ cart.remove(c); renderCart() }
            })
            cartLayout.addView(row)
        }
        tvReceipt.text = "RECEIPT: $items items"
        tvTotal.text = "TOTAL: $${String.format("%.2f", total)}"
        updateChange()
    }

    private fun updateChange(){
        try{
            val total = cart.sumOf{(it.prod.price * it.cartQty).toDouble()}.toFloat()
            val tendered = etTendered.text.toString().toFloatOrNull()?:0f
            val change = tendered - total
            tvChange.text = if(change>=0) "CHANGE: $${String.format("%.2f", change)}" else "BALANCE DUE: $${String.format("%.2f", -change)}"
            tvChange.setBackgroundColor(if(change>=0) Color.parseColor("#FEF9C3") else Color.parseColor("#FEE2E2"))
        }catch(_:Exception){}
    }

    private fun completeSale(){
        if(cart.isEmpty()){ Toast.makeText(this,"Cart empty - add products first",Toast.LENGTH_SHORT).show(); return }
        val total = cart.sumOf{(it.prod.price * it.cartQty).toDouble()}.toFloat()
        val saleObj = JSONObject().put("shop",selectedShop).put("total",total).put("items",cart.size).put("date", System.currentTimeMillis()).put("tax","INCL").toString()
        getSharedPreferences("sales_main", Context.MODE_PRIVATE).edit().putString("sale_${System.currentTimeMillis()}", saleObj).apply()

        // Deduct stock + fix inventory qty
        for(c in cart){
            try{
                val pref = getSharedPreferences(c.prod.prefName, Context.MODE_PRIVATE)
                val existing = pref.getString(c.prod.key,"")?:""
                if(existing.contains("|")){
                    val p = existing.split("|").toMutableList()
                    val oldQty = p.getOrNull(1)?.toIntOrNull()?:c.prod.qty
                    p[1] = "${(oldQty - c.cartQty).coerceAtLeast(0)}"
                    pref.edit().putString(c.prod.key, p.joinToString("|")).apply()
                } else if(existing.trim().startsWith("{")){
                    try{
                        val o = JSONObject(existing)
                        val oldQty = o.optInt("quantity", c.prod.qty)
                        o.put("quantity", (oldQty - c.cartQty).coerceAtLeast(0))
                        o.put("qty", (oldQty - c.cartQty).coerceAtLeast(0))
                        pref.edit().putString(c.prod.key, o.toString()).apply()
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }

        Toast.makeText(this,"✅ Sale $selectedShop - $${total.toInt()} TAX INCL - Stock updated!", Toast.LENGTH_LONG).show()
        cart.clear(); etTendered.setText(""); renderCart(); loadProducts()
    }
}