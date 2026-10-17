package com.smartpos
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

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

        root.addView(TextView(this).apply {
            text = "SMART POS - Reports & Analytics"
            setBackgroundColor(Color.parseColor("#111827"))
            setTextColor(Color.WHITE)
            setPadding(16, 14, 16, 14)
            setTypeface(null, Typeface.BOLD)
        })
        root.addView(Button(this).apply { text = "BACK"; setOnClickListener { finish() } })

        val scroll = ScrollView(this)
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(8, 8, 8, 8)

        // Branch
        val branchCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(12, 8, 12, 8) }
        branchCard.addView(TextView(this).apply { text = "Branch"; setTextColor(Color.GRAY); textSize = 12f })
        spinnerBranch = Spinner(this)
        branchCard.addView(spinnerBranch)
        content.addView(branchCard)

        // Dates
        val dateRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        btnFrom = Button(this).apply { text = "FROM: SELECT DATE"; textSize = 11f; setOnClickListener { pickDate(true) } }
        btnTo = Button(this).apply { text = "TO: SELECT DATE"; textSize = 11f; setOnClickListener { pickDate(false) } }
        dateRow.addView(btnFrom, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0, 0, 4, 0) })
        dateRow.addView(btnTo, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(4, 0, 0, 0) })
        content.addView(dateRow)

        // Product
        val prodCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(12, 8, 12, 8) }
        prodCard.addView(TextView(this).apply { text = "Product"; setTextColor(Color.GRAY); textSize = 12f })
        spinnerProduct = Spinner(this)
        prodCard.addView(spinnerProduct)
        content.addView(prodCard)

        // 3 Cards
        val cardsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val c1 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(8, 12, 8, 12); layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(2, 2, 2, 2) } }
        tvSales = TextView(this).apply { text = "$ 0.0"; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER }
        tvSalesSub = TextView(this).apply { text = "No data"; textSize = 10f; gravity = Gravity.CENTER; setTextColor(Color.RED) }
        c1.addView(TextView(this).apply { text = "Total Sales"; textSize = 10f; gravity = Gravity.CENTER })
        c1.addView(tvSales); c1.addView(tvSalesSub)

        val c2 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(8, 12, 8, 12); layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(2, 2, 2, 2) } }
        tvProfit = TextView(this).apply { text = "$ 0.0"; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER }
        tvProfitSub = TextView(this).apply { text = "No data"; textSize = 10f; gravity = Gravity.CENTER; setTextColor(Color.RED) }
        c2.addView(TextView(this).apply { text = "Total Profit"; textSize = 10f; gravity = Gravity.CENTER })
        c2.addView(tvProfit); c2.addView(tvProfitSub)

        val c3 = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(8, 12, 8, 12); layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(2, 2, 2, 2) } }
        tvLow = TextView(this).apply { text = "0 items"; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER }
        tvLowSub = TextView(this).apply { text = "All OK"; textSize = 10f; gravity = Gravity.CENTER; setTextColor(Color.GRAY) }
        c3.addView(TextView(this).apply { text = "Low Stock"; textSize = 10f; gravity = Gravity.CENTER })
        c3.addView(tvLow); c3.addView(tvLowSub)

        cardsRow.addView(c1); cardsRow.addView(c2); cardsRow.addView(c3)
        content.addView(cardsRow)

        // Daily Sales + Profit side by side - SAME AS YOUR PHOTO
        val middleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val dailyCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(8, 8, 8, 8); layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0, 0, 4, 0) } }
        dailyCard.addView(TextView(this).apply { text = "Daily Sales"; setTypeface(null, Typeface.BOLD); textSize = 12f })
        barContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        dailyCard.addView(barContainer)

        val profitCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(8, 8, 8, 8); layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(4, 0, 0, 0) } }
        profitCard.addView(TextView(this).apply { text = "Profit by Department"; setTypeface(null, Typeface.BOLD); textSize = 12f })
        pieContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        profitCard.addView(pieContainer)

        middleRow.addView(dailyCard); middleRow.addView(profitCard)
        content.addView(middleRow)

        // Recent
        val recentCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(8, 8, 8, 8) }
        recentCard.addView(TextView(this).apply { text = "Recent Transactions - REAL"; setTypeface(null, Typeface.BOLD); textSize = 12f })
        recentContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        recentCard.addView(recentContainer)
        content.addView(recentCard)

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

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
            if (isFrom) { fromDate = cal.time; btnFrom.text = "FROM: ${sdf.format(fromDate!!)}" }
            else { cal.set(Calendar.HOUR_OF_DAY, 23); toDate = cal.time; btnTo.text = "TO: ${sdf.format(cal.time)}" }
            render()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun loadData() {
        allSales.clear()
        for (pn in listOf("sales", "sales_history", "pos_sales", "transactions")) {
            try {
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for ((_, v) in pref.all) {
                    try {
                        val o = JSONObject(v.toString())
                        val product = o.optString("product_name", o.optString("name", ""))
                        if (product.isBlank()) continue
                        val qty = o.optInt("qty", 1)
                        val sell = o.optDouble("selling_price", o.optDouble("price", 0.0)).toFloat()
                        val cost = o.optDouble("cost", 0.0).toFloat()
                        val total = o.optDouble("total", (qty * sell).toDouble()).toFloat()
                        val profit = (sell - cost) * qty
                        val branch = o.optString("branch", "Main Shop")
                        val dept = o.optString("category", "General")
                        val dateStr = o.optString("date", sdfFull.format(Date()))
                        val date = try { sdfFull.parse(dateStr) ?: Date() } catch (_: Exception) { Date() }
                        allSales.add(Sale(date, product, qty, total, profit, branch, dept))
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        lowStockCount = 0
        for (pn in listOf("received_stock", "products", "inventory")) {
            try {
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for ((_, v) in pref.all) {
                    try {
                        val o = JSONObject(v.toString())
                        var q = 0
                        if (o.has("qnty")) q = o.optInt("qnty", 0)
                        else if (o.has("quantity")) q = o.optInt("quantity", 0)
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
            val bOk = selectedBranch == "All Branches" || it.branch == selectedBranch
            val pOk = selectedProduct == "All Products" || it.product == selectedProduct
            val dOk = (fromDate == null || !it.date.before(fromDate)) && (toDate == null || !it.date.after(toDate))
            bOk && pOk && dOk
        }.toMutableList()

        val totalSales = filteredSales.sumOf { it.total.toDouble() }.toFloat()
        val totalProfit = filteredSales.sumOf { it.profit.toDouble() }.toFloat()
        tvSales.text = "$ ${String.format("%.1f", totalSales)}"
        tvSalesSub.text = if (filteredSales.isEmpty()) "No data" else "${filteredSales.size} sales"
        tvProfit.text = "$ ${String.format("%.1f", totalProfit)}"
        tvProfitSub.text = if (filteredSales.isEmpty()) "No data" else "Profit"
        tvLow.text = "$lowStockCount items"
        tvLowSub.text = if (lowStockCount == 0) "All OK" else "Low!"

        // BAR CHART - using simple Views (no Canvas) - GREEN SAFE
        barContainer.removeAllViews()
        val daily = filteredSales.groupBy { sdf.format(it.date) }.mapValues { it.value.sumOf { s -> s.total.toDouble() }.toFloat() }.toList().takeLast(5)
        if (daily.isEmpty()) {
            barContainer.addView(TextView(this).apply { text = "No sales yet\nMake a sale to see bars"; setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(0, 20, 0, 20) })
        } else {
            val max = daily.maxOfOrNull { it.second } ?: 1f
            for ((day, amt) in daily) {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, 4, 0, 4) }
                row.addView(TextView(this).apply { text = day; textSize = 9f; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
                val barLen = ((amt / max) * 20).toInt().coerceAtLeast(1)
                row.addView(TextView(this).apply {
                    text = "█".repeat(barLen) + " $${amt.toInt()}"
                    textSize = 10f
                    setTextColor(Color.parseColor("#2563EB"))
                    layoutParams = LinearLayout.LayoutParams(0, -2, 1.5f)
                })
                barContainer.addView(row)
            }
        }

        // PIE CHART - using dots + % (GREEN SAFE, no Canvas)
        pieContainer.removeAllViews()
        val dept = filteredSales.groupBy { it.dept }.mapValues { it.value.sumOf { s -> s.profit.toDouble() }.toFloat() }.toList()
        if (dept.isEmpty()) {
            pieContainer.addView(TextView(this).apply { text = "No profit data"; setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(0, 20, 0, 20) })
        } else {
            val totalDept = dept.sumOf { it.second.toDouble() }.toFloat()
            val colors = listOf("#2563EB", "#16A34A", "#F59E0B", "#EF4444", "#8B5CF6")
            for (i in dept.indices) {
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 4, 0, 4); gravity = Gravity.CENTER_VERTICAL }
                val dot = View(this).apply { layoutParams = LinearLayout.LayoutParams(12, 12).apply { setMargins(0, 0, 6, 0) }; setBackgroundColor(Color.parseColor(colors[i % colors.size])) }
                row.addView(dot)
                val perc = if (totalDept > 0) (dept[i].second / totalDept * 100).toInt() else 0
                row.addView(TextView(this).apply { text = "${dept[i].first} ${perc}% $${dept[i].second.toInt()}"; textSize = 10f })
                pieContainer.addView(row)
            }
        }

        recentContainer.removeAllViews()
        if (filteredSales.isEmpty()) {
            recentContainer.addView(TextView(this).apply { text = "No transactions yet"; setTextColor(Color.GRAY); setPadding(8, 8, 8, 8) })
        } else {
            for (s in filteredSales.take(10)) {
                val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(6, 8, 6, 8); setBackgroundColor(Color.parseColor("#F9FAFB")) }
                r.addView(TextView(this).apply { text = sdf.format(s.date); textSize = 9f; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
                r.addView(TextView(this).apply { text = "${s.product} x${s.qty}"; textSize = 10f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
                r.addView(TextView(this).apply { text = "$${s.total}"; textSize = 10f })
                recentContainer.addView(r)
            }
        }
    }
}