package com.smartpos
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : Activity() {
    data class Sale(val date: Date, val product:String, val qty:Int, val total:Float, val profit:Float, val branch:String, val dept:String)
    data class StockItem(val code:String, val name:String, val qty:Int)

    var allSales = mutableListOf<Sale>()
    var filteredSales = mutableListOf<Sale>()
    var allStock = mutableListOf<StockItem>()
    lateinit var tvTotalSales: TextView
    lateinit var tvTotalProfit: TextView
    lateinit var tvLowStock: TextView
    lateinit var barContainer: LinearLayout
    lateinit var pieContainer: LinearLayout
    lateinit var listContainer: LinearLayout
    lateinit var btnFrom: Button
    lateinit var btnTo: Button
    var fromDate: Date? = null
    var toDate: Date? = null
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val sdfFull = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    inner class BarChartView(context: Context, val data: List<Pair<String, Float>>) : View(context) {
        val pBar = Paint().apply{ color=Color.parseColor("#2563EB"); style=Paint.Style.FILL; isAntiAlias=true }
        val pText = Paint().apply{ color=Color.GRAY; textSize=16f; isAntiAlias=true }
        val pVal = Paint().apply{ color=Color.BLACK; textSize=18f; typeface=Typeface.DEFAULT_BOLD; isAntiAlias=true }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if(data.isEmpty()){ canvas.drawText("No data", 20f, 50f, pText); return }
            val max = data.maxOfOrNull{ it.second } ?: 1f
            val bw = (width-60f)/data.size
            val ch = height-50f
            for((i, pair) in data.withIndex()){
                val x = 30f + i*bw
                val bh = (pair.second/max)*ch*0.6f
                val y = ch-bh
                canvas.drawRoundRect(x+2f, y, x+bw-2f, ch, 6f, 6f, pBar)
                canvas.drawText("$${pair.second.toInt()}", x, y-6f, pVal)
                canvas.drawText(pair.first.take(5), x, ch+18f, pText)
            }
        }
    }

    inner class PieChartView(context: Context, val data: List<Pair<String, Float>>) : View(context) {
        val colors = listOf(Color.parseColor("#2563EB"), Color.parseColor("#16A34A"), Color.parseColor("#F59E0B"), Color.RED, Color.MAGENTA)
        val pArc = Paint().apply{ style=Paint.Style.FILL; isAntiAlias=true }
        val pTxt = Paint().apply{ color=Color.WHITE; textSize=20f; typeface=Typeface.DEFAULT_BOLD; isAntiAlias=true; textAlign=Paint.Align.CENTER }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if(data.isEmpty()) return
            val total = data.sumOf{ it.second.toDouble() }.toFloat()
            if(total==0f) return
            val cx=width/2f; val cy=height/2f; val r=Math.min(width,height)*0.35f
            var start=-90f
            for((i, pair) in data.withIndex()){
                val sweep=pair.second/total*360f
                pArc.color=colors[i%colors.size]
                canvas.drawArc(cx-r, cy-r, cx+r, cy+r, start, sweep, true, pArc)
                start+=sweep
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this); root.orientation=LinearLayout.VERTICAL; root.setBackgroundColor(Color.parseColor("#F3F4F6"))

        root.addView(TextView(this).apply{ text="SMART POS - Reports"; setBackgroundColor(Color.BLACK); setTextColor(Color.WHITE); setPadding(16,14,16,14); setTypeface(null, Typeface.BOLD) })
        btnFrom=Button(this).apply{ text="FROM: SELECT DATE"; setOnClickListener{ pickDate(true) } }
        btnTo=Button(this).apply{ text="TO: SELECT DATE"; setOnClickListener{ pickDate(false) } }
        val dateRow=LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; addView(btnFrom.apply{ layoutParams=LinearLayout.LayoutParams(0,-2,1f) }); addView(btnTo.apply{ layoutParams=LinearLayout.LayoutParams(0,-2,1f) }) }
        root.addView(dateRow)

        val cards=LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL }
        tvTotalSales=TextView(this).apply{ text="$0.0"; gravity=Gravity.CENTER; setTypeface(null, Typeface.BOLD) }
        tvTotalProfit=TextView(this).apply{ text="$0.0"; gravity=Gravity.CENTER; setTypeface(null, Typeface.BOLD) }
        tvLowStock=TextView(this).apply{ text="0 items"; gravity=Gravity.CENTER; setTypeface(null, Typeface.BOLD) }
        fun card(tv: TextView, title:String): LinearLayout {
            val c=LinearLayout(this); c.orientation=LinearLayout.VERTICAL; c.setBackgroundColor(Color.WHITE); c.setPadding(10,10,10,10); c.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,4,4,4)}; c.gravity=Gravity.CENTER
            c.addView(TextView(this).apply{ text=title; gravity=Gravity.CENTER; textSize=10f })
            c.addView(tv); return c
        }
        cards.addView(card(tvTotalSales, "Total Sales")); cards.addView(card(tvTotalProfit, "Total Profit")); cards.addView(card(tvLowStock, "Low Stock"))
        root.addView(cards)

        barContainer=LinearLayout(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,220); setBackgroundColor(Color.WHITE) }
        root.addView(TextView(this).apply{ text="Daily Sales - BAR CHART"; setTypeface(null, Typeface.BOLD) }); root.addView(barContainer)

        pieContainer=LinearLayout(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,250); setBackgroundColor(Color.WHITE) }
        root.addView(TextView(this).apply{ text="Profit by Dept - PIE CHART"; setTypeface(null, Typeface.BOLD) }); root.addView(pieContainer)

        listContainer=LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL }
        val scroll=ScrollView(this).apply{ addView(listContainer) }
        root.addView(scroll.apply{ layoutParams=LinearLayout.LayoutParams(-1,0,1f) })

        setContentView(root)
        val cal=Calendar.getInstance(); toDate=cal.time; btnTo.text="TO: ${sdf.format(toDate!!)}"; cal.add(Calendar.DAY_OF_MONTH, -30); fromDate=cal.time; btnFrom.text="FROM: ${sdf.format(fromDate!!)}"
        loadData(); render()
    }

    fun pickDate(isFrom:Boolean){
        val cal=Calendar.getInstance()
        DatePickerDialog(this, { _, y,m,d ->
            cal.set(y,m,d)
            if(isFrom){ fromDate=cal.time; btnFrom.text="FROM: ${sdf.format(fromDate!!)}" } else { cal.set(Calendar.HOUR_OF_DAY, 23); toDate=cal.time; btnTo.text="TO: ${sdf.format(cal.time)}" }
            render()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun loadData(){
        allSales.clear(); allStock.clear()
        for(pn in listOf("sales","sales_history","pos_sales","transactions","invoices","receipts")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    try{
                        val o=JSONObject(v.toString()); val prod=o.optString("product_name", o.optString("name","")); if(prod.isBlank()) continue
                        val qty=o.optInt("qty", 1); val sell=o.optDouble("selling_price", o.optDouble("price",0.0)).toFloat(); val cost=o.optDouble("cost",0.0).toFloat()
                        val total=o.optDouble("total", (qty*sell).toDouble()).toFloat(); val profit=(sell-cost)*qty
                        val branch=o.optString("branch","All Branches"); val dept=o.optString("category","General")
                        val dateStr=o.optString("date", sdfFull.format(Date())); val date=try{ sdfFull.parse(dateStr) ?: Date() }catch(_:Exception){ Date() }
                        allSales.add(Sale(date, prod, qty, total, profit, branch, dept))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        for(pn in listOf("received_stock","products","inventory")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in pref.all){ try{ val o=JSONObject(v.toString()); val name=o.optString("name",""); var qty=0; for(qk in listOf("qnty","quantity","qty")){ if(o.has(qk)){ qty=o.optInt(qk,0); break } }; if(name.isNotBlank()) allStock.add(StockItem(k, name, qty)) }catch(_:Exception){} }
            }catch(_:Exception){}
        }
    }

    fun render(){
        filteredSales = allSales.filter{ (fromDate==null || !it.date.before(fromDate)) && (toDate==null || !it.date.after(toDate)) }.toMutableList()
        val totalSales = filteredSales.sumOf{ it.total.toDouble() }.toFloat()
        val totalProfit = filteredSales.sumOf{ it.profit.toDouble() }.toFloat()
        tvTotalSales.text="$${String.format("%.1f", totalSales)}"
        tvTotalProfit.text="$${String.format("%.1f", totalProfit)}"
        tvLowStock.text="${allStock.filter{ it.qty<5 }.size} items"

        val daily = filteredSales.groupBy{ sdf.format(it.date) }.mapValues{ it.value.sumOf{ s->s.total.toDouble() }.toFloat() }.toList().takeLast(7)
        barContainer.removeAllViews(); if(daily.isNotEmpty()) barContainer.addView(BarChartView(this, daily))

        val dept = filteredSales.groupBy{ it.dept }.mapValues{ it.value.sumOf{ s->s.profit.toDouble() }.toFloat() }.toList()
        pieContainer.removeAllViews(); if(dept.isNotEmpty()) pieContainer.addView(PieChartView(this, dept))

        listContainer.removeAllViews()
        for(s in filteredSales.take(20)){
            val row=LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setPadding(8,8,8,8); setBackgroundColor(Color.WHITE) }
            row.addView(TextView(this).apply{ text=s.product; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            row.addView(TextView(this).apply{ text="$${s.total}"; setTypeface(null, Typeface.BOLD) })
            listContainer.addView(row)
        }
    }
}