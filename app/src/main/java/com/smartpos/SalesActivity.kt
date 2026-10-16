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

    data class Product(val code:String, val name:String, var stock:Int, val price:Float, val shop:String)
    data class CartItem(var product:Product, var qty:Int)

    var products = mutableListOf<Product>()
    var filtered = mutableListOf<Product>()
    var cart = mutableListOf<CartItem>()
    lateinit var productList: LinearLayout
    lateinit var receiptList: LinearLayout
    lateinit var tvTotal: TextView
    lateinit var tvSubtotal: TextView
    lateinit var tvChange: TextView
    lateinit var etTendered: EditText
    lateinit var etSearch: EditText
    lateinit var tvCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F1F5F9"))

        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#0F2D6B"))
        header.setPadding(16,20,16,20)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        header.addView(TextView(this).apply{ text="🖨️ SMART POS"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=14f; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        header.addView(TextView(this).apply{ text="Cashier: John Mukucha\n${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}"; setTextColor(Color.WHITE); textSize=10f; gravity=Gravity.END })
        root.addView(header)

        val shopLabel = TextView(this)
        shopLabel.text = "SHOP"
        shopLabel.setPadding(16,10,16,2)
        shopLabel.textSize=10f
        root.addView(shopLabel)

        val spinnerShop = Spinner(this)
        spinnerShop.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("Main Shop","Rukudzo"))
        spinnerShop.setBackgroundColor(Color.WHITE)
        root.addView(spinnerShop, LinearLayout.LayoutParams(-1,-2).apply{ setMargins(12,0,12,8) })

        val searchBox = LinearLayout(this)
        searchBox.setBackgroundColor(Color.WHITE)
        searchBox.setPadding(12,8,12,8)
        searchBox.orientation = LinearLayout.HORIZONTAL
        searchBox.gravity = Gravity.CENTER_VERTICAL
        etSearch = EditText(this)
        etSearch.hint = "SEARCH CODE or Name"
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        val clear = TextView(this).apply{ text="X"; setPadding(12,0,12,0); setOnClickListener{ etSearch.text.clear() } }
        searchBox.addView(TextView(this).apply{ text="🔍" })
        searchBox.addView(etSearch)
        searchBox.addView(clear)
        root.addView(searchBox, LinearLayout.LayoutParams(-1,-2).apply{ setMargins(12,0,12,4) })

        tvCount = TextView(this)
        tvCount.setPadding(16,4,16,4)
        tvCount.textSize=11f
        tvCount.setTextColor(Color.parseColor("#2563EB"))
        root.addView(tvCount)

        root.addView(TextView(this).apply{ text="Products"; setPadding(16,4,16,4); setTypeface(null, Typeface.BOLD); textSize=12f })

        val scrollProd = ScrollView(this)
        scrollProd.layoutParams = LinearLayout.LayoutParams(-1,0,1f)
        productList = LinearLayout(this)
        productList.orientation = LinearLayout.VERTICAL
        productList.setPadding(12,0,12,0)
        scrollProd.addView(productList)
        root.addView(scrollProd)

        // RECEIPT
        val receiptContainer = LinearLayout(this)
        receiptContainer.setBackgroundColor(Color.parseColor("#FDE047"))
        receiptContainer.setPadding(12,12,12,12)
        receiptContainer.addView(TextView(this).apply{ text="Virtual Receipt"; setTypeface(null, Typeface.BOLD) })
        receiptList = LinearLayout(this)
        receiptList.orientation = LinearLayout.VERTICAL
        receiptContainer.addView(receiptList)
        
        val subRow = LinearLayout(this)
        subRow.addView(TextView(this).apply{ text="Subtotal"; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        tvSubtotal = TextView(this).apply{ text="$0" }
        subRow.addView(tvSubtotal)
        receiptContainer.addView(subRow)

        val totRow = LinearLayout(this)
        totRow.addView(TextView(this).apply{ text="TOTAL"; setTypeface(null, Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        tvTotal = TextView(this).apply{ text="$0"; setTypeface(null, Typeface.BOLD); textSize=16f }
        totRow.addView(tvTotal)
        receiptContainer.addView(totRow)

        val tendRow = LinearLayout(this)
        tendRow.addView(TextView(this).apply{ text="TENDERED:"; setTypeface(null, Typeface.BOLD) })
        etTendered = EditText(this)
        etTendered.hint = "$0"
        etTendered.setBackgroundColor(Color.WHITE)
        etTendered.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(8,0,0,0) }
        tendRow.addView(etTendered)
        receiptContainer.addView(tendRow)

        val chRow = LinearLayout(this)
        chRow.addView(TextView(this).apply{ text="CHANGE:"; setTypeface(null, Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        tvChange = TextView(this).apply{ text="$0"; setTypeface(null, Typeface.BOLD) }
        chRow.addView(tvChange)
        receiptContainer.addView(chRow)

        val btn = Button(this)
        btn.text = "COMPLETE SALE"
        btn.setBackgroundColor(Color.parseColor("#16A34A"))
        btn.setTextColor(Color.WHITE)
        btn.setOnClickListener{ completeSale() }
        receiptContainer.addView(btn)

        root.addView(receiptContainer)
        setContentView(root)

        loadRealProductsOnly()
        filter("")

        etSearch.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter(s.toString()) }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
        etTendered.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s:Editable?){ updateTotals() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    fun loadRealProductsOnly(){
        products.clear()
        // ONLY REAL USER PREFS - NO FAKE FALLBACK!
        val prefNames = listOf("products","products_main","products_db","inventory","stock","products_list","items","product_list")

        for(prefName in prefNames){
            try{
                val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    try{
                        val o = JSONObject(v.toString())
                        val qty = o.optInt("quantity", o.optInt("qty", o.optInt("stock",0)))
                        val code = o.optString("code", o.optString("sku",""))
                        val name = o.optString("name","")
                        val price = o.optDouble("price", o.optDouble("selling_price",0.0)).toFloat()
                        val shop = o.optString("shop","Main Shop")
                        if(name.isNotBlank()){
                            if(!products.any{ it.code==code && code.isNotBlank() }){
                                products.add(Product(code,name,qty,price,shop))
                            }
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        // NO FAKE DATA HERE! If empty, it stays empty!
    }

    fun filter(q:String){
        val query = q.trim().lowercase()
        filtered = if(query.isBlank()) products else products.filter{
            it.code.lowercase().contains(query) || it.name.lowercase().contains(query)
        }.toMutableList()
        renderProducts()
    }

    fun renderProducts(){
        productList.removeAllViews()
        tvCount.text = if(products.isEmpty()) "Products (0) - NO FAKE DATA" else "Products (${filtered.size}) - REAL USER DATA ONLY"

        if(products.isEmpty()){
            productList.addView(TextView(this).apply{
                text="\nNo products yet\n\nGo to Add Product screen\nAdd your real products first\n\nNO FAKE TR-01 / Travel Cup\nNO FAKE DATA"
                gravity = Gravity.CENTER
                setPadding(0,40,0,0)
                setTextColor(Color.GRAY)
                textSize=13f
            })
            return
        }

        for(p in filtered){
            val card = LinearLayout(this)
            card.setBackgroundColor(Color.WHITE)
            card.setPadding(12,10,12,10)
            card.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(0,0,0,8) }
            card.orientation = LinearLayout.HORIZONTAL
            card.gravity = Gravity.CENTER_VERTICAL

            val left = LinearLayout(this)
            left.orientation = LinearLayout.VERTICAL
            left.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
            left.addView(TextView(this).apply{ text="${p.code} ${p.name}"; setTypeface(null, Typeface.BOLD); textSize=12f })
            val stockColor = if(p.stock<=0) Color.RED else Color.parseColor("#16A34A")
            left.addView(TextView(this).apply{
                text="Stock ${p.stock} $${p.price} Code ${p.code} | ${p.shop}"
                textSize=11f
                setTextColor(stockColor)
                setTypeface(null, Typeface.BOLD)
            })
            card.addView(left)

            val btn = Button(this)
            if(p.stock<=0){
                btn.text = "OUT OF STOCK"
                btn.setBackgroundColor(Color.GRAY)
                btn.isEnabled = false
            } else {
                btn.text = "+ ADD"
                btn.setOnClickListener{ addToCart(p) }
            }
            card.addView(btn)
            productList.addView(card)
        }
    }

    fun addToCart(p:Product){
        if(p.stock<=0) return
        val ex = cart.find{ it.product.code==p.code }
        if(ex!=null){
            if(ex.qty >= p.stock){ Toast.makeText(this,"Only ${p.stock} left!",Toast.LENGTH_SHORT).show(); return }
            ex.qty++
        } else cart.add(CartItem(p,1))
        renderReceipt()
    }

    fun renderReceipt(){
        receiptList.removeAllViews()
        var total = 0f
        for(it in cart){
            total += it.product.price * it.qty
            receiptList.addView(TextView(this).apply{ text="${it.product.code} ${it.product.name} x${it.qty} = $${it.product.price*it.qty}" })
        }
        tvSubtotal.text = "$$total"
        tvTotal.text = "$$total"
        updateTotals()
    }

    fun updateTotals(){
        val total = cart.sumOf{ (it.product.price*it.qty).toDouble() }.toFloat()
        val tendered = etTendered.text.toString().toFloatOrNull()?:0f
        tvChange.text = "$${if(tendered>=total) tendered-total else 0f}"
    }

    fun completeSale(){
        if(cart.isEmpty()){ Toast.makeText(this,"Cart empty",Toast.LENGTH_SHORT).show(); return }
        Toast.makeText(this,"Sale Complete - Stock deducted",Toast.LENGTH_SHORT).show()
        // Deduct real stock
        for(item in cart){
            for(prefName in listOf("products","products_main","products_db")){
                try{
                    val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    val editor = pref.edit()
                    for((k,v) in pref.all){
                        try{
                            val o = JSONObject(v.toString())
                            if(o.optString("code","")==item.product.code){
                                val cur = o.optInt("quantity", o.optInt("qty",0))
                                if(o.has("quantity")) o.put("quantity", cur - item.qty)
                                if(o.has("qty")) o.put("qty", cur - item.qty)
                                if(o.has("stock")) o.put("stock", cur - item.qty)
                                editor.putString(k, o.toString())
                            }
                        }catch(_:Exception){}
                    }
                    editor.apply()
                }catch(_:Exception){}
            }
        }
        cart.clear()
        loadRealProductsOnly()
        filter(etSearch.text.toString())
        renderReceipt()
    }
}