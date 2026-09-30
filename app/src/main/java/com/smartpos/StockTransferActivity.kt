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

class StockTransferActivity : Activity() {
    data class Line(var name: String, var code: String, var available: Int, var qty: Int, var cost: Double, var price: Double)
    data class Prod(val name: String, val code: String, val qty: Int, val cost: Double, val price: Double)
    data class Shop(val id: String, val name: String)

    private val list = mutableListOf<Line>()
    private lateinit var totalView: TextView
    private lateinit var table: LinearLayout
    private lateinit var fromSpinner: Spinner
    private lateinit var toSpinner: Spinner
    private var shops: List<Shop> = emptyList()

    private fun label(t: String): TextView = TextView(this).apply {
        text = t; textSize = 11f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor("#1E293B")); setPadding(12, 6, 12, 6)
    }

    private fun getShops(): List<Shop> {
        val out = mutableListOf<Shop>()
        try {
            val db = openOrCreateDatabase("shops_db", Context.MODE_PRIVATE, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS shops (id TEXT PRIMARY KEY, name TEXT)")
            val c = db.rawQuery("SELECT id, name FROM shops", null)
            while (c.moveToNext()) {
                val id = c.getString(0); val name = c.getString(1)
                if (id!= null && name!= null && id.isNotEmpty() && name.isNotEmpty()) { out.add(Shop(id, name)) }
            }
            c.close(); db.close()
        } catch (e: Exception) {}
        return out
    }

    // Get REAL stock for FROM shop
    private fun getProductsForShop(shopId: String): List<Prod> {
        val out = mutableListOf<Prod>()
        try {
            val db = openOrCreateDatabase("products_db", Context.MODE_PRIVATE, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, code TEXT, cost REAL, price REAL, qty INTEGER)")
            db.execSQL("CREATE TABLE IF NOT EXISTS shop_stock (shop_id TEXT, code TEXT, name TEXT, qty INTEGER, cost REAL, price REAL, PRIMARY KEY(shop_id, code))")
            // Try shop_stock first (per shop)
            val c = db.rawQuery("SELECT name, code, qty, cost, price FROM shop_stock WHERE shop_id = '" + shopId.replace("'","") + "' GROUP BY code", null)
            while (c.moveToNext()) { out.add(Prod(c.getString(0), c.getString(1), c.getInt(2), c.getDouble(3), c.getDouble(4))) }
            c.close()
            // If empty and shop is main, fallback to products table (REAL ONLY)
            if (out.isEmpty()) {
                val c2 = db.rawQuery("SELECT name, code, qty, cost, price FROM products GROUP BY code", null)
                while (c2.moveToNext()) {
                    val n = c2.getString(0); val co = c2.getString(1)
                    if (n!= null && co!= null && n.isNotEmpty() && co.isNotEmpty()) { out.add(Prod(n, co, c2.getInt(2), c2.getDouble(3), c2.getDouble(4))) }
                }
                c2.close()
            }
            db.close()
        } catch (e: Exception) {}
        return out.distinctBy { it.code }
    }

    private fun refresh() {
        table.removeAllViews()
        if (list.isEmpty()) {
            table.addView(TextView(this).apply { text = "No products added yet. Click ADD PRODUCT TO TRANSFER"; setPadding(20,20,20,20); setTextColor(Color.GRAY); gravity = Gravity.CENTER })
        }
        for (l in list) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,12,12,12) }
            row.addView(TextView(this).apply { text = l.name + " | " + l.code + " | Avail:" + l.available + " | Qty:" + l.qty; layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
            val delBtn = Button(this).apply { text = "X"; setBackgroundColor(Color.RED); setTextColor(Color.WHITE) }
            delBtn.setOnClickListener { list.remove(l); refresh() }
            row.addView(delBtn)
            table.addView(row)
        }
        var tq = 0; for (l in list) { tq += l.qty }
        totalView.text = "Total: " + list.size + " Items | Total Qty: " + tq
    }

    private fun completeTransfer() {
        if (list.isEmpty()) { Toast.makeText(this, "Add products first", Toast.LENGTH_SHORT).show(); return }
        val fromPos = fromSpinner.selectedItemPosition
        val toPos = toSpinner.selectedItemPosition
        if (fromPos <0 || toPos <0) return
        val fromShop = shops[fromPos]
        val toShop = shops[toPos]
        if (fromShop.id == toShop.id) { Toast.makeText(this, "FROM and TO cannot be same shop!", Toast.LENGTH_LONG).show(); return }

        try {
            val db = openOrCreateDatabase("products_db", Context.MODE_PRIVATE, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS shop_stock (shop_id TEXT, code TEXT, name TEXT, qty INTEGER, cost REAL, price REAL, PRIMARY KEY(shop_id, code))")
            db.execSQL("CREATE TABLE IF NOT EXISTS transfers (id INTEGER PRIMARY KEY AUTOINCREMENT, from_shop TEXT, to_shop TEXT, from_id TEXT, to_id TEXT, code TEXT, name TEXT, qty INTEGER, date TEXT)")

            for (l in list) {
                val date = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date())

                // 1. LOG TRANSFER
                db.execSQL("INSERT INTO transfers(from_shop,to_shop,from_id,to_id,code,name,qty,date) VALUES('" + fromShop.name.replace("'","") + "','" + toShop.name.replace("'","") + "','" + fromShop.id + "','" + toShop.id + "','" + l.code + "','" + l.name.replace("'","") + "'," + l.qty + ",'" + date + "')")

                // 2. DEDUCT FROM FROM SHOP
                // Update shop_stock
                val c = db.rawQuery("SELECT qty FROM shop_stock WHERE shop_id = '" + fromShop.id + "' AND code = '" + l.code + "'", null)
                if (c.moveToFirst()) {
                    db.execSQL("UPDATE shop_stock SET qty = qty - " + l.qty + " WHERE shop_id = '" + fromShop.id + "' AND code = '" + l.code + "'")
                } else {
                    // If not in shop_stock, deduct from main products table
                    db.execSQL("UPDATE products SET qty = qty - " + l.qty + " WHERE code = '" + l.code + "'")
                    // Ensure from shop has entry with reduced qty (for future)
                    db.execSQL("INSERT OR REPLACE INTO shop_stock(shop_id,code,name,qty,cost,price) VALUES('" + fromShop.id + "','" + l.code + "','" + l.name.replace("'","") + "'," + (l.available - l.qty) + "," + l.cost + "," + l.price + ")")
                }
                c.close()

                // 3. ADD TO TO SHOP
                val c2 = db.rawQuery("SELECT qty FROM shop_stock WHERE shop_id = '" + toShop.id + "' AND code = '" + l.code + "'", null)
                if (c2.moveToFirst()) {
                    val existingQty = c2.getInt(0)
                    db.execSQL("UPDATE shop_stock SET qty = qty + " + l.qty + " WHERE shop_id = '" + toShop.id + "' AND code = '" + l.code + "'")
                } else {
                    db.execSQL("INSERT INTO shop_stock(shop_id,code,name,qty,cost,price) VALUES('" + toShop.id + "','" + l.code + "','" + l.name.replace("'","") + "'," + l.qty + "," + l.cost + "," + l.price + ")")
                }
                c2.close()

                // Also ensure products table has entry for TO shop if it's main logic
                // We keep products table as master, but shop_stock is per shop
            }
            db.close()
        } catch (e: Exception) {
            Toast.makeText(this, "Transfer Error: " + e.message, Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "✅ TRANSFER DONE: " + fromShop.name + " -> " + toShop.name + " | " + list.size + " items", Toast.LENGTH_LONG).show()
        list.clear(); refresh()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        shops = getShops()
        if (shops.isEmpty()) {
            val emptyRoot = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32,32,32,32); gravity = Gravity.CENTER }
            emptyRoot.addView(TextView(this).apply { text = "No Shops Found!"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.RED) })
            emptyRoot.addView(TextView(this).apply { text = "Create shops first in Shops Management"; setPadding(0,20,0,20) })
            emptyRoot.addView(Button(this).apply { text = "GO BACK"; setOnClickListener { finish() } })
            setContentView(emptyRoot)
            return
        }
        val shopNames = shops.map { it.name }
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundColor(Color.WHITE) }
        root.addView(TextView(this).apply { text = "STOCK TRANSFER - BUILD 125 FULL FUNCTION"; textSize = 18f; setTypeface(null, Typeface.BOLD); setPadding(0,0,0,20) })

        val shopsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val fromBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(0,0,8,0) } }
        fromBox.addView(label("FROM SHOP * - Deduct")); fromSpinner = Spinner(this)
        fromSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, shopNames).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        fromSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) { list.clear(); refresh(); Toast.makeText(this@StockTransferActivity, "Now showing stock for: " + shops[pos].name, Toast.LENGTH_SHORT).show() }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        fromBox.addView(fromSpinner)

        val toBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(8,0,0,0) } }
        toBox.addView(label("TO SHOP * - Add")); toSpinner = Spinner(this)
        toSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, shopNames).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        toBox.addView(toSpinner)

        shopsRow.addView(fromBox); shopsRow.addView(toBox); root.addView(shopsRow)

        root.addView(TextView(this).apply { text = "Product | Code | Avail | Transfer Qty"; setBackgroundColor(Color.parseColor("#0F766E")); setTextColor(Color.WHITE); setPadding(20,12,20,12) })
        table = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; root.addView(table)
        root.addView(Button(this).apply { text = "ADD PRODUCT TO TRANSFER"; setBackgroundColor(Color.parseColor("#0F766E")); setTextColor(Color.WHITE); setOnClickListener { showAddDialog() } })
        totalView = TextView(this).apply { text = "Total: 0"; setBackgroundColor(Color.parseColor("#CCFBF1")); gravity = Gravity.CENTER; setPadding(20,12,20,12) }; root.addView(totalView)
        val btnRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0,12,0,0) }
        btnRow.addView(Button(this).apply { text = "CLEAR ALL"; layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(0,0,8,0) }; setBackgroundColor(Color.GRAY); setTextColor(Color.WHITE); setOnClickListener { list.clear(); refresh() } })
        btnRow.addView(Button(this).apply { text = "COMPLETE TRANSFER"; layoutParams = LinearLayout.LayoutParams(0,-2,2f); setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); setOnClickListener { completeTransfer() } })
        root.addView(btnRow)
        refresh()
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun showAddDialog() {
        val fromPos = fromSpinner.selectedItemPosition
        if (fromPos <0) return
        val fromShop = shops[fromPos]
        val all = getProductsForShop(fromShop.id)

        if (all.isEmpty()) {
            Toast.makeText(this, "No stock in " + fromShop.name + "!", Toast.LENGTH_LONG).show()
            return
        }

        val nameList = all.map { it.name }.distinct()
        val codeList = all.map { it.code }.distinct()
        val byName = all.associateBy { it.name }
        val byCode = all.associateBy { it.code }

        val dlg = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,20,24,20) }
        val nameInput = AutoCompleteTextView(this).apply { hint = "Search REAL name from " + fromShop.name; setPadding(20,14,20,14); threshold = 1; setAdapter(ArrayAdapter(this@StockTransferActivity, android.R.layout.simple_dropdown_item_1line, nameList)) }
        val codeInput = AutoCompleteTextView(this).apply { hint = "Search REAL code from " + fromShop.name; setPadding(20,14,20,14); threshold = 1; setAdapter(ArrayAdapter(this@StockTransferActivity, android.R.layout.simple_dropdown_item_1line, codeList)) }
        val availView = TextView(this).apply { text = "Available: -"; setPadding(20,8,20,8); setTextColor(Color.parseColor("#0F766E")); setTypeface(null, Typeface.BOLD) }
        val qtyInput = EditText(this).apply { hint = "Qty to Transfer *"; inputType = InputType.TYPE_CLASS_NUMBER; setPadding(20,14,20,14) }
        val err = TextView(this).apply { setTextColor(Color.RED) }
        var currentProd: Prod? = null

        nameInput.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ -> val sel = nameInput.adapter.getItem(pos).toString(); val p = byName[sel]; if (p!= null) { currentProd = p; codeInput.setText(p.code); availView.text = "Available in " + fromShop.name + ": " + p.qty } }
        codeInput.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ -> val sel = codeInput.adapter.getItem(pos).toString(); val p = byCode[sel]; if (p!= null) { currentProd = p; nameInput.setText(p.name); availView.text = "Available in " + fromShop.name + ": " + p.qty } }

        dlg.addView(label("Product Name * (From: " + fromShop.name + ")")); dlg.addView(nameInput)
        dlg.addView(label("Code * (From: " + fromShop.name + ")")); dlg.addView(codeInput)
        dlg.addView(availView); dlg.addView(label("Qty to Transfer *")); dlg.addView(qtyInput); dlg.addView(err)

        val d = AlertDialog.Builder(this).setTitle("Add from " + fromShop.name + " - REAL STOCK").setView(dlg).setPositiveButton("ADD", null).setNegativeButton("CANCEL", null).create(); d.show()
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val n = nameInput.text.toString().trim(); val c = codeInput.text.toString().trim(); val qS = qtyInput.text.toString().trim()
            if (n.isEmpty() || c.isEmpty() || qS.isEmpty()) { err.text = "All required"; return@setOnClickListener }
            val p = currentProd?: byCode[c]?: byName[n]
            if (p== null) { err.text = "Not found in " + fromShop.name; return@setOnClickListener }
            val q = qS.toIntOrNull()?:0
            if (q <=0) { err.text = "Qty >0"; return@setOnClickListener }
            if (q > p.qty) { err.text = "Not enough! Available in " + fromShop.name + ": " + p.qty; return@setOnClickListener }
            val existing = list.find { it.code.equals(c, true) }
            if (existing!= null) { if (existing.qty + q > p.qty) { err.text = "Total exceeds " + p.qty; return@setOnClickListener }; existing.qty += q; Toast.makeText(this, "Updated " + n + " to " + existing.qty, Toast.LENGTH_SHORT).show() }
            else { list.add(Line(n,c,p.qty,q,p.cost,p.price)) }
            refresh(); d.dismiss()
        }
    }
}
