package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class SalesActivity : Activity() {
    data class Product(val code: String, val name: String, val cost: Double, val sell: Double, var qty: Int, val shopId: String, val dept: String)
    data class CartItem(var product: Product, var qty: Int)

    private val allProducts = mutableListOf<Product>()
    private val filtered = mutableListOf<Product>()
    private val cart = mutableListOf<CartItem>()
    private lateinit var spinnerShop: Spinner
    private lateinit var cartLayout: LinearLayout
    private lateinit var totalView: TextView
    private lateinit var receiptView: TextView
    private lateinit var tenderedInput: EditText
    private lateinit var changeView: TextView
    private val shops = mutableListOf<Pair<String,String>>()
    private val depts = mutableListOf<String>()
    private var cashierName = "Cashier"
    private var lastReceiptText = ""

    private fun getShopId(): String = if (spinnerShop.selectedItemPosition in shops.indices) shops[spinnerShop.selectedItemPosition].first else "main"

    private fun getShopDetails(shopId: String): Array<String> {
        try{
            val pref = getSharedPreferences("shops_db", Context.MODE_PRIVATE)
            val raw = pref.getString(shopId,"")?:""
            if(raw.contains("|")) return raw.split("|").toTypedArray()
            if(raw.isNotEmpty()) return arrayOf(raw,"","","","Thank you for shopping!")
        }catch(_:Exception){}
        return arrayOf("Main Shop","","","","Thank you!")
    }

    private fun getTotal(): Double { var t=0.0; for(c in cart) t+=c.product.sell*c.qty; return t }
    private fun getProfit(): Double { var p=0.0; for(c in cart) p+=(c.product.sell-c.product.cost)*c.qty; return p }
    private fun calcChange(){
        val total=getTotal(); val tendered=tenderedInput.text.toString().toDoubleOrNull()?:0.0; val change=tendered-total
        if(tendered==0.0) changeView.text="CHANGE: $0.00"
        else if(change<0) changeView.text="NEED $${String.format("%.2f",-change)}"
        else changeView.text="CHANGE: $${String.format("%.2f",change)}"
    }

    private fun loadRealStock(){
        allProducts.clear()
        try{
            val shopId=getShopId(); val pref=getSharedPreferences("stock_$shopId", Context.MODE_PRIVATE)
            for((code,v) in pref.all){
                try{
                    val p=v.toString().split("|")
                    if(p.size>=4){ val name=p[0]; val cost=p[1].toDoubleOrNull()?:0.0; val sell=p[2].toDoubleOrNull()?:0.0; val qty=p[3].toIntOrNull()?:0; val dept=if(p.size>5)p[5] else "General"; if(qty>=0) allProducts.add(Product(code,name,cost,sell,qty,shopId,dept)) }
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        depts.clear(); depts.add("ALL DEPARTMENTS"); for(d in allProducts.map{it.dept.ifEmpty{"General"}}.distinct()) depts.add(d)
        refreshCart()
    }

    private fun showSearchDialog(){
        val dlg=Dialog(this); val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundColor(Color.WHITE)}
        root.addView(TextView(this).apply{text="Search Product"; textSize=16f; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(16,12,16,12)})
        val ed=EditText(this).apply{hint="Enter name or code..."; setPadding(20,14,20,14); setBackgroundColor(Color.parseColor("#F1F5F9"))}
        root.addView(ed); val listLay=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}; val scroll=ScrollView(this).apply{layoutParams=LinearLayout.LayoutParams(-1,0,1f); addView(listLay)}; root.addView(scroll)
        fun refreshList(q:String){
            listLay.removeAllViews(); filtered.clear(); val qq=q.trim().lowercase()
            for(p in allProducts){ if(qq.isEmpty()||p.name.lowercase().contains(qq)||p.code.lowercase().contains(qq)) filtered.add(p) }
            for(p in filtered){
                val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(14,12,14,12); setBackgroundColor(if(p.qty<5) Color.parseColor("#FEF2F2") else Color.WHITE)}
                val left=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
                left.addView(TextView(this).apply{text=p.name + if(p.qty<5)" LOW" else ""; textSize=17f; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor("#0F172A"))})
                left.addView(TextView(this).apply{text="${p.code} | Stock:${p.qty}"; textSize=11f; setTextColor(Color.GRAY)})
                val price=TextView(this).apply{text="$${p.sell}"; textSize=16f; setTypeface(null,Typeface.BOLD); gravity=Gravity.END}
                row.addView(left); row.addView(price); row.setOnClickListener{ addToCart(p); dlg.dismiss() }; listLay.addView(row)
                listLay.addView(View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#E5E7EB"))})
            }
        }
        ed.addTextChangedListener(object:TextWatcher{override fun afterTextChanged(s:Editable?){refreshList(s.toString())} override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){} override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}})
        refreshList(""); val close=Button(this).apply{text="CLOSE"; setBackgroundColor(Color.parseColor("#E5E7EB"))}; close.setOnClickListener{dlg.dismiss()}; root.addView(close)
        dlg.setContentView(root); dlg.show(); dlg.window?.setLayout((resources.displayMetrics.widthPixels*0.92).toInt(),-2)
    }

    private fun addToCart(p: Product){
        val ex=cart.find{it.product.code==p.code}
        if(ex!=null){ if(ex.qty<p.qty) ex.qty++ else Toast.makeText(this,"Max ${p.qty}",Toast.LENGTH_SHORT).show() } else cart.add(CartItem(p,1))
        refreshCart()
    }

    private fun refreshCart(){
        cartLayout.removeAllViews()
        val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(10,10,10,10); setBackgroundColor(Color.parseColor("#E2E0F0"))}
        fun h(t:String,w:Float)=TextView(this).apply{text=t; textSize=11f; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor("#475569")); layoutParams=LinearLayout.LayoutParams(0,-2,w)}
        header.addView(TextView(this).apply{text="#"; layoutParams=LinearLayout.LayoutParams(60,-2); setTypeface(null,Typeface.BOLD); textSize=11f})
        header.addView(h("ITEM",2f)); header.addView(h("QTY",1.3f)); header.addView(h("PRICE",1f)); header.addView(h("TOTAL",1f))
        cartLayout.addView(header)
        var total=0.0; val copy = cart.toList()
        for(i in copy.indices){
            val cur = copy[i]; val line=cur.product.sell*cur.qty; total+=line
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(10,12,10,12); setBackgroundColor(Color.WHITE); gravity=Gravity.CENTER_VERTICAL}
            row.addView(TextView(this).apply{text="${i+1}."; layoutParams=LinearLayout.LayoutParams(60,-2); textSize=12f})
            row.addView(TextView(this).apply{text=cur.product.name; layoutParams=LinearLayout.LayoutParams(0,-2,2f); textSize=17f; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor("#0F172A"))})
            val qtyLay=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; layoutParams=LinearLayout.LayoutParams(0,-2,1.3f); gravity=Gravity.CENTER}
            val minus=Button(this).apply{text="-"; textSize=20f; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#EF4444")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(100,100).apply{setMargins(0,0,4,0)}; setOnClickListener{ if(cur.qty>1){cur.qty--} else {cart.remove(cur)}; refreshCart() }}
            val qty=TextView(this).apply{text="${cur.qty}"; textSize=18f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; setBackgroundColor(Color.parseColor("#E2E8F0")); setPadding(0,22,0,22); layoutParams=LinearLayout.LayoutParams(70,100).apply{setMargins(0,0,4,0)}}
            val plus=Button(this).apply{text="+"; textSize=20f; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(100,100); setOnClickListener{ if(cur.qty < cur.product.qty){cur.qty++; refreshCart()} else Toast.makeText(this@SalesActivity,"Max ${cur.product.qty}",Toast.LENGTH_SHORT).show() }}
            qtyLay.addView(minus); qtyLay.addView(qty); qtyLay.addView(plus); row.addView(qtyLay)
            row.addView(TextView(this).apply{text="$${cur.product.sell}"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); gravity=Gravity.CENTER; textSize=12f})
            row.addView(TextView(this).apply{text="$${String.format("%.2f",line)}"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); gravity=Gravity.END; setTypeface(null,Typeface.BOLD); textSize=12f})
            cartLayout.addView(row); cartLayout.addView(View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#E5E7EB"))})
        }
        receiptView.text="RECEIPT: ${cart.size} items"; totalView.text="TOTAL: $${String.format("%.2f",total)}"; calcChange()
    }

    private fun completeSale(){
        if(cart.isEmpty()){Toast.makeText(this,"Cart empty",Toast.LENGTH_SHORT).show(); return}
        val total=getTotal(); val tendered=tenderedInput.text.toString().toDoubleOrNull()?:0.0
        if(tendered<total){Toast.makeText(this,"Tendered less than TOTAL",Toast.LENGTH_LONG).show(); return}
        try{
            val shopId=getShopId()
            for(item in cart
