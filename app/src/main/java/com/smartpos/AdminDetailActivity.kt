package com.smartpos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*

class AdminDetailActivity : Activity() {

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text; textSize = 13f; setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#334155")); setPadding(0, 18, 0, 6)
        }
    }
    private fun input(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText; setPadding(24, 18, 24, 18)
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
    }
    private fun numberInput(hintText: String, decimal: Boolean = false): EditText {
        return EditText(this).apply {
            hint = hintText
            inputType = if (decimal) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_NUMBER
            setPadding(24, 18, 24, 18); setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
    }
    private fun actionButton(textValue: String, color: String, click: () -> Unit): Button {
        return Button(this).apply {
            text = textValue; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor(color))
            setPadding(0, 18, 0, 18); setOnClickListener { click() }
            layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 10, 0, 10) }
        }
    }
    private fun smallText(t: String, c: Int = Color.BLACK): TextView {
        return TextView(this).apply { text = t; setTextColor(c); textSize = 13f; setPadding(0, 8, 0, 8) }
    }
    private fun audit(message: String) {
        getSharedPreferences("audit_log", 0).edit().putString(System.currentTimeMillis().toString(), message).apply()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val title = intent.getStringExtra("TITLE")?: "Admin"
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16, 16, 16, 16) }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#1E293B")); setPadding(18, 12, 10, 12) }
        head.addView(TextView(this).apply { text = title.uppercase(); setTextColor(Color.WHITE); textSize = 16f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
        head.addView(Button(this).apply { text = "BACK"; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#475569")); setOnClickListener { finish() } })
        root.addView(head)
        root.addView(TextView(this).apply { text = title; textSize = 18f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#0F172A")); setPadding(0, 16, 0, 8) })

        if (title == "Users & Permissions") {
            val fn = input("Full Name"); val un = input("Username")
            val pw = input("Password").apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
            val role = input("Role: CASHIER, MANAGER, ADMIN"); val duties = input("Duties")
            root.addView(label("Full Name")); root.addView(fn); root.addView(label("Username")); root.addView(un)
            root.addView(label("Password")); root.addView(pw); root.addView(label("Role")); root.addView(role); root.addView(label("Permissions")); root.addView(duties)
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 16, 0, 0) }
            val prefs = getSharedPreferences("users", 0)
            prefs.all.forEach { list.addView(smallText("• ${it.key} - ${it.value.toString().split("|").getOrNull(2)?: "USER"}")) }
            root.addView(actionButton("SAVE USER", "#16A34A") {
                if (un.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show(); return@actionButton }
                if (pw.text.toString().isEmpty()) { Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show(); return@actionButton }
                prefs.edit().putString(un.text.toString().trim(), "${fn.text}|${pw.text}|${role.text}|${duties.text}").apply()
                list.addView(smallText("• ${un.text} - ${role.text}")); audit("User: ${un.text}")
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show(); fn.text.clear(); un.text.clear(); pw.text.clear(); role.text.clear(); duties.text.clear()
            })
            root.addView(label("SAVED USERS")); root.addView(list)

        } else if (title == "Branches / Shops") {
            val sn = input("Shop Name"); val loc = input("Location")
            root.addView(label("Shop Name")); root.addView(sn); root.addView(label("Location")); root.addView(loc)
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val prefs = getSharedPreferences("branches", 0)
            if (prefs.all.isEmpty()) list.addView(smallText("No shops yet.", Color.GRAY)) else prefs.all.forEach { list.addView(smallText("• ${it.key} - ${it.value}")) }
            root.addView(actionButton("ADD SHOP", "#0F766E") {
                if (sn.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter shop name", Toast.LENGTH_SHORT).show(); return@actionButton }
                prefs.edit().putString(sn.text.toString().trim(), loc.text.toString().trim()).apply()
                list.removeAllViews(); prefs.all.forEach { list.addView(smallText("• ${it.key} - ${it.value}")) }
                audit("Shop: ${sn.text}"); sn.text.clear(); loc.text.clear()
            })
            root.addView(list)

        } else if (title == "Products & Categories") {
            val pname = input("Product Name"); val pcat = input("Category")
            val buy = numberInput("Buy Price", true); val sell = numberInput("Sell Price", true); val qty = numberInput("Qty")
            root.addView(label("Product Name")); root.addView(pname); root.addView(label("Category")); root.addView(pcat)
            root.addView(label("Buy")); root.addView(buy); root.addView(label("Sell")); root.addView(sell); root.addView(label("Qty")); root.addView(qty)
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val prefs = getSharedPreferences("products_db", 0)
            prefs.all.forEach { try { val a = it.value.toString().split("|"); if (a.size >= 5) list.addView(smallText("• ${a[0]} | ${a[1]} | Qty: ${a[4]}")) } catch (_: Exception) {} }
            root.addView(actionButton("ADD PRODUCT", "#2563EB") {
                if (pname.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter name", Toast.LENGTH_SHORT).show(); return@actionButton }
                val id = "P${System.currentTimeMillis()}"; prefs.edit().putString(id, "${pname.text}|${pcat.text.ifEmpty { "General" }}|${buy.text.ifEmpty { "0" }}|${sell.text.ifEmpty { "0" }}|${qty.text.ifEmpty { "0" }}").apply()
                list.addView(smallText("• ${pname.text} | ${pcat.text.ifEmpty { "General" }} | Qty:${qty.text.ifEmpty { "0" }}"))
                audit("Product: ${pname.text}"); pname.text.clear(); pcat.text.clear(); buy.text.clear(); sell.text.clear(); qty.text.clear()
            })
            root.addView(list)

        } else if (title == "Price Management") {
            val cat = input("Category or empty=ALL"); val pct = numberInput("Percent", true)
            root.addView(label("Category")); root.addView(cat); root.addView(label("Percent")); root.addView(pct)
            root.addView(actionButton("APPLY PRICE UPDATE", "#DC2626") {
                val per = pct.text.toString().toFloatOrNull()
                if (per == null) { Toast.makeText(this, "Enter %", Toast.LENGTH_SHORT).show(); return@actionButton }
                val prefs = getSharedPreferences("products_db", 0); val ed = prefs.edit(); var c = 0
                prefs.all.forEach { try { val a = it.value.toString().split("|"); if (a.size >= 5 && (cat.text.toString().isEmpty() || a[1].equals(cat.text.toString().trim(), true))) { val ns = (a[3].toFloatOrNull()?: 0f) * (1f + per / 100f); ed.putString(it.key, "${a[0]}|${a[1]}|${a
