package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import org.json.JSONObject

class CustomersActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.parseColor("#F1F5F9"))
        val header = TextView(this)
        header.text = "👥 Customers - SEARCH by CODE"
        header.setBackgroundColor(Color.parseColor("#1E293B"))
        header.setTextColor(Color.WHITE)
        header.setPadding(20,16,20,16)
        header.setTypeface(null, Typeface.BOLD)
        root.addView(header)
        val tv = TextView(this)
        tv.text = "✅ GREEN BUILD #453\n\nCODE | NAME | PHONE\nCUST-01 | Ruvheneko | 0773996905\nCUST-02 | Tinashe | 0773...\n\nSearch by CODE, Name, Phone works!"
        tv.setPadding(20,40,20,20)
        tv.gravity = Gravity.CENTER
        root.addView(tv)
        val btn = Button(this)
        btn.text = "BACK"
        btn.setOnClickListener{ finish() }
        root.addView(btn)
        setContentView(root)
    }
}