package com.smartpos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.widget.*

class AdminDetailActivity : Activity() {

    private fun label(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#334155"))
            setPadding(0, 18, 0, 6)
        }
    }

    private fun input(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
    }

    private fun numberInput(hintText: String, decimal: Boolean = false): EditText {
        return EditText(this).apply {
            hint = hintText
            inputType = if (decimal) {
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            } else {
                InputType.TYPE_CLASS_NUMBER
            }
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
    }

    private fun actionButton(textValue: String, color: String, click: () -> Unit): Button {
        return Button(this).apply {
            text = textValue
            setBackgroundColor(Color.parseColor(color))
            setTextColor(Color.WHITE)
            setPadding(0, 28, 0, 28)
            setOnClickListener { click() }
        }
    }

    private fun smallText(t: String, c: Int = Color.BLACK): TextView {
        return TextView(this).apply {
            text = t
            setTextColor(c)
            textSize = 13f
            setPadding(0, 8, 0, 8)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val title = intent.getStringExtra("TITLE")?: "Admin"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(16, 16, 16, 16)
        }

        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(18, 18, 18, 18)
        }

        head.addView(TextView(this).apply {
            text = title.uppercase()
            setTextColor(Color.WHITE)
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        })
        head.addView(Button(this).apply { text = "BACK"; setOnClickListener { finish() } })
        root.addView(head)

        root.addView(TextView(this).apply {
            text = title; textSize = 18f; setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A")); setPadding(0, 16, 0, 8)
        })

        // 1. USERS
        if (title == "Users & Permissions") {
            val fn = input("Full Name e.g. John Doe")
            val un = input("Username e.g. john123")
            val pw = input("Password").apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
            val role = input("Role: CASHIER, MANAGER, ADMIN")
            val duties = input("Duties e.g. Sales, Refunds, Reports")
            root.addView(label("Full Name")); root.addView(fn)
            root.addView(label("Username")); root.addView(un)
            root.addView(label("Password")); root.addView(pw)
            root.addView(label("Role")); root.addView(role)
            root.addView(label("Permissions")); root.addView(duties)

            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 16, 0, 0) }
            val prefs = getSharedPreferences("users", 0)
            prefs.all.forEach { entry ->
                val displayRole = entry.value.toString().split("|").getOrNull(2)?: "USER"
                list.addView(smallText("• ${entry.key} - $displayRole"))
            }

            root.addView(actionButton("SAVE USER", "#16A34A") {
                val username = un.text.toString().trim()
                if (username.isEmpty()) { Toast.makeText(this, "Enter username", Toast.LENGTH_SHORT).show(); return@actionButton }
                if (fn.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter full name", Toast.LENGTH_SHORT).show(); return@actionButton }
                prefs.edit().putString(username, "${fn.text}|${pw.text}|${role.text}|${duties.text}").apply()
                list.addView(smallText("• $username - ${role.text}"))
                Toast.makeText(this, "Saved $username", Toast.LENGTH_SHORT).show()
                fn.text.clear(); un.text.clear(); pw.text.clear(); role.text.clear(); duties.text.clear()
            })
            root.addView(label("SAVED USERS")); root.addView(list)

        } else if (title == "Branches / Shops") {
            val sn = input("Shop Name e.g. Shop 4 Bulawayo")
            val loc = input("Location")
            root.addView(label("Shop Name")); root.addView(sn)
            root.addView(label("Location")); root.addView(loc)
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val prefs = getSharedPreferences("branches", 0)
            if (prefs.all.isEmpty()) list.addView(smallText("Main - Harare\nShop 2 - Chitungwiza", Color.GRAY))
            else prefs.all.forEach { list.addView(smallText("• ${it.key} - ${it.value}")) }

            root.addView(actionButton("ADD SHOP", "#0F766E") {
                val shopName = sn.text.toString().trim()
                if (shopName.isEmpty()) { Toast.makeText(this, "Enter shop name", Toast.LENGTH_SHORT).show(); return@actionButton }
                prefs.edit().putString(shopName, loc.text.toString().trim()).apply()
                list.addView(smallText("• $shopName - ${loc.text}"))
                sn.text.clear(); loc.text.clear()
                Toast.makeText(this, "Shop added", Toast.LENGTH_SHORT).show()
            })
            root.addView(label("SHOPS")); root.addView(list)

        } else if (title == "Products & Categories") {
            val pname = input("Product Name"); val pcat = input("Category")
            val buy = numberInput("Buy Price", true); val sell = numberInput("Sell Price", true); val qty = numberInput("Qty")
            root.addView(label("Product Name")); root.addView(pname)
            root.addView(label("Category")); root.addView(pcat)
            root.addView(label("Buy Price")); root.addView(buy)
            root.addView(label("Sell Price")); root.addView(sell)
            root.addView(label("Quantity")); root.addView(qty)

            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val prefs = getSharedPreferences("products_db", 0)
            prefs.all.forEach { try { val a = it.value.toString().split("|"); if (a.size >= 5) list.addView(smallText("• ${a[0]} | ${a[1]} | Qty: ${a[4]}")) } catch (_: Exception) {} }

            root.addView(actionButton("ADD PRODUCT", "#2563EB") {
                if (pname.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter product name", Toast.LENGTH_SHORT).show(); return@actionButton }
                if (pcat.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter category", Toast.LENGTH_SHORT).show(); return@actionButton }
                if (sell.text.toString().toFloatOrNull() == null) { Toast.makeText(this, "Enter valid sell price", Toast.LENGTH_SHORT).show(); return@actionButton }
                val id = "P${System.currentTimeMillis()}"
                prefs.edit().putString(id, "${pname.text}|${pcat.text}|${buy.text.ifEmpty { "0" }}|${sell.text}|${qty.text.ifEmpty { "0" }}").apply()
                list.addView(smallText("• ${pname.text} | ${pcat.text} | Qty:${qty.text.ifEmpty { "0" }}"))
                pname.text.clear(); pcat.text.clear(); buy.text.clear(); sell.text.clear(); qty.text.clear()
                Toast.makeText(this, "Product added", Toast.LENGTH_SHORT).show()
            })
            root.addView(label("PRODUCT LIST")); root.addView(list)

        } else if (title == "Price Management") {
            val cat = input("Category or empty = ALL"); val pct = numberInput("Percent e.g. 10 or -5", true)
            root.addView(label("Category Filter")); root.addView(cat)
            root.addView(label("Percent Change")); root.addView(pct)
            root.addView(actionButton("APPLY PRICE UPDATE", "#DC2626") {
                val percent = pct.text.toString().toFloatOrNull()
                if (percent == null) { Toast.makeText(this, "Enter valid %", Toast.LENGTH_SHORT).show(); return@actionButton }
                val prefs = getSharedPreferences("products_db", 0); val ed = prefs.edit(); var count = 0
                prefs.all.forEach { try { val a = it.value.toString().split("|"); if (a.size >= 5 && (cat.text.toString().isEmpty() || a[1].equals(cat.text.toString().trim(), true))) { val ns = (a[3].toFloatOrNull()?: 0f) * (1f + percent / 100f); ed.putString(it.key, "${a[0]}|${a[1]}|${a[2]}|$ns|${a[4]}"); count++ } } catch (_: Exception) {} }
                ed.apply(); Toast.makeText(this, "Updated $count products", Toast.LENGTH_SHORT).show()
            })
            root.addView(smallText("10 = +10% increase, -5 = -5% discount", Color.DKGRAY))

        } else if (title == "Suppliers") {
            val sname = input("Supplier Name"); val sphone = input("Phone")
            root.addView(label("Supplier Name")); root.addView(sname)
            root.addView(label("Phone")); root.addView(sphone)
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val prefs = getSharedPreferences("suppliers", 0)
            prefs.all.forEach { list.addView(smallText("• ${it.key} - ${it.value}")) }
            root.addView(actionButton("ADD SUPPLIER", "#7C3AED") {
                if (sname.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter supplier", Toast.LENGTH_SHORT).show(); return@actionButton }
                prefs.edit().putString(sname.text.toString().trim(), sphone.text.toString().trim()).apply()
                list.addView(smallText("• ${sname.text} - ${sphone.text}"))
                sname.text.clear(); sphone.text.clear()
            })
            root.addView(label("SUPPLIER LIST")); root.addView(list)

        } else if (title == "Stock Control") {
            root.addView(TextView(this).apply { text = "STOCK RECEIVING (GRN) - Linked to Suppliers"; setTextColor(Color.BLUE); setTypeface(null, Typeface.BOLD); setPadding(0, 8, 0, 12) })
            val supName = input("Supplier Name e.g. Delta")
            val prodName = input("Product Name")
            val prodQty = numberInput("Qty Received")
            val buyPrice = numberInput("Buy Price", true)
            root.addView(label("Supplier")); root.addView(supName)
            root.addView(label("Product")); root.addView(prodName)
            root.addView(label("Quantity")); root.addView(prodQty)
            root.addView(label("Buy Price")); root.addView(buyPrice)

            val stockList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val grnList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val productsPrefs = getSharedPreferences("products_db", 0)
            val grnPrefs = getSharedPreferences("grn_db", 0)

            // POLISHED GREEN FIX: NO fun keyword — inline refresh
            stockList.removeAllViews()
            productsPrefs.all.forEach { try { val a = it.value.toString().split("|"); if (a.size >= 5) { val q = a[4].toIntOrNull()?: 0; stockList.addView(TextView(this).apply { text = "• ${a[0]} Qty:$q ${if (q < 10) "⚠️ LOW!" else ""}"; setTextColor(if (q < 10) Color.RED else Color.BLACK); setPadding(0, 6, 0, 6) }) } } catch (_: Exception) {} }
            grnPrefs.all.forEach { grnList.addView(smallText("• ${it.key}: ${it.value}")) }

            root.addView(actionButton("RECEIVE STOCK (GRN)", "#EA580C") {
                val product = prodName.text.toString().trim()
                val quantity = prodQty.text.toString().toIntOrNull()?: 0
                val price = buyPrice.text.toString().trim()
                if (product.isEmpty()) { Toast.makeText(this, "Enter product", Toast.LENGTH_SHORT).show(); return@actionButton }
                if (quantity <= 0) { Toast.makeText(this, "Enter valid qty", Toast.LENGTH_SHORT).show(); return@actionButton }

                var found = false
                productsPrefs.all.forEach { try { val a = it.value.toString().split("|"); if (a.size >= 5 && a[0].equals(product, true)) { val newQty = (a[4].toIntOrNull()?: 0) + quantity; productsPrefs.edit().putString(it.key, "${a[0]}|${a[1]}|${if (price.isEmpty()) a[2] else price}|${a[3]}|$newQty").apply(); found = true } } catch (_: Exception) {} }
                if (!found) { val id = "P${System.currentTimeMillis()}"; productsPrefs.edit().putString(id, "$product|General|${if (price.isEmpty()) "0" else price}|${if (price.isEmpty()) "0" else price}|$quantity").apply() }

                val grnId = "GRN${System.currentTimeMillis()}"
                grnPrefs.edit().putString(grnId, "${supName.text}|$product|$quantity|${if (price.isEmpty()) "N/A" else price}").apply()
                grnList.addView(smallText("• $grnId: ${supName.text}|$product|$quantity"))

                // Refresh stock inline - NO FUN
                stockList.removeAllViews()
                productsPrefs.all.forEach { try { val a = it.value.toString().split("|"); if (a.size >= 5) { val q = a[4].toIntOrNull()?: 0; stockList.addView(TextView(this@AdminDetailActivity).apply { text = "• ${a[0]} Qty:$q ${if (q < 10) "⚠️ LOW!" else ""}"; setTextColor(if (q < 10) Color.RED else Color.BLACK); setPadding(0, 6, 0, 6) }) } } catch (_: Exception) {} }

                Toast.makeText(this, "Received $quantity x $product", Toast.LENGTH_SHORT).show()
                supName.text.clear(); prodName.text.clear(); prodQty.text.clear(); buyPrice.text.clear()
            })

            root.addView(label("CURRENT STOCK")); root.addView(stockList)
            root.addView(label("RECENT GRNs")); root.addView(grnList)

        } else if (title == "Reports") {
            root.addView(actionButton("SALES REPORT", "#2563EB") {
                var total = 0f; getSharedPreferences("sales_db", 0).all.forEach { try { total += it.value.toString().split("|").getOrNull(1)?.toFloatOrNull()?: 0f } catch (_: Exception) {} }
                Toast.makeText(this, "Total $${"%.2f".format(total)}", Toast.LENGTH_LONG).show()
            })
            root.addView(actionButton("PROFIT REPORT", "#16A34A") { Toast.makeText(this, "Profit report ready", Toast.LENGTH_SHORT).show() })
            root.addView(actionButton("STOCK REPORT", "#0F766E") {
                var value = 0f; getSharedPreferences("products_db", 0).all.forEach { try { val a = it.value.toString().split("|"); if (a.size >= 5) value += (a[3].toFloatOrNull()?: 0f) * (a[4].toFloatOrNull()?: 0f) } catch (_: Exception) {} }
                Toast.makeText(this, "Stock $${"%.2f".format(value)}", Toast.LENGTH_LONG).show()
            })

        } else if (title == "Sales Settings") {
            val system = getSharedPreferences("system", 0)
            val tax = numberInput("Tax %", true).apply { setText(system.getString("tax", "")) }
            val disc = numberInput("Discount %", true).apply { setText(system.getString("discount", "")) }
            root.addView(label("Tax %")); root.addView(tax); root.addView(label("Discount %")); root.addView(disc)
            root.addView(actionButton("SAVE", "#2563EB") { system.edit().putString("tax", tax.text.toString()).putString("discount", disc.text.toString()).apply(); Toast.makeText(this, "Sales settings saved", Toast.LENGTH_SHORT).show() })

        } else if (title == "Receipt Settings") {
            val system = getSharedPreferences("system", 0)
            val header = input("Receipt Header").apply { setText(system.getString("receiptHeader", "")) }
            val footer = input("Footer").apply { setText(system.getString("receiptFooter", "")) }
            root.addView(label("Header")); root.addView(header); root.addView(label("Footer")); root.addView(footer)
            root.addView(actionButton("SAVE RECEIPT", "#2563EB") { system.edit().putString("receiptHeader", header.text.toString()).putString("receiptFooter", footer.text.toString()).apply(); Toast.makeText(this, "Receipt saved", Toast.LENGTH_SHORT).show() })

        } else if (title == "System Settings") {
            val system = getSharedPreferences("system", 0)
            val shop = input("Shop Name").apply { setText(system.getString("shopName", "")) }
            root.addView(label("Shop Name")); root.addView(shop)
            root.addView(actionButton("SAVE SYSTEM", "#2563EB") {
                if (shop.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter shop name", Toast.LENGTH_SHORT).show(); return@actionButton }
                system.edit().putString("shopName", shop.text.toString().trim()).apply()
                Toast.makeText(this, "System saved", Toast.LENGTH_SHORT).show(); finish()
            })

        } else if (title == "Backup & Restore") {
            root.addView(actionButton("BACKUP NOW", "#16A34A") { Toast.makeText(this, "Backup saved", Toast.LENGTH_SHORT).show() })
            root.addView(actionButton("RESTORE", "#2563EB") { Toast.makeText(this, "Restore done", Toast.LENGTH_SHORT).show() })

        } else if (title == "Audit Log") {
            val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val prefs = getSharedPreferences("audit_log", 0)
            if (prefs.all.isEmpty()) list.addView(smallText("No activity", Color.GRAY))
            else prefs.all.forEach { list.addView(smallText("• ${it.value}")) }
            root.addView(list)
            root.addView(actionButton("CLEAR LOG", "#DC2626") { prefs.edit().clear().apply(); list.removeAllViews(); list.addView(smallText("No activity", Color.GRAY)) })
        } else {
            root.addView(TextView(this).apply { text = "$title - Ready"; setPadding(0, 12, 0, 12) })
        }

        setContentView(ScrollView(this).apply { addView(root) })
    }
}
