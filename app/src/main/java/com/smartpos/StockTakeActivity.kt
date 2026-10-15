package com.smartpos
import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.graphics.Color
import android.view.Gravity

class StockTakeActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply{ 
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(30,30,30,30)
        }
        root.addView(TextView(this).apply{ 
            text="STOCK TAKE #432 SAFE - APP WILL NOT CRASH"; 
            textSize=18f; setTextColor(Color.BLACK); setPadding(0,20,0,20)
            gravity=Gravity.CENTER
        })
        root.addView(TextView(this).apply{
            text="Build 432 is minimal so SMART POS will open!\n\nIf this opens, we know 431 crashed whole app.\n\nNext we add products ONE by ONE safely."
            textSize=15f; setPadding(20,20,20,20)
        })
        val back = Button(this).apply{ text="BACK TO POS"; setOnClickListener{ finish() } }
        root.addView(back)
        setContentView(root)
    }
}