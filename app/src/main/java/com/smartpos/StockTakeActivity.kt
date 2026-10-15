package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class StockTakeActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try {
            val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC")) }
            val scroll = ScrollView(this)
            val main = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(10,10,10,10) }

            val head = TextView(this).apply{ text="STOCK TAKE #431 REAL - FIXED CRASH"; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1E293B")); textSize=16f; setPadding(20,20,20,20); setTypeface(null,Typeface.BOLD) }
            main.addView(head)

            val edFileName = EditText(this).apply{
                try{ val sdf = SimpleDateFormat("MMMM yyyy", Locale.US); setText("${sdf.format(Date())} stock take") }catch(_:Exception){ setText("Stock Take") }
            }
            main.addView(edFileName)

            val txtCost = TextView(this).apply{ text="Loading..."; textSize=14f; setTypeface(null,Typeface.BOLD); setPadding(10,10,10,10) }
            val txtSell = TextView(this).apply{ text=""; textSize=14f; setTypeface(null,Typeface.BOLD); setPadding(10,10,10,10) }
            val txtSum = TextView(this).apply{ text=""; textSize=14f; setPadding(10,10,10,10) }
            main.addView(txtCost); main.addView(txtSell); main.addView(txtSum)

            val btnSave = Button(this).apply{ text="SAVE" }
            val btnHist = Button(this).apply{ text="VIEW HISTORY" }
            val btnBack = Button(this).apply{ text="BACK"; setOnClickListener{ finish() } }
            main.addView(btnSave); main.addView(btnHist); main.addView(btnBack)

            // HEADER
            val hdr = LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setBackgroundColor(Color.LTGRAY); setPadding(5,10,5,10) }
            hdr.addView(TextView(this).apply{ text="Product"; layoutParams=LinearLayout.LayoutParams(0,-2,2f); setTypeface(null,Typeface.BOLD) })
            hdr.addView(TextView(this).apply{ text="Hand"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); gravity=Gravity.CENTER; setTypeface(null,Typeface.BOLD) })
            hdr.addView(TextView(this).apply{ text="Counted"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); gravity=Gravity.CENTER; setTypeface(null,Typeface.BOLD) })
            main.addView(hdr)

            val products = mutableListOf<JSONObject>()
            try{
                val prefs = listOf("products_main","products_rukudzo","stock_main","products_db","products","stock_db")
                val seen = HashSet<String>()
                for(pn in prefs){
                    try{
                        val sp = getSharedPreferences(pn, Context.MODE_PRIVATE)
                        for((k,v) in sp.all){
                            if(seen.contains(k)) continue
                            try{
                                val raw = v.toString()
                                val j = JSONObject()
                                if(raw.trim().startsWith("{")){
                                    val o = JSONObject(raw)
                                    j.put("id", o.optString("id",k))
                                    j.put("name", o.optString("name",k))
                                    j.put("qty", o.optInt("quantity", o.optInt("qty", o.optInt("stock",1))))
                                    j.put("cost", o.optDouble("cost", o.optDouble("cost_price",0.0)))
                                    j.put("sell", o.optDouble("price", o.optDouble("sell_price",0.0)))
                                } else {
                                    j.put("id", k); j.put("name", raw.split("|")[0]); j.put("qty", 1); j.put("cost",0.0); j.put("sell",0.0)
                                }
                                seen.add(k); products.add(j)
                            }catch(_:Exception){}
                        }
                    }catch(_:Exception){}
                }
            }catch(e:Exception){ txtSum.text="Load err: ${e.message}" }

            if(products.isEmpty()){
                main.addView(TextView(this).apply{ text="NO PRODUCTS FOUND in products_main / products_rukudzo - Add product first"; setPadding(20,20,20,20) })
            }

            val countedMap = mutableMapOf<String,Int>()
            var totalCostHand=0.0; var totalSellHand=0.0
            for(p in products){
                val id=p.optString("id"); val hand=p.optInt("qty",0)
                countedMap[id]=hand
                totalCostHand+=hand*p.optDouble("cost",0.0)
                totalSellHand+=hand*p.optDouble("sell",0.0)

                val row = LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setPadding(5,12,5,12); setBackgroundColor(Color.WHITE) }
                val tvN = TextView(this).apply{ text=p.optString("name"); layoutParams=LinearLayout.LayoutParams(0,-2,2f); textSize=15f }
                val tvH = TextView(this).apply{ text=hand.toString(); layoutParams=LinearLayout.LayoutParams(0,-2,1f); gravity=Gravity.CENTER; textSize=15f; setTypeface(null,Typeface.BOLD) }
                val edC = EditText(this).apply{ setText(hand.toString()); layoutParams=LinearLayout.LayoutParams(0,-2,1f); gravity=Gravity.CENTER; textSize=15f }
                row.addView(tvN); row.addView(tvH); row.addView(edC)
                main.addView(row)

                edC.setOnFocusChangeListener{ _,_ ->
                    val v = edC.text.toString().toIntOrNull()?:hand
                    countedMap[id]=v
                    var cCost=0.0; var cSell=0.0
                    for(pp in products){ cCost+=(countedMap[pp.optString("id")]?:0)*pp.optDouble("cost",0.0); cSell+=(countedMap[pp.optString("id")]?:0)*pp.optDouble("sell",0.0) }
                    txtCost.text="Cost Hand $${"%.2f".format(totalCostHand)} -> Counted $${"%.2f".format(cCost)}"
                    txtSell.text="Sell Hand $${"%.2f".format(totalSellHand)} -> Counted $${"%.2f".format(cSell)} Profit $${"%.2f".format(cSell-cCost)}"
                }
            }

            txtCost.text="Cost Value: $${"%.2f".format(totalCostHand)}"
            txtSell.text="Sell Value: $${"%.2f".format(totalSellHand)}"
            txtSum.text="${products.size} REAL products loaded"

            btnSave.setOnClickListener{
                try{
                    val fn = edFileName.text.toString()
                    val arr = JSONArray(); var cCost=0.0; var cSell=0.0
                    for(p in products){
                        val id=p.optString("id"); val hand=p.optInt("qty"); val counted=countedMap[id]?:hand
                        cCost+=counted*p.optDouble("cost",0.0); cSell+=counted*p.optDouble("sell",0.0)
                        val o=JSONObject(); o.put("id",id); o.put("name",p.optString("name")); o.put("in_hand",hand); o.put("counted",counted); o.put("variance",counted-hand)
                        arr.put(o)
                    }
                    val mainO=JSONObject(); mainO.put("fileName",fn); mainO.put("date", SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())); mainO.put("items",arr); mainO.put("total_cost_value",cCost); mainO.put("total_sell_value",cSell)
                    val sp=getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
                    sp.edit().putString(fn.replace(" ","_")+"_"+System.currentTimeMillis(), mainO.toString()).apply()
                    Toast.makeText(this,"SAVED $fn",Toast.LENGTH_LONG).show()
                }catch(e:Exception){ Toast.makeText(this,"Save err ${e.message}",Toast.LENGTH_LONG).show() }
            }

            btnHist.setOnClickListener{
                try{
                    val sp=getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
                    if(sp.all.isEmpty()){ Toast.makeText(this,"No history",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                    val sb=StringBuilder()
                    for((_,v) in sp.all){ try{ val j=JSONObject(v.toString()); sb.append("${j.optString("fileName")} - ${j.optString("date")}\n") }catch(_:Exception){} }
                    val tv=TextView(this).apply{ text=sb.toString(); setPadding(20,20,20,20) }
                    android.app.AlertDialog.Builder(this).setTitle("Saved ${sp.all.size}").setView(tv).setPositiveButton("OK",null).show()
                }catch(e:Exception){ Toast.makeText(this,"Hist err ${e.message}",Toast.LENGTH_LONG).show() }
            }

            scroll.addView(main); root.addView(scroll); setContentView(root)

        }catch(e:Exception){
            val err = TextView(this).apply{ text="CRASH FIXED #431\nError: ${e.message}\n\n${e.stackTraceToString().take(500)}"; setPadding(30,30,30,30); textSize=14f }
            setContentView(err)
        }
    }
}