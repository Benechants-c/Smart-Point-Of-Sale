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

class ReportsActivity : Activity() {
    data class Sale(val date:Date, val product:String, val total:Float, val profit:Float, val dept:String)
    var allSales=mutableListOf<Sale>()
    var allStockQty=0
    lateinit var tvSales: TextView
    lateinit var tvProfit: TextView
    lateinit var tvLow: TextView
    lateinit var container: LinearLayout
    val sdfFull=SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val sdf=SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this)
        root.orientation=LinearLayout.VERTICAL
        root.addView(TextView(this).apply{text="SMART POS Reports"; setPadding(16,14,16,14); setBackgroundColor(Color.BLACK); setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD)})
        val row=LinearLayout(this)
        row.orientation=LinearLayout.HORIZONTAL
        tvSales=TextView(this).apply{text="Sales $0"; gravity=1; setPadding(10,20,10,20); setBackgroundColor(Color.WHITE)}
        tvProfit=TextView(this).apply{text="Profit $0"; gravity=1; setPadding(10,20,10,20); setBackgroundColor(Color.WHITE)}
        tvLow=TextView(this).apply{text="Low 0"; gravity=1; setPadding(10,20,10,20); setBackgroundColor(Color.WHITE)}
        row.addView(tvSales, LinearLayout.LayoutParams(0,-2,1f))
        row.addView(tvProfit, LinearLayout.LayoutParams(0,-2,1f))
        row.addView(tvLow, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(row)
        val sv=ScrollView(this)
        container=LinearLayout(this)
        container.orientation=LinearLayout.VERTICAL
        sv.addView(container)
        root.addView(sv, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        load()
        render()
    }
    fun load(){
        allSales.clear()
        var low=0
        for(pn in listOf("sales","sales_history","pos_sales","transactions","invoices")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    try{
                        val o=JSONObject(v.toString())
                        val prod=o.optString("product_name", o.optString("name",""))
                        if(prod.isBlank()) continue
                        val qty=o.optInt("qty",1)
                        val sell=o.optDouble("selling_price", o.optDouble("price",0.0)).toFloat()
                        val cost=o.optDouble("cost",0.0).toFloat()
                        val total=o.optDouble("total", (qty*sell).toDouble()).toFloat()
                        val profit=(sell-cost)*qty
                        val dept=o.optString("category","General")
                        val dateStr=o.optString("date", sdfFull.format(Date()))
                        val date=try{sdfFull.parse(dateStr)?:Date()}catch(_:Exception){Date()}
                        allSales.add(Sale(date,prod,total,profit,dept))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        for(pn in listOf("received_stock","products","inventory")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    try{
                        val o=JSONObject(v.toString())
                        var q=0
                        if(o.has("qnty")) q=o.optInt("qnty",0)
                        else if(o.has("quantity")) q=o.optInt("quantity",0)
                        if(q<5) low++
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        allStockQty=low
    }
    fun render(){
        val totalSales=allSales.sumOf{it.total.toDouble()}.toFloat()
        val totalProfit=allSales.sumOf{it.profit.toDouble()}.toFloat()
        tvSales.text="Sales $${String.format("%.1f",totalSales)}\n${allSales.size} sales"
        tvProfit.text="Profit $${String.format("%.1f",totalProfit)}"
        tvLow.text="Low Stock\n$allStockQty items"

        container.removeAllViews()
        container.addView(TextView(this).apply{text="Daily Sales - BAR CHART (Text)"; setTypeface(null, Typeface.BOLD); setPadding(8,16,8,8)})
        val daily=allSales.groupBy{sdf.format(it.date)}.mapValues{it.value.sumOf{ s->s.total.toDouble()}.toFloat()}
        for((day,amt) in daily){
            val r=LinearLayout(this)
            r.orientation=LinearLayout.HORIZONTAL
            r.setPadding(8,4,8,4)
            r.addView(TextView(this).apply{text=day; layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
            val bar=TextView(this)
            val len=(amt/10).toInt().coerceAtMost(20)
            bar.text="█".repeat(len)+" $${amt.toInt()}"
            bar.setTextColor(Color.BLUE)
            bar.layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            r.addView(bar)
            container.addView(r)
        }

        container.addView(TextView(this).apply{text="Profit by Dept - PIE CHART (List)"; setTypeface(null, Typeface.BOLD); setPadding(8,16,8,8)})
        val dept=allSales.groupBy{it.dept}.mapValues{it.value.sumOf{s->s.profit.toDouble()}.toFloat()}
        val totalDept=dept.values.sumOf{it.toDouble()}.toFloat()
        for((d,p) in dept){
            val perc=if(totalDept>0) (p/totalDept*100).toInt() else 0
            container.addView(TextView(this).apply{text="● $d : $${String.format("%.2f",p)} ($perc%)"; setPadding(12,6,12,6)})
        }

        container.addView(TextView(this).apply{text="Recent Transactions"; setTypeface(null, Typeface.BOLD); setPadding(8,16,8,8)})
        for(s in allSales.take(20)){
            container.addView(TextView(this).apply{text="${sdfFull.format(s.date)} - ${s.product} - $${s.total}"; setPadding(8,6,8,6); setBackgroundColor(Color.parseColor("#F9FAFB"))})
        }
    }
}