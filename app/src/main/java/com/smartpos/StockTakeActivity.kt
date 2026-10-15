package com.smartpos
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.*
import org.json.JSONObject

class StockTakeActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try {
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(20,20,20,20) }
            root.addView(TextView(this).apply{ text="STOCK TAKE #433 REAL DATA SAFE"; textSize=18f })
            
            val summary = TextView(this).apply{ text="Loading real products..."; textSize=14f }
            root.addView(summary)

            var count = 0
            val seen = HashSet<String>()
            try{
                val prefs = listOf("products_main","products_rukudzo")
                for(pn in prefs){
                    try{
                        val sp = getSharedPreferences(pn, Context.MODE_PRIVATE)
                        for((k,v) in sp.all){
                            if(seen.contains(k)) continue
                            try{
                                val raw = v.toString()
                                var name = k; var qty = 0
                                if(raw.trim().startsWith("{")){
                                    val o = JSONObject(raw)
                                    name = o.optString("name", k)
                                    qty = o.optInt("quantity", o.optInt("qty",0))
                                } else {
                                    name = raw.split("|").getOrNull(0) ?: k
                                }
                                root.addView(TextView(this).apply{ text="$name - Qty: $qty ($pn)"; textSize=15f; setPadding(10,12,10,12) })
                                seen.add(k); count++
                            }catch(_:Exception){}
                        }
                    }catch(_:Exception){}
                }
            }catch(e:Exception){ root.addView(TextView(this).apply{ text="Error: ${e.message}" }) }

            summary.text="$count REAL products from products_main + rukudzo"
            if(count==0) root.addView(TextView(this).apply{ text="NO PRODUCTS FOUND - Add in POS first" })

            val back = Button(this).apply{ text="BACK"; setOnClickListener{ finish() } }
            root.addView(back)
            scroll.addView(root)
            setContentView(scroll)
        }catch(e:Exception){
            setContentView(TextView(this).apply{ text="CRASH CAUGHT #433: ${e.message}\n${e.stackTraceToString().take(800)}" })
        }
    }
}