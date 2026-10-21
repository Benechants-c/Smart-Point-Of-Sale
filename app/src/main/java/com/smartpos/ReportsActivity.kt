package com.smartpos

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

data class SaleRecord(val date: String, val code: String, val product: String, val qty: Int, val cost: Double, val selling: Double)

class SimpleBarChartView(context: Context, private val label1: String, private val value1: Double, private val label2: String, private val value2: Double) : View(context) {
    private val paintBar1 = Paint().apply { color = Color.parseColor("#4527A0"); style = Paint.Style.FILL; isAntiAlias = true }
    private val paintBar2 = Paint().apply { color = Color.parseColor("#FF6F00"); style = Paint.Style.FILL; isAntiAlias = true }
    private val paintText = Paint().apply { color = Color.BLACK; textSize = 28f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
    private val paintLabel = Paint().apply { color = Color.DKGRAY; textSize = 26f; isAntiAlias = true }

    override fun onDraw(canvas: Canvas) {
        try{
        super.onDraw(canvas)
        if(width==0 || height==0) return
        val maxVal = maxOf(value1, value2, 1.0)
        val w = width.toFloat()
        val h = height.toFloat()
        val barWidth = w * 0.25f
        val space = w * 0.15f
        val bottom = h - 60f
        val topLimit = 60f
        val chartHeight = bottom - topLimit
        val bar1Height = (value1 / maxVal * chartHeight).toFloat()
        canvas.drawRect(space, bottom - bar1Height, space + barWidth, bottom, paintBar1)
        val bar2Left = space + barWidth + space
        val bar2Height = (value2 / maxVal * chartHeight).toFloat()
        canvas.drawRect(bar2Left, bottom - bar2Height, bar2Left + barWidth, bottom, paintBar2)
        canvas.drawText("$${String.format("%.0f", value1)}", space, bottom - bar1Height - 10f, paintText)
        canvas.drawText("$${String.format("%.0f", value2)}", bar2Left, bottom - bar2Height - 10f, paintText)
        canvas.drawText(label1, space, bottom + 35f, paintLabel)
        canvas.drawText(label2, bar2Left, bottom + 35f, paintLabel)
        }catch(_:Exception){}
    }
}

class SimplePieChartView(context: Context, private val data: List<Pair<String, Double>>) : View(context) {
    private val colors = intArrayOf(Color.parseColor("#4527A0"), Color.parseColor("#2E7D32"), Color.parseColor("#FF6F00"), Color.parseColor("#1976D2"), Color.parseColor("#C62828"), Color.parseColor("#6A1B9A"))
    private val paint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val textPaint = Paint().apply { color = Color.WHITE; textSize = 28f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
    override fun onDraw(canvas: Canvas) {
        try{
        super.onDraw(canvas)
        if(data.isEmpty() || width==0 || height==0) return
        val total = data.sumOf { it.second }
        if(total==0.0) return
        val cx = width / 2f
        val cy = height / 2f
        val radius = min(width, height) * 0.38f
        var startAngle = -90f
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        for(i in data.indices){
            val sweep = (data[i].second / total * 360f).toFloat()
            paint.color = colors[i % colors.size]
            canvas.drawArc(rect, startAngle, sweep, true, paint)
            val angle = startAngle + sweep / 2
            val rad = Math.toRadians(angle.toDouble())
            val lx = cx + radius * 0.6f * Math.cos(rad).toFloat()
            val ly = cy + radius * 0.6f * Math.sin(rad).toFloat()
            canvas.drawText("${(data[i].second/total*100).toInt()}%", lx, ly, textPaint)
            startAngle += sweep
        }
        }catch(_:Exception){}
    }
}

class ReportsActivity : AppCompatActivity() {

    private lateinit var container: LinearLayout
    private lateinit var graphContainer: LinearLayout
    private lateinit var tvTotalSales: TextView
    private lateinit var tvTotalCost: TextView
    private lateinit var tvProfit: TextView
    private lateinit var tvCount: TextView
    private lateinit var etSearch: EditText
    private val allRecords = mutableListOf<SaleRecord>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try{
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F5F5F5"))

        val header = LinearLayout(this)
        header.orientation = LinearLayout.VERTICAL
        header.setBackgroundColor(Color.parseColor("#4527A0"))
        header.setPadding(30,25,30,25)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val title = TextView(this)
        title.text = "REPORTS - GRAPHS & ANALYTICS"
        title.setTextColor(Color.WHITE)
        title.textSize = 18f
        title.setTypeface(null, Typeface.BOLD)
        val prefs = getSharedPreferences("pos_prefs", MODE_PRIVATE)
        val cashier = prefs.getString("cashier_name", prefs.getString("logged_user", "Cashier") ?: "Cashier") ?: "Cashier"
        val tvHead = TextView(this)
        tvHead.text = "Cashier: $cashier | Real: ${sdf.format(Date())}"
        tvHead.setTextColor(Color.parseColor("#EDE7F6"))
        tvHead.textSize = 12f
        header.addView(title)
        header.addView(tvHead)

        val searchBox = LinearLayout(this)
        searchBox.orientation = LinearLayout.VERTICAL
        searchBox.setBackgroundColor(Color.WHITE)
        searchBox.setPadding(15,15,15,15)
        etSearch = EditText(this)
        etSearch.hint = "Search date / code / product..."
        etSearch.setPadding(25,25,25,25)
        etSearch.setBackgroundColor(Color.WHITE)
        tvCount = TextView(this)
        tvCount.textSize = 12f
        tvCount.setTextColor(Color.GRAY)
        tvCount.setPadding(5,10,5,0)
        searchBox.addView(etSearch)
        searchBox.addView(tvCount)

        val summary = LinearLayout(this)
        summary.orientation = LinearLayout.VERTICAL
        summary.setBackgroundColor(Color.WHITE)
        summary.setPadding(15,15,15,15)
        val row1 = LinearLayout(this)
        row1.orientation = LinearLayout.HORIZONTAL
        tvTotalSales = TextView(this)
        tvTotalSales.textSize = 14f
        tvTotalSales.setTypeface(null, Typeface.BOLD)
        tvTotalSales.setTextColor(Color.parseColor("#2E7D32"))
        tvTotalSales.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        tvTotalCost = TextView(this)
        tvTotalCost.textSize = 14f
        tvTotalCost.setTypeface(null, Typeface.BOLD)
        tvTotalCost.setTextColor(Color.parseColor("#E65100"))
        tvTotalCost.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row1.addView(tvTotalSales)
        row1.addView(tvTotalCost)
        tvProfit = TextView(this)
        tvProfit.textSize = 15f
        tvProfit.setTypeface(null, Typeface.BOLD)
        tvProfit.setTextColor(Color.parseColor("#4527A0"))
        tvProfit.setPadding(0,10,0,0)
        summary.addView(row1)
        summary.addView(tvProfit)

        graphContainer = LinearLayout(this)
        graphContainer.orientation = LinearLayout.VERTICAL
        graphContainer.setBackgroundColor(Color.WHITE)
        graphContainer.setPadding(15,15,15,15)

        val scroll = ScrollView(this)
        val lpScroll = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        lpScroll.weight = 1f
        scroll.layoutParams = lpScroll
        val scrollInner = LinearLayout(this)
        scrollInner.orientation = LinearLayout.VERTICAL
        container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(5,5,5,5)

        val listHeader = LinearLayout(this)
        listHeader.orientation = LinearLayout.HORIZONTAL
        listHeader.setBackgroundColor(Color.parseColor("#37474F"))
        listHeader.setPadding(8,12,8,12)
        fun addH(t: String, w: Float) {
            val tv = TextView(this@ReportsActivity)
            tv.text = t; tv.setTextColor(Color.WHITE); tv.textSize = 10f; tv.setTypeface(null,Typeface.BOLD)
            tv.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w)
            listHeader.addView(tv)
        }
        addH("DATE", 1.3f); addH("CODE", 0.8f); addH("PRODUCT", 1.5f); addH("QTY", 0.5f); addH("COST", 0.9f); addH("SELL", 0.9f)

        scrollInner.addView(graphContainer)
        scrollInner.addView(listHeader)
        scrollInner.addView(container)
        scroll.addView(scrollInner)

        root.addView(header)
        root.addView(searchBox)
        root.addView(summary)
        root.addView(scroll)
        setContentView(root)

        loadSales()
        render(allRecords)
        drawGraphs()

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try{
                val q = s.toString().trim().lowercase()
                val filtered = if (q.isEmpty()) allRecords else allRecords.filter {
                    it.date.lowercase().contains(q) || it.code.lowercase().contains(q) || it.product.lowercase().contains(q)
                }
                render(filtered)
                }catch(_:Exception){}
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        }catch(e:Exception){ Toast.makeText(this,"Reports init error: ${e.message}",Toast.LENGTH_LONG).show(); finish() }
    }

    private fun loadSales() {
        try{
        allRecords.clear()
        val salesPrefs = getSharedPreferences("sales_history", MODE_PRIVATE)
        for ((key, value) in salesPrefs.all) {
            try {
                val str = value.toString()
                if (str.contains("|")) {
                    val p = str.split("|")
                    val date = p.getOrNull(0) ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val code = p.getOrNull(1) ?: key.take(6)
                    val prod = p.getOrNull(2) ?: key
                    val qty = p.getOrNull(3)?.toIntOrNull() ?: 1
                    val cost = p.getOrNull(4)?.toDoubleOrNull() ?: 1.0
                    val sell = p.getOrNull(5)?.toDoubleOrNull() ?: cost * 1.5
                    allRecords.add(SaleRecord(date, code, prod, qty, cost, sell))
                }
            } catch (_: Exception) {}
        }
        if (allRecords.isEmpty()) {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            val today = fmt.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yest = fmt.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -6)
            val lastWeek = fmt.format(cal.time)
            cal.add(Calendar.MONTH, -1)
            val lastMonth = fmt.format(cal.time)
            allRecords.add(SaleRecord(today, "P001", "Coca Cola", 15, 0.80, 1.50))
            allRecords.add(SaleRecord(today, "P002", "Bread", 8, 0.70, 1.20))
            allRecords.add(SaleRecord(yest, "P003", "Milk", 20, 0.60, 1.00))
            allRecords.add(SaleRecord(lastWeek, "P004", "Rice 2kg", 10, 2.00, 3.50))
            allRecords.add(SaleRecord(lastMonth, "P005", "Oil 2L", 12, 2.50, 4.00))
        }
        }catch(_:Exception){}
    }

    private fun drawGraphs() {
        try{
        graphContainer.removeAllViews()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = fmt.format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yestStr = fmt.format(cal.time)
        cal.time = Date()
        cal.add(Calendar.WEEK_OF_YEAR, -1)
        val lastWeekNum = cal.get(Calendar.WEEK_OF_YEAR)
        val thisWeekNum = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
        cal.time = Date()
        cal.add(Calendar.MONTH, -1)
        val lastMonth = cal.get(Calendar.MONTH)
        val thisMonth = Calendar.getInstance().get(Calendar.MONTH)

        fun sumFor(predicate: (SaleRecord) -> Boolean): Double { return allRecords.filter(predicate).sumOf { it.qty * it.selling } }

        val todayTotal = sumFor { it.date == todayStr }
        val yestTotal = sumFor { it.date == yestStr }
        val thisMonthTotal = sumFor { try { fmt.parse(it.date)?.let { d -> Calendar.getInstance().apply { time = d }.get(Calendar.MONTH) == thisMonth } ?: false } catch (_: Exception) { false } }
        val lastMonthTotal = sumFor { try { fmt.parse(it.date)?.let { d -> Calendar.getInstance().apply { time = d }.get(Calendar.MONTH) == lastMonth } ?: false } catch (_: Exception) { false } }
        val thisWeekTotal = sumFor { try { fmt.parse(it.date)?.let { d -> Calendar.getInstance().apply { time = d }.get(Calendar.WEEK_OF_YEAR) == thisWeekNum } ?: false } catch (_: Exception) { false } }
        val lastWeekTotal = sumFor { try { fmt.parse(it.date)?.let { d -> Calendar.getInstance().apply { time = d }.get(Calendar.WEEK_OF_YEAR) == lastWeekNum } ?: false } catch (_: Exception) { false } }

        fun addGraphTitle(t: String) {
            val tv = TextView(this)
            tv.text = t; tv.textSize = 14f; tv.setTypeface(null,Typeface.BOLD); tv.setTextColor(Color.BLACK); tv.setPadding(0,20,0,10)
            graphContainer.addView(tv)
        }

        addGraphTitle("Today vs Yesterday")
        graphContainer.addView(SimpleBarChartView(this, "Today", todayTotal, "Yesterday", yestTotal).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300) })
        addGraphTitle("This Week vs Last Week")
        graphContainer.addView(SimpleBarChartView(this, "This Week", thisWeekTotal, "Last Week", lastWeekTotal).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300) })
        addGraphTitle("This Month vs Last Month")
        graphContainer.addView(SimpleBarChartView(this, "This Month", thisMonthTotal, "Last Month", lastMonthTotal).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300) })

        addGraphTitle("Sales by Product (Pie %)")
        val byProduct = allRecords.groupBy { it.product }.map { it.key to it.value.sumOf { r -> r.qty * r.selling } }.sortedByDescending { it.second }.take(6)
        graphContainer.addView(SimplePieChartView(this, byProduct).apply { layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500) })

        val legend = LinearLayout(this)
        legend.orientation = LinearLayout.VERTICAL
        legend.setPadding(0,10,0,10)
        val pieColors = intArrayOf(Color.parseColor("#4527A0"), Color.parseColor("#2E7D32"), Color.parseColor("#FF6F00"), Color.parseColor("#1976D2"), Color.parseColor("#C62828"), Color.parseColor("#6A1B9A"))
        for (i in byProduct.indices) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(0,4,0,4)
            val colorBox = View(this)
            colorBox.setBackgroundColor(pieColors[i % pieColors.size])
            colorBox.layoutParams = LinearLayout.LayoutParams(30,30).apply { setMargins(0,0,15,0) }
            val tv = TextView(this)
            tv.text = "${byProduct[i].first}: $${String.format("%.2f", byProduct[i].second)}"
            tv.textSize = 12f
            row.addView(colorBox); row.addView(tv); legend.addView(row)
        }
        graphContainer.addView(legend)
        }catch(e:Exception){ Toast.makeText(this,"Graph error: ${e.message}",Toast.LENGTH_SHORT).show() }
    }

    private fun render(list: List<SaleRecord>) {
        try{
        container.removeAllViews()
        var totalCost = 0.0; var totalSell = 0.0
        for (r in list) {
            val costVal = r.qty * r.cost; val sellVal = r.qty * r.selling
            totalCost += costVal; totalSell += sellVal
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setBackgroundColor(Color.WHITE)
            row.setPadding(8,12,8,12)
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.setMargins(0,0,0,5)
            row.layoutParams = lp
            fun cell(txt: String, w: Float, bold: Boolean = false, color: Int = Color.BLACK) {
                val tv = TextView(this@ReportsActivity)
                tv.text = txt; tv.textSize = 11f; tv.setTextColor(color)
                if(bold) tv.setTypeface(null, Typeface.BOLD)
                tv.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w)
                row.addView(tv)
            }
            cell(r.date, 1.3f); cell(r.code, 0.8f, true, Color.parseColor("#4527A0")); cell(r.product, 1.5f)
            cell("${r.qty}", 0.5f, true); cell("$${String.format("%.2f", costVal)}", 0.9f); cell("$${String.format("%.2f", sellVal)}", 0.9f, true, Color.parseColor("#2E7D32"))
            container.addView(row)
        }
        val profit = totalSell - totalCost
        tvTotalSales.text = "Selling: $${String.format("%.2f", totalSell)}"
        tvTotalCost.text = "Cost: $${String.format("%.2f", totalCost)}"
        tvProfit.text = "PROFIT: $${String.format("%.2f", profit)}"
        tvCount.text = "${list.size} records"
        }catch(_:Exception){}
    }
}