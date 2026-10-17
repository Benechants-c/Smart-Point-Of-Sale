package com.smartpos
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

// PIE CHART - Canvas - No library
class SimplePieChart(context: Context, val data: List<Pair<String, Float>>) : View(context) {
    private val colors = intArrayOf(
        Color.parseColor("#2563EB"),
        Color.parseColor("#16A34A"),
        Color.parseColor("#F59E0B"),
        Color.parseColor("#EF4444"),
        Color.parseColor("#8B5CF6"),
        Color.parseColor("#EC4899")
    )
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val textPaint = Paint().apply { isAntiAlias = true; color = Color.WHITE; textSize = 24f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return
        val total = data.sumOf { it.second.toDouble() }.toFloat()
        if (total == 0f) return
        val cx = width / 2f
        val cy = height / 2f
        val r = Math.min(width, height) * 0.38f
        var start = -90f
        for (i in data.indices) {
            val sweep = data[i].second / total * 360f
            paint.color = colors[i % colors.size]
            canvas.drawArc(cx - r, cy - r, cx + r, cy + r, start, sweep, true, paint)
            start += sweep
        }
    }
}

class ReportsActivity : Activity() {

    data class Sale(val date: Date, val product: String, val qty: Int, val total: Float, val profit: Float, val branch: String, val dept: String)

    private var allSales = mutableListOf<Sale>()
    private var filteredSales = mutableListOf<Sale>()
    private var lowStockCount = 0
    private lateinit var tvSales: TextView
    private lateinit var tvSalesSub: TextView
    private lateinit var tvProfit: TextView
    private lateinit var tvProfitSub: TextView
    private lateinit var tvLow: TextView
    private lateinit var tvLowSub: TextView
    private lateinit var barContainer: LinearLayout
    private lateinit var pieContainer: LinearLayout
    private lateinit var pieLegend: LinearLayout
    private lateinit var dailyLegend: LinearLayout
    private lateinit var recentContainer: LinearLayout
    private lateinit var btnFrom: Button
    private lateinit var btnTo: Button
    private lateinit var spinnerBranch: Spinner
    private lateinit var spinnerProduct: Spinner

    private var fromDate: Date? = null
    private var toDate: Date? = null
    private var selectedBranch = "All Branches"
    private var selectedProduct = "All Products"

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val sdfFull = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F3F4F6"))

        // Top bar
        root.addView(TextView(this).apply {
            text = "SMART POS"
            setBackgroundColor(Color.parseColor("#111827"))
            setTextColor(Color.WHITE)
            setPadding(16, 14, 16, 14)
            setTypeface(null, Typeface.BOLD)
            textSize = 16f
        })
        root.addView(TextView(this).apply {
            text = "<- Reports & Analytics"
            setBackgroundColor(Color.parseColor("#1F2937"))
            setTextColor(Color.WHITE)
            setPadding(16, 10, 16, 10)
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
        })
        root.addView(Button(this).apply {
            text = "BACK"
            setBackgroundColor(Color.parseColor("#E5E7EB"))
            setOnClickListener { finish() }
        })

        val scroll = ScrollView(this)
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(8, 8, 8, 8)

        // Branch - KEEP SELECT BRANCH
        val branchCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(12, 8, 12, 8)
        }
        branchCard.addView(TextView(this).apply { text = "Branch"; setTextColor(Color.GRAY); textSize = 12f; setTypeface(null, Typeface.BOLD) })
        spinnerBranch = Spinner(this)
        branchCard.addView(spinnerBranch)
        content.addView(branchCard)

        // FROM TO - KEEP SELECT DATE
        val dateRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        btnFrom = Button(this).apply { text = "FROM: SELECT DATE"; textSize = 11f; setOnClickListener { pickDate(true) } }
        btnTo = Button(this).apply { text = "TO: SELECT DATE"; textSize = 11f; setOnClickListener { pickDate(false) } }
        dateRow.addView(btnFrom, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0, 0, 4, 0) })
        dateRow.addView(btnTo, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(4, 0, 0, 0) })
        content.addView(dateRow)

        // Product - KEEP SELECT PRODUCT
        val prodCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(12, 8, 12, 8)
        }
        prodCard.addView(TextView(this).apply { text = "Product"; setTextColor(Color.GRAY); textSize = 12f; setTypeface(null, Typeface.BOLD) })
        spinnerProduct = Spinner(this)
        prodCard.addView(spinnerProduct)
        content.addView(prodCard)

        // 3 Cards - KEEP Total Sales, Total Profit, Low Stock
        val cardsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun makeCard(title: String): LinearLayout {
            val c = LinearLayout(this@ReportsActivity)
            c.orientation = LinearLayout.VERTICAL
            c.setBackgroundColor(Color.WHITE)
            c.gravity = Gravity.CENTER
            c.setPadding(8, 12, 8, 12)
            c.layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(3, 3, 3, 3) }
            c.addView(TextView(this@ReportsActivity).apply { text = title; textSize = 10f; gravity = Gravity.CENTER; setTextColor(Color.GRAY) })
            val tvMain = TextView(this@ReportsActivity).apply { text = "$ 0.0"; textSize = 16f; gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD) }
            val tvSub = TextView(this@ReportsActivity).apply { text = "No data"; textSize = 9f; gravity = Gravity.CENTER; setTextColor(Color.RED) }
            c.addView(tvMain)
            c.addView(tvSub)
            c.tag = Pair(tvMain, tvSub)
            return c
        }
        val cardSales = makeCard("Total Sales")
        val cardProfit = makeCard("Total Profit")
        val cardLow = makeCard("Low Stock")
        tvSales = (cardSales.tag as Pair<TextView, TextView>).first
        tvSalesSub = (cardSales.tag as Pair<TextView, TextView>).second
        tvProfit = (cardProfit.tag as Pair<TextView, TextView>).first
        tvProfitSub = (cardProfit.tag as Pair<TextView, TextView>).second
        tvLow = (cardLow.tag as Pair<TextView, TextView>).first
        tvLowSub = (cardLow.tag as Pair<TextView, TextView>).second
        cardsRow.addView(cardSales)
        cardsRow.addView(cardProfit)
        cardsRow.addView(cardLow)
        content.addView(cardsRow)

        // DAILY SALES + PROFIT SIDE BY SIDE - SAME AS SCREENSHOT
        val middleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val dailyCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0, 0, 4, 0) }
        }
        dailyCard.addView(TextView(this).apply { text = "Daily Sales"; setTypeface(null, Typeface.BOLD); textSize = 12f })
        barContainer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM; layoutParams = LinearLayout.LayoutParams(-1, 180) }
        dailyCard.addView(barContainer)
        dailyLegend = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        dailyCard.addView(dailyLegend)

        val profitCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(4, 0, 0, 0) }
        }
        profitCard.addView(TextView(this).apply { text = "Profit by Department"; setTypeface(null, Typeface.BOLD); textSize = 12f })
        pieContainer = LinearLayout(this).apply { layoutParams = LinearLayout.LayoutParams(-1, 180); gravity = Gravity.CENTER }
        profitCard.addView(pieContainer)
        pieLegend = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        profitCard.addView(pieLegend)

        middleRow.addView(dailyCard)
        middleRow.addView(profitCard)
        content.addView(middleRow)

        // Recent - KEEP
        val recentCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(8, 8, 8, 8)
        }
        recentCard.addView(TextView(this).apply { text = "Recent Transactions - REAL"; setTypeface(null, Typeface.BOLD); textSize = 12f })
        recentContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        recentCard.addView(recentContainer)
        content.addView(recentCard)

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        // Init dates
        val cal = Calendar.getInstance()
        toDate = cal.time
        btnTo.text = "TO: ${sdf.format(toDate!!)}"
        cal.add(Calendar.DAY_OF_MONTH, -30)
        fromDate = cal.time
        btnFrom.text = "FROM: ${sdf.format(fromDate!!)}"

        loadData()
        setupSpinners()
        render()
    }

    private fun pickDate(isFrom: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            if (isFrom) {
                fromDate = cal.time
                btnFrom.text = "FROM: ${sdf.format(fromDate!!)}"
            } else {
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                toDate = cal.time
                btnTo.text = "TO: ${sdf.format(cal.time)}"
            }
            render()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadData() {
        allSales.clear()
        // Read ALL possible sales prefs
        for (pn in listOf("sales", "sales_history", "pos_sales", "transactions", "sales_list", "invoices", "receipts", "sales_data")) {
            try {
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for ((_, v) in pref.all) {
                    try {
                        val raw = v.toString()
                        if (!raw.trim().startsWith("{")) continue
                        val o = JSONObject(raw)
                        val product = o.optString("product_name", o.optString("name", o.optString("product", "")))
                        if (product.isBlank()) continue
                        val qty = o.optInt("qty", o.optInt("quantity", 1))
                        val sell = o.optDouble("selling_price", o.optDouble("price", 0.0)).toFloat()
                        val cost = o.optDouble("cost", o.optDouble("buying_price", 0.0)).toFloat()
                        val total = o.optDouble("total", (qty * sell).toDouble()).toFloat()
                        val profit = (sell - cost) * qty
                        val branch = o.optString("branch", "Main Shop")
                        val dept = o.optString("department", o.optString("category", "General"))
                        val dateStr = o.optString("date", sdfFull.format(Date()))
                        val date = try { sdfFull.parse(dateStr) ?: sdf.parse(dateStr) ?: Date() } catch (_: Exception) { Date() }
                        allSales.add(Sale(date, product, qty, total, profit, branch, dept))
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        // Low stock count
        lowStockCount = 0
        for (pn in listOf("received_stock", "products", "inventory", "stock")) {
            try {
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for ((_, v) in pref.all) {
                    try {
                        val o = JSONObject(v.toString())
                        var q = 0
                        if (o.has("qnty")) q = o.optInt("qnty", 0)
                        else if (o.has("quantity")) q = o.optInt("quantity", 0)
                        else if (o.has("qty")) q = o.optInt("qty", 0)
                        if (q in 1..4) lowStockCount++
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        allSales.sortByDescending { it.date }
    }

    private fun setupSpinners() {
        val branches = mutableListOf("All Branches")
        branches.addAll(allSales.map { it.branch }.distinct())
        try {
            val p = getSharedPreferences("shops", Context.MODE_PRIVATE)
            for ((_, v) in p.all) {
                try {
                    val o = JSONObject(v.toString())
                    val n = o.optString("name", "")
                    if (n.isNotBlank() && !branches.contains(n)) branches.add(n)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}

        spinnerBranch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branches.distinct())
        spinnerBranch.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(a: AdapterView<*>?, v: View?, p: Int, id: Long) { selectedBranch = branches[p]; render() }
            override fun onNothingSelected(a: AdapterView<*>?) {}
        }

        val products = mutableListOf("All Products")
        products.addAll(allSales.map { it.product }.distinct())
        spinnerProduct.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, products.distinct())
        spinnerProduct.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(a: AdapterView<*>?, v: View?, p: Int, id: Long) { selectedProduct = products[p]; render() }
            override fun onNothingSelected(a: AdapterView<*>?) {}
        }
    }

    private fun render() {
        filteredSales = allSales.filter {
            val bOk = selectedBranch == "All Branches" || it.branch.contains(selectedBranch, true)
            val pOk = selectedProduct == "All Products" || it.product == selectedProduct
            val dOk = (fromDate == null || !it.date.before(fromDate)) && (toDate == null || !it.date.after(toDate))
            bOk && pOk && dOk
        }.toMutableList()

        val totalSales = filteredSales.sumOf { it.total.toDouble() }.toFloat()
        val totalProfit = filteredSales.sumOf { it.profit.toDouble() }.toFloat()

        tvSales.text = "$ ${String.format("%.1f", totalSales)}"
        tvSalesSub.text = if (filteredSales.isEmpty()) "No data" else "${filteredSales.size} sales"
        tvSalesSub.setTextColor(if (filteredSales.isEmpty()) Color.RED else Color.parseColor("#16A34A"))

        tvProfit.text = "$ ${String.format("%.1f", totalProfit)}"
        tvProfitSub.text = if (filteredSales.isEmpty()) "No data" else "Profit"
        tvProfitSub.setTextColor(if (filteredSales.isEmpty()) Color.RED else Color.parseColor("#16A34A"))

        tvLow.text = "$lowStockCount items"
        tvLowSub.text = if (lowStockCount == 0) "All OK" else "Low!"
        tvLowSub.setTextColor(if (lowStockCount == 0) Color.parseColor("#16A34A") else Color.RED)

        // DAILY SALES BAR CHART - Bars with Views (always compiles)
        barContainer.removeAllViews()
        dailyLegend.removeAllViews()
        val dailyMap = filteredSales.groupBy { sdf.format(it.date) }.mapValues { it.value.sumOf { s -> s.total.toDouble() }.toFloat() }.toList().sortedBy { try { sdf.parse(it.first) } catch (_: Exception) { Date() } }.takeLast(5)

        if (dailyMap.isEmpty()) {
            barContainer.addView(TextView(this).apply { text = "No sales\nMake a sale to see bars"; gravity = Gravity.CENTER; setTextColor(Color.GRAY); textSize = 10f })
        } else {
            val max = dailyMap.maxOfOrNull { it.second } ?: 1f
            for ((day, amt) in dailyMap) {
                val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; layoutParams = LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(2, 0, 2, 0) } }
                col.addView(TextView(this).apply { text = "$${amt.toInt()}"; textSize = 8f; gravity = Gravity.CENTER; setTypeface(null, Typeface.BOLD) })
                val bar = View(this).apply {
                    val h = ((amt / max) * 100).toInt().coerceAtLeast(8)
                    layoutParams = LinearLayout.LayoutParams(24, h).apply { setMargins(0, 4, 0, 4) }
                    setBackgroundColor(Color.parseColor("#2563EB"))
                }
                col.addView(bar)
                col.addView(TextView(this).apply { text = day.take(5); textSize = 7f; gravity = Gravity.CENTER })
                barContainer.addView(col)
            }
        }

        // PIE CHART - Real Canvas Pie + Legend
        pieContainer.removeAllViews()
        pieLegend.removeAllViews()
        val deptMap = filteredSales.groupBy { it.dept }.mapValues { it.value.sumOf { s -> s.profit.toDouble() }.toFloat() }.toList()

        if (deptMap.isEmpty() || deptMap.sumOf { it.second.toDouble() } == 0.0) {
            pieContainer.addView(TextView(this).apply { text = "No profit data"; gravity = Gravity.CENTER; setTextColor(Color.GRAY); textSize = 10f })
        } else {
            pieContainer.addView(SimplePieChart(this, deptMap))
            val colors = listOf("#2563EB", "#16A34A", "#F59E0B", "#EF4444", "#8B5CF6")
            val totalDept = deptMap.sumOf { it.second.toDouble() }.toFloat()
            for ((i, pair) in deptMap.withIndex()) {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 2, 0, 2); gravity = Gravity.CENTER_VERTICAL }
                val dot = View(this).apply { layoutParams = LinearLayout.LayoutParams(10, 10).apply { setMargins(0, 0, 6, 0) }; setBackgroundColor(Color.parseColor(colors[i % colors.size])) }
                row.addView(dot)
                val perc = if (totalDept > 0) (pair.second / totalDept * 100).toInt() else 0
                row.addView(TextView(this).apply { text = "${pair.first} ${perc}% $${pair.second.toInt()}"; textSize = 9f })
                pieLegend.addView(row)
            }
        }

        // Recent
        recentContainer.removeAllViews()
        if (filteredSales.isEmpty()) {
            recentContainer.addView(TextView(this).apply { text = "No transactions yet - Make a sale in POS"; setPadding(0, 8, 0, 8); setTextColor(Color.GRAY); gravity = Gravity.CENTER })
        } else {
            for (s in filteredSales.take(10)) {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(6, 8, 6, 8); setBackgroundColor(Color.parseColor("#F9FAFB")); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 0, 0, 4) } }
                row.addView(TextView(this).apply { text = sdfFull.format(s.date); textSize = 9f; layoutParams = LinearLayout.LayoutParams(0, -2, 1f); setTextColor(Color.GRAY) })
                row.addView(TextView(this).apply { text = "${s.product} x${s.qty}"; textSize = 10f; layoutParams = LinearLayout.LayoutParams(0, -2, 1f); setTypeface(null, Typeface.BOLD) })
                row.addView(TextView(this).apply { text = "$${s.total}"; textSize = 10f; setTypeface(null, Typeface.BOLD) })
                recentContainer.addView(row)
            }
        }
    }
}