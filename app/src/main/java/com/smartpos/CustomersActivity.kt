package com.smartpos
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import org.json.JSONObject

class CustomersActivity : Activity() {

    data class Customer(val code:String, val name:String, val phone:String, val balance:Float)

    var all = mutableListOf<Customer>()
    var filtered = mutableListOf<Customer>()
    lateinit var table: LinearLayout
    lateinit var etSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)

        root.addView(LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#111827"))
            setPadding(16,14,16,14)
            addView(TextView(this@CustomersActivity).apply{ text="SMART POS"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=16f })
        })

        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#1E3A8A"))
        header.setPadding(12,12,12,12)
        header.addView(TextView(this).apply{ text="Customers"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=14f; layoutParams=LinearLayout.LayoutParams(-2,-2) })
        header.addView(TextView(this).apply{ text=" · SEARCH by CODE"; setTextColor(Color.parseColor("#93C5FD")); textSize=12f; setPadding(8,0,0,0) })
        root.addView(header)

        // SEARCH - CODE, Name, Phone - e.g. CUST-01
        val searchBox = LinearLayout(this)
        searchBox.setBackgroundColor(Color.WHITE)
        searchBox.setPadding(12,12,12,12)
        searchBox.orientation=LinearLayout.HORIZONTAL
        searchBox.gravity=Gravity.CENTER_VERTICAL
        searchBox.elevation=4f
        etSearch = EditText(this)
        etSearch.hint="Search CODE, Name, Phone - e.g. CUST-01"
        etSearch.textSize=13f
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.layoutParams=LinearLayout.LayoutParams(-1,-2,1f)
        searchBox.addView(TextView(this).apply{ text="🔍"; textSize=14f })
        searchBox.addView(etSearch)
        root.addView(searchBox)

        // TABLE HEADER CODE NAME PHONE BAL
        val th = LinearLayout(this)
        th.setBackgroundColor(Color.parseColor("#E5E7EB"))
        th.setPadding(8,8,8,8)
        fun tht(t:String,w:Float):TextView{ val tv=TextView(this); tv.text=t; tv.textSize=10f; tv.setTypeface(null, Typeface.BOLD); tv.setTextColor(Color.parseColor("#6B7280")); tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv }
        th.addView(tht("CODE",0.7f)); th.addView(tht("NAME",1.0f)); th.addView(tht("PHONE",0.8f)); th.addView(tht("BAL",0.6f))
        root.addView(th)

        val scroll = ScrollView(this)
        scroll.layoutParams=LinearLayout.LayoutParams(-1,0,1f)
        table = LinearLayout(this)
        table.orientation=LinearLayout.VERTICAL
        scroll.addView(table)
        root.addView(scroll)

        // ADD CUSTOMER BUTTON - FIXED INTENT NAME
        val btnAdd = Button(this)
        btnAdd.text="ADD CUSTOMER"
        btnAdd.setBackgroundColor(Color.parseColor("#1E3A8A"))
        btnAdd.setTextColor(Color.WHITE)
        btnAdd.setTypeface(null, Typeface.BOLD)
        btnAdd.setPadding(0,20,0,20)
        btnAdd.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,0)}
        btnAdd.setOnClickListener{
            try{
                // FIX: CORRECT PACKAGE NAME - com.smartpos.AddCustomerActivity
                val intent = Intent(this, AddCustomerActivity::class.java)
                startActivity(intent)
            }catch(e:Exception){
                try{
                    // Fallback 2: try explicit name
                    val intent = Intent()
                    intent.setClassName("com.smartpos", "com.smartpos.AddCustomerActivity")
                    startActivity(intent)
                }catch(e2:Exception){
                    Toast.makeText(this,"AddCustomerActivity not found! Create file: com.smartpos.AddCustomerActivity", Toast.LENGTH_LONG).show()
                }
            }
        }
        root.addView(btnAdd)

        root.addView(Button(this).apply {
            text="BACK"; setBackgroundColor(Color.parseColor("#E5E7EB")); setPadding(0,18,0,18)
            setOnClickListener{ finish() }
        })

        setContentView(root)
        loadCustomers()
        render()

        etSearch.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    override fun onResume(){
        super.onResume()
        loadCustomers()
        filter()
    }

    fun loadCustomers(){
        all.clear()
        val prefs = listOf("customers","customer_list","customers_db","cust_list")
        for(pn in prefs){
            try{
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    try{
                        val raw = v.toString()
                        if(!raw.trim().startsWith("{")) continue
                        val o = JSONObject(raw)
                        val code = o.optString("code", o.optString("customer_code", k))
                        val name = o.optString("name", o.optString("customer_name", ""))
                        val phone = o.optString("phone", o.optString("phone_number", ""))
                        val bal = o.optDouble("balance", o.optDouble("bal", 0.0)).toFloat()
                        if(name.isNotBlank() && all.none{ it.code==code }) all.add(Customer(code, name, phone, bal))
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
    }

    fun filter(){
        val q = etSearch.text.toString().lowercase().trim()
        filtered = if(q.isBlank()) all else all.filter{ it.code.lowercase().contains(q) || it.name.lowercase().contains(q) || it.phone.lowercase().contains(q) }.toMutableList()
        render()
    }

    fun render(){
        table.removeAllViews()
        if(filtered.isEmpty()){
            val empty = LinearLayout(this)
            empty.orientation=LinearLayout.VERTICAL; empty.gravity=Gravity.CENTER; empty.setPadding(0,80,0,0)
            empty.addView(TextView(this).apply{ text="👥👥"; textSize=30f; gravity=Gravity.CENTER })
            empty.addView(TextView(this).apply{ text="No customers yet"; textSize=14f; setTypeface(null, Typeface.BOLD); gravity=Gravity.CENTER; setPadding(0,12,0,4) })
            empty.addView(TextView(this).apply{ text="Customers you add will appear here.\nSearch by code, name or phone."; textSize=11f; setTextColor(Color.parseColor("#9CA3AF")); gravity=Gravity.CENTER })
            table.addView(empty)
            return
        }
        for(c in filtered){
            val row = LinearLayout(this)
            row.setPadding(8,12,8,12)
            row.setBackgroundColor(Color.WHITE)
            fun td(t:String,w:Float):TextView{ val tv=TextView(this); tv.text=t; tv.textSize=11f; tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv }
            row.addView(td(c.code,0.7f).apply{ setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#2563EB")) })
            row.addView(td(c.name,1.0f).apply{ setTypeface(null, Typeface.BOLD) })
            row.addView(td(c.phone,0.8f))
            row.addView(td("$${c.balance}",0.6f).apply{ setTextColor(if(c.balance>0) Color.RED else Color.GRAY) })
            table.addView(row)
            table.addView(View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#F3F4F6")) })
        }
    }
}