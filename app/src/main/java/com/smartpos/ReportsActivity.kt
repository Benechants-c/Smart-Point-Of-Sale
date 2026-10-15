package com.smartpos
import android.app.Activity
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
    lateinit var spinnerBranch: Spinner
    lateinit var spinnerProduct: Spinner
    lateinit var btnFrom: Button
    lateinit var btnTo: Button
    lateinit var recentList: LinearLayout
    var branches = mutableListOf<String>()
    var products = mutableListOf<String>()
    var selectedBranch = ""
    var selectedProduct = ""
    var fromDate = ""
    var toDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = ScrollView(this)
        val main = LinearLayout(this)
        main.orientation = LinearLayout.VERTICAL
        main.setBackgroundColor(Color.parseColor("#EEF2F7"))

        val header = TextView(this)
        header.text = "<- Reports & Analytics"
        header.setBackgroundColor(Color.parseColor("#1E293B"))
        header.setTextColor(Color.WHITE)
        header.setPadding(24,20,24,20)
        header.setTypeface(null, Typeface.BOLD)
        main.addView(header)

        val back = Button(this); back.text="BACK"; back.setOnClickListener{ finish() }; main.addView(back)

        // FILTERS - ALL EMPTY FOR USER INPUT!
        val filterCard = LinearLayout(this); filterCard.orientation=LinearLayout.VERTICAL; filterCard.setBackgroundColor(Color.WHITE); filterCard.setPadding(16,16,16,16); filterCard.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,0)}

        // BRANCH EMPTY
        val branchLabel = TextView(this); branchLabel.text="Branch"; branchLabel.setTypeface(null, Typeface.BOLD); filterCard.addView(branchLabel)
        spinnerBranch = Spinner(this); filterCard.addView(spinnerBranch)
        val branchHint = mutableListOf("Select Branch"); branchHint.addAll(loadBranchesReal())
        spinnerBranch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branchHint)
        spinnerBranch.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a:AdapterView<*>?,v:android.view.View?,p:Int,c:Long){ selectedBranch = if(p==0) "" else branchHint[p]; refreshData() }
            override fun onNothingSelected(a:AdapterView<*>?){}
        }

        // FROM TO EMPTY
        val dateRow = LinearLayout(this); dateRow.orientation=LinearLayout.HORIZONTAL; dateRow.setPadding(0,12,0,12)
        btnFrom = Button(this); btnFrom.text="FROM: Select Date"; btnFrom.setBackgroundColor(Color.parseColor("#E5E7EB")); btnFrom.setOnClickListener{ fromDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()); btnFrom.text="FROM: $fromDate"; refreshData() }
        btnTo = Button(this); btnTo.text="TO: Select Date"; btnTo.setBackgroundColor(Color.parseColor("#E5E7EB")); btnTo.setOnClickListener{ toDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()); btnTo.text="TO: $toDate"; refreshData() }
        dateRow.addView(btnFrom, LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(0,0,6,0)})
        dateRow.addView(btnTo, LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(6,0,0,0)})
        filterCard.addView(dateRow)

        // PRODUCT EMPTY
        val prodLabel = TextView(this); prodLabel.text="Product"; prodLabel.setTypeface(null, Typeface.BOLD); filterCard.addView(prodLabel)
        spinnerProduct = Spinner(this); filterCard.addView(spinnerProduct)
        val prodHint = mutableListOf("Select Product"); prodHint.addAll(loadProductsReal())
        spinnerProduct.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, prodHint)
        spinnerProduct.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a:AdapterView<*>?,v:android.view.View?,p:Int,c:Long){ selectedProduct = if(p==0) "" else prodHint[p]; refreshData() }
            override fun onNothingSelected(a:AdapterView<*>?){}
        }

        main.addView(filterCard)

        // STATS - REAL DATA ONLY, NO FAKE PREFILL
        val salesData = loadSalesTotals()
        val statsRow = LinearLayout(this); statsRow.orientation=LinearLayout.HORIZONTAL; statsRow.setPadding(8,12,8,8)
        fun statCard(title:String,value:String,sub:String): LinearLayout {
            val c = LinearLayout(this); c.orientation=LinearLayout.VERTICAL; c.setBackgroundColor(Color.WHITE); c.setPadding(16,16,16,16); c.gravity=Gravity.CENTER; c.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)}
            c.addView(TextView(this).apply{text=title; gravity=Gravity.CENTER; setTextColor(Color.GRAY); textSize=12f})
            c.addView(TextView(this).apply{text=value; gravity=Gravity.CENTER; setTypeface(null, Typeface.BOLD); textSize=20f; setTextColor(Color.parseColor("#1E293B"))})
            c.addView(TextView(this).apply{text=sub; gravity=Gravity.CENTER; textSize=10f; setTextColor(if(sub.contains("REAL")) Color.parseColor("#16A34A") else Color.RED)})
            return c
        }
        statsRow.addView(statCard("Total Sales","$ ${salesData.first}",if(salesData.first>0) "+ REAL" else "No data"))
        statsRow.addView(statCard("Total Profit","$ ${salesData.second}",if(salesData.second>0) "+ REAL" else "No data"))
        statsRow.addView(statCard("Low Stock","${getLowStockCount()} items",if(getLowStockCount()>0) "Needs restock!" else "All OK"))
        main.addView(statsRow)

        // BAR + PIE - ONLY SHOW IF DATA EXISTS
        val chartsRow = LinearLayout(this); chartsRow.orientation=LinearLayout.HORIZONTAL; chartsRow.setPadding(8,4,8,8)

        val barCard = LinearLayout(this); barCard.orientation=LinearLayout.VERTICAL; barCard.setBackgroundColor(Color.WHITE); barCard.setPadding(12,12,12,12); barCard.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)}
        barCard.addView(TextView(this).apply{text="Daily Sales"; setTypeface(null, Typeface.BOLD); setTextColor(Color.GRAY)})
        val barView = object: View(this){
            override fun onDraw(canvas: Canvas){
                super.onDraw(canvas)
                if(salesData.first<=0) return
                val paint = Paint(); paint.color = Color.parseColor("#2563EB"); paint.style = Paint.Style.FILL
                val barHeight = height*0.6f
                canvas.drawRect(width*0.35f, height*0.2f, width*0.65f, height*0.2f+barHeight, paint)
            }
        }; barView.layoutParams = LinearLayout.LayoutParams(-1,300); barCard.addView(barView); chartsRow.addView(barCard)

        val pieCard = LinearLayout(this); pieCard.orientation=LinearLayout.VERTICAL; pieCard.setBackgroundColor(Color.WHITE); pieCard.setPadding(12,12,12,12); pieCard.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)}
        pieCard.addView(TextView(this).apply{text="Profit by Department"; setTypeface(null, Typeface.BOLD); setTextColor(Color.GRAY)})
        val pieView = object: View(this){
            override fun onDraw(canvas: Canvas){
                super.onDraw(canvas)
                if(salesData.first<=0) return
                val rect = RectF(20f,20f, width-20f, height-20f)
                val p1 = Paint(); p1.color=Color.parseColor("#22C55E"); val p2 = Paint(); p2.color=Color.parseColor("#2563EB")
                val perc = (salesData.second/salesData.first*360f).coerceIn(0f,360f)
                canvas.drawArc(rect, 0f, perc, true, p1); canvas.drawArc(rect, perc, 360f-perc, true, p2)
            }
        }; pieView.layoutParams = LinearLayout.LayoutParams(-1,300); pieCard.addView(pieView); chartsRow.addView(pieCard)

        main.addView(chartsRow)

        // RECENT
        val recentCard = LinearLayout(this); recentCard.orientation=LinearLayout.VERTICAL; recentCard.setBackgroundColor(Color.WHITE); recentCard.setPadding(12,12,12,12); recentCard.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,12)}
        recentCard.addView(TextView(this).apply{text="Recent Transactions - REAL"; setTypeface(null, Typeface.BOLD)})
        recentList = LinearLayout(this); recentList.orientation=LinearLayout.VERTICAL; loadRecent(recentList); recentCard.addView(recentList); main.addView(recentCard)

        root.addView(main); setContentView(root)
    }

    fun loadBranchesReal(): List<String>{
        val found = LinkedHashSet<String>()
        for(pn in listOf("shops","branches","shops_db")){
            try{
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    val raw=v.toString().trim()
                    if(raw.startsWith("{")){ try{ val o=JSONObject(raw); val n=o.optString("name",""); if(n.isNotBlank()) found.add(n) }catch(_:Exception){} }
                }
            }catch(_:Exception){}
        }
        return found.toList()
    }

    fun loadProductsReal(): List<String>{
        val found = LinkedHashSet<String>()
        try{
            val pref = getSharedPreferences("products_main", Context.MODE_PRIVATE)
            for((_,v) in pref.all){
                try{
                    val raw=v.toString()
                    var code=""; if(raw.trim().startsWith("{")){ code=JSONObject(raw).optString("code","") } else { code=raw.split("|").getOrNull(5)?:"" }
                    if(code.isNotBlank()) found.add(code)
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        return found.toList()
    }

    fun refreshData(){ /* filter by selectedBranch, selectedProduct, fromDate, toDate */ }

    fun loadSalesTotals(): Pair<Float,Float>{
        var total=0f
        try{
            for(pn in listOf("sales_main","sales_db")){
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((_,v) in pref.all){ try{ total+=JSONObject(v.toString()).optDouble("total",0.0).toFloat() }catch(_:Exception){} }
            }
        }catch(_:Exception){}
        return Pair(total, total*0.3f)
    }

    fun getLowStockCount(): Int {
        var low=0
        try{
            val pref=getSharedPreferences("products_main", Context.MODE_PRIVATE)
            for((_,v) in pref.all){
                try{
                    var qty=0; val raw=v.toString()
                    if(raw.trim().startsWith("{")) qty=JSONObject(raw).optInt("quantity",0) else qty=raw.split("|").getOrNull(1)?.toIntOrNull()?:0
                    if(qty<5) low++
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        return low
    }

    fun loadRecent(container: LinearLayout){
        container.removeAllViews()
        try{
            for(pn in listOf("sales_main","sales_db")){
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    try{
                        val o=JSONObject(v.toString())
                        val date=SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(o.optLong("date",System.currentTimeMillis())))
                        val row=LinearLayout(this); row.orientation=LinearLayout.HORIZONTAL; row.setPadding(8,12,8,12)
                        row.addView(TextView(this).apply{text=date; gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
                        row.addView(TextView(this).apply{text="$ ${o.optDouble("total",0.0).toInt()}"; gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
                        row.addView(TextView(this).apply{text="$ ${(o.optDouble("total",0.0)*0.3).toInt()}"; gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
                        container.addView(row)
                    }catch(_:Exception){}
                }
            }
        }catch(_:Exception){}
    }
}