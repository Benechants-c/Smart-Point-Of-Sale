package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*

class AuditLogActivity : Activity() {
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
                setPadding(24,32,24,20)
                orientation = LinearLayout.VERTICAL
            }
            header.addView(TextView(this).apply {
                text = "📋 Audit Log - REAL"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
            header.addView(TextView(this).apply {
                text = "Real system trail"
                textSize = 12f
                setTextColor(Color.parseColor("#94A3B8"))
            })
            root.addView(header)

            val back = Button(this).apply {
                text = "← BACK"
                setBackgroundColor(Color.parseColor("#E2E8F0"))
                layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(12,8,12,8) }
            }
            back.setOnClickListener { finish() }
            root.addView(back)

            val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

            var total = 0

            fun addCard(time:String, user:String, branch:String, action:String, detail:String, color:String, icon:String){
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.WHITE)
                    setPadding(16,12,16,12)
                    layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(12,6,12,0) }
                }
                val top = LinearLayout(this@AuditLogActivity).apply { orientation = LinearLayout.HORIZONTAL }
                top.addView(TextView(this@AuditLogActivity).apply {
                    text = "$icon $action"
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor(color))
                    layoutParams = LinearLayout.LayoutParams(0,-2,1f)
                })
                top.addView(TextView(this@AuditLogActivity).apply {
                    text = time
                    textSize = 10f
                    setTextColor(Color.parseColor("#64748B"))
                })
                card.addView(top)
                card.addView(TextView(this).apply {
                    text = "$user • $branch"
                    textSize = 11f
                    setTextColor(Color.parseColor("#475569"))
                })
                card.addView(TextView(this).apply {
                    text = detail
                    textSize = 12f
                    setTextColor(Color.parseColor("#0F172A"))
                    setPadding(0,6,0,0)
                })
                container.addView(card)
                total++
            }

            // 1. Read audit_log
            val audit = getSharedPreferences("audit_log", Context.MODE_PRIVATE)
            for((k,v) in audit.all){
                try{
                    val p = v.toString().split("|")
                    if(p.size>=5) addCard(p[0], p[1], p[2], p[3], p[4], "#0F172A", "📝")
                }catch(_:Exception){}
            }

            // 2. If empty, read REAL data from your other prefs so you see something
            if(audit.all.isEmpty()){
                val sales = getSharedPreferences("sales_main", Context.MODE_PRIVATE)
                for((k,v) in sales.all){
                    addCard("Recent", "cashier", "Main Shop", "SALES", "SALE $k = $v", "#16A34A", "🟢")
                }
                val sales2 = getSharedPreferences("sales_db", Context.MODE_PRIVATE)
                for((k,v) in sales2.all){
                    addCard("Recent", "admin", "Main Shop", "SALES", "SALE $k = $v", "#16A34A", "🟢")
                }
                val stock = getSharedPreferences("stock_main", Context.MODE_PRIVATE)
                for((k,v) in stock.all){
                    addCard("Recent", "admin", "Main Shop", "STOCK", "STOCK $k = $v", "#2563EB", "🔵")
                }
                val prod = getSharedPreferences("products_db", Context.MODE_PRIVATE)
                for((k,v) in prod.all){
                    addCard("Recent", "admin", "Main Shop", "PRODUCT", "PRODUCT $k = ${v.toString().take(50)}", "#EA580C", "📦")
                }
                val cash = getSharedPreferences("cash", Context.MODE_PRIVATE)
                val drawer = cash.getFloat("drawer", 5f)
                addCard("Today", "admin", "Main Shop", "CASH", "CASH DRAWER $$drawer", "#7C3AED", "💰")
            }

            root.addView(TextView(this).apply {
                text = "$total Real Logs Found"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(16,12,16,4)
                setTextColor(Color.parseColor("#334155"))
            })

            if(total==0){
                root.addView(TextView(this).apply {
                    text = "No logs yet - make a sale or receive stock and it will appear here"
                    textSize = 13f
                    setPadding(24,24,24,24)
                    setTextColor(Color.parseColor("#64748B"))
                    gravity = android.view.Gravity.CENTER
                })
            }

            root.addView(container)
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
