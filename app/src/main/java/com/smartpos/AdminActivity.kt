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

        // TOP BAR - ADMIN DASHBOARD + SETTINGS
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

        // SHOP + DATE ROW
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
        shopRow.addView(TextView(this).apply {
            text = "Date: [29/09/2026]"
        })
        root.addView(shopRow)

        // STATS GRID
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
                val p = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(4,4,4,4) }
                layoutParams = p
                elevation = 2f
            }
        }

        val statsRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,8,8,4) }
        statsRow1.addView(statCard("TODAY'S SALES", "$2,485.60"))
        statsRow1.addView(statCard("STOCK VALUE", "$18,750.00"))
        statsRow1.addView(statCard("PRODUCTS", "1,248"))
        root.addView(statsRow1)

        val statsRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,4,8,8) }
        statsRow2.addView(statCard("TODAY'S PROFIT", "$624.30"))
        statsRow2.addView(statCard("LOW STOCK", "37 Products"))
        statsRow2.addView(statCard("CUSTOMERS", "856"))
        root.addView(statsRow2)

        // QUICK ACTIONS
        root.addView(TextView(this).apply {
            text = "QUICK ACTIONS"
            setTypeface(null, Typeface.BOLD)
            setPadding(20,20,20,10)
        })

        fun quickBtn(text: String, color: String, action: () -> Unit): Button {
            return Button(this).apply {
                this.text = text
                setBackgroundColor(Color.parseColor(color))
                setTextColor(Color.WHITE)
                textSize = 12f
                setPadding(0,20,0,20)
                setOnClickListener { action() }
                layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(4,4,4,4) }
            }
        }

        val quickRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,0,8,0) }
        quickRow1.addView(quickBtn("+ RECEIVE STOCK", "#2563EB") { startActivity(Intent(this, ReceiveStockActivity::class.java)) })
        quickRow1.addView(quickBtn("+ ADD PRODUCT", "#16A34A") { openDetail("Add Product") })
        quickRow1.addView(quickBtn("+ NEW SALE", "#7C3AED") { finish() })
        root.addView(quickRow1)

        val quickRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,0,8,8) }
        quickRow2.addView(quickBtn("STOCK TRANSFER", "#0F766E") { openDetail("Stock Transfers") })
        quickRow2.addView(quickBtn("ADD SUPPLIER", "#EA580C") { openDetail("Suppliers") })
        quickRow2.addView(quickBtn("ADD USER", "#475569") { openDetail("Add User") })
        root.addView(quickRow2)

        // ADMIN MENU
        root.addView(TextView(this).apply {
            text = "ADMIN MENU"
            setTypeface(null, Typeface.BOLD)
            setPadding(20,20,20,10)
        })

        fun menuBtn(text: String, action: () -> Unit): Button {
            return Button(this).apply {
                this.text = text
                setBackgroundColor(Color.WHITE)
                setTextColor(Color.parseColor("#1E293B"))
                gravity = Gravity.LEFT or Gravity.CENTER_VERTICAL
                setPadding(20,24,20,24)
                setOnClickListener { action() }
                layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(4,4,4,4) }
            }
        }

        val menuRow1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,0,8,0) }
        menuRow1.addView(menuBtn("PRODUCTS & STOCK") { openDetail("Products / Inventory") })
        menuRow1.addView(menuBtn("SUPPLIERS") { openDetail("Suppliers") })
        root.addView(menuRow1)

        val menuRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,0,8,0) }
        menuRow2.addView(menuBtn("USERS & PERMISSIONS") { openDetail("User Management") })
        menuRow2.addView(menuBtn("CUSTOMERS") { openDetail("Customers") })
        root.addView(menuRow2)

        val menuRow3 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,0,8,0) }
        menuRow3.addView(menuBtn("SALES MANAGEMENT") { openDetail("Sales Management") })
        menuRow3.addView(menuBtn("PURCHASING") { 
            startActivity(Intent(this@AdminActivity, ReceiveStockActivity::class.java))
        })
        root.addView(menuRow3)

        val menuRow4 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,0,8,0) }
        menuRow4.addView(menuBtn("CASH MANAGEMENT") { openDetail("Cash Management") })
        menuRow4.addView(menuBtn("BRANCHES / SHOPS") { openDetail("Branches / Shops") })
        root.addView(menuRow4)

        val menuRow5 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,0,8,0) }
        menuRow5.addView(menuBtn("REPORTS") { openDetail("Reports") })
        menuRow5.addView(menuBtn("PRICE MANAGEMENT") { openDetail("Pricing") })
        root.addView(menuRow5)

        val menuRow6 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(8,0,8,4) }
        menuRow6.addView(menuBtn("SYSTEM SETTINGS") { openDetail("System Settings") })
        menuRow6.addView(menuBtn("AUDIT LOG") { openDetail("Audit Log") })
        root.addView(menuRow6)

        // RECENT ACTIVITY
        root.addView(TextView(this).apply {
            text = "RECENT ACTIVITY"
            setTypeface(null, Typeface.BOLD)
            setPadding(20,20,20,10)
        })

        val activityBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(16,12,16,12)
        }
        fun activityLine(time: String, action: String, detail: String, extra: String): TextView {
            return TextView(this).apply {
                text = "$time  $action      $detail       $extra"
                textSize = 12f
                setPadding(0,8,0,8)
                setTextColor(Color.parseColor("#334155"))
            }
        }
        activityBox.addView(activityLine("10:25", "Stock received", "Coca-Cola 500ml", "+24"))
        activityBox.addView(activityLine("10:18", "Sale completed", "Receipt #004582", "$45.60"))
        activityBox.addView(activityLine("09:55", "Price changed", "Mazoe Orange", "$3.50"))
        activityBox.addView(activityLine("09:40", "New product", "Bread 700g", "Added"))
        root.addView(activityBox, LinearLayout.LayoutParams(-1,-2).apply { setMargins(12,0,12,20) })

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun openDetail(title: String) {
        startActivity(Intent(this, AdminDetailActivity::class.java).putExtra("TITLE", title))
    }
}
