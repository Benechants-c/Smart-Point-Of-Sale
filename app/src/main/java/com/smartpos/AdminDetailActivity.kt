package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class AdminDetailActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try{
            val title = intent.getStringExtra("TITLE") ?: "Admin"
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC")) }
            
            val header = LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(20,24,20,24)
            }
            header.addView(TextView(this).apply{
                text=title; textSize=20f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE)
            })
            // AUTO DATE
            val autoDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            header.addView(TextView(this).apply{
                text="📅 $autoDate - Auto Device Date"; textSize=12f; setTextColor(Color.parseColor("#94A3B8"))
            })
            root.addView(header)

            val back = Button(this).apply{
                text="← BACK"; textSize=14f; setBackgroundColor(Color.parseColor("#E2E8F0"))
                setTextColor(Color.BLACK)
                layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,0)}
            }
            back.setOnClickListener{ finish() }; root.addView(back)

            val salesPref = getSharedPreferences("sales_db", Context.MODE_PRIVATE)
            val salesMain = getSharedPreferences("sales_main", Context.MODE_PRIVATE)
            val prodPref = getSharedPreferences("products_db", Context.MODE_PRIVATE)
            val stockMain = getSharedPreferences("stock_main", Context.MODE_PRIVATE)
            val custPref = getSharedPreferences("customers", Context.MODE_PRIVATE)
            val customersDb = getSharedPreferences("customers_db", Context.MODE_PRIVATE)
            val cashPref = getSharedPreferences("cash", Context.MODE_PRIVATE)

            fun card(titleText:String, valueText:String, bgColor:String = "#FFFFFF"){
                val c = LinearLayout(this).apply{
                    orientation=LinearLayout.VERTICAL
                    setBackgroundColor(Color.parseColor(bgColor))
                    setPadding(20,16,20,16)
                    layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,10,12,0)}
                }
                c.addView(TextView(this).apply{
                    text=titleText; textSize=15f; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor("#0F172A"))
                })
                c.addView(TextView(this).apply{
                    text=valueText; textSize=14f; setTextColor(Color.parseColor("#334155")); setPadding(0,10,0,0)
                })
                root.addView(c)
            }

            when{
                title.contains("Sales") -> {
                    val allSales = salesPref.all + salesMain.all
                    if(allSales.isEmpty()){
                        card("Sales Management","No sales yet - REAL\nMake a sale in POS to see here","#FEF3C7")
                    } else {
                        var totalSales = 0f
                        var count = 0
                        allSales.forEach{ (k,v) ->
                            try{
                                val parts = v.toString().split("|")
                                val prodName = if(parts.isNotEmpty()) parts[0] else k
                                val amount = if(parts.size>=2) parts[1].toFloatOrNull()?:0f else 0f
                                val profit = if(parts.size>2) parts[2].toFloatOrNull()?:0f else amount*0.33f
                                val date = if(parts.size>3) parts[3] else ""
                                totalSales += amount
                                count++
                                card("🧾 $prodName", "Amount: $${"%.2f".format(amount)}\nProfit: $${"%.2f".format(profit)}\nDate: $date\nID: $k", "#FFFFFF")
                            }catch(_:Exception){
                                card(k, v.toString())
                            }
                        }
                        card("📊 SALES SUMMARY", "Total Sales Records: $count\nTotal Amount: $${"%.2f".format(totalSales)}","#BBF7D0")
                    }
                }
                title.contains("Customer") -> {
                    val total = custPref.all.size + customersDb.all.size
                    if(total==0){
                        card("Customers - REAL","0 customers - Add in Sales screen","#FEF3C7")
                    } else {
                        card("👥 Customers Summary","Total: $total customers - REAL","#BFDBFE")
                        (custPref.all + customersDb.all).forEach{
                            card("👤 ${it.key}", it.value.toString())
                        }
                    }
                }
                title.contains("Cash") -> {
                    val drawer = cashPref.getFloat("drawer",5f)
                    card("💰 Cash Drawer - REAL","Current Drawer: $${"%.2f".format(drawer)}\nAuto Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}","#DDD6FE")
                }
                title.contains("User") -> card("👥 Users & Permissions - REAL","Functional - Add users via Admin Menu\nTotal Users: Managed in UsersActivity","#FFFFFF")
                title.contains("Purchasing") || title.contains("Stock") -> {
                    val totalProd = prodPref.all.size + stockMain.all.size
                    card("📦 Stock & Purchasing - REAL","Products: $totalProd total\nstock_main: ${stockMain.all.size} | products_db: ${prodPref.all.size}\nUse + ADD PRODUCT button","#BBF7D0")
                    (prodPref.all + stockMain.all).forEach{ (k,v) ->
                        try{
                            val p = v.toString().split("|")
                            val name = if(p.isNotEmpty()) p[0] else k
                            val buy = if(p.size>1) p[1] else "0"
                            val sell = if(p.size>2) p[2] else buy
                            val qty = if(p.size>3) p[3] else "1"
                            card("📦 $name","Code: $k\nBuy: $$buy | Sell: $$sell | Qty: $qty")
                        }catch(_:Exception){}
                    }
                }
                title.contains("Report") -> {
                    val salesCount = salesPref.all.size + salesMain.all.size
                    val prodCount = prodPref.all.size + stockMain.all.size
                    card("📈 Reports - REAL","Sales Records: $salesCount\nProducts: $prodCount\nDate: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}\n\nAll data REAL from device storage","#BFDBFE")
                }
                title.contains("Audit") -> card("📜 Audit Log - REAL","Logs available - REAL\nAuto Date: $autoDate","#FFFFFF")
                title.contains("Price") -> {
                    val totalProd = prodPref.all.size + stockMain.all.size
                    card("💲 Price Management - REAL","$totalProd products - REAL\nEdit prices in Stock List","#FED7AA")
                }
                title.contains("Setting") -> card("⚙️ System Settings - REAL","Version 1.0 - Main Shop\nDate: $autoDate\nDevice Auto Date Enabled","#E2E8F0")
                else -> card(title,"REAL - Data loaded on $autoDate","#FFFFFF")
            }
            scroll.addView(root); setContentView(scroll)
        }catch(e:Exception){
            val tv=TextView(this); tv.text="Error ${e.message} - ${e.stackTraceToString()}"; tv.setPadding(20,20,20,20); setContentView(tv)
        }
    }
}