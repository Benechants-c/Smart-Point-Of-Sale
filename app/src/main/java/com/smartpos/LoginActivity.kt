package com.smartpos

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24,24,24,24)
            setBackgroundColor(Color.WHITE)
        }

        // Title
        root.addView(TextView(this).apply {
            text = "SMART POS \$12/mo"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0,0,0,16)
        })

        root.addView(TextView(this).apply {
            text = "SMART POS \$12/mo - SALES"
            setBackgroundColor(Color.parseColor("#F1F5F9"))
            setTextColor(Color.parseColor("#334155"))
            setPadding(16,12,16,12)
            setTypeface(null, Typeface.BOLD)
        })

        // Search
        val searchInput = EditText(this).apply {
            hint = "Enter code or name"
        }
        root.addView(searchInput, LinearLayout.LayoutParams(-1,-2).apply { topMargin = 12 })

        val searchBtn = Button(this).apply {
            text = "SEARCH ADD"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
        }
        root.addView(searchBtn)

        // Cart
        val cartBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F8FAFC"))
            setPadding(0,40,0,40)
            gravity = Gravity.CENTER
        }
        cartBox.addView(TextView(this).apply {
            text = "Cart empty - Search product"
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
        })
        root.addView(cartBox, LinearLayout.LayoutParams(-1,-2).apply { topMargin = 12 })

        // Total
        root.addView(TextView(this).apply {
            text = "Total 0.00"
            setBackgroundColor(Color.parseColor("#1E293B"))
            setTextColor(Color.WHITE)
            gravity = Gravity.RIGHT
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(16,18,16,18)
        })

        // PAY ROW - ONLY CASH + ECOCASH - FIXED - NO RECEIVE STOCK
        val payRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val cashBtn = Button(this).apply {
            text = "CASH"
            setBackgroundColor(Color.parseColor("#16A34A"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        }
        val ecoBtn = Button(this).apply {
            text = "ECOCASH"
            setBackgroundColor(Color.parseColor("#7C3AED"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        }

        payRow.addView(cashBtn, LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(0,6,4,0) })
        payRow.addView(ecoBtn, LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(4,6,0,0) })

        root.addView(payRow)

        cashBtn.setOnClickListener {
            Toast.makeText(this, "Cash sale", Toast.LENGTH_SHORT).show()
        }
        ecoBtn.setOnClickListener {
            Toast.makeText(this, "EcoCash sale", Toast.LENGTH_SHORT).show()
        }

        // Amount
        root.addView(EditText(this).apply {
            hint = "Amount Received"
        }, LinearLayout.LayoutParams(-1,-2).apply { topMargin = 12 })

        root.addView(TextView(this).apply {
            text = "Change: 0.00"
            setBackgroundColor(Color.parseColor("#DCFCE7"))
            setTextColor(Color.parseColor("#166534"))
            gravity = Gravity.RIGHT
            setPadding(16,16,16,16)
            setTypeface(null, Typeface.BOLD)
        })

        root.addView(Button(this).apply {
            text = "COMPLETE SALE"
            setBackgroundColor(Color.parseColor("#16A34A"))
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
        }, LinearLayout.LayoutParams(-1,-2).apply { topMargin = 8 })

        // Hidden admin link - long press title to go to admin receiving
        root.addView(TextView(this).apply {
            text = "Admin? Tap here for Receiving"
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0,24,0,0)
            textSize = 12f
            setOnClickListener {
                startActivity(Intent(this@LoginActivity, ReceiveStockActivity::class.java))
            }
        })

        scroll.addView(root)
        setContentView(scroll)
    }
}
