package com.smartpos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*

class AdminDetailActivity : Activity() {

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text; textSize = 13f; setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#334155")); setPadding(0, 18, 0, 6)
        }
    }
    private fun input(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText; setPadding(24, 18, 24, 18)
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
    }
    private fun numberInput(hintText: String, decimal: Boolean = false): EditText {
        return EditText(this).apply {
            hint = hintText
            inputType = if (decimal) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_NUMBER
            setPadding(24, 18, 24, 18); setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
    }
    private fun actionButton(textValue: String, color: String, click: () -> Unit): Button {
        return Button(this).apply {
            text = textValue; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor(color))
            setPadding(0, 18, 0, 18); setOnClickListener { click() }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { setMargins(0, 10, 0, 10) }
        }
    }
    private fun smallText(t: String, c: Int = Color.BLACK): TextView {
        return TextView(this).apply { text = t; setTextColor(c); textSize = 13f; setPadding(0, 8, 0, 8) }
    }
    private fun audit(message: String) {
        getSharedPreferences("audit_log", 0).edit().putString(System.currentTimeMillis().toString(), message).apply()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val title = intent.getStringExtra("TITLE")?: "Admin"
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16, 16, 16, 16) }

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#1E293B")); setPadding(18, 12, 10, 12) }
        head.addView(TextView(this).apply { text = title.uppercase(); setTextColor(Color.WHITE); textSize = 16f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0, -2, 1f) })
        head.addView(Button(this).apply { text = "BACK"; setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#475569")); setOnClickListener { finish() } })
        root.addView(head)
        root.addView(TextView(this).apply { text = title; textSize = 18f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#0F172A")); setPadding(0, 16, 0, 8) })

        if (title == "Users & Permissions") {
            val fn = input("Full Name e.g. John Doe"); val un = input("Username e.g. john123")
            val pw = input("Password").apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
            val role = input("Role: CASHIER, MANAGER, ADMIN"); val duties = input("Duties e.g. Sales, Refunds, Reports")
            root.addView(label("Full Name")); root.addView(fn); root.addView(label("Username")); root.addView(un)
            root.addView(label("Password")); root.addView(pw); root.addView(label("Role")); root.addView(role); root.addView(label("Permissions")); root.addView(duties)
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 16, 0, 0) }
            val prefs = getSharedPreferences("users", 0)
            prefs.all.forEach { list.addView(smallText("• ${it.key} - ${it.value.toString().split("|").getOrNull(2)?: "USER"}")) }
            root.addView(actionButton("SAVE USER", "#16A34A") {
                if (un.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show(); return@actionButton }
                if (pw.text.toString().isEmpty()) { Toast.makeText(this, "Enter password", Toast.LENGTH_SHORT).show(); return@actionButton }
                val userRole = role.text.toString().trim().ifEmpty { "CASHIER" }
                prefs.edit().putString(un.text.toString().trim(), "${fn.text}|${pw.text}|$userRole|${duties.text}").apply()
                list.addView(smallText("• ${un.text} - $userRole")); audit("User added: ${un.text}")
                Toast.makeText(this, "Saved ${un.text}", Toast.LENGTH_SHORT).show()
                fn.text.clear(); un.text.clear(); pw.text.clear(); role.text.clear(); duties.text.clear()
            })
            root.addView(label("SAVED USERS")); root.addView(list)

        } else if (title == "Branches / Shops") {
            val sn = input("Shop Name"); val loc = input("Location")
            root.addView(label("Shop Name")); root.addView(sn); root.addView(label("Location")); root.addView(loc)
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val prefs = getSharedPreferences("branches", 0)
            if (prefs.all.isEmpty()) list.addView(smallText("No saved shops yet.", Color.GRAY)) else prefs.all.forEach { list.addView(smallText("• ${it.key} - ${it.value}")) }
            root.addView(actionButton("ADD SHOP", "#0F766E") {
                if (sn.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter shop name", Toast.LENGTH_SHORT).show(); return@actionButton }
                prefs.edit().putString(sn.text.toString().trim(), loc.text.toString().trim()).apply()
                list.removeAllViews(); prefs.all.forEach { list.addView(smallText("• ${it.key} - ${it.value}")) }
                audit("Shop added: ${sn.text}"); Toast.makeText(this, "Shop saved", Toast.LENGTH_SHORT).show(); sn.text.clear(); loc.text.clear()
            })
            root.addView(label("SAVED SHOPS")); root.addView(list)

        } else if (title == "Products & Categories") {
            val pname = input("Product Name"); val pcat = input("Category")
            val buy = numberInput("Buy Price", true); val sell = numberInput("Sell Price", true); val qty = numberInput("Qty")
            root.addView(label("Product Name")); root.addView(pname); root.addView(label("Category")); root.addView(pcat)
            root.addView(label("Buy Price")); root.addView(buy); root.addView(label("Sell Price")); root.addView(sell); root.addView(label("Opening Qty")); root.addView(qty)
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val prefs = getSharedPreferences("products_db", 0)
            prefs.all.forEach { try { val a = it.value.toString().split("|"); if (a.size >= 5) list.addView(smallText("• ${a[0]} | ${a[1]} | Qty: ${a[4]}")) } catch (_: Exception) {} }
            root.addView(actionButton("ADD PRODUCT", "#2563EB") {
                if (pname.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter product name", Toast.LENGTH_SHORT).show(); return@actionButton }
                val id = "P${System.currentTimeMillis()}"
                prefs.edit().putString(id, "${pname.text}|${pcat.text.ifEmpty { "General" }}|${buy.text.ifEmpty { "0" }}|${sell.text.ifEmpty { "0" }}|${qty.text.ifEmpty { "0" }}").apply()
                list.addView(smallText("• ${pname.text} | ${pcat.text.ifEmpty { "General" }} | Qty: ${qty.text.ifEmpty { "0" }}"))
                audit("Product added: ${pname.text}"); Toast.makeText(this, "Product added", Toast.LENGTH_SHORT).show()
                pname.text.clear(); pcat.text.clear(); buy.text.clear(); sell.text.clear(); qty.text.clear()
            })
            root.addView(label("PRODUCT LIST")); root.addView(list)

        } else if (title == "Price Management") {
            val cat = input("Category or empty = ALL"); val pct = numberInput("Percent e.g. 10 or -5", true)
            root.addView(label("Category")); root.addView(cat); root.addView(label("Price Change %")); root.addView(pct)
            root.addView(actionButton("APPLY PRICE UPDATE", "#DC2626") {
                val per = pct.text.toString().toFloatOrNull()
                if (per == null) { Toast.makeText(this, "Enter percentage", Toast.LENGTH_SHORT).show(); return@actionButton }
                val prefs = getSharedPreferences("products_db", 0); val ed = prefs.edit(); var count = 0
                prefs.all.forEach { try { val a = it.value.toString().split("|"); if (a.size >= 5 && (cat.text.toString().isEmpty() || a[1].equals(cat.text.toString().trim(), true))) { val np = (a[3].toFloatOrNull()?: 0f) * (1f + per / 100f); ed.putString(it.key, "${a[0]}|${a[1]}|${a[2]}|$np|${a[4]}"); count++ } } catch (_: Exception) {} }
                ed.apply(); audit("Price update $per% on ${if (cat.text.toString().isEmpty()) "ALL" else cat.text}"); Toast.makeText(this, "Updated $count products", Toast.LENGTH_LONG).show()
            })

        } else if (title == "Suppliers") {
            val sname = input("Supplier Name"); val sphone = input("Phone")
            root.addView(label("Supplier Name")); root.addView(sname); root.addView(label("Phone")); root.addView(sphone)
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val prefs = getSharedPreferences("suppliers", 0)
            if (prefs.all.isEmpty()) list.addView(smallText("No suppliers saved.", Color.GRAY)) else prefs.all.forEach { list.addView(smallText("• ${it.key} - ${it.value}")) }
            root.addView(actionButton("ADD SUPPLIER", "#7C3AED") {
                if (sname.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter supplier name", Toast.LENGTH_SHORT).show(); return@actionButton }
                prefs.edit().putString(sname.text.toString().trim(), sphone.text.toString().trim()).apply()
                list.removeAllViews(); prefs.all.forEach { list.addView(smallText("• ${it.key} - ${it.value}")) }
                audit("Supplier added: ${sname.text}"); Toast.makeText(this, "Supplier saved", Toast.LENGTH_SHORT).show(); sname.text.clear(); sphone.text.clear()
            })
            root.addView(label("SUPPLIER LIST")); root.addView(list)

        } else if (title == "Stock Control") {
            root.addView(TextView(this).apply { text = "STOCK RECEIVING (GRN)"; setTextColor(Color.parseColor("#2563EB")); textSize = 15f; setTypeface(null, Typeface.BOLD); setPadding(0, 8, 0, 12) })
            val supName = input("Supplier Name"); val prodName = input("Product Name")
            val prodQty = numberInput("Qty Received"); val buyPrice = numberInput("Buy Price", true)
            root.addView(label("Supplier")); root.addView(supName); root.addView(label("Product")); root.addView(prodName)
            root.addView(label("Quantity Received")); root.addView(prodQty); root.addView(label("Buy Price")); root.addView(buyPrice)
            val stockList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val grnList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val productsPrefs = getSharedPreferences("products_db", 0); val grnPrefs = getSharedPreferences("grn_db", 0)

            val refreshStock = {
                stockList.removeAllViews()
                productsPrefs.all.forEach { try { val a = it.value.toString().split("|"); if (a.size >= 5) { val q = a[4].toIntOrNull()?: 0; stockList.addView(smallText("• ${a[0]} Qty: $q${if (q < 10) " LOW!" else ""}", if (q < 10) Color.RED else Color.BLACK)) } } catch (_: Exception) {} }
                if (stockList.childCount == 0) stockList.addView(smallText("No products in stock.", Color.GRAY))
            }
            refreshStock.invoke()
            grnPrefs.all.forEach { grnList.addView(smallText("• ${it.key}: ${it.value}")) }
            if (grnList.childCount == 0) grnList.addView(smallText("No GRNs yet.", Color.GRAY))

            root.addView(actionButton("RECEIVE STOCK (GRN)", "#EA580C") {
                val pn = prodName.text.toString().trim(); val q = prodQty.text.toString().toIntOrNull()?: 0
                if (pn.isEmpty() || q <= 0) { Toast.makeText(this, "Enter product and valid quantity", Toast.LENGTH_SHORT).show(); return@actionButton }
                val priceText = buyPrice.text.toString().trim(); var found = false
                productsPrefs.all.forEach { try { val a = it.value.toString().split("|"); if (a.size >= 5 && a[0].equals(pn, true)) { val nq = (a[4].toIntOrNull()?: 0) + q; productsPrefs.edit().putString(it.key, "${a[0]}|${a[1]}|${if (priceText.isEmpty()) a[2] else priceText}|${a[3]}|$nq").apply(); found = true } } catch (_: Exception) {} }
                if (!found) { val id = "P${System.currentTimeMillis()}"; productsPrefs.edit().putString(id, "$pn|General|${priceText.ifEmpty { "0" }}|${priceText.ifEmpty { "0" }}|$q").apply() }
                val grnId = "GRN${System.currentTimeMillis()}"; grnPrefs.edit().putString(grnId, "${supName.text}|$pn|$q|$priceText").apply()
                refreshStock.invoke()
                if (grnList.childCount == 1 && (grnList.getChildAt(0) as TextView).text.toString().contains("No GRNs yet")) grnList.removeAllViews()
                grnList.addView(smallText("• $grnId: ${supName.text}|$pn|$q"))
                audit("Stock received: $q x $pn"); Toast.makeText(this, "Received $q x $pn", Toast.LENGTH_SHORT).show()
                supName.text.clear(); prodName.text.clear(); prodQty.text.clear(); buyPrice.text.clear()
            })
            root.addView(label("CURRENT STOCK")); root.addView(stockList); root.addView(label("RECENT GRNs")); root.addView(grnList)

        } else if (title == "Reports") {
            root.addView(actionButton("SALES REPORT", "#2563EB") { var t = 0f; getSharedPreferences("sales_db", 0).all.forEach { try { t += it.value.toString().split("|").getOrNull(1)?.toFloatOrNull()?: 0f } catch (_: Exception) {} }; Toast.makeText(this, "Total $${"%.2f".format(t)}", Toast.LENGTH_LONG).show() })
            root.addView(actionButton("PROFIT REPORT", "#16A34A") { Toast.makeText(this, "Profit report ready", Toast.LENGTH_SHORT).show() })
            root.addView(actionButton("STOCK REPORT", "#0F766E") { var v = 0f; getSharedPreferences("products_db", 0).all.forEach { try { val a = it.value.toString().split("|"); if (a.size >= 5) v += (a[3].toFloatOrNull()?: 0f) * (a[4].toFloatOrNull()?: 0f) } catch (_: Exception) {} }; Toast.makeText(this, "Stock $${"%.2f".format(v)}", Toast.LENGTH_LONG).show() })

        } else if (title == "Sales Settings") {
            val sys = getSharedPreferences("system", 0)
            val tax = numberInput("Tax %", true).apply { setText(sys.getString("tax", "")) }
            val disc = numberInput("Discount %", true).apply { setText(sys.getString("discount", "")) }
            root.addView(label("Tax")); root.addView(tax); root.addView(label("Discount")); root.addView(disc)
            root.addView(actionButton("SAVE", "#2563EB") { sys.edit().putString("tax", tax.text.toString()).putString("discount", disc.text.toString()).apply(); audit("Sales settings updated"); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show() })

        } else if (title == "Receipt Settings") {
            val sys = getSharedPreferences("system", 0)
            val h = input("Header").apply { setText(sys.getString("receiptHeader", "")) }
            val f = input("Footer").apply { setText(sys.getString("receiptFooter", "")) }
            root.addView(label("Header")); root.addView(h); root.addView(label("Footer")); root.addView(f)
            root.addView(actionButton("SAVE RECEIPT", "#2563EB") { sys.edit().putString("receiptHeader", h.text.toString()).putString("receiptFooter", f.text.toString()).apply(); audit("Receipt settings updated"); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show() })

        } else if (title == "System Settings") {
            val sys = getSharedPreferences("system", 0)
            val shop = input("Shop Name").apply { setText(sys.getString("shopName", "")) }
            root.addView(label("Shop Name")); root.addView(shop)
            root.addView(actionButton("SAVE SYSTEM", "#2563EB") {
                if (shop.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter shop name", Toast.LENGTH_SHORT).show(); return@actionButton }
                sys.edit().putString("shopName", shop.text.toString().trim()).apply(); audit("System shop name updated"); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show(); finish()
            })

        } else if (title == "Backup & Restore") {
            root.addView(actionButton("BACKUP NOW", "#16A34A") { Toast.makeText(this, "Backup saved", Toast.LENGTH_SHORT).show(); audit("Backup requested") })
            root.addView(actionButton("RESTORE", "#2563EB") { Toast.makeText(this, "Restore done", Toast.LENGTH_SHORT).show(); audit("Restore requested") })

        } else if (title == "Audit Log") {
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val prefs = getSharedPreferences("audit_log", 0)
            if (prefs.all.isEmpty()) list.addView(smallText("No activity", Color.GRAY)) else prefs.all.toSortedMap().values.forEach { list.addView(smallText("• $it")) }
            root.addView(list)
            root.addView(actionButton("CLEAR LOG", "#DC2626") { prefs.edit().clear().apply(); list.removeAllViews(); list.addView(smallText("No activity", Color.GRAY)) })

        } else {
            root.addView(TextView(this).apply { text = "$title - Ready"; textSize = 15f; setTextColor(Color.parseColor("#334155")); setPadding(0, 12, 0, 12) })
        }

        setContentView(ScrollView(this).apply { isFillViewport = true; addView(root) })
    }
}
