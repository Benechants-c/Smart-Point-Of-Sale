package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*

class AuditLogActivity : Activity() {

    data class LogEntry(val timeMillis: Long, val timeStr: String, val user: String, val branch: String, val action: String, val detail: String)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try {
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#F8FAFC"))
            }

            val header = LinearLayout(this).apply {
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(24, 32, 24, 20)
                orientation = LinearLayout.VERTICAL
            }
            header.addView(TextView(this).apply {
                text = "📋 Audit Log"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
            header.addView(TextView(this).apply {
                text = "System activity trail"
                textSize = 12f
                setTextColor(Color.parseColor("#94A3B8"))
            })
            root.addView(header)

            val back = Button(this).apply {
                text = "← BACK"
                setBackgroundColor(Color.parseColor("#E2E8F0"))
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(12, 8, 12, 0) }
            }
            back.setOnClickListener { finish() }
            root.addView(back)

            val auditPref = getSharedPreferences("audit_log", Context.MODE_PRIVATE)
            val list = ArrayList<LogEntry>()

            for ((k, v) in auditPref.all) {
                try {
                    val s = v.toString()
                    val parts = s.split("|")
                    if (parts.size >= 5) {
                        val millis = k.toLongOrNull()?: 0L
                        list.add(LogEntry(millis, parts[0], parts[1], parts[2], parts[3], parts[4]))
                    }
                } catch (_: Exception) {}
            }

            list.sortByDescending { it.timeMillis }

            root.addView(TextView(this).apply {
                text = "${list.size} Records"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(16, 16, 16, 8)
                setTextColor(Color.parseColor("#334155"))
            })

            if (list.isEmpty()) {
                val empty = LinearLayout(this).apply {
                    setBackgroundColor(Color.WHITE)
                    setPadding(24, 40, 24, 40)
                    layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(12, 8, 12, 0) }
                    orientation = LinearLayout.VERTICAL
                }
                empty.addView(TextView(this).apply {
                    text = "No activity yet"
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    gravity = android.view.Gravity.CENTER
                    setTextColor(Color.parseColor("#64748B"))
                })
                empty.addView(TextView(this).apply {
                    text = "Sales, stock receives, transfers, price changes and logins will be recorded here automatically."
                    textSize = 12f
                    gravity = android.view.Gravity.CENTER
                    setPadding(0, 12, 0, 0)
                    setTextColor(Color.parseColor("#94A3B8"))
                })
                root.addView(empty)
            } else {
                for (log in list) {
                    val color = when {
                        log.action.contains("SALE") -> "#16A34A"
                        log.action.contains("STOCK") || log.action.contains("RECEIVE") -> "#2563EB"
                        log.action.contains("TRANSFER") -> "#0D9488"
                        log.action.contains("PRICE") -> "#EA580C"
                        log.action.contains("CASH") -> "#7C3AED"
                        log.action.contains("LOGIN") || log.action.contains("USER") -> "#1E293B"
                        else -> "#0F172A"
                    }
                    val icon = when {
                        log.action.contains("SALE") -> "🟢"
                        log.action.contains("STOCK") || log.action.contains("RECEIVE") -> "🔵"
                        log.action.contains("TRANSFER") -> "🔄"
                        log.action.contains("PRICE") -> "🏷️"
                        log.action.contains("CASH") -> "💰"
                        log.action.contains("LOGIN") -> "👤"
                        else -> "📝"
                    }

                    val card = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(Color.WHITE)
                        setPadding(16, 12, 16, 12)
                        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(12, 6, 12, 0) }
                    }

                    val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                    top.addView(TextView(this@AuditLogActivity).apply {
                        text = "$icon ${log.action}"
                        textSize = 13f
                        setTypeface(null, Typeface.BOLD)
                        setTextColor(Color.parseColor(color))
                        layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    })
                    top.addView(TextView(this@AuditLogActivity).apply {
                        text = log.timeStr
                        textSize = 10f
                        setTextColor(Color.parseColor("#64748B"))
                    })
                    card.addView(top)
                    card.addView(TextView(this).apply {
                        text = "${log.user} • ${log.branch}"
                        textSize = 11f
                        setTextColor(Color.parseColor("#475569"))
                        setPadding(0, 4, 0, 0)
                    })
                    card.addView(TextView(this).apply {
                        text = log.detail
                        textSize = 12f
                        setTextColor(Color.parseColor("#0F172A"))
                        setPadding(0, 8, 0, 0)
                    })
                    root.addView(card)
                }
            }

            scroll.addView(root)
            setContentView(scroll)

        } catch (e: Exception) {
            val tv = TextView(this)
            tv.text = "Error: ${e.message}"
            setContentView(tv)
        }
    }

    companion object {
        fun log(ctx: Context, user: String, branch: String, action: String, detail: String) {
            try {
                val fmt = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                val timeStr = fmt.format(java.util.Date())
                val key = System.currentTimeMillis().toString()
                val value = "$timeStr|$user|$branch|$action|$detail"
                ctx.getSharedPreferences("audit_log", Context.MODE_PRIVATE).edit().putString(key, value).apply()
            } catch (_: Exception) {}
        }
    }
}
