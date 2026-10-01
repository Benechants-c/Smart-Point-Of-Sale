package com.smartpos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class AdminActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(16,16,16,16)
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }
        var profit=0.0
        try{
            val today = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())
            val shopsPref=getSharedPreferences("shops_db", Context.MODE_PRIVATE)
            val shops = if(shopsPref.all.isEmpty()) listOf("main") else shopsPref.all.keys.toList()
            for(shopId in shops){
                val pref=getSharedPreferences("sales_$shopId", Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    val p=v.toString().split("|")
                    if(p.size>=3){
                        val time=p[0].toLongOrNull()?:0
                        val d=java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(time))
                        if(d==today) profit+=p[2].toDoubleOrNull()?:0.0
                    }
                }
            }
        }catch(_:Exception){}
        
        val profitView=TextView(this).apply{
            text="PROFIT: $${String.format("%.2f",profit)}"
            textSize=30f
            setTypeface(null,Typeface.BOLD)
            setTextColor(Color.parseColor("#16A34A"))
            setBackgroundColor(Color.WHITE)
            setPadding(20,40,20,40)
            gravity=Gravity.CENTER
        }
        root.addView(profitView)
        
        val stockBtn=Button(this).apply{
            text="STOCK"
            setBackgroundColor(Color.parseColor("#1E293B"))
            setTextColor(Color.WHITE)
            textSize=20f
            setPadding(0,30,0,30)
        }
        stockBtn.setOnClickListener{
            Toast.makeText(this,"Stock Manager - Add your code here",1).show()
        }
        root.addView(stockBtn)
        
        val posBtn=Button(this).apply{
            text="POS Sales"
            setBackgroundColor(Color.parseColor("#16A34A"))
            setTextColor(Color.WHITE)
            textSize=20f
            setPadding(0,30,0,30)
        }
        posBtn.setOnClickListener{
            startActivity(Intent(this@AdminActivity, SalesActivity::class.java))
        }
        root.addView(posBtn)
        
        setContentView(root)
    }
}
