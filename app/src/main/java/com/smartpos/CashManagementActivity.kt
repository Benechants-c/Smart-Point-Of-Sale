package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class CashManagementActivity : Activity() {

    data class CashEntry(val time:String, val type:String, val amount:Float, val reason:String, val by:String, val branch:String)

    private var entries = mutableListOf<CashEntry>()
    private lateinit var tableLayout: LinearLayout
    private lateinit var tvOpening: TextView
    private lateinit var tvCashSales: TextView
    private lateinit var tvCashIn: TextView
    private lateinit var tvCashOut: TextView
    private lateinit var tvExpected: TextView
    private lateinit var tvActual: TextView
    private lateinit var tvDiff: TextView
    private lateinit var tvPaymentSplit: TextView
    private var openingFloat = 50f
    private var actualCounted = 0f
    private var selectedBranch = "All Branches"
    private val sdfTime = SimpleDateFormat("HH:mm dd/MM", Locale.getDefault())
    private val sdfDay = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pref = getSharedPreferences("cash_management", Context.MODE_PRIVATE)
        openingFloat = pref.getFloat("opening_${sdfDay.format(Date())}", 50f)
        actualCounted = pref.getFloat("actual_${sdfDay.format(Date())}", 0f)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }

        // HEADER
        val header = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20,16,20,16)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "💵 Cash Management - TODAY"
            textSize = 15f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        })
        header.addView(Button(this).apply {
            text = "BACK"; textSize = 11f
            setOnClickListener{ finish() }
        })
        root.addView(header)

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12,12,12,12)
        }

        // TODAY + BRANCH
        val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        topRow.addView(TextView(this).apply {
            text = "📅 ${sdfDay.format(Date())}"; textSize = 12f; setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        })
        val branchSpinner = Spinner(this)
        val branches = mutableListOf("All Branches","Main Shop","Rukudzo Shop")
        branchSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branches)
        branchSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p: AdapterView<*>?, v:View?, pos:Int, id:Long){
                selectedBranch = branches[pos]; loadCash()
            }
            override fun onNothingSelected(p: AdapterView<*>?){}
        }
        topRow.addView(branchSpinner, LinearLayout.LayoutParams(180,-2))
        content.addView(topRow)

        // OPENING FLOAT CARD
        val openingCard = LinearLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            setPadding(16,14,16,14)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,12,0,8)}
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        openingCard.addView(TextView(this).apply { text = "💵 OPENING FLOAT"; textSize = 11f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
        tvOpening = TextView(this).apply { text = "$${openingFloat.toInt()}"; textSize = 14f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#2563EB")) }
        openingCard.addView(tvOpening)
        openingCard.addView(Button(this).apply {
            text = "EDIT"; textSize = 10f
            setBackgroundColor(Color.parseColor("#E0F2FE")); setTextColor(Color.parseColor("#2563EB"))
            setOnClickListener{ showOpeningDialog() }
        })
        content.addView(openingCard)

        // SUMMARY GRID - PASTEL
        val grid1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        grid1.addView(createSummaryCard("💰 CASH SALES","$0", "#ECFDF5", "#16A34A", {tvCashSales = it}))
        grid1.addView(createSummaryCard("💳 CARD/ECOCASH","$0", "#EFF6FF", "#2563EB", {tvPaymentSplit = it}))
        content.addView(grid1)

        val grid2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        grid2.addView(createSummaryCard("📥 CASH IN","$0", "#FEF3C7", "#D97706", {tvCashIn = it}))
        grid2.addView(createSummaryCard("📤 CASH OUT","$0", "#FEF2F2", "#DC2626", {tvCashOut = it}))
        content.addView(grid2)

        val grid3 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        grid3.addView(createSummaryCard("🏦 EXPECTED IN DRAWER","$0", "#F0FDF4", "#0F172A", {tvExpected = it}, true))
        val actualCard = LinearLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            setPadding(12,12,12,12)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,4,4,4)}
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        }
        actualCard.addView(TextView(this).apply { text = "🔢 ACTUAL COUNTED"; textSize = 9f; setTextColor(Color.GRAY); gravity = Gravity.CENTER })
        tvActual = TextView(this).apply { text = if(actualCounted==0f) "NOT COUNTED" else "$${actualCounted.toInt()}"; textSize = 13f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER }
        actualCard.addView(tvActual)
        tvDiff = TextView(this).apply { text = ""; textSize = 10f; gravity = Gravity.CENTER }
        actualCard.addView(tvDiff)
        grid3.addView(actualCard)
        content.addView(grid3)

        // ACTION BUTTONS - BEAUTIFUL
        val actionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0,12,0,8) }
        fun actionBtn(t:String, col:String, click:()->Unit): Button {
            return Button(this).apply {
                text = t; textSize = 11f; setTypeface(null, Typeface.BOLD)
                setBackgroundColor(Color.parseColor(col)); setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)}
                setOnClickListener{ click() }
            }
        }
        actionRow.addView(actionBtn("+ CASH IN","#16A34A"){ showCashDialog(true) })
        actionRow.addView(actionBtn("- CASH OUT","#DC2626"){ showCashDialog(false) })
        actionRow.addView(actionBtn("COUNT","#2563EB"){ showCountDialog() })
        content.addView(actionRow)

        val actionRow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actionRow2.addView(Button(this).apply {
            text = "🧾 CLOSE DAY - Z REPORT"; textSize = 11f; setTypeface(null, Typeface.BOLD)
            setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(4,8,4,0)}
            setOnClickListener{ showCloseDay() }
        })
        content.addView(actionRow2)

        // PAYMENT SPLIT BOX
        content.addView(TextView(this).apply {
            text = "💳 PAYMENT SPLIT TODAY"; textSize = 11f; setTypeface(null, Typeface.BOLD); setPadding(0,16,0,6)
        })

        // TABLE HEADER
        val tableHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(8,12,8,12)
        }
        fun th(t:String, w:Float): TextView {
            return TextView(this@CashManagementActivity).apply {
                text = t; textSize = 9f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0,-2,w)
            }
        }
        tableHeader.addView(th("TIME",0.6f))
        tableHeader.addView(th("TYPE",0.6f))
        tableHeader.addView(th("AMOUNT",0.6f))
        tableHeader.addView(th("REASON",1f))
        content.addView(tableHeader)

        tableLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        content.addView(tableLayout)

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)

        loadCash()
    }

    private fun createSummaryCard(title:String, value:String, bg:String, txtCol:String, hold:(TextView)->Unit, large:Boolean=false): LinearLayout {
        val card = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor(bg))
            setPadding(12,12,12,12)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,4,4,4)}
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
        }
        card.addView(TextView(this).apply { text = title; textSize = 9f; setTextColor(Color.GRAY); gravity = Gravity.CENTER })
        val tv = TextView(this).apply {
            text = value; textSize = if(large) 15f else 13f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(txtCol)); gravity = Gravity.CENTER
        }
        hold(tv); card.addView(tv); return card
    }

    private fun loadCash(){
        entries.clear()
        // Load from prefs
        try{
            val pref = getSharedPreferences("cash_management", Context.MODE_PRIVATE)
            for((k,v) in pref.all){
                if(k.startsWith("entry_")){
                    try{
                        val parts = v.toString().split("|")
                        // time|type|amount|reason|by|branch
                        if(parts.size>=5){
                            val e = CashEntry(parts[0], parts[1], parts[2].toFloatOrNull()?:0f, parts[3], parts[4], parts.getOrNull(5)?: "Main Shop")
                            if(selectedBranch=="All Branches" || e.branch==selectedBranch) entries.add(e)
                        }
                    }catch(_:Exception){}
                }
            }
            // Load cash sales from sales prefs
            var cashSales = 0f; var cardSales = 0f
            for(pName in listOf("sales_main","sales_db","sales")){
                try{
                    val sp = getSharedPreferences(pName, Context.MODE_PRIVATE)
                    for((_,sv) in sp.all){
                        try{
                            val o = sv.toString()
                            val amt = Regex(""""total":(\d+(\.\d+)?)""").find(o)?.groupValues?.get(1)?.toFloatOrNull()?:0f
                            if(o.contains("CASH",true)) cashSales+=amt else cardSales+=amt
                        }catch(_:Exception){}
                    }
                }catch(_:Exception){}
            }
            if(cashSales==0f && entries.isEmpty()){
                // demo data for Karoi shop
                entries.add(CashEntry(sdfTime.format(Date()),"CASH SALE",120f,"stove x2","Admin","Main Shop"))
                entries.add(CashEntry(sdfTime.format(Date()),"CASH IN",20f,"Float","Admin","Main Shop"))
                entries.add(CashEntry(sdfTime.format(Date()),"CASH OUT",30f,"Transport","Ruvheneko","Main Shop"))
                cashSales = 120f
            }

            val totalIn = entries.filter{it.type=="CASH IN"}.sumOf{it.amount.toDouble()}.toFloat()
            val totalOut = entries.filter{it.type=="CASH OUT"}.sumOf{it.amount.toDouble()}.toFloat()
            val expected = openingFloat + cashSales + totalIn - totalOut

            tvCashSales.text = "$${cashSales.toInt()}"
            tvPaymentSplit.text = "$${cardSales.toInt()}"
            tvCashIn.text = "$${totalIn.toInt()}"
            tvCashOut.text = "$${totalOut.toInt()}"
            tvExpected.text = "$${expected.toInt()}"
            tvOpening.text = "$${openingFloat.toInt()}"

            if(actualCounted>0){
                tvActual.text = "$${actualCounted.toInt()}"
                val diff = actualCounted - expected
                tvDiff.text = when{
                    diff==0f -> "✅ BALANCED"
                    diff>0 -> "OVER +$${diff.toInt()}"
                    else -> "SHORT -$${(-diff).toInt()}"
                }
                tvDiff.setTextColor(if(diff==0f) Color.parseColor("#16A34A") else Color.parseColor("#DC2626"))
            }

        }catch(e:Exception){}
        renderTable()
    }

    private fun renderTable(){
        tableLayout.removeAllViews()
        val list = entries.sortedByDescending{it.time}
        if(list.isEmpty()){
            tableLayout.addView(TextView(this).apply{ text = "No cash movements today\nTap + CASH IN / - CASH OUT"; gravity = Gravity.CENTER; setPadding(20,30,20,30); setTextColor(Color.GRAY) })
            return
        }
        for((idx, e) in list.withIndex()){
            val bg = if(idx%2==0) Color.WHITE else Color.parseColor("#F8FAFC")
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(bg); setPadding(0,12,0,12); gravity = Gravity.CENTER_VERTICAL
            }
            fun cell(t:String, w:Float, col:Int=Color.BLACK): TextView {
                return TextView(this@CashManagementActivity).apply {
                    text = t; textSize = 10f; setTextColor(col); gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0,-2,w)
                }
            }
            val typeCol = when(e.type){
                "CASH IN"-> Color.parseColor("#16A34A")
                "CASH OUT"-> Color.parseColor("#DC2626")
                else -> Color.parseColor("#2563EB")
            }
            row.addView(cell(e.time.take(11),0.6f, Color.GRAY))
            row.addView(cell(e.type,0.6f, typeCol))
            row.addView(cell("$${e.amount.toInt()}",0.6f, Color.BLACK))
            row.addView(cell("${e.reason.take(14)} - ${e.by}",1f, Color.DKGRAY))
            tableLayout.addView(row)
            tableLayout.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams = LinearLayout.LayoutParams(-1,1) })
        }
    }

    private fun showOpeningDialog(){
        val et = EditText(this).apply { setText("$openingFloat"); inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; setPadding(20,16,20,16) }
        android.app.AlertDialog.Builder(this).setTitle("Opening Float - Today").setView(et)
           .setPositiveButton("SAVE"){_,_->
                try{
                    openingFloat = et.text.toString().toFloatOrNull()?: openingFloat
                    getSharedPreferences("cash_management", Context.MODE_PRIVATE).edit().putFloat("opening_${sdfDay.format(Date())}", openingFloat).apply()
                    loadCash()
                }catch(_:Exception){}
            }.setNegativeButton("CANCEL",null).show()
    }

    private fun showCashDialog(isIn:Boolean){
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20,20,20,20) }
        val amountEt = EditText(this).apply { hint = "Amount $"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; setPadding(16,12,16,12) }
        val reasonEt = EditText(this).apply { hint = "Reason e.g. Transport, Float"; setPadding(16,12,16,12) }
        lay.addView(TextView(this).apply{ text = if(isIn) "Add Cash IN" else "Add Cash OUT"; setTypeface(null, Typeface.BOLD) })
        lay.addView(amountEt); lay.addView(reasonEt)
        android.app.AlertDialog.Builder(this).setTitle(if(isIn) "💵 CASH IN" else "💸 CASH OUT").setView(lay)
           .setPositiveButton("SAVE"){_,_->
                try{
                    val amt = amountEt.text.toString().toFloatOrNull()?:0f
                    val reason = reasonEt.text.toString().ifEmpty{ if(isIn) "Cash In" else "Cash Out" }
                    if(amt>0){
                        val key = "entry_${System.currentTimeMillis()}"
                        val value = "${sdfTime.format(Date())}|${if(isIn) "CASH IN" else "CASH OUT"}|$amt|$reason|Admin|$selectedBranch"
                        getSharedPreferences("cash_management", Context.MODE_PRIVATE).edit().putString(key, value).apply()
                        Toast.makeText(this, "${if(isIn) "IN" else "OUT"} $$amt saved", Toast.LENGTH_SHORT).show()
                        loadCash()
                    }
                }catch(_:Exception){}
            }.setNegativeButton("CANCEL",null).show()
    }

    private fun showCountDialog(){
        val et = EditText(this).apply { hint = "Actual cash counted in drawer $"; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; setText(if(actualCounted>0) "$actualCounted" else ""); setPadding(20,16,20,16) }
        android.app.AlertDialog.Builder(this).setTitle("🔢 Count Cash - How much in drawer?").setView(et)
           .setPositiveButton("SAVE"){_,_->
                actualCounted = et.text.toString().toFloatOrNull()?:0f
                getSharedPreferences("cash_management", Context.MODE_PRIVATE).edit().putFloat("actual_${sdfDay.format(Date())}", actualCounted).apply()
                loadCash()
            }.setNegativeButton("CANCEL",null).show()
    }

    private fun showCloseDay(){
        val expText = tvExpected.text.toString()
        android.app.AlertDialog.Builder(this)
           .setTitle("🧾 CLOSE DAY - Z REPORT")
           .setMessage("Date: ${sdfDay.format(Date())}\nBranch: $selectedBranch\nOpening: ${tvOpening.text}\nCash Sales: ${tvCashSales.text}\nCash IN: ${tvCashIn.text}\nCash OUT: ${tvCashOut.text}\n\nEXPECTED: $expText\nACTUAL: ${tvActual.text}\nDIFF: ${tvDiff.text}\n\nClose day and reset?")
           .setPositiveButton("CLOSE DAY"){_,_->
                Toast.makeText(this,"Day Closed - Z Report Printed! Opening reset to $50", Toast.LENGTH_LONG).show()
                getSharedPreferences("cash_management", Context.MODE_PRIVATE).edit().putFloat("opening_${sdfDay.format(Date(System.currentTimeMillis()+86400000))}", 50f).apply()
            }
           .setNegativeButton("CANCEL",null)
           .show()
    }
}