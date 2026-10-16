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
    data class P(val code:String, val name:String, val qty:Int, val cost:Float, val sell:Float)
    var all = mutableListOf<P>()
    var filtered = mutableListOf<P>()
    lateinit var table: LinearLayout
    lateinit var etSearch: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)
        root.addView(TextView(this).apply{ text="Price Management - COST FIXED"; setPadding(16,14,16,14); setBackgroundColor(Color.BLACK); setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD) })
        etSearch = EditText(this).apply{ hint="Search CODE or Name"; setPadding(16,16,16,16) }
        root.addView(etSearch)
        val sv = ScrollView(this)
        table = LinearLayout(this)
        table.orientation = LinearLayout.VERTICAL
        sv.addView(table)
        root.addView(sv, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        load()
        render()
        etSearch.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }
    fun load(){
        all.clear()
        for(pn in listOf("received_stock","products","inventory","stock")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    try{
                        val o=JSONObject(v.toString())
                        val code=o.optString("code",k)
                        val name=o.optString("name","")
                        if(name.isBlank()) continue
                        var qty=0
                        if(o.has("qnty")) qty=o.optInt("qnty",0)
                        else if(o.has("quantity")) qty=o.optInt("quantity",0)
                        else if(o.has("qty")) qty=o.optInt("qty",0)
                        var cost=0f
                        if(o.has("cost")) cost=o.optDouble("cost",0.0).toFloat()
                        else if(o.has("buying_price")) cost=o.optDouble("buying_price",0.0).toFloat()
                        else if(o.has("cost_price")) cost=o.optDouble("cost_price",0.0).toFloat()
                        var sell=0f
                        if(o.has("selling_price")) sell=o.optDouble("selling_price",0.0).toFloat()
                        else if(o.has("price")) sell=o.optDouble("price",0.0).toFloat()
                        if(all.none{it.code==code}) all.add(P(code,name,qty,cost,sell))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
    }
    fun filter(){
        val q=etSearch.text.toString().lowercase().trim()
        filtered = if(q.isBlank()) all else all.filter{it.code.lowercase().contains(q) || it.name.lowercase().contains(q)}.toMutableList()
        render()
    }
    fun render(){
        table.removeAllViews()
        for(p in filtered){
            val row=LinearLayout(this)
            row.setPadding(12,12,12,12)
            row.addView(TextView(this).apply{text=p.code; layoutParams=LinearLayout.LayoutParams(0,-2,0.5f); setTypeface(null, Typeface.BOLD)})
            row.addView(TextView(this).apply{text="${p.name} qty:${p.qty}"; layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
            row.addView(TextView(this).apply{text="Cost:${p.cost}"; layoutParams=LinearLayout.LayoutParams(0,-2,0.6f)})
            row.addView(TextView(this).apply{text="Sell:${p.sell}"; layoutParams=LinearLayout.LayoutParams(0,-2,0.6f); setTypeface(null, Typeface.BOLD)})
            val profit=if(p.cost>0) ((p.sell-p.cost)/p.cost*100).toInt() else 0
            row.addView(TextView(this).apply{text="${profit}%"; layoutParams=LinearLayout.LayoutParams(0,-2,0.5f); setTextColor(Color.BLUE)})
            table.addView(row)
        }
    }
}