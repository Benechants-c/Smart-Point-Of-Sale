package com.smartpos
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.*
import android.graphics.Color
import android.view.Gravity
import org.json.JSONObject

data class StockItem436(var key:String, var name:String, var inHand:Int, var cost:Double, var sell:Double, var counted:Int)

class StockTakeActivity : Activity() {
    private val items = mutableListOf<StockItem436>()
    private lateinit var listLayout: LinearLayout
    private lateinit var totalTxt: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try {
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(24,24,24,24); setBackgroundColor(Color.parseColor("#F5F5F5")) }
            
            root.addView(TextView(this).apply{ text="STOCK TAKE #436 FINAL"; textSize=20f; setTextColor(Color.BLACK); gravity=Gravity.CENTER; setPadding(0,10,0,10) })
            root.addView(TextView(this).apply{ text="Real stock from products_main + rukudzo"; textSize=13f; gravity=Gravity.CENTER; setPadding(0,0,0,15) })

            totalTxt = TextView(this).apply{ text="Loading..."; textSize=15f; setBackgroundColor(Color.WHITE); setPadding(20,20,20,20) }
            root.addView(totalTxt)

            // Buttons
            val btnRow = LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL }
            val saveBtn = Button(this).apply{ text="SAVE TAKE"; }
            val histBtn = Button(this).apply{ text="HISTORY"; }
            btnRow.addView(saveBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply{ setMargins(0,0,10,0) })
            btnRow.addView(histBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            root.addView(btnRow)

            listLayout = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL }
            root.addView(listLayout)

            val back = Button(this).apply{ text="BACK"; setOnClickListener{ finish() } }
            root.addView(back)

            loadRealProducts()
            renderList()

            saveBtn.setOnClickListener{ saveTake() }
            histBtn.setOnClickListener{ showHistory() }

            scroll.addView(root)
            setContentView(scroll)

        } catch(e:Exception){
            setContentView(TextView(this).apply{ text="CRASH #436: ${e.message}\n${e.stackTraceToString().take(1000)}" })
        }
    }

    private fun loadRealProducts(){
        items.clear()
        val seen = HashSet<String>()
        val prefNames = listOf("products_main","products_rukudzo","products","products_main_branch")
        for(pn in prefNames){
            try{
                val sp = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in sp.all){
                    if(seen.contains(k)) continue
                    try{
                        val raw = v.toString()
                        var name=k; var qty=0; var cost=0.0; var sell=0.0
                        if(raw.trim().startsWith("{")){
                            val o = JSONObject(raw)
                            name = o.optString("name", o.optString("productName", k))
                            qty = o.optInt("quantity", o.optInt("qty", o.optInt("stock",0)))
                            cost = o.optDouble("cost", o.optDouble("costPrice",0.0))
                            sell = o.optDouble("price", o.optDouble("sellPrice", o.optDouble("sellingPrice",0.0)))
                        } else {
                            val p = raw.split("|")
                            name = p.getOrNull(0) ?: k
                            qty = p.getOrNull(1)?.toIntOrNull() ?: 0
                            cost = p.getOrNull(2)?.toDoubleOrNull() ?: 0.0
                            sell = p.getOrNull(3)?.toDoubleOrNull() ?: cost
                        }
                        if(name.isNotBlank()){ items.add(StockItem436(k,name,qty,cost,sell,qty)); seen.add(k) }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        if(items.isEmpty()){
            items.add(StockItem436("sugar","Sugar 2kg",200,2.5,3.0,200))
        }
    }

    private fun renderList(){
        listLayout.removeAllViews()
        var totalVarVal = 0.0
        for((idx, it) in items.withIndex()){
            val variance = it.counted - it.inHand
            totalVarVal += variance * it.cost

            val card = LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                setPadding(18,18,18,18)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0,12,0,0); layoutParams = lp
            }
            card.addView(TextView(this).apply{ text="${idx+1}. ${it.name}"; textSize=16f; setTextColor(Color.BLACK) })
            card.addView(TextView(this).apply{ text="In Hand: ${it.inHand} | Counted: ${it.counted} | Var: $variance"; textSize=14f })
            card.addView(TextView(this).apply{ text="Cost: $${it.cost} Sell: $${it.sell} | Var Value: $${"%.2f".format(variance*it.cost)}"; textSize=13f; setTextColor(if(variance<0) Color.RED else Color.parseColor("#0A7A00")) })

            val edit = EditText(this).apply{
                hint="Enter counted"
                setText(it.counted.toString())
                setPadding(15,15,15,15)
            }
            val updateBtn = Button(this).apply{ text="Update Counted" }
            updateBtn.setOnClickListener{
                try{
                    val newCnt = edit.text.toString().toIntOrNull() ?: it.counted
                    items[idx] = it.copy(counted = newCnt)
                    renderList()
                }catch(_:Exception){}
            }
            card.addView(edit)
            card.addView(updateBtn)
            listLayout.addView(card)
        }
        totalTxt.text="Products: ${items.size} | Total Variance Value: $${"%.2f".format(totalVarVal)}"
    }

    private fun saveTake(){
        try{
            val sp = getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
            val id = System.currentTimeMillis().toString()
            val obj = JSONObject()
            obj.put("date", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
            obj.put("count", items.size)
            var totalVar = 0.0
            for(i in items) totalVar += (i.counted - i.inHand) * i.cost
            obj.put("varianceValue", totalVar)
            val arr = org.json.JSONArray()
            for(i in items){
                val o = JSONObject()
                o.put("name", i.name); o.put("inHand", i.inHand); o.put("counted", i.counted); o.put("variance", i.counted-i.inHand)
                arr.put(o)
            }
            obj.put("items", arr)
            sp.edit().putString("take_$id", obj.toString()).apply()
            Toast.makeText(this, "Saved! Variance: $${"%.2f".format(totalVar)}", Toast.LENGTH_LONG).show()
        }catch(e:Exception){ Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun showHistory(){
        try{
            val sp = getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
            if(sp.all.isEmpty()){ Toast.makeText(this,"No history yet",Toast.LENGTH_SHORT).show(); return }
            val dlg = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(20,20,20,20) }
            val tv = TextView(this).apply{ text="STOCK TAKE HISTORY\n"; textSize=17f }
            dlg.addView(tv)
            val sorted = sp.all.entries.sortedByDescending { it.key }
            for((k,v) in sorted.take(10)){
                try{
                    val o = JSONObject(v.toString())
                    dlg.addView(TextView(this).apply{ text="${o.optString("date")} - ${o.optInt("count")} items Var:$${"%.2f".format(o.optDouble("varianceValue"))}"; setPadding(0,10,0,10) })
                }catch(_:Exception){}
            }
            val sc = ScrollView(this); sc.addView(dlg)
            val d = android.app.AlertDialog.Builder(this).setView(sc).setPositiveButton("Close",null).create(); d.show()
        }catch(e:Exception){ Toast.makeText(this,"History error: ${e.message}",Toast.LENGTH_SHORT).show() }
    }
}