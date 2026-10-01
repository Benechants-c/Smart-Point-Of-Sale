package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*

class SalesActivity : Activity() {
    data class Product(val code: String, val name: String, val sell: Double, var qty: Int, val shopId: String, val dept: String)
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
    private lateinit var receiptView: TextView
    private lateinit var tenderedInput: EditText
    private lateinit var changeView: TextView
    private val shops = mutableListOf<Pair<String,String>>()
    private val depts = mutableListOf<String>()

    private fun getShopId(): String = if (spinnerShop.selectedItemPosition in shops.indices) shops[spinnerShop.selectedItemPosition].first else "main"
    private fun getShopName(): String = if (spinnerShop.selectedItemPosition in shops.indices) shops[spinnerShop.selectedItemPosition].second else "Main Shop"
    private fun getTotal(): Double { var t=0.0; for(c in cart) t+=c.product.sell*c.qty; return t }
    private fun calcChange(){
        val total=getTotal(); val tendered=tenderedInput.text.toString().toDoubleOrNull()?:0.0; val change=tendered-total
        if(tendered==0.0){ changeView.text="CHANGE: $0.00"; changeView.setTextColor(Color.parseColor("#16A34A")) }
        else if(change<0){ changeView.text="NEED $"+String.format("%.2f",-change)+" MORE"; changeView.setTextColor(Color.RED) }
        else { changeView.text="CHANGE: $"+String.format("%.2f",change); changeView.setTextColor(Color.parseColor("#16A34A")) }
    }
    private fun printReceipt(tendered: Double, change: Double){
        if(cart.isEmpty()) return
        var total=0.0; var receipt="=== "+getShopName().uppercase()+" ===\n"
        for(item in cart){ val line=item.product.sell*item.qty; total+=line; receipt+=item.product.name+" x"+item.qty+" = $"+String.format("%.2f",line)+"\n" }
        receipt+="TOTAL: $"+String.format("%.2f",total)+"\nTENDERED: $"+String.format("%.2f",tendered)+"\nCHANGE: $"+String.format("%.2f",change)+"\n"
        try{ val i=android.content.Intent(android.content.Intent.ACTION_SEND); i.type="text/plain"; i.putExtra(android.content.Intent.EXTRA_TEXT,receipt); startActivity(android.content.Intent.createChooser(i,"PRINT")) }catch(_:Exception){ Toast.makeText(this,receipt,Toast.LENGTH_LONG).show() }
    }
    private fun loadRealStock(){
        allProducts.clear()
        try{
            val shopId=getShopId(); val stockPref=getSharedPreferences("stock_"+shopId, Context.MODE_PRIVATE)
            for((code,v) in stockPref.all){
                try{
                    val parts=v.toString().split("|")
                    if(parts.size>=4){ val name=parts[0]; val sell=parts[2].toDoubleOrNull()?:0.0; val qty=parts[3].toIntOrNull()?:0; val dept=if(parts.size>5) parts[5] else "General"; if(qty>0) allProducts.add(Product(code,name,sell,qty,shopId,dept)) }
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        depts.clear(); depts.add("ALL DEPARTMENTS"); for(d in allProducts.map{it.dept.ifEmpty{"General"}}.distinct()) depts.add(d)
        spinnerDept.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,depts); applyFilter()
    }
    private fun applyFilter(){
        filtered.clear(); val q=searchInput.text.toString().trim().lowercase(); val deptSel=if(spinnerDept.selectedItemPosition in depts.indices) depts[spinnerDept.selectedItemPosition] else "ALL DEPARTMENTS"
        for(p in allProducts){ val ms=q.isEmpty()||p.name.lowercase().contains(q)||p.code.lowercase().contains(q); val md=deptSel=="ALL DEPARTMENTS"||p.dept==deptSel; if(ms&&md) filtered.add(p) }
        refreshProducts()
    }
    private fun refreshProducts(){
        productsLayout.removeAllViews()
        if(filtered.isEmpty()){ productsLayout.addView(TextView(this).apply{text="No stock in "+getShopName(); setPadding(20,30,20,30); setTextColor(Color.GRAY)}); return }
        for(p in filtered){
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(12,12,12,12); setBackgroundColor(Color.WHITE)}
            row.addView(TextView(this).apply{text=p.name+"\n"+p.code+" | Stock:"+p.qty+" | $"+p.sell; textSize=13f; layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
            row.addView(Button(this).apply{text="ADD"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); setOnClickListener{addToCart(p)}})
            productsLayout.addView(row); productsLayout.addView(View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,2); setBackgroundColor(Color.parseColor("#E5E7EB"))})
        }
    }
    private fun addToCart(p: Product){
        val exist=cart.find{it.product.code==p.code}
        if(exist!=null){ if(exist.qty<p.qty) exist.qty++ else Toast.makeText(this,"No more stock",Toast.LENGTH_SHORT).show() } else cart.add(CartItem(p,1))
        refreshCart()
    }
    private fun refreshCart(){
        cartLayout.removeAllViews(); var total=0.0; var tq=0; var rTxt=""
        for(item in cart){
            total+=item.product.sell*item.qty; tq+=item.qty
            val r=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(12,12,12,12); setBackgroundColor(Color.WHITE)}
            r.addView(TextView(this).apply{text=item.product.name+"\n$"+item.product.sell+" x"+item.qty+" = $"+String.format("%.2f",item.product.sell*item.qty); textSize=12f; layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
            val minus=Button(this).apply{text="-"; setBackgroundColor(Color.parseColor("#EF4444")); setTextColor(Color.WHITE)}
            val qtyTv=TextView(this).apply{text=""+item.qty; setPadding(20,10,20,10); setTypeface(null,Typeface.BOLD)}
            val plus=Button(this).apply{text="+"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE)}
            minus.setOnClickListener{ if(item.qty>1){item.qty--; refreshCart()} else {cart.remove(item); refreshCart()} }
            plus.setOnClickListener{ if(item.qty<item.product.qty){item.qty++; refreshCart()} else Toast.makeText(this,"Max "+item.product.qty,Toast.LENGTH_SHORT).show() }
            r.addView(minus); r.addView(qtyTv); r.addView(plus)
            cartLayout.addView(r)
            rTxt+=item.product.name+" x"+item.qty+" = $"+String.format("%.2f",item.product.sell*item.qty)+"\n"
        }
        if(cart.isEmpty()) receiptView.text="RECEIPT: No items\nAdd $1 + $2 to see total"
        else receiptView.text=rTxt+"--------------------\nTOTAL: $"+String.format("%.2f",total)+" | ITEMS: "+tq
        totalView.text="TOTAL: $"+String.format("%.2f",total)+" | "+tq+" items | "+getShopName(); calcChange()
    }
    private fun completeSale(){
        if(cart.isEmpty()){Toast.makeText(this,"Cart empty",Toast.LENGTH_SHORT).show(); return}
        val total=getTotal(); val tendered=tenderedInput.text.toString().toDoubleOrNull()?:0.0
        if(tendered<total){Toast.makeText(this,"Tendered less than TOTAL",Toast.LENGTH_LONG).show(); return}
        val change=tendered-total; printReceipt(tendered,change)
        try{
            val shopId=getShopId()
            for(item in cart){
                val stockPref=getSharedPreferences("stock_"+shopId,Context.MODE_PRIVATE)
                val old=stockPref.getString(item.product.code,"")?:""
                if(old.contains("|")){
                    val parts=old.split("|").toMutableList(); val oldQty=parts.getOrNull(3)?.toIntOrNull()?:0; parts[3]=(oldQty-item.qty).toString()
                    stockPref.edit().putString(item.product.code,parts.joinToString("|")).apply()
                    getSharedPreferences("inventory_db",Context.MODE_PRIVATE).edit().putString(shopId.lowercase()+"_"+item.product.code.lowercase(),parts.joinToString("|")).apply()
                }
            }
        }catch(_:Exception){}
        Toast.makeText(this,"SALE COMPLETE Change $"+String.format("%.2f",change),Toast.LENGTH_LONG).show()
        cart.clear(); tenderedInput.setText(""); refreshCart(); loadRealStock()
    }
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try{ val ps=getSharedPreferences("shops_db",Context.MODE_PRIVATE); for((k,v) in ps.all){ shops.add(Pair(k,k)) } }catch(_:Exception){}
        if(shops.isEmpty()) shops.add(Pair("main","Main Shop"))
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundColor(Color.parseColor("#F8FAFC"))}
        root.addView(TextView(this).apply{text="POS SALES - BUILD 154 RECEIPT FIX"; textSize=18f; setTypeface(null,Typeface.BOLD); setPadding(0,0,0,10)})
        root.addView(TextView(this).apply{text="SELECT SHOP"; textSize=11f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1E293B")); setPadding(12,6,12,6)})
        spinnerShop=Spinner(this); spinnerShop.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,shops.map{it.second}); root.addView(spinnerShop)
        root.addView(TextView(this).apply{text="DEPARTMENT"; textSize=11f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1E293B")); setPadding(12,6,12,6)})
        spinnerDept=Spinner(this); depts.add("ALL DEPARTMENTS"); spinnerDept.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,depts); root.addView(spinnerDept)
        searchInput=EditText(this).apply{hint="Search product"; setPadding(20,14,20,14); setBackgroundColor(Color.WHITE)}; root.addView(searchInput)
        root.addView(TextView(this).apply{text="PRODUCTS"; setBackgroundColor(Color.parseColor("#DBEAFE")); setPadding(12,6,12,6); setTypeface(null,Typeface.BOLD)})
        productsLayout=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}; root.addView(ScrollView(this).
