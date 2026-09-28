package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.content.SharedPreferences

class LoginActivity : Activity() {
    lateinit var pref: SharedPreferences
    var stock = mutableMapOf<String, Item>()
    var list = mutableListOf<Item>()
    var supplier = "Select Supplier"

    data class Item(var name: String, var code: String, var cost: Double, var sell: Double, var qty: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("smartshop_final", 0)
        load()
        login()
    }

    fun load() {
        val s = pref.getString("stock", "")?: ""
        if (s.isNotEmpty()) for (row in s.split(";;")) {
            val p = row.split("|")
            if (p.size == 5) stock[p[1]] = Item(p[0], p[1], p[2].toDoubleOrNull()?:0.0, p[3].toDoubleOrNull()?:0.0, p[4].toIntOrNull()?:0)
        }
    }

    fun save() {
        var str = ""; var first = true
        for (it in stock.values) {
            if (!first) str += ";;"
            str += it.name + "|" + it.code + "|" + it.cost + "|" + it.sell + "|" + it.qty
            first = false
        }
        pref.edit().putString("stock", str).apply()
    }

    fun login() {
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(60,250,60,60); gravity = Gravity.CENTER; setBackgroundColor(0xFF0A1931.toInt()) }
        val t = TextView(this).apply { text = "SmartShop POS\nSHOP001-HRE\nHarare"; textSize = 22f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER; setPadding(0,0,0,40) }
        val pin = EditText(this).apply { hint = "PIN 1234"; inputType = 129; setBackgroundColor(0xFFFFFFFF.toInt()) }
        val btn = Button(this).apply { text = "LOGIN"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        btn.setOnClickListener { if (pin.text.toString()=="1234") receivingUI() else Toast.makeText(this,"1234",Toast.LENGTH_SHORT).show() }
        lay.addView(t); lay.addView(pin); lay.addView(btn)
        setContentView(lay)
    }

    fun receivingUI() {
        // Main container
        val root = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(0xFFF1F5F9.toInt()) }

        // LEFT SIDEBAR - YOUR OWN MENU
        val sidebar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF0A1931.toInt())
            setPadding(10, 20, 10, 20)
        }
        val menu = arrayOf("🏠\nHome", "🛒\nSales", "🚚\nReceiving", "📦\nStock", "👥\nSuppliers", "📊\nReports", "⚙️\nSettings")
        for (m in menu) {
            val tv = TextView(this).apply {
                text = m
                setTextColor(if (m.contains("Receiving")) 0xFFFFFFFF.toInt() else 0xFF94A3B8.toInt())
                setBackgroundColor(if (m.contains("Receiving")) 0xFF185ADB.toInt() else 0x00000000)
                gravity = Gravity.CENTER
                setPadding(15, 25, 15, 25)
                textSize = 11f
            }
            sidebar.addView(tv)
        }

        // RIGHT CONTENT
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(10, 10, 10, 10) }

        // TOP BAR
        val topBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xFF185ADB.toInt())
            setPadding(15, 15, 15, 15)
        }
        val topTitle = TextView(this).apply { text = "🛒 SmartShop POS"; setTextColor(0xFFFFFFFF.toInt()); textSize = 16f }
        val topDate = TextView(this).apply { text = " 27 Sep 2026 10:24"; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.RIGHT }
        topBar.addView(topTitle); topBar.addView(topDate)

        // RECEIVING TITLE
        val titleRec = TextView(this).apply { text = "Receiving"; textSize = 20f; setPadding(10, 15, 10, 15); setTextColor(0xFF0A1931.toInt()) }

        // FORM CARD - YOUR OWN FIELDS (EMPTY)
        val formCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(12, 12, 12, 12) }
        val rowLabels = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        rowLabels.addView(TextView(this).apply { text = "Supplier"; textSize = 11f; setPadding(0,0,40,0) })
        rowLabels.addView(TextView(this).apply { text = "Invoice No."; textSize = 11f; setPadding(0,0,40,0) })
        rowLabels.addView(TextView(this).apply { text = "Date"; textSize = 11f; setPadding(0,0,60,0) })
        rowLabels.addView(TextView(this).apply { text = "Reference / PO No."; textSize = 11f })

        val rowInputs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val sp = Spinner(this)
        val supList = arrayOf("Select Supplier", "Harare Wholesalers", "ZimBake", "ColdChain ZW", "My Supplier")
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, supList)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(a: AdapterView<*>?, v: android.view.View?, p: Int, id: Long) { supplier = supList[p] }
            override fun onNothingSelected(a: AdapterView<*>?) {}
        }
        val eInv = EditText(this).apply { hint = "Enter Invoice No."; textSize = 12f }
        val eDate = EditText(this).apply { hint = "27/09/2026"; textSize = 12f }
        val eRef = EditText(this).apply { hint = "Enter PO No."; textSize = 12f }
        rowInputs.addView(sp); rowInputs.addView(eInv); rowInputs.addView(eDate); rowInputs.addView(eRef)

        val lblScan = TextView(this).apply { text = "Scan or Enter Product Code / Name"; textSize = 12f; setPadding(0,15,0,5) }
        val eSearch = EditText(this).apply { hint = "Search by code or product name..."; setBackgroundColor(0xFFFFFFFF.toInt()) }

        // PRODUCT INPUT ROW - YOUR OWN
        val inputRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0,10,0,0) }
        val eName = EditText(this).apply { hint = "Product Name"; textSize = 11f }
        val eCode = EditText(this).apply { hint = "Code"; textSize = 11f }
        val eCost = EditText(this).apply { hint = "Cost (USD)"; inputType = 8194; textSize = 11f }
        val eSell = EditText(this).apply { hint = "Selling (USD)"; inputType = 8194; textSize = 11f }
        val eQty = EditText(this).apply { hint = "Qty"; inputType = 2; textSize = 11f }
        inputRow.addView(eName); inputRow.addView(eCode); inputRow.addView(eCost); inputRow.addView(eSell); inputRow.addView(eQty)

        val btnAdd = Button(this).apply { text = "+ Add Item"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()); textSize = 12f }

        // TABLE HEADER - SAME LAYOUT, USD
        val tableHeader = TextView(this).apply {
            text = "# | Product Name | Code | Cost (USD) | Selling (USD) | Qty | Total (USD) | Action"
            setBackgroundColor(0xFFE8F0FE.toInt())
            setPadding(8,10,8,10)
            textSize = 10f
        }
        val tvTable = TextView(this).apply {
            text = "No items yet. Add your products above."
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(10,15,10,15)
            textSize = 12f
        }
        val tvGrand = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.RIGHT; setPadding(10,15,10,15); setBackgroundColor(0xFFF1F5F9.toInt()) }
        val tvCount = TextView(this).apply { text = "Total Items: 0 "; textSize = 12f }
        val tvTotal = TextView(this).apply { text = "Grand Total (USD): $0.00"; textSize = 16f; setTextColor(0xFF185ADB.toInt()) }
        tvGrand.addView(tvCount); tvGrand.addView(tvTotal)

        // BOTTOM BUTTONS - SAME LAYOUT
        val bottomRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(10,15,0,0) }
        val btnClear = Button(this).apply { text = "Clear"; setBackgroundColor(0xFF94A3B8.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        val btnDraft = Button(this).apply { text = "Save Draft"; setBackgroundColor(0xFF16A34A.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        val btnPost = Button(this).apply { text = "✓ Post Receiving"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        bottomRow.addView(btnClear); bottomRow.addView(btnDraft); bottomRow.addView(btnPost)

        fun refresh() {
            if (list.isEmpty()) {
                tvTable.text = "No items yet. Add your products above."
                tvCount.text = "Total Items: 0 "
                tvTotal.text = "Grand Total (USD): $0.00"
                return
            }
            var txt = ""; var grand = 0.0; var i = 1
            for (it in list) {
                val tot = it.cost * it.qty
                txt += i.toString() + " | " + it.name + " | " + it.code + " | $" + it.cost + " | $" + it.sell + " | " + it.qty + " | $" + String.format("%.2f", tot) + " | [X]\n"
                grand += tot; i++
            }
            tvTable.text = txt
            tvCount.text = "Total Items: " + list.size + " "
            tvTotal.text = "Grand Total (USD): $" + String.format("%.2f", grand)
        }

        btnAdd.setOnClickListener {
            if (eName.text.isEmpty() || eCode.text.isEmpty() || eCost.text.isEmpty() || eSell.text.isEmpty() || eQty.text.isEmpty()) {
                Toast.makeText(this, "Fill all: Name, Code, Cost USD, Selling USD, Qty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                val c = eCost.text.toString().toDouble(); val s = eSell.text.toString().toDouble()
                if (s < c) { Toast.makeText(this, "Selling $" + s + " cannot be < Cost $" + c, Toast.LENGTH_LONG).show(); return@setOnClickListener }
                list.add(Item(eName.text.toString(), eCode.text.toString().uppercase(), c, s, eQty.text.toString().toInt()))
                refresh()
                eName.text.clear(); eCode.text.clear(); eCost.text.clear(); eSell.text.clear(); eQty.text.clear()
            } catch (e: Exception) { Toast.makeText(this, "Check numbers", Toast.LENGTH_SHORT).show() }
        }
        btnClear.setOnClickListener { list.clear(); refresh() }
        btnPost.setOnClickListener {
            if (list.isEmpty()) return@setOnClickListener
            for (it in list) {
                val ex = stock[it.code]
                if (ex!= null) { ex.qty += it.qty; ex.cost = it.cost; ex.sell = it.sell; ex.name = it.name }
                else stock[it.code] = it
            }
            save()
            Toast.makeText(this, "Posted " + list.size + " items from " + supplier + " to Stock (USD)", Toast.LENGTH_LONG).show()
            list.clear(); refresh()
        }

        formCard.addView(rowLabels); formCard.addView(rowInputs); formCard.addView(lblScan); formCard.addView(eSearch)
        content.addView(topBar); content.addView(titleRec); content.addView(formCard); content.addView(inputRow); content.addView(btnAdd)
        content.addView(tableHeader); content.addView(tvTable); content.addView(tvGrand); content.addView(bottomRow)

        scroll.addView(content)
        root.addView(sidebar); root.addView(scroll)
        setContentView(root)
    }
}
