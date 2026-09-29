package com.smartpos

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }

        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20,20,20,20)
            gravity = Gravity.CENTER_VERTICAL
        }
        topBar.addView(TextView(this).apply {
            text = "ADMIN DASHBOARD"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        })
        topBar.addView(Button(this).apply {
            text = "⚙ SETTINGS"
            textSize = 12f
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            setOnClickListener { openDetail("System Settings") }
        })
        root.addView(topBar)

        val shopRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(16,12,16,12)
        }
        shopRow.addView(TextView(this).apply {
            text = "Shop: [ Main Shop ▼ ]"
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        })
        shopRow.addView(TextView(this).apply { text = "Date: [29/09/2026]" })
        root.addView(shopRow)

        fun statCard(title: String, value: String): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                setPadding(16,16,16,16)
                addView(TextView(this@AdminActivity).apply {
                    text = title
                    textSize = 11f
                    setTextColor(Color.parseColor("#64748B"))
                    setTypeface(null, Typeface.BOLD)
                })
                addView(TextView(this@AdminActivity).apply {
                    text = value
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#0F172A"))
                    setPadding(0,6,0,0)
                })
                layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(4,4,4,4) }
            }
        }

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,8,8,4) }
        row1.addView(statCard("TODAY'S SALES", "$2,485.60"))
        row1.addView(statCard("STOCK VALUE", "$18,750.00"))
        row1.addView(statCard("PRODUCTS", "1,248"))
        root.addView(row1)

        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,4,8,8) }
        row2.addView(statCard("TODAY'S PROFIT", "$624.30"))
        row2.addView(statCard("LOW STOCK", "37 Products"))
        row2.addView(statCard("CUSTOMERS", "856"))
        root.addView(row2)

        root.addView(TextView(this).apply {
            text = "QUICK ACTIONS"
            setTypeface(null, Typeface.BOLD)
            setPadding(20,20,20,10)
        })

        fun quickBtn(t: String, c: String, a: () -> Unit): Button {
            return Button(this).apply {
                text = t
                setBackgroundColor(Color.parseColor(c))
                setTextColor(Color.WHITE)
                textSize = 12f
                setPadding(0,20,0,20)
                setOnClickListener { a() }
                layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(4,4,4,4) }
            }
        }

        val q1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,0,8,0) }
        q1.addView(quickBtn("+ RECEIVE STOCK", "#2563EB") { startActivity(Intent(this, ReceiveStockActivity::class.java)) })
        q1.addView(quickBtn("+ ADD PRODUCT", "#16A34A") { openDetail("Add Product") })
        q1.addView(quickBtn("+ NEW SALE", "#7C3AED") { finish() })
        root.addView(q1)

        val q2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,0,8,8) }
        q2.addView(quickBtn("STOCK TRANSFER", "#0F766E") { openDetail("Stock Transfers") })
        q2.addView(quickBtn("ADD SUPPLIER", "#EA580C") { openDetail("Suppliers") })
        q2.addView(quickBtn("ADD USER", "#475569") { openDetail("Add User") })
        root.addView(q2)

        root.addView(TextView(this).apply {
            text = "ADMIN MENU"
            setTypeface(null, Typeface.BOLD)
            setPadding(20,20,20,10)
        })

        fun menuBtn(t: String, act: () -> Unit): Button {
            return Button(this).apply {
                text = t
                setBackgroundColor(Color.WHITE)
                setTextColor(Color.parseColor("#1E293B"))
                gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                setPadding(20,24,20,24)
                setOnClickListener { act() }
                layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(4,4,4,4) }
            }
        }

        fun addMenuRow(b1: String, a1: () -> Unit, b2: String, a2: () -> Unit) {
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,0,8,0) }
            r.addView(menuBtn(b1, a1))
            r.addView(menuBtn(b2, a2))
            root.addView(r)
        }

        addMenuRow("PRODUCTS & STOCK", { openDetail("Products / Inventory") }, "SUPPLIERS", { openDetail("Suppliers") })
        addMenuRow("USERS & PERMISSIONS", { openDetail("User Management") }, "CUSTOMERS", { openDetail("Customers") })
        addMenuRow("SALES MANAGEMENT", { openDetail("Sales Management") }, "PURCHASING", { startActivity(Intent(this, ReceiveStockActivity::class.java)) })
        addMenuRow("CASH MANAGEMENT", { openDetail("Cash Management") }, "BRANCHES / SHOPS", { openDetail("Branches / Shops") })
        addMenuRow("REPORTS", { openDetail("Reports") }, "PRICE MANAGEMENT", { openDetail("Pricing") })
        addMenuRow("SYSTEM SETTINGS", { openDetail("System Settings") }, "AUDIT LOG", { openDetail("Audit Log") })

        root.addView(TextView(this).apply {
            text = "RECENT ACTIVITY"
            setTypeface(null, Typeface.BOLD)
            setPadding(20,20,20,10)
        })

        val actBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(16,12,16,12)
        }
        fun line(s: String): TextView = TextView(this).apply { text = s; textSize = 12f; setPadding(0,8,0,8) }
        actBox.addView(line("10:25  Stock received      Coca-Cola 500ml       +24"))
        actBox.addView(line("10:18  Sale completed      Receipt #004582       $45.60"))
        actBox.addView(line("09:55  Price changed       Mazoe Orange          $3.50"))
        actBox.addView(line("09:40  New product         Bread 700g             Added"))
        root.addView(actBox, LinearLayout.LayoutParams(-1,-2).apply { setMargins(12,0,12,20) })

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun openDetail(title: String) {
        startActivity(Intent(this, AdminDetailActivity::class.java).putExtra("TITLE", title))
    }
}
