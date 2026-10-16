package com.smartpos
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : Activity() {

    data class Sale(val date: Date, val product:String, val code:String, val qty:Int, val cost:Float, val sell:Float, val profit:Float, val total:Float, val branch:String, val dept:String)
    data class StockItem(val code:String, val name:String, val qty:Int, val branch:String)

    var allSales = mutableListOf<Sale>()
    var filteredSales = mutableListOf<Sale>()
    var allStock = mutableListOf<StockItem>()
    lateinit var tvTotalSales: TextView
    lateinit var tvTotalProfit: TextView
    lateinit var tvLowStock: TextView
    lateinit var tvFrom: Button
    lateinit var tvTo: Button
    lateinit var spinnerBranch: Spinner
    lateinit var spinnerProduct: Spinner
    lateinit var containerDaily: LinearLayout
    lateinit var containerDept: LinearLayout
    lateinit var containerRecent: LinearLayout
    lateinit var barChartContainer: LinearLayout
    lateinit var pieChartContainer: LinearLayout
    var fromDate: Date? = null
    var toDate: Date? = null
    var selectedBranch = "All Branches"
    var selectedProduct = "All Products"
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val sdfFull = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    // ===== BAR CHART VIEW =====
    inner class BarChartView(context: Context, val data: List<Pair<String, Float>>) : View(context) {
        val paintBar = Paint().apply{ color=Color.parseColor("#2563EB"); style=Paint.Style.FILL; isAntiAlias=true }
        val paintText = Paint().apply{ color=Color.parseColor("#6B7280"); textSize=18f; isAntiAlias=true }
        val paintValue = Paint().apply{ color=Color.parseColor("#111827"); textSize=20f; typeface=Typeface.DEFAULT_BOLD; isAntiAlias=true }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if(data.isEmpty()){ canvas.drawText("No sales data", 20f, height/2f, paintText); return }
            val max = data.maxOfOrNull{ it.second } ?: 1f
            val barWidth = (width - 80f) / data.size
            val chartHeight = height - 60f

            for((i, pair) in data.withIndex()){
                val x = 40f + i * barWidth
                val barH = (pair.second / max) * chartHeight * 0.7f
                val y = chartHeight - barH + 20f

                // Bar
                canvas.drawRoundRect(x+4f, y, x+barWidth-4f, chartHeight+20f, 8f, 8f, paintBar)
                // Value
                canvas.drawText("$${pair.second.toInt()}", x+4f, y-8f, paintValue)
                // Label
                canvas.drawText(pair.first.take(5), x+4f, chartHeight+40f, paintText)
            }
        }
    }

    // ===== PIE CHART VIEW =====
    inner class PieChartView(context: Context, val data: List<Pair<String, Float>>) : View(context) {
        val colors = listOf(Color.parseColor("#2563EB"), Color.parseColor("#16A34A"), Color.parseColor("#F59E0B"), Color.parseColor("#EF4444"), Color.parseColor("#8B5CF6"), Color.parseColor("#EC4899"))
        val paintArc = Paint().apply{ style=Paint.Style.FILL; isAntiAlias=true }
        val paintText = Paint().apply{ color=Color.WHITE; textSize=22f; typeface=Typeface.DEFAULT_BOLD; isAntiAlias=true; textAlign=Paint.Align.CENTER }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            if(data.isEmpty() || data.sumOf{ it.second.toDouble() }==0.0){
                paintText.color=Color.GRAY
                canvas.drawText("No dept data", width/2f, height/2f, paintText)
                return
            }
            val total = data.sumOf{ it.second.toDouble() }.toFloat()
            val cx = width/2f
            val cy = height/2f - 10f
            val radius = Math.min(width, height) * 0.38f
            var startAngle = -90f

            for((i, pair) in data.withIndex()){
                val sweep = (pair.second / total) * 360f
                paintArc.color = colors[i % colors.size]
                canvas.drawArc(cx-radius, cy-radius, cx+radius, cy+radius, startAngle, sweep, true, paintArc)

                // Label
                if(sweep>20f){
                    val angle = Math.toRadians((startAngle + sweep/2).toDouble())
                    val lx = cx + Math.cos(angle).toFloat() * radius * 0.6f
                    val ly = cy + Math.sin(angle).toFloat() * radius * 0.6f
                    canvas.drawText("${((pair.second/total)*100).toInt()}%", lx, ly, paintText)
                }
                startAngle += sweep
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F3F4F6"))

        root.addView(LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#111827"))
            setPadding(16,14,16,14)
            addView(TextView(this@ReportsAnalyticsActivity).apply{ text="SMART POS"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=16f })
        })

        root.addView(LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1F2937"))
            setPadding(12,10,12,10)
            addView(TextView(this@ReportsAnalyticsActivity).apply{ text="<- Reports & Analytics - WITH GRAPHS"; setTextColor(Color.WHITE); textSize=13f; setTypeface(null, Typeface.BOLD) })
        })

        root.addView(Button(this).apply {
            text="BACK"; setBackgroundColor(Color.parseColor("#E5E7EB"))
            layoutParams=LinearLayout.LayoutParams(-1,-2).apply{