package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import org.json.JSONObject

class StockTransferActivity : Activity() {
    data class Shop(val id: String, val name: String)
    data class Prod(val key: String, val name: String, val code: String, var qty: Int)
    private var shops: List<Shop> = emptyList()
    private lateinit var fromSpin: Spinner
    private lateinit var toSpin: Spinner
    private lateinit var table: LinearLayout
    private lateinit var qtyInput: EditText
    private lateinit var prodNameView: TextView
    private var selectedProd: Prod? = null

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        shops = getShops()
        val root = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE);setPadding(10,10,10,10)}
        val head = LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setBackgroundColor(Color.parseColor("#1E293B"));setPadding(16,12,10,12)}
        head.addView(TextView(this).apply{text="STOCK TRANSFER - BUILD 128 REAL ONLY";setTextColor(Color.WHITE);textSize=13f;setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        head.addView(Button(this).apply{text="BACK";setBackgroundColor(Color.parseColor("#475569"));setTextColor(Color.WHITE);setOnClickListener{finish()}})
        root.addView(head)

        if(shops.size < 2){
            root.addView(TextView(this).apply{text="❌ Need at least 2 shops!\nCurrent: ${shops.size}\nGo to BRANCHES / SHOPS and add shops.\nExample: karoi_main and chikangwe";setPadding(20,20,20,20);setTextColor(Color.RED);textSize=14f;gravity=Gravity.CENTER})
            setContentView(ScrollView(this).apply{addView(root)}); return
        }

        fun label(t:String):TextView=TextView(this).apply{text=t;textSize=11f;setTypeface(null,Typeface.BOLD);setTextColor(Color.WHITE);setBackgroundColor(Color.parseColor("#1E293B"));setPadding(12,8,12,8)}
        root.addView(label("FROM SHOP")); fromSpin = Spinner(this); root.addView(fromSpin)
        root.addView(label("TO SHOP")); toSpin = Spinner(this); root.addView(toSpin)
        val shopNames = shops.map{"${it.id} - ${it.name}"}
        fromSpin.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shopNames)
        toSpin.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shopNames)
        toSpin.setSelection(1)

        root.addView(label("SELECT PRODUCT FROM FROM SHOP")); prodNameView = TextView(this).apply{text="No product selected";setPadding(12,12,12,12);setBackgroundColor(Color.parseColor("#F1F5F9"));setTextColor(Color.BLACK)}; root.addView(prodNameView)
        root.addView(Button(this).apply{text="SELECT PRODUCT";setBackgroundColor(Color.parseColor("#2563EB"));setTextColor(Color.WHITE);setOnClickListener{selectProduct()}})
        root.addView(label("QUANTITY TO TRANSFER *")); qtyInput = EditText(this).apply{hint="e.g. 5";inputType=android.text.InputType.TYPE_CLASS_NUMBER;setPadding(16,12,16,12);setBackgroundColor(Color.WHITE)}; root.addView(qtyInput)
        root.addView(Button(this).apply{text="TRANSFER NOW";setBackgroundColor(Color.parseColor("#22C55E"));setTextColor(Color.WHITE);setOnClickListener{doTransfer()}})
        root.addView(label("TRANSFER LOG")); table = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}; root.addView(table); refreshLog()
        setContentView(ScrollView(this).apply{isFillViewport=true;addView(root)})
    }

    private fun getShops(): List<Shop>{
        val out = mutableListOf<Shop>()
        try{
            val prefs = getSharedPreferences("shops_db", Context.MODE_PRIVATE)
            prefs.all.forEach{(k,v)-> try{ val j=JSONObject(v.toString()); out.add(Shop(j.optString("id",k), j.optString("name",k))) }catch(_:Exception){} }
        }catch(_:Exception){}
        return out
    }

    private fun getProductsForShop(shopId: String): List<Prod>{
        val list = mutableListOf<Prod>()
        try{
            val prefs = getSharedPreferences("products_db", 0)
            prefs.all.forEach{(k,v)-> try{
                val s=v.toString(); if(!s.trim().startsWith("{")) return@forEach
                val j=JSONObject(s); val name=j.optString("name"); val code=j.optString("code"); val qty=j.optInt("qty",0)
                // Show all products for now, qty check happens on transfer
                if(name.isNotEmpty()) list.add(Prod(k,name,code,qty))
            }catch(_:Exception){} }
        }catch(_:Exception){}
        return list
    }

    private fun selectProduct(){
        val fromId = shops[fromSpin.selectedItemPosition].id
        val prods = getProductsForShop(fromId)
        if(prods.isEmpty()){Toast.makeText(this,"No products! Add via STOCK RECEIVE first",Toast.LENGTH_LONG).show(); return}
        val names = prods.map{"${it.name} (${it.code}) Qty:${it.qty}"}.toTypedArray()
        android.app.AlertDialog.Builder(this).setTitle("Select from $fromId").setItems(names){_,which-> selectedProd = prods[which]; prodNameView.text = "${selectedProd!!.name} | ${selectedProd!!.code} | Available: ${selectedProd!!.qty}" }.show()
    }

    private fun doTransfer(){
        val p = selectedProd; if(p==null){Toast.makeText(this,"Select product first!",Toast.LENGTH_SHORT).show(); return}
        val q = qtyInput.text.toString().toIntOrNull()?:0; if(q<=0){Toast.makeText(this,"Enter qty >0!",Toast.LENGTH_SHORT).show(); return}
        if(q>p.qty){Toast.makeText(this,"Not enough stock! Only ${p.qty}",Toast.LENGTH_LONG).show(); return}
        val fromId = shops[fromSpin.selectedItemPosition].id; val toId = shops[toSpin.selectedItemPosition].id
        if(fromId==toId){Toast.makeText(this,"FROM and TO cannot be same!",Toast.LENGTH_SHORT).show(); return}
        try{
            val prefs = getSharedPreferences("products_db",0)
            val fromJson = prefs.getString(p.key,"{}")?.let{JSONObject(it)}?: JSONObject()
            fromJson.put("qty", fromJson.optInt("qty",p.qty) - q)
            prefs.edit().putString(p.key, fromJson.toString()).apply()
            // Log
            getSharedPreferences("transfer_log",0).edit().putString(System.currentTimeMillis().toString(), "$fromId -> $toId | ${p.name} x$q | ${java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date())}").apply()
            Toast.makeText(this,"✅ Transferred $q x ${p.name} from $fromId to $toId!",Toast.LENGTH_LONG).show()
            qtyInput.setText(""); selectedProd=null; prodNameView.text="No product selected"; refreshLog()
        }catch(e:Exception){Toast.makeText(this,"Error: ${e.message}",Toast.LENGTH_LONG).show()}
    }

    private fun refreshLog(){
        table.removeAllViews()
        val logs = getSharedPreferences("transfer_log",0).all.values.map{it.toString()}.reversed()
        if(logs.isEmpty()) table.addView(TextView(this).apply{text="No transfers yet";gravity=Gravity.CENTER;setPadding(0,20,0,20);setTextColor(Color.GRAY)})
        else logs.take(20).forEach{ l-> table.addView(TextView(this).apply{text=l;setPadding(10,8,10,8);setBackgroundColor(Color.WHITE);setTextColor(Color.BLACK)}) }
    }
}
