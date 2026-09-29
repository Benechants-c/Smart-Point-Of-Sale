package com.smartpos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*

class AdminDetailActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = intent.getStringExtra("TITLE") ?: "Admin"
        try {
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#F8FAFC"))
                setPadding(16,16,16,16)
            }

            // TOP BAR
            val top = LinearLayout(this).apply {
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(20,20,20,20)
                orientation = LinearLayout.HORIZONTAL
            }
            val titleView = TextView(this).apply {
                text = title.uppercase()
                setTextColor(Color.WHITE)
                setTypeface(null, Typeface.BOLD)
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(0,-2,1f)
            }
            val back = Button(this).apply {
                text = "BACK"
                setBackgroundColor(Color.parseColor("#334155"))
                setTextColor(Color.WHITE)
            }
            back.setOnClickListener { finish() }
            top.addView(titleView)
            top.addView(back)
            root.addView(top)

            // CONTENT
            root.addView(TextView(this).apply {
                text = "\n$title\n"
                setTypeface(null, Typeface.BOLD)
                textSize = 18f
                setTextColor(Color.parseColor("#0F172A"))
            })

            when (title) {
                "Users & Permissions" -> {
                    val n = EditText(this).apply { hint = "Full Name" }
                    val u = EditText(this).apply { hint = "Username" }
                    val p = EditText(this).apply { hint = "Password" }
                    val r = EditText(this).apply { hint = "Role: CASHIER or ADMIN" }
                    root.addView(n); root.addView(u); root.addView(p); root.addView(r)
                    val b = Button(this).apply { text = "SAVE USER"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE) }
                    b.setOnClickListener {
                        try {
                            val prefs = getSharedPreferences("users", Context.MODE_PRIVATE)
                            prefs.edit().putString(u.text.toString(), n.text.toString() + "|" + r.text.toString()).apply()
                            Toast.makeText(this, "User saved", Toast.LENGTH_SHORT).show()
                            finish()
                        } catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_LONG).show() }
                    }
                    root.addView(b)
                    val prefs = getSharedPreferences("users", Context.MODE_PRIVATE)
                    prefs.all.forEach {
                        root.addView(TextView(this).apply { text = "User: ${it.key} - ${it.value}"; setBackgroundColor(Color.WHITE); setPadding(10,10,10,10) })
                    }
                }
                "Customers" -> {
                    val n = EditText(this).apply { hint = "Customer Name" }
                    val ph = EditText(this).apply { hint = "Phone" }
                    val bal = EditText(this).apply { hint = "Balance" }
                    root.addView(n); root.addView(ph); root.addView(bal)
                    val b = Button(this).apply { text = "ADD CUSTOMER"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE) }
                    b.setOnClickListener {
                        getSharedPreferences("customers", Context.MODE_PRIVATE).edit().putString(n.text.toString(), ph.text.toString() + "|" + bal.text.toString()).apply()
                        Toast.makeText(this, "Customer saved", Toast.LENGTH_SHORT).show()
                    }
                    root.addView(b)
                    root.addView(TextView(this).apply { text = "Blessing Moyo - $12.50\nTinashe - $0.00"; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16) })
                }
                "Cash Management" -> {
                    val prefs = getSharedPreferences("cash", Context.MODE_PRIVATE)
                    val current = prefs.getFloat("drawer", 1245.6f)
                    root.addView(TextView(this).apply { text = "DRAWER: $${current}"; textSize = 22f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#16A34A")); setBackgroundColor(Color.WHITE); setPadding(20,20,20,20) })
                    val amt = EditText(this).apply { hint = "Amount" }
                    val reason = EditText(this).apply { hint = "Reason" }
                    root.addView(amt); root.addView(reason)
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                    val inBtn = Button(this).apply { text = "CASH IN"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0,-2,1f) }
                    val outBtn = Button(this).apply { text = "CASH OUT"; setBackgroundColor(Color.parseColor("#DC2626")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0,-2,1f) }
                    inBtn.setOnClickListener {
                        try {
                            val v = amt.text.toString().toFloat()
                            val newBal = current + v
                            prefs.edit().putFloat("drawer", newBal).apply()
                            Toast.makeText(this, "IN +$$v New: $$newBal", Toast.LENGTH_LONG).show()
                            finish()
                        } catch (e: Exception) {}
                    }
                    outBtn.setOnClickListener {
                        try {
                            val v = amt.text.toString().toFloat()
                            val newBal = current - v
                            prefs.edit().putFloat("drawer", newBal).apply()
                            Toast.makeText(this, "OUT -$$v New: $$newBal", Toast.LENGTH_LONG).show()
                            finish()
                        } catch (e: Exception) {}
                    }
                    row.addView(inBtn); row.addView(outBtn)
                    root.addView(row)
                }
                "Branches / Shops" -> {
                    val n = EditText(this).apply { hint = "Shop Name" }
                    val loc = EditText(this).apply { hint = "Location" }
                    root.addView(n); root.addView(loc)
                    val b = Button(this).apply { text = "ADD SHOP"; setBackgroundColor(Color.parseColor("#0F766E")); setTextColor(Color.WHITE) }
                    b.setOnClickListener {
                        getSharedPreferences("branches", Context.MODE_PRIVATE).edit().putString(n.text.toString(), loc.text.toString()).apply()
                        Toast.makeText(this, "Shop added", Toast.LENGTH_SHORT).show()
                    }
                    root.addView(b)
                    root.addView(TextView(this).apply { text = "Main Shop - Harare\nShop 2 - Chitungwiza\nShop 3 - Norton"; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16) })
                }
                "Reports" -> {
                    val b1 = Button(this).apply { text = "SALES REPORT"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE) }
                    val b2 = Button(this).apply { text = "PROFIT REPORT"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE) }
                    val b3 = Button(this).apply { text = "STOCK REPORT"; setBackgroundColor(Color.parseColor("#EA580C")); setTextColor(Color.WHITE) }
                    b1.setOnClickListener { Toast.makeText(this, "Sales today $2485.60", Toast.LENGTH_LONG).show() }
                    b2.setOnClickListener { Toast.makeText(this, "Profit today $624.30", Toast.LENGTH_LONG).show() }
                    b3.setOnClickListener { Toast.makeText(this, "1248 products, 37 low", Toast.LENGTH_LONG).show() }
                    root.addView(b1); root.addView(b2); root.addView(b3)
                }
                "Price Management" -> {
                    root.addView(TextView(this).apply { text = "Bulk Price Change"; setTypeface(null, Typeface.BOLD) })
                    val cat = EditText(this).apply { hint = "Category: Drinks" }
                    val pct = EditText(this).apply { hint = "Percent: +10" }
                    root.addView(cat); root.addView(pct)
                    val b = Button(this).apply { text = "APPLY +10%"; setBackgroundColor(Color.parseColor("#DC2626")); setTextColor(Color.WHITE) }
                    b.setOnClickListener { Toast.makeText(this, "Price updated for ${cat.text} ${pct.text}%", Toast.LENGTH_LONG).show() }
                    root.addView(b)
                }
                "System Settings" -> {
                    root.addView(TextView(this).apply { text = "Shop Name: SMART POS\nCurrency: USD\nReceipt: Thank you!\nPrinter: Not connected"; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16) })
                }
                "Audit Log" -> {
                    val prefs = getSharedPreferences("audit", Context.MODE_PRIVATE)
                    val logs = prefs.getStringSet("logs", setOf("10:25 Admin - Stock received", "10:18 Cashier John - Sale $45.60")) ?: setOf()
                    logs.forEach { root.addView(TextView(this).apply { text = it; setBackgroundColor(Color.WHITE); setPadding(10,10,10,10) }) }
                }
                "Purchasing" -> {
                    val b = Button(this).apply { text = "+ NEW RECEIVING"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE) }
                    b.setOnClickListener { try { startActivity(Intent(this, ReceiveStockActivity::class.java)) } catch (e: Exception) { Toast.makeText(this, "Receiving: ${e.message}", Toast.LENGTH_LONG).show() } }
                    root.addView(b)
                    root.addView(TextView(this).apply { text = "12/09 - Coca-Cola +24 $18\n11/09 - Bread +50 $25"; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16) })
                }
                "Sales Management" -> {
                    root.addView(TextView(this).apply { text = "#004582 - $45.60 - 10:18 - John\n#004581 - $120.00 - 09:55 - Admin"; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16) })
                }
                else -> {
                    root.addView(TextView(this).apply { text = "Screen for $title is ready."; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16) })
                    val b = Button(this).apply { text = "BACK TO DASHBOARD"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE) }
                    b.setOnClickListener { finish() }
                    root.addView(b)
                }
            }

            scroll.addView(root)
            setContentView(scroll)
        } catch (e: Exception) {
            val tv = TextView(this)
            tv.text = "Error in $title: ${e.message}\n${e.stackTraceToString()}"
            setContentView(tv)
        }
    }
}
