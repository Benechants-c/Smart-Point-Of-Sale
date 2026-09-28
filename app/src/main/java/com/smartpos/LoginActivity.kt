package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import java.text.SimpleDateFormat
import java.util.*

class LoginActivity : Activity() {
    lateinit var pref: SharedPreferences
    var stock = mutableMapOf<String, Item>()
    var list = mutableListOf<Item>()
    var supplier = "Select Supplier"
    var isAutoFilling = false

    data class Item(var name: String, var code: String, var cost: Double, var sell: Double, var qty: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("smartshop_v7", 0)
        load()
        login()
    }

    fun load() {
        val s = pref.getString("stock", "")?: ""
        if (s.isNotEmpty()) for (row in s.split(";;")) {
            val p = row.split("|")
            if (p.size == 5) stock[p[1]] = Item(p[0], p[1], p[2].toDoubleOrNull()?:0.0, p[3].toDoubleOrNull()?:0.0, p[4].toIntOrNull()?:0)
        }
        if (stock.isEmpty()) {
            stock["BRD001"] = Item("White Bread", "BRD001", 1.2, 2.0, 20)
            stock["OIL001"] = Item("Cooking Oil 1L", "OIL001", 3.0, 4.5, 15)
            save()
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
        val t = TextView(this).apply { text = "SmartShop POS\nSHOP001-HRE"; textSize = 22f; setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER }
        val pin = EditText(this).apply { hint = "PIN"; inputType = 129; setBackgroundColor(0xFFFFFFFF.toInt()) }
        val btn = Button(this).apply { text = "LOGIN"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        btn.setOnClickListener { if (pin.text.toString()=="1234") receivingUI() else Toast.makeText(this,"1234",Toast.LENGTH_SHORT).show() }
        lay.addView(t); lay.addView(pin); lay.addView(btn)
        setContentView(lay)
    }

    fun receivingUI() {
        val scroll = ScrollView(this)
        val main = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12,12,12,12); setBackgroundColor(0xFFF1F5F9.toInt()) }

        val top = TextView(this).apply { text = " SmartShop POS - Receiving (USD)"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()); setPadding(15,15,15,15); textSize = 14f }
        val title = TextView(this).apply { text = "Receiving"; textSize = 20f; setPadding(10,15,10,10) }

        // AUTO DATE FROM SYSTEM
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(12,12,12,12) }

        val hSup = TextView(this).apply { text = "Supplier"; textSize = 12f; setTextColor(0xFF475569.toInt()) }
        val sp = Spinner(this)
        val sups = arrayOf("Select Supplier", "Harare Wholesalers", "ZimBake", "ColdChain ZW", "My Supplier")
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, sups)
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(a: AdapterView<*>?, v: android.view.View?, p: Int, id: Long) { supplier = sups[p] }
            override fun onNothingSelected(a: AdapterView<*>?) {}
        }

        // DATE AUTO - READ ONLY
        val hDate = TextView(this).apply { text = "Date"; textSize = 12f; setPadding(0,12,0,2); setTextColor(0xFF475569.toInt()) }
        val tvDate = TextView(this).apply {
            text = currentDate
            textSize = 15f
            setPadding(10,10,10,10)
            setBackgroundColor(0xFFF1F5F9.toInt())
            setTextColor(0xFF0F172A.toInt())
        }

        card.addView(hSup); card.addView(sp)
        card.addView(hDate); card.addView(tvDate)

        val prodCard = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(12,12,12,12); setPadding(0,15,0,0) }

        val hSearch = TextView(this).apply { text = "Scan or Enter Product Code / Name"; textSize = 12f; setPadding(0,5,0,5) }
        val eSearch = EditText(this).apply { hint = "Search..." }

        val lName = TextView(this).apply { text = "Product Name"; textSize = 12f; setPadding(0,12,0,2) }
        val eName = EditText(this).apply { hint = "Product Name" }

        val lCode = TextView(this).apply { text = "Code"; textSize = 12f; setPadding(0,10,0,2) }
        val eCode = EditText(this).apply { hint = "Code" }

        val lCost = TextView(this).apply { text = "Cost (USD)"; textSize = 12f; setPadding(0,10,0,2) }
        val eCost = EditText(this).apply { hint = "Cost (USD)"; inputType = 8194 }

        val lSell = TextView(this).apply { text = "Selling Price (USD)"; textSize = 12f; setPadding(0,10,0,2) }
        val eSell = EditText(this).apply { hint = "Selling Price (USD)"; inputType = 8194 }

        val lQty = TextView(this).apply { text = "Qty"; textSize = 12f; setPadding(0,10,0,2) }
        val eQty = EditText(this).apply { hint = "Qty"; inputType = 2 }

        eCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isAutoFilling) return
                val code = s.toString().trim().uppercase()
                if (stock.containsKey(code)) {
                    isAutoFilling = true
                    val item = stock[code]!!
                    eName.setText(item.name)
                    eCost.setText(item.cost.toString())
                    eSell.setText(item.sell.toString())
                    isAutoFilling = false
                    Toast.makeText(this@LoginActivity, "Found: " + item.name, Toast.LENGTH_SHORT).show()
                }
            }
        })

        eName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isAutoFilling) return
                val name = s.toString().trim()
                if (name.length < 2) return
                for (it in stock.values) {
                    if (it.name.lowercase().contains(name.lowercase())) {
                        isAutoFilling = true
                        eCode.setText(it.code)
                        eCost.setText(it.cost.toString())
                        eSell.setText(it.sell.toString())
                        isAutoFilling = false
                        break
                    }
                }
            }
        })

        val btnAdd = Button(this).apply { text = "+ Add Item"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }

        prodCard.addView(hSearch); prodCard.addView(eSearch)
        prodCard.addView(lName); prodCard.addView(eName)
        prodCard.addView(lCode); prodCard.addView(eCode)
        prodCard.addView(lCost); prodCard.addView(eCost)
        prodCard.addView(lSell); prodCard.addView(eSell)
        prodCard.addView(lQty); prodCard.addView(eQty)
        prodCard.addView(btnAdd)

        val th = TextView(this).apply { text = "# | Product Name | Code | Cost USD | Sell USD | Qty | Total USD"; setBackgroundColor(0xFFE8F0FE.toInt()); setPadding(8,12,8,12); textSize = 11f }
        val tvTable = TextView(this).apply { text = "No items yet"; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(10,15,10,15) }
        val tvGrand = TextView(this).apply { text = "Total Items: 0 | Grand Total (USD): $0.00"; gravity = Gravity.RIGHT; setPadding(10,15,10,15); textSize = 15f; setTextColor(0xFF185ADB.toInt()) }

        val bottom = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0,15,0,0) }
        val bClear = Button(this).apply { text = "Clear"; setBackgroundColor(0xFF94A3B8.toInt()) }
        val bDraft = Button(this).apply { text = "Save Draft"; setBackgroundColor(0xFF16A34A.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        val bPost = Button(this).apply { text = "Post Receiving"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        bottom.addView(bClear); bottom.addView(bDraft); bottom.addView(bPost)

        fun refresh() {
            if (list.isEmpty()) {
                tvTable.text = "No items yet"
                tvGrand.text = "Total Items: 0 | Grand Total (USD): $0.00"
                return
            }
            var txt = ""; var grand = 0.0; var i = 1
            for (it in list) {
                val tot = it.cost * it.qty
                txt += i.toString() + " " + it.name + " " + it.code + " $" + it.cost + " $" + it.sell + " x" + it.qty + " = $" + String.format("%.2f", tot) + "\n"
                grand += tot; i++
            }
            tvTable.text = txt
            tvGrand.text = "Total Items: " + list.size + " | Grand Total (USD): $" + String.format("%.2f", grand)
        }

        btnAdd.setOnClickListener {
            if (eName.text.isEmpty() || eCode.text.isEmpty() || eCost.text.isEmpty() || eSell.text.isEmpty() || eQty.text.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                val c = eCost.text.toString().toDouble(); val s = eSell.text.toString().toDouble()
                if (s < c) { Toast.makeText(this, "Selling < Cost", Toast.LENGTH_LONG).show(); return@setOnClickListener }
                list.add(Item(eName.text.toString(), eCode.text.toString().uppercase(), c, s, eQty.text.toString().toInt()))
                refresh()
                eName.text.clear(); eCode.text.clear(); eCost.text.clear(); eSell.text.clear(); eQty.text.clear(); eSearch.text.clear()
            } catch (e: Exception) { Toast.makeText(this, "Check numbers", Toast.LENGTH_SHORT).show() }
        }
        bClear.setOnClickListener { list.clear(); refresh() }
        bPost.setOnClickListener {
            if (list.isEmpty()) return@setOnClickListener
            for (it in list) {
                val ex = stock[it.code]
                if (ex!= null) { ex.qty += it.qty; ex.cost = it.cost; ex.sell = it.sell; ex.name = it.name }
                else stock[it.code] = it
            }
            save()
            Toast.makeText(this, "Posted " + list.size + " items on " + currentDate + " from " + supplier, Toast.LENGTH_LONG).show()
            list.clear(); refresh()
        }

        main.addView(top); main.addView(title); main.addView(card); main.addView(prodCard); main.addView(th); main.addView(tvTable); main.addView(tvGrand); main.addView(bottom)
        scroll.addView(main)
        setContentView(scroll)
    }
}
