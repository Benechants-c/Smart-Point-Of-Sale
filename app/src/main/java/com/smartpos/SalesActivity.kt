package com.smartpos
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class SalesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "Sales GREEN"
        setContentView(tv)
    }
}