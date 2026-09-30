package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.widget.*

class ReceiveStockActivity : Activity() {
    data class Line(val name: String, val code: String, val cost: Double, val sell: Double, val qty: Int)
    data class Prod(val name: String, val code: String, val cost: Double, val price: Double)

    private val list = mutableListOf<Line>()
    private lateinit var totalView: TextView
    private lateinit var table: LinearLayout

    private fun label(t: String): TextView = TextView(this).apply {
        text = t; textSize = 11f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor("#1E293B")); setPadding(12, 6, 12, 6)
    }

    // ONLY REAL DATA FROM YOUR SYSTEM - NO DEMO
    private fun getAllProducts(): List<Prod> {
        val out = mutableListOf<Prod>()
        try {
            val db = openOrCreateDatabase("products_db", Context.MODE_PRIVATE, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, code TEXT, cost REAL, price REAL, qty INTEGER)")
            val c = db.rawQuery("SELECT name, code, cost, price FROM products", null)
            while (c.moveToNext()) {
                val n = c.getString(0); val co = c.getString(1)
                if (n!= null && co!= null && n.isNotEmpty() && co.isNotEmpty()) {
                    out.add(Prod(n, co, c.getDouble(2), c.getDouble(3)))
                }
            }
            c.close(); db.close()
        } catch (e: Exception) {}
        return out // NO DEMO, only real
    }

    private fun refresh() {
        table.removeAllViews()
        for (l in list) { table.addView(TextView(this).apply { text = l.name + " | " + l.code + " | " + l.cost + " | " + l.sell + " | " + l.qty; setPadding(12,14,12,14) }) }
        var tq = 0; var tc = 0.0; for (l in list) { tq += l.qty; tc += l.cost * l.qty }
        totalView.text = "Total: " + list.size + " | Qty: " + tq + " | Cost: $" + String.format("%.1f", tc)
    }

    private fun printGrn() {
        if (list.isEmpty()) { Toast.makeText(this, "Nothing", Toast.LENGTH_SHORT).show(); return }
        var txt = "GRN\n"; for (l in list) { txt += l.name + " | " + l.code + " | " + l.qty + "\n" }
        val i = android.content.Intent(android.content.Intent.ACTION_SEND); i.type = "text/plain"; i.putExtra(android.content.Intent.EXTRA_TEXT, txt)
        startActivity(android.content.Intent.createChooser(i, "PRINT GRN"))
    }

    private fun completeReceive() {
        if (list.isEmpty()) { return }
        try {
            val db = openOrCreateDatabase("products_db", Context.MODE_PRIVATE, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, code TEXT, cost REAL, price REAL, qty INTEGER)")
            for (l in list) { db.execSQL("INSERT INTO products(name,code,cost,price,qty) VALUES('" + l.name.replace("'","") + "','" + l.code + "'," + l.cost + "," + l.sell + "," + l.qty + ")") }
            db.close()
        } catch (e: Exception) {}
        Toast.makeText(this, "RECEIVE COMPLETE", Toast.LENGTH_LONG).show(); list.clear(); refresh()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundColor(Color.WHITE) }
        root.addView(TextView(this).apply { text = "RECEIVE STOCK - BUILD 121 REAL DATA"; textSize = 18f; setTypeface(null, Typeface.BOLD); setPadding(0,0,0,20) })
        root.addView(TextView(this).apply { text = "Product | Code | Cost | Sell | Qty"; setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(20,12,20,12) })
        table = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; root.addView(table)
        root.addView(Button(this).apply { text = "ADD PRODUCT"; setOnClickListener { showAddDialog() } })
        totalView = TextView(this).apply { text = "Total: 0"; setBackgroundColor(Color.parseColor("#DBEAFE")); setPadding(20,12,20,12) }; root.addView(totalView)
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bottom.addView(Button(this).apply { text = "PRINT GRN"; layoutParams = LinearLayout.LayoutParams(0,-2,1f); setOnClickListener { printGrn() } })
        bottom.addView(Button(this).apply { text = "COMPLETE RECEIVE"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0,-2,1f); setOnClickListener { completeReceive() } })
        root.addView(bottom)
        refresh()
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun showAddDialog() {
        val all = getAllProducts()
        val nameList = all.map { it.name }
        val codeList = all.map { it.code }
        val byName = all.associateBy { it.name }
        val byCode = all.associateBy { it.code }

        val dlg = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,20,24,20) }

        val nameInput = AutoCompleteTextView(this).apply {
            hint = "Search REAL product name from your stock"
            setPadding(20,14,20,14); threshold = 1
            setAdapter(ArrayAdapter(this@ReceiveStockActivity, android.R.layout.simple_dropdown_item_1line, nameList))
        }
        val codeInput = AutoCompleteTextView(this).apply {
            hint = "Search REAL code from your stock"
            setPadding(20,14,20,14); threshold = 1
            setAdapter(ArrayAdapter(this@ReceiveStockActivity, android.R.layout.simple_dropdown_item_1line, codeList))
        }
        val costInput = EditText(this).apply { hint = "Cost *"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setPadding(20,14,20,14) }
        val sellInput = EditText(this).apply { hint = "Sell *"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setPadding(20,14,20,14) }
        val qtyInput = EditText(this).apply { hint = "Qty *"; inputType = InputType.TYPE_CLASS_NUMBER; setPadding(20,14,20,14) }
        val err = TextView(this).apply { setTextColor(Color.RED) }

        // NAME -> CODE (Real data only)
        nameInput.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            val sel = nameInput.adapter.getItem(pos).toString()
            val p = byName[sel]
            if (p!= null) {
                codeInput.setText(p.code) // REAL code you saved
                costInput.setText(p.cost.toString())
                sellInput.setText(p.price.toString())
            }
        }

        // CODE -> NAME (Real data only)
        codeInput.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            val sel = codeInput.adapter.getItem(pos).toString()
            val p = byCode[sel]
            if (p!= null) {
                nameInput.setText(p.name) // REAL name you saved
                costInput.setText(p.cost.toString())
                sellInput.setText(p.price.toString())
            }
        }

        dlg.addView(label("Product Name * (REAL SEARCH)")); dlg.addView(nameInput)
        dlg.addView(label("Code * (REAL SEARCH - Your saved code)")); dlg.addView(codeInput)
        dlg.addView(label("Cost *")); dlg.addView(costInput)
        dlg.addView(label("Selling *")); dlg.addView(sellInput)
        dlg.addView(label("Quantity *")); dlg.addView(qtyInput)
        dlg.addView(err)

        // Show count of real products
        if (all.isEmpty()) {
            err.text = "⚠️ No products in system yet. Add new product manually."
            err.setTextColor(Color.parseColor("#D97706"))
        } else {
            dlg.addView(TextView(this).apply { text = "Found " + all.size + " real products in your system"; textSize = 10f; setTextColor(Color.GRAY); setPadding(0,8,0,0) })
        }

        val d = AlertDialog.Builder(this).setTitle("Add Product - BUILD 121 - REAL DATA ONLY").setView(dlg).setPositiveButton("ADD", null).setNegativeButton("CANCEL", null).create()
        d.show()
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val n = nameInput.text.toString().trim(); val c = codeInput.text.toString().trim()
            val coS = costInput.text.toString().trim(); val seS = sellInput.text.toString().trim(); val qS = qtyInput.text.toString().trim()
            if (n.isEmpty()) { err.text = "Name required"; err.setTextColor(Color.RED); return@setOnClickListener }
            if (c.isEmpty()) { err.text = "Code required - use your saved code"; err.setTextColor(Color.RED); return@setOnClickListener }
            if (coS.isEmpty()) { err.text = "Cost required"; return@setOnClickListener }
            if (seS.isEmpty()) { err.text = "Sell required"; return@setOnClickListener }
            if (qS.isEmpty()) { err.text = "Qty required"; return@setOnClickListener }
            list.add(Line(n,c,coS.toDouble(),seS.toDouble(),qS.toInt())); refresh(); d.dismiss()
        }
    }
}
