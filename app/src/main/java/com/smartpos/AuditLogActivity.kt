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

    data class LogItem(val ts: Long, val timeStr:String, val user:String, val branch:String, val action:String, val detail:String, val productKey:String, val color:String, val icon:String)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC")) }

        val header = LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#0F172A")); setPadding(24,28,24,20); orientation = LinearLayout.VERTICAL }
        header.addView(TextView(this).apply { text="Audit Trail"; textSize=18f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE) })
        root.addView(header)

        val topBar = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(12,8,12,8) }
        val back = Button(this).apply { text="BACK" }
        back.setOnClickListener { finish() }
        topBar.addView(back)
        root.addView(topBar)

        // DATE ROW
        val dateRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(12,8,12,8) }
        val btnFrom = Button(this).apply { text="From: ALL"; textSize=11f }
        val btnTo = Button(this).apply { text="To: ${sdfDate.format(Date(toDate))}"; textSize=11f }
        btnFrom.setOnClickListener { pickDate(true, btnFrom) }
        btnTo.setOnClickListener { pickDate(false, btnTo) }
        dateRow.addView(btnFrom, LinearLayout.LayoutParams(0,-2,1f))
        dateRow.addView(btnTo, LinearLayout.LayoutParams(0,-2,1f))
        root.addView(dateRow)

        // PRODUCT SPINNER - COMPILE SAFE
        val products = getProductNames()
        val allSpinnerItems = mutableListOf<String>()
        allSpinnerItems.add("ALL PRODUCTS")
        allSpinnerItems.addAll(products)
        val spinner = Spinner(this)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, allSpinnerItems)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        root.addView(spinner, LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,4,12,4)})

        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p: AdapterView<*>?, v:android.view.View?, pos:Int, id:Long){
                selectedProduct = if(pos==0) "ALL" else allSpinnerItems[pos]
                filterAndShow()
            }
            override fun onNothingSelected(p: AdapterView<*>?){}
        }

        val search = EditText(this).apply { hint="Search..."; setPadding(16,12,16,12); setBackgroundColor(Color.WHITE) }
        root.addView(search)

        val filters = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(12,4,12,4) }
        fun addF(name:String){
            val b = Button(this).apply { text=name; textSize=11f }
            b.setOnClickListener { currentFilter=name; filterAndShow(search.text.toString()) }
            filters.addView(b)
        }
        addF("ALL"); addF("SALES"); addF("STOCK")
        root.addView(filters)

        countText = TextView(this).apply { setPadding(16,8,16,8); setTypeface(null,Typeface.BOLD) }
        root.addView(countText)
        container = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        root.addView(container)

        loadLogs()
        search.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?){ filterAndShow(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, a:Int, b:Int, c:Int){}
            override fun onTextChanged(s: CharSequence?, a:Int, b:Int, c:Int){}
        })
        filterAndShow()
        scroll.addView(root)
        setContentView(scroll)
    }

    private fun pickDate(isFrom:Boolean, btn:Button){
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val cal = Calendar.getInstance()
            cal.set(y,m,d, if(isFrom) 0 else 23, if(isFrom) 0 else 59, 0)
            if(isFrom) fromDate=cal.timeInMillis else toDate=cal.timeInMillis
            btn.text = (if(isFrom) "From: " else "To: ") + sdfDate.format(Date(cal.timeInMillis))
            filterAndShow()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun getProductNames(): List<String>{
        val set = mutableSetOf<String>()
        try{
            val p1 = getSharedPreferences("stock_main", MODE_PRIVATE)
            for((_,v) in p1.all){ set.add(v.toString().split("|")[0]) }
            val p2 = getSharedPreferences("stock_main", MODE_PRIVATE)
            for((_,v) in p2.all){ set.add(v.toString().split("|")[0]) }
        }catch(_:Exception){}
        return set.toList()
    }

    private fun loadLogs(){
        allLogs.clear()
        try{
            val sales = getSharedPreferences("sales_main", MODE_PRIVATE)
            for((k,v) in sales.all){
                val ts = try{ k.replace("SALE_","").toLong() }catch(_:Exception){ System.currentTimeMillis() }
                val parts = v.toString().split("|")
                val total = if(parts.size>1) parts[1] else "0"
                val profit = if(parts.size>2) parts[2] else "0"
                var itemsText = ""
                var keyForFilter = ""
                try{
                    val itemsPref = getSharedPreferences("sale_items_$k", MODE_PRIVATE)
                    if(itemsPref.all.isNotEmpty()){
                        val list = itemsPref.all.values.map{
                            val p = it.toString().split("|")
                            val name = if(p.isNotEmpty()) p[0] else it.toString()
                            val qty = if(p.size>1) p[1] else "1"
                            "$name x$qty"
                        }
                        itemsText = list.joinToString(", ")
                        keyForFilter = itemsText.lowercase()
                    }
                }catch(_:Exception){}

                if(itemsText.isEmpty()){
                    itemsText = "Legacy Sale"
                    keyForFilter = "legacy tv 32 stove all products " + getProductNames().joinToString(" ").lowercase()
                }

                val detail = "$itemsText | Total: $${fmt(total)} Profit: $${fmt(profit)}"
                allLogs.add(LogItem(ts, sdfFull.format(Date(ts)), "cashier","Main Shop","SALES", detail, keyForFilter, "#16A34A","SALE"))
            }
        }catch(_:Exception){}

        try{
            val stock = getSharedPreferences("stock_main", MODE_PRIVATE)
            for((_,v) in stock.all){
                val parts = v.toString().split("|")
                val name = parts[0]
                allLogs.add(LogItem(System.currentTimeMillis(), sdfFull.format(Date()), "admin","Main Shop","STOCK","Stock $name", name.lowercase(), "#2563EB","STOCK"))
            }
        }catch(_:Exception){}
        allLogs.sortByDescending { it.ts }
    }

    private fun filterAndShow(q:String=""){
        container.removeAllViews()
        var list = allLogs.filter { if(fromDate==0L) true else it.ts in fromDate..toDate }
        if(currentFilter!="ALL") list = list.filter { it.action==currentFilter }
        if(selectedProduct!="ALL"){
            val sel = selectedProduct.lowercase()
            list = list.filter { it.productKey.contains(sel) || it.detail.lowercase().contains(sel) }
        }
        if(q.isNotEmpty()){
            list = list.filter { it.detail.lowercase().contains(q.lowercase()) }
        }
        countText.text = "${list.size} Logs"
        if(list.isEmpty()){
            container.addView(TextView(this).apply{ text="No activity for filters"; gravity=Gravity.CENTER; setPadding(24,24,24,24) })
            return
        }
        for(log in list){
            val card = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,12,16,12); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,6,12,6)} }
            card.addView(TextView(this).apply{ text="${log.action} ${log.timeStr}"; setTypeface(null,Typeface.BOLD) })
            card.addView(TextView(this).apply{ text=log.detail; textSize=13f })
            container.addView(card)
        }
    }

    private fun fmt(s:String):String{ return try{ String.format("%.2f", s.toDouble()) }catch(_:Exception){ s } }

    companion object {
        fun log(ctx: Context, user: String, branch: String, action: String, detail: String) {
            try {
                val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                val timeStr = fmt.format(Date())
                val key = System.currentTimeMillis().toString()
                val value = "$timeStr|$user|$branch|$action|$detail"
                ctx.getSharedPreferences("audit_log", Context.MODE_PRIVATE).edit().putString(key, value).apply()
            } catch (_: Exception) {}
        }
    }
}
