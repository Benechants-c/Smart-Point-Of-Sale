package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.content.SharedPreferences

class LoginActivity : Activity() {
    lateinit var pref: SharedPreferences
    var stock = mutableMapOf<String, Item>()
    var receiving = mutableListOf<Item>()
    var supplier = "ABC Wholesalers"

    data class Item(var name: String, var code: String, var cost: Double, var sell: Double, var qty: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("supermart_final", 0)
        load()
        login()
    }

    fun load() {
        val s = pref.getString("stock", "")?: ""
        if (s.isNotEmpty()) {
            for (row in s.split(";;")) {
                val p = row.split("|")
                if (p.size == 5) stock[p[1]] = Item(p[0], p[1], p[2].toDoubleOrNull()?: 0.0, p[3].toDoubleOrNull()?: 0.0, p[4].toIntOrNull()?: 0)
            }
        }
    }

    fun save() {
        var str = ""
        var first = true
        for (it in stock.values) {
            if (!first) str += ";;"
            str += it.name + "|" + it.code + "|" + it.cost + "|" + it.sell + "|" + it.qty
            first = false
        }
        pref.edit().putString("stock", str).apply()
    }

    fun login() {
        val lay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 250, 60, 60)
            setBackgroundColor(0xFF0B2447.toInt())
            gravity = Gravity.CENTER
        }
        val title = TextView(this).apply {
            text = "SuperMart POS\nUSD EDITION\nSHOP001-HRE"
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        val pin = EditText(this).apply {
            hint = "PIN 1234"
            inputType = 129
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val btn = Button(this).apply {
            text = "LOGIN"
            setBackgroundColor(0xFF1967D2.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }
        btn.setOnClickListener {
            if (pin.text.toString() == "1234") receivingScreen()
            else Toast.makeText(this, "PIN is 1234", Toast.LENGTH_SHORT).show()
        }
        lay.addView(title); lay.addView(pin); lay.addView(btn)
        setContentView(lay)
    }

    fun receivingScreen() {
        val scroll = ScrollView(this)
        val main = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(0xFFF8FAFC.toInt())
        }

        // Header
        val header = TextView(this).apply {
            text = " SuperMart POS - Receiving (USD)\n 27 Sep 2026 10:24 | SHOP001-HRE"
            textSize = 15f
            setPadding(15, 15, 15, 15)
            setBackgroundColor(0xFF0B4DA2.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        // Supplier Info
        val card1 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(0xFFFFFFFF.toInt())
        }
        val lblSup = TextView(this).apply { text = "Supplier | Invoice No. | Date | PO No."; textSize = 11f }
        val rowSup = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val sp = Spinner(this)
        val sups = arrayOf("ABC Wholesalers", "Cold Chain", "Bakers Inn", "Zam Bevs")
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sups)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(a: AdapterView<*>?, v: android.view.View?, p: Int, id: Long) { supplier = sups[p] }
            override fun onNothingSelected(a: AdapterView<*>?) {}
        }
        val eInv = EditText(this).apply { hint = "INV-0045"; setPadding(8, 8, 8, 8) }
        val eDate = EditText(this).apply { hint = "27/09/2026"; setPadding(8, 8, 8, 8) }
        val ePO = EditText(this).apply { hint = "PO-1234"; setPadding(8, 8, 8, 8) }
        rowSup.addView(sp); rowSup.addView(eInv); rowSup.addView(eDate); rowSup.addView(ePO)

        // Input Section
        val lblScan = TextView(this).apply { text = "Scan or Enter Product Code / Name"; setPadding(0, 15, 0, 5); textSize = 13f }
        val eName = EditText(this).apply { hint = "Product Name (e.g. White Bread)"; setBackgroundColor(0xFFFFFFFF.toInt()) }
        val rowInput = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val eCode = EditText(this).apply { hint = "Code BRD001"; setBackgroundColor(0xFFFFFFFF.toInt()) }
        val eCost = EditText(this).apply { hint = "Cost (USD)"; inputType = 8194 }
        val eSell = EditText(this).apply { hint = "Selling (USD)"; inputType = 8194 }
        val eQty = EditText(this).apply { hint = "Qty"; inputType = 2 }
        rowInput.addView(eCode); rowInput.addView(eCost); rowInput.addView(eSell); rowInput.addView(eQty)

        val btnAdd = Button(this).apply {
            text = "+ Add Item"
            setBackgroundColor(0xFF1967D2.toInt())
            setTextColor(0xFFFFFFFF.toInt())
        }

        // Table
        val th = TextView(this).apply {
            text = "# Product Name Code Cost (USD) Selling (USD) Qty Total (USD)"
            setPadding(8, 12, 8, 12)
            setBackgroundColor(0xFFE8F0FE.toInt())
            textSize = 11f
        }
        val tvTable = TextView(this).apply {
            text = "No items - Add above"
            setPadding(10, 10, 10, 10)
            setBackgroundColor(0xFFFFFFFF.toInt())
            textSize = 13f
        }
        val tvGrand = TextView(this).apply {
            text = "Total Items: 0 Grand Total (USD): $0.00"
            gravity = Gravity.RIGHT
            textSize = 16f
            setPadding(10, 15, 10, 15)
            setBackgroundColor(0xFFF1F5F9.toInt())
        }

        // Buttons
        val rowBtn = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, 15, 0, 0) }
        val btnClear = Button(this).apply { text = "Clear"; setBackgroundColor(0xFF9AA0A6.toInt()) }
        val btnDraft = Button(this).apply { text = "Save Draft"; setBackgroundColor(0xFF1E8E3E.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        val btnPost = Button(this).apply { text = "Post Receiving"; setBackgroundColor(0xFF0B4DA2.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        rowBtn.addView(btnClear); rowBtn.addView(btnDraft); rowBtn.addView(btnPost)

        fun refresh() {
            if (receiving.isEmpty()) {
                tvTable.text = "No items - Add above"
                tvGrand.text = "Total Items: 0 Grand Total (USD): $0.00"
                return
            }
            var txt = ""
            var grand = 0.0
            var i = 1
            for (it in receiving) {
                val tot = it.cost * it.qty
                txt += i.toString() + " " + it.name + " " + it.code + " $" + it.cost + " $" + it.sell + " " + it.qty + " $" + String.format("%.2f", tot) + "\n"
                grand += tot
                i++
            }
            tvTable.text = txt
            tvGrand.text = "Total Items: " + receiving.size + " Grand Total (USD): $" + String.format("%.2f", grand)
        }

        btnAdd.setOnClickListener {
            if (eName.text.isEmpty() || eCode.text.isEmpty() || eCost.text.isEmpty() || eSell.text.isEmpty() || eQty.text.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                val c = eCost.text.toString().toDouble()
                val s = eSell.text.toString().toDouble()
                if (s < c) {
                    Toast.makeText(this, "DENIED! Selling $" + s + " < Cost $" + c, Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                val item = Item(eName.text.toString(), eCode.text.toString().uppercase(), c, s, eQty.text.toString().toInt())
                receiving.add(item)
                refresh()
                eName.text.clear(); eCode.text.clear(); eCost.text.clear(); eSell.text.clear(); eQty.text.clear()
            } catch (e: Exception) {
                Toast.makeText(this, "Check numbers", Toast.LENGTH_SHORT).show()
            }
        }

        btnClear.setOnClickListener { receiving.clear(); refresh() }
        btnDraft.setOnClickListener { Toast.makeText(this, "Draft saved for " + supplier, Toast.LENGTH_SHORT).show() }
        btnPost.setOnClickListener {
            if (receiving.isEmpty()) return@setOnClickListener
            for (it in receiving) {
                val ex = stock[it.code]
                if (ex!= null) { ex.qty += it.qty; ex.cost = it.cost; ex.sell = it.sell; ex.name = it.name }
                else stock[it.code] = it
            }
            save()
            Toast.makeText(this, "Posted to STOCK: " + receiving.size + " items from " + supplier + " (USD)", Toast.LENGTH_LONG).show()
            receiving.clear()
            refresh()
        }

        card1.addView(lblSup); card1.addView(rowSup)
        main.addView(header)
        main.addView(card1)
        main.addView(lblScan)
        main.addView(eName)
        main.addView(rowInput)
        main.addView(btnAdd)
        main.addView(th)
        main.addView(tvTable)
        main.addView(tvGrand)
        main.addView(rowBtn)

        scroll.addView(main)
        setContentView(scroll)
    }
}
