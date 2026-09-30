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

class ReceiveStockActivity : Activity() {
    data class Line(val name: String, val code: String, val cost: Double, val sell: Double, val qty: Int)
    private val list = mutableListOf<Line>()
    private lateinit var totalView: TextView
    private lateinit var table: LinearLayout

    private fun label(t: String): TextView = TextView(this).apply {
        text = t; textSize = 11f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor("#1E293B")); setPadding(12, 6, 12, 6)
    }

    private fun getNames(): List<String> {
        val out = mutableListOf<String>()
        try {
            val db = openOrCreateDatabase("products_db", Context.MODE_PRIVATE, null)
            val c = db.rawQuery("SELECT name FROM products", null)
            while (c.moveToNext()) { out.add(c.getString(0)) }
            c.close(); db.close()
        } catch (e: Exception) {}
        if (out.isEmpty()) {
            out.add("Coca-Cola 500ml"); out.add("Mazoe Orange"); out.add("Bread 700g")
            out.add("Cake Chocolate"); out.add("Cake Vanilla"); out.add("Sugar 2kg")
            out.add("Rice 2kg"); out.add("Cooking Oil 2L")
        }
        return out
    }

    private fun refresh() {
        table.removeAllViews()
        for (l in list) {
            table.addView(TextView(this).apply {
                text = l.name + " | " + l.code + " | " + l.cost + " | " + l.sell + " | " + l.qty
                setPadding(12, 14, 12, 14)
            })
        }
        var tq = 0; var tc = 0.0
        for (l in list) { tq += l.qty; tc += l.cost * l.qty }
        totalView.text = "Total: " + list.size + " Products, Qty: " + tq + " COST: $" + String.format("%.1f", tc)
    }

    private fun printGrn() {
        if (list.isEmpty()) { Toast.makeText(this, "Nothing", Toast.LENGTH_SHORT).show(); return }
        val date = java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())
        var txt = "GRN - " + date + "\n\n"
        for (l in list) { txt += l.name + " | " + l.code + " | " + l.qty + "\n" }
        val i = android.content.Intent(android.content.Intent.ACTION_SEND)
        i.type = "text/plain"; i.putExtra(android.content.Intent.EXTRA_TEXT, txt)
        startActivity(android.content.Intent.createChooser(i, "PRINT GRN"))
    }

    private fun completeReceive() {
        if (list.isEmpty()) { Toast.makeText(this, "Add first", Toast.LENGTH_SHORT).show(); return }
        try {
            val db = openOrCreateDatabase("products_db", Context.MODE_PRIVATE, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, code TEXT, cost REAL, price REAL, qty INTEGER)")
            for (l in list) {
                db.execSQL("INSERT INTO products(name,code,cost,price,qty) VALUES('" + l.name + "','" + l.code + "'," + l.cost + "," + l.sell + "," + l.qty + ")")
            }
            db.close()
        } catch (e: Exception) {}
        Toast.makeText(this, "RECEIVE COMPLETE", Toast.LENGTH_LONG).show()
        list.clear(); refresh()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 16, 16, 16); setBackgroundColor(Color.WHITE) }
        root.addView(TextView(this).apply { text = "RECEIVE STOCK - NEW (BUILD 119)"; textSize = 18f; setTypeface(null, Typeface.BOLD); setPadding(0, 0, 0, 20) })
        root.addView(TextView(this).apply { text = "Product | Code | Cost | Sell | Qty"; setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(20, 12, 20, 12) })
        table = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; root.addView(table)
        root.addView(Button(this).apply { text = "ADD PRODUCT"; setOnClickListener { showAddDialog() } })
        totalView = TextView(this).apply { text = "Total: 0"; setBackgroundColor(Color.parseColor("#DBEAFE")); gravity = Gravity.CENTER; setPadding(20, 12, 20, 12) }; root.addView(totalView)
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bottom.addView(Button(this).apply { text = "PRINT GRN"; layoutParams = LinearLayout.LayoutParams(0, -2, 1f); setOnClickListener { printGrn() } })
        bottom.addView(Button(this).apply { text = "COMPLETE RECEIVE"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, -2, 1f); setOnClickListener { completeReceive() } })
        root.addView(bottom)
        list.add(Line("Coca-Cola 500ml", "12345", 0.6, 1.0, 24)); refresh()
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun showAddDialog() {
        val names = getNames()

        val dlg = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 20, 24, 20) }

        // DROPDOWN SEARCH FIELD
        val nameInput = AutoCompleteTextView(this).apply {
            hint = "Type to search product name"
            setPadding(20, 14, 20, 14)
            setAdapter(ArrayAdapter(this@ReceiveStockActivity, android.R.layout.simple_dropdown_item_1line, names))
            threshold = 1
        }
        val codeInput = EditText(this).apply { hint = "Code *"; setPadding(20, 14, 20, 14) }
        val costInput = EditText(this).apply { hint = "Cost *"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setPadding(20, 14, 20, 14) }
        val sellInput = EditText(this).apply { hint = "Sell *"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setPadding(20, 14, 20, 14) }
        val qtyInput = EditText(this).apply { hint = "Qty *"; inputType = InputType.TYPE_CLASS_NUMBER; setPadding(20, 14, 20, 14) }
        val err = TextView(this).apply { setTextColor(Color.RED) }

        dlg.addView(label("Product Name * (Type to search)")); dlg.addView(nameInput)
        dlg.addView(label("Code *")); dlg.addView(codeInput)
        dlg.addView(label("Cost *")); dlg.addView(costInput)
        dlg.addView(label("Selling *")); dlg.addView(sellInput)
        dlg.addView(label("Quantity *")); dlg.addView(qtyInput)
        dlg.addView(err)

        val d = AlertDialog.Builder(this).setTitle("Add Product - BUILD 119 - SEARCH DROPDOWN").setView(dlg).setPositiveButton("ADD", null).setNegativeButton("CANCEL", null).create()
        d.show()
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val n = nameInput.text.toString().trim()
            val c = codeInput.text.toString().trim()
            val coS = costInput.text.toString().trim()
            val seS = sellInput.text.toString().trim()
            val qS = qtyInput.text.toString().trim()
            if (n.isEmpty()) { err.text = "Name required"; return@setOnClickListener }
            if (c.isEmpty()) { err.text = "Code required"; return@setOnClickListener }
            if (coS.isEmpty()) { err.text = "Cost required"; return@setOnClickListener }
            if (seS.isEmpty()) { err.text = "Sell required"; return@setOnClickListener }
            if (qS.isEmpty()) { err.text = "Qty required"; return@setOnClickListener }
            list.add(Line(n, c, coS.toDouble(), seS.toDouble(), qS.toInt()))
            refresh(); d.dismiss()
        }
    }
}
