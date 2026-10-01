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

            try{
                val allSales = listOf("sales_db", "sales_main")
                for(name in allSales){
                    val pref = getSharedPreferences(name, Context.MODE_PRIVATE)
                    for((_,v) in pref.all){
                        try{
                            val parts = v.toString().split("|")
                            if(parts.size>=2){
                                val amount = parts[1].toFloatOrNull()?:0f
                                val cost = if(parts.size>2) parts[2].toFloatOrNull()?:0f else 0f
                                todaySales += amount
                                profit += if(cost>0) amount-cost else amount*0.2f
                            }
                        }catch(_:Exception){}
                    }
                }
            }catch(_:Exception){}

            try{
                val allProd = listOf("products_db", "stock_main", "products")
                val seen = HashSet<String>()
                for(name in allProd){
                    val pref = getSharedPreferences(name, Context.MODE_PRIVATE)
                    for((k,v) in pref.all){
                        try{
                            if(seen.contains(k)) continue
                            seen.add(k)
                            val p = v.toString().split("|")
                            val buy = if(p.size>1) p[1].toFloatOrNull()?:0f else 0f
                            var sell = if(p.size>2) p[2].toFloatOrNull()?:0f else 0f
                            if(sell==0f) sell = buy
                            var qty = 1f
                            if(p.size>3) qty = p[3].toFloatOrNull()?:1f
                            stockValue += sell * qty
                            if(qty < 5) lowStock++
                            prodCount++
                        }catch(_:Exception){}
                    }
                }
            }catch(_:Exception){}

            try{
                val c1 = getSharedPreferences("customers", Context.MODE_PRIVATE).all.size
                val c2 = getSharedPreferences("customers_db", Context.MODE_PRIVATE).all.size
                custCount = c1 + c2
            }catch(_:Exception){}

            val prefsCash = getSharedPreferences("cash", Context.MODE_PRIVATE)
            val drawer = prefsCash.getFloat("drawer", 5.0f)

            val dateRow = LinearLayout(this).apply { setPadding(24,16,24,8); orientation = LinearLayout.HORIZONTAL }
            dateRow.addView(TextView(this).apply { text = "Overview"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#0F172A")); layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
            dateRow.addView(TextView(this).apply { text = "📅 Today, 29/09/2026"; textSize = 12f; setTextColor(Color.parse
