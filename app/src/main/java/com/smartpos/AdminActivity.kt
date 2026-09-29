package com.smartpos

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : android.app.Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8FAFC"))
            setPadding(10,10,10,10)
        }

        // TOP BAR
        val top = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20,20,20,20)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(this).apply {
            text = "ADMIN DASHBOARD"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        })
        top.addView(Button(this).apply {
            text = "⚙ SETTINGS"
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
            setOnClickListener { openDetail("System Settings") }
        })
        root.addView(top)

        // SHOP + DATE
        val shopRow = LinearLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            setPadding(16,12,16,12)
            orientation = LinearLayout.HORIZONTAL
        }
        shopRow.addView(TextView(this).apply {
            text = "Shop: Main Shop"
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        })
        shopRow.addView(TextView(this).apply { text = "29/09/2026" })
        root.addView(shopRow)

        // STATS
        fun stat(t: String, v: String): LinearLayout {
            return LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                setPadding(16,16,16,16)
                addView(TextView(this@AdminActivity).apply { text = t; textSize = 11f; setTextColor(Color.parseColor("#64748B")); setTypeface(null, Typeface.BOLD) })
                addView(TextView(this@AdminActivity).apply { text = v; textSize = 15f; setTypeface(null, Typeface.BOLD) })
                layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(4,4,4,4) }
            }
        }
        val r1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(6,6,6,3) }
        r1.addView(stat("TODAY'S SALES", "$2,485.60"))
        r1.addView(stat("STOCK VALUE", "$18,750.00"))
        r1.addView(stat("PRODUCTS", "1,248"))
        root.addView(r1)
        val r2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(6,3,6,6) }
        r2.addView(stat("TODAY'S PROFIT", "$624.30"))
        r2.addView(stat("LOW STOCK", "37 Products"))
        r2.addView(stat("CUSTOMERS", "856"))
        root.addView(r2)

        // QUICK ACTIONS
        root.addView(TextView(this).apply { text = "QUICK ACTIONS"; setTypeface(null, Typeface.BOLD); setPadding(16,16,16,6) })
        fun quickRow(b1: String, c1: String, b2: String, c2: String, b3: String, c3: String, act1: () -> Unit, act2: () -> Unit, act3: () -> Unit) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(Button(this).apply { text = b1; setBackgroundColor(Color.parseColor(c1)); setTextColor(Color.WHITE); textSize = 11f; layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(3,3,3,3) }; setOnClickListener { act1() } })
            row.addView(Button(this).apply { text = b2; setBackgroundColor(Color.parseColor(c2)); setTextColor(Color.WHITE); textSize = 11f; layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(3,3,3,3) }; setOnClickListener { act2() } })
            row.addView(Button(this).apply { text = b3; setBackgroundColor(Color.parseColor(c3)); setTextColor(Color.WHITE); textSize = 11f; layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(3,3,3,3) }; setOnClickListener { act3() } })
            root.addView(row)
        }
        quickRow("+ RECEIVE STOCK", "#2563EB", "+ ADD PRODUCT", "#16A34A", "+ NEW SALE", "#7C3AED",
            { safeOpenReceiving() }, { openDetail("Add Product") }, { finish() })
        quickRow("STOCK TRANSFER", "#0F766E", "ADD SUPPLIER", "#EA580C", "ADD USER", "#475569",
            { openDetail("Stock Transfers") }, { openDetail("Suppliers") }, { openDetail("Add User") })

        // ADMIN MENU
        root.addView(TextView(this).apply { text = "ADMIN MENU"; setTypeface(null, Typeface.BOLD); setPadding(16,16,16,6) })
        fun menuRow(a: String, b: String) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row.addView(Button(this).apply { text = a; setBackgroundColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(3,3,3,3) }; setOnClickListener { if(a=="PURCHASING") safeOpenReceiving() else openDetail(a) } })
            row.addView(Button(this).apply { text = b; setBackgroundColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(3,3,3,3) }; setOnClickListener { if(b=="PURCHASING") safeOpenReceiving() else openDetail(b) } })
            root.addView(row)
        }
        menuRow("PRODUCTS & STOCK", "SUPPLIERS")
        menuRow("USERS & PERMISSIONS", "CUSTOMERS")
        menuRow("SALES MANAGEMENT", "PURCHASING")
        menuRow("CASH MANAGEMENT", "BRANCHES / SHOPS")
        menuRow("REPORTS", "PRICE MANAGEMENT")
        menuRow("SYSTEM SETTINGS", "AUDIT LOG")

        // RECENT ACTIVITY
        root.addView(TextView(this).apply { text = "RECENT ACTIVITY"; setTypeface(null, Typeface.BOLD); setPadding(16,16,16,6) })
        root.addView(TextView(this).apply {
            text = "10:25 Stock received - Coca-Cola 500ml +24\n10:18 Sale completed - Receipt #004582 $45.60\n09:55 Price changed - Mazoe Orange $3.50\n09:40 New product - Bread 700g Added"
            setBackgroundColor(Color.WHITE)
            setPadding(16,12,16,12)
        })

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun safeOpenReceiving() {
        try { startActivity(Intent(this, ReceiveStockActivity::class.java)) }
        catch (e: Exception) { Toast.makeText(this, "Opening Receiving...", Toast.LENGTH_SHORT).show(); openDetail("Receiving") }
    }

    private fun openDetail(t: String) {
        try { startActivity(Intent(this, AdminDetailActivity::class.java).putExtra("TITLE", t)) }
        catch (e: Exception) { Toast.makeText(this, t, Toast.LENGTH_SHORT).show() }
    }
}
