package com.smartpos

import android.os.Bundle
import android.graphics.Color
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.view.Gravity

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12,12,12,12)
            setBackgroundColor(Color.WHITE)
        }

        // HEADER
        root.addView(TextView(this).apply {
            text = "SMART POS \$12/mo"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(16,16,16,16)
        })

        root.addView(TextView(this).apply {
            text = "SMART POS \$12/mo - SALES"
            setBackgroundColor(Color.parseColor("#F1F5F9"))
            setPadding(8,8,8,8)
        })

        // SEARCH
        val searchInput = EditText(this).apply { hint = "Enter code or name" }
        root.addView(searchInput)

        val searchBtn = Button(this).apply {
            text = "SEARCH ADD"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
        }
        root.addView(searchBtn)

        root.addView(TextView(this).apply {
            text = "Cart empty - Search product"
            gravity = Gravity.CENTER
            setPadding(20,20,20,20)
            setTextColor(Color.GRAY)
        })

        // TOTAL
        root.addView(TextView(this).apply {
            text = "Total 0.00"
            setBackgroundColor(Color.parseColor("#1E293B"))
            setTextColor(Color.WHITE)
            gravity = Gravity.RIGHT
            textSize = 18f
            setPadding(16,16,16,16)
            setTypeface(null, android.graphics.Typeface.BOLD)
        })

        // PAY ROW - ONLY CASH + ECOCASH - NO RECEIVE STOCK!
        val payRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val cashBtn = Button(this).apply {
            text = "CASH"
            setBackgroundColor(Color.parseColor("#16A34A"))
            setTextColor(Color.WHITE)
        }

        val ecoBtn = Button(this).apply {
            text = "ECOCASH"
            setBackgroundColor(Color.parseColor("#7C3AED"))
            setTextColor(Color.WHITE)
        }

        // FIXED: Only 2 buttons, equal weight
        payRow.addView(cashBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(2,2,2,2) })
        payRow.addView(ecoBtn
