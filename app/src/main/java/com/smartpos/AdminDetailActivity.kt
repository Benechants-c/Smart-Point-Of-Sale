package com.smartpos

import android.app.Activity
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
        root.addView(TextView(this).apply { text = title; textSize = 18f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#0F172A")); setPadding(0,16,0,8) })

        // ===== USERS - UNTOUCHED =====
        if (title == "Users & Permissions") {
            val fn = input("Full Name"); val un = input("Username")
            val pw = input("Password").apply{ inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
            val role = input("Role: CASHIER, MANAGER, ADMIN"); val duties = input("Duties")
            root.addView(label("Full Name")); root.addView(fn); root.addView(label("Username")); root.addView(un)
            root.addView(label("Password")); root.addView(pw); root.addView(label("Role")); root.addView(role); root.addView(label("Permissions")); root.addView(duties)
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,16,0,0) }
            val prefs = getSharedPreferences("users",0)
            prefs.all.forEach { list.addView(smallText("• ${it.key} - ${it.value.toString().split("|").getOrNull(2)?: "USER"}")) }
            root.addView(actionButton("SAVE USER", "#16A34A") {
                if (un.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show(); return@actionButton }
                if (pw.text.toString().isEmpty()) { Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show(); return@actionButton }
                prefs.edit().putString(un.text.toString().trim(), "${fn.text}|${pw.text}|${role.text}|${duties.text}").apply()
                list.addView(smallText("• ${un.text} - ${role.text}")); audit("User: ${un.text}")
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show(); fn.text.clear(); un.text.clear(); pw.text.clear(); role.text.clear(); duties.text.clear()
            })
            root.addView(label("SAVED USERS")); root.addView(list)

        // ===== BRANCHES - UNTOUCHED =====
        } else if (title == "Branches / Shops") {
            val sn = input("Shop Name"); val loc = input("Location")
            root.addView(label("Shop Name")); root.addView(sn); root.addView(label("Location")); root.addView(loc)
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val prefs = getSharedPreferences("branches",0)
            if (prefs.all.isEmpty())
