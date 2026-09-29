package com.smartpos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*

class LoginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#0F172A"))
                setPadding(40,100,40,40)
                gravity = android.view.Gravity.CENTER
            }
            root.addView(TextView(this).apply {
                text = "SMART POS"
                textSize = 28f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                gravity = android.view.Gravity.CENTER
            })
            val userInput = EditText(this).apply {
                hint = "Username: admin"
                setBackgroundColor(Color.WHITE)
                setPadding(20,20,20,20)
            }
            val passInput = EditText(this).apply {
                hint = "Password: admin"
                setBackgroundColor(Color.WHITE)
                setPadding(20,20,20,20)
            }
            val btn = Button(this).apply {
                text = "LOGIN"
                setBackgroundColor(Color.parseColor("#2563EB"))
                setTextColor(Color.WHITE)
            }
            btn.setOnClickListener {
                try {
                    val u = userInput.text.toString().trim()
                    if (u.isEmpty() || u.equals("admin", true)) {
                        startActivity(Intent(this, AdminActivity::class.java))
                    } else {
                        Toast.makeText(this, "Logged as $u - Opening Admin", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, AdminActivity::class.java))
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Login error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            root.addView(userInput)
            root.addView(passInput)
            root.addView(btn)
            setContentView(root)
        } catch (e: Exception) {
            val tv = TextView(this)
            tv.text = "Error: ${e.message}\n${e.stackTraceToString()}"
            setContentView(tv)
        }
    }
}
