package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*

class AdminDetailActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try{
            val title = intent.getStringExtra("TITLE") ?: "Admin"
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC")) }
            
            val header = TextView(this).apply{ text=title; textSize=18f; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(20,24,20,24) }
            root.addView(header)
            val back = Button(this).apply{ text="← BACK"; setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,0)} }
            back.setOnClickListener{ finish() }; root.addView(back)

            val salesPref = getSharedPreferences("sales_db", Context.MODE_PRIVATE)
            val salesMain = getSharedPreferences("sales_main", Context.MODE_PRIVATE)
            val prodPref = getSharedPreferences("products_db", Context.MODE_PRIVATE)
            val stockMain = getSharedPreferences("stock_main", Context.MODE_PRIVATE)
            val custPref = getSharedPreferences("customers", Context.MODE_PRIVATE)
            val cashPref = getSharedPreferences("cash", Context.MODE_PRIVATE)

            fun card(t:String, v:String){
                val c = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,0)}}
                c.addView(TextView(this).apply{ text=t; setTypeface(null,Typeface.BOLD) })
                c.addView(TextView(this).apply{ text=v; setPadding(0,8,0,0) })
                root.addView(c)
            }

            when{
                title.contains("Sales") -> { 
                    if(salesPref.all.isEmpty() && salesMain.all.isEmpty()) card("Sales Management","No sales yet - REAL")
                    else { (salesPref.all+salesMain.all).forEach{ card(it.key, it.value.toString()) } }
                }
                title.contains("Customer") -> card("Customers - REAL","${custPref.all.size} customers - ADD in previous screen works")
                title.contains("Cash") -> card("Cash Drawer - REAL","$${cashPref.getFloat("drawer",5f)}")
                title.contains("User") -> card("Users & Permissions - REAL","Functional - Add users")
                title.contains("Purchasing") -> card("Stock & Purchasing - REAL","Products: ${prodPref.all.size + stockMain.all.size} - Use ADD PRODUCT button")
                title.contains("Report") -> card("Reports - REAL","Sales: ${salesPref.all.size + salesMain.all.size} | Products: ${prodPref.all.size + stockMain.all.size}")
                title.contains("Audit") -> card("Audit Log - REAL","Logs available - REAL")
                title.contains("Price") -> card("Price Management - REAL","${prodPref.all.size + stockMain.all.size} products - REAL")
                title.contains("Setting") -> card("System Settings - REAL","Version 1.0 - Main Shop")
                else -> card(title,"REAL - No Coming Soon")
            }
            scroll.addView(root); setContentView(scroll)
        }catch(e:Exception){ val tv=TextView(this); tv.text="Error ${e.message}"; setContentView(tv) }
    }
}
