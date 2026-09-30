package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import android.widget.AutoCompleteTextView

class ReceiveStockActivity : Activity() {
    data class Line(val name: String, val code: String, val cost: Double, val sell: Double, val qty: Int)
    private val list = mutableListOf<Line>()
    private lateinit var totalView: TextView
    private lateinit var table: LinearLayout

    private fun label(t: String): TextView = TextView(this).apply {
        text = t; textSize = 11f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor("#1E293B")); setPadding(12, 6, 12, 6)
    }

    private fun getProductNames(): List<String> {
        val out = mutableListOf<String>()
        try {
            val db = openOrCreateDatabase("products_db", Context.MODE_PRIVATE, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, code TEXT, cost REAL, price REAL, qty INTEGER)")
            val cur = db.rawQuery("SELECT name FROM products", null)
            while (cur.moveToNext()) { out.add(cur.getString(0)) }
            cur.close(); db.close()
        } catch (e: Exception) {}
        if (out.isEmpty()) {
            out.add("Coca-Cola 500ml"); out.add("Mazoe Orange"); out.add("Bread 700g")
            out.add("Cake Chocolate"); out.add("Cake Vanilla"); out.add("Sugar 2kg")
        }
        return out.distinct()
    }

    private fun getProductByName(name: String): Array<String>? {
        try {
            val db = openOrCreateDatabase("products_db", Context.MODE_PRIVATE, null)
            val cur = db.rawQuery("SELECT code, cost, price FROM products WHERE name=? LIMIT 1", arrayOf(name))
            if (cur.moveToFirst()) {
                val c = cur.getString(0); val co = cur.getDouble(1).toString(); val pr = cur.getDouble(2).toString()
                cur.close(); db.close(); return arrayOf(c, co, pr)
            }
            cur.close(); db.close()
        } catch (e: Exception) {}
        return null
    }

    private fun refresh() {
        table.removeAllViews()
        for (l in list) {
            table.addView(TextView(this).apply {
                text = l.name + " | " + l.code + " | " + l.cost + " | " + l.sell + " | " + l.qty
                setPadding(12, 14, 12, 14)
                setOnLongClickListener {
                    AlertDialog.Builder(this@ReceiveStockActivity).setTitle("Delete " + l.name + "?")
                        .setPositiveButton("Delete") { _, _ -> list.remove(l); refresh() }
                        .setNegativeButton("Cancel", null).show(); true
                }
            })
        }
        var tq = 0; var tc = 0.0
        for (l in list) { tq += l.qty; tc += l.cost * l.qty }
        totalView.text = "Total: " + list.size + " Products, Qty: " + tq + " COST: $" + String.format("%.1f", tc)
    }

    private fun printGrn() {
        if (list.isEmpty()) { Toast.makeText(this, "Nothing to print", Toast.LENGTH_SHORT).show(); return }
        val date = java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())
        val sb = StringBuilder()
        sb.append("=== GRN ===\nDate: " + date + "\n\n")
        for (l in list) { sb.append(l.name + " | " + l.code + " | " + l.cost + " | " + l.sell + " | " + l.qty + "\n") }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
        startActivity(android.content.Intent.createChooser(intent, "PRINT GRN"))
    }

    private fun completeReceive() {
        if (list.isEmpty()) { Toast.makeText(this, "Add products first", Toast.LENGTH_SHORT).show(); return }
        try {
            val db = openOrCreateDatabase("products_db", Context.MODE_PRIVATE, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, code TEXT, cost REAL, price REAL, qty INTEGER)")
            for (l in list) {
                val cur = db.rawQuery("SELECT qty FROM products WHERE code=?", arrayOf(l.code))
                if (cur.moveToFirst()) {
                    db.execSQL("UPDATE products SET qty = qty + " + l.qty + ", cost=" + l.cost + ", price=" + l.sell + ", name='" + l.name.replace("'","") + "' WHERE code='" + l.code + "'")
                } else {
                    db.execSQL("INSERT INTO products(name,code,cost,price,qty) VALUES('" + l.name.replace("'","") + "','" + l.code + "'," + l.cost + "," + l.sell + "," + l.qty + ")")
                }
                cur.close()
            }
            db.close()
        } catch (e: Exception) { Toast.makeText(this, "DB Error: " + e.message, Toast.LENGTH_LONG).show(); return }
        Toast.makeText(this, "RECEIVE COMPLETE - " + list.size + " products!", Toast.LENGTH_LONG).show()
        list.clear(); refresh()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 16, 16, 16); setBackgroundColor(Color.WHITE) }
        root.addView(TextView(this).apply { text = "RECEIVE STOCK - NEW (BUILD 118)"; textSize = 18f; setTypeface(null, Typeface.BOLD); setPadding(0, 0, 0, 20) })

        val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        topRow.addView(Spinner(this).apply { adapter = ArrayAdapter(this@ReceiveStockActivity, android.R.layout.simple_spinner_item, listOf("Optional - Select Supplier")) }, LinearLayout.LayoutParams(0, -2, 1f))
        topRow.addView(TextView(this).apply { text = java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date()); gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
        root.addView(topRow)

        root.addView(TextView(this).apply { text = "Product | Code | Cost | Sell | Qty"; setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(20, 12, 20, 12) })
        table = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; root.addView(table)
        root.addView(Button(this).apply { text = "ADD PRODUCT"; setOnClickListener { showAddDialog() } })
        totalView = TextView(this).apply { text = "Total: 0"; setBackgroundColor(Color.parseColor("#DBEAFE")); gravity = Gravity.CENTER; setPadding(20, 12, 20, 12) }; root.addView(totalView)

        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 10, 0, 0) }
        bottom.addView(Button(this).apply { text = "SAVE DRAFT"; layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0, 0, 4, 0) }; setOnClickListener { Toast.makeText(this@ReceiveStockActivity, "Draft Saved", Toast.LENGTH_SHORT).show() } })
        bottom.addView(Button(this).apply { text = "PRINT GRN"; layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(4, 0, 4, 0) }; setOnClickListener { printGrn() } })
        bottom.addView(Button(this).apply { text = "COMPLETE RECEIVE"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(4, 0, 0, 0) }; setOnClickListener { completeReceive() } })
        root.addView(bottom)

        list.add(Line("Coca-Cola 500ml", "12345", 0.6, 1.0,
