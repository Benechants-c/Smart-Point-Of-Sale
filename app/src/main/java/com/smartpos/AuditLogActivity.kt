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
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AuditLogActivity : Activity() {

    data class Log(
        val date: Date,
        val dateStr: String,
        val type: String,
        val product: String,
        val code: String,
        val qty: Int,
        val total: Float,
        val branch: String,
        val id: String
    )

    var allLogs = mutableListOf<Log>()
    var filteredLogs = mutableListOf<Log>()
    lateinit var table: LinearLayout
    lateinit var tvFrom: TextView
    lateinit var tvTo: TextView
    lateinit var etSearch: EditText
    lateinit var spinnerBranch: Spinner
    lateinit var tvResult: TextView
    var fromDate: Date? = null
    var toDate: Date? = null
    var filterType = "SALES"
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)

        root.addView(LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#111827"))
            setPadding(16,14,16,14)
            addView(TextView(this@IJ_AuditTrailActivity).apply{ text="SMART POS"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=16f })
        })

        root.addView(LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1F2937"))
            setPadding(12,10,12,10)
            addView(TextView(this@IJ_AuditTrailActivity).apply{ text="IJ Audit Trail - Ultra"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=13f })
        })

        // DATE ROW WITH CALENDAR
        val dateRow = LinearLayout(this)
        dateRow.orientation = LinearLayout.HORIZONTAL
        dateRow.setPadding(12,8,12,4)

        val fromBox = LinearLayout(this)
        fromBox.orientation = LinearLayout.VERTICAL
        fromBox.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        fromBox.addView(TextView(this).apply{ text="FROM:"; textSize=10f; setTextColor(Color.GRAY) })
        tvFrom = TextView(this)
        tvFrom.text = "Select Date"
        tvFrom.textSize = 12f
        tvFrom.setTypeface(null, Typeface.BOLD)
        tvFrom.setPadding(0,4,0,8)
        tvFrom.setTextColor(Color.parseColor("#2563EB"))
        tvFrom.setOnClickListener{ pickDate(true) }
        fromBox.addView(tvFrom)
        dateRow.addView(fromBox)

        val toBox = LinearLayout(this)
        toBox.orientation = LinearLayout.VERTICAL
        toBox.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        toBox.addView(TextView(this).apply{ text="TO:"; textSize=10f; setTextColor(Color.GRAY) })
        tvTo = TextView(this)
        tvTo.text = "16/10/2026"
        tvTo.textSize = 12f
        tvTo.setTypeface(null, Typeface.BOLD)
        tvTo.setPadding(0,4,0,8)
        tvTo.setTextColor(Color.parseColor("#2563EB"))
        tvTo.setOnClickListener{ pickDate(false) }
        toBox.addView(tvTo)
        dateRow.addView(toBox)

        root.addView(dateRow)

        // BRANCH
        val branchRow = LinearLayout(this)
        branchRow.setPadding(12,4,12,8)
        branchRow.addView(TextView(this).apply{ text="Branch:"; textSize=11f; setTextColor(Color.GRAY) })
        root.addView(branchRow)

        spinnerBranch = Spinner(this)
        val branches = mutableListOf("All Branches","Rukudzo","Main Shop","Rukudzo Shop")
        spinnerBranch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branches)
        spinnerBranch.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,0,12,8)}
        root.addView(spinnerBranch)

        spinnerBranch.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a:AdapterView<*>?, v:View?, p:Int, id:Long){ filterLogs() }
            override fun onNothingSelected(a:AdapterView<*>?){}
        }

        // SEARCH
        etSearch = EditText(this)
        etSearch.hint = "Search product, type, ID..."
        etSearch.textSize = 13f
        etSearch.setBackgroundColor(Color.parseColor("#F3F4F6"))
        etSearch.setPadding(16,16,16,16)
        etSearch.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,0,8,8)}
        root.addView(etSearch)

        // FILTER BUTTONS - ALL / SALES / STOCK
        val filterRow = LinearLayout(this)
        filterRow.orientation = LinearLayout.HORIZONTAL
        filterRow.setPadding(8,0,8,0)

        fun makeFilterBtn(title:String, type:String): Button {
            val b = Button(this)
            b.text = title
            b.textSize = 12f
            b.setPadding(0,18,0,18)
            b.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)}
            if(type==filterType){
                b.setBackgroundColor(Color.parseColor("#2563EB"))
                b.setTextColor(Color.WHITE)
            }else{
                b.setBackgroundColor(Color.parseColor("#E5E7EB"))
                b.setTextColor(Color.BLACK)
            }
            b.setOnClickListener{
                filterType = type
                // redraw buttons
                for(i in 0 until filterRow.childCount){
                    val btn = filterRow.getChildAt(i) as Button
                    if(btn.text.toString()==title){
                        btn.setBackgroundColor(Color.parseColor("#2563EB")); btn.setTextColor(Color.WHITE)
                    }else{
                        btn.setBackgroundColor(Color.parseColor("#E5E7EB")); btn.setTextColor(Color.BLACK)
                    }
                }
                filterLogs()
            }
            return b
        }

        filterRow.addView(makeFilterBtn("ALL","ALL"))
        filterRow.addView(makeFilterBtn("SALES","SALES"))
        filterRow.addView(makeFilterBtn("STOCK","STOCK"))
        root.addView(filterRow)

        // TABLE HEADER
        val th = LinearLayout(this)
        th.setBackgroundColor(Color.parseColor("#F3F4F6"))
        th.setPadding(4,8,4,8)
        fun tht(t:String,w:Float):TextView{ val tv=TextView(this); tv.text=t; tv.textSize=9f; tv.setTypeface(null, Typeface.BOLD); tv.setTextColor(Color.GRAY); tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv }
        th.addView(tht("DATE/TIME",1.2f)); th.addView(tht("TYPE",0.6f)); th.addView(tht("PRODUCT",1.0f)); th.addView(tht("QTY",0.5f)); th.addView(tht("TOTAL",0.7f))
        root.addView(th)

        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(-1,0,1f)
        table = LinearLayout(this)
        table.orientation = LinearLayout.VERTICAL
        scroll.addView(table)
        root.addView(scroll)

        tvResult = TextView(this)
        tvResult.textSize = 9f
        tvResult.setTextColor(Color.GRAY)
        tvResult.setPadding(8,6,8,6)
        tvResult.gravity = Gravity.CENTER
        root.addView(tvResult)

        setContentView(root)

        // INIT DATES
        toDate = Date()
        tvTo.text = dateFormat.format(toDate!!)

        // Calendar for FROM = 30 days ago
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -30)
        fromDate = cal.time
        tvFrom.text = dateFormat.format(fromDate!!)

        etSearch.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filterLogs() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })

        loadAllLogs()
        filterLogs()
    }

    fun pickDate(isFrom:Boolean){
        val cal = Calendar.getInstance()
        val current = if(isFrom) fromDate else toDate
        if(current!=null) cal.time = current

        val dp = DatePickerDialog(this, { _, y, m, d ->
            cal.set(y,m,d)
            if(isFrom){
                fromDate = cal.time
                tvFrom.text = dateFormat.format(fromDate!!)
            }else{
                toDate = cal.time
                // set to end of day
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                toDate = cal.time
                tvTo.text = dateFormat.format(cal.time)
            }
            filterLogs()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        dp.show()
    }

    fun loadAllLogs(){
        allLogs.clear()
        val sdfList = listOf(
            SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        )

        fun parseDate(str:String): Date {
            for(sdf in sdfList){ try{ return sdf.parse(str)!! }catch(_:Exception){} }
            return Date()
        }

        // 1. LOAD SALES - CHECK ALL POSSIBLE PREFS
        val salesPrefs = listOf("sales","sales_history","pos_sales","transactions","sales_list","invoices","receipts","sales_data","sale_items","orders")
        for(pn in salesPrefs){
            try{
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    try{
                        val s = v.toString()
                        if(!s.trim().startsWith("{")) continue
                        val o = JSONObject(s)
                        val product = o.optString("product_name", o.optString("name", o.optString("product","Sale")))
                        val code = o.optString("code", o.optString("sku", k))
                        val qty = o.optInt("qty", o.optInt("quantity", 1))
                        val total = o.optDouble("total", o.optDouble("amount", o.optDouble("price", 0.0))).toFloat()
                        val branch = o.optString("branch","All Branches")
                        val dateStr = o.optString("date", o.optString("time", o.optString("datetime", o.optString("timestamp", dateFormat.format(Date())))))
                        val date = parseDate(dateStr)
                        val id = o.optString("id", o.optString("invoice_id", k))

                        allLogs.add(Log(date, dateStr, "SALES", product, code, qty, total, branch, id))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }

        // 2. LOAD RECEIVED STOCK - CHECK ALL POSSIBLE PREFS
        val stockPrefs = listOf("received_stock","stock_received","goods_received","grn","purchase_stock","products","inventory","stock","receiving","products_list")
        for(pn in stockPrefs){
            try{
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    try{
                        val s = v.toString()
                        if(!s.trim().startsWith("{")) continue
                        val o = JSONObject(s)
                        // Avoid duplicate if already counted as sales - check if has received marker
                        val isStock = o.has("qnty") || o.has("quantity") || o.has("received_qty") || pn.contains("received") || pn.contains("stock") || pn=="products" || pn=="inventory"

                        if(isStock && !pn.contains("sales")){
                            val product = o.optString("product_name", o.optString("name", o.optString("product","")))
                            if(product.isBlank() || allLogs.any{ it.id==k && it.type=="STOCK"}) continue
                            val code = o.optString("code", o.optString("sku", k))
                            var qty = 0
                            for(qk in listOf("qnty","quantity","qty","received_qty","stock")){
                                if(o.has(qk)){ qty = o.optInt(qk, o.optString(qk,"0").toIntOrNull() ?: 0); if(qty!=0) break }
                            }
                            val cost = o.optDouble("cost", o.optDouble("buying_price", 0.0)).toFloat()
                            val total = qty * cost
                            val branch = o.optString("branch","All Branches")
                            val dateStr = o.optString("date", o.optString("received_date", dateFormat.format(Date())))
                            val date = parseDate(dateStr)
                            val id = k

                            // Only add if looks like stock received, not sales
                            if(product.isNotBlank()){
                                // Check if already added as stock
                                if(allLogs.none{ it.code==code && it.product==product && it.type=="STOCK" && it.qty==qty }){
                                    allLogs.add(Log(date, dateStr, "STOCK", product, code, qty, total, branch, id))
                                }
                            }
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }

        // Sort by date descending
        allLogs.sortByDescending{ it.date }
    }

    fun filterLogs(){
        val q = etSearch.text.toString().lowercase().trim()
        val branch = try{ (spinnerBranch.selectedItem as String) }catch(_:Exception){ "All Branches" }

        filteredLogs = allLogs.filter{ log ->
            val typeOk = filterType=="ALL" || log.type==filterType
            val branchOk = branch=="All Branches" || log.branch.contains(branch, true) || branch.contains(log.branch, true)
            val searchOk = q.isBlank() || log.product.lowercase().contains(q) || log.code.lowercase().contains(q) || log.type.lowercase().contains(q) || log.id.lowercase().contains(q)
            val dateOk = (fromDate==null || !log.date.before(fromDate)) && (toDate==null || !log.date.after(toDate))
            typeOk && branchOk && searchOk && dateOk
        }.toMutableList()

        render()
    }

    fun render(){
        table.removeAllViews()
        if(filteredLogs.isEmpty()){
            val empty = LinearLayout(this)
            empty.orientation = LinearLayout.VERTICAL
            empty.gravity = Gravity.CENTER
            empty.setPadding(0,60,0,0)
            empty.addView(TextView(this).apply{ text="No logs yet"; textSize=14f; setTextColor(Color.GRAY); gravity=Gravity.CENTER })
            empty.addView(TextView(this).apply{ text="Add products or make a sale\nLogs will appear here"; textSize=12f; setTextColor(Color.parseColor("#9CA3AF")); gravity=Gravity.CENTER; setPadding(0,8,0,0) })
            table.addView(empty)
        }else{
            for(log in filteredLogs){
                val row = LinearLayout(this)
                row.setPadding(4,10,4,10)
                row.setBackgroundColor(Color.WHITE)

                fun td(t:String,w:Float,bold:Boolean=false,color:Int=Color.BLACK):TextView{
                    val tv=TextView(this); tv.text=t; tv.textSize=10f; tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); tv.setTextColor(color)
                    if(bold) tv.setTypeface(null, Typeface.BOLD)
                    return tv
                }

                row.addView(td(SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(log.date),1.2f))
                row.addView(td(log.type,0.6f,true, if(log.type=="SALES") Color.parseColor("#2563EB") else Color.parseColor("#16A34A")))
                row.addView(TextView(this).apply{ text=log.product; textSize=10f; layoutParams=LinearLayout.LayoutParams(0,-2,1.0f); setPadding(4,0,0,0) })
                row.addView(td("${log.qty}",0.5f,true))
                row.addView(td("$${log.total}",0.7f,true))

                table.addView(row)
                table.addView(View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#F3F4F6")) })
            }
        }

        tvResult.text = "Ultra FINAL - ${filteredLogs.size}/${allLogs.size} result - Filter: $filterType - Branch: ${try{spinnerBranch.selectedItem}catch(_:Exception){ "All Branches" }} - Search: '${etSearch.text}'"
    }
}