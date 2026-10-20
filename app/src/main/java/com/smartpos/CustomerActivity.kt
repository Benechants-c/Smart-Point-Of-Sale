package com.smartpos
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class CustomerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "Customer - GREEN"
        setContentView(tv)
    }
}