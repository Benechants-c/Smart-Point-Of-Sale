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
 data class Product(val code:String,val name:String,val sell:Double,var qty:Int,val shopId:String,val dept:String)
 data class CartItem(var product:Product,var qty:Int)
 private val allProducts=mutableListOf<Product>()
 private val filtered=mutableListOf<Product>()
 private val cart=mutableListOf<CartItem>()
 private lateinit var spinnerShop:Spinner
 private lateinit var spinnerDept:Spinner
 private lateinit var searchInput:EditText
 private lateinit var productsLayout:LinearLayout
 private lateinit var cartLayout:LinearLayout
 private lateinit var totalView:TextView
 private lateinit var receiptView:TextView
 private lateinit var tenderedInput:EditText
 private lateinit var changeView:TextView
 private val shops=mutableListOf<Pair<String,String>>()
 private val depts=mutableListOf<String>()
 private fun getShopId():String{
  return if(spinnerShop.selectedItemPosition in shops.indices) shops[spinnerShop.selectedItemPosition].first else "main"
 }
 private fun getShopName():String{
  return if(spinnerShop.selectedItemPosition in shops.indices) shops[spinnerShop.selectedItemPosition].second else "Main Shop"
 }
 private fun getTotal():Double{
  var t=0.0
  for(c in cart) t+=c.product.sell*c.qty
  return t
 }
 private fun calcChange(){
  val total=getTotal()
  val tend=tenderedInput.text.toString().toDoubleOrNull()?:0.0
  val ch=tend-total
  if(tend==0.0){changeView.text="CHANGE: $0.00"}
  else if(ch<0){changeView.text="NEED $"+String.format("%.2f",-ch)+" MORE"; changeView.setTextColor(Color.RED); return}
  else {changeView.text="CHANGE: $"+String.format("%.2f",ch)}
  changeView.setTextColor(Color.parseColor("#16A34A"))
 }
 private fun loadRealStock(){
  allProducts.clear()
  try{
   val sid=getShopId()
   val pref=getSharedPreferences("stock_"+sid,Context.MODE_PRIVATE)
   for((code,v) in pref.all){
    try{
     val p=v.toString().split("|")
     if(p.size>=4){
      val n=p[0]
      val s=p[2].toDoubleOrNull()?:0.0
      val q=p[3].toIntOrNull()?:0
      val d=if(p.size>5)p[5]else"General"
      if(q>0) allProducts.add(Product(code,n,s,q,sid,d))
     }
    }catch(_:Exception){}
   }
  }catch(_:Exception){}
  depts.clear()
  depts.add("ALL DEPARTMENTS")
  for(d in allProducts.map{it.dept.ifEmpty{"General"}}.distinct()) depts.add(d)
  spinnerDept.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,depts)
  applyFilter()
 }
 private fun applyFilter(){
  filtered.clear()
  val q=searchInput.text.toString().trim().lowercase()
  val ds=if(spinnerDept.selectedItemPosition in depts.indices) depts[spinnerDept.selectedItemPosition] else "ALL DEPARTMENTS"
  for(p in allProducts){
   val ms=q.isEmpty()||p.name.lowercase().contains(q)||p.code.lowercase().contains(q)
   val md=ds=="ALL DEPARTMENTS"||p.dept==ds
   if(ms&&md) filtered.add(p)
  }
  refreshProducts()
 }
 private fun refreshProducts(){
  productsLayout.removeAllViews()
  if(filtered.isEmpty()){
   productsLayout.addView(TextView(this).apply{text="No stock in "+getShopName(); setPadding(20,30,20,30); setTextColor(Color.GRAY)})
   return
  }
  for(p in filtered){
   val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(12,12,12,12); setBackgroundColor(Color.WHITE)}
   row.addView(TextView(this).apply{text=p.name+"\n"+p.code+" | Stock:"+p.qty+" | $"+p.sell; textSize=13f; layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
   row.addView(Button(this).apply{text="ADD"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); setOnClickListener{addToCart(p)}})
   productsLayout.addView(row)
  }
 }
 private fun addToCart(p:Product){
  val ex=cart.find{it.product.code==p.code}
  if(ex!=null){if(ex.qty<p.qty) ex.qty++ else Toast.makeText(this,"No more stock",Toast.LENGTH_SHORT).show()}
  else cart.add(CartItem(p,1))
  refreshCart()
 }
