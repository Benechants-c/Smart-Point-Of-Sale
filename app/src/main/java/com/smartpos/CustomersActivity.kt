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

class CustomersActivity : Activity() {

    data class Cust(val code:String, val name:String, val phone:String, val bal:Float, val key:String)

    private var allCust = mutableListOf<Cust>()
    private var filtered = mutableListOf<Cust>()
    private lateinit var tableLayout: LinearLayout
    private lateinit var searchEt: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }

        val header = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20,16,20,16)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "Customers"; textSize = 15f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        })
        header.addView(Button(this).apply { text = "BACK"; setOnClickListener{ finish() } })
        root.addView(header)

        val searchBox = LinearLayout(this).apply {
            setBackgroundColor(Color.WHITE); setPadding(12,12,12,12)
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,8)}
        }
        searchEt = EditText(this).apply {
            hint = "Search Name Phone Code"; textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        }
        searchBox.addView(searchEt)
        val addBtn = Button(this).apply {
            text = "+ ADD"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE)
            setOnClickListener{ showAdd() }
        }
        searchBox.addView(addBtn)
        root.addView(searchBox)

        val th = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B")); setPadding(8,12,8,12); orientation = LinearLayout.HORIZONTAL
        }
        fun hd(t:String,w:Float): TextView {
            return TextView(this@CustomersActivity).apply { text=t; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,w); textSize=10f }
        }
        th.addView(hd("CODE",0.6f)); th.addView(hd("NAME",1f)); th.addView(hd("PHONE",0.8f)); th.addView(hd("BAL",0.6f)); th.addView(hd("VIEW",0.6f))
        root.addView(th)

        val scroll = ScrollView(this)
        tableLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
        scroll.addView(tableLayout)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))

        setContentView(root)

        loadCustomers()
        searchEt.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter() }
            override fun beforeTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}
            override fun onTextChanged(s:CharSequence?,a:Int,b:Int,c:Int){}
        })
    }

    private fun loadCustomers(){
        allCust.clear()
        try{
            val pref = getSharedPreferences("customers", Context.MODE_PRIVATE)
            for((k,v) in pref.all){
                try{
                    var name=k; var phone=""; var bal=0f; var code=k
                    val raw = v.toString()
                    if(raw.trim().startsWith("{")){
                        val o = JSONObject(raw)
                        name=o.optString("name",k); phone=o.optString("phone",""); bal=o.optDouble("balance",0.0).toFloat(); code=o.optString("code",k)
                    } else {
                        val p=raw.split("|"); name=p.getOrNull(0)?:k; phone=p.getOrNull(1)?:""; bal=p.getOrNull(2)?.toFloatOrNull()?:0f; code=p.getOrNull(3)?:k
                    }
                    allCust.add(Cust(code,name,phone,bal,k))
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        if(allCust.isEmpty()){
            allCust.add(Cust("CUST-01","Ruvheneko","0773996905",0f,"ruvheneko"))
            allCust.add(Cust("CUST-02","Tinashe","0773991234",50f,"tinashe"))
        }
        filter()
    }

    private fun filter(){
        val q = searchEt.text.toString().trim().lowercase()
        filtered = allCust.filter{ q.isEmpty() || it.name.lowercase().contains(q) || it.phone.contains(q) || it.code.lowercase().contains(q) }.toMutableList()
        tableLayout.removeAllViews()
        for((idx,c) in filtered.withIndex()){
            val bg = if(idx%2==0) Color.WHITE else Color.parseColor("#F0FDF4")
            val row = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setBackgroundColor(bg); setPadding(0,12,0,12); gravity=Gravity.CENTER_VERTICAL }
            fun cell(t:String,w:Float,col:Int=Color.BLACK): TextView {
                return TextView(this@CustomersActivity).apply { text=t; textSize=11f; setTextColor(col); gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,w) }
            }
            row.addView(cell(c.code,0.6f, Color.parseColor("#2563EB")))
            row.addView(cell(c.name,1f))
            row.addView(cell(c.phone,0.8f, Color.GRAY))
            row.addView(cell("$${c.bal.toInt()}",0.6f, if(c.bal>0) Color.parseColor("#DC2626") else Color.parseColor("#16A34A")))
            val b = Button(this).apply { text="VIEW"; textSize=9f; layoutParams=LinearLayout.LayoutParams(0,-2,0.6f); setOnClickListener{ showDetail(c) } }
            row.addView(b)
            tableLayout.addView(row)
        }
    }

    private fun showAdd(){
        val lay = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(20,20,20,20) }
        val nameEt = EditText(this).apply { hint="Name" }
        val phoneEt = EditText(this).apply { hint="Phone" }
        val codeEt = EditText(this).apply { hint="Code"; setText("CUST-${allCust.size+1}") }
        lay.addView(nameEt); lay.addView(phoneEt); lay.addView(codeEt)
        android.app.AlertDialog.Builder(this).setTitle("Add Customer").setView(lay)
           .setPositiveButton("SAVE"){_,_->
                val name=nameEt.text.toString().trim(); if(name.isEmpty()) return@setPositiveButton
                val phone=phoneEt.text.toString().trim(); val code=codeEt.text.toString().trim()
                val obj = JSONObject().put("name",name).put("phone",phone).put("code",code).put("balance",0)
                getSharedPreferences("customers", Context.MODE_PRIVATE).edit().putString(code, obj.toString()).apply()
                loadCustomers()
            }.setNegativeButton("CANCEL",null).show()
    }

    private fun showDetail(c:Cust){
        android.app.AlertDialog.Builder(this).setTitle(c.name).setMessage("CODE: ${c.code}\nPhone: ${c.phone}\nBalance: $${c.bal}").setPositiveButton("OK",null).show()
    }
}