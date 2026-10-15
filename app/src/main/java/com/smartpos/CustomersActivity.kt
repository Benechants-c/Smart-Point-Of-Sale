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

    data class Cust(val code:String, val name:String, val phone:String, val address:String, val balance:Float, val type:String, val key:String)

    private var allCust = mutableListOf<Cust>()
    private var filtered = mutableListOf<Cust>()
    private lateinit var tableLayout: LinearLayout
    private lateinit var searchEt: EditText
    private lateinit var tvTotal: TextView
    private lateinit var tvCredit: TextView
    private lateinit var tvTop: TextView

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
            text = "👥 Customers - Credit & Loyalty"
            textSize = 15f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        })
        header.addView(Button(this).apply { text = "BACK"; textSize = 11f; setOnClickListener{ finish() } })
        root.addView(header)

        // SEARCH
        val searchBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); setPadding(12,12,12,12)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,8)}; gravity = Gravity.CENTER_VERTICAL
        }
        searchBox.addView(TextView(this).apply { text = "🔍"; textSize = 16f; setPadding(0,0,8,0) })
        searchEt = EditText(this).apply {
            hint = "Search by Name, Phone or Code..."; textSize = 13f
            setBackgroundColor(Color.parseColor("#F8FAFC")); setPadding(16,14,16,14)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        }
        searchBox.addView(searchEt)
        searchBox.addView(Button(this).apply {
            text = "+ ADD"; textSize = 11f; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE)
            setOnClickListener{ showAddDialog() }
        })
        root.addView(searchBox)

        // SUMMARY CARDS
        val grid = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun card(title:String, holder:(TextView)->Unit, bg:String, col:String): LinearLayout {
            val c = LinearLayout(this@CustomersActivity).apply {
                setBackgroundColor(Color.parseColor(bg)); setPadding(12,12,12,12)
                layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,4,4,4)}
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            }
            c.addView(TextView(this@CustomersActivity).apply { text = title; textSize = 9f; setTextColor(Color.GRAY); gravity = Gravity.CENTER })
            val tv = TextView(this@CustomersActivity).apply { text = "0"; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor(col)); gravity = Gravity.CENTER }
            holder(tv); c.addView(tv); return c
        }
        grid.addView(card("👥 TOTAL", {tvTotal=it}, "#FFFFFF", "#0F172A"))
        grid.addView(card("💳 CREDIT OWES", {tvCredit=it}, "#FEF2F2", "#DC2626"))
        grid.addView(card("⭐ TOP CUSTOMER", {tvTop=it}, "#FEF3C7", "#D97706"))
        root.addView(grid)

        // TABLE HEADER
        val th = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#1E293B")); setPadding(8,12,8,12)
        }
        fun hd(t:String,w:Float): TextView {
            return TextView(this@CustomersActivity).apply { text=t; textSize=9f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0,-2,w) }
        }
        th.addView(hd("CODE",0.6f)); th.addView(hd("NAME",1f)); th.addView(hd("PHONE",0.8f)); th.addView(hd("BALANCE",0.6f)); th.addView(hd("VIEW",0.6f))
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
                    val raw = v.toString()
                    var name=k; var phone=""; var addr=""; var bal=0f; var code=k; var type="Cash"
                    if(raw.trim().startsWith("{")){
                        val o = JSONObject(raw)
                        name=o.optString("name",k); phone=o.optString("phone",""); addr=o.optString("address","")
                        bal=o.optDouble("balance",0.0).toFloat(); code=o.optString("code",k); type=o.optString("type","Cash")
                    } else {
                        val p=raw.split("|"); name=p.getOrNull(0)?:k; phone=p.getOrNull(1)?:""; bal=p.getOrNull(2)?.toFloatOrNull()?:0f; code=p.getOrNull(3)?:k
                    }
                    allCust.add(Cust(code, name, phone, addr, bal, type, k))
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        if(allCust.isEmpty()){
            allCust.add(Cust("CUST-01","Ruvheneko","0773996905","Karoi",0f,"Cash","ruvheneko"))
            allCust.add(Cust("CUST-02","Tinashe","0773991234","Harare",50f,"Credit","tinashe"))
            allCust.add(Cust("CUST-03","Mama Shop","0773000000","Chinhoyi",0f,"Cash","mama"))
        }
        filter()
    }

    private fun filter(){
        val q = searchEt.text.toString().trim().lowercase()
        filtered = allCust.filter{
            q.isEmpty() || it.name.lowercase().contains(q) || it.phone.contains(q) || it.code.lowercase().contains(q)
        }.toMutableList()
        render()
    }

    private fun render(){
        tableLayout.removeAllViews()
        tvTotal.text = "${allCust.size}"
        val creditOwe = allCust.filter{it.balance>0}.sumOf{it.balance.toDouble()}.toFloat()
        tvCredit.text = "$${creditOwe.toInt()} (${allCust.count{it.balance>0}} owe)"
        tvTop.text = allCust.maxByOrNull{it.balance}?.name?.take(10) ?: "None"

        if(filtered.isEmpty()){
            tableLayout.addView(TextView(this).apply{ text="No customers for '${searchEt.text}'\nTap + ADD"; gravity=Gravity.CENTER; setPadding(20,40,20,40); setTextColor(Color.GRAY) })
            return
        }
        for((idx,c) in filtered.withIndex()){
            val bg = if(idx%2==0) Color.WHITE else Color.parseColor("#F0FDF4")
            val row = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setBackgroundColor(bg); setPadding(0,12,0,12); gravity=Gravity.CENTER_VERTICAL }
            fun cell(t:String,w:Float,col:Int=Color.BLACK, bold:Boolean=false): TextView {
                return TextView(this@CustomersActivity).apply { text=t; textSize=11f; setTextColor(col); gravity=Gravity.CENTER; if(bold) setTypeface(null, Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,w) }
            }
            row.addView(cell(c.code.take(8),0.6f, Color.parseColor("#2563EB"), true))
            row.addView(cell(c.name.take(10),1f, Color.BLACK, true))
            row.addView(cell(c.phone.take(10),0.8f, Color.GRAY))
            row.addView(cell(if(c.balance>0) "$${c.balance.toInt()} owes" else "$0",0.6f, if(c.balance>0) Color.parseColor("#DC2626") else Color.parseColor("#16A34A")))
            val btn = Button(this).apply {
                text="VIEW"; textSize=9f; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)
                layoutParams=LinearLayout.LayoutParams(0,-2,0.6f).apply{setMargins(4,0,4,0)}
                setOnClickListener{ showDetail(c) }
            }
            row.addView(btn)
            tableLayout.addView(row)
            tableLayout.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams=LinearLayout.LayoutParams(-1,1) })
        }
    }

    private fun showAddDialog(){
        val lay = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(20,20,20,20) }
        val nameEt = EditText(this).apply { hint="Customer Name *"; setPadding(16,12,16,12) }
        val phoneEt = EditText(this).apply { hint="Phone e.g. 0773996905"; setPadding(16,12,16,12) }
        val codeEt = EditText(this).apply { hint="Code e.g. CUST-04"; setPadding(16,12,16,12); setText("CUST-${allCust.size+1}") }
        val balEt = EditText(this).apply { hint="Opening Balance $ (0 if cash)"; setPadding(16,12,16,12); inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL }
        lay.addView(nameEt); lay.addView(phoneEt); lay.addView(codeEt); lay.addView(balEt)
        android.app.AlertDialog.Builder(this).setTitle("Add Customer").setView(lay)
           .setPositiveButton("SAVE"){_,_->
                val name=nameEt.text.toString().trim(); if(name.isEmpty()) return@setPositiveButton
                val phone=phoneEt.text.toString().trim(); val code=codeEt.text.toString().trim().ifEmpty{name.take(4).uppercase()+"-${allCust.size+1}"}
                val bal=balEt.text.toString().toFloatOrNull()?:0f
                val obj = JSONObject().put("name",name).put("phone",phone).put("code",code).put("balance",bal).put("type", if(bal>0) "Credit" else "Cash")
                getSharedPreferences("customers", Context.MODE_PRIVATE).edit().putString(code, obj.toString()).apply()
                Toast.makeText(this,"Customer $name added CODE $code", Toast.LENGTH_SHORT).show()
                loadCustomers()
            }.setNegativeButton("CANCEL",null).show()
    }

    private fun showDetail(c:Cust){
        val lay = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(20,20,20,20) }
        lay.addView(TextView(this).apply{ text="CODE: ${c.code}"; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#2563EB")) })
        lay.addView(TextView(this).apply{ text="Name: ${c.name}"; textSize=14f; setTypeface(null, Typeface.BOLD) })
        lay.addView(TextView(this).apply{ text="Phone: ${c.phone}"; textSize=12f })
        lay.addView(TextView(this).apply{ text="Balance: $${c.balance} ${if(c.balance>0) "owes you" else "paid"}"; setTextColor(if(c.balance>0) Color.parseColor("#DC2626") else Color.parseColor("#16A34A")); setTypeface(null, Typeface.BOLD); setPadding(0,12,0,12) })
        lay.addView(TextView(this).apply{ text="Branch: Main Shop\nTotal Purchases: $${(c.balance+200).toInt()}\nLast Sale: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}"; textSize=11f; setTextColor(Color.GRAY) })
        android.app.AlertDialog.Builder(this).setTitle("Customer - ${c.name}").setView(lay)
           .setPositiveButton("ADD PAYMENT"){_,_->
                val et = EditText(this).apply{ hint="Payment amount $"; inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL; setPadding(20,16,20,16) }
                android.app.AlertDialog.Builder(this).setTitle("Payment for ${c.name} - owes $${c.balance}").setView(et)
                   .setPositiveButton("PAY"){_,_->
                        val pay = et.text.toString().toFloatOrNull()?:0f
                        val newBal = (c.balance - pay).coerceAtLeast(0f)
                        try{
                            val pref = getSharedPreferences("customers", Context.MODE_PRIVATE)
                            val o = JSONObject(pref.getString(c.key,pref.getString(c.code,"{}")?:"{}")?:"{}")
                            o.put("balance",newBal)
                            pref.edit().putString(c.key, o.toString()).putString(c.code, o.toString()).apply()
                            Toast.makeText(this,"Paid $$pay - New balance $$newBal", Toast.LENGTH_SHORT).show()
                            loadCustomers()
                        }catch(_:Exception){}
                    }.setNegativeButton("CANCEL",null).show()
            }
           .setNegativeButton("CLOSE",null).show()
    }
}