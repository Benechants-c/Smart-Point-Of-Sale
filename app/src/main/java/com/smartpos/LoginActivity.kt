package com.smartpos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class LoginActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        
        val prefsAdmin = getSharedPreferences("login", Context.MODE_PRIVATE)
        val prefsSales = getSharedPreferences("pos_login", Context.MODE_PRIVATE)
        if(!prefsSales.contains("cashier_pin")) prefsSales.edit().putString("cashier_pin", "1234").apply()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            gravity = Gravity.CENTER
            setPadding(30,30,30,30)
        }

        root.addView(TextView(this).apply {
            text = "SMART POS"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0,0,0,40)
        })

        // --- ADMIN CARD ---
        val adminCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(20,20,20,20)
        }
        adminCard.addView(TextView(this).apply { text = "Admin Login"; setTypeface(null, Typeface.BOLD); textSize=14f })
        val adminInput = EditText(this).apply { hint = "Password: admin"; setPadding(20,20,20,20) }
        adminCard.addView(adminInput)
        val adminBtn = Button(this).apply {
            text = "LOGIN AS ADMIN"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
        }
        adminCard.addView(adminBtn)
        root.addView(adminCard)

        root.addView(TextView(this).apply { text = ""; setPadding(0,10,0,10) })

        // --- SALES CARD - SEPARATE! ---
        val salesCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(20,20,20,20)
        }
        salesCard.addView(TextView(this).apply { text = "Sales / Cashier Login"; setTypeface(null, Typeface.BOLD); textSize=14f; setTextColor(Color.parseColor("#A855F7")) })
        val salesInput = EditText(this).apply { hint = "Sales PIN: 1234"; setPadding(20,20,20,20); inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        salesCard.addView(salesInput)
        val salesBtn = Button(this).apply {
            text = "LOGIN TO SALES WINDOW →"
            setBackgroundColor(Color.parseColor("#A855F7"))
            setTextColor(Color.WHITE)
        }
        salesCard.addView(salesBtn)
        salesCard.addView(TextView(this).apply { text = "Separate password - Does not affect Admin"; textSize=10f; setTextColor(Color.GRAY); gravity = Gravity.CENTER })
        root.addView(salesCard)

        adminBtn.setOnClickListener {
            if(adminInput.text.toString() == "admin" || adminInput.text.toString() == "0000") {
                startActivity(Intent(this, AdminActivity::class.java))
            } else {
                Toast.makeText(this, "Wrong Admin Password! Use admin", Toast.LENGTH_SHORT).show()
            }
        }

        salesBtn.setOnClickListener {
            val savedPin = prefsSales.getString("cashier_pin", "1234")
            if(salesInput.text.toString() == savedPin) {
                // DIRECT TO SALES - DOES NOT TOUCH ADMIN PROGRESS!
                startActivity(Intent(this, SalesActivity::class.java))
            } else {
                Toast.makeText(this, "Wrong Sales PIN! Default 1234", Toast.LENGTH_SHORT).show()
            }
        }

        setContentView(root)
    }
}
