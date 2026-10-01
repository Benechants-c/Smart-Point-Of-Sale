package com.smartpos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*

class PosLoginActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        
        val prefs = getSharedPreferences("pos_login", Context.MODE_PRIVATE)
        // Default PIN 1234 - you can change it later
        if (!prefs.contains("cashier_pin")) {
            prefs.edit().putString("cashier_pin", "1234").apply()
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(40,80,40,40)
            gravity = android.view.Gravity.CENTER
        }

        root.addView(TextView(this).apply {
            text = "🏪 SMART POS\nSALES WINDOW"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = android.view.Gravity.CENTER
            setPadding(0,0,0,30)
        })

        root.addView(TextView(this).apply {
            text = "Cashier Login - Separate from Admin"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = android.view.Gravity.CENTER
            setPadding(0,0,0,30)
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(30,30,30,30)
        }

        card.addView(TextView(this).apply {
            text = "Enter Sales PIN"
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
        })

        val pinInput = EditText(this).apply {
            hint = "PIN - Default 1234"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setPadding(20,20,20,20)
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }
        card.addView(pinInput)

        card.addView(TextView(this).apply {
            text = "Default PIN: 1234 - Change in settings later"
            textSize = 10f
            setTextColor(Color.GRAY)
            setPadding(0,10,0,10)
        })

        val btnLogin = Button(this).apply {
            text = "LOGIN TO SALES →"
            setBackgroundColor(Color.parseColor("#A855F7"))
            setTextColor(Color.WHITE)
        }
        card.addView(btnLogin)

        val btnChangePin = Button(this).apply {
            text = "Change PIN"
            setBackgroundColor(Color.parseColor("#334155"))
            setTextColor(Color.WHITE)
        }
        card.addView(btnChangePin)

        root.addView(card)

        btnLogin.setOnClickListener {
            val savedPin = prefs.getString("cashier_pin", "1234")
            if (pinInput.text.toString() == savedPin) {
                Toast.makeText(this, "Welcome Cashier!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, SalesActivity::class.java))
                // Don't finish - so you can go back to login
            } else {
                Toast.makeText(this, "Wrong PIN! Default is 1234", Toast.LENGTH_LONG).show()
            }
        }

        btnChangePin.setOnClickListener {
            val newPin = pinInput.text.toString()
            if (newPin.length < 4) {
                Toast.makeText(this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
            } else {
                prefs.edit().putString("cashier_pin", newPin).apply()
                Toast.makeText(this, "New Sales PIN saved: $newPin", Toast.LENGTH_LONG).show()
            }
        }

        setContentView(root)
    }
}
