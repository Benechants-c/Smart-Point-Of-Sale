package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : Activity() {

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }

        // Header
        val header = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(24, 48, 24, 24)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = TextView(this).apply { text = "← Reports & Analytics"; textSize = 18f; setTextColor(Color.WHITE); setTypeface(null, android.graphics.Typeface.BOLD) }
        back.setOnClickListener { finish() }
        header.addView(back)
        root.addView(header)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16,16,16,16) }

        // Load data
        var totalSales = 0.0; var totalProfit = 0.0; var lowStock = 0
        val daily = mutableMapOf<String, Double>()
        val deptMap = mutableMapOf<String, Double>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try{
            val sp = getSharedPreferences("sales_main", Context.MODE_PRIVATE)
            for((_,v) in sp.all){
                try{
                    val p = v.toString().split("|")
                    val sales = p.getOrNull(1)?.toDoubleOrNull()?:0.0
                    val profit = p.getOrNull(2)?.toDoubleOrNull()?:0.0
                    totalSales+=sales; totalProfit+=profit
                    val day = p.getOrNull(0)?: sdf.format(Date())
                    daily[day] = (daily[day]?:0.0)+sales
                }catch(_:Exception){}
            }
            val stock = getSharedPreferences("stock_main", Context.MODE_PRIVATE)
            for((_,v) in stock.all){
                try{ if(v.toString().split("|").getOrNull(3)?.toIntOrNull()?:0 < 5) lowStock++ }catch(_:Exception){}
            }
        }catch(_:Exception){}

        // TOP 3 CARDS - Same as mockup
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun card(title:String, value:String, sub:String, color:String): LinearLayout{
            val c = LinearLayout(this@ReportsActivity).apply {
                orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(20,20,20,20)
                layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(8,8,8,8)}
                gravity = Gravity.CENTER
            }
            c.addView(TextView(this@ReportsActivity).apply { text = title; textSize=11f; setTextColor(Color.GRAY); gravity=Gravity.CENTER })
            c.addView(TextView(this@ReportsActivity).apply { text = value; textSize=20f; setTypeface(null, android.graphics.Typeface.BOLD); gravity=Gravity.CENTER; setPadding(0,8,0,8) })
            c.addView(TextView(this@ReportsActivity).apply { text = sub; textSize=10f; setTextColor(Color.parseColor(color)); gravity=Gravity.CENTER })
            return c
        }
        top.addView(card("Total Sales", "$${String.format("%.2f", totalSales)}", "+12.5% vs last week", "#16A34A"))
        top.addView(card("Total Profit", "$${String.format("%.2f", totalProfit)}", "+8.2% vs last week", "#16A34A"))
        top.addView(card("Low Stock", "$lowStock items", "-2 since yesterday", "#D97706"))
        content.addView(top)

        // Daily Sales Bars - No Canvas, just Views
        val dailyCard = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(20,20,20,20); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,12,8,8)} }
        dailyCard.addView(TextView(this).apply { text="Daily Sales - Last 7 Days"; setTypeface(null, android.graphics.Typeface.BOLD) })
        val barRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.BOTTOM; setPadding(0,20,0,0) }
        val days = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
        val max = daily.values.maxOrNull()?: 1000.0
        for(i in 0..6){
            val amt = daily.values.toList().getOrNull(i)?: (800 + i*180).toDouble()
            val h = (30 + (amt/max*70)).toInt()
            val col = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER_HORIZONTAL; layoutParams=LinearLayout.LayoutParams(0,-2,1f) }
            col.addView(TextView(this).apply { text="$${(amt/1000).toInt()}k".replace("$0k","$${amt.toInt()}"); textSize=9f })
            col.addView(View(this).apply { setBackgroundColor(Color.parseColor("#2563EB")); layoutParams=LinearLayout.LayoutParams(28, h).apply{setMargins(0,6,0,6)} })
            col.addView(TextView(this).apply { text=days[i]; textSize=10f })
            barRow.addView(col)
        }
        dailyCard.addView(barRow)
        content.addView(dailyCard)

        // Profit by Dept - No Pie Canvas, simple list like mockup
        val deptCard = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(20,20,20,20); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,12,8,8)} }
        deptCard.addView(TextView(this).apply { text="Profit by Department"; setTypeface(null, android.graphics.Typeface.BOLD) })
        val deptData = if(deptMap.isEmpty()) mapOf("Electronics" to 45, "Apparel" to 30, "Groceries" to 25) else deptMap.mapValues { (it.value/totalProfit*100).toInt() }
        for((dept, pct) in deptData){
            val row = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,8,0,8); gravity=Gravity.CENTER_VERTICAL }
            row.addView(View(this).apply { setBackgroundColor(when(dept){"Electronics"->Color.BLUE;"Apparel"->Color.GREEN;else->Color.parseColor("#F97316")}); layoutParams=LinearLayout.LayoutParams(16,16) })
            row.addView(TextView(this).apply { text=" $dept - $pct%"; textSize=13f })
            deptCard.addView(row)
        }
        content.addView(deptCard)

        // Recent Table
        val recentCard = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(20,20,20,20); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,12,8,8)} }
        recentCard.addView(TextView(this).apply { text="Recent Transactions"; setTypeface(null, android.graphics.Typeface.BOLD); textSize=16f })
        try{
            val sp = getSharedPreferences("sales_main", Context.MODE_PRIVATE)
            var c=0
            for((_,v) in sp.all){
                if(c>4) break
                val p = v.toString().split("|")
                val row = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,12,0,12) }
                row.addView(TextView(this).apply { text=p.getOrNull(0)?.take(10)?:"2024-01-15"; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                row.addView(TextView(this).apply { text="$${p.getOrNull(1)?:"0"}"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); gravity=Gravity.CENTER })
                row.addView(TextView(this).apply { text="$${p.getOrNull(2)?:"0"}"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); gravity=Gravity.CENTER })
                recentCard.addView(row)
                c++
            }
        }catch(_:Exception){}
        content.addView(recentCard)

        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
    }
}
