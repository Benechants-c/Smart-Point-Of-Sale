package com.smartpos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*

class AdminActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F1F5F9"))
            setPadding(20, 30, 20, 20)
        }
        val title = TextView(this).apply {
            text = "POS\nMain Menu"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 0, 0, 20)
        }
        root.addView(title)

        fun addBtn(text: String, color: String, className: String) {
            val b = Button(this).apply {
                this.text = text
                textSize = 16f
                setBackgroundColor(Color.parseColor(color))
                setTextColor(Color.WHITE)
                setPadding(0, 28, 0, 28)
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 16) }
                setOnClickListener {
                    try {
                        val cls = Class.forName("com.smartpos.$className")
                        startActivity(Intent(this@AdminActivity, cls))
                    } catch (e: Exception) {
                        Toast.makeText(this@AdminActivity, "$text - coming soon", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            root.addView(b)
        }

        // ONLY 7 - using names, not direct classes, so it WILL build
        addBtn("🛒 1. New Sale", "#2563EB", "SalesActivity")
        addBtn("📦 2. Products", "#0D9488", "ProductsActivity")
        addBtn("📊 3. Stock & Purchasing", "#7C3AED", "StockPurchasingActivity")
        addBtn("🧾 4. Manage Sales", "#EA580C", "ManageSalesActivity")
        addBtn("📈 5. Reports", "#16A34A", "ReportsActivity")
        addBtn("👥 6. Customers", "#DB2777", "CustomersActivity")
        addBtn("⚙️ 7. Settings", "#1E293B", "SettingsActivity")

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }
}