package com.smartpos
import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import android.graphics.Color
class StockListActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this); tv.text = "Stock List - GREEN"; tv.setPadding(30,30,30,30); tv.setBackgroundColor(Color.WHITE); setContentView(tv)
    }
}