package com.smartpos
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class AuditLogActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#FDF6FF"))
        
        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setBackgroundColor(Color.parseColor("#E8D5F2"))
        header.setPadding(30,25,30,25)
        val title = TextView(this)
        title.text = "📜 AUDIT LOG - PASTEL"
        title.setTextColor(Color.parseColor("#4A148C"))
        title.textSize = 18f
        title.setTypeface(null, Typeface.BOLD)
        header.addView(title)
        
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val cashier = prefs.getString("cashier_name", "System")
        val tvHead = TextView(this)
        tvHead.text = "Account: $cashier | Date: ${sdf.format(Date())}"
        tvHead.setTextColor(Color.parseColor("#7B1FA2"))
        header.addView(tvHead)
        
        val colHeader = TextView(this)
        colHeader.text = "ACCOUNT | DATE & TIME | ACTION | DETAILS"
        colHeader.setBackgroundColor(Color.parseColor("#CE93D8"))
        colHeader.setTextColor(Color.WHITE)
        colHeader.setPadding(15,15,15,15)
        colHeader.setTypeface(null, Typeface.BOLD)
        
        val scroll = ScrollView(this)
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(10,10,10,10)
        scroll.addView(container)
        
        root.addView(header)
        root.addView(colHeader)
        root.addView(scroll)
        setContentView(root)
        
        // Load logs
        val allLogs = mutableListOf<String>()
        val sales = getSharedPreferences("sales_history", MODE_PRIVATE)
        for ((k, v) in sales.all) {
            allLogs.add("👤 $cashier | ${sdf.format(Date())} | SALE | Sold $k - $v")
        }
        val cust = getSharedPreferences("customers_data", MODE_PRIVATE)
        for ((k, v) in cust.all) {
            allLogs.add("👤 $cashier | ${sdf.format(Date())} | CUSTOMER | Added $k")
        }
        val price = getSharedPreferences("price_history", MODE_PRIVATE)
        for ((k, v) in price.all) {
            allLogs.add("👤 $cashier | ${sdf.format(Date())} | PRICE | $v")
        }
        val goods = getSharedPreferences("goods_received", MODE_PRIVATE)
        for ((k, v) in goods.all) {
            allLogs.add("👤 $cashier | ${sdf.format(Date())} | STOCK | $k - $v")
        }
        
        if (allLogs.isEmpty()) {
            allLogs.add("👤 $cashier | ${sdf.format(Date())} | LOGIN | User logged in - ${android.os.Build.MODEL}")
            allLogs.add("👤 $cashier | ${sdf.format(Date())} | SYSTEM | Audit Log Pastel Nice")
        }
        
        for (log in allLogs) {
            val card = TextView(this)
            card.text = log
            card.setBackgroundColor(Color.WHITE)
            card.setPadding(15,15,15,15)
            card.textSize = 12f
            val lp = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            // pastel border effect
            if (log.contains("SALE")) card.setBackgroundColor(Color.parseColor("#E8F5E9"))
            if (log.contains("CUSTOMER")) card.setBackgroundColor(Color.parseColor("#E3F2FD"))
            if (log.contains("PRICE")) card.setBackgroundColor(Color.parseColor("#FFF9C4"))
            if (log.contains("STOCK")) card.setBackgroundColor(Color.parseColor("#FFF3E0"))
            val wrap = LinearLayout(this)
            wrap.orientation = LinearLayout.VERTICAL
            val lpWrap = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lpWrap.setMargins(0,0,0,10)
            wrap.layoutParams = lpWrap
            wrap.addView(card)
            container.addView(wrap)
        }
    }
}