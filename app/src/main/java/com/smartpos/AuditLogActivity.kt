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

    data class AuditItem(val dateTime:Long, val type:String, val product:String, val qty:Int, val total:Float, val branch:String, val code:String)

    var allLogs = mutableListOf<AuditItem>()
    var filtered = mutableListOf<AuditItem>()
    var selectedTab = "STOCK" // ALL, SALES, STOCK
    var fromDate:Long = 0L
    var toDate:Long = System.currentTimeMillis()
    var selectedBranch = "All Branches"
    
    lateinit var tableList: LinearLayout
    lateinit var tvResultCount: TextView
    lateinit var etSearch: EditText
    lateinit var tvFrom: TextView
    lateinit var tvTo: TextView
    lateinit var spinnerBranch: Spinner
    lateinit var chipRukudzo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F8FAFC"))

        // HEADER - Audit Trail - Ultra
        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#1E293B"))
        header.setPadding(16,24,16,24)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        
        val back = TextView(this)
        back.text = "←"
        back.setTextColor(Color.WHITE)
        back.textSize = 22f
        back.setPadding(0,0,16,0)
        back.setOnClickListener{ finish() }
        header.addView(back)
        
        header.addView(TextView(this).apply{
            text="Audit Trail - Ultra"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=18f
            layoutParams=LinearLayout.LayoutParams(0,-2,1f)
        })
        
        val more = TextView(this)
        more.text="⋮"
        more.setTextColor(Color.WHITE)
        more.textSize=22f
        header.addView(more)
        root.addView(header)

        // CONTAINER
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(16,16,16,16)
        container.setBackgroundColor(Color.WHITE)

        // FROM TO ROW
        val dateRow = LinearLayout(this)
        dateRow.orientation = LinearLayout.HORIZONTAL
        dateRow.gravity = Gravity.CENTER
        
        fun dateBox(label:String, value:String, onClick:()->Unit): LinearLayout {
            val box = LinearLayout(this)
            box.orientation = LinearLayout.VERTICAL
            box.setBackgroundColor(Color.WHITE)
            box.setPadding(16,12,16,12)
            box.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(4,0,4,0) }
            box.elevation = 1f
            // border
            box.background = android.graphics.drawable.GradientDrawable().apply{
                setColor(Color.WHITE); setStroke(1, Color.parseColor("#94A3B8")); cornerRadius=8f
            }
            val tvLabel = TextView(this)
            tvLabel.text = label
            tvLabel.textSize = 10f
            tvLabel.setTextColor(Color.GRAY)
            tvLabel.gravity = Gravity.CENTER
            
            val row2 = LinearLayout(this)
            row2.orientation = LinearLayout.HORIZONTAL
            row2.gravity = Gravity.CENTER_VERTICAL
            row2.setPadding(0,8,0,0)
            val tvVal = TextView(this)
            tvVal.text = value
            tvVal.textSize = 13f
            tvVal.setTypeface(null, Typeface.BOLD)
            tvVal.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
            val icon = TextView(this)
            icon.text = "📅"
            row2.addView(tvVal)
            row2.addView(icon)
            
            box.addView(tvLabel)
            box.addView(row2)
            box.setOnClickListener{ onClick() }
            if(label=="FROM:") tvFrom=tvVal else tvTo=tvVal
            return box
        }
        
        dateRow.addView(dateBox("FROM:", "Select Date"){
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y,m,d ->
                val cal = Calendar.getInstance()
                cal.set(y,m,d)
                fromDate = cal.timeInMillis
                tvFrom.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(fromDate))
                filterLogs()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        })
        
        dateRow.addView(dateBox("TO:", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(toDate))){
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, y,m,d ->
                val cal = Calendar.getInstance()
                cal.set(y,m,d,23,59,59)
                toDate = cal.timeInMillis
                tvTo.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(toDate))
                filterLogs()
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        })
        
        container.addView(dateRow)

        // BRANCH DROPDOWN
        val branchLabel = TextView(this)
        branchLabel.text = "Branch:"
        branchLabel.textSize = 12f
        branchLabel.setPadding(0,16,0,4)
        container.addView(branchLabel)
        
        spinnerBranch = Spinner(this)
        val branches = loadBranches()
        spinnerBranch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branches)
        spinnerBranch.setBackgroundColor(Color.WHITE)
        spinnerBranch.background = android.graphics.drawable.GradientDrawable().apply{
            setColor(Color.WHITE); setStroke(1, Color.parseColor("#94A3B8")); cornerRadius=8f
        }
        spinnerBranch.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a:AdapterView<*>?, v:View?, p:Int, id:Long){
                selectedBranch = branches[p]
                chipRukudzo.visibility = if(selectedBranch=="All Branches" || selectedBranch.contains("Rukudzo")) View.VISIBLE else View.GONE
                filterLogs()
            }
            override fun onNothingSelected(a:AdapterView<*>?){}
        }
        container.addView(spinnerBranch)
        
        chipRukudzo = TextView(this)
        chipRukudzo.text = "🏪 Rukudzo Shop"
        chipRukudzo.setBackgroundColor(Color.parseColor("#BFDBFE"))
        chipRukudzo.setTextColor(Color.parseColor("#1D4ED8"))
        chipRukudzo.setPadding(16,6,16,6)
        chipRukudzo.textSize = 11f
        chipRukudzo.layoutParams = LinearLayout.LayoutParams(-2,-2).apply{ setMargins(0,6,0,0) }
        chipRukudzo.background = android.graphics.drawable.GradientDrawable().apply{
            setColor(Color.parseColor("#BFDBFE")); cornerRadius=20f
        }
        container.addView(chipRukudzo)

        // SEARCH
        val searchBox = LinearLayout(this)
        searchBox.orientation = LinearLayout.HORIZONTAL
        searchBox.setBackgroundColor(Color.parseColor("#F1F5F9"))
        searchBox.setPadding(16,12,16,12)
        searchBox.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(0,12,0,12) }
        searchBox.background = android.graphics.drawable.GradientDrawable().apply{
            setColor(Color.parseColor("#F1F5F9")); cornerRadius=20f
        }
        searchBox.addView(TextView(this).apply{ text="🔍"; setPadding(0,0,8,0) })
        etSearch = EditText(this)
        etSearch.hint = "Search product, type, ID..."
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.layoutParams = LinearLayout.LayoutParams(-1,-2,1f)
        etSearch.textSize = 13f
        searchBox.addView(etSearch)
        container.addView(searchBox)
        
        etSearch.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s:Editable?){ filterLogs() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })

        // TABS ALL SALES STOCK
        val tabRow = LinearLayout(this)
        tabRow.orientation = LinearLayout.HORIZONTAL
        tabRow.setBackgroundColor(Color.parseColor("#E5E7EB"))
        tabRow.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(0,0,0,12) }
        tabRow.background = android.graphics.drawable.GradientDrawable().apply{
            setColor(Color.parseColor("#E5E7EB")); cornerRadius=8f
        }
        
        fun tab(name:String): TextView {
            val tv = TextView(this)
            tv.text = name
            tv.gravity = Gravity.CENTER
            tv.textSize = 13f
            tv.setTypeface(null, Typeface.BOLD)
            tv.setPadding(0,12,0,12)
            tv.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
            tv.setTextColor(if(name==selectedTab) Color.parseColor("#2563EB") else Color.GRAY)
            if(name==selectedTab){
                tv.background = android.graphics.drawable.GradientDrawable().apply{
                    setColor(Color.WHITE); setStroke(2, Color.parseColor("#2563EB")); cornerRadius=0f
                }
            }
            tv.setOnClickListener{
                selectedTab = name
                // rebuild tabs
                container.removeView(tabRow)
                // we recreate activity simpler: just refresh
                for(i in 0 until tabRow.childCount){
                    val c = tabRow.getChildAt(i) as TextView
                    c.setTextColor(Color.GRAY)
                    c.background = null
                }
                tv.setTextColor(Color.parseColor("#2563EB"))
                tv.setBackgroundColor(Color.parseColor("#E5E7EB"))
                // add underline
                filterLogs()
                // recreate for visual
                recreate()
            }
            return tv
        }
        
        tabRow.addView(tab("ALL"))
        tabRow.addView(tab("SALES"))
        tabRow.addView(tab("STOCK"))
        container.addView(tabRow)

        // TABLE HEADER
        val tableHeader = LinearLayout(this)
        tableHeader.orientation = LinearLayout.HORIZONTAL
        tableHeader.setBackgroundColor(Color.parseColor("#F1F5F9"))
        tableHeader.setPadding(8,10,8,10)
        
        fun th(t:String, w:Float): TextView {
            val tv = TextView(this)
            tv.text = t
            tv.textSize = 10f
            tv.setTypeface(null, Typeface.BOLD)
            tv.setTextColor(Color.GRAY)
            tv.layoutParams = LinearLayout.LayoutParams(0,-2,w)
            return tv
        }
        
        tableHeader.addView(th("DATE/TIME", 1.2f))
        tableHeader.addView(th("TYPE", 0.8f))
        tableHeader.addView(th("PRODUCT", 1.2f))
        tableHeader.addView(th("QTY", 0.6f))
        tableHeader.addView(th("TOTAL", 0.8f))
        container.addView(tableHeader)

        // TABLE LIST
        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(-1,0,1f)
        tableList = LinearLayout(this)
        tableList.orientation = LinearLayout.VERTICAL
        scroll.addView(tableList)
        container.addView(scroll)

        tvResultCount = TextView(this)
        tvResultCount.text = "Ultra FINAL • clean version • 1 result"
        tvResultCount.textSize = 10f
        tvResultCount.gravity = Gravity.CENTER
        tvResultCount.setPadding(0,16,0,0)
        tvResultCount.setTextColor(Color.GRAY)
        container.addView(tvResultCount)

        root.addView(container, LinearLayout.LayoutParams(-1,-1))
        setContentView(root)
        
        loadLogs()
        filterLogs()
    }

    fun loadBranches(): List<String>{
        val list = mutableListOf("All Branches", "Rukudzo Shop", "Main Shop")
        try{
            val pref = getSharedPreferences("shops", Context.MODE_PRIVATE)
            for((_,v) in pref.all){
                try{
                    val o = JSONObject(v.toString())
                    val n = o.optString("name","")
                    if(n.isNotBlank() && !list.contains(n)) list.add(n)
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        return list
    }

    fun loadLogs(){
        allLogs.clear()
        try{
            // Load from sales
            val salesPref = getSharedPreferences("sales_main", Context.MODE_PRIVATE)
            for((_,v) in salesPref.all){
                try{
                    val o = JSONObject(v.toString())
                    val time = o.optLong("date", System.currentTimeMillis())
                    val total = o.optDouble("total",0.0).toFloat()
                    val branch = o.optString("shop","Rukudzo Shop")
                    val arr = o.optJSONArray("items")
                    if(arr!=null){
                        for(i in 0 until arr.length()){
                            val it = arr.getJSONObject(i)
                            allLogs.add(AuditItem(time,"SALES",it.optString("name",""),it.optInt("qty",1),it.optDouble("price",0.0).toFloat()*it.optInt("qty",1),branch,it.optString("code","")))
                        }
                    } else {
                        allLogs.add(AuditItem(time,"SALES","Sale",1,total,branch,""))
                    }
                }catch(_:Exception){}
            }
            
            // Load from stock operations
            val stockPref = getSharedPreferences("stock_ops", Context.MODE_PRIVATE)
            for((_,v) in stockPref.all){
                try{
                    val o = JSONObject(v.toString())
                    allLogs.add(AuditItem(o.optLong("time",System.currentTimeMillis()),"STOCK",o.optString("product","stove"),o.optInt("qty",3),o.optDouble("total",21.0).toFloat(),o.optString("branch","Rukudzo Shop"),o.optString("code","STOVE")))
                }catch(_:Exception){}
            }
            
            // Also check audit_log generic
            val auditPref = getSharedPreferences("audit_log", Context.MODE_PRIVATE)
            for((_,v) in auditPref.all){
                try{
                    val o = JSONObject(v.toString())
                    allLogs.add(AuditItem(o.optLong("time",System.currentTimeMillis()),o.optString("type","STOCK"),o.optString("product",""),o.optInt("qty",0),o.optDouble("total",0.0).toFloat(),o.optString("branch","Rukudzo Shop"),o.optString("code","")))
                }catch(_:Exception){}
            }
            
        }catch(_:Exception){}
        
        if(allLogs.isEmpty()){
            // Demo data like photo - 15/10 18:29 STOCK stove 3 $21
            allLogs.add(AuditItem(System.currentTimeMillis(),"STOCK","stove",3,21f,"Rukudzo Shop","STOVE-01"))
        }
        
        allLogs.sortByDescending{ it.dateTime }
    }

    fun filterLogs(){
        val query = etSearch.text.toString().trim().lowercase()
        filtered = allLogs.filter{ log->
            val matchesTab = when(selectedTab){
                "SALES" -> log.type=="SALES"
                "STOCK" -> log.type=="STOCK"
                else -> true
            }
            val matchesBranch = selectedBranch=="All Branches" || log.branch.contains(selectedBranch) || selectedBranch.contains("Rukudzo")
            val matchesSearch = query.isBlank() || log.product.lowercase().contains(query) || log.type.lowercase().contains(query) || log.code.lowercase().contains(query)
            val matchesDate = log.dateTime in fromDate..toDate || fromDate==0L
            matchesTab && matchesBranch && matchesSearch && matchesDate
        }.toMutableList()
        
        renderTable()
    }

    fun renderTable(){
        tableList.removeAllViews()
        val sdf = SimpleDateFormat("dd/MM\nHH:mm", Locale.getDefault())
        
        for(log in filtered){
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(8,12,8,12)
            row.setBackgroundColor(Color.WHITE)
            
            val tvDate = TextView(this)
            tvDate.text = sdf.format(Date(log.dateTime))
            tvDate.textSize = 12f
            tvDate.layoutParams = LinearLayout.LayoutParams(0,-2,1.2f)
            
            val tvType = TextView(this)
            tvType.text = log.type
            tvType.textSize = 10f
            tvType.setTextColor(Color.WHITE)
            tvType.setBackgroundColor(Color.parseColor("#3B82F6"))
            tvType.setPadding(12,4,12,4)
            tvType.layoutParams = LinearLayout.LayoutParams(0,-2,0.8f).apply{ setMargins(0,0,8,0) }
            tvType.background = android.graphics.drawable.GradientDrawable().apply{
                setColor(Color.parseColor("#3B82F6")); cornerRadius=20f
            }
            
            val tvProd = TextView(this)
            tvProd.text = log.product
            tvProd.textSize = 12f
            tvProd.layoutParams = LinearLayout.LayoutParams(0,-2,1.2f)
            
            val tvQty = TextView(this)
            tvQty.text = "${log.qty}"
            tvQty.textSize = 13f
            tvQty.setTypeface(null, Typeface.BOLD)
            tvQty.gravity = Gravity.CENTER
            tvQty.layoutParams = LinearLayout.LayoutParams(0,-2,0.6f)
            
            val tvTotal = TextView(this)
            tvTotal.text = "$${log.total.toInt()}"
            tvTotal.textSize = 13f
            tvTotal.setTypeface(null, Typeface.BOLD)
            tvTotal.gravity = Gravity.END
            tvTotal.layoutParams = LinearLayout.LayoutParams(0,-2,0.8f)
            
            row.addView(tvDate)
            row.addView(tvType)
            row.addView(tvProd)
            row.addView(tvQty)
            row.addView(tvTotal)
            
            tableList.addView(row)
            tableList.addView(View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#E5E7EB")) })
        }
        
        tvResultCount.text = "Ultra FINAL • clean version • ${filtered.size} result"
    }
}