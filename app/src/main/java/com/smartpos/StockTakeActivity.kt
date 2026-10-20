package com.smartpos

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.Typeface
import java.text.SimpleDateFormat
import java.util.*

data class TakeEntry(
    val stockItem: StockItem,
    var counted: Int? = null,
    var variance: Int? = null
)

class StockTakeActivity : AppCompatActivity() {

    private lateinit var etTakeName: EditText
    private lateinit var container: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var tvSummary: TextView
    private lateinit var tvCount: TextView
    private lateinit var btnStart: Button

    private val allEntries = mutableListOf<TakeEntry>()
    private var filteredEntries = mutableListOf<TakeEntry>()
    private var takeStarted = false
    private var currentTakeName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#263238"))
            setPadding(30,25,30,25)
        }
        val title = TextView(this).apply {
            text = "STOCK TAKE - LIVE VARIANCE"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val cashier = prefs.getString("cashier_name", prefs.getString("logged_user", "Cashier"))
        val tvHead = TextView(this).apply {
            text = "Cashier: $cashier | Real: ${sdf.format(Date())}"
            setTextColor(Color.parseColor("#CFD8DC"))
            textSize = 11f
        }
        header.addView(title)
        header.addView(tvHead)

        // START NEW STOCK TAKE - NAME REQUIRED
        val nameBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(20,20,20,20)
        }
        val nameLabel = TextView(this).apply {
            text = "Enter Stock Take Name to Start (e.g. January Stock Take, Spot Check, End of Month):"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#263238"))
            setPadding(0,0,0,10)
        }
        etTakeName = EditText(this).apply {
            hint = ""
            setBackgroundResource(android.R.drawable.editbox_background)
            setPadding(25,25,25,25)
        }
        btnStart = Button(this).apply {
            text = "START STOCK TAKE"
            setBackgroundColor(Color.parseColor("#263238"))
            setTextColor(Color.WHITE)
        }
        nameBox.addView(nameLabel)
        nameBox.addView(etTakeName)
        nameBox.addView(btnStart)

        // Search
        val searchBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E0F2F1"))
            setPadding(15,15,15,15)
        }
        etSearch = EditText(this).apply {
            hint = ""
            setBackgroundResource(android.R.drawable.editbox_background)
            setPadding(25,25,25,25)
        }
        tvCount = TextView(this).apply { textSize = 12f; setTextColor(Color.GRAY); setPadding(5,10,5,0) }
        searchBox.addView(etSearch)
        searchBox.addView(tvCount)

        val summaryBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#ECEFF1"))
            setPadding(15,15,15,15)
        }
        tvSummary = TextView(this).apply { textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.BLACK) }
        val legend = TextView(this).apply {
            text = "Variance LIVE: ✅ 0 = GREEN | 🔵 Positive = BLUE | 🔴 Negative = RED"
            textSize = 11f
            setTextColor(Color.DKGRAY)
            setPadding(0,8,0,0)
        }
        summaryBox.addView(tvSummary)
        summaryBox.addView(legend)

        val listHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#37474F"))
            setPadding(10,12,10,12)
        }
        fun addH(t: String, w: Float) {
            val tv = TextView(this@StockTakeActivity).apply {
                text = t; setTextColor(Color.WHITE); textSize = 11f; typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w)
            }
            listHeader.addView(tv)
        }
        addH("PRODUCT", 1.8f); addH("CODE", 1f); addH("ON HAND", 0.9f); addH("COUNTED", 1f); addH("VARIANCE", 1f)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(5,5,5,5) }
        scroll.addView(container)

        val btnSave = Button(this).apply {
            text = "💾 SAVE & OVERRIDE STOCK"
            setBackgroundColor(Color.parseColor("#00796B"))
            setTextColor(Color.WHITE)
        }

        root.addView(header)
        root.addView(nameBox)
        root.addView(searchBox)
        root.addView(summaryBox)
        root.addView(listHeader)
        root.addView(scroll)
        root.addView(btnSave)

        setContentView(root)

        btnStart.setOnClickListener {
            val name = etTakeName.text.toString().trim()
            if (name.isEmpty()) {
                etTakeName.error = "Enter stock take name e.g. January Stock Take"
                return@setOnClickListener
            }
            currentTakeName = name
            takeStarted = true
            loadFromStockList()
            render(allEntries)
            Toast.makeText(this, "Started: $name - Enter counted qty - Live variance", Toast.LENGTH_LONG).show()
        }

        btnSave.setOnClickListener { saveAndOverride() }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s.toString().trim().lowercase()
                filteredEntries = if (q.isEmpty()) allEntries.toMutableList() else allEntries.filter {
                    it.stockItem.code.lowercase().contains(q) || it.stockItem.name.lowercase().contains(q)
                }.toMutableList()
                render(filteredEntries)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadFromStockList() {
        allEntries.clear()
        val goodsPrefs = getSharedPreferences("goods_received", MODE_PRIVATE)
        val productsPrefs = getSharedPreferences("products", MODE_PRIVATE)
        val stockPrefs = getSharedPreferences("stock_data", MODE_PRIVATE)

        val combined = mutableMapOf<String, Any?>()
        for ((k, v) in goodsPrefs.all) combined[k] = v
        for ((k, v) in productsPrefs.all) combined[k] = v
        for ((k, v) in stockPrefs.all) combined[k] = v

        val tempStock = mutableListOf<StockItem>()

        if (combined.isNotEmpty()) {
            for ((key, value) in combined) {
                try {
                    val str = value.toString()
                    if (str.contains("|")) {
                        val parts = str.split("|")
                        val code = parts.getOrNull(0) ?: key
                        val name = parts.getOrNull(1) ?: key
                        val qty = parts.getOrNull(2)?.toIntOrNull() ?: 0
                        val cost = parts.getOrNull(3)?.toDoubleOrNull() ?: 1.0
                        val sell = parts.getOrNull(4)?.toDoubleOrNull() ?: cost * 1.3
                        tempStock.add(StockItem(code, name, qty, cost, sell))
                    } else {
                        val cost = str.toDoubleOrNull()
                        if (cost != null) {
                            tempStock.add(StockItem(key.take(8).uppercase(), key, 0, cost, cost * 1.5))
                        }
                    }
                } catch (e: Exception) {}
            }
        }

        // No demo hints - leave empty if no stock, user said don't put hints
        // But if we have real stock from prefs we show it
        for (item in tempStock) {
            allEntries.add(TakeEntry(item, null, null))
        }
        filteredEntries = allEntries.toMutableList()
    }

    private fun render(list: List<TakeEntry>) {
        container.removeAllViews()
        if (!takeStarted) {
            val tv = TextView(this).apply {
                text = "Enter Stock Take Name above and press START"
                setPadding(30,50,30,50)
                textSize = 14f
                setTextColor(Color.GRAY)
                gravity = android.view.Gravity.CENTER
            }
            container.addView(tv)
            return
        }

        for (entry in list) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.WHITE)
                setPadding(10,10,10,10)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0,0,0,6)
                layoutParams = lp
            }

            val tvProduct = TextView(this).apply {
                text = entry.stockItem.name
                textSize = 12f
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.8f)
            }
            val tvCode = TextView(this).apply {
                text = entry.stockItem.code
                textSize = 11f
                setTextColor(Color.parseColor("#1976D2"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val tvOnHand = TextView(this).apply {
                text = "${entry.stockItem.qty}"
                textSize = 12f
                setTextColor(Color.BLACK)
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.9f)
                gravity = android.view.Gravity.CENTER
            }
            val etCounted = EditText(this).apply {
                hint = ""
                textSize = 12f
                setBackgroundResource(android.R.drawable.editbox_background)
                setPadding(10,10,10,10)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(5,0,5,0) }
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                if (entry.counted != null) setText("${entry.counted}")
            }
            val tvVariance = TextView(this).apply {
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                gravity = android.view.Gravity.CENTER
                setPadding(5,8,5,8)
                text = ""
                setBackgroundColor(Color.parseColor("#EEEEEE"))
            }

            fun updateVariance() {
                val countedStr = etCounted.text.toString().trim()
                if (countedStr.isEmpty()) {
                    entry.counted = null
                    entry.variance = null
                    tvVariance.text = ""
                    tvVariance.setBackgroundColor(Color.parseColor("#EEEEEE"))
                    tvVariance.setTextColor(Color.GRAY)
                } else {
                    val countedVal = countedStr.toIntOrNull() ?: 0
                    entry.counted = countedVal
                    val varianceVal = countedVal - entry.stockItem.qty
                    entry.variance = varianceVal
                    when {
                        varianceVal == 0 -> {
                            tvVariance.setBackgroundColor(Color.parseColor("#C8E6C9"))
                            tvVariance.setTextColor(Color.parseColor("#2E7D32"))
                            tvVariance.text = "0"
                        }
                        varianceVal > 0 -> {
                            tvVariance.setBackgroundColor(Color.parseColor("#BBDEFB"))
                            tvVariance.setTextColor(Color.parseColor("#0D47A1"))
                            tvVariance.text = "+$varianceVal"
                        }
                        else -> {
                            tvVariance.setBackgroundColor(Color.parseColor("#FFCDD2"))
                            tvVariance.setTextColor(Color.parseColor("#B71C1C"))
                            tvVariance.text = "$varianceVal"
                        }
                    }
                }
                updateSummary()
            }

            etCounted.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { updateVariance() }
                override fun afterTextChanged(s: Editable?) {}
            })

            row.addView(tvProduct)
            row.addView(tvCode)
            row.addView(tvOnHand)
            row.addView(etCounted)
            row.addView(tvVariance)

            container.addView(row)
        }
        tvCount.text = "${list.size} products from Stock List | Take: $currentTakeName"
        updateSummary()
    }

    private fun updateSummary() {
        var totalOnHand = 0
        var totalCounted = 0
        var totalVariance = 0
        var countedItems = 0
        var ok = 0
        var over = 0
        var shortage = 0
        for (e in allEntries) {
            totalOnHand += e.stockItem.qty
            if (e.counted != null) {
                countedItems++
                totalCounted += e.counted!!
                totalVariance += e.variance ?: 0
                when {
                    e.variance == 0 -> ok++
                    (e.variance ?: 0) > 0 -> over++
                    (e.variance ?: 0) < 0 -> shortage++
                }
            }
        }
        tvSummary.text = "On Hand: $totalOnHand | Counted: $totalCounted ($countedItems items) | Var: $totalVariance | ✅ $ok 🔵 $over 🔴 $shortage"
    }

    private fun saveAndOverride() {
        if (!takeStarted || currentTakeName.isEmpty()) {
            Toast.makeText(this, "Enter Stock Take Name and START first", Toast.LENGTH_SHORT).show()
            return
        }
        val hasCount = allEntries.any { it.counted != null }
        if (!hasCount) {
            Toast.makeText(this, "Enter at least one counted qty", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Override available stocks - update goods_received, stock_data, products
        val goodsPrefs = getSharedPreferences("goods_received", MODE_PRIVATE)
        val stockPrefs = getSharedPreferences("stock_data", MODE_PRIVATE)
        val productsPrefs = getSharedPreferences("products", MODE_PRIVATE)
        val goodsEdit = goodsPrefs.edit()
        val stockEdit = stockPrefs.edit()
        val prodEdit = productsPrefs.edit()

        for (e in allEntries) {
            if (e.counted != null) {
                val newQty = e.counted!!
                val code = e.stockItem.code
                val name = e.stockItem.name
                val cost = e.stockItem.cost
                val sell = e.stockItem.selling
                val value = "$code|$name|$newQty|$cost|$sell"
                goodsEdit.putString(code, value)
                stockEdit.putString(code, value)
                prodEdit.putString(code, value)
            }
        }
        goodsEdit.apply()
        stockEdit.apply()
        prodEdit.apply()

        // 2. Save this stock take history with name e.g. January Stock Take
        val historyPrefs = getSharedPreferences("stock_take_history", MODE_PRIVATE)
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val key = "${currentTakeName}_${dateStr}".replace(" ", "_")
        val sb = StringBuilder()
        for (e in allEntries) {
            if (e.counted != null) {
                sb.append("${e.stockItem.code}|${e.stockItem.name}|${e.stockItem.qty}|${e.counted}|${e.variance}\n")
            }
        }
        historyPrefs.edit().putString(key, sb.toString()).apply()

        // Also save last take name
        getSharedPreferences("stock_take", MODE_PRIVATE).edit()
            .putString("last_take_name", currentTakeName)
            .putString("last_take_date", dateStr)
            .apply()

        Toast.makeText(this, "Saved '$currentTakeName' and OVERRIDED stock - ${allEntries.count { it.counted != null }} items", Toast.LENGTH_LONG).show()
    }
}