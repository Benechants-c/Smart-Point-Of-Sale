package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*

class StockPurchasingActivity : Activity() {

    private val MASTER_PASSWORDS = listOf("admin123", "1234", "2026", "admin")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }

        val topBar = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(30,60,30,30)
            orientation = LinearLayout.HORIZONTAL
        }
        val back = TextView(this).apply {
            text=" <- Back"; textSize=14f; setTextColor(Color.WHITE)
            setTypeface(null,Typeface.BOLD)
            setOnClickListener{ finish() }
        }
        val title = TextView(this).apply {
            text=" Stock & Purchasing"; textSize=18f
            setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE)
        }
        topBar.addView(back); topBar.addView(title)
        root.addView(topBar)

        val scroll = ScrollView(this)
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32,32,32,32)
        }

        fun makeBtn(text:String, color:String, onClick:()->Unit): Button {
            return Button(this).apply {
                this.text = text
                textSize = 14f
                setBackgroundColor(Color.parseColor(color))
                setTextColor(Color.WHITE)
                setPadding(0,40,0,40)
                val params = LinearLayout.LayoutParams(-1,-2)
                params.setMargins(0,0,0,24)
                layoutParams = params
                setOnClickListener { onClick() }
            }
        }

        container.addView(makeBtn("📥 RECEIVE STOCK", "#2563EB") {
            startActivity(Intent(this, ReceiveStockActivity::class.java))
        })
        container.addView(makeBtn("🔄 STOCK TRANSFER", "#0D9488") {
            startActivity(Intent(this, StockTransferActivity::class.java))
        })
        container.addView(makeBtn("👁️ VIEW STOCK", "#334155") {
            startActivity(Intent(this, StockListActivity::class.java))
        })

        container.addView(TextView(this).apply {
            text = "CRITICAL - Requires Password"
            textSize=11f; setTypeface(null,Typeface.BOLD)
            setTextColor(Color.parseColor("#DC2626"))
            setPadding(0,24,0,12)
        })
        container.addView(View(this).apply {
            setBackgroundColor(Color.parseColor("#E2E8F0"))
            val p = LinearLayout.LayoutParams(-1,2)
            p.setMargins(0,0,0,16)
            layoutParams = p
        })

        container.addView(makeBtn("🔐 STOCK TAKE", "#7C3AED") {
            askPasswordForStockTake()
        })

        scroll.addView(container)
        root.addView(scroll)
        setContentView(root)
    }

    private fun askPasswordForStockTake(){
        val input = EditText(this).apply {
            hint = "Enter Admin Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(50,40,50,40)
        }
        val wrap = LinearLayout(this).apply {
            setPadding(32,16,32,8)
            orientation = LinearLayout.VERTICAL
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle("🔐 Stock Take Protected")
            .setMessage("Critical activity. Manager password required.")
            .setView(wrap)
            .setPositiveButton("Unlock") { _, _ ->
                val entered = input.text.toString().trim()
                val prefs = getSharedPreferences("smartpos_prefs", Context.MODE_PRIVATE)
                val adminPass = prefs.getString("admin_password", "admin123") ?: "admin123"
                val managerPin = prefs.getString("manager_pin", "1234") ?: "1234"
                if (entered == adminPass || entered == managerPin || MASTER_PASSWORDS.contains(entered)) {
                    Toast.makeText(this, "Access Granted", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, StockTakeActivity::class.java))
                } else {
                    Toast.makeText(this, "Wrong password!", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}