package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class StockTakeActivity : Activity() {

    private lateinit var container: LinearLayout
    private lateinit var txtSummary: TextView
    private lateinit var txtValueCost: TextView
    private lateinit var txtValueSell: TextView
    private val countedMap = mutableMapOf<String, Int>()
    private var productsCache = listOf<JSONObject>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }

        val head = LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20,16,20,16); gravity=Gravity.CENTER_VERTICAL
        }
        head.addView(TextView(this).apply{
            text="📦 STOCK TAKE - BUILD 132 REAL FIX"; setTextColor(Color.WHITE); textSize=16f
            setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f)
        })
        head.addView(Button(this).apply{
            text="BACK"; textSize=13f; setBackgroundColor(Color.parseColor("#475569")); setTextColor(Color.WHITE)
            setOnClickListener{ finish() }
        })
        root.addView(head)

        val valuesBar = LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(16,14,16,14); setBackgroundColor(Color.WHITE)
        }
        txtValueCost = TextView(this).apply{ text="Cost Value: $0.00"; setTypeface(null,Typeface.BOLD); textSize=15f; setTextColor(Color.BLACK) }
        txtValueSell = TextView(this).apply{ text="Sell Value: $0.00"; setTypeface(null,Typeface.BOLD); textSize=15f; setTextColor(Color.parseColor("#2563EB")) }
        valuesBar.addView(txtValueCost); valuesBar.addView(txtValueSell)
        root.addView(valuesBar)

        val topForm = LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL; setPadding(16,12,16,12); setBackgroundColor(Color.WHITE)
        }
        val edFileName = EditText(this).apply{
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            setText("${sdf.format(Date())} stock take"); setTextColor(Color.BLACK); textSize=16f
        }
        txtSummary = TextView(this).apply{ text="Loading..."; setTextColor(Color.GRAY); textSize=14f; setTypeface(null,Typeface.BOLD) }
        val btnSave = Button(this).apply{ text="💾 SAVE ANYTIME"; textSize=14f; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE) }
        val btnHistory = Button(this).apply{ text="📜 VIEW SAVED TAKES"; textSize=14f; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE) }
        topForm.addView(edFileName); topForm.addView(txtSummary); topForm.addView(btnSave); topForm.addView(btnHistory)
        root.addView(topForm)

        val headerRow = LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#E2E8F0")); setPadding(10,14,10,14)
        }
        fun h(t:String,w:Float)=TextView(this).apply{ text=t; setTypeface(null,Typeface.BOLD); textSize=14f; setTextColor(Color.BLACK); gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,w) }
        headerRow.addView(h("Product",2f)); headerRow.addView(h("Code",1f)); headerRow.addView(h("In Hand",0.8f)); headerRow.addView(h("Counted",1f)); headerRow.addView(h("Var",0.8f))
        root.addView(headerRow)

        container = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply{ addView(container) }, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)

        loadProducts()

        btnSave.setOnClickListener{
            val fn = edFileName.text.toString().trim()
            if(fn.isEmpty()){ Toast.makeText(this,"Enter file name",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            saveStockTake(fn)
        }
        btnHistory.setOnClickListener{ showHistory() }
    }

    private fun loadProducts(){
        container.removeAllViews(); countedMap.clear()
        productsCache = getAllProducts()
        if(productsCache.isEmpty()){
            container.addView(TextView(this).apply{ text="No products found - Add product first in ADD PRODUCT (Main or Rukudzo)"; textSize=16f; gravity=Gravity.CENTER; setPadding(20,20,20,20) }); return
        }
        for(prod in productsCache){
            val id = prod.optString("id"); val name = prod.optString("name","Unknown"); val code = prod.optString("code",id)
            val stockHand = prod.optInt("quantity",0)
            countedMap[id]=stockHand
            val row = LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setPadding(10,16,10,16); setBackgroundColor(Color.WHITE) }
            val tvName = TextView(this).apply{ text=name; textSize=15f; setTextColor(Color.BLACK); setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,2f) }
            val tvCode = TextView(this).apply{ text=code; textSize=13f; setTextColor(Color.GRAY); gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,1f) }
            val tvHand = TextView(this).apply{ text=stockHand.toString(); textSize=15f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,0.8f) }
            val edCounted = EditText(this).apply{ setText(stockHand.toString()); textSize=15f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; setBackgroundColor(Color.parseColor("#FEF3C7")); layoutParams=LinearLayout.LayoutParams(0,-2,1f) }
            val tvVar = TextView(this).apply{ text="0"; textSize=15f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; setTextColor(Color.parseColor("#16A34A")); layoutParams=LinearLayout.LayoutParams(0,-2,0.8f) }
            edCounted.addTextChangedListener(object:TextWatcher{
                override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}
                override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}
                override fun afterTextChanged(s:Editable?){
                    val c = s.toString().toIntOrNull()?:0; countedMap[id]=c
                    val v = c-stockHand; tvVar.text = if(v>0) "+$v" else "$v"
                    tvVar.setTextColor(when{ v<0->Color.RED; v==0->Color.parseColor("#16A34A"); else->Color.parseColor("#2563EB") })
                    updateLive()
                }
            })
            row.addView(tvName); row.addView(tvCode); row.addView(tvHand); row.addView(edCounted); row.addView(tvVar)
            container.addView(row)
            val sep = View(this)
            sep.layoutParams = LinearLayout.LayoutParams(-1,2)
            sep.setBackgroundColor(Color.parseColor("#E2E8F0"))
            container.addView(sep)
        }
        updateLive()
    }

    private fun updateLive(){
        var costH=0.0; var sellH=0.0; var costC=0.0; var sellC=0.0; var neg=0; var pos=0; var zero=0
        for(p in productsCache){
            val id=p.optString("id"); val hand=p.optInt("quantity",0); val counted=countedMap[id]?:hand
            val cost=p.optDouble("cost",0.0); val sell=p.optDouble("sell",0.0)
            costH+=hand*cost; sellH+=hand*sell; costC+=counted*cost; sellC+=counted*sell
            val v=counted-hand; when{ v<0->neg++; v==0->zero++; else->pos++ }
        }
        txtValueCost.text="Cost: Hand $${"%.2f".format(costH)} → Counted $${"%.2f".format(costC)}"
        txtValueSell.text="Sell: Hand $${"%.2f".format(sellH)} → Counted $${"%.2f".format(sellC)} | Profit $${"%.2f".format(sellC-costC)}"
        txtSummary.text="${productsCache.size} items | 🔴$neg missing 🟢$zero ok 🔵$pos extra"
    }

    private fun getAllProducts(): List<JSONObject>{
        val list=mutableListOf<JSONObject>()
        val seen = HashSet<String>()
        val prefNames=listOf("products_main","products_rukudzo","stock_main","products_db","products","stock_db","inventory","stock_rukudzo")
        for(prefName in prefNames.distinct()){
            try{
                val pref=getSharedPreferences(prefName, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    if(seen.contains(k)) continue
                    try{
                        val raw = v.toString()
                        val norm = JSONObject()
                        if(raw.trim().startsWith("{")){
                            val j=JSONObject(raw)
                            norm.put("id",j.optString("id",j.optString("code",k)))
                            norm.put("name",j.optString("name",j.optString("product_name",k)))
                            norm.put("code",j.optString("code",j.optString("barcode",norm.optString("id"))))
                            norm.put("quantity",j.optInt("quantity",j.optInt("stock",j.optInt("qty",j.optInt("stock_in_hand",0)))))
                            norm.put("cost",j.optDouble("cost",j.optDouble("cost_price",j.optDouble("buy_price",0.0))))
                            norm.put("sell",j.optDouble("price",j.optDouble("selling_price",j.optDouble("sell_price",0.0))))
                        } else {
                            val parts = raw.split("|")
                            val name = if(parts.isNotEmpty()) parts[0] else k
                            val price = if(parts.size>2) parts[2].toFloatOrNull()?:0f else if(parts.size>1) parts[1].toFloatOrNull()?:0f else 0f
                            var qty = 1f
                            if(parts.size>3) qty = parts[3].toFloatOrNull()?:1f
                            norm.put("id",k)
                            norm.put("name",name)
                            norm.put("code",k)
                            norm.put("quantity",qty.toInt())
                            norm.put("cost",price.toDouble()*0.7)
                            norm.put("sell",price.toDouble())
                        }
                        if(norm.optString("name").isNotBlank()){
                            seen.add(k)
                            list.add(norm)
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        return list.distinctBy{it.optString("id")}
    }

    private fun saveStockTake(fileName:String){
        try{
            val arr=JSONArray(); var costC=0.0; var sellC=0.0
            for(p in productsCache){
                val id=p.optString("id"); val hand=p.optInt("quantity"); val counted=countedMap[id]?:hand
                costC+=counted*p.optDouble("cost",0.0); sellC+=counted*p.optDouble("sell",0.0)
                val obj=JSONObject()
                obj.put("id",id); obj.put("name",p.optString("name")); obj.put("code",p.optString("code"))
                obj.put("in_hand",hand); obj.put("counted",counted); obj.put("variance",counted-hand)
                obj.put("cost_price",p.optDouble("cost",0.0)); obj.put("selling_price",p.optDouble("sell",0.0))
                arr.put(obj)
            }
            val mainObj=JSONObject()
            mainObj.put("fileName",fileName); mainObj.put("date",SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            mainObj.put("items",arr); mainObj.put("total_items",productsCache.size)
            mainObj.put("total_cost_value",costC); mainObj.put("total_sell_value",sellC)
            val pref=getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
            pref.edit().putString(fileName.lowercase().replace(" ","_")+"_"+System.currentTimeMillis(),mainObj.toString()).apply()
            Toast.makeText(this,"✅ $fileName SAVED! Cost $${"%.2f".format(costC)} Sell $${"%.2f".format(sellC)}",Toast.LENGTH_LONG).show()
        }catch(e:Exception){ Toast.makeText(this,"Error: ${e.message}",Toast.LENGTH_LONG).show() }
    }

    private fun showHistory(){
        val pref=getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
        if(pref.all.isEmpty()){ Toast.makeText(this,"No stock takes yet",Toast.LENGTH_SHORT).show(); return }
        val layout=LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(20,20,20,20) }
        val txt=TextView(this).apply{ textSize=14f; setTextColor(Color.BLACK) }
        val sb=StringBuilder()
        for((_,v) in pref.all){
            try{
                val j=JSONObject(v.toString())
                sb.append("📁 ${j.optString("fileName")} - ${j.optString("date")}\n Cost $${"%.2f".format(j.optDouble("total_cost_value"))} Sell $${"%.2f".format(j.optDouble("total_sell_value"))}\n\n")
            }catch(_:Exception){}
        }
        txt.text=sb.toString(); layout.addView(txt)
        android.app.AlertDialog.Builder(this).setTitle("📜 Saved (${pref.all.size})").setView(layout).setPositiveButton("OK",null).setNegativeButton("CLEAR ALL"){_,_-> pref.edit().clear().apply() }.show()
    }
}