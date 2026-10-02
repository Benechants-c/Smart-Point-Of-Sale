package com.smartpos

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class AuditLogActivity : Activity() {

    private val allLogs = mutableListOf<LogItem>()
    private lateinit var container: LinearLayout
    private lateinit var countText: TextView
    private var fromDate: Long = 0L
    private var toDate: Long = System.currentTimeMillis()
    private var selectedProduct = "ALL"
    private var currentType = "ALL"
    private val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val sdfFull = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    data class LogItem(val ts: Long, val timeStr: String, val type: String, val detail: String, val productKey: String, val dept: String)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC")) }

        // Header
        val header = LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#0F172A")); setPadding(20,20,20,16); orientation = LinearLayout.VERTICAL }
        header.addView(TextView(this).apply { text = "Audit Trail - Ultra"; textSize = 18f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE) })
        header.addView(TextView(this).apply { text = "Date + Product + Category Filter"; textSize = 11f; setTextColor(Color.parseColor("#94A3B8")) })
        root.addView(header)

        val back = Button(this).apply { text = "BACK"; }
        back.setOnClickListener { finish() }
        root.addView(back, LinearLayout.LayoutParams(-1,-2).apply { setMargins(12,8,12,4) })

        // DATE ROW
        val dateRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,4,12,4) }
        val btnFrom = Button(this).apply { text = "From: ALL"; textSize = 11f }
        val btnTo = Button(this).apply { text = "To: ${sdfDate.format(Date(toDate))}"; textSize = 11f }
        btnFrom.setOnClickListener { pickDate(true, btnFrom) }
        btnTo.setOnClickListener { pickDate(false, btnTo) }
        dateRow.addView(btnFrom, LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(0,0,4,0) })
        dateRow.addView(btnTo, LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(4,0,0,0) })
        root.addView(dateRow)

        // PRODUCT + CATEGORY
        val products = getProductNames()
        val spinnerItems = mutableListOf<String>()
        spinnerItems.add("ALL PRODUCTS")
        spinnerItems.addAll(products)
        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerItems)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        root.addView(spinner, LinearLayout.LayoutParams(-1,-2).apply { setMargins(12,4,12,4) })
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                selectedProduct = if (pos == 0) "ALL" else spinnerItems[pos]
                filterAndShow()
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        val search = EditText(this).apply { hint = "Search stove, tv, dept..."; setBackgroundColor(Color.WHITE); setPadding(16,12,16,12) }
        root.addView(search, LinearLayout.LayoutParams(-1,-2).apply { setMargins(12,4,12,4) })

        val typeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,4,12,4) }
        fun addType(name: String) {
            val btn = Button(this).apply { text = name; textSize = 11f }
            btn.setOnClickListener { currentType = name; filterAndShow(search.text.toString()) }
            typeRow.addView(btn, LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(2,0,2,0) })
        }
        addType("ALL"); addType("SALES"); addType("STOCK")
        root.addView(typeRow)

        countText = TextView(this).apply { setPadding(16,8,16,4); setTypeface(null, Typeface.BOLD); textSize = 12f }
        root.addView(countText)

        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(container) }
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))

        loadLogs()
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterAndShow(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        filterAndShow()
        setContentView(root)
    }

    private fun pickDate(isFrom: Boolean, btn: Button) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val cal = Calendar.getInstance()
            cal.set(y, m, d, if (isFrom) 0 else 23, if (isFrom) 0 else 59, 0)
            if (isFrom) fromDate = cal.timeInMillis else toDate = cal.timeInMillis
            btn.text = (if (isFrom) "From: " else "To: ") + sdfDate.format(Date(cal.timeInMillis))
            filterAndShow()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun getProductNames(): List<String> {
        val set = mutableSetOf<String>()
        try {
            val pref = getSharedPreferences("stock_main", Context.MODE_PRIVATE)
            for ((_, v) in pref.all) { set.add(v.toString().split("|")[0]) }
            val pref2 = getSharedPreferences("stock_main", Context.MODE_PRIVATE)
            for ((_, v) in pref2.all) { set.add(v.toString().split("|")[0]) }
            // also from sale_items
            val sales = getSharedPreferences("sales_main", Context.MODE_PRIVATE)
            for ((k, _) in sales.all) {
                try {
                    val ip = getSharedPreferences("sale_items_$k", Context.MODE_PRIVATE)
                    for ((_, v2) in ip.all) { set.add(v2.toString().split("|")[0]) }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return set.filter { it.isNotEmpty() }.distinct().sorted()
    }

    private fun loadLogs() {
        allLogs.clear()
        try {
            val sales = getSharedPreferences("sales_main", Context.MODE_PRIVATE)
            for ((k, v) in sales.all) {
                val ts = try { k.replace("SALE_","").toLong() } catch (_: Exception) { System.currentTimeMillis() }
                var itemsText = ""
                var productKey = ""
                var deptKey = ""
                try {
                    val ip = getSharedPreferences("sale_items_$k", Context.MODE_PRIVATE)
                    if (ip.all.isNotEmpty()) {
                        val partsList = ip.all.values.map { it.toString() }
                        itemsText = partsList.joinToString(", ") {
                            val p = it.split("|")
                            val name = if (p.isNotEmpty()) p[0] else it
                            val qty = if (p.size > 1) p[1] else "1"
                            val dept = if (p.size > 3) p[3] else "General"
                            deptKey += " $dept"
                            "$name x$qty"
                        }
                        productKey = itemsText.lowercase()
                    }
                } catch (_: Exception) {}

                if (itemsText.isEmpty()) {
                    itemsText = "Legacy Sale"
                    productKey = "legacy " + getProductNames().joinToString(" ").lowercase()
                    deptKey = "General"
                }

                val parts = v.toString().split("|")
                val total = if (parts.size > 1) parts[1] else "0"
                val detail = "$itemsText\nTotal: $$total\nDept: $deptKey"
                allLogs.add(LogItem(ts, sdfFull.format(Date(ts)), "SALES", detail, productKey, deptKey.lowercase()))
            }
        } catch (_: Exception) {}

        try {
            val stock = getSharedPreferences("stock_main", Context.MODE_PRIVATE)
            for ((_, v) in stock.all) {
                val p = v.toString().split("|")
                val name = if (p.isNotEmpty()) p[0] else "Item"
                val dept = if (p.size > 5) p[5] else "General"
                allLogs.add(LogItem(System.currentTimeMillis(), sdfFull.format(Date()), "STOCK", "Stock $name Qty:${if(p.size>3) p[3] else "0"} Dept:$dept", name.lowercase(), dept.lowercase()))
            }
        } catch (_: Exception) {}

        allLogs.sortByDescending { it.ts }
    }

    private fun filterAndShow(q: String = "") {
        container.removeAllViews()
        var list = allLogs.filter { if (fromDate == 0L) true else it.ts in fromDate..toDate }
        if (currentType!= "ALL") list = list.filter { it.type == currentType }
        if (selectedProduct!= "ALL") {
            val sel = selectedProduct.lowercase()
            list = list.filter { it.productKey.contains(sel) || it.detail.lowercase().contains(sel) || it.dept.contains(sel) }
        }
        if (q.isNotEmpty()) {
            list = list.filter { it.detail.lowercase().contains(q.lowercase()) || it.productKey.contains(q.lowercase()) || it.dept.contains(q.lowercase()) }
        }

        countText.text = "${list.size} Logs • ${selectedProduct} • ${if (fromDate == 0L) "ALL TIME" else sdfDate.format(Date(fromDate)) + "→" + sdfDate.format(Date(toDate))}"

        if (list.isEmpty()) {
            container.addView(TextView(this).apply { text = "No activity for filters\nTry ALL PRODUCTS"; gravity = Gravity.CENTER; setPadding(24,32,24,32); setTextColor(Color.GRAY) })
            return
        }

        for (log in list) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                setPadding(16,12,16,12)
                layoutParams = LinearLayout.LayoutParams(-1,-2).apply { setMargins(12,6,12,6) }
            }
            card.addView(TextView(this).apply { text = "${log.type} • ${log.timeStr}"; setTypeface(null, Typeface.BOLD); textSize = 11f; setTextColor(if (log.type == "SALES") Color.parseColor("#16A34A") else Color.parseColor("#2563EB")) })
            card.addView(TextView(this).apply { text = log.detail; textSize = 13f; setTextColor(Color.parseColor("#0F172A")); setPadding(0,6,0,0) })
            container.addView(card)
        }
    }

    companion object {
        fun log(ctx: Context, user: String, branch: String, action: String, detail: String) {
            try {
                val key = System.currentTimeMillis().toString()
                ctx.getSharedPreferences("audit_log", Context.MODE_PRIVATE).edit().putString(key, "$user|$branch|$action|$detail").apply()
            } catch (_: Exception) {}
        }
    }
}
