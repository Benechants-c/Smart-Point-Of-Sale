package com.smartpos

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class AdminDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val title = intent.getStringExtra("TITLE") ?: "Admin Section"
        
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24,24,24,24)
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }

        // Top bar
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20,20,20,20)
            addView(TextView(this@AdminDetailActivity).apply {
                text = title.uppercase()
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
            })
        })

        root.addView(TextView(this).apply {
            text = "\n$title\n\nThis screen is ready.\n\nWe will build full functionality next.\nFor now, your RECEIVING is fully working in PURCHASING.\n\nRecent Activity will show here."
            textSize = 15f
            setTextColor(Color.parseColor("#334155"))
            setPadding(0,20,0,20)
        })

        root.addView(Button(this).apply {
            text = "BACK TO ADMIN DASHBOARD"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(0,20,0,20)
            setOnClickListener { finish() }
        })

        setContentView(root)
    }
}
