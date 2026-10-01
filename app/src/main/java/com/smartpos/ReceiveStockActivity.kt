package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.widget.*
import org.json.JSONObject

class ReceiveStockActivity : Activity() {
    data class Line(var name: String, var code: String, var cost: Double, var sell: Double, var qty: Int)
    data class Prod(val name: String, val code: String, val cost: Double, val price: Double)

    private val list = mutableListOf<Line>()
    private lateinit var totalView: TextView
    private lateinit var table: LinearLayout
    private lateinit var spinnerShop: Spinner
    private val shops = mutableListOf<Pair<String,String>>()
    private fun getSelectedShopId(): String = if(spinnerShop.selectedItemPosition in shops.indices) shops[spinnerShop.selectedItemPosition].first else "main"
    private fun getSelectedShopName(): String = if(spinnerShop.selectedItemPosition in shops.indices) shops[spinnerShop.selectedItemPosition].second else "Main Shop"

    private fun label(t: String): TextView = TextView(this).apply {
        text = t; textSize = 11f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor("#1E293B")); setPadding(12, 6, 12, 6)
    }

    private fun getAllProducts(): List<Prod> {
        val out = mutableListOf<Prod>()
        try {
            val db = openOrCreateDatabase("products_db", Context.MODE_PRIVATE, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, code TEXT, cost REAL, price REAL, qty INTEGER)")
            val c = db.rawQuery("SELECT name, code, cost, price FROM products GROUP BY code", null)
            while (c.moveToNext()) {
                val n = c.getString(0); val co = c.getString(1)
                if (n!= null && co!= null && n.isNotEmpty() && co.isNotEmpty()) {
                    out.add(Prod(n, co, c.getDouble(2), c.getDouble(3)))
                }
            }
            c.close(); db.close()
        } catch (e: Exception) {}
        return out.distinctBy { it.code.lowercase() }
    }

    private fun refresh() {
        table.removeAllViews()
        for (l in list) { table.addView(TextView(this).apply { text = l.name + " | " + l.code + " | " + l.cost + " | " + l.sell + " | " + l.qty; setPadding(12,14,12,14) }) }
        var tq = 0; var tc = 0.0; for (l in list) { tq += l.qty; tc += l.cost * l.qty }
        totalView.text = "Total: " + list.size + " UNIQUE | Qty: " + tq + " | Cost: $" + String.format("%.1f", tc) + " | Shop: " + getSelectedShopName()
    }

    private fun printGrn() {
        if (list.isEmpty()) { Toast.makeText(this, "Nothing", Toast.LENGTH_SHORT).show(); return }
        var txt = "GRN - Shop: ${getSelectedShopName()}\n"; for (l in list) { txt += l.name + " | " + l.code + " | " + l.qty + "\n" }
        val i = android.content.Intent(android.content.Intent.ACTION_SEND); i.type = "text/plain"; i.putExtra(android.content.Intent.EXTRA_TEXT, txt)
        startActivity(android.content.Intent.createChooser(i, "PRINT GRN"))
    }

    private fun completeReceive() {
        if (list.isEmpty()) { return }
        try {
            val db = openOrCreateDatabase("products_db", Context.MODE_PRIVATE, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, code TEXT, cost REAL, price REAL, qty INTEGER)")
            try{ db.execSQL("ALTER TABLE products ADD COLUMN shop_id TEXT") }catch(_:Exception){}
            val shopId = getSelectedShopId()
            for (l in list) {
                db.execSQL("INSERT INTO products(name,code,cost,price,qty,shop_id) VALUES('" + l.name.replace("'","") + "','" + l.code + "'," + l.cost + "," + l.sell + "," + l.qty + ",'" + shopId + "')")
                try{
                    val inv = getSharedPreferences("inventory_db", Context.MODE_PRIVATE)
                    val invKey = "${shopId}_${l.code}".lowercase()
                    val old = inv.getString(invKey,"")
                    var newQty = l.qty
                    if(old!=null && old.contains("|")){
                        newQty = (old.split("|").getOrNull(3)?.toIntOrNull()?:0) + l.qty
                    }
                    inv.edit().putString(invKey, "${l.name}|${l.cost}|${l.sell}|$newQty|$shopId").apply()
                    val shopPref = getSharedPreferences("stock_$shopId", Context.MODE_PRIVATE)
                    shopPref.edit().putString(l.code, "${l.name}|${l.cost}|${l.sell}|$newQty|$shopId").apply()
                }catch(_:Exception){}
            }
            db.close()
        } catch (e: Exception) {}
        Toast.makeText(this, "RECEIVE COMPLETE to ${getSelectedShopName()}", Toast.LENGTH_LONG).show(); list.clear(); refresh()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try{
            val prefsShops = getSharedPreferences("shops_db", Context.MODE_PRIVATE)
            prefsShops.all.forEach { (keyId, value) ->
                try{
                    val str = value.toString().trim()
                    if(str.startsWith("{")){
                        val json = JSONObject(str)
                        val id = json.optString("id", keyId)
                        val name = json.optString("name", id)
                        shops.add(Pair(id, "$name ($id)"))
                    } else {
                        shops.add(Pair(keyId, "$keyId"))
                    }
                }catch(_:Exception){ shops.add(Pair(keyId, keyId)) }
            }
        }catch(_:Exception){}
        if(shops.isEmpty()) shops.add(Pair("main","Main Shop"))

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundColor(Color.WHITE) }
        root.addView(TextView(this).apply { text = "RECEIVE STOCK - BUILD 145 NO OVERRIDE"; textSize = 18f; setTypeface(null, Typeface.BOLD); setPadding(0,0,0,10) })

        root.addView(label("SELECT SHOP TO RECEIVE *"))
        spinnerShop = Spinner(this)
        spinnerShop.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shops.map{it.second})
        root.addView(spinnerShop)
        spinnerShop.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: android.view.View?, p2: Int, p3: Long){ refresh() }
            override fun onNothingSelected(p0: AdapterView<*>?){}
        }

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
        val nameList = all.map { it.name }.distinct()
        val codeList = all.map { it.code }.distinct()
        val byName = all.associateBy { it.name.lowercase() }
        val byCode = all.associateBy { it.code.lowercase() }

        val dlg = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,20,24,20) }

        val nameInput = AutoCompleteTextView(this).apply {
            hint = "Search REAL product name"; setPadding(20,14,20,14); threshold = 1
            setAdapter(ArrayAdapter(this@ReceiveStockActivity, android.R.layout.simple_dropdown_item_1line, nameList))
        }
        val codeInput = AutoCompleteTextView(this).apply {
            hint = "Search REAL code"; setPadding(20,14,20,14); threshold = 1
            setAdapter(ArrayAdapter(this@ReceiveStockActivity, android.R.layout.simple_dropdown_item_1line, codeList))
        }
        val costInput = EditText(this).apply { hint = "Cost *"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setPadding(20,14,20,14) }
        val sellInput = EditText(this).apply { hint = "Sell *"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setPadding(20,14,20,14) }
        val qtyInput = EditText(this).apply { hint = "Qty *"; inputType = InputType.TYPE_CLASS_NUMBER; setPadding(20,14,20,14) }
        val err = TextView(this).apply { setTextColor(Color.RED); textSize = 13f; setTypeface(null, Typeface.BOLD) }

        nameInput.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            val sel = nameInput.adapter.getItem(pos).toString()
            val p = byName[sel.lowercase()]
            if (p!= null) { codeInput.setText(p.code); costInput.setText(p.cost.toString()); sellInput.setText(p.price.toString()) }
        }
        codeInput.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            val sel = codeInput.adapter.getItem(pos).toString()
            val p = byCode[sel.lowercase()]
            if (p!= null) { nameInput.setText(p.name); costInput.setText(p.cost.toString()); sellInput.setText(p.price.toString()) }
        }

        dlg.addView(label("Product Name * (REAL)")); dlg.addView(nameInput)
        dlg.addView(label("Code * (REAL - Your saved code)")); dlg.addView(codeInput)
        dlg.addView(label("Cost *")); dlg.addView(costInput)
        dlg.addView(label("Selling *")); dlg.addView(sellInput)
        dlg.addView(label("Quantity *")); dlg.addView(qtyInput)
        dlg.addView(err)

        val d = AlertDialog.Builder(this).setTitle("Add Product - BUILD 145 - BLOCK DUPLICATE CODE").setView(dlg).setPositiveButton("ADD", null).setNegativeButton("CANCEL", null).create()
        d.show()
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val n = nameInput.text.toString().trim(); val c = codeInput.text.toString().trim()
            val coS = costInput.text.toString().trim(); val seS = sellInput.text.toString().trim(); val qS = qtyInput.text.toString().trim()
            if (n.isEmpty() || c.isEmpty() || coS.isEmpty() || seS.isEmpty() || qS.isEmpty()) { err.text = "All fields required"; return@setOnClickListener }

            // === FIX 1: BLOCK IF CODE ALREADY EXISTS IN DB WITH DIFFERENT NAME ===
            val existsInDb = all.find { it.code.equals(c, ignoreCase = true) }
            if (existsInDb != null) {
                if (!existsInDb.name.equals(n, ignoreCase = true)) {
                    err.text = "❌ CODE ALREADY EXISTS: Code '$c' already belongs to '${existsInDb.name}'. Use different code!"
                    Toast.makeText(this@ReceiveStockActivity, "❌ CODE ALREADY EXISTS: $c is for ${existsInDb.name}", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                // If same code + same name, allow - but update qty in current list
            }

            // Check current receive list - merge if same code
            val existing = list.find { it.code.equals(c, true) }
            if (existing!= null) {
                // Only merge if name matches - otherwise block
                if (!existing.name.equals(n, true)) {
                    err.text = "❌ CODE ALREADY EXISTS in this GRN: $c is ${existing.name}"
                    return@setOnClickListener
                }
                existing.qty += qS.toInt()
                existing.cost = coS.toDouble()
                existing.sell = seS.toDouble()
                refresh()
                Toast.makeText(this, "Updated " + n + " qty to " + existing.qty, Toast.LENGTH_SHORT).show()
                d.dismiss()
            } else {
                list.add(Line(n,c,coS.toDouble(),seS.toDouble(),qS.toInt()))
                refresh(); d.dismiss()
            }
        }
    }
}
