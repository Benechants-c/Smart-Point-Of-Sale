package com.smartpos
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class CustomersActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "Customers GREEN"
        setContentView(tv)
    }
}