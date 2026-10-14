package com.smartpos
import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.graphics.Color

class PriceManagementActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply{ 
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
        
        root.addView(TextView(this).apply{ 
            text="Price Management - REAL GREEN"
            textSize=18f
            setPadding(20,30,20,10)
            setTextColor(Color.BLACK)
        })
        
        val back = Button(this).apply{ text="BACK" }
        back.setOnClickListener{ finish() }
        root.addView(back)
        
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(10,10,10,10) }
        scroll.addView(list)
        root.addView(scroll)
        
        try{
            val pref = getSharedPreferences("products_main", 0)
            val settings = getSharedPreferences("system_settings", 0)
            val cur = settings.getString("currency", "$")
            
            if(pref.all.isEmpty()){
                list.addView(TextView(this).apply{
                    text="No products yet\nGo to Stock List to add products\n\nTax Inclusive Mode: ON\nCurrency: $cur"
                    setPadding(20,40,20,20)
                })
            } else {
                for((key,value) in pref.all){
                    val row = TextView(this).apply{
                        text="$key : $value"
                        textSize=15f
                        setPadding(20,15,20,15)
                        setBackgroundColor(Color.WHITE)
                        setTextColor(Color.BLACK)
                    }
                    list.addView(row)
                    val line = View(this).apply{
                        setBackgroundColor(Color.parseColor("#E2E8F0"))
                        layoutParams=LinearLayout.LayoutParams(-1,2)
                    }
                    list.addView(line)
                }
            }
        }catch(e:Exception){
            list.addView(TextView(this).apply{ text="Error: ${e.message}" })
        }
        
        setContentView(root)
    }
}