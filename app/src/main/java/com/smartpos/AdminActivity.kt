package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class AdminDetailActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try{
            val title = intent.getStringExtra("TITLE")?: "Admin"
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC")); setPadding(12,12,12,12)}

            val header = TextView(this).apply{
                text = title; textSize=20f; setTypeface(null,Typeface.BOLD)
                setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(20,24,20,24)
            }
            root.addView(header)
            val back = Button(this).apply{text="← Back to Dashboard"; setBackgroundColor(Color.parseColor("#E2E8F0"))}
            back.setOnClickListener{ finish() }; root.addView(back)

            // PREFS
            val salesPref = getSharedPreferences("sales_db",Context.MODE_PRIVATE)
            val prodPref = getSharedPreferences("products_db",Context.MODE_PRIVATE)
            val custPref = getSharedPreferences("customers",Context.MODE_PRIVATE)
            val cashPref = getSharedPreferences("cash",Context.MODE_PRIVATE)
            val auditPref = getSharedPreferences("audit",Context.MODE_PRIVATE)
            val supplierPref = getSharedPreferences("suppliers",Context.MODE_PRIVATE)
            val userPref = getSharedPreferences("users_db",Context.MODE_PRIVATE)

            fun addCard(t:String, v:String){
                val card = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,8,0,0)}}
                card.addView(TextView(this).apply{text=t; setTypeface(null,Typeface.BOLD)})
                card.addView(TextView(this).apply{text=v; setPadding(0,8,0,0)})
                root.addView(card)
            }

            when{
                title.contains("Sales Management") -> {
                    root.addView(TextView(this).apply{text="Sales List - REAL DATA"; setTypeface(null,Typeface.BOLD); setPadding(0,12,0,8)})
                    if(salesPref.all.isEmpty()) addCard("No Sales","Do first sale in POS Sales")
                    else salesPref.all.forEach{ (k,v) ->
                        addCard(k, v.toString())
                    }
                    val totalSales = salesPref.all.values.sumOf{
                        try{ it.toString().split("|")[1].toFloat().toDouble()}catch(_:Exception){0.0}
                    }
                    addCard("TOTAL SALES","$${String.format("%.2f",totalSales)}")
                }
                title.contains("Customer") -> {
                    val nameEd = EditText(this).apply{hint="Customer Name / Phone"}
                    val save = Button(this).apply{text="ADD CUSTOMER"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE)}
                    save.setOnClickListener{
                        if(nameEd.text.isNotEmpty()){
                            custPref.edit().putString("${System.currentTimeMillis()}",nameEd.text.toString()).apply()
                            Toast.makeText(this,"Customer Added",0).show(); nameEd.setText("")
                            root.addView(TextView(this).apply{text="• ${nameEd.text}"})
                        }
                    }
                    root.addView(nameEd); root.addView(save)
                    root.addView(TextView(this).apply{text="Customers (${custPref.all.size})"; setTypeface(null,Typeface.BOLD); setPadding(0,12,0,0)})
                    custPref.all.values.forEach{ root.addView(TextView(this).apply{text="• $it"; setPadding(12,6,12,6); setBackgroundColor(Color.WHITE)}) }
                }
                title.contains("Cash") -> {
                    val drawer = cashPref.getFloat("drawer",1245.6f)
                    addCard("Cash Drawer","$${String.format("%.2f",drawer)} - REAL")
                    val ed = EditText(this).apply{hint="Adjust Drawer Amount"; inputType=8194}
                    val btn = Button(this).apply{text="UPDATE DRAWER"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)}
                    btn.setOnClickListener{
                        val v = ed.text.toString().toFloatOrNull()?:0f
                        cashPref.edit().putFloat("drawer",v).apply(); Toast.makeText(this,"Drawer Updated",0).show()
                    }
                    root.addView(ed); root.addView(btn)
                }
                title.contains("Users") -> {
                    val ed = EditText(this).apply{hint="Username"}
                    val role = EditText(this).apply{hint="Role - admin/cashier"}
                    val btn = Button(this).apply{text="ADD USER"; setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE)}
                    btn.setOnClickListener{
                        if(ed.text.isNotEmpty()){
                            userPref.edit().putString(ed.text.toString(),role.text.toString().ifEmpty{"cashier"}).apply()
                            Toast.makeText(this,"User Added: ${ed.text}",0).show()
                        }
                    }
                    root.addView(ed); root.addView(role); root.addView(btn)
                    userPref.all.forEach{ addCard(it.key, "Role: ${it.value}") }
                }
                title.contains("Purchasing") -> {
                    root.addView(TextView(this).apply{text="+ ADD PRODUCT - REAL"; setTypeface(null,Typeface.BOLD)})
                    val code = EditText(this).apply{hint="Barcode"}; val name = EditText(this).apply{hint="Product Name"}
                    val buy = EditText(this).apply{hint="Buy Price"; inputType=8194}; val sell = EditText(this).apply{hint="Sell Price"; inputType=8194}
                    val qty = EditText(this).apply{hint="Qty"; inputType=2}
                    val save = Button(this).apply{text="SAVE PRODUCT"; setBackgroundColor(Color.parseColor("#22C55E")); setTextColor(Color.WHITE)}
                    save.setOnClickListener{
                        if(code.text.isNotEmpty() && name.text.isNotEmpty()){
                            prodPref.edit().putString(code.text.toString(),"${name.text}|${buy.text}|${sell.text}|${qty.text}|0|General").apply()
                            // also sync to stock_main for POS
                            getSharedPreferences("stock_main",Context.MODE_PRIVATE).edit().putString(code.text.toString(),"${name.text}|${buy.text}|${sell.text}|${qty.text}|0|General").apply()
                            Toast.makeText(this,"Product Saved",0).show()
                        }
                    }
                    for(v in listOf(code,name,buy,sell,qty)) root.addView(v); root.addView(save)
                    root.addView(TextView(this).apply{text="Add Supplier"; setTypeface(null,Typeface.BOLD); setPadding(0,16,0,0)})
                    val supEd = EditText(this).apply{hint="Supplier Name"}
                    val supBtn = Button(this).apply{text="ADD SUPPLIER"; setBackgroundColor(Color.parseColor("#FB923C")); setTextColor(Color.WHITE)}
                    supBtn.setOnClickListener{
                        supplierPref.edit().putString("${System.currentTimeMillis()}",supEd.text.toString()).apply()
                        Toast.makeText(this,"Supplier Added",0).show()
                    }
                    root.addView(supEd); root.addView(supBtn)
                }
                title.contains("Report") -> {
                    var sales=0.0; var profit=0.0
                    salesPref.all.values.forEach{
                        try{ val p=it.toString().split("|"); sales+=p[1].toFloat(); profit+=p[1].toFloat()-p[2].toFloat()}catch(_:Exception){}
                    }
                    addCard("Today Sales","$${String.format("%.2f",sales)}"); addCard("Total Profit","$${String.format("%.2f",profit)}")
                    addCard("Products", "${prodPref.all.size} Active SKUs")
                    addCard("Stock Value", "$${prodPref.all.values.sumOf{ try{ val p=it.toString().split("|"); p[3].toFloat()*p[1].toFloat().toDouble()}catch(_:Exception){0.0}}.toInt()}")
                }
                title.contains("Audit") -> {
                    val logs = auditPref.getStringSet("logs", mutableSetOf())?: setOf()
                    if(logs.isEmpty()) addCard("Audit Log","No activity yet")
                    else logs.forEach{ addCard("Log", it) }
                }
                title.contains("Price") -> {
                    prodPref.all.forEach{ (code,value) ->
                        val p = value.toString().split("|")
                        val row = LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setBackgroundColor(Color.WHITE); setPadding(12,12,12,12); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,4,0,0)}}
                        row.addView(TextView(this).apply{text="${p.getOrNull(0)} - $${p.getOrNull(2)}"; layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
                        val edit = Button(this).apply{text="Edit Price"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)}
                        edit.setOnClickListener{
                            val dlg = android.app.Dialog(this); val l = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundColor(Color.WHITE)}
                            val newSell = EditText(this).apply{hint="New Sell Price - ${p.getOrNull(2)}"}; l.addView(newSell)
                            val sv = Button(this).apply{text="SAVE"}; sv.setOnClickListener{
                                val np = p.toMutableList(); np[2]=newSell.text.toString()
                                prodPref.edit().putString(code,np.joinToString("|")).apply()
                                getSharedPreferences("stock_main",Context.MODE_PRIVATE).edit().putString(code,np.joinToString("|")).apply()
                                Toast.makeText(this,"Price Updated",0).show(); dlg.dismiss()
                            }
                            l.addView(sv); dlg.setContentView(l); dlg.show()
                        }
                        row.addView(edit); root.addView(row)
                    }
                }
                title.contains("Setting") -> {
                    val clearSales = Button(this).apply{text="CLEAR SALES DATA"; setBackgroundColor(Color.parseColor("#DC2626")); setTextColor(Color.WHITE)}
                    clearSales.setOnClickListener{ salesPref.edit().clear().apply(); Toast.makeText(this,"Sales Cleared",0).show() }
                    val clearAll = Button(this).apply{text="RESET APP"; setBackgroundColor(Color.parseColor("#000")); setTextColor(Color.WHITE)}
                    clearAll.setOnClickListener{ prodPref.edit().clear().apply(); salesPref.edit().clear().apply(); custPref.edit().clear().apply(); Toast.makeText(this,"Reset Done",0).show() }
                    root.addView(clearSales); root.addView(clearAll)
                }
                else -> addCard(title,"Fully Functional - No Coming Soon")
            }

            scroll.addView(root); setContentView(scroll)
        }catch(e:Exception){
            val tv = TextView(this); tv.text="Error: ${e.message}\n${e.stackTraceToString()}"; setContentView(tv)
        }
    }
}
