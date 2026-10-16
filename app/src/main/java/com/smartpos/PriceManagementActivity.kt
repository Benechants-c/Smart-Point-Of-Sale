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
import org.json.JSONObject

class PriceManagementActivity : Activity() {
    data class Product(val code:String, val name:String, val qty:Int, val cost:Float, val sell:Float)
    var all=mutableListOf<Product>()
    var filtered=mutableListOf<Product>()
    lateinit var table: LinearLayout
    lateinit var etSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this); root.orientation=LinearLayout.VERTICAL; root.setBackgroundColor(Color.WHITE)
        root.addView(TextView(this).apply{ text="Price Management"; setBackgroundColor(Color.parseColor("#111827")); setTextColor(Color.WHITE); setPadding(16,14,16,14); setTypeface(null, Typeface.BOLD) })
        etSearch=EditText(this).apply{ hint="Search CODE or Name"; setPadding(16,16,16,16); setBackgroundColor(Color.parseColor("#F3F4F6")) }
        root.addView(etSearch)
        val th=LinearLayout(this).apply{ setBackgroundColor(Color.parseColor("#1F2937")); setPadding(4,8,4,8) }
        fun tht(t:String,w:Float): TextView { val tv=TextView(this); tv.text=t; tv.setTextColor(Color.WHITE); tv.setTypeface(null, Typeface.BOLD); tv.textSize=10f; tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv }
        th.addView(tht("CODE",0.7f)); th.addView(tht("PRODUCT",1f)); th.addView(tht("COST",0.6f)); th.addView(tht("SELL",0.6f)); th.addView(tht("PROFIT%",0.7f))
        root.addView(th)
        val scroll=ScrollView(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,0,1f) }
        table=LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL }
        scroll.addView(table); root.addView(scroll); setContentView(root)
        loadProducts(); render()
        etSearch.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    fun loadProducts(){
        all.clear()
        for(pn in listOf("received_stock","products","inventory","stock")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    try{
                        val o=JSONObject(v.toString())
                        val code=o.optString("code", o.optString("sku", k))
                        val name=o.optString("name", o.optString("product_name",""))
                        if(name.isBlank()) continue
                        var qty=0
                        for(qk in listOf("qnty","quantity","qty")){ if(o.has(qk)){ qty=o.optString(qk,"0").toIntOrNull() ?: o.optInt(qk,0); if(qty!=0) break } }
                        var cost=0f
                        for(ck in listOf("cost","buying_price","cost_price","purchase_price")){ if(o.has(ck)){ cost=o.optString(ck,"0").toFloatOrNull() ?: o.optDouble(ck,0.0).toFloat(); if(cost!=0f) break } }
                        var sell=0f
                        for(sk in listOf("selling_price","price","sell_price")){ if(o.has(sk)){ sell=o.optString(sk,"0").toFloatOrNull() ?: o.optDouble(sk,0.0).toFloat(); if(sell!=0f) break } }
                        if(all.none{ it.code==code }) all.add(Product(code, name, qty, cost, sell))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
    }

    fun filter(){
        val q=etSearch.text.toString().lowercase().trim()
        filtered = if(q.isBlank()) all else all.filter{ it.code.lowercase().contains(q) || it.name.lowercase().contains(q) }.toMutableList()
        render()
    }

    fun render(){
        table.removeAllViews()
        for(p in filtered){
            val row=LinearLayout(this).apply{ setPadding(8,10,8,10); setBackgroundColor(Color.WHITE) }
            fun td(t:String,w:Float): TextView { val tv=TextView(this); tv.text=t; tv.textSize=11f; tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv }
            row.addView(td(p.code,0.7f)); row.addView(TextView(this).apply{ text=p.name; textSize=11f; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            row.addView(td("$${p.cost}",0.6f)); row.addView(td("$${p.sell}",0.6f))
            val profit=if(p.cost>0) ((p.sell-p.cost)/p.cost*100) else 0f
            row.addView(td("${profit.toInt()}%",0.7f).apply{ setTextColor(if(profit>0) Color.parseColor("#16A34A") else Color.RED) })
            table.addView(row)
        }
    }
}