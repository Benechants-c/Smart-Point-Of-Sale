package com.smartpos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*

class AdminDetailActivity : Activity() {

    private val grnProducts = mutableListOf<Array<String>>()

    private fun label(t: String): TextView {
        return TextView(this).apply { text = t; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#334155")); setPadding(0,18,0,6) }
    }
    private fun input(h: String): EditText {
        return EditText(this).apply { hint = h; setPadding(24,18,24,18); setBackgroundColor(Color.parseColor("#F8FAFC")) }
    }
    private fun numberInput(h: String, dec: Boolean = false): EditText {
        return EditText(this).apply {
            hint = h; inputType = if (dec) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_NUMBER
            setPadding(24,18,24,18); setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
    }
    private fun actionButton(txt: String, col: String, click: () -> Unit): Button {
        return Button(this).apply {
            text = txt; setBackgroundColor(Color.parseColor(col)); setTextColor(Color.WHITE)
            setPadding(0,18,0,18); setOnClickListener { click() }
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,10,0,10)}
        }
    }
    private fun smallText(t: String, c: Int = Color.BLACK): TextView {
        return TextView(this).apply { text = t; setTextColor(c); textSize = 13f; setPadding(0,8,0,8) }
    }
    private fun audit(m: String) { getSharedPreferences("audit_log",0).edit().putString(System.currentTimeMillis().toString(), m).apply() }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val title = intent.getStringExtra("TITLE")?: "Admin"
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16) }

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#1E293B")); setPadding(18,12,10,12) }
        head.addView(TextView(this).apply { text = title.uppercase(); setTextColor(Color.WHITE); textSize = 16f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
        head.addView(Button(this).apply { text = "BACK"; setBackgroundColor(Color.parseColor("#475569")); setTextColor(Color.WHITE); setOnClickListener { finish() } })
        root.addView(head)

        // OTHER SCREENS - UNTOUCHED SAFE
        if (title == "Users & Permissions") {
            val fn = input("Full Name"); val un = input("Username"); val pw = input("Password").apply{ inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }; val role = input("Role")
            root.addView(fn); root.addView(un); root.addView(pw); root.addView(role)
            root.addView(actionButton("SAVE USER", "#16A34A"){ getSharedPreferences("users",0).edit().putString(un.text.toString().trim(), "${fn.text}|${pw.text}|${role.text}").apply(); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show() })
        } else if (title == "Branches / Shops") {
            val sn = input("Shop Name"); val loc = input("Location")
            root.addView(sn); root.addView(loc)
            root.addView(actionButton("ADD SHOP", "#0F766E"){ getSharedPreferences("branches",0).edit().putString(sn.text.toString().trim(), loc.text.toString().trim()).apply(); Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show() })
        } else if (title == "Products & Categories") {
            val pn = input("Product"); val cat = input("Category"); val buy = input("Buy"); val sell = input("Sell"); val qty = input("Qty")
            root.addView(pn); root.addView(cat); root.addView(buy); root.addView(sell); root.addView(qty)
            root.addView(actionButton("ADD PRODUCT", "#2563EB"){ getSharedPreferences("products_db",0).edit().putString("P${System.currentTimeMillis()}", "${pn.text}|${cat.text.ifEmpty{"General"}}|${buy.text.ifEmpty{"0"}}|${sell.text.ifEmpty{"0"}}|${qty.text.ifEmpty{"0"}}").apply(); Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show() })

        // ===== RECEIVING - FULL FUNCTIONAL - REAL DATA =====
        } else if (title == "Stock Control" || title == "Stock & Purchasing" || title.contains("RECEIVE", true)) {

            root.addView(TextView(this
