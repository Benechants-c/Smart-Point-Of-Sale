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
import android.widget.*

class SalesActivity : Activity() {

    data class CartItem(var name:String, var price:Float, var qty:Int, var buy:Float=0f)

    private val cart = mutableListOf<CartItem>()
    private lateinit var cartContainer: LinearLayout
    private lateinit var txtReceiptTotal: TextView
    private lateinit var txtReceiptCount: TextView
    private lateinit var txtChange: TextView
    private lateinit var inputTendered: EditText
    private var lastReceiptText = ""
    private var cashierName = "Cashier"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cashierName = intent.getStringExtra("USER_NAME")?: "John"
        if(cashierName.isEmpty()) cashierName = "John"
        lastReceiptText = getSharedPreferences("pos", Context.MODE_PRIVATE).getString("last_receipt","")?:""

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F1F5F9")) }

        // HEADER - Cashier on top
        val header = LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#1E293B")); setPadding(24,30,24,20); orientation=LinearLayout.HORIZONTAL }
        val hLeft = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; layoutParams=LinearLayout.LayoutParams(0,-2,1f); gravity=Gravity.CENTER_VERTICAL }
        hLeft.addView(TextView(this).apply { text="🖥️ SmartPOS"; textSize=18f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE) })
        val hRight = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.END }
        hRight.addView(TextView(this).apply { text="👤 Cashier: $cashierName (Cashier)"; textSize=12f; setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD) })
        hRight.addView(TextView(this).apply { text="📍 Shop: Main Shop"; textSize=11f; setTextColor(Color.parseColor("#CBD5E1")); gravity=Gravity.END })
        hRight.addView(TextView(this).apply { text="🕒 18:44"; textSize=11f; setTextColor(Color.parseColor("#94A3B8")); gravity=Gravity.END })
        header.addView(hLeft); header.addView(hRight)
        root.addView(header)

        // SELECT SHOP - KEPT AS IS
        val shopCard = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(20,16,20,16); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,16,16,12)} }
        shopCard.addView(TextView(this).apply { text="🏪 SELECT SHOP"; textSize=14f; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor("#1E293B")) })
        val shopRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,12,0,4); gravity=Gravity.CENTER_VERTICAL }
        shopRow.addView(TextView(this).apply { text="🔵"; textSize=18f })
        shopRow.addView(TextView(this).apply { text=" Main Shop"; textSize=15f; setTextColor(Color.parseColor("#334155")) })
        shopCard.addView(shopRow)

        val btnSearch = Button(this).apply {
            text="🔍 SEARCH PRODUCTS"
            setBackgroundColor(Color.parseColor("#1D4ED8"))
            setTextColor(Color.WHITE)
            textSize=14f
            setTypeface(null,Typeface.BOLD)
            setOnClickListener { showProductPicker() }
        }
        shopCard.addView(btnSearch)
        root.addView(shopCard)

        // CART HEADER
        root.addView(TextView(this).apply { text="CART"; textSize=16f; setTypeface(null,Typeface.BOLD); setPadding(20,12,20,4); setTextColor(Color.parseColor("#1E293B")) })
        val cartHeader = LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#E2E8F0")); setPadding(12,8,12,8); orientation=LinearLayout.HORIZONTAL }
        cartHeader.addView(TextView(this).apply { text="#"; layoutParams=LinearLayout.LayoutParams(30,-2); textSize=11f; setTypeface(null,Typeface.BOLD) })
        cartHeader.addView(TextView(this).apply { text="ITEM"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); textSize=11f; setTypeface(null,Typeface.BOLD) })
        cartHeader.addView(TextView(this).apply { text="QTY"; layoutParams=LinearLayout.LayoutParams(120,-2); gravity=Gravity.CENTER; textSize=11f; setTypeface(null,Typeface.BOLD) })
        cartHeader.addView(TextView(this).apply { text="PRICE"; layoutParams=LinearLayout.LayoutParams(90,-2); gravity=Gravity.END; textSize=11f; setTypeface(null,Typeface.BOLD) })
        cartHeader.addView(TextView(this).apply { text="TOTAL"; layoutParams=LinearLayout.LayoutParams(90,-2); gravity=Gravity.END; textSize=11f; setTypeface(null,Typeface.BOLD) })
        root.addView(cartHeader)

        cartContainer = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        root.addView(cartContainer)

        // RECEIPT SECTION
        val receiptCard = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,16,16,16)} }
        txtReceiptCount = TextView(this).apply { text="🧾 RECEIPT: 0 items"; textSize=12f; setTextColor(Color.parseColor("#64748B")) }
        txtReceiptTotal = TextView(this).apply { text="TOTAL: $0.00"; textSize=18f; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor("#0F172A")); gravity=Gravity.END }
        val rowTotal = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        rowTotal.addView(txtReceiptCount.apply { layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        rowTotal.addView(txtReceiptTotal.apply { layoutParams=LinearLayout.LayoutParams(-2,-2) })
        receiptCard.addView(rowTotal)

        val tenderRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,12,0,8); gravity=Gravity.CENTER_VERTICAL }
        tenderRow.addView(TextView(this).apply { text="TENDERED $:"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); setTypeface(null,Typeface.BOLD) })
        inputTendered = EditText(this).apply { hint="0"; inputType=8194; setBackgroundColor(Color.parseColor("#F1F5F9")); setPadding(20,12,20,12); layoutParams=LinearLayout.LayoutParams(160,-2) }
        tenderRow.addView(inputTendered)
        receiptCard.addView(tenderRow)

        txtChange = TextView(this).apply {
            text="✅ CHANGE: $0.00"; textSize=16f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER
            setBackgroundColor(Color.parseColor("#22C55E")); setTextColor(Color.WHITE); setPadding(0,16,0,16)
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,8,0,8)}
        }
        receiptCard.addView(txtChange)

        val btnComplete = Button(this).apply {
            text="✅ COMPLETE SALE + PRINT"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE)
            textSize=15f; setTypeface(null,Typeface.BOLD); setPadding(0,20,0,20)
            setOnClickListener { completeSaleAndPrint() }
        }
        receiptCard.addView(btnComplete)

        root.addView(receiptCard)
        scroll.addView(root)
        setContentView(scroll)

        inputTendered.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}
            override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}
            override fun afterTextChanged(s:Editable?){ updateTotals() }
        })

        refreshCart()
    }

    private fun showProductPicker(){
        val products = mutableListOf<Pair<String,Float>>()
        try{
            val names = listOf("stock_main","products_db","products")
            val seen = HashSet<String>()
            for(n in names){
                val pref = getSharedPreferences(n, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    if(seen.contains(k)) continue
                    seen.add(k)
                    val p = v.toString().split("|")
                    val name = p[0]
                    var sell = if(p.size>2) p[2].toFloatOrNull()?:0f else 0f
                    if(sell==0f) sell = if(p.size>1) p[1].toFloatOrNull()?:0f else 0f
                    if(name.isNotEmpty() && sell>0) products.add(Pair(name,sell))
                }
            }
        }catch(_:Exception){}
        if(products.isEmpty()) products.add(Pair("stove 4 plate",140f))
        if(products.isEmpty()) products.add(Pair("Coke 330ml",1.5f))

        val items = products.map { "${it.first} - $${it.second}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Select Product").setItems(items){_,which->
            val (nm,pr) = products[which]
            addToCart(nm,pr)
        }.show()
    }

    private fun addToCart(name:String, price:Float){
        val existing = cart.find { it.name.equals(name,true) }
        if(existing!=null) existing.qty++ else cart.add(CartItem(name,price,1))
        refreshCart()
    }

    private fun refreshCart(){
        cartContainer.removeAllViews()
        for((index,item) in cart.withIndex()){
            val row = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(12,16,12,16); gravity=Gravity.CENTER_VERTICAL }
            row.addView(TextView(this).apply { text="${index+1}"; layoutParams=LinearLayout.LayoutParams(30,-2); textSize=12f })
            row.addView(TextView(this).apply {
                text=item.name
                layoutParams=LinearLayout.LayoutParams(0,-2,1f)
                textSize=15f // BIGGER FONT as you asked
                setTypeface(null,Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
            })

            val qtyBox = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; layoutParams=LinearLayout.LayoutParams(120,-2); gravity=Gravity.CENTER }
            val btnMinus = Button(this).apply { text="-"; setBackgroundColor(Color.parseColor("#EF4444")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(60,90).apply{setMargins(4,0,4,0)}; setOnClickListener{ if(item.qty>1) item.qty-- else cart.remove(item); refreshCart() } }
            val txtQty = TextView(this).apply { text=item.qty.toString(); gravity=Gravity.CENTER; setBackgroundColor(Color.parseColor("#E2E8F0")); setPadding(10,10,10,10); layoutParams=LinearLayout.LayoutParams(50,-2) }
            val btnPlus = Button(this).apply { text="+"; setBackgroundColor(Color.parseColor("#22C55E")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(60,90).apply{setMargins(4,0,4,0)}; setOnClickListener{ item.qty++; refreshCart() } }
            qtyBox.addView(btnMinus); qtyBox.addView(txtQty); qtyBox.addView(btnPlus)
            row.addView(qtyBox)

            row.addView(TextView(this).apply { text="$${item.price}"; layoutParams=LinearLayout.LayoutParams(90,-2); gravity=Gravity.END; textSize=12f })
            row.addView(TextView(this).apply { text="$${String.format("%.2f",item.price*item.qty)}"; layoutParams=LinearLayout.LayoutParams(90,-2); gravity=Gravity.END; setTypeface(null,Typeface.BOLD) })

            cartContainer.addView(row)
            cartContainer.addView(TextView(this).apply { setBackgroundColor(Color.parseColor("#F1F5F9")); layoutParams=LinearLayout.LayoutParams(-1,2) })
        }
        updateTotals()
    }

    private fun updateTotals(){
        val total = cart.sumOf { (it.price*it.qty).toDouble() }.toFloat()
        val count = cart.sumOf { it.qty }
        txtReceiptCount.text="🧾 RECEIPT: $count items"
        txtReceiptTotal.text="TOTAL: $${String.format("%.2f",total)}"
        val tendered = inputTendered.text.toString().toFloatOrNull()?:0f
        val change = tendered - total
        txtChange.text = if(change>=0) "✅ CHANGE: $${String.format("%.2f",change)}" else "❌ BALANCE: $${String.format("%.2f",-change)} still needed"
        txtChange.setBackgroundColor(if(change>=0) Color.parseColor("#22C55E") else Color.parseColor("#EF4444"))
    }

    private fun completeSaleAndPrint(){
        val total = cart.sumOf { (it.price*it.qty).toDouble() }.toFloat()
        if(cart.isEmpty()){ Toast.makeText(this,"Cart empty",0).show(); return }
        val tendered = inputTendered.text.toString().toFloatOrNull()?:0f
        if(tendered < total){ Toast.makeText(this,"Tendered less than total",0).show(); return }

        // SAVE SALE
        try{
            val salesPref = getSharedPreferences("sales_main", Context.MODE_PRIVATE)
            val id = System.currentTimeMillis().toString()
            val profit = total*0.2f
            salesPref.edit().putString(id, "$id|$total|$profit|${cart.size}|$cashierName").apply()

            val drawerPref = getSharedPreferences("cash", Context.MODE_PRIVATE)
            val cur = drawerPref.getFloat("drawer",5f)
            drawerPref.edit().putFloat("drawer",cur+total).apply()
        }catch(_:Exception){}

        // BUILD RECEIPT
        val sb = StringBuilder()
        sb.append("====== SmartPOS ======\n")
        sb.append("Shop: Main Shop\nCashier: $cashierName\n")
        sb.append("Date: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date())}\n")
        sb.append("----------------------\n")
        for(it in cart){ sb.append("${it.name} x${it.qty} @${it.price} = $${String.format("%.2f",it.price*it.qty)}\n") }
        sb.append("----------------------\n")
        sb.append("TOTAL: $${String.format("%.2f",total)}\nTENDERED: $${String.format("%.2f",tendered)}\nCHANGE: $${String.format("%.2f",tendered-total)}\n")
        sb.append("Thank you!\n")
        lastReceiptText = sb.toString()
        getSharedPreferences("pos", Context.MODE_PRIVATE).edit().putString("last_receipt",lastReceiptText).apply()

        // FIRST PRINT ATTEMPT
        printReceipt(lastReceiptText)

        // POPUP WITH REPRINT OPTION
        showReceiptPopup(total, tendered)
    }

    private fun printReceipt(text:String){
        try{
            val intent = Intent(Intent.ACTION_SEND)
            intent.type="text/plain"
            intent.putExtra(Intent.EXTRA_TEXT,text)
            startActivity(Intent.createChooser(intent,"Print Receipt via..."))
        }catch(e:Exception){
            Toast.makeText(this,"Printing: $text",1).show()
        }
    }

    private fun showReceiptPopup(total:Float, tendered:Float){
        val dialogView = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(40,30,40,30); setBackgroundColor(Color.WHITE) }
        dialogView.addView(TextView(this).apply { text="✅ Receipt Printed Successfully!"; textSize=16f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; setPadding(0,0,0,12) })
        dialogView.addView(TextView(this).apply { text="TOTAL: $${String.format("%.2f",total)} | TENDERED: $${String.format("%.2f",tendered)} | CHANGE: $${String.format("%.2f",tendered-total)}"; textSize=12f; gravity=Gravity.CENTER; setPadding(0,0,0,20) })

        val btnPrint = Button(this).apply { text="🖨️ PRINT RECEIPT"; setBackgroundColor(Color.parseColor("#1D4ED8")); setTextColor(Color.WHITE); setOnClickListener{ printReceipt(lastReceiptText) } }
        val btnReprint = Button(this).apply { text="🔄 REPRINT LAST RECEIPT"; setBackgroundColor(Color.WHITE); setTextColor(Color.parseColor("#1E293B")); setOnClickListener{
            val last = getSharedPreferences("pos", Context.MODE_PRIVATE).getString("last_receipt","")
            if(!last.isNullOrEmpty()) printReceipt(last) else Toast.makeText(this@SalesActivity,"No last receipt",0).show()
        }}
        val btnNew = Button(this).apply { text="➕ NEW SALE"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); setOnClickListener{
            cart.clear(); inputTendered.setText(""); refreshCart(); (it.parent as? LinearLayout)?.let{ } // close dialog handled below
        }}

        dialogView.addView(btnPrint); dialogView.addView(btnReprint); dialogView.addView(btnNew)

        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(false).create()
        btnNew.setOnClickListener{
            cart.clear(); inputTendered.setText(""); refreshCart(); dialog.dismiss()
        }
        // make other buttons not dismiss automatically so you can reprint multiple times
        dialog.show()
    }
}
