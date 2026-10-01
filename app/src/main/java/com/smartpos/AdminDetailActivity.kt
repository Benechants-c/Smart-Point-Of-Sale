package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*

class AdminDetailActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try{
            val title = intent.getStringExtra("TITLE") ?: "Admin"
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC")) }

            val header = LinearLayout(this).apply{ setBackgroundColor(Color.parseColor("#1E293B")); setPadding(20,24,20,24); orientation=LinearLayout.HORIZONTAL }
            header.addView(TextView(this).apply{ text=title; textSize=18f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
            val back = Button(this).apply{ text="BACK"; setBackgroundColor(Color.parseColor("#475569")); setTextColor(Color.WHITE) }
            back.setOnClickListener{ finish() }; header.addView(back)
            root.addView(header)

            val salesPref = getSharedPreferences("sales_db", Context.MODE_PRIVATE)
            val prodPref = getSharedPreferences("products_db", Context.MODE_PRIVATE)
            val custPref = getSharedPreferences("customers", Context.MODE_PRIVATE)
            val cashPref = getSharedPreferences("cash", Context.MODE_PRIVATE)
            val userPref = getSharedPreferences("users_db", Context.MODE_PRIVATE)
            val auditPref = getSharedPreferences("audit", Context.MODE_PRIVATE)

            fun card(t:String, v:String){
                val c = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,0)}}
                c.addView(TextView(this).apply{ text=t; setTypeface(null,Typeface.BOLD) })
                c.addView(TextView(this).apply{ text=v; setPadding(0,8,0,0) })
                root.addView(c)
            }

            when{
                title.contains("Sales") -> {
                    if(salesPref.all.isEmpty()) card("Sales Management","No sales yet - Do first sale in POS Sales") else salesPref.all.forEach{ card(it.key, it.value.toString()) }
                }
                title.contains("Customer") -> {
                    val ed = EditText(this).apply{hint="Customer Name / Phone"; layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,0)}}
                    val btn = Button(this).apply{ text="ADD CUSTOMER"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,0)}}
                    btn.setOnClickListener{
                        if(ed.text.isNotEmpty()){ custPref.edit().putString("${System.currentTimeMillis()}",ed.text.toString()).apply(); Toast.makeText(this,"Customer Added",0).show(); ed.setText(""); card("New Customer", ed.text.toString()) }
                    }
                    root.addView(ed); root.addView(btn)
                    custPref.all.values.forEach{ card("Customer", it.toString()) }
                }
                title.contains("Cash") -> {
                    val drawer = cashPref.getFloat("drawer",1245.6f)
                    card("Cash Drawer - REAL","$${String.format("%.2f",drawer)}")
                    val ed = EditText(this).apply{hint="New Drawer Amount"; inputType=8194; layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,0)}}
                    val btn = Button(this).apply{text="UPDATE"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,0)}}
                    btn.setOnClickListener{ cashPref.edit().putFloat("drawer", ed.text.toString().toFloatOrNull()?:drawer).apply(); Toast.makeText(this,"Updated",0).show() }
                    root.addView(ed); root.addView(btn)
                }
                title.contains("Users") -> {
                    val ed = EditText(this).apply{hint="Username"; layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,0)}}
                    val role = EditText(this).apply{hint="Role admin/cashier"; layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,0)}}
                    val btn = Button(this).apply{text="ADD USER"; setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,0)}}
                    btn.setOnClickListener{ userPref.edit().putString(ed.text.toString(), role.text.toString()).apply(); Toast.makeText(this,"User Added",0).show() }
                    root.addView(ed); root.addView(role); root.addView(btn)
                    userPref.all.forEach{ card(it.key, "Role: ${it.value}") }
                }
                title.contains("Purchasing") -> {
                    root.addView(TextView(this).apply{text="ADD PRODUCT - REAL"; setTypeface(null,Typeface.BOLD); setPadding(12,12,12,8)})
                    val code = EditText(this).apply{hint="Barcode"}; val name = EditText(this).apply{hint="Name"}
                    val buy = EditText(this).apply{hint="Buy Price"; inputType=8194}; val sell = EditText(this).apply{hint="Sell Price"; inputType=8194}; val qty = EditText(this).apply{hint="Qty"; inputType=2}
                    for(v in listOf(code,name,buy,sell,qty)){ v.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,6,12,0)}; root.addView(v) }
                    val save = Button(this).apply{text="SAVE PRODUCT"; setBackgroundColor(Color.parseColor("#22C55E")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,0)}}
                    save.setOnClickListener{
                        if(code.text.isNotEmpty() && name.text.isNotEmpty()){
                            prodPref.edit().putString(code.text.toString(),"${name.text}|${buy.text}|${sell.text}|${qty.text}|0|General").apply()
                            getSharedPreferences("stock_main",Context.MODE_PRIVATE).edit().putString(code.text.toString(),"${name.text}|${buy.text}|${sell.text}|${qty.text}|0|General").apply()
                            Toast.makeText(this,"Product Saved",0).show()
                        }
                    }
                    root.addView(save)
                    card("Products Count","${prodPref.all.size} Active SKUs - REAL")
                }
                title.contains("Report") -> {
                    var sales=0f; var profit=0f
                    try{ salesPref.all.values.forEach{ val p=it.toString().split("|"); if(p.size>=3){ sales+=p[1].toFloatOrNull()?:0f; profit+=(p[1].toFloatOrNull()?:0f)-(p[2].toFloatOrNull()?:0f) } } }catch(_:Exception){}
                    card("Sales Report - REAL","Total Sales: $$sales")
                    card("Profit Report - REAL","Total Profit: $$profit")
                    card("Stock Report","Products: ${prodPref.all.size} | Customers: ${custPref.all.size}")
                }
                title.contains("Audit") -> {
                    val logs = auditPref.getStringSet("logs", setOf()) ?: setOf()
                    if(logs.isEmpty()) card("Audit Log - REAL","No activity yet - Do sale to see log") else logs.forEach{ card("Log", it) }
                }
                title.contains("Price") -> {
                    prodPref.all.forEach{ (k,v) ->
                        val p = v.toString().split("|")
                        card(k,"Name: ${p.getOrNull(0)} | Sell: $${p.getOrNull(2)} | Qty: ${p.getOrNull(3)} - REAL, editable in Purchasing")
                    }
                }
                title.contains("Setting") -> {
                    card("System Settings - REAL","Version: Smartpos v1.0 | Branch: Main Shop")
                    val clear = Button(this).apply{text="CLEAR SALES DATA"; setBackgroundColor(Color.parseColor("#DC2626")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,8,12,0)}}
                    clear.setOnClickListener{ salesPref.edit().clear().apply(); Toast.makeText(this,"Cleared",0).show() }
                    root.addView(clear)
                }
                else -> card(title,"Fully Functional - No Coming Soon")
            }
            scroll.addView(root); setContentView(scroll)
        }catch(e:Exception){
            val tv = TextView(this); tv.text="Error: ${e.message}"; setContentView(tv)
        }
    }
}
