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

data class StockItem(val code: String, val name: String, var qty: Int, val cost: Double, val selling: Double)

class StockListActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var productContainer: LinearLayout
    private lateinit var tvValueCost: TextView
    private lateinit var tvValueSelling: TextView
    private lateinit var tvCount: TextView
    private val allStock = mutableListOf<StockItem>()
    private var filteredStock = mutableListOf<StockItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try{
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F5F5F5"))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setBackgroundColor(Color.parseColor("#1976D2"))
        header.setPadding(30,25,30,25)
        val title = TextView(this)
        title.text = "STOCK LIST - STOCK IN HAND"
        title.setTextColor(Color.WHITE)
        title.textSize = 18f
        title.setTypeface(null, Typeface.BOLD)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val tvTime = TextView(this)
        tvTime.text = "Real Time: ${sdf.format(Date())}"
        tvTime.setTextColor(Color.parseColor("#E3F2FD"))
        tvTime.textSize = 12f
        header.addView(title)
        header.addView(tvTime)

        val searchBox = LinearLayout(this)
        searchBox.orientation = LinearLayout.VERTICAL
        searchBox.setBackgroundColor(Color.WHITE)
        searchBox.setPadding(15,15,15,15)
        etSearch = EditText(this)
        etSearch.hint = "Search Product Code or Name at top..."
        etSearch.setPadding(25,25,25,25)
        etSearch.setBackgroundColor(Color.WHITE)
        tvCount = TextView(this)
        tvCount.textSize = 12f
        tvCount.setTextColor(Color.GRAY)
        tvCount.setPadding(5,10,5,0)
        searchBox.addView(etSearch)
        searchBox.addView(tvCount)

        val summaryBox = LinearLayout(this)
        summaryBox.orientation = LinearLayout.HORIZONTAL
        summaryBox.setBackgroundColor(Color.parseColor("#FFF3E0"))
        summaryBox.setPadding(20,15,20,15)
        tvValueCost = TextView(this)
        tvValueCost.textSize = 13f
        tvValueCost.setTypeface(null, Typeface.BOLD)
        tvValueCost.setTextColor(Color.parseColor("#E65100"))
        tvValueCost.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        tvValueSelling = TextView(this)
        tvValueSelling.textSize = 13f
        tvValueSelling.setTypeface(null, Typeface.BOLD)
        tvValueSelling.setTextColor(Color.parseColor("#2E7D32"))
        tvValueSelling.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        summaryBox.addView(tvValueCost)
        summaryBox.addView(tvValueSelling)

        val listHeader = LinearLayout(this)
        listHeader.orientation = LinearLayout.HORIZONTAL
        listHeader.setBackgroundColor(Color.parseColor("#37474F"))
        listHeader.setPadding(10,12,10,12)
        fun addH(text: String, weight: Float) {
            val tv = TextView(this@StockListActivity)
            tv.text = text
            tv.setTextColor(Color.WHITE)
            tv.textSize = 11f
            tv.setTypeface(null, Typeface.BOLD)
            tv.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
            listHeader.addView(tv)
        }
        addH("CODE", 1.2f); addH("PRODUCT", 2f); addH("QTY", 0.7f); addH("IN HAND", 0.9f); addH("COST", 0.8f); addH("SELL", 0.8f)

        val scroll = ScrollView(this)
        val lpScroll = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        lpScroll.weight = 1f
        scroll.layoutParams = lpScroll
        productContainer = LinearLayout(this)
        productContainer.orientation = LinearLayout.VERTICAL
        productContainer.setPadding(5,5,5,5)
        scroll.addView(productContainer)

        root.addView(header)
        root.addView(searchBox)
        root.addView(summaryBox)
        root.addView(listHeader)
        root.addView(scroll)

        setContentView(root)

        loadFromGoodsReceived()
        setupSearch()
        renderStock(allStock)
        }catch(e:Exception){ Toast.makeText(this,"StockList init error: ${e.message}",Toast.LENGTH_LONG).show(); finish() }
    }

    private fun loadFromGoodsReceived() {
        try{
        allStock.clear()
        val goodsPrefs = getSharedPreferences("goods_received", MODE_PRIVATE)
        val productsPrefs = getSharedPreferences("products", MODE_PRIVATE)
        val stockPrefs = getSharedPreferences("stock_data", MODE_PRIVATE)

        val combined = mutableMapOf<String, Any?>()
        for ((k, v) in goodsPrefs.all) combined[k] = v
        for ((k, v) in productsPrefs.all) combined[k] = v
        for ((k, v) in stockPrefs.all) combined[k] = v

        for ((key, value) in combined) {
            try {
                val str = value.toString()
                if (str.contains("|")) {
                    val parts = str.split("|")
                    val code = parts.getOrNull(0) ?: key
                    val name = parts.getOrNull(1) ?: key
                    val qty = parts.getOrNull(2)?.toIntOrNull() ?: 10
                    val cost = parts.getOrNull(3)?.toDoubleOrNull() ?: 1.0
                    val sell = parts.getOrNull(4)?.toDoubleOrNull() ?: cost * 1.3
                    allStock.add(StockItem(code, name, qty, cost, sell))
                }
            } catch (_: Exception) {}
        }

        if (allStock.isEmpty()) {
            allStock.add(StockItem("P001", "Coca Cola 500ml", 45, 0.80, 1.50))
            allStock.add(StockItem("P002", "Bread Loaf Brown", 20, 0.70, 1.20))
            allStock.add(StockItem("P003", "Milk 1L Full Cream", 30, 0.60, 1.00))
            allStock.add(StockItem("P004", "Rice 2kg Mahatma", 15, 2.00, 3.50))
        }
        }catch(_:Exception){ allStock.add(StockItem("P001", "Coca Cola 500ml", 45, 0.80, 1.50)) }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try{
                val query = s.toString().trim().lowercase()
                filteredStock = if (query.isEmpty()) allStock.toMutableList() else allStock.filter {
                    it.code.lowercase().contains(query) || it.name.lowercase().contains(query)
                }.toMutableList()
                renderStock(filteredStock)
                }catch(_:Exception){}
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun renderStock(list: List<StockItem>) {
        try{
        productContainer.removeAllViews()
        var totalCostValue = 0.0
        var totalSellValue = 0.0
        for (item in list) {
            totalCostValue += item.qty * item.cost
            totalSellValue += item.qty * item.selling
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setBackgroundColor(Color.WHITE)
            row.setPadding(10,14,10,14)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0,0,0,6)
            row.layoutParams = lp
            fun addCell(text: String, weight: Float, bold: Boolean = false, color: Int = Color.BLACK) {
                val tv = TextView(this@StockListActivity)
                tv.text = text
                tv.textSize = 12f
                tv.setTextColor(color)
                if (bold) tv.setTypeface(null, Typeface.BOLD)
                tv.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
                row.addView(tv)
            }
            addCell(item.code, 1.2f, true, Color.parseColor("#1976D2"))
            addCell(item.name, 2f)
            addCell("${item.qty}", 0.7f, true)
            addCell("${item.qty}", 0.9f, true, Color.parseColor("#2E7D32"))
            addCell("$${item.cost}", 0.8f)
            addCell("$${item.selling}", 0.8f, true)
            productContainer.addView(row)
        }
        tvValueCost.text = "Value at COST: $${String.format("%.2f", totalCostValue)}"
        tvValueSelling.text = "Value at SELLING: $${String.format("%.2f", totalSellValue)}"
        tvCount.text = "${list.size} products | ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
        }catch(_:Exception){}
    }
}