package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.widget.*

class BranchesActivity : Activity() {
    data class Shop(val id: String, val name: String, val location: String)

    private lateinit var table: LinearLayout

    private fun getShops(): List<Shop> {
        val out = mutableListOf<Shop>()
        try {
            val db = openOrCreateDatabase("shops_db", Context.MODE_PRIVATE, null)
            db.execSQL("CREATE TABLE IF NOT EXISTS shops (id TEXT PRIMARY KEY, name TEXT, location TEXT)")
            val c = db.rawQuery("SELECT id, name, location FROM shops", null)
            while (c.moveToNext()) { out.add(Shop(c.getString(0), c.getString(1), c.getString(2)?: "")) }
            c.close(); db.close()
        } catch (e: Exception) {}
        return out
    }

    private fun refresh() {
        table.removeAllViews()
        val shops = getShops()
        if (shops.isEmpty()) {
            table.addView(TextView(this).apply { text = "No branches/shops yet. Click ADD BRANCH."; setPadding(20,20,20,20); setTextColor(Color.GRAY) })
            return
        }
        for (s in shops) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,12,12,12); setBackgroundColor(Color.parseColor("#F8FAFC")) }
            row.addView(TextView(this).apply { text = s.name + "\n" + s.id + " | " + s.location; layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
            val del = Button(this).apply { text = "DELETE"; setBackgroundColor(Color.RED); setTextColor(Color.WHITE) }
            del.setOnClickListener {
                try {
                    val db = openOrCreateDatabase("shops_db", Context.MODE_PRIVATE, null)
                    db.execSQL("DELETE FROM shops WHERE id = '" + s.id + "'")
                    db.close()
                } catch (e: Exception) {}
                refresh()
            }
            row.addView(del)
            table.addView(row)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundColor(Color.WHITE) }
        root.addView(TextView(this).apply { text = "BRANCHES / SHOPS - BUILD 126 REAL ONLY"; textSize = 18f; setTypeface(null, Typeface.BOLD); setPadding(0,0,0,20) })
        table = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(table) })
        root.addView(Button(this).apply {
            text = "ADD BRANCH / SHOP"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)
            setOnClickListener { showAddDialog() }
        })
        root.addView(Button(this).apply { text = "BACK"; setOnClickListener { finish() } })
        refresh()
        setContentView(root)
    }

    private fun showAddDialog() {
        val dlg = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,20,24,20) }
        val idInput = EditText(this).apply { hint = "Shop ID * e.g. main, gweru, harare"; inputType = InputType.TYPE_CLASS_TEXT; setPadding(20,14,20,14) }
        val nameInput = EditText(this).apply { hint = "Shop Name * e.g. Main Shop, Gweru Branch"; setPadding(20,14,20,14) }
        val locInput = EditText(this).apply { hint = "Location e.g. Gweru CBD"; setPadding(20,14,20,14) }
        val err = TextView(this).apply { setTextColor(Color.RED) }
        dlg.addView(TextView(this).apply { text = "Shop ID * (no spaces)"; setTypeface(null, Typeface.BOLD) }); dlg.addView(idInput)
        dlg.addView(TextView(this).apply { text = "Shop Name *"; setTypeface(null, Typeface.BOLD) }); dlg.addView(nameInput)
        dlg.addView(TextView(this).apply { text = "Location"; setTypeface(null, Typeface.BOLD) }); dlg.addView(locInput)
        dlg.addView(err)
        val d = AlertDialog.Builder(this).setTitle("Add Branch/Shop - REAL").setView(dlg).setPositiveButton("SAVE", null).setNegativeButton("CANCEL", null).create(); d.show()
        d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val id = idInput.text.toString().trim().lowercase().replace(" ","_")
            val name = nameInput.text.toString().trim()
            val loc = locInput.text.toString().trim()
            if (id.isEmpty() || name.isEmpty()) { err.text = "ID and Name required"; return@setOnClickListener }
            try {
                val db = openOrCreateDatabase("shops_db", Context.MODE_PRIVATE, null)
                db.execSQL("CREATE TABLE IF NOT EXISTS shops (id TEXT PRIMARY KEY, name TEXT, location TEXT)")
                db.execSQL("INSERT OR REPLACE INTO shops(id,name,location) VALUES('" + id.replace("'","") + "','" + name.replace("'","") + "','" + loc.replace("'","") + "')")
                // Also create shop_stock table for this shop
                db.execSQL("CREATE TABLE IF NOT EXISTS shop_stock (shop_id TEXT, code TEXT, name TEXT, qty INTEGER, cost REAL, price REAL, PRIMARY KEY(shop_id, code))")
                db.close()
                // If it's main shop, copy existing products into shop_stock main
                if (id == "main") {
                    val pdb = openOrCreateDatabase("products_db", Context.MODE_PRIVATE, null)
                    pdb.execSQL("CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, code TEXT, cost REAL, price REAL, qty INTEGER)")
                    pdb.execSQL("CREATE TABLE IF NOT EXISTS shop_stock (shop_id TEXT, code TEXT, name TEXT, qty INTEGER, cost REAL, price REAL, PRIMARY KEY(shop_id, code))")
                    val c = pdb.rawQuery("SELECT name, code, qty, cost, price FROM products GROUP BY code", null)
                    while (c.moveToNext()) {
                        pdb.execSQL("INSERT OR IGNORE INTO shop_stock(shop_id,code,name,qty,cost,price) VALUES('main','" + c.getString(1).replace("'","") + "','" + c.getString(0).replace("'","") + "'," + c.getInt(2) + "," + c.getDouble(3) + "," + c.getDouble(4) + ")")
                    }
                    c.close(); pdb.close()
                }
            } catch (e: Exception) { err.text = e.message; return@setOnClickListener }
            Toast.makeText(this, "Shop Saved: " + name, Toast.LENGTH_SHORT).show()
            refresh(); d.dismiss()
        }
    }
}
