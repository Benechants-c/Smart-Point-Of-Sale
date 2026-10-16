package com.smartpos
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class SalesActivity : Activity() {

    data class Product(val code:String, val name:String, var stock:Int, val price:Float, val cost:Float, val shop:String)
    data class CartItem(var product:Product, var qty:Int)

    var products = mutableListOf<Product>()
    var filtered = mutableListOf<Product>()
    var cart = mutableListOf<CartItem>()
    lateinit var productList: LinearLayout
    lateinit var receiptList: LinearLayout
    lateinit var tvSubtotal: TextView
    lateinit var tvTotal: TextView
    lateinit var etSearch: EditText
    var shopName = "Rukudzo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F1F5F9"))

        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#0F2D6B"))
        header.setPadding(16,20,16,20)
        header.addView(TextView(this).apply{ text="SMART POS - Cashier"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=14f })
        root.addView(header)

        val shopRow = LinearLayout(this)
        shopRow.setPadding(12,6,12,6)
        shopRow.addView(TextView(this).apply{ text="Shop: $shopName"; textSize=12f; setTypeface(null, Typeface.BOLD) })
        root.addView(shopRow)

        val searchBox = LinearLayout(this)
        searchBox.setBackgroundColor(Color.WHITE)
        searchBox.setPadding(12,8,12,8)
        searchBox.orientation = LinearLayout.HORIZONTAL
        searchBox.gravity = Gravity.CENTER_VERTICAL
        searchBox.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(12,4,12,8) }
        etSearch = EditText(this)
        etSearch.hint = "🔍 Search CODE or Name - see STOCK QTY"
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.layoutParams = LinearLayout.LayoutParams(-1,-2,1f)
        searchBox.addView(etSearch)
        root.addView(searchBox)

        root.addView(TextView(this).apply{ text="Products (with REAL STOCK QTY)"; setPadding(16,4,16,4); setTypeface(null, Typeface.BOLD); textSize=12f })

        val scrollProd = ScrollView(this)
        scrollProd.layoutParams = LinearLayout.LayoutParams(-1,0,1f)
        productList = LinearLayout(this)
        productList.orientation = LinearLayout.VERTICAL
        productList.setPadding(12,0,12,0)
        scrollProd.addView(productList)
        root.addView(scrollProd)

        val receiptContainer = LinearLayout(this)
        receiptContainer.setBackgroundColor(Color.parseColor("#FDE047"))
        receiptContainer.setPadding(12,12,12,12)
        receiptList = LinearLayout(this)
        receiptList.orientation = LinearLayout.VERTICAL
        receiptContainer.addView(TextView(this).apply{ text="Virtual Receipt"; setTypeface(null, Typeface.BOLD) })
        receiptContainer.addView(receiptList)

        val totalRow = LinearLayout(this)
        totalRow.orientation = LinearLayout.HORIZONTAL
        totalRow.addView(TextView(this).apply{ text="TOTAL"; setTypeface(null, Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        tvTotal = TextView(this).apply{ text="$0"; setTypeface(null, Typeface.BOLD); textSize=18f }
        tvSubtotal = TextView(this).apply{ text="$0" }
        totalRow.addView(tvTotal)
        receiptContainer.addView(totalRow)

        val btnComplete = Button(this)
        btnComplete.text = "COMPLETE SALE - Deduct Stock"
        btnComplete.setBackgroundColor(Color.parseColor("#16A34A"))
        btnComplete.setTextColor(Color.WHITE)
        btnComplete.setOnClickListener{ completeSale() }
        receiptContainer.addView(btnComplete)

        root.addView(receiptContainer)
        setContentView(root)

        loadRealProductsWithStock()
        filter("")

        etSearch.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter(s.toString()) }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    fun loadRealProductsWithStock(){
        products.clear()
        val prefNames = listOf("products","products_main","products_db","inventory","stock","products_list")

        for(prefName in prefNames){
            try{
                val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    try{
                        val o = JSONObject(v.toString())
                        val qty = when{
                            o.has("quantity") -> o.optInt("quantity",0)
                            o.has("qty") -> o.optInt("qty",0)
                            o.has("stock") -> o.optInt("stock",0)
                            o.has("current_stock") -> o.optInt("current_stock",0)
                            else -> o.optInt("quantity",0)
                        }
                        val code = o.optString("code", o.optString("sku",""))
                        val name = o.optString("name","")
                        val price = o.optDouble("price",0.0).toFloat()
                        val cost = o.optDouble("cost",0.0).toFloat()
                        val shop = o.optString("shop","Rukudzo")
                        if(name.isNotBlank()){
                            val ex = products.find{ it.code==code && code.isNotBlank() }
                            if(ex!=null) ex.stock = maxOf(ex.stock, qty) else products.add(Product(code,name,qty,price,cost,shop))
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
    }

    fun filter(q:String){
        val query = q.lowercase().trim()
        filtered = if(query.isBlank()) products else products.filter{
            it.code.lowercase().contains(query) || it.name.lowercase().contains(query)
        }.toMutableList()
        renderProducts()
    }

    fun renderProducts(){
        productList.removeAllViews()
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
            // STOCK QTY VISIBLE FOR CASHIER!
            val tvStock = TextView(this)
            tvStock.text = "STOCK: ${p.stock} | $${p.price} | ${p.shop}"
            tvStock.textSize = 11f
            tvStock.setTextColor(if(p.stock<=0) Color.RED else Color.parseColor("#16A34A"))
            tvStock.setTypeface(null, Typeface.BOLD)
            left.addView(tvStock)

            card.addView(left)

            val btn = Button(this)
            if(p.stock<=0){
                btn.text = "OUT OF STOCK"
                btn.setBackgroundColor(Color.parseColor("#9CA3AF"))
                btn.isEnabled = false
            } else {
                btn.text = "+ ADD (${p.stock})"
                btn.setBackgroundColor(Color.parseColor("#2563EB"))
                btn.setTextColor(Color.WHITE)
                btn.setOnClickListener{ addToCart(p) }
            }
            card.addView(btn)
            productList.addView(card)
        }
    }

    fun addToCart(p:Product){
        if(p.stock<=0){ Toast.makeText(this,"${p.name} OUT OF STOCK!",Toast.LENGTH_SHORT).show(); return }
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
        for(item in cart){
            total += item.product.price * item.qty
            val row = LinearLayout(this)
            row.addView(TextView(this).apply{ text="${item.product.name} x${item.qty} (Stock left: ${item.product.stock - item.qty}) - $${item.product.price*item.qty}"; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            receiptList.addView(row)
        }
        tvTotal.text = "$$total"
        tvSubtotal.text = "$$total"
    }

    fun completeSale(){
        if(cart.isEmpty()) return
        // Deduct stock from REAL storage
        for(item in cart){
            try{
                for(prefName in listOf("products","products_main","products_db","inventory")){
                    val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    val editor = pref.edit()
                    for((k,v) in pref.all){
                        try{
                            val o = JSONObject(v.toString())
                            val code = o.optString("code", o.optString("sku",""))
                            if(code==item.product.code || o.optString("name","")==item.product.name){
                                val currentQty = when{
                                    o.has("quantity") -> o.optInt("quantity",0)
                                    o.has("qty") -> o.optInt("qty",0)
                                    o.has("stock") -> o.optInt("stock",0)
                                    else -> 0
                                }
                                val newQty = (currentQty - item.qty).coerceAtLeast(0)
                                if(o.has("quantity")) o.put("quantity", newQty)
                                if(o.has("qty")) o.put("qty", newQty)
                                if(o.has("stock")) o.put("stock", newQty)
                                editor.putString(k, o.toString())
                            }
                        }catch(_:Exception){}
                    }
                    editor.apply()
                }
            }catch(_:Exception){}
        }
        Toast.makeText(this,"Sale Complete! Stock Deducted!",Toast.LENGTH_LONG).show()
        cart.clear()
        loadRealProductsWithStock()
        filter(etSearch.text.toString())
        renderReceipt()
    }
}