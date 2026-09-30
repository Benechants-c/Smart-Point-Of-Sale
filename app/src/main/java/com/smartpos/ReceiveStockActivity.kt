package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*

class ReceiveStockActivity : Activity() {
    data class Line(val name: String, val code: String, val cost: Double, val sell: Double, val qty: Int)
    private val list = mutableListOf<Line>()
    private lateinit var totalView: TextView
    private lateinit var table: LinearLayout

    private fun refresh() {
        table.removeAllViews()
        list.forEach { l ->
            table.addView(TextView(this).apply {
                text = "${l.name} | ${l.code} | ${l.cost} | ${l.sell} | ${l.qty}"
                setPadding(12, 14, 12, 14)
                setTextColor(Color.BLACK)
            })
        }
        var tq = 0
        var tc = 0.0
        list.forEach { tq += it.qty; tc += it.cost * it.qty }
        totalView.text = "Total: ${list.size} Products, Qty: $tq COST: $${String.format("%.1f", tc)}"
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.WHITE)
        }

        root.addView(TextView(this).apply {
            text = "RECEIVE STOCK - NEW (BUILD 113)"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 20)
        })

        val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        topRow.addView(Spinner(this).apply {
            adapter = ArrayAdapter(this@ReceiveStockActivity, android.R.layout.simple_spinner_item, listOf("Optional - Select Supplier"))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        topRow.addView(TextView(this).apply {
            text = java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date())
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        root.addView(topRow)

        val invRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 20, 0, 0) }
        invRow.addView(TextView(this).apply { text = "Invoice No\n[Optional]"; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
        invRow.addView(TextView(this).apply { text = "AUTO-000125"; gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
        root.addView(invRow)

        root.addView(TextView(this).apply {
            text = "Product | Code | Cost | Sell | Qty"
            setBackgroundColor(Color.parseColor("#1E293B"))
            setTextColor(Color.WHITE)
            setPadding(20, 12, 20, 12)
        })

        table = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(table)

        root.addView(Button(this).apply {
            text = "ADD PRODUCT"
            setOnClickListener { showAddDialog() }
        })

        totalView = TextView(this).apply {
            text = "Total: 0 Products"
            setBackgroundColor(Color.parseColor("#DBEAFE"))
            gravity = Gravity.CENTER
            setPadding(20, 12, 20, 12)
        }
        root.addView(totalView)

        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bottom.addView(Button(this).apply { text = "SAVE DRAFT"; layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(0, 0, 4, 0) } })
        bottom.addView(Button(this).apply { text = "PRINT GRN"; layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(4, 0, 4, 0) } })
        bottom.addView(Button(this).apply { text = "COMPLETE RECEIVE"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(4, 0, 0, 0) } })
        root.addView(bottom)

        // Demo data
        if (list.isEmpty()) {
            list.add(Line("Coca-Cola 500ml", "12345", 0.6, 1.0, 24))
            list.add(Line("Mazoe Orange", "67890", 2.5, 3.5, 12))
            list.add(Line("Bread 700g", "45678", 1.2, 1.8, 20))
        }
        refresh()

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun showAddDialog() {
        val dlg = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(30, 20, 30, 20) }
        val nameIn = EditText(this).apply { hint = "Product Name *" }
        val codeIn = EditText(this).apply { hint = "Code * REQUIRED" }
        val costIn = EditText(this).apply { hint = "Cost * REQUIRED e.g. 5.00"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val sellIn = EditText(this).apply { hint = "Selling * REQUIRED e.g. 8.00"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val qtyIn = EditText(this).apply { hint = "Quantity * REQUIRED e.g. 10"; inputType = InputType.TYPE_CLASS_NUMBER }
        val err = TextView(this).apply { setTextColor(Color.RED); textSize = 12f }
        dlg.addView(nameIn); dlg.addView(codeIn); dlg.addView(costIn); dlg.addView(sellIn); dlg.addView(qtyIn); dlg.addView(err)

        val d = AlertDialog.Builder(this).setTitle("Add Product - BUILD 113").setView(dlg)
            .setPositiveButton("ADD", null).setNegativeButton("CANCEL", null).create()
        d.show()
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val n = nameIn.text.toString().trim()
            val c = codeIn.text.toString().trim()
            val coS = costIn.text.toString().trim()
            val seS = sellIn.text.toString().trim()
            val qS = qtyIn.text.toString().trim()
            if (n.isEmpty()) { err.text = "❌ Name required"; return@setOnClickListener }
            if (c.isEmpty()) { err.text = "❌ Code required! No more empty code"; return@setOnClickListener }
            if (coS.isEmpty()) { err.text = "❌ Cost required"; return@setOnClickListener }
            if (seS.isEmpty()) { err.text = "❌ Selling required"; return@setOnClickListener }
            if (qS.isEmpty()) { err.text = "❌ Quantity required"; return@setOnClickListener }
            val co = coS.toDoubleOrNull(); if (co == null || co <= 0) { err.text = "❌ Cost must be >0"; return@setOnClickListener }
            val se = seS.toDoubleOrNull(); if (se == null || se <= 0) { err.text = "❌ Selling must be >0"; return@setOnClickListener }
            val q = qS.toIntOrNull(); if (q == null || q <= 0) { err.text = "❌ Qty must be >0"; return@setOnClickListener }
            list.add(Line(n, c, co, se, q))
            refresh()
            d.dismiss()
            Toast.makeText(this, "✅ $n | $c | $q ADDED", Toast.LENGTH_SHORT).show()
        }
    }
}
