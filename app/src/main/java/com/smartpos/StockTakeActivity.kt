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

data class StockItem(val code: String, val name: String, val qty: Int, val cost: Double, val selling: Double)
data class TakeEntry(val stockItem: StockItem, var counted: Int? = null, var variance: Int? = null)

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
        try{
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F5F5F5"))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setBackgroundColor(Color.parseColor("#263238"))
        header.setPadding(30,25,30,25)
        val title = TextView(this)
        title.text = "STOCK TAKE - LIVE VARIANCE"
        title.setTextColor(Color.WHITE)
        title.textSize = 18f
        title.setTypeface(null, Typeface.BOLD)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val cashier = prefs.getString("cashier_name", prefs.getString("logged_user", "Cashier") ?: "Cashier") ?: "Cashier"
        val tvHead = TextView(this)
        tvHead.text = "Cashier: $cashier | Real: ${sdf.format(Date())}"
        tvHead.setTextColor(Color.parseColor("#CFD8DC"))
        tvHead.textSize = 11f
        header.addView(title)
        header.addView(tvHead)

        val nameBox = LinearLayout(this)
        nameBox.orientation = LinearLayout.VERTICAL
        nameBox.setBackgroundColor(Color.WHITE)
        nameBox.setPadding(20,20,20,20)
        val nameLabel = TextView(this)
        nameLabel.text = "Enter Stock Take Name to Start (e.g. January Stock Take, Spot Check):"
        nameLabel.textSize = 13f
        nameLabel.setTypeface(null, Typeface.BOLD)
        nameLabel.setTextColor(Color.parseColor("#263238"))
        nameLabel.setPadding(0,0,0,10)
        etTakeName = EditText(this)
        etTakeName.hint = "Stock Take Name"
        etTakeName.setPadding(25,25,25,25)
        etTakeName.setBackgroundColor(Color.WHITE)
        btnStart = Button(this)
        btnStart.text = "START STOCK TAKE"
        btnStart.setBackgroundColor(Color.parseColor("#263238"))
        btnStart.setTextColor(Color.WHITE)
        nameBox.addView(nameLabel)
        nameBox.addView(etTakeName)
        nameBox.addView(btnStart)

        val searchBox = LinearLayout(this)
        searchBox.orientation = LinearLayout.VERTICAL
        searchBox.setBackgroundColor(Color.parseColor("#E0F2F1"))
        searchBox.setPadding(15,15,15,15)
        etSearch = EditText(this)
        etSearch.hint = "Search Code or Product..."
        etSearch.setPadding(25,25,25,25)
        etSearch.setBackgroundColor(Color.WHITE)
        tvCount = TextView(this)
        tvCount.textSize = 12f
        tvCount.setTextColor(Color.GRAY)
        tvCount.setPadding(5,10,5,0)
        searchBox.addView(etSearch)
        searchBox.addView(tvCount)

        val summaryBox = LinearLayout(this)
        summaryBox.orientation = LinearLayout.VERTICAL
        summaryBox.setBackgroundColor(Color.parseColor("#ECEFF1"))
        summaryBox.setPadding(15,15,15,15)
        tvSummary = TextView(this)
        tvSummary.textSize = 13f
        tvSummary.setTypeface(null, Typeface.BOLD)
        val legend = TextView(this)
        legend.text = "Variance LIVE: 0 = GREEN | Positive = BLUE | Negative = RED"
        legend.textSize = 11f
        legend.setTextColor(Color.DKGRAY)
        legend.setPadding(0,8,0,0)
        summaryBox.addView(tvSummary)
        summaryBox.addView(legend)

        val listHeader = LinearLayout(this)
        listHeader.orientation = LinearLayout.HORIZONTAL
        listHeader.setBackgroundColor(Color.parseColor("#37474F"))
        listHeader.setPadding(10,12,10,12)
        fun addH(t: String, w: Float) {
            val tv = TextView(this@StockTakeActivity)
            tv.text = t; tv.setTextColor(Color.WHITE); tv.textSize = 11f; tv.setTypeface(null,Typeface.BOLD)
            tv.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w)
            listHeader.addView(tv)
        }
        addH("PRODUCT", 1.8f); addH("CODE", 1f); addH("ON HAND", 0.9f); addH("COUNTED", 1f); addH("VARIANCE", 1f)

        val scroll = ScrollView(this)
        val lpScroll = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        lpScroll.weight = 1f
        scroll.layoutParams = lpScroll
        container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(5,5,5,5)
        scroll.addView(container)

        val btnSave = Button(this)
        btnSave.text = "SAVE & OVERRIDE STOCK"
        btnSave.setBackgroundColor(Color.parseColor("#00796B"))
        btnSave.setTextColor(Color.WHITE)

        root.addView(header)
        root.addView(nameBox)
        root.addView(searchBox)
        root.addView(summaryBox)
        root.addView(listHeader)
        root.addView(scroll)
        root.addView(btnSave)

        setContentView(root)

        btnStart.setOnClickListener {
            try{
            val name = etTakeName.text.toString().trim()
            if (name.isEmpty()) {
                etTakeName.error = "Enter stock take name e.g. January Stock Take"
                return@setOnClickListener
            }
            currentTakeName = name
            takeStarted = true
            loadFromStockList()
            render(allEntries)
            Toast.makeText(this, "Started: $name - Enter counted qty", Toast.LENGTH_LONG).show()
            }catch(e:Exception){ Toast.makeText(this,"Start error: ${e.message}",Toast.LENGTH_LONG).show() }
        }

        btnSave.setOnClickListener { saveAndOverride() }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try{
                val q = s.toString().trim().lowercase()
                filteredEntries = if (q.isEmpty()) allEntries.toMutableList() else allEntries.filter {
                    it.stockItem.code.lowercase().contains(q) || it.stockItem.name.lowercase().contains(q)
                }.toMutableList()
                render(filteredEntries)
                }catch(_:Exception){}
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        }catch(e:Exception){ Toast.makeText(this,"Init error: ${e.message}",Toast.LENGTH_LONG).show(); finish() }
    }

    private fun loadFromStockList() {
        try{
        allEntries.clear()
        val goodsPrefs = getSharedPreferences("goods_received", MODE_PRIVATE)
        val productsPrefs = getSharedPreferences("products", MODE_PRIVATE)
        val stockPrefs = getSharedPreferences("stock_data", MODE_PRIVATE)

        val combined = mutableMapOf<String, Any?>()
        for ((k, v) in goodsPrefs.all) combined[k] = v
        for ((k, v) in productsPrefs.all) combined[k] = v
        for ((k, v) in stockPrefs.all) combined[k] = v

        val tempStock = mutableListOf<StockItem>()

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
                }
            } catch (_: Exception) {}
        }
        for (item in tempStock) {
            allEntries.add(TakeEntry(item, null, null))
        }
        filteredEntries = allEntries.toMutableList()
        }catch(_:Exception){ allEntries.clear() }
    }

    private fun render(list: List<TakeEntry>) {
        try{
        container.removeAllViews()
        if (!takeStarted) {
            val tv = TextView(this)
            tv.text = "Enter Stock Take Name above and press START"
            tv.setPadding(30,50,30,50)
            tv.textSize = 14f
            tv.setTextColor(Color.GRAY)
            container.addView(tv)
            return
        }

        for (entry in list) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setBackgroundColor(Color.WHITE)
            row.setPadding(10,10,10,10)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0,0,0,6)
            row.layoutParams = lp

            val tvProduct = TextView(this)
            tvProduct.text = entry.stockItem.name
            tvProduct.textSize = 12f
            tvProduct.setTextColor(Color.BLACK)
            tvProduct.setTypeface(null, Typeface.BOLD)
            tvProduct.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.8f)

            val tvCode = TextView(this)
            tvCode.text = entry.stockItem.code
            tvCode.textSize = 11f
            tvCode.setTextColor(Color.parseColor("#1976D2"))
            tvCode.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

            val tvOnHand = TextView(this)
            tvOnHand.text = "${entry.stockItem.qty}"
            tvOnHand.textSize = 12f
            tvOnHand.setTextColor(Color.BLACK)
            tvOnHand.setTypeface(null, Typeface.BOLD)
            tvOnHand.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 0.9f)

            val etCounted = EditText(this)
            etCounted.hint = "0"
            etCounted.textSize = 12f
            etCounted.setBackgroundColor(Color.WHITE)
            etCounted.setPadding(10,10,10,10)
            etCounted.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            etCounted.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            if (entry.counted != null) etCounted.setText("${entry.counted}")

            val tvVariance = TextView(this)
            tvVariance.textSize = 13f
            tvVariance.setTypeface(null, Typeface.BOLD)
            tvVariance.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            tvVariance.setPadding(5,8,5,8)
            tvVariance.text = ""
            tvVariance.setBackgroundColor(Color.parseColor("#EEEEEE"))

            fun updateVariance() {
                try{
                val countedStr = etCounted.text.toString().trim()
                if (countedStr.isEmpty()) {
                    entry.counted = null
                    entry.variance = null
                    tvVariance.text = ""
                    tvVariance.setBackgroundColor(Color.parseColor("#EEEEEE"))
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
                }catch(_:Exception){}
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
        tvCount.text = "${list.size} products | Take: $currentTakeName"
        updateSummary()
        }catch(e:Exception){ Toast.makeText(this,"Render error",Toast.LENGTH_SHORT).show() }
    }

    private fun updateSummary() {
        try{
        var totalOnHand = 0
        var totalCounted = 0
        var countedItems = 0
        var ok = 0
        var over = 0
        var shortage = 0
        for (e in allEntries) {
            totalOnHand += e.stockItem.qty
            if (e.counted != null) {
                countedItems++
                totalCounted += e.counted!!
                when {
                    e.variance == 0 -> ok++
                    (e.variance ?: 0) > 0 -> over++
                    (e.variance ?: 0) < 0 -> shortage++
                }
            }
        }
        tvSummary.text = "On Hand: $totalOnHand | Counted: $totalCounted ($countedItems items) | OK:$ok OVER:$over SHORT:$shortage"
        }catch(_:Exception){}
    }

    private fun saveAndOverride() {
        try{
        if (!takeStarted || currentTakeName.isEmpty()) {
            Toast.makeText(this, "Enter Stock Take Name and START first", Toast.LENGTH_SHORT).show()
            return
        }
        val hasCount = allEntries.any { it.counted != null }
        if (!hasCount) {
            Toast.makeText(this, "Enter at least one counted qty", Toast.LENGTH_SHORT).show()
            return
        }

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

        Toast.makeText(this, "Saved '$currentTakeName' and OVERRIDED stock - ${allEntries.count { it.counted != null }} items", Toast.LENGTH_LONG).show()
        }catch(e:Exception){ Toast.makeText(this,"Save error: ${e.message}",Toast.LENGTH_LONG).show() }
    }
}