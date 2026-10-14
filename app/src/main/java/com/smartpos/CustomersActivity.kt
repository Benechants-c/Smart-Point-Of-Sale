package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class CustomersActivity : Activity() {
    private lateinit var listLayout: LinearLayout
    private lateinit var countView: TextView
    private fun getCurrency(): String = getSharedPreferences("system_settings",0).getString("currency","$")?:"$"

    private fun loadCustomers(){
        listLayout.removeAllViews()
        val pref = getSharedPreferences("customers_db", Context.MODE_PRIVATE)
        val all = pref.all
        countView.text = "${all.size} customers - REAL | Currency: ${getCurrency()} | Tax Inclusive"

        if(all.isEmpty()){
            listLayout.addView(TextView(this).apply{text="No customers yet.\nAdd during Sale or via Add Customer.\nAll prices Tax Inclusive"; gravity=Gravity.CENTER; setPadding(20,100,20,20)})
            return
        }

        for((id, v) in all){
            val parts = v.toString().split("|")
            val name = parts.getOrNull(0)?:"Customer"
            val phone = parts.getOrNull(1)?:""
            val balance = parts.getOrNull(2)?:"0.00"

            val row = LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL; setPadding(16,14,16,14); setBackgroundColor(Color.WHITE); gravity=Gravity.CENTER_VERTICAL}
            val left = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; layoutParams=LinearLayout.LayoutParams(0,-2,2f)}
            left.addView(TextView(this).apply{text=name; textSize=15f; setTypeface(null,Typeface.BOLD)})
            left.addView(TextView(this).apply{text="Phone: $phone | ID: $id | Bal: ${getCurrency()}$balance"; textSize=11f; setTextColor(Color.GRAY)})

            val del = Button(this).apply{text="DELETE"; setBackgroundColor(Color.parseColor("#EF4444")); setTextColor(Color.WHITE); setOnClickListener{
                AlertDialog.Builder(this@CustomersActivity).setTitle("Delete $name?").setPositiveButton("Delete"){_,_->
                    pref.edit().remove(id).apply(); loadCustomers()
                }.setNegativeButton("Cancel",null).show()
            }}
            row.addView(left); row.addView(del)
            listLayout.addView(row)
            listLayout.addView(View(this).apply{layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#E5E7EB"))})
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC"))}
        root.addView(TextView(this).apply{text="Customers"; textSize=18f; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(20,30,20,12)})
        val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        root.addView(TextView(this).apply{text="📅 $date - Auto Device Date"; textSize=12f; setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.parseColor("#94A3B8")); setPadding(20,0,20,12)})

        val back = Button(this).apply{text="IJ BACK"; setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,8,8,8)}}; back.setOnClickListener{finish()}; root.addView(back)

        countView = TextView(this).apply{text="0 customers - Add in Sales screen"; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#FEF9C3")); setPadding(16,12,16,12)}; root.addView(countView)

        val addBtn = Button(this).apply{text="➕ ADD CUSTOMER"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,8,8,8)}}
        addBtn.setOnClickListener{
            val lay = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(30,20,30,20)}
            val edName = EditText(this).apply{hint="Customer Name"}; val edPhone = EditText(this).apply{hint="Phone +263..."}
            lay.addView(edName); lay.addView(edPhone)
            AlertDialog.Builder(this).setTitle("Add Customer - Tax Incl").setView(lay).setPositiveButton("SAVE"){_,_->
                val name = edName.text.toString(); if(name.isEmpty()){Toast.makeText(this,"Enter name",0).show(); return@setPositiveButton}
                val id = "CUST_${System.currentTimeMillis()}"; getSharedPreferences("customers_db",0).edit().putString(id,"$name|${edPhone.text}|0.00|${System.currentTimeMillis()}").apply(); loadCustomers()
            }.setNegativeButton("Cancel",null).show()
        }; root.addView(addBtn)

        listLayout = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val scroll = ScrollView(this).apply{layoutParams=LinearLayout.LayoutParams(-1,0,1f); addView(listLayout)}; root.addView(scroll)
        setContentView(root); loadCustomers()
    }
}