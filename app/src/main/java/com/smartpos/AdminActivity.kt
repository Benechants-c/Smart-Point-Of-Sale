package com.smartpos

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class AdminActivity : Activity() {
    private val ADMIN_PIN = "1234"
    private lateinit var profitView: TextView

    private fun calcProfit(): Double {
        var profit=0.0
        try{
            val shopsPref=getSharedPreferences("shops_db", Context.MODE_PRIVATE)
            val shops = if(shopsPref.all.isEmpty()) listOf("main") else shopsPref.all.keys.toList()
            val today=SimpleDateFormat("yyyy-MM-dd").format(Date())
            for(shopId in shops){
                val pref=getSharedPreferences("sales_$shopId", Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    val p=v.toString().split("|")
                    if(p.size>=3){
                        val time=p[0].toLongOrNull()?:0
                        val d=SimpleDateFormat("yyyy-MM-dd").format(Date(time))
                        if(d==today) profit+=p[2].toDoubleOrNull()?:0.0
                    }
                }
            }
        }catch(_:Exception){}
        return profit
    }

    private fun askPin(onOk: ()->Unit){
        val dlg=Dialog(this)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setPadding(20,20,20,20); setBackgroundColor(Color.WHITE)}
        val pinEd=EditText(this).apply{hint="Enter PIN 1234"; inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD; setPadding(16,16,16,16); setBackgroundColor(Color.parseColor("#F1F5F9"))}
        root.addView(pinEd)
        val ok=Button(this).apply{text="UNLOCK"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE)}
        ok.setOnClickListener{ if(pinEd.text.toString()==ADMIN_PIN){dlg.dismiss(); onOk()} else Toast.makeText(this,"Wrong PIN!",0).show() }
        root.addView(ok)
        dlg.setContentView(root); dlg.show(); dlg.window?.setLayout((resources.displayMetrics.widthPixels*0.85).toInt(),-2)
    }

    private fun showStock(){
        val dlg=Dialog(this); val root=LinearLayout(this).apply{orientation
