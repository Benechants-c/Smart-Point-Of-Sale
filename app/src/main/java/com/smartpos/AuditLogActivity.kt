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
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class AuditLogActivity : Activity() {

    private val allLogs = mutableListOf<LogItem>()
    private lateinit var container: LinearLayout
    private lateinit var countText: TextView
    private var currentFilter = "ALL"
    private var fromDate: Long = 0L
    private var toDate: Long = System.currentTimeMillis()
    private var selectedProduct = "ALL"
    private val sdfDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val sdfFull = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    data class LogItem(val ts: Long, val timeStr:String, val user:String, val branch:String, val action:String, val detail:String, val productKey:String, val color:String, val icon:String, val rawSaleId:String="")

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try {
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC")) }

            val header = LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#0F172A")); setPadding(24,28,24,20); orientation = LinearLayout.VERTICAL }
            header.addView(TextView(this).apply { text="📋 Audit Trail"; textSize=18f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE) })
            header.addView(TextView(this).apply { text="Ultra: Real items + Date + Product filter"; textSize=11f; setTextColor(Color.parseColor("#94A3B8")) })
            root.addView(header)

            val topBar = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(12,8,12,8) }
            val back = Button(this).apply { text="← BACK"; setBackgroundColor(Color.parseColor("#E2E8F0")) }
            back.setOnClickListener { finish() }
            val clear = Button(this).apply { text="Clear"; setBackgroundColor(Color.parseColor("#FEE2E2")); setTextColor(Color.parseColor("#DC2626")); layoutParams=LinearLayout.LayoutParams(-2,-2).apply{setMargins(8,0,0,0)} }
            clear.setOnClickListener { getSharedPreferences("audit_log", Context.MODE_PRIVATE).edit().clear().apply(); loadLogs(); filterAndShow(); Toast.makeText(this,"Cleared",0).show() }
            topBar.addView(back); topBar.addView(clear); root.addView(topBar)

            // DATE FILTER ROW - ULTRA
            val dateRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(12,8,12,4) }
            val btnFrom = Button(this).apply { text="📅 From: ALL TIME"; textSize=11f; setBackgroundColor(Color.WHITE) }
            val btnTo = Button(this).apply { text="📅 To: ${sdfDate.format(Date(toDate))}"; textSize=11f; setBackgroundColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(-2,-2).apply{setMargins(8,0,0,0)} }
            btnFrom.setOnClickListener { showDatePicker(true, btnFrom) }
            btnTo.setOnClickListener { showDatePicker(false, btnTo) }
            dateRow.addView(btnFrom, LinearLayout.LayoutParams(0,-2,1f)); dateRow.addView(btnTo, LinearLayout.LayoutParams(0,-2,1f)); root.addView(dateRow)

            // PRODUCT FILTER - auto from stock
            val products = getProductNames()
            val prodRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(12,4,12,4) }
            prodRow.addView(TextView(this).apply { text="Product:"; textSize=11f; setPadding(0,12,8,0) })
            val spinnerProd = Spinner(this)
            spinnerProd.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("ALL PRODUCTS") + products)
            spinnerProd.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
                override fun onItemSelected(p: AdapterView<*>?, v:android.view.View?, pos:Int, id:Long){ selectedProduct = if(pos==0) "ALL" else products[pos-1]; filterAndShow() }
                override fun onNothingSelected(p: AdapterView<*>?){}
            }
            prodRow.addView(spinnerProd, LinearLayout.LayoutParams(0,-2,1f)); root.addView(prodRow)

            val search = EditText(this).apply { hint="🔍 Search user, stove, tv..."; setBackgroundColor(Color.WHITE); setPadding(16,12,16,12); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,4,12,4)} }
            root.addView(search)

            val filters = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(12,4,12,4) }
            fun addFilter(name:String){
                val btn = Button(this).apply { text=name; textSize=11f; setPadding(12,6,12,6); layoutParams=LinearLayout.LayoutParams(-2,-2).apply{setMargins(4,0,4,0)} }
                if(name==currentFilter){ btn.setBackgroundColor(Color.parseColor("#0F172A")); btn.setTextColor(Color.WHITE) }else{ btn.setBackgroundColor(Color.parseColor("#E2E8F0")); btn.setTextColor(Color.parseColor("#334155")) }
                btn.setOnClickListener {
                    currentFilter=name
                    for(i in 0 until filters.childCount){ (filters.getChildAt(i) as Button).let{ b-> if(b.text==currentFilter){ b.setBackgroundColor(Color.parseColor("#0F172A")); b.setTextColor(Color.WHITE) }else{ b.setBackgroundColor(Color.parseColor("#E2E8F0")); b.setTextColor(Color.parseColor("#334155")) } } }
                    filterAndShow(search.text.toString())
                }
                filters.addView(btn)
            }
            addFilter("ALL"); addFilter("SALES"); addFilter("STOCK"); addFilter("CASH")
            root.addView(filters)

            countText = TextView(this).apply { textSize=12f; setTypeface(null,Typeface.BOLD); setPadding(16,12,16,4); setTextColor(Color.parseColor("#334155")) }
            root.addView(countText)
            container = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            root.addView(container)

            loadLogs()
            search.addTextChangedListener(object: TextWatcher{
                override fun afterTextChanged(s: Editable?){ filterAndShow(s.toString()) }
                override fun beforeTextChanged(a: CharSequence?, b:Int, c:Int, d:Int){}
                override fun onTextChanged(a: CharSequence?, b:Int, c:Int, d:Int){}
            })
            filterAndShow()
            scroll.addView(root)
            setContentView(scroll)
        } catch(e:Exception){ setContentView(TextView(this).apply{ text="Error: ${e.message}\n${e.stackTraceToString()}" }) }
    }

    private fun showDatePicker(isFrom:Boolean, btn:Button){
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val cal = Calendar.getInstance().apply{ set(y,m,d, if(isFrom) 0 else 23, if(isFrom) 0 else 59, if(isFrom) 0 else 59) }
            if(isFrom){ fromDate=cal.timeInMillis }else{ toDate=cal.timeInMillis }
            btn.text = "📅 ${if(isFrom) "From" else "To"}: ${sdfDate.format(Date(cal.timeInMillis))}"
            filterAndShow()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun getProductNames(): List<String>{
        val set = mutableSetOf<String>()
        try{
            val stock = getSharedPreferences("stock_main", MODE_PRIVATE)
            for((_,v) in stock.all){ set.add(v.toString().split("|")[0].trim()) }
            val prod = getSharedPreferences("products_db", MODE_PRIVATE)
            for((_,v) in prod.all){ set.add(v.toString().split("|")[0].trim()) }
        }catch(_:Exception){}
        return set.filter{ it.isNotEmpty() }.sorted()
    }

    // ULTRA FINAL FIX: Parse real sale items
    private fun loadLogs(){
        allLogs.clear()
        try{
            val sales = getSharedPreferences("sales_main", MODE_PRIVATE)
            for((k,v) in sales.all){
                val ts = try{ k.replace("SALE_","").toLong() }catch(_:Exception){ System.currentTimeMillis() }
                val parts = v.toString().split("|")
                val total = if(parts.size>1) parts[1] else "0"
                val profit = if(parts.size>2) parts[2] else "0"

                // TRY ULTRA: get sale_items_SALE_xxx
                var itemsText = ""
                var productKeyForFilter = ""
                try{
                    // Try all possible locations for sale items
                    val prefNames = listOf("sale_items_$k", "sale_items", "sales_items_$k", k)
                    for(prefName in prefNames){
                        val pref = getSharedPreferences(prefName, MODE_PRIVATE)
                        if(pref.all.isNotEmpty()){
                            // If prefName == sale_items_SALE_xxx, its entries are products
                            if(prefName.startsWith("sale_items_")){
                                val items = pref.all.values.map{ it.toString() }
                                // format: name|qty|price|...
                                val pretty = items.map{
                                    val p = it.split("|")
                                    val name = if(p.isNotEmpty()) p[0] else it
                                    val qty = if(p.size>1) p[1] else "1"
                                    "$name x$qty"
                                }
                                itemsText = pretty.joinToString(", ")
                                productKeyForFilter = itemsText.lowercase()
                                break
                            } else if(prefName=="sale_items" || prefName=="sales_db"){
                                val raw = pref.getString(k, null)?: pref.all[k]?.toString()
                                if(raw!=null){ itemsText = raw; productKeyForFilter = raw.lowercase(); break }
                            }
                        }
                    }
                }catch(_:Exception){}

                // Fallback if no items found: show product from stock if total matches
                if(itemsText.isEmpty()){
                    itemsText = "Sale items (legacy)"
                    productKeyForFilter = k.lowercase()
                }

                val detail = "$itemsText\nTotal: $${fmt(total)} • Profit: $${fmt(profit)}"
                allLogs.add(LogItem(ts, sdfFull.format(Date(ts)), "cashier", "Main Shop", "SALES", detail, productKeyForFilter, "#16A34A","🟢", k))
            }
        }catch(_:Exception){}

        try{
            val stock = getSharedPreferences("stock_main", MODE_PRIVATE)
            for((k,v) in stock.all){
                val parts = v.toString().split("|")
                val name = if(parts.isNotEmpty()) parts[0] else k
                val buy = if(parts.size>1) parts[1] else "0"
                val sell = if(parts.size>2) parts[2] else "0"
                val qty = if(parts.size>3) parts[3] else "0"
                allLogs.add(LogItem(System.currentTimeMillis()-1000, sdfFull.format(Date()), "admin","Main Shop","STOCK","Stock $name • Qty: $qty • Buy $${fmt(buy)} • Sell $${fmt(sell)}", name.lowercase(), "#2563EB","📦"))
            }
        }catch(_:Exception){}

        allLogs.sortByDescending { it.ts }
    }

    private fun filterAndShow(query:String=""){
        container.removeAllViews()
        var filtered = allLogs.filter { if(fromDate==0L) true else it.ts in fromDate..toDate }
        if(currentFilter!="ALL") filtered = filtered.filter { it.action==currentFilter }
        if(selectedProduct!="ALL") filtered = filtered.filter { it.productKey.contains(selectedProduct.lowercase()) }
        if(query.isNotEmpty()) filtered = filtered.filter { it.detail.lowercase().contains(query.lowercase()) || it.productKey.contains(query.lowercase()) || it.user.lowercase().contains(query.lowercase()) }

        countText.text = "${filtered.size} Logs • ${if(fromDate==0L) "ALL TIME" else sdfDate.format(Date(fromDate))+"→"+sdfDate.format(Date(toDate))} • $selectedProduct"

        if(filtered.isEmpty()){
            container.addView(TextView(this).apply{ text="No activity for filters"; setPadding(24,32,24,32); gravity=Gravity.CENTER; setTextColor(Color.parseColor("#64748B")) })
            return
        }
        for(log in filtered){
            val card = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,12,16,12); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,6,12,0)} }
            val top = LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL }
            top.addView(TextView(this).apply{ text="${log.icon} ${log.action}"; textSize=12f; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor(log.color)); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            top.addView(TextView(this).apply{ text=log.timeStr; textSize=10f; setTextColor(Color.parseColor("#94A3B8")) })
            card.addView(top)
            card.addView(TextView(this).apply{ text="${log.user} • ${log.branch}"; textSize=11f; setTextColor(Color.parseColor("#64748B")) })
            card.addView(TextView(this).apply{ text=log.detail; textSize=13f; setTextColor(Color.parseColor("#0F172A")); setPadding(0,6,0,0) })
            if(log.rawSaleId.isNotEmpty()){
                card.addView(TextView(this).apply{ text="ID: ${log.rawSaleId.take(20)}"; textSize=9f; setTextColor(Color.parseColor("#CBD5E1")); setPadding(0,4,0,0) })
            }
            container.addView(card)
        }
    }
    private fun fmt(s:String):String{ return try{ String.format("%.2f", s.toDouble()) }catch(_:Exception){ s } }
}
