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

        // ONLY 7 - NO DUPLICATES, NO SUB-ITEMS
        fun addBtn(text: String, color: String, cls: Class<*>?) {
            val b = Button(this).apply {
                this.text = text
                textSize = 16f
                setBackgroundColor(Color.parseColor(color))
                setTextColor(Color.WHITE)
                setPadding(0, 28, 0, 28)
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 16) }
                setOnClickListener {
                    if (cls != null) {
                        try { startActivity(Intent(this@AdminActivity, cls)) }
                        catch (e: Exception) { Toast.makeText(this@AdminActivity, "$text - coming soon", Toast.LENGTH_SHORT).show() }
                    }
                }
            }
            root.addView(b)
        }

        addBtn("🛒 1. New Sale", "#2563EB", SalesActivity::class.java)
        addBtn("📦 2. Products", "#0D9488", ProductsActivity::class.java)
        addBtn("📊 3. Stock & Purchasing", "#7C3AED", StockPurchasingActivity::class.java)
        addBtn("🧾 4. Manage Sales", "#EA580C", ManageSalesActivity::class.java)
        addBtn("📈 5. Reports", "#16A34A", ReportsActivity::class.java)
        addBtn("👥 6. Customers", "#DB2777", CustomersActivity::class.java)
        addBtn("⚙️ 7. Settings", "#1E293B", SettingsActivity::class.java)

        val scroll = ScrollView(this)
        scroll.addView(root)
        setContentView(scroll)
    }
}