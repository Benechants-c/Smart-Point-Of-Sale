package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
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
    private lateinit var txtValueProfit: TextView
    private val countedMap = mutableMapOf<String, Int>()
    private var productsCache = listOf<JSONObject>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }

        // HEADER BUILD 131.1
        val head = LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(16,12,16,12); gravity=Gravity.CENTER_VERTICAL
        }
        head.addView(TextView(this).apply{
            text="📦 STOCK TAKE - BUILD 131.1"; setTextColor(Color.WHITE); textSize=14f
            setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f)
        })
        head.addView(Button(this).apply{
            text="BACK"; setBackgroundColor(Color.parseColor("#475569")); setTextColor(Color.WHITE)
            setOnClickListener{ finish() }
        })
        root.addView(head)

        root.addView(TextView(this).apply{
            text = LicenseManager.getStatusText(this@StockTakeActivity)
            setPadding(12,6,12,6); textSize=11f; setBackgroundColor(Color.parseColor("#F1F5F9"))
            setTextColor(if(LicenseManager.canUseApp(this@StockTakeActivity)) Color.parseColor("#16A34A") else Color.RED)
        })

        // LIVE VALUES TOP BAR
        val valuesBar = LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(12,12,12,12); setBackgroundColor(Color.parseColor("#FFFFFF"))
        }
        txtValueCost = TextView(this).apply{ text="Cost Value: $0.00"; setTypeface(null,Typeface.BOLD); textSize=13f; setTextColor(Color.parseColor("#1E293B")) }
        txtValueSell = TextView(this).apply{ text="Selling Value: $0.00"; setTypeface(null,Typeface.BOLD); textSize=13f; setTextColor(Color.parseColor("#2563EB")) }
        txtValueProfit = TextView(this).apply{ text="Expected Profit: $0.00"; textSize=12f; setTextColor(Color.parseColor("#16A34A")) }
        valuesBar.addView(txtValueCost); valuesBar.addView(txtValueSell); valuesBar.addView(txtValueProfit)
        root.addView(valuesBar)

        // Form
        val topForm = LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL; setPadding(12,12,12,12); setBackgroundColor(Color.WHITE)
        }
        val edFileName = EditText(this).apply{
            val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            hint="File name e.g. ${sdf.format(Date())} stock take"
            setText("${sdf.format(Date())} stock take"); setTextColor(Color.BLACK)
        }
        txtSummary = TextView(this).apply{ text="Loading..."; setTextColor(Color.GRAY); textSize=12f; setPadding(0,8,0,8) }
        val btnSave = Button(this).apply{
            text="💾 SAVE (even if not finished)"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE)
        }
        val btnSaveDraft = Button(this).apply{
            text="📝 SAVE DRAFT & CONTINUE LATER"; setBackgroundColor(Color.parseColor("#F59E0B")); setTextColor(Color.WHITE)
        }
        val btnHistory = Button(this).apply{
            text="📜 VIEW SAVED STOCK TAKES"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)
        }
        topForm.addView(TextView(this).apply{text="Stock Take File Name *"; setTypeface(null,Typeface.BOLD)})
        topForm.addView(edFileName); topForm.addView(txtSummary)
        topForm.addView(btnSave); topForm.addView(btnSaveDraft); topForm.addView(btnHistory)
        root.addView(topForm)

        // Table header
        val headerRow = LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#E2E8F0")); setPadding(8,8,8,8)
        }
        headerRow.addView(createHeaderCell("Product", 2f))
        headerRow.addView(createHeaderCell("Code", 1f))
        headerRow.addView(createHeaderCell("In Hand", 0.8f))
        headerRow.addView(createHeaderCell("Counted", 1f))
        headerRow.addView(createHeaderCell("Variance", 0.8f))
        root.addView(headerRow)

        container = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(4,4,4,4) }
        val scroll = ScrollView(this).apply{ addView(container) }
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))

        setContentView(root)

        loadProducts()

        btnSave.setOnClickListener{
            val fn = edFileName.text.toString().trim()
            if(fn.isEmpty()){ Toast.makeText(this,"Enter file name",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            saveStockTake(fn, isDraft=false)
        }
        btnSaveDraft.setOnClickListener{
            val fn = edFileName.text.toString().trim() + " DRAFT"
            saveStockTake(fn, isDraft=true)
        }
        btnHistory.setOnClickListener{ showHistory() }
    }

    private fun createHeaderCell(t:String, w:Float)=TextView(this).apply{
        text=t; setTypeface(null,Typeface.BOLD); textSize=10f; setTextColor(Color.BLACK); gravity=Gravity.CENTER
        layoutParams=LinearLayout.LayoutParams(0,-2,w)
    }

    private fun loadProducts(){
        container.removeAllViews()
        countedMap.clear()
        productsCache = getAllProducts()

        if(productsCache.isEmpty()){
            container.addView(TextView(this).apply{
                text="No products found.\nAdd products first."; gravity=Gravity.CENTER
                setPadding(20,20,20,20); setTextColor(Color.GRAY)
            }); return
        }

        // Try load draft if exists
        val draftPref = getSharedPreferences("stock_take_draft", Context.MODE_PRIVATE)

        for(prod in productsCache){
            val id = prod.optString("id")
            val name = prod.optString("name","Unknown")
            val code = prod.optString("code", id)
            val stockHand = prod.optInt("quantity",0)
            val savedCounted = if(draftPref.contains(id)) draftPref.getInt(id, stockHand) else stockHand
            countedMap[id] = savedCounted

            val row = LinearLayout(this).apply{
                orientation=LinearLayout.HORIZONTAL; setPadding(6,6,6,6); setBackgroundColor(Color.WHITE)
            }
            val tvName = TextView(this).apply{ text=name; textSize=11f; setTextColor(Color.BLACK); layoutParams=LinearLayout.LayoutParams(0,-2,2f) }
            val tvCode = TextView(this).apply{ text=code; textSize=10f; setTextColor(Color.GRAY); gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,1f) }
            val tvHand = TextView(this).apply{ text=stockHand.toString(); textSize=11f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,0.8f) }
            val edCounted = EditText(this).apply{
                setText(savedCounted.toString()); textSize=11f; gravity=Gravity.CENTER
                setBackgroundColor(Color.parseColor("#FEF3C7")); layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            }
            val tvVariance = TextView(this).apply{
                val v = savedCounted - stockHand
                text= if(v>0) "+$v" else "$v"; textSize=12f; setTypeface(null,Typeface.BOLD); gravity=Gravity.CENTER
                setTextColor(when{ v<0 -> Color.RED; v==0 -> Color.parseColor("#16A34A"); else -> Color.parseColor("#2563EB") })
                layoutParams=LinearLayout.LayoutParams(0,-2,0.8f)
            }

            edCounted.addTextChangedListener(object:TextWatcher{
                override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){}
                override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){}
                override fun afterTextChanged(s:Editable?){
                    val counted = s.toString().toIntOrNull()?: 0
                    countedMap[id]=counted
                    // Save draft live
                    getSharedPreferences("stock_take_draft", Context.MODE_PRIVATE).edit().putInt(id, counted).apply()
                    val variance = counted - stockHand
                    tvVariance.text = if(variance>0) "+$variance" else "$variance"
                    tvVariance.setTextColor(when{ variance<0 -> Color.RED; variance==0 -> Color.parseColor("#16A34A"); else -> Color.parseColor("#2563EB") })
                    updateLiveValues()
                }
            })

            row.addView(tvName); row.addView(tvCode); row.addView(tvHand); row.addView(edCounted); row.addView(tvVariance)
            container.addView(row)
            container.addView(View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#E2E8F0")) })
        }
        updateLiveValues()
    }

    private fun updateLiveValues(){
        var totalCostHand=0.0; var totalSellHand=0.0
        var totalCostCounted=0.0; var totalSellCounted=0.0
        var neg=0; var pos=0; var zero=0

        for(p in productsCache){
            val id = p.optString("id")
            val hand = p.optInt("quantity",0)
            val counted = countedMap[id]?: hand
            val cost = p.optDouble("cost", p.optDouble("cost_price", p.optDouble("buy_price",0.0)))
            val sell = p.optDouble("sell", p.optDouble("selling_price", p.optDouble("price", cost*1.3)))

            totalCostHand += hand * cost
            totalSellHand += hand * sell
            totalCostCounted += counted * cost
            totalSellCounted += counted * sell

            val v = counted - hand
            when{ v<0 -> neg++; v==0 -> zero++; else -> pos++ }
        }

        txtValueCost.text = "Cost Value: In Hand $${"%.2f".format(totalCostHand)} → Counted $${"%.2f".format(totalCostCounted)}"
        txtValueSell.text = "Selling Value: In Hand $${"%.2f".format(totalSellHand)} → Counted $${"%.2f".format(totalSellCounted)}"
        txtValueProfit.text = "Expected Profit: $${"%.2f".format(totalSellCounted - totalCostCounted)} | 🔴$neg missing 🟢$zero ok 🔵$pos extra | ${productsCache.size} items"
        txtSummary.text = "${productsCache.size} products - draft auto-saved"
    }

    private fun getAllProducts(): List<JSONObject>{
        val list = mutableListOf<JSONObject>()
        val prefNames = listOf("products_db","products","stock_db","inventory","products_list")
        for(prefName in prefNames){
            try{
                val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    try{
                        val j = JSONObject(v.toString())
                        if(j.has("name") || j.has("product_name")){
                            val norm = JSONObject()
                            norm.put("id", j.optString("id", j.optString("code", j.optString("barcode",""))))
                            norm.put("name", j.optString("name", j.optString("product_name","")))
                            norm.put("code", j.optString("code", j.optString("barcode", norm.optString("id"))))
                            norm.put("quantity", j.optInt("quantity", j.optInt("stock", j.optInt("qty", j.optInt("stock_in_hand",0)))))
                            norm.put("cost", j.optDouble("cost", j.optDouble("cost_price", j.optDouble("buy_price", j.optDouble("buying_price",0.0)))))
                            norm.put("sell", j.optDouble("price", j.optDouble("selling_price", j.optDouble("sell_price", j.optDouble("retail_price",0.0)))))
                            list.add(norm)
                        }
                    }catch(_:Exception){}
                }
                if(list.isNotEmpty()) break
            }catch(_:Exception){}
        }
        return list.distinctBy { it.optString("id") }
    }

    private fun saveStockTake(fileName:String, isDraft:Boolean){
        try{
            val arr = JSONArray()
            var totalCostCounted=0.0; var totalSellCounted=0.0
            for(p in productsCache){
                val id = p.optString("id")
                val hand = p.optInt("quantity")
                val counted = countedMap[id]?: hand
                val variance = counted - hand
                val cost = p.optDouble("cost",0.0)
                val sell = p.optDouble("sell",0.0)
                totalCostCounted += counted*cost
                totalSellCounted += counted*sell

                val obj = JSONObject()
                obj.put("id", id); obj.put("name", p.optString("name")); obj.put("code", p.optString("code"))
                obj.put("in_hand", hand); obj.put("counted", counted); obj.put("variance", variance)
                obj.put("cost_price", cost); obj.put("selling_price", sell)
                obj.put("cost_value", counted*cost); obj.put("sell_value", counted*sell)
                arr.put(obj)
            }
            val mainObj = JSONObject()
            mainObj.put("fileName", fileName)
            mainObj.put("date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            mainObj.put("items", arr)
            mainObj.put("total_items", productsCache.size)
            mainObj.put("total_cost_value", totalCostCounted)
            mainObj.put("total_sell_value", totalSellCounted)
            mainObj.put("profit", totalSellCounted - totalCostCounted)
            mainObj.put("isDraft", isDraft)

            val pref = getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
            pref.edit().putString(fileName.lowercase().replace(" ","_")+"_"+System.currentTimeMillis(), mainObj.toString()).apply()

            if(!isDraft){
                // Clear draft after final save
                getSharedPreferences("stock_take_draft", Context.MODE_PRIVATE).edit().clear().apply()
                Toast.makeText(this,"✅ $fileName SAVED! Cost: $${"%.2f".format(totalCostCounted)} Sell: $${"%.2f".format(totalSellCounted)}",Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this,"📝 Draft saved: $fileName",Toast.LENGTH_SHORT).show()
            }
        }catch(e:Exception){ Toast.makeText(this,"Error: ${e.message}",Toast.LENGTH_LONG).show() }
    }

    private fun showHistory(){
        val pref = getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
        if(pref.all.isEmpty()){ Toast.makeText(this,"No stock takes yet",Toast.LENGTH_SHORT).show(); return }
        val layout = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(20,20,20,20) }
        val txt = TextView(this).apply{ textSize=11f; setTextColor(Color.BLACK) }
        val sb = StringBuilder()
        for((_,v) in pref.all){
            try{
                val j = JSONObject(v.toString())
                sb.append("📁 ${j.optString("fileName")} - ${j.optString("date")}\n")
                sb.append(" Items: ${j.optInt("total_items")} | Cost: $${"%.2f".format(j.optDouble("total_cost_value"))} | Sell: $${"%.2f".format(j.optDouble("total_sell_value"))}\n")
                sb.append(" Profit: $${"%.2f".format(j.optDouble("profit"))} ${if(j.optBoolean("isDraft")) "(DRAFT)" else ""}\n\n")
            }catch(_:Exception){}
        }
        txt.text = sb.toString()
        layout.addView(txt)
        android.app.AlertDialog.Builder(this).setTitle("📜 Saved (${pref.all.size})").setView(layout)
           .setPositiveButton("OK",null)
           .setNegativeButton("CLEAR ALL"){_,_->
                pref.edit().clear().apply(); Toast.makeText(this,"Cleared",Toast.LENGTH_SHORT).show()
            }.show()
    }
}
