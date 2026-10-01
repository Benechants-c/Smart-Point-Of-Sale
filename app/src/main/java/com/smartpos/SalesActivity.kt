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
        else if(change<0){ changeView.text="NEED $${String.format("%.2f",-change)} MORE"; changeView.setTextColor(Color.RED) }
        else { changeView.text="CHANGE: $${String.format("%.2f",change)}"; changeView.setTextColor(Color.parseColor("#16A34A")) }
    }
    private fun printReceipt(tendered: Double, change: Double){
        if(cart.isEmpty()) return
        var total=0.0; var receipt="=== ${getShopName().uppercase()} ===\n"
        for(item in cart){ val line=item.product.sell*item.qty; total+=line; receipt+="${item.product.name} x${item.qty} = $${String.format("%.2f",line)}\n" }
        receipt+="TOTAL: $${String.format("%.2f",total)}\nTENDERED: $${String.format("%.2f",tendered)}\nCHANGE: $${String.format("%.2f",change)}\n"
        try{ val i=android.content.Intent(android.content.Intent.ACTION_SEND); i.type="text/plain"; i.putExtra(android.content.Intent.EXTRA_TEXT,receipt); startActivity(android.content.Intent.createChooser(i,"PRINT")) }catch(_:Exception){ Toast.makeText(this,receipt,Toast.LENGTH_LONG).show() }
    }
    private fun loadRealStock(){
        allProducts.clear()
        try{
            val shopId=getShopId(); val stockPref=getSharedPreferences("stock_$shopId", Context.MODE_PRIVATE)
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
        if(filtered.isEmpty()){ productsLayout.addView(TextView(this).apply{text="No stock in ${getShopName()}"; setPadding(20,30,20,30); setTextColor(Color.GRAY)}); return }
        for(p in filtered){
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(12,12,12,12); setBackgroundColor(Color.WHITE)}
            row.addView(TextView(this).apply{text="${p.name}\n${p.code} | Stock:${p.qty} | $${p.sell}"; textSize=13f; layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
            row.addView(Button(this).apply{text="ADD"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); setOnClickListener{addToCart(p)}})
            productsLayout.addView(row); productsLayout.addView(View(this).apply{layoutParams=LinearLayout
