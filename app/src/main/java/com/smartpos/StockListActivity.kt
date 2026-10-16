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

class StockListActivity : Activity() {
    data class Product(val code:String, val name:String, val qty:Int, val cost:Float, val price:Float, val branch:String)
    var all=mutableListOf<Product>()
    var filtered=mutableListOf<Product>()
    lateinit var table: LinearLayout
    lateinit var etSearch: EditText
    lateinit var tvCost: TextView
    lateinit var tvSell: TextView
    lateinit var tvCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this); root.orientation=LinearLayout.VERTICAL; root.setBackgroundColor(Color.WHITE)
        root.addView(TextView(this).apply{ text="View Stock"; setBackgroundColor(Color.parseColor("#111827")); setTextColor(Color.WHITE); setPadding(16,14,16,14); setTypeface(null, Typeface.BOLD) })

        val cards=LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setPadding(8,8,8,8) }
        fun card(title:String): LinearLayout {
            val c=LinearLayout(this); c.orientation=LinearLayout.VERTICAL; c.setBackgroundColor(Color.WHITE); c.setPadding(8,10,8,10); c.gravity=Gravity.CENTER; c.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)}
            c.addView(TextView(this).apply{ text=title; textSize=9f; gravity=Gravity.CENTER }); val tv=TextView(this).apply{ text="$0"; setTypeface(null, Typeface.BOLD); gravity=Gravity.CENTER }; c.addView(tv); return c
        }
        val c1=card("Total at COST"); tvCost=c1.getChildAt(1) as TextView
        val c2=card("Total at SELLING"); tvSell=c2.getChildAt(1) as TextView
        val c3=card("Products"); tvCount=c3.getChildAt(1) as TextView
        cards.addView(c1); cards.addView(c2); cards.addView(c3); root.addView(cards)

        etSearch=EditText(this).apply{ hint="Search name or code..."; setBackgroundColor(Color.parseColor("#F3F4F6")); setPadding(16,16,16,16) }
        root.addView(etSearch)

        val scroll=ScrollView(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,0,1f) }
        table=LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL }
        scroll.addView(table); root.addView(scroll); setContentView(root)

        loadStock(); render()
        etSearch.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    fun loadStock(){
        all.clear()
        for(pn in listOf("received_stock","stock_received","products","inventory","stock","products_list")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    try{
                        val o=JSONObject(v.toString())
                        val code=o.optString("code", o.optString("sku", k))
                        val name=o.optString("name", o.optString("product_name",""))
                        if(name.isBlank()) continue
                        var qty=0; for(qk in listOf("qnty","quantity","qty","received_qty")){ if(o.has(qk)){ qty=o.optString(qk,"0").toIntOrNull() ?: o.optInt(qk,0); if(qty!=0) break } }
                        var cost=0f; for(ck in listOf("cost","buying_price","cost_price")){ if(o.has(ck)){ cost=o.optString(ck,"0").toFloatOrNull() ?: 0f; if(cost!=0f) break } }
                        var price=0f; for(sk in listOf("selling_price","price","sell_price")){ if(o.has(sk)){ price=o.optString(sk,"0").toFloatOrNull() ?: 0f; if(price!=0f) break } }
                        val branch=o.optString("branch","Main Shop")
                        if(all.none{ it.code==code }) all.add(Product(code, name, qty, cost, price, branch))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
    }

    fun filter(){
        val q=etSearch.text.toString().lowercase().trim()
        filtered = if(q.isBlank()) all else all.filter{ it.name.lowercase().contains(q) || it.code.lowercase().contains(q) }.toMutableList()
        render()
    }

    fun render(){
        table.removeAllViews()
        var tc=0f; var ts=0f
        for(p in filtered){ tc+=p.qty*p.cost; ts+=p.qty*p.price }
        tvCost.text="$${String.format("%.2f", tc)}"; tvSell.text="$${String.format("%.2f", ts)}"; tvCount.text="${filtered.size}"
        for(p in filtered){
            val row=LinearLayout(this).apply{ setPadding(8,8,8,8); setBackgroundColor(Color.WHITE) }
            fun td(t:String,w:Float): TextView { val tv=TextView(this); tv.text=t; tv.textSize=11f; tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv }
            row.addView(td(p.code,0.7f)); row.addView(TextView(this).apply{ text=p.name; layoutParams=LinearLayout.LayoutParams(0,-2,1f) }); row.addView(td("${p.qty}",0.5f)); row.addView(td("$${p.cost}",0.6f)); row.addView(td("$${p.price}",0.6f))
            table.addView(row)
        }
        if(filtered.isEmpty()) table.addView(TextView(this).apply{ text="No stock - Add products in Received Stock"; gravity=Gravity.CENTER; setPadding(0,40,0,0); setTextColor(Color.GRAY) })
    }
}