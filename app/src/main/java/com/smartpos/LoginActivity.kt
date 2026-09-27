package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.content.Intent
import android.view.Gravity

class LoginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val lay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 200, 60, 60)
            gravity = Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "SMART POS LOGIN\n\$12/mo Commercial"
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0,0,0,60)
        }

        val biz = EditText(this).apply {
            hint = "BusinessID: SHOP001-HRE"
        }

        val pin = EditText(this).apply {
            hint = "PIN: 1234 ADMIN"
            inputType = 129 // password
        }

        val btn = Button(this).apply {
            text = "LOGIN"
        }

        val status = TextView(this).apply {
            text = "Enter ADMIN PIN to start audit"
            setPadding(0,40,0,0)
        }

        btn.setOnClickListener {
            val b = biz.text.toString()
            val p = pin.text.toString()
            if (p == "1234") {
                Toast.makeText(this, "ADMIN Login - Audit ON", Toast.LENGTH_SHORT).show()
                try {
                    val intent = Intent(this, SalesActivity::class.java)
                    intent.putExtra("BUSINESS_ID", b)
                    startActivity(intent)
                } catch (e: Exception) {
                    status.text = "Logged in as ADMIN\n${e.message}\nSalesActivity not found - create it"
                }
            } else {
                Toast.makeText(this, "Wrong PIN! Use 1234", Toast.LENGTH_SHORT).show()
            }
        }

        lay.addView(title)
        lay.addView(biz)
        lay.addView(pin)
        lay.addView(btn)
        lay.addView(status)

        setContentView(lay)
    }
}
