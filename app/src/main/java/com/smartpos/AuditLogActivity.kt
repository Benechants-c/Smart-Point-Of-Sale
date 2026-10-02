package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class AuditLogActivity : Activity() {

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try {
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#F8FAFC"))
                setPadding(16,16,16,16)
            }

            root.addView(TextView(this).apply {
                text = "Audit Trail"
                textSize = 20f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
                setPadding(0,0,0,16)
            })

            val btnBack = Button(this).apply { text = "← BACK TO ADMIN" }
            btnBack.setOnClickListener { finish() }
            root.addView(btnBack)

            val scroll = ScrollView(this)
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,16,0,0) }

            // Load sales
            var count = 0
            try {
                val sales = getSharedPreferences("sales_main", Context.MODE_PRIVATE)
                val sorted = sales.all.toList().sortedByDescending { it.first }
                for ((k, v) in sorted) {
                    count++
                    var items = ""
                    try {
                        val ip = getSharedPreferences("sale_items_$k", Context.MODE_PRIVATE)
                        if (ip.all.isNotEmpty()) {
                            items = ip.all.values.joinToString(", ") { it.toString().split("|")[0] }
                        }
                    } catch (_: Exception) {}

                    if (items.isEmpty()) items = "Legacy Sale - $k"
                    
                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(Color.WHITE)
                        setPadding(16,12,16,12)
                        layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(0,0,0,12) }
                    }
                    card.addView(TextView(this).apply {
                        text = "SALES • ${k.take(20)}"
                        setTypeface(null, Typeface.BOLD)
                        textSize = 12f
                        setTextColor(Color.parseColor("#16A34A"))
                    })
                    card.addView(TextView(this).apply {
                        text = items
                        textSize = 14f
                        setTextColor(Color.BLACK)
                        setPadding(0,6,0,6)
                    })
                    card.addView(TextView(this).apply {
                        text = v.toString()
                        textSize = 11f
                        setTextColor(Color.GRAY)
                    })
                    list.addView(card)
                }
            } catch (e: Exception) {
                list.addView(TextView(this).apply { text = "Error: ${e.message}" })
            }

            if (count == 0) {
                list.addView(TextView(this).apply {
                    text = "No sales yet. Make a new sale with #191 Sales and it will appear here."
                    gravity = Gravity.CENTER
                    setPadding(20,40,20,40)
                    setTextColor(Color.GRAY)
                })
            }

            scroll.addView(list)
            root.addView(scroll)
            setContentView(root)

        } catch (e: Exception) {
            setContentView(TextView(this).apply { text = "Audit Error: ${e.message}" })
        }
    }

    companion object {
        fun log(ctx: Context, user: String, branch: String, action: String, detail: String) {
            try {
                val key = System.currentTimeMillis().toString()
                val value = "$user|$branch|$action|$detail"
                ctx.getSharedPreferences("audit_log", Context.MODE_PRIVATE).edit().putString(key, value).apply()
            } catch (_: Exception) {}
        }
    }
}
