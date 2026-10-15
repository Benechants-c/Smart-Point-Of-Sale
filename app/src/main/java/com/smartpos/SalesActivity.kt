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

        // HEADER - CASHIER + DATE TIME TOP RIGHT - NO TAX!
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
        tvCashier.text = "👤 Cashier: ${loadCashierName()}"
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

        // SHOP DROPDOWN
        val shopLabel = TextView(this)
        shopLabel.text = "🏬 SHOP"
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

        // SEARCH - PREFIX SEARCH tr -> TR-01, searchable by CODE too!
        val searchBox = LinearLayout(this)
        searchBox.setBackgroundColor(Color.WHITE)
        searchBox.setPadding(0,8,0,8)
        searchBox.orientation = LinearLayout.HORIZONTAL
        searchBox.gravity = Gravity.CENTER_VERTICAL
        searchBox.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(12,0,12,4) }
        searchBox.elevation = 2f
        searchBox.addView(TextView(this).apply{ text="🔍"; setPadding(16,0,0,0); textSize=16f })
        etSearch = EditText(this)
        etSearch.hint = "SEARCH CODE or Name (e.g. tr)"
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        etSearch.setPadding(12,16,12,16)
        val clearBtn = TextView(this)
        clearBtn.text = "✕"
        clearBtn.setPadding(16,0,16,0)
        clearBtn.textSize = 16f
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

        root.addView(TextView(this).apply{ text="Products"; setPadding(16,10,16,4); setTypeface(null, Typeface.BOLD); textSize=13f; setTextColor(Color.GRAY) })

        // PRODUCT LIST - NO PHOTOS, ONLY CODE + DESC
        val scrollProd = ScrollView(this)
        scrollProd.layoutParams = LinearLayout.LayoutParams(-1,0,1f)
        productList = LinearLayout(this)
        productList.orientation = LinearLayout.VERTICAL
        productList.setPadding(12,0,12,0)
        scrollProd.addView(productList)
        root.addView(scrollProd)

        // VIRTUAL RECEIPT - YELLOW - WITH + - BUTTONS!
        val receiptContainer = LinearLayout(this)
        receiptContainer.orientation = LinearLayout.VERTICAL
        receiptContainer.setBackgroundColor(Color.parseColor("#FDE047"))
        receiptContainer.setPadding(16,14,16,14)
        receiptContainer.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(8,8,8,8) }
        receiptContainer.elevation = 4f

        val receiptHeader = LinearLayout(this)
        receiptHeader.orientation = LinearLayout.HORIZONTAL
        receiptHeader.gravity = Gravity.CENTER_VERTICAL
        receiptHeader.addView(TextView(this).apply{ text="🧾 Virtual Receipt"; setTypeface(null, Typeface.BOLD); textSize=15f; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
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
        subRow.gravity = Gravity.CENTER_VERTICAL
        subRow.addView(TextView(this).apply{ text="Subtotal"; textSize=13f; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        tvSubtotal = TextView(this)
        tvSubtotal.text = "$0"
        tvSubtotal.setTypeface(null, Typeface.BOLD)
        subRow.addView(tvSubtotal)
        receiptContainer.addView(subRow)

        val totalRow = LinearLayout(this)
        totalRow.orientation = LinearLayout.HORIZONTAL
        totalRow.gravity = Gravity.CENTER_VERTICAL
        totalRow.addView(TextView(this).apply{ text="TOTAL"; setTypeface(null, Typeface.BOLD); textSize=16f; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        tvTotal = TextView(this)
        tvTotal.text = "$0"
        tvTotal.setTypeface(null, Typeface.BOLD)
        tvTotal.textSize = 18f
        totalRow.addView(tvTotal)
        receiptContainer.addView(totalRow)

        val tendRow = LinearLayout(this)
        tendRow.orientation = LinearLayout.HORIZONTAL
        tendRow.gravity = Gravity.CENTER_VERTICAL
        tendRow.setPadding(0,8,0,4)
        tendRow.addView(TextView(this).apply{ text="TENDERED:"; textSize=12f; setTypeface(null, Typeface.BOLD) })
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
        changeRow.gravity = Gravity.CENTER_VERTICAL
        changeRow.addView(TextView(this).apply{ text="CHANGE:"; setTypeface(null, Typeface.BOLD); textSize=12f; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        tvChange = TextView(this)
        tvChange.text = "$0"
        tvChange.setTypeface(null, Typeface.BOLD)
        tvChange.textSize = 16f
        changeRow.addView(tvChange)
        receiptContainer.addView(changeRow)

        val btnComplete = Button(this)
        btnComplete.text = "✅ COMPLETE SALE"
        btnComplete.setBackgroundColor(Color.parseColor("#16A34A"))
        btnComplete.setTextColor(Color.WHITE)
        btnComplete.setTypeface(null, Typeface.BOLD)
        btnComplete.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(0,12,0,0) }
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
            val sess = getSharedPreferences("user_session", Context.MODE_PRIVATE)
            for((_,v) in sess.all){
                try{
                    val o = JSONObject(v.toString())
                    val name = o.optString("name","")
                    if(name.isNotBlank()) return name
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        return "John Mukucha"
    }

    fun loadShops(): List<String>{
        val list = mutableListOf("Main Shop")
        for(pn in listOf("shops","branches","shops_db")){
            try{
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    try{
                        val o = JSONObject(v.toString())
                        val n = o.optString("name","")
                        if(n.isNotBlank() && !list.contains(n)) list.add(n)
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        return list
    }

    fun loadProducts(){
        products.clear()
        try{
            val pref = getSharedPreferences("products_main", Context.MODE_PRIVATE)
            for((_,v) in pref.all){
                try{
                    var code=""; var name=""; var desc=""; var stock=0; var price=8f
                    val raw=v.toString()
                    if(raw.trim().startsWith("{")){
                        val o=JSONObject(raw)
                        code=o.optString("code",o.optString("sku",""))
                        name=o.optString("name","")
                        desc=o.optString("description",o.optString("desc",""))
                        stock=o.optInt("quantity",0)
                        price=o.optDouble("price",8.0).toFloat()
                        if(code.isBlank()) code=name.replace(" ","-").uppercase()
                        if(desc.isBlank()) desc=name
                    } else {
                        val p=raw.split("|")
                        name=p.getOrNull(0)?:""
                        stock=p.getOrNull(1)?.toIntOrNull()?:0
                        price=p.getOrNull(3)?.toFloatOrNull()?:8f
                        code=p.getOrNull(5)?:name.uppercase()
                        desc=p.getOrNull(6)?:""
                    }
                    if(name.isNotBlank()) products.add(Product(code,name,desc,stock,price))
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        if(products.isEmpty()){
            products.add(Product("TR-01","Travel Cup","stainless steel",10,8f))
            products.add(Product("TR-02","Tray Large","plastic serving tray",5,12f))
            products.add(Product("TRUCK-01","Truck Toy","Kids toy",2,8f))
            products.add(Product("STOVE-01","Portable Gas Stove","2 burner portable",3,8f))
        }
    }

    // SEARCHABLE BY CODE TOO! PREFIX tr -> TR-01
    fun filterProducts(q:String){
        val query = q.trim().lowercase()
        filtered = if(query.isBlank()){
            products
        } else {
            products.filter{ p->
                val codeLower = p.code.lowercase()
                val nameLower = p.name.lowercase()
                val descLower = p.desc.lowercase()
                codeLower.startsWith(query) ||
                nameLower.startsWith(query) ||
                descLower.startsWith(query) ||
                codeLower.contains(query) ||
                nameLower.contains(query)
            }.sortedWith(compareBy(
                { !it.code.lowercase().startsWith(query) },
                { !it.name.lowercase().startsWith(query) },
                { it.code }
            )).toMutableList()
        }
        tvSearchCount.text = if(query.isBlank()) "Products (${filtered.size})" else "${filtered.size} results for \"$q\" • search by CODE or Name"
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
            card.elevation = 2f

            val left = LinearLayout(this)
            left.orientation = LinearLayout.VERTICAL
            left.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
            left.addView(TextView(this).apply{ text="${p.code} ${p.name}"; setTypeface(null, Typeface.BOLD); textSize=13f })
            left.addView(TextView(this).apply{ text="$${String.format("%.2f", p.price)} • Stock ${p.stock} • Code ${p.code}"; setTextColor(Color.GRAY); textSize=11f })
            left.addView(TextView(this).apply{ text="Description: ${p.desc}"; setTextColor(Color.parseColor("#6B7280")); textSize=10f })
            card.addView(left)

            val btn = Button(this)
            btn.text = "+ ADD"
            btn.setBackgroundColor(Color.WHITE)
            btn.setTextColor(Color.parseColor("#2563EB"))
            btn.textSize = 12f
            btn.setTypeface(null, Typeface.BOLD)
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
            top.gravity = Gravity.CENTER_VERTICAL
            top.addView(TextView(this).apply{
                text="${idx+1}. ${item.product.code} ${item.product.name} x${item.qty}"
                setTypeface(null, Typeface.BOLD)
                textSize=13f
                layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            })
            top.addView(TextView(this).apply{
                text="$${(item.product.price*item.qty).toInt()}"
                setTypeface(null, Typeface.BOLD)
                textSize=14f
            })
            row.addView(top)

            // + - BUTTONS TO INCREASE / SUBTRACT
            val controls = LinearLayout(this)
            controls.orientation = LinearLayout.HORIZONTAL
            controls.gravity = Gravity.END
            controls.setPadding(0,6,0,0)

            val btnMinus = Button(this)
            btnMinus.text = "⊖"
            btnMinus.textSize = 14f
            btnMinus.layoutParams = LinearLayout.LayoutParams(-2,-2).apply{ setMargins(0,0,6,0) }
            btnMinus.setOnClickListener{
                if(item.qty>1){ item.qty--; renderReceipt() } else { cart.removeAt(idx); renderReceipt() }
            }

            val tvQty = TextView(this)
            tvQty.text = "${item.qty}"
            tvQty.setPadding(14,6,14,6)
            tvQty.setBackgroundColor(Color.WHITE)
            tvQty.setTypeface(null, Typeface.BOLD)
            tvQty.textSize = 12f
            tvQty.layoutParams = LinearLayout.LayoutParams(-2,-2).apply{ setMargins(0,0,6,0) }

            val btnPlus = Button(this)
            btnPlus.text = "⊕"
            btnPlus.textSize = 14f
            btnPlus.layoutParams = LinearLayout.LayoutParams(-2,-2).apply{ setMargins(0,0,8,0) }
            btnPlus.setOnClickListener{ item.qty++; renderReceipt() }

            val btnDel = TextView(this)
            btnDel.text = "🗑️"
            btnDel.textSize = 16f
            btnDel.setPadding(8,0,0,0)
            btnDel.setOnClickListener{ cart.removeAt(idx); renderReceipt() }

            controls.addView(btnMinus)
            controls.addView(tvQty)
            controls.addView(btnPlus)
            controls.addView(btnDel)
            row.addView(controls)
            receiptList.addView(row)
            receiptList.addView(View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1).apply{ setMargins(0,6,0,0) }; setBackgroundColor(Color.parseColor("#EAB308")) })
        }
        updateTotals()
    }

    fun updateTotals(){
        val total = cart.sumOf{ (it.product.price*it.qty).toDouble() }.toFloat()
        tvSubtotal.text = "$${total.toInt()}"
        tvTotal.text = "$${total.toInt()}"
        val tendered = etTendered.text.toString().replace("$","").toFloatOrNull()?:0f
        val change = tendered-total
        tvChange.text = "$${if(change<0) 0 else change.toInt()}"
    }

    fun completeSale(){
        if(cart.isEmpty()){ Toast.makeText(this,"Cart empty - add product",Toast.LENGTH_SHORT).show(); return }
        val total = cart.sumOf{ (it.product.price*it.qty).toDouble() }.toFloat()
        val tendered = etTendered.text.toString().replace("$","").toFloatOrNull()?:0f
        val change = if(tendered>=total) tendered-total else 0f
        val receiptNum = "Receipt #${(100..999).random()}"

        try{
            val saleId = "SALE-${System.currentTimeMillis()}"
            val obj = JSONObject()
            obj.put("id",saleId)
            obj.put("date",System.currentTimeMillis())
            obj.put("total",total)
            obj.put("tendered",tendered)
            obj.put("change",change)
            obj.put("cashier",loadCashierName())
            obj.put("receipt",receiptNum)
            obj.put("shop",shopName)
            val arr = org.json.JSONArray()
            for(it in cart){
                val jo=JSONObject()
                jo.put("code",it.product.code)
                jo.put("name",it.product.name)
                jo.put("qty",it.qty)
                jo.put("price",it.product.price)
                arr.put(jo)
            }
            obj.put("items",arr)
            getSharedPreferences("sales_main", Context.MODE_PRIVATE).edit().putString(saleId,obj.toString()).apply()
        }catch(_:Exception){}

        showReceiptOnScreen(receiptNum, total, tendered, change)
    }

    fun showReceiptOnScreen(receiptNum:String, total:Float, tendered:Float, change:Float){
        val dialog = Dialog(this)
        dialog.setCancelable(false)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#FDE047"))
        root.setPadding(16,16,16,16)

        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#1E293B"))
        header.setPadding(16,14,16,14)
        header.orientation = LinearLayout.HORIZONTAL
        header.addView(TextView(this).apply{ text="SMART POS"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        header.addView(TextView(this).apply{ text="Cashier\n${loadCashierName()}\n${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}"; setTextColor(Color.WHITE); textSize=10f; gravity=Gravity.END })
        root.addView(header)

        val paper = LinearLayout(this)
        paper.orientation = LinearLayout.VERTICAL
        paper.setBackgroundColor(Color.WHITE)
        paper.setPadding(24,24,24,24)
        paper.elevation = 8f
        paper.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(16,20,16,16) }

        paper.addView(TextView(this).apply{ text="SMART POS\nRECEIPT"; gravity=Gravity.CENTER; setTypeface(null, Typeface.BOLD); textSize=16f; setPadding(0,0,0,12) })
        paper.addView(TextView(this).apply{ text="----------------------------------------"; gravity=Gravity.CENTER; textSize=10f })

        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        paper.addView(TextView(this).apply{ text="$receiptNum\nDate     $dateStr\nCashier  ${loadCashierName()}\nShop     $shopName"; textSize=11f; typeface=Typeface.MONOSPACE; setPadding(0,8,0,8) })
        paper.addView(TextView(this).apply{ text="----------------------------------------"; gravity=Gravity.CENTER; textSize=10f })
        paper.addView(TextView(this).apply{ text="ITEMS:"; setTypeface(null, Typeface.BOLD); textSize=12f; typeface=Typeface.MONOSPACE; setPadding(0,8,0,4) })

        for((idx,item) in cart.withIndex()){
            paper.addView(TextView(this).apply{ text="${idx+1}. ${item.product.code} ${item.product.name}  Code ${item.product.code}"; textSize=10f; typeface=Typeface.MONOSPACE })
            paper.addView(TextView(this).apply{ text="   x${item.qty} @ $${item.product.price.toInt()}              = $${(item.product.price*item.qty).toInt()}"; textSize=10f; typeface=Typeface.MONOSPACE; setPadding(0,0,0,6) })
        }

        paper.addView(TextView(this).apply{ text="----------------------------------------"; gravity=Gravity.CENTER; textSize=10f; setPadding(0,6,0,6) })
        paper.addView(TextView(this).apply{ text="Subtotal               $${total.toInt()}"; textSize=11f; typeface=Typeface.MONOSPACE; gravity=Gravity.END })
        paper.addView(TextView(this).apply{ text="TOTAL                  $${total.toInt()}"; setTypeface(null, Typeface.BOLD); textSize=12f; typeface=Typeface.MONOSPACE; gravity=Gravity.END; setPadding(0,4,0,0) })
        paper.addView(TextView(this).apply{ text="Tendered               $${tendered.toInt()}"; textSize=11f; typeface=Typeface.MONOSPACE; gravity=Gravity.END })
        paper.addView(TextView(this).apply{ text="Change                 $${change.toInt()}"; textSize=11f; typeface=Typeface.MONOSPACE; gravity=Gravity.END })
        paper.addView(TextView(this).apply{ text="----------------------------------------"; gravity=Gravity.CENTER; textSize=10f; setPadding(0,8,0,8) })
        paper.addView(TextView(this).apply{ text="★ Thank you! Please come again ★"; gravity=Gravity.CENTER; textSize=10f; typeface=Typeface.MONOSPACE; setPadding(0,0,0,8) })
        paper.addView(TextView(this).apply{ text="All sales are final."; gravity=Gravity.CENTER; textSize=8f; typeface=Typeface.MONOSPACE })

        root.addView(paper)

        val btnRow = LinearLayout(this)
        btnRow.orientation = LinearLayout.HORIZONTAL
        btnRow.setPadding(16,8,16,0)
        btnRow.gravity = Gravity.CENTER
        fun btn(t:String, col:Int): Button{
            val b=Button(this)
            b.text=t
            b.setBackgroundColor(col)
            b.setTextColor(if(col==Color.WHITE) Color.parseColor("#0F2D6B") else Color.WHITE)
            b.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(4,0,4,0) }
            b.textSize=11f
            b.setTypeface(null, Typeface.BOLD)
            return b
        }

        val bPrint = btn("🖨️ Print", Color.WHITE)
        bPrint.setOnClickListener{ Toast.makeText(this,"Printing $receiptNum...",Toast.LENGTH_SHORT).show() }
        val bNew = btn("➕ New Sale", Color.parseColor("#0F2D6B"))
        bNew.setOnClickListener{
            cart.clear()
            renderReceipt()
            etTendered.text.clear()
            dialog.dismiss()
            tvReceiptNum.text="Receipt #${(100..999).random()}"
            Toast.makeText(this,"New sale started",Toast.LENGTH_SHORT).show()
        }
        val bShare = btn("⬆️ Share", Color.WHITE)
        bShare.setOnClickListener{ Toast.makeText(this,"Share receipt",Toast.LENGTH_SHORT).show() }

        btnRow.addView(bPrint)
        btnRow.addView(bNew)
        btnRow.addView(bShare)
        root.addView(btnRow)
        root.addView(TextView(this).apply{ text="Sale completed • Tap Print"; textSize=10f; gravity=Gravity.CENTER; setPadding(0,12,0,0) })

        dialog.setContentView(root)
        dialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        dialog.show()
    }
}