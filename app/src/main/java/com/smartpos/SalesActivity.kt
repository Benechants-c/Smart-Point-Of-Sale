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
        cashierName = intent.getStringExtra("USER_NAME")?: "John"
        cashierRole = intent.getStringExtra("USER_ROLE")?: "Cashier"
        if(cashierName.trim().isEmpty()) cashierName = "John"
        selectedShop = intent.getStringExtra("SHOP_NAME")?: "Main Shop"
        getSharedPreferences("pos", Context.MODE_PRIVATE).edit().putString("last_user", cashierName).apply()
        loadAllProducts()
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F1F5F9")) }
        val header = LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#1E293B")); setPadding(24,28,24,18); orientation=LinearLayout.HORIZONTAL }
        header.addView(TextView(this).apply { text="SmartPOS"; textSize=18f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        val hRight = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.END }
        txtCashier = TextView(this).apply { text="Cashier: $cashierName ($cashierRole)"; textSize=12f; setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD) }
        txtShopTop = TextView(this).apply { text="Shop: $selectedShop"; textSize=11f; setTextColor(Color.parseColor("#93C5FD")); gravity=Gravity.END }
        hRight.addView(txtCashier); hRight.addView(txtShopTop)
        header.addView(hRight); root.addView(header)
        val shopCard = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,12,16,12); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,8)} }
        shopCard.addView(TextView(this).apply { text="SELECT SHOP"; textSize=13f; setTypeface(null,Typeface.BOLD) })
        shopContainer = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        shopCard.addView(shopContainer); loadShopsInto(shopContainer); root.addView(shopCard)
        val searchCard = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(12,12,12,12); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,0,12,8)} }
        searchCard.addView(TextView(this).apply { text="QUICK ADD - Code / Barcode / Name + ENTER"; textSize=11f; setTextColor(Color.parseColor("#16A34A")); setTypeface(null,Typeface.BOLD) })
        val searchRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        inputQuick = EditText(this).apply { hint="e.g. 001 or stove..."; setBackgroundColor(Color.parseColor("#F1F5F9")); setPadding(16,14,16,14); layoutParams=LinearLayout.LayoutParams(0,-2,1f) }
        val btnAdd = Button(this).apply { text="ADD"; setBackgroundColor(Color.parseColor("#22C55E")); setTextColor(Color.WHITE); setOnClickListener{ quickAddProduct() } }
        searchRow.addView(inputQuick); searchRow.addView(btnAdd); searchCard.addView(searchRow)
        searchCard.addView(Button(this).apply { text="SEARCH PRODUCTS LIST"; setBackgroundColor(Color.parseColor("#1D4ED8")); setTextColor(Color.WHITE); setOnClickListener{ showProductPicker() } })
        root.addView(searchCard)
        root.addView(TextView(this).apply { text="CART"; textSize=15f; setTypeface(null,Typeface.BOLD); setPadding(16,8,16,4) })
        cartContainer = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        root.addView(cartContainer)
                val receiptCard = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(14,14,14,14); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,12)} }
        txtReceiptCount = TextView(this).apply { text="0 items"; textSize=12f }
        txtReceiptTotal = TextView(this).apply { text="TOTAL: $0.00"; textSize=18f; setTypeface(null,Typeface.BOLD) }
        val rRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        rRow.addView(txtReceiptCount.apply { layoutParams=LinearLayout.LayoutParams(0,-2,1f) }); rRow.addView(txtReceiptTotal); receiptCard.addView(rRow)
        val tRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,10,0,6) }
        tRow.addView(TextView(this).apply { text="TENDERED $:"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); setTypeface(null,Typeface.BOLD) })
        inputTendered = EditText(this).apply { hint="0"; inputType=8194; setPadding(16,10,16,10); layoutParams=LinearLayout.LayoutParams(120,-2) }
        tRow.addView(inputTendered); receiptCard.addView(tRow)
        txtChange = TextView(this).apply { text="CHANGE: $0.00"; gravity=Gravity.CENTER; setBackgroundColor(Color.parseColor("#22C55E")); setTextColor(Color.WHITE); setPadding(0,14,0,14); layoutParams=LinearLayout.LayoutParams(-1,-2); setTypeface(null,Typeface.BOLD) }
        receiptCard.addView(txtChange)
        receiptCard.addView(Button(this).apply { text="COMPLETE SALE + PRINT"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); setOnClickListener{ completeSaleAndPrint() } })
        root.addView(receiptCard); scroll.addView(root); setContentView(scroll)
        inputQuick.setOnKeyListener { _, keyCode, event -> if(keyCode==KeyEvent.KEYCODE_ENTER && event.action==KeyEvent.ACTION_UP){ quickAddProduct(); true } else false }
        inputTendered.addTextChangedListener(object:TextWatcher{ override fun afterTextChanged(s:Editable?){ updateTotals() } override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){} override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){} })
        refreshCart()
    }
    private fun loadShopsInto(container: LinearLayout){
        container.removeAllViews()
        val shops = mutableListOf("Main Shop")
        try{ val pref = getSharedPreferences("shops_db", Context.MODE_PRIVATE); for((_,v) in pref.all){ val name=v.toString().split("|")[0]; if(name.isNotEmpty() &&!shops.contains(name)) shops.add(name) } }catch(_:Exception){}
        val rg = RadioGroup(this); rg.orientation = RadioGroup.VERTICAL
        for(s in shops){ val rb = RadioButton(this); rb.text = s; rb.isChecked = (s == selectedShop); rb.setOnCheckedChangeListener { _, isChecked -> if(isChecked){ selectedShop = s; txtShopTop.text = "Shop: $selectedShop" } }; rg.addView(rb) }
        container.addView(rg)
    }
    private fun loadAllProducts(){
        allProducts.clear()
        try{ for(db in listOf("stock_main","products_db","products")){ val pref=getSharedPreferences(db, Context.MODE_PRIVATE); for((k,v) in pref.all){ val p=v.toString().split("|"); if(p.isEmpty()) continue; val name=p[0]; var price=p.getOrNull(2)?.toFloatOrNull()?: p.getOrNull(1)?.toFloatOrNull()?: 0f; val code=if(p.size>4) p[4] else k; if(name.isNotEmpty() && price>0) allProducts.add(Triple(code,name,price)) } } }catch(_:Exception){}
        if(allProducts.isEmpty()) allProducts.add(Triple("001","stove 4 plate",50f))
    }
    private fun quickAddProduct(){
        val q=inputQuick.text.toString().trim(); if(q.isEmpty()) return
        val found = allProducts.filter { it.first.equals(q,true) || it.second.contains(q,true) }
        if(found.isEmpty()){ Toast.makeText(this,"Not found: $q",Toast.LENGTH_SHORT).show(); return }
        if(found.size==1){ addToCart(found[0].second,found[0].third,found[0].first); inputQuick.setText("") } else { val items=found.map { "${it.first} | ${it.second} - $${it.third}" }.toTypedArray(); AlertDialog.Builder(this).setTitle("Found ${found.size}").setItems(items){_,w-> addToCart(found[w].second,found[w].third,found[w].first); inputQuick.setText("") }.show() }
    }
    private fun showProductPicker(){ val items=allProducts.map { "${it.first} | ${it.second} - $${it.third}" }.toTypedArray(); AlertDialog.Builder(this).setTitle("Products").setItems(items){_,w-> addToCart(allProducts[w].second,allProducts[w].third,allProducts[w].first) }.show() }
    private fun addToCart(name:String,price:Float,code:String=""){ val ex=cart.find { it.name.equals(name,true) }; if(ex!=null) ex.qty++ else cart.add(CartItem(name,price,1,code)); refreshCart() }
    private fun refreshCart(){
        cartContainer.removeAllViews()
        for((i,it) in cart.withIndex()){
            val row=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(10,14,10,14) }
            row.addView(TextView(this).apply { text="${i+1}"; layoutParams=LinearLayout.LayoutParams(30,-2) })
            row.addView(TextView(this).apply { text=it.name; layoutParams=LinearLayout.LayoutParams(0,-2,1f); textSize=15f; setTypeface(null,Typeface.BOLD) })
            val qb=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; layoutParams=LinearLayout.LayoutParams(110,-2) }
            qb.addView(Button(this).apply { text="-"; setBackgroundColor(Color.RED); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(40,80); setOnClickListener{ if(it.qty>1) it.qty-- else cart.remove(it); refreshCart() } })
            qb.addView(TextView(this).apply { text=it.qty.toString(); gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(30,-2) })
            qb.addView(Button(this).apply { text="+"; setBackgroundColor(Color.parseColor("#22C55E")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(40,80); setOnClickListener{ it.qty++; refreshCart() } })
            row.addView(qb); row.addView(TextView(this).apply { text="$${it.price*it.qty}"; layoutParams=LinearLayout.LayoutParams(80,-2); gravity=Gravity.END; setTypeface(null,Typeface.BOLD) }); cartContainer.addView(row)
        }; updateTotals()
    }
    private fun updateTotals(){ val total=cart.sumOf { (it.price*it.qty).toDouble() }.toFloat(); txtReceiptCount.text="${cart.sumOf { it.qty }} items"; txtReceiptTotal.text="TOTAL: $${String.format("%.2f",total)}"; val tend=inputTendered.text.toString().toFloatOrNull()?:0f; txtChange.text="CHANGE: $${String.format("%.2f",tend-total)}" }
    private fun completeSaleAndPrint(){
        val total=cart.sumOf { (it.price*it.qty).toDouble() }.toFloat(); if(cart.isEmpty()) return; val tend=inputTendered.text.toString().toFloatOrNull()?:0f; if(tend<total) return
        val receipt="SmartPOS - $selectedShop\nCashier: $cashierName\nTOTAL $total\nTENDERED $tend\nCHANGE ${tend-total}"; lastReceiptText=receipt
        getSharedPreferences("pos", Context.MODE_PRIVATE).edit().putString("last_receipt",receipt).apply()
        printReceipt(receipt); showPopup(total,tend)
    }
    private fun printReceipt(txt:String){ try{ startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain"; putExtra(Intent.EXTRA_TEXT,txt) },"Print")) }catch(_:Exception){} }
    private fun showPopup(total:Float,tend:Float){
        val v=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(30,20,30,20) }
        v.addView(TextView(this).apply { text="Receipt Printed Successfully!"; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER })
        v.addView(TextView(this).apply { text="TOTAL: $$total | TENDERED: $$tend | CHANGE: $${tend-total}"; gravity=Gravity.CENTER; setPadding(0,10,0,10) })
        val b1=Button(this).apply { text="PRINT RECEIPT"; setBackgroundColor(Color.parseColor("#1D4ED8")); setTextColor(Color.WHITE); setOnClickListener{ printReceipt(lastReceiptText) } }
        val b2=Button(this).apply { text="REPRINT LAST RECEIPT"; setOnClickListener{ printReceipt(lastReceiptText) } }
        val b3=Button(this).apply { text="NEW SALE"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE) }
        v.addView(b1); v.addView(b2); v.addView(b3)
        val d=AlertDialog.Builder(this).setView(v).setCancelable(false).create()
        b3.setOnClickListener{ cart.clear(); inputTendered.setText(""); inputQuick.setText(""); refreshCart(); d.dismiss() }; d.show()
    }
}
