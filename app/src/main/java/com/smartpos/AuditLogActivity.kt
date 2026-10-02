package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*

class AuditLogActivity : Activity() {

    private val allLogs = mutableListOf<LogItem>()
    private lateinit var container: LinearLayout
    private lateinit var countText: TextView
    private var currentFilter = "ALL"

    data class LogItem(val time:String, val user:String, val branch:String, val action:String, val detail:String, val color:String, val icon:String)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try {
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#F8FAFC"))
            }

            // HEADER
            val header = LinearLayout(this).apply {
                setBackgroundColor(Color.parseColor("#0F172A"))
                setPadding(24,28,24,20)
                orientation = LinearLayout.VERTICAL
            }
            header.addView(TextView(this).apply {
                text = "📋 Audit Trail"
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
            header.addView(TextView(this).apply {
                text = "Real-time system activity"
                textSize = 12f
                setTextColor(Color.parseColor("#94A3B8"))
            })
            root.addView(header)

            // TOP BAR
            val topBar = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(12,8,12,8)
            }
            val back = Button(this).apply {
                text = "← BACK"
                setBackgroundColor(Color.parseColor("#E2E8F0"))
                layoutParams = LinearLayout.LayoutParams(-2,-2).apply { setMargins(0,0,8,0) }
            }
            back.setOnClickListener { finish() }
            val clear = Button(this).apply {
                text = "Clear"
                setBackgroundColor(Color.parseColor("#FEE2E2"))
                setTextColor(Color.parseColor("#DC2626"))
            }
            clear.setOnClickListener {
                getSharedPreferences("audit_log", MODE_PRIVATE).edit().clear().apply()
                loadLogs()
                filterAndShow()
                Toast.makeText(this, "Audit cleared", Toast.LENGTH_SHORT).show()
            }
            topBar.addView(back)
            topBar.addView(clear)
            root.addView(topBar)

            // SEARCH
            val search = EditText(this).apply {
                hint = "🔍 Search user, action, product..."
                setBackgroundColor(Color.WHITE)
                setPadding(16,12,16,12)
                layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(12,4,12,4) }
            }
            root.addView(search)

            // FILTERS
            val filters = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(12,4,12,4)
            }
            fun addFilter(name:String){
                val btn = Button(this).apply {
                    text = name
                    textSize = 11f
                    setPadding(12,6,12,6)
                    layoutParams = LinearLayout.LayoutParams(-2,-2).apply { setMargins(4,0,4,0) }
                }
                fun refreshStyle(){
                    if(currentFilter==name){
                        btn.setBackgroundColor(Color.parseColor("#0F172A"))
                        btn.setTextColor(Color.WHITE)
                    }else{
                        btn.setBackgroundColor(Color.parseColor("#E2E8F0"))
                        btn.setTextColor(Color.parseColor("#334155"))
                    }
                }
                refreshStyle()
                btn.setOnClickListener {
                    currentFilter = name
                    for(i in 0 until filters.childCount){
                        val b = filters.getChildAt(i) as Button
                        if(b.text.toString()==currentFilter){
                            b.setBackgroundColor(Color.parseColor("#0F172A"))
                            b.setTextColor(Color.WHITE)
                        }else{
                            b.setBackgroundColor(Color.parseColor("#E2E8F0"))
                            b.setTextColor(Color.parseColor("#334155"))
                        }
                    }
                    filterAndShow()
                }
                filters.addView(btn)
            }
            addFilter("ALL"); addFilter("SALES"); addFilter("STOCK"); addFilter("CASH"); addFilter("PRODUCT")
            root.addView(filters)

            countText = TextView(this).apply {
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setPadding(16,12,16,4)
                setTextColor(Color.parseColor("#334155"))
            }
            root.addView(countText)

            container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            root.addView(container)

            loadLogs()

            search.addTextChangedListener(object: TextWatcher{
                override fun afterTextChanged(s: Editable?) { filterAndShow(s.toString()) }
                override fun beforeTextChanged(s: CharSequence?, p1:Int, p2:Int, p3:Int) {}
                override fun onTextChanged(s: CharSequence?, p1:Int, p2:Int, p3:Int) {}
            })

            filterAndShow()

            scroll.addView(root)
            setContentView(scroll)

        } catch (e: Exception) {
            val tv = TextView(this)
            tv.text = "Error: ${e.message}\n${e.stackTraceToString()}"
            setContentView(tv)
        }
    }

    private fun loadLogs(){
        allLogs.clear()

        // 1. audit_log pref (future)
        try{
            val audit = getSharedPreferences("audit_log", Context.MODE_PRIVATE)
            for((_,v) in audit.all){
                val p = v.toString().split("|")
                if(p.size>=5) allLogs.add(LogItem(p[0], p[1], p[2], p[3], p[4], "#0F172A", "📝"))
            }
        }catch(_:Exception){}

        // 2. sales_main - format: id|total|profit|paid?
        try{
            val sales = getSharedPreferences("sales_main", MODE_PRIVATE)
            for((k,v) in sales.all){
                val s = v.toString()
                val parts = s.split("|")
                val total = if(parts.size>1) parts[1] else s
                val profit = if(parts.size>2) parts[2] else "0"
                allLogs.add(LogItem("Today", "cashier", "Main Shop", "SALES",
                    "Sold ${k.take(15)} • Total: $${fmt(total)} • Profit: $${fmt(profit)}", "#16A34A", "🟢"))
            }
        }catch(_:Exception){}

        // 3. sales_db
        try{
            val sales = getSharedPreferences("sales_db", MODE_PRIVATE)
            for((k,v) in sales.all){
                val parts = v.toString().split("|")
                val total = if(parts.size>1) parts[1] else v.toString()
                allLogs.add(LogItem("Today", "admin", "Main Shop", "SALES",
                    "Sale $k • $${fmt(total)}", "#16A34A", "🟢"))
            }
        }catch(_:Exception){}

        // 4. stock_main - format: name|buy|sell|qty|branch
        try{
            val stock = getSharedPreferences("stock_main", MODE_PRIVATE)
            for((k,v) in stock.all){
                val parts = v.toString().split("|")
                val name = if(parts.isNotEmpty()) parts[0] else k
                val buy = if(parts.size>1) parts[1] else "0"
                val sell = if(parts.size>2) parts[2] else "0"
                val qty = if(parts.size>3) parts[3] else "0"
                allLogs.add(LogItem("Today", "admin", "Main Shop", "STOCK",
                    "Stock $name • Qty: $qty • Buy $${fmt(buy)} • Sell $${fmt(sell)}", "#2563EB", "📦"))
            }
        }catch(_:Exception){}

        // 5. products_db
        try{
            val prod = getSharedPreferences("products_db", MODE_PRIVATE)
            for((k,v) in prod.all){
                val parts = v.toString().split("|")
                val name = if(parts.isNotEmpty()) parts[0] else k
                allLogs.add(LogItem("Today", "admin", "Main Shop", "PRODUCT",
                    "Product $name updated", "#EA580C", "🏷️"))
            }
        }catch(_:Exception){}

        // 6. cash drawer
        try{
            val cash = getSharedPreferences("cash", MODE_PRIVATE)
            val drawer = cash.getFloat("drawer", 0f)
            if(drawer>0) allLogs.add(LogItem("Today", "admin", "Main Shop", "CASH",
                "Cash Drawer: $${fmt(drawer.toString())}", "#7C3AED", "💰"))
        }catch(_:Exception){}

        allLogs.sortByDescending { it.time }
    }

    private fun filterAndShow(query:String=""){
        container.removeAllViews()
        var filtered = allLogs
        if(currentFilter!="ALL"){
            filtered = filtered.filter { it.action==currentFilter }.toMutableList()
        }
        if(query.isNotEmpty()){
            filtered = filtered.filter {
                it.detail.lowercase().contains(query.lowercase()) ||
                it.user.lowercase().contains(query.lowercase()) ||
                it.action.lowercase().contains(query.lowercase())
            }.toMutableList()
        }
        countText.text = "${filtered.size} Logs • Filter: $currentFilter"
        if(filtered.isEmpty()){
            container.addView(TextView(this).apply {
                text = "No activity found"
                setPadding(24,32,24,32)
                gravity = Gravity.CENTER
                setTextColor(Color.parseColor("#64748B"))
            })
            return
        }
        for(log in filtered){
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                setPadding(16,12,16,12)
                layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(12,6,12,0) }
            }
            val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            top.addView(TextView(this).apply {
                text = "${log.icon} ${log.action}"
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(log.color))
                layoutParams = LinearLayout.LayoutParams(0,-2,1f)
            })
            top.addView(TextView(this).apply {
                text = log.time
                textSize = 10f
                setTextColor(Color.parseColor("#94A3B8"))
            })
            card.addView(top)
            card.addView(TextView(this).apply {
                text = "${log.user} • ${log.branch}"
                textSize = 11f
                setTextColor(Color.parseColor("#64748B"))
            })
            card.addView(TextView(this).apply {
                text = log.detail
                textSize = 13f
                setTextColor(Color.parseColor("#0F172A"))
                setPadding(0,6,0,0)
            })
            container.addView(card)
        }
    }

    private fun fmt(s:String):String{
        return try{ String.format("%.2f", s.toDouble()) }catch(_:Exception){ s }
    }

    companion object {
        fun log(ctx: Context, user: String, branch: String, action: String, detail: String) {
            try {
                val fmt = java.text.SimpleDateFormat("dd/MM HH:mm:ss")
                val timeStr = fmt.format(java.util.Date())
                val key = System.currentTimeMillis().toString()
                val value = "$timeStr|$user|$branch|$action|$detail"
                ctx.getSharedPreferences("audit_log", Context.MODE_PRIVATE).edit().putString(key, value).apply()
            } catch (_: Exception) {}
        }
    }
}
