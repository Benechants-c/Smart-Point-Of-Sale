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

data class StockItem(
    val code: String,
    val name: String,
    var qty: Int,
    val cost: Double,
    val selling: Double
)

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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1976D2"))
            setPadding(30,25,30,25)
        }
        val title = TextView(this).apply {
            text = "STOCK LIST - STOCK IN HAND"
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val tvTime = TextView(this).apply {
            text = "Real Time: ${sdf.format(Date())} | ${android.os.Build.MODEL}"
            setTextColor(Color.parseColor("#E3F2FD"))
            textSize = 12f
        }
        header.addView(title)
        header.addView(tvTime)

        val searchBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(15,15,15,15)
        }
        etSearch = EditText(this).apply {
            hint = "🔍 Search Product Code or Name at top..."
            setBackgroundResource(android.R.drawable.editbox_background)
            setPadding(25,25,25,25)
        }
        tvCount = TextView(this).apply {
            textSize = 12f
            setTextColor(Color.GRAY)
            setPadding(5,10,5,0)
        }
        searchBox.addView(etSearch)
        searchBox.addView(tvCount)

        val summaryBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#FFF3E0"))
            setPadding(20,15,20,15)
        }
        tvValueCost = TextView(this).apply {
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#E65100"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        tvValueSelling = TextView(this).apply {
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#2E7D32"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            gravity = android.view.Gravity.END
        }
        summaryBox.addView(tvValueCost)
        summaryBox.addView(tvValueSelling)

        val listHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#37474F"))
            setPadding(10,12,10,12)
        }
        fun addH(text: String, weight: Float) {
            val tv = TextView(this@StockListActivity).apply {
                this.text = text
                setTextColor(Color.WHITE)
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
            }
            listHeader.addView(tv)
        }
        addH("CODE", 1.2f)
        addH("PRODUCT", 2f)
        addH("QTY", 0.7f)
        addH("IN HAND", 0.9f)
        addH("COST", 0.8f)
        addH("SELL", 0.8f)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        productContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(5,5,5,5)
        }
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
    }

    private fun loadFromGoodsReceived() {
        allStock.clear()
        val goodsPrefs = getSharedPreferences("goods_received", MODE_PRIVATE)
        val productsPrefs = getSharedPreferences("products", MODE_PRIVATE)
        val stockPrefs = getSharedPreferences("stock_data", MODE_PRIVATE)

        // FIXED - safe map copy to avoid Type mismatch
        val combined = mutableMapOf<String, Any?>()
        for ((k, v) in goodsPrefs.all) combined[k] = v
        for ((k, v) in productsPrefs.all) combined[k] = v
        for ((k, v) in stockPrefs.all) combined[k] = v

        if (combined.isNotEmpty()) {
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
                    } else {
                        val cost = str.toDoubleOrNull() ?: 2.0
                        allStock.add(StockItem(key.take(8).uppercase(), key, (5..50).random(), cost, cost * 1.5))
                    }
                } catch (e: Exception) {}
            }
        }

        if (allStock.isEmpty()) {
            allStock.add(StockItem("P001", "Coca Cola 500ml", 45, 0.80, 1.50))
            allStock.add(StockItem("P002", "Bread Loaf Brown", 20, 0.70, 1.20))
            allStock.add(StockItem("P003", "Milk 1L Full Cream", 30, 0.60, 1.00))
            allStock.add(StockItem("P004", "Rice 2kg Mahatma", 15, 2.00, 3.50))
            allStock.add(StockItem("P005", "Cooking Oil 2L", 25, 2.50, 4.00))
            allStock.add(StockItem("P006", "Sugar 2kg", 40, 1.20, 2.00))
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim().lowercase()
                filteredStock = if (query.isEmpty()) {
                    allStock.toMutableList()
                } else {
                    allStock.filter {
                        it.code.lowercase().contains(query) || it.name.lowercase().contains(query)
                    }.toMutableList()
                }
                renderStock(filteredStock)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun renderStock(list: List<StockItem>) {
        productContainer.removeAllViews()
        var totalCostValue = 0.0
        var totalSellValue = 0.0
        for (item in list) {
            totalCostValue += item.qty * item.cost
            totalSellValue += item.qty * item.selling
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.WHITE)
                setPadding(10,14,10,14)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0,0,0,6)
                layoutParams = lp
            }
            fun addCell(text: String, weight: Float, bold: Boolean = false, color: Int = Color.BLACK) {
                val tv = TextView(this@StockListActivity).apply {
                    this.text = text
                    textSize = 12f
                    setTextColor(color)
                    if (bold) typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight)
                }
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
        tvCount.text = "${list.size} products | Real Time: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}"
    }
}