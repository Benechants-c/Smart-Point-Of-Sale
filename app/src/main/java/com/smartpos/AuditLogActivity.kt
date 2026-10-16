package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AuditLogActivity : Activity() {
    data class Log(val date:Date, val type:String, val name:String, val qty:Int, val total:Float)
    var all=mutableListOf<Log>()
    lateinit var table: LinearLayout
    val sdfFull=SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this)
        root.orientation=LinearLayout.VERTICAL
        root.addView(TextView(this).apply{text="IJ Audit Trail"; setPadding(16,14,16,14); setBackgroundColor(Color.BLACK); setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD)})
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
        for(pn in listOf("sales","pos_sales","transactions")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    try{
                        val o=JSONObject(v.toString())
                        val name=o.optString("product_name", o.optString("name",""))
                        if(name.isBlank()) continue
                        val qty=o.optInt("qty",1)
                        val total=o.optDouble("total",0.0).toFloat()
                        val dateStr=o.optString("date", sdfFull.format(Date()))
                        val date=try{sdfFull.parse(dateStr)?:Date()}catch(_:Exception){Date()}
                        all.add(Log(date,"SALES",name,qty,total))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        for(pn in listOf("received_stock","products")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    try{
                        val o=JSONObject(v.toString())
                        val name=o.optString("name","")
                        if(name.isBlank()) continue
                        var q=0
                        if(o.has("qnty")) q=o.optInt("qnty",0)
                        val cost=o.optDouble("cost",0.0).toFloat()
                        val dateStr=o.optString("date", sdfFull.format(Date()))
                        val date=try{sdfFull.parse(dateStr)?:Date()}catch(_:Exception){Date()}
                        all.add(Log(date,"STOCK",name,q,cost*q))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        all.sortByDescending{it.date}
    }
    fun render(){
        table.removeAllViews()
        if(all.isEmpty()){
            table.addView(TextView(this).apply{text="No logs yet"; setPadding(0,40,0,0); gravity=1})
            return
        }
        for(l in all.take(100)){
            val row=LinearLayout(this)
            row.setPadding(8,8,8,8)
            row.addView(TextView(this).apply{text=sdfFull.format(l.date); textSize=10f; layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
            row.addView(TextView(this).apply{text=l.type; textSize=10f; setTypeface(null, Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,0.5f)})
            row.addView(TextView(this).apply{text=l.name; textSize=10f; layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
            row.addView(TextView(this).apply{text="${l.qty}"; textSize=10f; layoutParams=LinearLayout.LayoutParams(0,-2,0.3f)})
            row.addView(TextView(this).apply{text="$${l.total}"; textSize=10f; layoutParams=LinearLayout.LayoutParams(0,-2,0.5f)})
            table.addView(row)
        }
    }
}