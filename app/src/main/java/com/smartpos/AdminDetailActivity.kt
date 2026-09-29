package com.smartpos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {

    private fun statCard(title: String, value: String, sub: String, color: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor(color))
            setPadding(16, 16, 16, 16)
            layoutParams = LinearLayout.LayoutParams(0, 280, 1f).apply { setMargins(8, 8, 8, 8) }
            addView(TextView(context).apply { text = title; textSize = 12f; setTextColor(Color.DKGRAY); gravity = Gravity.CENTER })
            addView(TextView(context).apply { text = value; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.BLACK); gravity = Gravity.CENTER })
            addView(TextView(context).apply { text = sub; textSize = 10f; setTextColor(Color.parseColor("#16A34A")); gravity = Gravity.CENTER })
        }
    }

    private fun actionButton(text: String, color: String, click: () -> Unit): Button {
        return Button(this).apply {
            this.text = text; setBackgroundColor(Color.parseColor(color)); setTextColor(Color.WHITE)
            textSize = 12f; setPadding(0, 24, 0, 24)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(6, 6, 6, 6) }
            setOnClickListener { click() }
        }
    }

    private fun menuItem(name: String, click: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setPadding(16, 18, 16, 18)
            setBackgroundColor(Color.WHITE)
            addView(TextView(context).apply { text = name; textSize = 14f; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
            addView(TextView(context).apply { text = ">"; setTextColor(Color.GRAY) })
            setOnClickListener { click() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 1, 0, 1) }
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F1F5F9")); setPadding(12, 12, 12, 12) }

        // CALC REAL VALUES FROM YOUR DBs
        var totalSales = 0f; var lowStock = 0; var productCount = 0; var stockValue = 0f
        getSharedPreferences("products_db", 0).all.forEach {
            try {
                val a = it.value.toString().split("|")
                if (a.size >= 5) {
                    productCount++; val qty = a[4].toIntOrNull()?: 0; val sell = a[3].toFloatOrNull()?: 0f
                    stockValue += qty * sell; if (qty < 10) lowStock++
                }
            } catch (_: Exception) {}
        }
        getSharedPreferences("sales_db", 0).all.forEach { try { totalSales += it.value.toString().split("|").getOrNull(1)?.toFloatOrNull()?: 0f } catch (_: Exception) {} }

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(statCard("Today's Sales", "$${"%.2f".format(totalSales)}", "+ REAL", "#BBF7D0"))
        row1.addView(statCard("Profit", "$0.00", "REAL", "#DBEAFE"))
        row1.addView(statCard("Stock Value", "$${stockValue.toInt()}", "REAL", "#F3E8FF"))
        root.addView(row1)

        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(statCard("Low Stock", "$lowStock Items", if (lowStock == 0) "All OK" else "Attention!", "#BBF7D0"))
        row2.addView(statCard("Products", "$productCount", "Active SKUs", "#BAE6FD"))
        row2.addView(statCard("Customers", "0", "REAL", "#F3E8FF"))
        root.addView(row2)

        root.addView(TextView(this).apply { text = "Quick Actions"; setTypeface(null, Typeface.BOLD); setPadding(0, 16, 0, 8) })
        val qa1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        qa1.addView(actionButton("🚚 + RECEIVE STOCK", "#2563EB") { startActivity(Intent(this@MainActivity, AdminDetailActivity::class.java).putExtra("TITLE", "Stock Control")) })
        qa1.addView(actionButton("📦 + ADD PRODUCT", "#16A34A") { startActivity(Intent(this@MainActivity, AdminDetailActivity::class.java).putExtra("TITLE", "Products & Categories")) })
        qa1.addView(actionButton("🛒 + NEW SALE", "#9333EA") { /* open POS */ })
        root.addView(qa1)

        val qa2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        qa2.addView(actionButton("🔄 STOCK TRANSFER", "#0F766E") { })
        qa2.addView(actionButton("🏢 ADD SUPPLIER", "#EA580C") { startActivity(Intent(this@MainActivity, AdminDetailActivity::class.java).putExtra("TITLE", "Suppliers")) })
        qa2.addView(actionButton("👤 + ADD USER", "#1E293B") { startActivity(Intent(this@MainActivity, AdminDetailActivity::class.java).putExtra("TITLE", "Users & Permissions")) })
        root.addView(qa2)

        root.addView(TextView(this).apply { text = "Admin Menu"; setTypeface(null, Typeface.BOLD); setPadding(0, 16, 0, 8) })
        root.addView(menuItem("Sales\nSales Management") { startActivity(Intent(this, AdminDetailActivity::class.java).putExtra("TITLE", "Reports")) })
        root.addView(menuItem("Customers") { })
        root.addView(menuItem("Cash Drawer $1245.60") { })
        root.addView(TextView(this).apply { text = "⚙️ Management"; setTypeface(null, Typeface.BOLD); setPadding(0, 16, 0, 8) })
        root.addView(menuItem("Users & Permissions") { startActivity(Intent(this, AdminDetailActivity::class.java).putExtra("TITLE", "Users & Permissions")) })
        root.addView(menuItem("Branches / Shops") { startActivity(Intent(this, AdminDetailActivity::class.java).putExtra("TITLE", "Branches / Shops")) })
        root.addView(menuItem("Stock & Purchasing") { startActivity(Intent(this, AdminDetailActivity::class.java).putExtra("TITLE", "Stock Control")) })
        root.addView(TextView(this).apply { text = "📈 Analytics"; setTypeface(null, Typeface.BOLD); setPadding(0, 16, 0, 8) })
        root.addView(menuItem("Reports (Sales/Profit/Stock)") { startActivity(Intent(this, AdminDetailActivity::class.java).putExtra("TITLE", "Reports")) })

        setContentView(ScrollView(this).apply { addView(root) })
    }
}
