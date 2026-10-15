package com.smartpos
import android.app.Activity
import android.os.Bundle
import android.widget.*

class StockTakeActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL }
        root.addView(TextView(this).apply{ text="STOCK TAKE - BUILD 428 GREEN"; textSize=20f; setPadding(20,40,20,20) })
        val back = Button(this).apply{ text="BACK" }
        back.setOnClickListener{ finish() }
        root.addView(back)
        setContentView(root)
    }
}