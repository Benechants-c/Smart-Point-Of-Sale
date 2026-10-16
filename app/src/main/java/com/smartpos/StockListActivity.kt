package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*
import org.json.JSONObject

class StockListActivity : Activity() {
    data class P(val code:String, val name:String, val qty:Int, val cost:Float, val sell:Float)
    var all=mutableListOf<P>()
    lateinit var table: LinearLayout
    lateinit var tvCost: TextView
    lateinit var tvSell: TextView
    lateinit var tvCount: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this)
        root.orientation=LinearLayout.VERTICAL
        root.addView(TextView(this).apply{text="View Stock"; setPadding(16,14,16,14); setBackgroundColor(Color.BLACK); setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD)})
        val cards=LinearLayout(this)
        cards.orientation=LinearLayout.HORIZONTAL
        tvCost=TextView(this).apply{text="Cost $0"; gravity=1; setPadding(10,10,10,10)}
        tvSell=TextView(this).apply{text="Sell $0"; gravity=1; setPadding(10,10,10,10)}
        tvCount=TextView(this).apply{text="0"; gravity=1; setPadding(10,10,10,10)}
        cards.addView(tvCost, LinearLayout.LayoutParams(0,-2,1f))
        cards.addView(tvSell, LinearLayout.LayoutParams(0,-2,1f))
        cards.addView(tvCount, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(cards)
        val sv=ScrollView(this)
        table=LinearLayout(this)
        table.orientation=LinearLayout.VERTICAL
        sv.addView(table)
        root.addView(sv, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        load()
        render()
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
                        var sell=0f
                        if(o.has("selling_price")) sell=o.optDouble("selling_price",0.0).toFloat()
                        else if(o.has("price")) sell=o.optDouble("price",0.0).toFloat()
                        if(all.none{it.code==code}) all.add(P(code,name,qty,cost,sell))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
    }
    fun render(){
        table.removeAllViews()
        var tc=0f
        var ts=0f
        for(p in all){ tc+=p.qty*p.cost; ts+=p.qty*p.sell }
        tvCost.text="Cost $${String.format("%.2f",tc)}"
        tvSell.text="Sell $${String.format("%.2f",ts)}"
        tvCount.text="${all.size} products"
        for(p in all){
            val row=LinearLayout(this)
            row.setPadding(8,8,8,8)
            row.addView(TextView(this).apply{text=p.code; layoutParams=LinearLayout.LayoutParams(0,-2,0.6f)})
            row.addView(TextView(this).apply{text=p.name; layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
            row.addView(TextView(this).apply{text="${p.qty}"; layoutParams=LinearLayout.LayoutParams(0,-2,0.4f)})
            row.addView(TextView(this).apply{text="$${p.cost}"; layoutParams=LinearLayout.LayoutParams(0,-2,0.5f)})
            row.addView(TextView(this).apply{text="$${p.sell}"; layoutParams=LinearLayout.LayoutParams(0,-2,0.5f)})
            table.addView(row)
        }
    }
}