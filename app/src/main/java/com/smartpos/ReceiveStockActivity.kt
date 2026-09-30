package com.smartpos

import android.app.Activity
import android.app.AlertDialog
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
        text = t.uppercase()
        textSize = 11f
        setTypeface(null, Typeface.BOLD)
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor("#1E293B"))
        setPadding(12, 6, 12, 6)
    }
    private fun box(h: String): EditText = EditText(this).apply {
        hint = h
        setPadding(20, 12, 20, 12)
        setBackgroundColor(Color.WHITE)
    }

    private fun refresh() {
        table.removeAllViews()
        list.forEach { l ->
            table.addView(TextView(this).apply {
                text = "${l.name} | ${l.code} | ${l.cost} | ${l.sell} | ${l.qty}"
                setPadding(12, 14, 12, 14)
            })
        }
        var tq = 0; var tc = 0.0
        list.forEach { tq += it.qty; tc += it.cost * it.qty }
        totalView.text = "Total: ${list.size} Products, Qty: $tq COST: $${String.format("%.1f", tc)}"
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 16, 16, 16); setBackgroundColor(Color.WHITE) }
        root.addView(TextView(this).apply { text = "RECEIVE STOCK - NEW (BUILD 114)"; textSize = 18f; setTypeface(null, Typeface.BOLD); setPadding(0, 0, 0, 20) })
        val topRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        topRow.addView(Spinner(this).apply { adapter = ArrayAdapter(this@ReceiveStockActivity, android.R.layout.simple_spinner_item, listOf("Optional - Select Supplier")) }, LinearLayout.LayoutParams(0, -2, 1f))
        topRow.addView(TextView(this).apply { text = java.text.SimpleDateFormat("dd/MM/yyyy").format(java.util.Date()); gravity = Gravity.END; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
        root.addView(topRow)
        root.addView(TextView(this).apply { text = "Product | Code | Cost | Sell | Qty"; setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(20, 12, 20, 12) })
        table = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(table)
        root.addView(Button(this).apply { text = "ADD PRODUCT"; setOnClickListener { showAddDialog() } })
        totalView = TextView(this).apply { text = "Total: 0"; setBackgroundColor(Color.parseColor("#DBEAFE")); gravity = Gravity.CENTER; setPadding(20, 12, 20, 12) }
        root.addView(totalView)
        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bottom.addView(Button(this).apply { text = "SAVE DRAFT"; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
        bottom.addView(Button(this).apply { text = "PRINT GRN"; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
        bottom.addView(Button(this).apply { text = "COMPLETE RECEIVE"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
        root.addView(bottom)
        if (list.isEmpty()) {
            list.add(Line("Coca-Cola 500ml", "12345", 0.6, 1.0, 24))
            list.add(Line("Mazoe Orange", "67890", 2.5, 3.5, 12))
        }
        refresh()
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun showAddDialog() {
        val dlg = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 20, 24, 20) }
        val nameIn = box("e.g. cake"); val codeIn = box("e.g. CK01")
        val costIn = box("e.g. 5.00").apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val sellIn = box("e.g. 8.00").apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        val qtyIn = box("e.g. 10").apply { inputType = InputType.TYPE_CLASS_NUMBER }
        val err = TextView(this).apply { setTextColor(Color.RED); textSize = 12f }

        dlg.addView(label("Product Name *")); dlg.addView(nameIn)
        dlg.addView(label("Code * REQUIRED")); dlg.addView(codeIn)
        dlg.addView(label("Cost * REQUIRED")); dlg.addView(costIn)
        dlg.addView(label("Selling * REQUIRED")); dlg.addView(sellIn)
        dlg.addView(label("Quantity * REQUIRED")); dlg.addView(qtyIn)
        dlg.addView(err)

        val d = AlertDialog.Builder(this).setTitle("Add Product - BUILD 114 (Fixed Titles)").setView(dlg)
            .setPositiveButton("ADD", null).setNegativeButton("CANCEL", null).create()
        d.show()
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val n = nameIn.text.toString().trim(); val c = codeIn.text.toString().trim()
            val coS = costIn.text.toString().trim(); val seS = sellIn.text.toString().trim(); val qS = qtyIn.text.toString().trim()
            if (n.isEmpty()) { err.text = "❌ Product Name required"; return@setOnClickListener }
            if (c.isEmpty()) { err.text = "❌ Code required"; return@setOnClickListener }
            if (coS.isEmpty()) { err.text = "❌ Cost required"; return@setOnClickListener }
            if (seS.isEmpty()) { err.text = "❌ Selling required"; return@setOnClickListener }
            if (qS.isEmpty()) { err.text = "❌ Quantity required"; return@setOnClickListener }
            val co = coS.toDoubleOrNull(); if (co == null || co <= 0) { err.text = "❌ Cost >0"; return@setOnClickListener }
            val se = seS.toDoubleOrNull(); if (se == null || se <= 0) { err.text = "❌ Selling >0"; return@setOnClickListener }
            val q = qS.toIntOrNull(); if (q == null || q <= 0) { err.text = "❌ Qty >0"; return@setOnClickListener }
            list.add(Line(n, c, co, se, q)); refresh(); d.dismiss()
            Toast.makeText(this, "✅ $n Added", Toast.LENGTH_SHORT).show()
        }
    }
}
