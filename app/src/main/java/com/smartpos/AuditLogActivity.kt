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
    data class Log(val date:Date, val type:String, val product:String, val qty:Int, val total:Float, val branch:String)
    var allLogs=mutableListOf<Log>()
    var filtered=mutableListOf<Log>()
    lateinit var table: LinearLayout
    lateinit var etSearch: EditText
    lateinit var btnFrom: Button
    lateinit var btnTo: Button
    var fromDate: Date? = null
    var toDate: Date? = null
    var filterType="ALL"
    val sdf=SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val sdfFull=SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this); root.orientation=LinearLayout.VERTICAL; root.setBackgroundColor(Color.WHITE)
        root.addView(TextView(this).apply{ text="IJ Audit Trail - Ultra"; setBackgroundColor(Color.parseColor("#1F2937")); setTextColor(Color.WHITE); setPadding(12,10,12,10); setTypeface(null, Typeface.BOLD) })

        val dateRow=LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setPadding(8,8,8,8) }
        btnFrom=Button(this).apply{ text="FROM: SELECT DATE"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); setOnClickListener{ pickDate(true) } }
        btnTo=Button(this).apply{ text="TO: SELECT DATE"; layoutParams=LinearLayout.LayoutParams(0,-2,1f); setOnClickListener{ pickDate(false) } }
        dateRow.addView(btnFrom); dateRow.addView(btnTo); root.addView(dateRow)

        etSearch=EditText(this).apply{ hint="Search product, type, ID..."; setBackgroundColor(Color.parseColor("#F3F4F6")); setPadding(16,16,16,16) }
        root.addView(etSearch)

        val filterRow=LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setPadding(8,0,8,8) }
        for(t in listOf("ALL","SALES","STOCK")){
            val b=Button(this).apply{ text=t; layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)}; setOnClickListener{ filterType=t; filterLogs() } }
            filterRow.addView(b)
        }
        root.addView(filterRow)

        val scroll=ScrollView(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,0,1f) }
        table=LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL }
        scroll.addView(table); root.addView(scroll); setContentView(root)

        val cal=Calendar.getInstance(); toDate=cal.time; btnTo.text="TO: ${sdf.format(toDate!!)}"; cal.add(Calendar.DAY_OF_MONTH, -30); fromDate=cal.time; btnFrom.text="FROM: ${sdf.format(fromDate!!)}"

        etSearch.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filterLogs() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
        loadLogs(); filterLogs()
    }

    fun pickDate(isFrom:Boolean){
        val cal=Calendar.getInstance()
        DatePickerDialog(this, { _, y,m,d ->
            cal.set(y,m,d)
            if(isFrom){ fromDate=cal.time; btnFrom.text="FROM: ${sdf.format(fromDate!!)}" } else { cal.set(Calendar.HOUR_OF_DAY, 23); toDate=cal.time; btnTo.text="TO: ${sdf.format(cal.time)}" }
            filterLogs()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    fun loadLogs(){
        allLogs.clear()
        for(pn in listOf("sales","sales_history","pos_sales","transactions")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    try{
                        val o=JSONObject(v.toString()); val prod=o.optString("product_name", o.optString("name","Sale")); val qty=o.optInt("qty",1); val total=o.optDouble("total",0.0).toFloat()
                        val branch=o.optString("branch","All Branches"); val dateStr=o.optString("date", sdfFull.format(Date())); val date=try{ sdfFull.parse(dateStr) ?: Date() }catch(_:Exception){ Date() }
                        allLogs.add(Log(date, "SALES", prod, qty, total, branch))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        for(pn in listOf("received_stock","products","inventory")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    try{
                        val o=JSONObject(v.toString()); val prod=o.optString("name",""); if(prod.isBlank()) continue
                        var qty=0; for(qk in listOf("qnty","quantity","qty")){ if(o.has(qk)){ qty=o.optInt(qk,0); break } }
                        val cost=o.optDouble("cost",0.0).toFloat(); val total=qty*cost; val branch=o.optString("branch","All Branches")
                        val dateStr=o.optString("date", sdfFull.format(Date())); val date=try{ sdfFull.parse(dateStr) ?: Date() }catch(_:Exception){ Date() }
                        allLogs.add(Log(date, "STOCK", prod, qty, total, branch))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        allLogs.sortByDescending{ it.date }
    }

    fun filterLogs(){
        val q=etSearch.text.toString().lowercase().trim()
        filtered = allLogs.filter{
            val typeOk=filterType=="ALL" || it.type==filterType
            val searchOk=q.isBlank() || it.product.lowercase().contains(q) || it.type.lowercase().contains(q)
            val dateOk=(fromDate==null || !it.date.before(fromDate)) && (toDate==null || !it.date.after(toDate))
            typeOk && searchOk && dateOk
        }.toMutableList()
        table.removeAllViews()
        if(filtered.isEmpty()){
            table.addView(TextView(this).apply{ text="No logs yet"; gravity=Gravity.CENTER; setPadding(0,40,0,0); setTextColor(Color.GRAY) })
        }else{
            for(log in filtered){
                val row=LinearLayout(this).apply{ setPadding(8,8,8,8); setBackgroundColor(Color.WHITE) }
                row.addView(TextView(this).apply{ text=sdfFull.format(log.date); textSize=10f; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                row.addView(TextView(this).apply{ text=log.type; textSize=10f; setTypeface(null, Typeface.BOLD); setTextColor(if(log.type=="SALES") Color.BLUE else Color.parseColor("#16A34A")); layoutParams=LinearLayout.LayoutParams(0,-2,0.6f) })
                row.addView(TextView(this).apply{ text=log.product; textSize=10f; layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
                row.addView(TextView(this).apply{ text="${log.qty}"; textSize=10f; layoutParams=LinearLayout.LayoutParams(0,-2,0.4f) })
                row.addView(TextView(this).apply{ text="$${log.total}"; textSize=10f; setTypeface(null, Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,0.6f) })
                table.addView(row)
            }
        }
    }
}