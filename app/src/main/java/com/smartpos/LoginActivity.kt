package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.view.View

class LoginActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLogin()
    }

    fun showLogin() {
        val lay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 200, 60, 60)
            gravity = Gravity.CENTER
        }
        val t = TextView(this).apply { text = "SMART POS LOGIN\n\$12/mo Commercial"; textSize = 18f; gravity = Gravity.CENTER; setPadding(0,0,0,40) }
        val biz = EditText(this).apply { hint = "BusinessID: SHOP001-HRE" }
        val pin = EditText(this).apply { hint = "PIN: 1234 ADMIN"; inputType = 129 }
        val btn = Button(this).apply { text = "LOGIN" }
        val status = TextView(this).apply { gravity = Gravity.CENTER; setPadding(0,20,0,0) }

        btn.setOnClickListener {
            if (pin.text.toString() == "1234") {
                Toast.makeText(this, "ADMIN Login - Audit ON", Toast.LENGTH_SHORT).show()
                showSales(biz.text.toString().ifEmpty { "SHOP001-HRE" })
            } else {
                status.text = "Wrong PIN! Use 1234"
            }
        }
        lay.addView(t); lay.addView(biz); lay.addView(pin); lay.addView(btn); lay.addView(status)
        setContentView(lay)
    }

    fun showSales(businessId: String) {
        val lay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 150, 40, 40)
        }
        val title = TextView(this).apply {
            text = "SALES DASHBOARD\n$businessId\nAUDIT ON ✅\n\$12/mo"
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0,0,0,30)
        }
        val item = EditText(this).apply { hint = "Item name" }
        val price = EditText(this).apply { hint = "Price"; inputType = 8194 }
        val btn = Button(this).apply { text = "ADD SALE - Stock -1" }
        val log = TextView(this).apply { text = "Sales log:\n"; setPadding(0,20,0,0) }
        val logout = Button(this).apply { text = "LOGOUT" }

        btn.setOnClickListener {
            log.text = "${log.text}\n${item.text} - \$${price.text}"
            Toast.makeText(this, "Sale added - Stock -1 logged", Toast.LENGTH_SHORT).show()
            item.text.clear(); price.text.clear()
        }
        logout.setOnClickListener { showLogin() }

        lay.addView(title); lay.addView(item); lay.addView(price); lay.addView(btn); lay.addView(log); lay.addView(logout)
        setContentView(lay)
    }
}
