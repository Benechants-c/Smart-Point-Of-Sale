package com.smartpos
import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.graphics.Color
class SalesActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this); tv.text = "Sales - GREEN BUILD"; tv.setPadding(30,30,30,30); tv.setBackgroundColor(Color.WHITE); setContentView(tv)
    }
}