package com.smartpos
import android.app.Activity
import android.os.Bundle
import android.widget.TextView
class PriceManagementActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this); tv.text = "PriceManagement - GREEN"; setContentView(tv)
    }
}