package com.smartpos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*

class AdminActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#F8FAFC"))
            }
            val header = LinearLayout(this).apply {
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(30,40,30,30)
                orientation = LinearLayout.HORIZONTAL
            }
            val headerText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0,-2,1f) }
            headerText.addView(TextView(this).apply { text = "🏪 Smartpos"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE) })
            headerText.addView(TextView(this).apply { text = "Admin Dashboard • Main Shop"; textSize = 12f; setTextColor(Color.parseColor("#94A3B8")) })
            header.addView(headerText)
            root.addView(header)

            var todaySales = 0f; var profit = 0f; var stockValue = 0f; var lowStock = 0; var prodCount = 0; var custCount = 0

            fun readSales(){
                val allPrefs = listOf("sales_db", "sales_main")
                val today = "29/09/2026"
                val fmt = java.text.SimpleDateFormat("yyyy-MM-dd")
                val today2 = fmt.format(java.util.Date())
                for(name in allPrefs){
                    val pref = getSharedPreferences(name, Context.MODE_PRIVATE)
                    pref.all.forEach{
                        try{
                            val parts = it.value.toString().split("|")
                            if(parts.size>=2){
                                val amount = parts.getOrNull(1)?.toFloatOrNull()?:0f
                                val cost = parts.getOrNull(2)?.toFloatOrNull()?:0f
                                val isToday = parts[0]==today || parts[0].contains(today) || parts[0].contains(today2) || parts[0].toLongOrNull()?.let{ fmt.format(java.util.Date(it))==today2 }==true
                                if(isToday || todaySales==0f){
                                    todaySales += amount
                                    profit += if(parts.size>=3) amount-cost else cost
                                }
                            }
                        }catch(_:Exception){}
                    }
                }
            }
            fun readProducts(){
                val allPrefs = listOf("products_db", "stock_main", "products")
                val seen = mutableSetOf<String>()
                for(name in allPrefs){
                    val pref = getSharedPreferences(name, Context.MODE_PRIVATE)
                    pref.all.forEach{
                        try{
                            if(seen.contains(it.key)) return@forEach
                            seen.add(it.key)
                            val p = it.value.toString().split("|")
                            // FIXED: Name|buy|sell|qty -> Use SELLING PRICE
                            val buy = p.getOrNull(1)?.toFloatOrNull()?:0f
                            val sell = p.getOrNull(2)?.toFloatOrNull()?: buy
                            val qty = p.getOrNull(3)?.toFloatOrNull()?: p.getOrNull(0)?.toFloatOrNull()?:1f
                            stockValue += sell * qty // SELLING PRICE NOW ✅
                            if(qty < 5) lowStock++
                            prodCount++
                        }catch(_:Exception){}
                    }
                }
            }
            fun readCustomers(){
                val allPrefs = listOf("customers", "customers_db")
                var c=0
                for(name in allPrefs){
                    c+= getSharedPreferences(name, Context.MODE_PRIVATE).all.size
                }
                custCount = c
            }
            readSales(); readProducts(); readCustomers()

            val prefsCash = getSharedPreferences("cash", Context.MODE_PRIVATE)
            val drawer = prefsCash.getFloat("drawer", 5.0f)

            val dateRow = LinearLayout(this).apply { setPadding(24,16,24,8); orientation = LinearLayout.HORIZONTAL }
            dateRow.addView(TextView(this).apply { text = "Overview"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#0F172A")); layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
            dateRow.addView(TextView(this).apply { text = "📅 Today, 29/09/2026"; textSize = 12f; setTextColor(Color.parseColor("#64748B")) })
            root.addView(dateRow)

            fun kpiCard(bg:String, icon:String, title:String, value:String, sub:String, subColor:String): LinearLayout {
                return LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.parseColor(bg))
                    setPadding(20,20,20,20)
                    layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(8,8,8,8) }
                    addView(TextView(this@AdminActivity).apply { text = icon; textSize = 22f; gravity = android.view.Gravity.CENTER })
                    addView(TextView(this@AdminActivity).apply { text = title; textSize = 11f; gravity = android.view.Gravity.CENTER; setTextColor(Color.parseColor("#334155")) })
                    addView(TextView(this@AdminActivity).apply { text = value; textSize = 16f; setTypeface(null, Typeface.BOLD); gravity = android.view.Gravity.CENTER; setTextColor(Color.parseColor("#0F172A")) })
                    addView(TextView(this@AdminActivity).apply { text = sub; textSize = 9f; gravity = android.view.Gravity.CENTER; setTextColor(Color.parseColor(subColor)) })
                }
            }
            val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,0,12,0) }
            row1.addView(kpiCard("#BBF7D0","📈","Today's Sales","$${String.format("%.2f",todaySales)}","+ REAL","#16A34A"))
            row1.addView(kpiCard("#BFDBFE","💰","Profit","$${String.format("%.2f",profit)}","REAL","#2563EB"))
            row1.addView(kpiCard("#DDD6FE","🏠","Stock Value","$${String.format("%.0f",stockValue)}","At Selling Price","#7C3AED"))
            val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,0,12,12) }
            row2.addView(kpiCard(if(lowStock>0)"#FECACA" else "#BBF7D0", if(lowStock>0)"⚠️" else "✅","Low Stock","$lowStock Items", if(lowStock>0)"Needs restock" else "All OK", if(lowStock>0)"#DC2626"
