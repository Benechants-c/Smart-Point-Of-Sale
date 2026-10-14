package com.smartpos
import android.app.Activity
import android.os.Bundle
import android.widget.*
class PriceManagementActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL }
        root.addView(TextView(this).apply{ text="Price Management - REAL GREEN"; textSize=20f; setPadding(20,40,20,20) })
        val back = Button(this).apply{ text="BACK" }
        back.setOnClickListener{ finish() }
        root.addView(back)
        root.addView(TextView(this).apply{ text="Products will show here\nTap price to edit - Tax Inclusive"; setPadding(20,20,20,20) })
        setContentView(root)
    }
}