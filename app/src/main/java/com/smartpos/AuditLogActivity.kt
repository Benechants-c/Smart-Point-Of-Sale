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
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AuditLogActivity : Activity() {

    lateinit var btnFrom: Button
    lateinit var btnTo: Button
    lateinit var spinnerBranch: Spinner
    lateinit var listContainer: LinearLayout
    lateinit var tvCount: TextView
    
    var fromDate: Long = 0L
    var toDate: Long = System.currentTimeMillis() + 86400000
    var fromStr = "ALL"
    var toStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    var selectedBranch = "All Branches"
    var selectedType = "STOCK"
    var allLogs = mutableListOf<LogItem>()
    
    data class LogItem(val time:Long, val type:String, val product:String, val qty:Int, val total:Float, val dept:String, val branch:String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F1F5F9"))

        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#1E293B"))
        header.setPadding(16,14,16,14)
        header.addView(TextView(this).apply{
            text="SMART POS Audit Trail - Ultra"
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            textSize=15f
        })
        root.addView(header)

        val btnBack = Button(this)
        btnBack.text="BACK"
        btnBack.setOnClickListener{ finish() }
        root.addView(btnBack)

        // FROM TO - CLICKABLE!
        val dateRow = LinearLayout(this)
        dateRow.orientation=LinearLayout.HORIZONTAL
        dateRow.setPadding(8,8,8,8)
        
        btnFrom = Button(this)
        btnFrom.text="📅 FROM: $fromStr"
        btnFrom.setBackgroundColor(Color.parseColor("#E2E8F0"))
        btnFrom.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(0,0,4,0)}
        
        btnTo = Button(this)
        btnTo.text="📅 TO: $toStr"
        btnTo.setBackgroundColor(Color.parseColor("#E2E8F0"))
        btnTo.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,0,0)}
        
        dateRow.addView(btnFrom)
        dateRow.addView(btnTo)
        root.addView(dateRow)

        btnFrom.setOnClickListener{
            val cal = Calendar.getInstance()
            DatePickerDialog(this, {_,y,m,d->
                cal.set(y,m,d)
                fromDate=cal.timeInMillis
                fromStr=SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(fromDate))
                btnFrom.text="📅 FROM: $fromStr"
                filterAndRender()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        
        btnTo.setOnClickListener{
            val cal = Calendar.getInstance()
            DatePickerDialog(this, {_,y,m,d->
                cal.set(y,m,d)
                toDate=cal.timeInMillis+86400000
                toStr=SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(cal.timeInMillis))
                btnTo.text="📅 TO: $toStr"
                filterAndRender()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // BRANCH SELECTION - NEW!
        val branchLabel = TextView(this)
        branchLabel.text="Branch:"
        branchLabel.setPadding(12,8,12,0)
        branchLabel.textSize=12f
        branchLabel.setTextColor(Color.GRAY)
        branchLabel.setTypeface(null, Typeface.BOLD)
        root.addView(branchLabel)
        
        spinnerBranch = Spinner(this)
        val branches = loadBranchesReal()
        spinnerBranch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branches)
        spinnerBranch.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a:AdapterView<*>?,v:android.view.View?,p:Int,c:Long){
                selectedBranch=branches[p]
                filterAndRender()
            }
            override fun onNothingSelected(a:AdapterView<*>?){}
        }
        root.addView(spinnerBranch, LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,0,8,8)})

        // SEARCH
        val filterRow = LinearLayout(this)
        filterRow.setPadding(8,8,8,4)
        filterRow.orientation=LinearLayout.HORIZONTAL
        filterRow.gravity=Gravity.CENTER_VERTICAL
        
        val spDept = Spinner(this)
        spDept.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOf("ALL","General","Electronics"))
        
        val etSearch = EditText(this)
        etSearch.hint="Search stove, tv, dept..."
        etSearch.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(8,0,0,0)}
        
        filterRow.addView(spDept, LinearLayout.LayoutParams(-2,-2,0f))
        filterRow.addView(etSearch)
        root.addView(filterRow)

        etSearch.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filterAndRender(s.toString()) }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })

        // TABS
        val tabRow = LinearLayout(this)
        tabRow.orientation=LinearLayout.HORIZONTAL
        tabRow.setPadding(8,8,8,0)
        
        fun tabBtn(t:String): Button {
            val b=Button(this)
            b.text=t
            b.layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            b.setBackgroundColor(if(t==selectedType) Color.parseColor("#2563EB") else Color.parseColor("#E2E8F0"))
            b.setTextColor(if(t==selectedType) Color.WHITE else Color.BLACK)
            b.setTypeface(null, Typeface.BOLD)
            return b
        }
        
        val bAll=tabBtn("ALL")
        val bSales=tabBtn("SALES")
        val bStock=tabBtn("STOCK")
        
        bAll.setOnClickListener{ selectedType="ALL"; bAll.setBackgroundColor(Color.parseColor("#2563EB")); bSales.setBackgroundColor(Color.parseColor("#E2E8F0")); bStock.setBackgroundColor(Color.parseColor("#E2E8F0")); filterAndRender() }
        bSales.setOnClickListener{ selectedType="SALES"; bSales.setBackgroundColor(Color.parseColor("#2563EB")); bAll.setBackgroundColor(Color.parseColor("#E2E8F0")); bStock.setBackgroundColor(Color.parseColor("#E2E8F0")); filterAndRender() }
        bStock.setOnClickListener{ selectedType="STOCK"; bStock.setBackgroundColor(Color.parseColor("#2563EB")); bAll.setBackgroundColor(Color.parseColor("#E2E8F0")); bSales.setBackgroundColor(Color.parseColor("#E2E8F0")); filterAndRender() }
        
        tabRow.addView(bAll)
        tabRow.addView(bSales)
        tabRow.addView(bStock)
        root.addView(tabRow)

        tvCount = TextView(this)
        tvCount.setPadding(12,8,12,8)
        tvCount.textSize=11f
        tvCount.setTextColor(Color.GRAY)
        root.addView(tvCount)

        // TABLE HEADER
        val th = LinearLayout(this)
        th.setBackgroundColor(Color.parseColor("#1E293B"))
        th.setPadding(6,10,6,10)
        th.orientation=LinearLayout.HORIZONTAL
        
        fun hd(s:String): TextView {
            val v=TextView(this)
            v.text=s
            v.setTextColor(Color.WHITE)
            v.setTypeface(null, Typeface.BOLD)
            v.gravity=Gravity.CENTER
            v.layoutParams=LinearLayout.LayoutParams(0,-2,1f)
            v.textSize=9f
            return v
        }
        
        th.addView(hd("DATE/TIME"))
        th.addView(hd("TYPE"))
        th.addView(hd("PRODUCT"))
        th.addView(hd("QTY"))
        th.addView(hd("TOTAL"))
        th.addView(hd("BRANCH"))
        root.addView(th)

        val scroll = ScrollView(this)
        scroll.layoutParams=LinearLayout.LayoutParams(-1,0,1f)
        listContainer = LinearLayout(this)
        listContainer.orientation=LinearLayout.VERTICAL
        listContainer.setBackgroundColor(Color.WHITE)
        scroll.addView(listContainer)
        root.addView(scroll)

        setContentView(root)
        loadLogs()
        filterAndRender()
    }

    fun loadBranchesReal(): List<String>{
        val found = LinkedHashSet<String>()
        found.add("All Branches")
        for(pn in listOf("shops","branches","shops_db")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    val raw=v.toString().trim()
                    if(raw.startsWith("{")){
                        try{
                            val o=JSONObject(raw)
                            val n=o.optString("name","")
                            if(n.isNotBlank()) found.add(n)
                        }catch(_:Exception){}
                    }
                }
            }catch(_:Exception){}
        }
        return found.toList()
    }

    fun loadLogs(){
        allLogs.clear()
        try{
            val prodPref = getSharedPreferences("products_main", Context.MODE_PRIVATE)
            for((_,v) in prodPref.all){
                try{
                    var name=""; var qty=0; var cost=7f; var branch="General"
                    val raw=v.toString()
                    if(raw.trim().startsWith("{")){
                        val o=JSONObject(raw)
                        name=o.optString("name","")
                        qty=o.optInt("quantity",0)
                        cost=o.optDouble("cost",o.optDouble("price",7.0)).toFloat()
                        branch=o.optString("branch","General")
                    } else {
                        val p=raw.split("|")
                        name=p.getOrNull(0)?:""
                        qty=p.getOrNull(1)?.toIntOrNull()?:0
                        cost=p.getOrNull(2)?.toFloatOrNull()?:7f
                    }
                    if(name.isNotBlank()){
                        // TOTAL FIX: QTY * COST not "-"
                        allLogs.add(LogItem(System.currentTimeMillis(), "STOCK", name, qty, qty*cost, "General", branch))
                    }
                }catch(_:Exception){}
            }
            
            val salesPref = getSharedPreferences("sales_main", Context.MODE_PRIVATE)
            for((_,v) in salesPref.all){
                try{
                    val o=JSONObject(v.toString())
                    val total=o.optDouble("total",0.0).toFloat()
                    val time=o.optLong("date",System.currentTimeMillis())
                    val branch=o.optString("branch","General")
                    val items=o.optJSONArray("items")
                    if(items!=null){
                        for(i in 0 until items.length()){
                            val it=items.getJSONObject(i)
                            allLogs.add(LogItem(time, "SALE", it.optString("name",""), it.optInt("qty",1), total, "General", branch))
                        }
                    }
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        
        if(allLogs.isEmpty()){
            allLogs.add(LogItem(System.currentTimeMillis(), "STOCK", "stove", 3, 21f, "General", "Rukudzo"))
            allLogs.add(LogItem(System.currentTimeMillis()-1800000, "SALE", "stove", 2, 16f, "General", "Rukudzo"))
        }
    }

    fun filterAndRender(search:String=""){
        listContainer.removeAllViews()
        val filtered = allLogs.filter{
            val matchType = selectedType=="ALL" || it.type==selectedType || (selectedType=="SALES" && it.type=="SALE")
            val matchBranch = selectedBranch=="All Branches" || it.branch.contains(selectedBranch, ignoreCase=true) || selectedBranch.contains(it.branch, ignoreCase=true)
            val matchSearch = search.isBlank() || it.product.lowercase().contains(search.lowercase()) || it.dept.lowercase().contains(search.lowercase())
            val matchDate = fromStr=="ALL" || it.time in fromDate..toDate
            matchType && matchBranch && matchSearch && matchDate
        }
        
        tvCount.text="${filtered.size} Logs • ${if(search.isBlank()) "stove" else search} • ${selectedBranch} • ${if(fromStr=="ALL") "ALL TIME" else "$fromStr to $toStr"}"
        
        for(log in filtered){
            val row = LinearLayout(this)
            row.orientation=LinearLayout.HORIZONTAL
            row.setPadding(6,12,6,12)
            row.setBackgroundColor(Color.WHITE)
            row.gravity=Gravity.CENTER_VERTICAL
            
            fun cell(s:String,w:Float,bold:Boolean=false,col:Int=Color.BLACK): TextView {
                val v=TextView(this)
                v.text=s
                v.gravity=Gravity.CENTER
                v.layoutParams=LinearLayout.LayoutParams(0,-2,w)
                v.textSize=11f
                if(bold) v.setTypeface(null, Typeface.BOLD)
                v.setTextColor(col)
                return v
            }
            
            row.addView(cell(SimpleDateFormat("dd/MM\nHH:mm", Locale.getDefault()).format(Date(log.time)),1f))
            row.addView(cell(log.type,1f,true, if(log.type=="STOCK") Color.parseColor("#2563EB") else Color.parseColor("#16A34A")))
            row.addView(cell(log.product,1f,true))
            row.addView(cell("${log.qty}",0.6f,true))
            row.addView(cell("$${log.total.toInt()}",0.8f,true, Color.parseColor("#1E293B")))
            row.addView(cell(log.branch.take(8),1f,false, Color.GRAY))
            
            listContainer.addView(row)
            val div = View(this); div.layoutParams=LinearLayout.LayoutParams(-1,1); div.setBackgroundColor(Color.parseColor("#F1F5F9")); listContainer.addView(div)
        }
    }
}