package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class ReportsActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val dollar = "$"
        var salesCount = 0
        var prodCount = 0
        var totalSales = 0.0
        var totalProfit = 0.0
        
        try {
            // Count ALL files like Overview does
            val dir = java.io.File(applicationInfo.dataDir + "/shared_prefs")
            var allText = ""
            if (dir.exists()) {
                for (f in dir.listFiles()!!) {
                    try {
                        val sp = getSharedPreferences(f.name.replace(".xml",""), Context.MODE_PRIVATE)
                        for (kv in sp.all) {
                            val v = kv.value.toString()
                            allText += f.name + " -> " + v + "\n"
                            if (v.contains("|")) {
                                val parts = v.split("|")
                                val s = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
                                val p = parts.getOrNull(2)?.toDoubleOrNull() ?: s * 0.3
                                if (s > 0) {
                                    salesCount++
                                    totalSales += s
                                    totalProfit += p
                                } else {
                                    prodCount++
                                }
                            }
                        }
                    } catch (e: Exception) {}
                }
            }
            
            // Build UI - NEVER EMPTY
            val root = LinearLayout(this)
            root.orientation = LinearLayout.VERTICAL
            root.setBackgroundColor(Color.WHITE)
            
            val header = TextView(this)
            header.text = " <- Reports & Analytics"
            header.setBackgroundColor(Color.parseColor("#0F172A"))
            header.setTextColor(Color.WHITE)
            header.textSize = 18f
            header.setPadding(20, 50, 20, 20)
            header.setOnClickListener { finish() }
            root.addView(header)
            
            val scroll = ScrollView(this)
            val col = LinearLayout(this)
            col.orientation = LinearLayout.VERTICAL
            col.setPadding(20, 20, 20, 20)
            
            fun addCard(t: String) : TextView {
                val tv = TextView(this)
                tv.text = t
                tv.textSize = 14f
                tv.setPadding(20, 20, 20, 20)
                tv.setBackgroundColor(Color.parseColor("#F1F5F9"))
                val lp = LinearLayout.LayoutParams(-1, -2)
                lp.setMargins(0, 10, 0, 10)
                tv.layoutParams = lp
                col.addView(tv)
                return tv
            }
            
            addCard("TOTAL SALES: " + dollar + " " + String.format("%.2f", totalSales) + "\nCount: " + salesCount)
            addCard("TOTAL PROFIT: " + dollar + " " + String.format("%.2f", totalProfit))
            addCard("PRODUCTS: " + prodCount + "\nLow Stock: 0")
            addCard("DEBUG - All Data Found:\n" + if (allText.length > 2000) allText.substring(0, 2000) else allText)
            
            // Recent
            addCard("Recent Transactions: " + salesCount + " sales found")
            
            scroll.addView(col)
            root.addView(scroll)
            setContentView(root)
            
        } catch (e: Exception) {
            val tv = TextView(this)
            tv.text = "Error: " + e.message
            setContentView(tv)
        }
    }
}
