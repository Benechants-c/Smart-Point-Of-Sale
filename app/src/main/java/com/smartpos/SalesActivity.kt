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
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class SalesActivity : Activity() {

    data class Product(val code:String, val name:String, val desc:String, val stock:Int, val price:Float)
    data class CartItem(var product:Product, var qty:Int)

    var products = mutableListOf<Product>()
    var filtered = mutableListOf<Product>()
    var cart = mutableListOf<CartItem>()
    lateinit var productList: LinearLayout
    lateinit var receiptList: LinearLayout
    lateinit var tvCashier: TextView
    lateinit var tvDateTime: TextView
    lateinit var tvSubtotal: TextView
    lateinit var tvTotal: TextView
    lateinit var tvChange: TextView
    lateinit var etTendered: EditText
    lateinit var etSearch: EditText
    lateinit var tvReceiptNum: TextView
    lateinit var tvSearchCount: TextView
    val handler = Handler(Looper.getMainLooper())
    var shopName = "Main Shop"

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

        val leftH = LinearLayout(this)
        leftH.orientation = LinearLayout.HORIZONTAL
        leftH.gravity = Gravity.CENTER_VERTICAL
        leftH.addView(TextView(this).apply{ text="🖨️"; textSize=22f })
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(8,0,0,0)
        col.addView(TextView(this).apply{ text="SMART"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=18f })
        col.addView(TextView(this).apply{ text="POS"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=18f })
        leftH.addView(col)
        header.addView(leftH, LinearLayout.LayoutParams(0,-2,1f))

        val rightH = LinearLayout(this)
        rightH.orientation = LinearLayout.VERTICAL
        rightH.gravity = Gravity.END
        tvCashier = TextView(this)
        tvCashier.text = "Cashier: ${loadCashierName()}"
        tvCashier.setTextColor(Color.WHITE)
        tvCashier.setTypeface(null, Typeface.BOLD)
        tvCashier.textSize = 11f
        tvCashier.gravity = Gravity.END
        tvDateTime = TextView(this)
        tvDateTime.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        tvDateTime.setTextColor(Color.parseColor("#CBD5E1"))
        tvDateTime.textSize = 11f
        tvDateTime.gravity = Gravity.END
        rightH.addView(tvCashier)
        rightH.addView(tvDateTime)
        header.addView(rightH)
        root.addView(header)

        handler.post(object: Runnable{
            override fun run(){
                tvDateTime.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
                handler.postDelayed(this, 1000)
            }
        })

        val shopLabel = TextView(this)
        shopLabel.text = "SHOP"
        shopLabel.setPadding(16,12,16,0)
        shopLabel.textSize = 11f
        shopLabel.setTextColor(Color.GRAY)
        shopLabel.setTypeface(null, Typeface.BOLD)
        root.addView(shopLabel)

        val spinnerShop = Spinner(this)
        val shops = loadShops()
        spinnerShop.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shops)
        spinnerShop.setBackgroundColor(Color.WHITE)
        spinnerShop.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a:AdapterView<*>?, v:View?, p:Int, id:Long){ shopName = shops[p] }
            override fun onNothingSelected(a:AdapterView<*>?){}
        }
        root.addView(spinnerShop, LinearLayout.LayoutParams(-1,-2).apply{ setMargins(12,4,12,8) })

        val searchBox = LinearLayout(this)
        searchBox.setBackgroundColor(Color.WHITE)
        searchBox.setPadding(0,8,0,8)
        searchBox.orientation = LinearLayout.HORIZONTAL
        searchBox.gravity = Gravity.CENTER_VERTICAL
        searchBox.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(12,0,12,4) }
        searchBox.addView(TextView(this).apply{ text="🔍"; setPadding(16,0,0,0) })
        etSearch = EditText(this)
        etSearch.hint = "SEARCH CODE or Name tr"
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        etSearch.setPadding(12,16,12,16)
        val clearBtn = TextView(this)
        clearBtn.text = "X"
        clearBtn.setPadding(16,0,16,0)
        clearBtn.setOnClickListener{ etSearch.text.clear() }
        searchBox.addView(etSearch)
        searchBox.addView(clearBtn)
        root.addView(searchBox)

        tvSearchCount = TextView(this)
        tvSearchCount.setPadding(16,4,16,0)
        tvSearchCount.textSize = 11f
        tvSearchCount.setTextColor(Color.parseColor("#2563EB"))
        root.addView(tvSearchCount)

        etSearch.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s:Editable?){ filterProducts(s.toString()) }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })

        root.addView(TextView(this).apply{ text="Products"; setPadding(16,10,16,4); setTypeface(null, Typeface.BOLD); textSize=13f })

        val scrollProd = ScrollView(this)
        scrollProd.layoutParams = LinearLayout.LayoutParams(-1,0,1f)
        productList = LinearLayout(this)
        productList.orientation = LinearLayout.VERTICAL
        productList.setPadding(12,0,12,0)
        scrollProd.addView(productList)
        root.addView(scrollProd)

        val receiptContainer = LinearLayout(this)
        receiptContainer.orientation = LinearLayout.VERTICAL
        receiptContainer.setBackgroundColor(Color.parseColor("#FDE047"))
        receiptContainer.setPadding(16,14,16,14)
        receiptContainer.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(8,8,8,8) }

        val receiptHeader = LinearLayout(this)
        receiptHeader.orientation = LinearLayout.HORIZONTAL
        receiptHeader.gravity = Gravity.CENTER_VERTICAL
        receiptHeader.addView(TextView(this).apply{ text="Virtual Receipt"; setTypeface(null, Typeface.BOLD); textSize=15f; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        tvReceiptNum = TextView(this)
        tvReceiptNum.text = "Receipt #${(100..999).random()}"
        tvReceiptNum.textSize = 12f
        receiptHeader.addView(tvReceiptNum)
        receiptContainer.addView(receiptHeader)
        receiptContainer.addView(View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1).apply{ setMargins(0,10,0,8) }; setBackgroundColor(Color.parseColor("#EAB308")) })

        receiptList = LinearLayout(this)
        receiptList.orientation = LinearLayout.VERTICAL
        receiptContainer.addView(receiptList)
        receiptContainer.addView(View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1).apply{ setMargins(0,8,0,8) }; setBackgroundColor(Color.parseColor("#EAB308")) })

        val subRow = LinearLayout(this)
        subRow.orientation = LinearLayout.HORIZONTAL
        subRow.addView(TextView(this).apply{ text="Subtotal"; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        tvSubtotal = TextView(this)
        tvSubtotal.text = "$0"
        tvSubtotal.setTypeface(null, Typeface.BOLD)
        subRow.addView(tvSubtotal)
        receiptContainer.addView(subRow)

        val totalRow = LinearLayout(this)
        totalRow.orientation = LinearLayout.HORIZONTAL
        totalRow.addView(TextView(this).apply{ text="TOTAL"; setTypeface(null, Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        tvTotal = TextView(this)
        tvTotal.text = "$0"
        tvTotal.setTypeface(null, Typeface.BOLD)
        tvTotal.textSize = 18f
        totalRow.addView(tvTotal)
        receiptContainer.addView(totalRow)

        val tendRow = LinearLayout(this)
        tendRow.orientation = LinearLayout.HORIZONTAL
        tendRow.setPadding(0,8,0,4)
        tendRow.addView(TextView(this).apply{ text="TENDERED:"; setTypeface(null, Typeface.BOLD) })
        etTendered = EditText(this)
        etTendered.hint = "$0"
        etTendered.setBackgroundColor(Color.WHITE)
        etTendered.setPadding(12,8,12,8)
        etTendered.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(12,0,0,0) }
        etTendered.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        tendRow.addView(etTendered)
        receiptContainer.addView(tendRow)

        val changeRow = LinearLayout(this)
        changeRow.orientation = LinearLayout.HORIZONTAL
        changeRow.addView(TextView(this).apply{ text="CHANGE:"; setTypeface(null, Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        tvChange = TextView(this)
        tvChange.text = "$0"
        tvChange.setTypeface(null, Typeface.BOLD)
        changeRow.addView(tvChange)
        receiptContainer.addView(changeRow)

        val btnComplete = Button(this)
        btnComplete.text = "COMPLETE SALE"
        btnComplete.setBackgroundColor(Color.parseColor("#16A34A"))
        btnComplete.setTextColor(Color.WHITE)
        btnComplete.setTypeface(null, Typeface.BOLD)
        btnComplete.setOnClickListener{ completeSale() }
        receiptContainer.addView(btnComplete)

        root.addView(receiptContainer)
        setContentView(root)
        loadProducts()
        filterProducts("")
        etTendered.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s:Editable?){ updateTotals() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    fun loadCashierName(): String{
        try{
            val pref = getSharedPreferences("login_data", Context.MODE_PRIVATE)
            val n = pref.getString("cashier_name","") ?: pref.getString("username","")
            if(!n.isNullOrBlank()) return n
        }catch(_:Exception){}
        return "John Mukucha"
    }

    fun loadShops(): List<String>{
        return listOf("Main Shop","Rukudzo Shop")
    }

    fun loadProducts(){
        products.clear()
        try{
            val pref = getSharedPreferences("products_main", Context.MODE_PRIVATE)
            for((_,v) in pref.all){
                try{
                    val o = JSONObject(v.toString())
                    val code = o.optString("code", o.optString("sku",""))
                    val name = o.optString("name","")
                    val desc = o.optString("description","")
                    val stock = o.optInt("quantity",0)
                    val price = o.optDouble("price",8.0).toFloat()
                    if(name.isNotBlank()) products.add(Product(code,name,desc,stock,price))
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        if(products.isEmpty()){
            products.add(Product("TR-01","Travel Cup","stainless steel",10,8f))
            products.add(Product("TR-02","Tray Large","plastic",5,12f))
            products.add(Product("STOVE-01","Stove","2 burner",3,8f))
        }
    }

    fun filterProducts(q:String){
        val query = q.trim().lowercase()
        filtered = if(query.isBlank()) products else products.filter{ p->
            p.code.lowercase().startsWith(query) || p.name.lowercase().startsWith(query) || p.code.lowercase().contains(query) || p.name.lowercase().contains(query)
        }.toMutableList()
        tvSearchCount.text = if(query.isBlank()) "Products (${filtered.size})" else "${filtered.size} results for \"$q\""
        renderProducts()
    }

    fun renderProducts(){
        productList.removeAllViews()
        for(p in filtered){
            val card = LinearLayout(this)
            card.orientation = LinearLayout.HORIZONTAL
            card.setBackgroundColor(Color.WHITE)
            card.setPadding(14,12,14,12)
            card.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(0,0,0,8) }
            card.gravity = Gravity.CENTER_VERTICAL
            val left = LinearLayout(this)
            left.orientation = LinearLayout.VERTICAL
            left.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
            left.addView(TextView(this).apply{ text="${p.code} ${p.name}"; setTypeface(null, Typeface.BOLD) })
            left.addView(TextView(this).apply{ text="Stock ${p.stock} $${p.price} Code ${p.code}"; setTextColor(Color.GRAY); textSize=11f })
            card.addView(left)
            val btn = Button(this)
            btn.text = "+ ADD"
            btn.setOnClickListener{ addToCart(p) }
            card.addView(btn)
            productList.addView(card)
        }
    }

    fun addToCart(p:Product){
        val ex = cart.find{ it.product.code==p.code }
        if(ex!=null) ex.qty++ else cart.add(CartItem(p,1))
        renderReceipt()
    }

    fun renderReceipt(){
        receiptList.removeAllViews()
        for((idx,item) in cart.withIndex()){
            val row = LinearLayout(this)
            row.orientation = LinearLayout.VERTICAL
            row.setPadding(0,8,0,8)
            val top = LinearLayout(this)
            top.orientation = LinearLayout.HORIZONTAL
            top.addView(TextView(this).apply{ text="${idx+1}. ${item.product.code} ${item.product.name} x${item.qty}"; setTypeface(null, Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            top.addView(TextView(this).apply{ text="$${(item.product.price*item.qty).toInt()}"; setTypeface(null, Typeface.BOLD) })
            row.addView(top)
            val controls = LinearLayout(this)
            controls.orientation = LinearLayout.HORIZONTAL
            controls.gravity = Gravity.END
            val btnMinus = Button(this)
            btnMinus.text = "-"
            btnMinus.setOnClickListener{ if(item.qty>1) item.qty-- else cart.removeAt(idx); renderReceipt() }
            val tvQty = TextView(this)
            tvQty.text = " ${item.qty} "
            tvQty.setPadding(14,6,14,6)
            tvQty.setBackgroundColor(Color.WHITE)
            val btnPlus = Button(this)
            btnPlus.text = "+"
            btnPlus.setOnClickListener{ item.qty++; renderReceipt() }
            val btnDel = TextView(this)
            btnDel.text = " 🗑️"
            btnDel.setOnClickListener{ cart.removeAt(idx); renderReceipt() }
            controls.addView(btnMinus); controls.addView(tvQty); controls.addView(btnPlus); controls.addView(btnDel)
            row.addView(controls)
            receiptList.addView(row)
        }
        updateTotals()
    }

    fun updateTotals(){
        val total = cart.sumOf{ (it.product.price*it.qty).toDouble() }.toFloat()
        tvSubtotal.text = "$${total.toInt()}"
        tvTotal.text = "$${total.toInt()}"
        val tendered = etTendered.text.toString().toFloatOrNull()?:0f
        tvChange.text = "$${(if(tendered>=total) tendered-total else 0f).toInt()}"
    }

    fun completeSale(){
        if(cart.isEmpty()){ Toast.makeText(this,"Cart empty",Toast.LENGTH_SHORT).show(); return }
        val total = cart.sumOf{ (it.product.price*it.qty).toDouble() }.toFloat()
        val tendered = etTendered.text.toString().toFloatOrNull()?:0f
        val change = if(tendered>=total) tendered-total else 0f
        val receiptNum = "Receipt #${(100..999).random()}"
        try{
            val saleId = "SALE-${System.currentTimeMillis()}"
            val obj = JSONObject()
            obj.put("id",saleId); obj.put("date",System.currentTimeMillis()); obj.put("total",total)
            obj.put("cashier",loadCashierName()); obj.put("receipt",receiptNum); obj.put("shop",shopName)
            val arr = org.json.JSONArray()
            for(it in cart){ val jo=JSONObject(); jo.put("code",it.product.code); jo.put("name",it.product.name); jo.put("qty",it.qty); jo.put("price",it.product.price); arr.put(jo) }
            obj.put("items",arr)
            getSharedPreferences("sales_main", Context.MODE_PRIVATE).edit().putString(saleId,obj.toString()).apply()
        }catch(_:Exception){}
        showReceipt(receiptNum, total, tendered, change)
    }

    fun showReceipt(receiptNum:String, total:Float, tendered:Float, change:Float){
        val dialog = Dialog(this)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#FDE047"))
        root.setPadding(16,16,16,16)
        val paper = LinearLayout(this)
        paper.orientation = LinearLayout.VERTICAL
        paper.setBackgroundColor(Color.WHITE)
        paper.setPadding(24,24,24,24)
        paper.addView(TextView(this).apply{ text="SMART POS RECEIPT\n$receiptNum\n${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\nCashier ${loadCashierName()}\nShop $shopName\n"; gravity=Gravity.CENTER; setTypeface(null, Typeface.BOLD) })
        for((idx,item) in cart.withIndex()){
            paper.addView(TextView(this).apply{ text="${idx+1}. ${item.product.code} ${item.product.name} Code ${item.product.code} x${item.qty} @ $${item.product.price.toInt()} = $${(item.product.price*item.qty).toInt()}"; textSize=11f })
        }
        paper.addView(TextView(this).apply{ text="\nTOTAL $${total.toInt()}\nTendered $${tendered.toInt()}\nChange $${change.toInt()}\n\nThank you!"; setTypeface(null, Typeface.BOLD); gravity=Gravity.CENTER; setPadding(0,12,0,0) })
        root.addView(paper)
        val btn = Button(this)
        btn.text = "New Sale"
        btn.setBackgroundColor(Color.parseColor("#0F2D6B"))
        btn.setTextColor(Color.WHITE)
        btn.setOnClickListener{ cart.clear(); renderReceipt(); etTendered.text.clear(); dialog.dismiss(); tvReceiptNum.text="Receipt #${(100..999).random()}" }
        root.addView(btn)
        dialog.setContentView(root)
        dialog.show()
    }
}