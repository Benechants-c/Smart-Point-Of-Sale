package com.smartpos
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.*
import android.graphics.Color
import android.view.Gravity
import org.json.JSONObject
import org.json.JSONArray

class StockTakeActivity : Activity() {
    private val names = mutableListOf<String>()
    private val inHands = mutableListOf<Int>()
    private val costs = mutableListOf<Double>()
    private val sells = mutableListOf<Double>()
    private val countedList = mutableListOf<Int>()
    private lateinit var listLayout: LinearLayout
    private lateinit var totalTxt: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try{
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(24,24,24,24); setBackgroundColor(Color.parseColor("#F5F5F5")) }
            root.addView(TextView(this).apply{ text="STOCK TAKE #436B FINAL"; textSize=20f; setTextColor(Color.BLACK); gravity=Gravity.CENTER })
            totalTxt = TextView(this).apply{ text="Loading real..."; setBackgroundColor(Color.WHITE); setPadding(20,20,20,20) }
            root.addView(totalTxt)
            val btnRow = LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL }
            val saveBtn = Button(this).apply{ text="SAVE" }
            val histBtn = Button(this).apply{ text="HISTORY" }
            btnRow.addView(saveBtn, LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(0,0,8,0) })
            btnRow.addView(histBtn, LinearLayout.LayoutParams(0,-2,1f))
            root.addView(btnRow)
            listLayout = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL }
            root.addView(listLayout)
            root.addView(Button(this).apply{ text="BACK"; setOnClickListener{ finish() } })
            loadProducts()
            render()
            saveBtn.setOnClickListener{ save() }
            histBtn.setOnClickListener{ history() }
            scroll.addView(root)
            setContentView(scroll)
        }catch(e:Exception){ setContentView(TextView(this).apply{ text="Error: ${e.message}" }) }
    }

    private fun loadProducts(){
        names.clear(); inHands.clear(); costs.clear(); sells.clear(); countedList.clear()
        val seen = HashSet<String>()
        val prefs = listOf("products_main","products_rukudzo")
        for(pn in prefs){
            try{
                val sp = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in sp.all){
                    if(seen.contains(k)) continue
                    try{
                        val raw=v.toString()
                        var n=k; var q=0; var c=0.0; var s=0.0
                        if(raw.trim().startsWith("{")){
                            val o=JSONObject(raw)
                            n=o.optString("name",k)
                            q=o.optInt("quantity", o.optInt("qty",0))
                            c=o.optDouble("cost",0.0)
                            s=o.optDouble("price", o.optDouble("sellPrice",0.0))
                        } else {
                            val parts=raw.split("|")
                            n=parts.getOrNull(0)?:k
                            q=parts.getOrNull(1)?.toIntOrNull()?:0
                        }
                        names.add(n); inHands.add(q); costs.add(c); sells.add(s); countedList.add(q); seen.add(k)
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        if(names.isEmpty()){ names.add("Sugar 2kg"); inHands.add(200); costs.add(2.5); sells.add(3.0); countedList.add(200) }
    }

    private fun render(){
        listLayout.removeAllViews()
        var totalVarVal=0.0
        for(i in names.indices){
            val variance = countedList[i] - inHands[i]
            totalVarVal += variance * costs[i]
            val card = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(18,18,18,18); val lp=LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,12,0,0); layoutParams=lp }
            card.addView(TextView(this).apply{ text="${i+1}. ${names[i]}"; textSize=16f; setTextColor(Color.BLACK) })
            card.addView(TextView(this).apply{ text="InHand:${inHands[i]} Counted:${countedList[i]} Var:$variance Cost:$${costs[i]}"; textSize=14f })
            val et = EditText(this).apply{ hint="Counted"; setText(countedList[i].toString()) }
            val idx=i
            val upBtn = Button(this).apply{ text="Update" }
            upBtn.setOnClickListener{
                try{
                    val nc = et.text.toString().toIntOrNull()?: countedList[idx]
                    countedList[idx]=nc
                    render()
                }catch(_:Exception){}
            }
            card.addView(et); card.addView(upBtn)
            listLayout.addView(card)
        }
        totalTxt.text="Products:${names.size} | Total Var Value:$${"%.2f".format(totalVarVal)}"
    }

    private fun save(){
        try{
            val sp=getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
            val id=System.currentTimeMillis().toString()
            val o=JSONObject()
            o.put("date", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
            o.put("count", names.size)
            var tv=0.0; for(i in names.indices) tv += (countedList[i]-inHands[i])*costs[i]
            o.put("varVal", tv)
            val arr=JSONArray()
            for(i in names.indices){ val jo=JSONObject(); jo.put("name",names[i]); jo.put("inHand",inHands[i]); jo.put("counted",countedList[i]); arr.put(jo) }
            o.put("items",arr)
            sp.edit().putString("take_$id", o.toString()).apply()
            Toast.makeText(this,"Saved Variance $${"%.2f".format(tv)}",Toast.LENGTH_LONG).show()
        }catch(e:Exception){ Toast.makeText(this,"Save fail ${e.message}",Toast.LENGTH_LONG).show() }
    }

    private fun history(){
        try{
            val sp=getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
            if(sp.all.isEmpty()){ Toast.makeText(this,"No history",Toast.LENGTH_SHORT).show(); return }
            val lay=LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(20,20,20,20) }
            lay.addView(TextView(this).apply{ text="HISTORY\n"; textSize=18f })
            for((_,v) in sp.all.entries.sortedByDescending{it.key}.take(10)){
                try{ val o=JSONObject(v.toString()); lay.addView(TextView(this).apply{ text="${o.optString("date")} - ${o.optInt("count")} items Var ${"%.2f".format(o.optDouble("varVal"))}"; setPadding(0,10,0,10) }) }catch(_:Exception){}
            }
            val sv=ScrollView(this); sv.addView(lay)
            android.app.AlertDialog.Builder(this).setView(sv).setPositiveButton("Close",null).show()
        }catch(e:Exception){ Toast.makeText(this,"Hist err ${e.message}",Toast.LENGTH_SHORT).show() }
    }
}