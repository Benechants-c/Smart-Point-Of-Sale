package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AuditLogActivity : Activity() {

    data class LogEntry(val dateTime:String, val type:String, val product:String, val code:String, val qty:Int, val total:Float, val branch:String, val rawTime:Long)

    var allLogs = mutableListOf<LogEntry>()
    var filtered = mutableListOf<LogEntry>()
    lateinit var tableList: LinearLayout
    lateinit var etSearch: EditText
    lateinit var btnAll: Button
    lateinit var btnSales: Button
    lateinit var btnStock: Button
    lateinit var tvResult: TextView
    var currentFilter = "STOCK"
    var selectedBranch = "All Branches"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)

        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#111827"))
        header.setPadding(16,20,16,16)
        header.addView(TextView(this).apply{ text="IJ Audit Trail - Ultra"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=15f; setOnClickListener{ finish() } })
        root.addView(header)

        // DATE
        val dateRow = LinearLayout(this)
        dateRow.orientation=LinearLayout.HORIZONTAL
        dateRow.setPadding(8,8,8,4)
        fun dateBox(lbl:String, value:String): LinearLayout{
            val b=LinearLayout(this); b.orientation=LinearLayout.VERTICAL; b.setPadding(8,6,8,6); b.setBackgroundColor(Color.WHITE)
            b.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(4,0,4,0) }
            b.addView(TextView(this).apply{ text=lbl; textSize=9f; setTextColor(Color.GRAY) })
            b.addView(TextView(this).apply{ text=value; setTypeface(null, Typeface.BOLD); textSize=12f })
            return b
        }
        dateRow.addView(dateBox("FROM:","Select Date"))
        dateRow.addView(dateBox("TO:","16/10/2026"))
        root.addView(dateRow)

        root.addView(TextView(this).apply{ text="Branch:"; textSize=11f; setPadding(12,4,12,2) })
        val spinnerBranch = Spinner(this)
        val branches = mutableListOf("All Branches","Rukudzo Shop","Rukudzo","Main Shop")
        try{
            val pref=getSharedPreferences("shops", Context.MODE_PRIVATE)
            for((_,v) in pref.all){ try{ val o=JSONObject(v.toString()); val n=o.optString("name",""); if(n.isNotBlank() &&!branches.contains(n)) branches.add(n) }catch(_:Exception){} }
        }catch(_:Exception){}
        spinnerBranch.adapter=ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branches)
        root.addView(spinnerBranch, LinearLayout.LayoutParams(-1,-2).apply{ setMargins(12,0,12,4) })

        // SEARCH - LIVE BY CODE OR NAME
        val searchBox = LinearLayout(this)
        searchBox.setBackgroundColor(Color.parseColor("#F3F4F6"))
        searchBox.setPadding(12,10,12,10)
        searchBox.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{ setMargins(8,4,8,8) }
        etSearch = EditText(this)
        etSearch.hint="Search product, type, ID..."
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.layoutParams=LinearLayout.LayoutParams(-1,-2,1f)
        searchBox.addView(etSearch)
        root.addView(searchBox)

        // BUTTONS - ALL 3 FIXED CLICKABLE
        val btnRow = LinearLayout(this)
        btnRow.orientation=LinearLayout.HORIZONTAL
        btnRow.setPadding(8,0,8,8)

        btnAll = Button(this).apply{ text="ALL"; isEnabled=true; isClickable=true }
        btnAll.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(4,0,4,0) }
        btnSales = Button(this).apply{ text="SALES"; isEnabled=true; isClickable=true }
        btnSales.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(4,0,4,0) }
        btnStock = Button(this).apply{ text="STOCK"; isEnabled=true; isClickable=true }
        btnStock.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(4,0,4,0) }

        fun setActive(active:Button){
            val inactiveBg = Color.parseColor("#E5E7EB")
            val activeBg = Color.parseColor("#2563EB")
            listOf(btnAll,btnSales,btnStock).forEach{
                it.setBackgroundColor(inactiveBg)
                it.setTextColor(Color.GRAY)
            }
            active.setBackgroundColor(activeBg)
            active.setTextColor(Color.WHITE)
        }

        btnAll.setOnClickListener{ currentFilter="ALL"; setActive(btnAll); filterLogs() }
        btnSales.setOnClickListener{ currentFilter="SALES"; setActive(btnSales); filterLogs() }
        btnStock.setOnClickListener{ currentFilter="STOCK"; setActive(btnStock); filterLogs() }

        btnRow.addView(btnAll); btnRow.addView(btnSales); btnRow.addView(btnStock)
        root.addView(btnRow)
        setActive(btnStock)

        spinnerBranch.onItemSelectedListener=object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a:AdapterView<*>?, v:android.view.View?, p:Int, id:Long){ selectedBranch=branches[p]; filterLogs() }
            override fun onNothingSelected(a:AdapterView<*>?){}
        }

        // HEADER TABLE
        val hRow = LinearLayout(this)
        hRow.setBackgroundColor(Color.parseColor("#F3F4F6"))
        hRow.setPadding(4,8,4,8)
        fun th(t:String,w:Float):TextView{ val tv=TextView(this); tv.text=t; tv.textSize=9f; tv.setTypeface(null, Typeface.BOLD); tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv }
        hRow.addView(th("DATE/TIME",1.2f)); hRow.addView(th("TYPE",0.6f)); hRow.addView(th("PRODUCT",1.0f)); hRow.addView(th("QTY",0.5f)); hRow.addView(th("TOTAL",0.6f))
        root.addView(hRow)

        val scroll = ScrollView(this)
        scroll.layoutParams=LinearLayout.LayoutParams(-1,0,1f)
        tableList = LinearLayout(this)
        tableList.orientation=LinearLayout.VERTICAL
        scroll.addView(tableList)
        root.addView(scroll)

        tvResult = TextView(this)
        tvResult.text="Loading..."
        tvResult.textSize=9f
        tvResult.gravity=Gravity.CENTER
        tvResult.setPadding(0,12,0,12)
        tvResult.setTextColor(Color.GRAY)
        root.addView(tvResult)

        setContentView(root)

        loadAllLogs()
        filterLogs()

        etSearch.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filterLogs() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    fun loadAllLogs(){
        allLogs.clear()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        // FIX 1: CHECK ALL PRODUCT PREFS FOR STOCK LOGS - qnty, quantity, qty
        val productPrefs = listOf("products","products_main","products_db","inventory","stock","products_list","items")
        for(pn in productPrefs){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    try{
                        val o=JSONObject(v.toString())
                        val name=o.optString("name", o.optString("product_name",""))
                        val code=o.optString("code", o.optString("sku", o.optString("id",k)))
                        var qty=0
                        for(qk in listOf("qnty","quantity","qty","stock","current_stock")){
                            if(o.has(qk)){ qty=o.optString(qk,"0").toIntOrNull()?: o.optInt(qk,0); break }
                        }
                        var cost=0f
                        for(ck in listOf("cost","buying_price","buy_price")){ if(o.has(ck)){ cost=o.optString(ck,"0").toFloatOrNull()?: o.optDouble(ck,0.0).toFloat(); break } }
                        var price=0f
                        for(pk in listOf("price","selling_price","sell_price")){ if(o.has(pk)){ price=o.optString(pk,"0").toFloatOrNull()?: o.optDouble(pk,0.0).toFloat(); break } }
                        if(name.isNotBlank()){
                            allLogs.add(LogEntry(sdf.format(Date()), "STOCK", name, code, qty, qty*price, "Rukudzo Shop", System.currentTimeMillis()-allLogs.size))
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }

        // FIX 2: CHECK ALL LOG PREFS FOR SALES/STOCK
        val logPrefs = listOf("audit_logs","sales_logs","stock_logs","activities","transactions","inventory_logs","sales_history","stock_history","logs","audit_trail","sales","receipts","invoices")
        for(pn in logPrefs){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    try{
                        val raw=v.toString()
                        if(raw.trim().startsWith("{")){
                            val o=JSONObject(raw)
                            val product = o.optString("product", o.optString("name", o.optString("product_name","")))
                            val code = o.optString("code", o.optString("sku", o.optString("id", o.optString("product_code",k))))
                            val type = o.optString("type", o.optString("action", if(pn.contains("sale")) "SALES" else "STOCK"))
                            var qty=0
                            for(qk in listOf("qnty","qty","quantity","count")){ if(o.has(qk)){ qty=o.optString(qk,"0").toIntOrNull()?: o.optInt(qk,0); break } }
                            var total=0f
                            for(tk in listOf("total","amount","value","price")){ if(o.has(tk)){ total=o.optString(tk,"0").toFloatOrNull()?: o.optDouble(tk,0.0).toFloat(); break } }
                            val branch = o.optString("branch", o.optString("shop","Rukudzo Shop"))
                            val dateStr = o.optString("date", o.optString("datetime", sdf.format(Date())))
                            val time = o.optLong("time", o.optLong("timestamp", System.currentTimeMillis()))
                            if(product.isNotBlank() || code.isNotBlank()){
                                allLogs.add(LogEntry(dateStr, type.uppercase(), product.ifBlank{ code }, code, qty, total, branch, time))
                            }
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }

        allLogs.sortByDescending{ it.rawTime }
    }

    fun filterLogs(){
        val q = etSearch.text.toString().lowercase().trim()
        filtered = allLogs.filter{
            val branchOk = selectedBranch=="All Branches" || it.branch.lowercase().contains(selectedBranch.lowercase()) || selectedBranch.lowercase().contains(it.branch.lowercase())
            val typeOk = when(currentFilter){
                "ALL" -> true
                "SALES" -> it.type.contains("SALE", true)
                "STOCK" -> it.type.contains("STOCK", true) || it.type.contains("IN", true) || it.type.contains("ADJUST", true) || it.type.contains("ADD", true)
                else -> true
            }
            // SEARCH BY CODE OR NAME OR TYPE OR ID
            val searchOk = q.isBlank() ||
                    it.product.lowercase().contains(q) ||
                    it.code.lowercase().contains(q) ||
                    it.type.lowercase().contains(q) ||
                    "${it.qty}".contains(q) ||
                    it.branch.lowercase().contains(q)

            branchOk && typeOk && searchOk
        }.toMutableList()
        renderLogs()
    }

    fun renderLogs(){
        tableList.removeAllViews()
        if(filtered.isEmpty()){
            tableList.addView(TextView(this).apply{
                text=if(allLogs.isEmpty()) "\nNo logs yet\n\nAdd products or make a sale\nLogs will appear here" else "\nNo match for '${etSearch.text}'\nFilter: $currentFilter"
                gravity=Gravity.CENTER
                setPadding(0,40,0,0)
                setTextColor(Color.GRAY)
            })
        } else {
            for(log in filtered){
                val row=LinearLayout(this)
                row.orientation=LinearLayout.HORIZONTAL
                row.setPadding(4,10,4,10)
                row.setBackgroundColor(Color.WHITE)
                fun td(t:String,w:Float):TextView{ val tv=TextView(this); tv.text=t; tv.textSize=10f; tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv }
                row.addView(td(log.dateTime,1.2f).apply{ textSize=8f })
                row.addView(td(log.type,0.6f).apply{ setTypeface(null, Typeface.BOLD); setTextColor(if(log.type.contains("SALE")) Color.parseColor("#2563EB") else Color.parseColor("#16A34A")) })
                row.addView(td("${log.product}\n(${log.code})",1.0f).apply{ gravity=Gravity.START; setPadding(8,0,0,0); textSize=9f })
                row.addView(td("${log.qty}",0.5f).apply{ setTypeface(null, Typeface.BOLD) })
                row.addView(td("$${log.total}",0.6f))
                tableList.addView(row)
                tableList.addView(android.view.View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#E5E7EB")) })
            }
        }
        tvResult.text="Ultra FINAL - ${filtered.size}/${allLogs.size} result - Filter: $currentFilter - Branch: $selectedBranch - Search: '${etSearch.text}'"
    }
}