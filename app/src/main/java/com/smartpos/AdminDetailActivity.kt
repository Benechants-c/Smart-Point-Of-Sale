package com.smartpos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*

class AdminDetailActivity : Activity() {

    private val grnProducts = mutableListOf<Array<String>>()

    private fun label(t: String): TextView {
        return TextView(this).apply { text = t; textSize = 13f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#334155")); setPadding(0,18,0,6) }
    }
    private fun input(h: String): EditText {
        return EditText(this).apply { hint = h; setPadding(24,18,24,18); setBackgroundColor(Color.parseColor("#F8FAFC")) }
    }
    private fun numberInput(h: String, dec: Boolean = false): EditText {
        return EditText(this).apply {
            hint = h; inputType = if (dec) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_NUMBER
            setPadding(24,18,24,18); setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
    }
    private fun actionButton(txt: String, col: String, click: () -> Unit): Button {
        return Button(this).apply {
            text = txt; setBackgroundColor(Color.parseColor(col)); setTextColor(Color.WHITE)
            setPadding(0,18,0,18); setOnClickListener { click() }
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,10,0,10)}
        }
    }
    private fun audit(m: String) { getSharedPreferences("audit_log",0).edit().putString(System.currentTimeMillis().toString(), m).apply() }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val title = intent.getStringExtra("TITLE")?: "Admin"
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16) }

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#1E293B")); setPadding(18,12,10,12) }
        head.addView(TextView(this).apply { text = title.uppercase(); setTextColor(Color.WHITE); textSize = 16f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
        head.addView(Button(this).apply { text = "BACK"; setBackgroundColor(Color.parseColor("#475569")); setTextColor(Color.WHITE); setOnClickListener { finish() } })
        root.addView(head)

        if (title == "Users & Permissions") {
            val fn = input("Full Name"); val un = input("Username"); val pw = input("Password").apply{ inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }; val role = input("Role")
            root.addView(fn); root.addView(un); root.addView(pw); root.addView(role)
            root.addView(actionButton("SAVE USER", "#16A34A"){ getSharedPreferences("users",0).edit().putString(un.text.toString().trim(), "${fn.text}|${pw.text}|${role.text}").apply(); Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show() })
        } else if (title == "Branches / Shops") {
            val sn = input("Shop Name"); val loc = input("Location")
            root.addView(sn); root.addView(loc)
            root.addView(actionButton("ADD SHOP", "#0F766E"){ getSharedPreferences("branches",0).edit().putString(sn.text.toString().trim(), loc.text.toString().trim()).apply(); Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show() })
        } else if (title == "Products & Categories") {
            val pn = input("Product"); val cat = input("Category"); val buy = input("Buy"); val sell = input("Sell"); val qty = input("Qty")
            root.addView(pn); root.addView(cat); root.addView(buy); root.addView(sell); root.addView(qty)
            root.addView(actionButton("ADD PRODUCT", "#2563EB"){ getSharedPreferences("products_db",0).edit().putString("P${System.currentTimeMillis()}", "${pn.text}|${cat.text.ifEmpty{"General"}}|${buy.text.ifEmpty{"0"}}|${sell.text.ifEmpty{"0"}}|${qty.text.ifEmpty{"0"}}").apply(); Toast.makeText(this, "Added", Toast.LENGTH_SHORT).show() })
        } else if (title == "Stock Control" || title == "Stock & Purchasing" || title.contains("RECEIVE", true)) {

            root.addView(TextView(this).apply { text = "≡ RECEIVE STOCK"; textSize = 16f; setTypeface(null, Typeface.BOLD); setPadding(0,8,0,8) })
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC")); setPadding(12,12,12,12) }

            card.addView(label("Supplier")); val sup = input("Supplier e.g. ABC Suppliers"); card.addView(sup)
            card.addView(label("Date")); val date = input("Date").apply{setText("15 Jan 2024")}; card.addView(date)
            card.addView(label("Invoice No")); val inv = input("Invoice No"); card.addView(inv)
            card.addView(label("GRN No")); val grnNo = "AUTO-${System.currentTimeMillis()%100000}"; val grn = input(grnNo).apply{setText(grnNo); isEnabled = false; setBackgroundColor(Color.parseColor("#D1D5DB"))}; card.addView(grn)

            val th = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#BFDBFE")); setPadding(6,8,6,8) }
            fun h(t: String, w: Float): TextView { return TextView(this).apply{text = t; textSize = 10f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0,-2,w); gravity = Gravity.CENTER} }
            th.addView(h("Product Name",2f)); th.addView(h("Code",1f)); th.addView(h("Cost",1f)); th.addView(h("Selling",1f)); th.addView(h("Quantity",1f))
            card.addView(th)

            val tableBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
            card.addView(tableBody)

            val refreshTable = {
                tableBody.removeAllViews()
                if (grnProducts.isEmpty()) {
                    tableBody.addView(TextView(this@AdminDetailActivity).apply{text = "No products added yet. Click + ADD PRODUCT"; setTextColor(Color.GRAY); gravity = Gravity.CENTER; setPadding(0,20,0,20); textSize = 12f})
                } else {
                    var tq = 0; var tc = 0f
                    grnProducts.forEach { p ->
                        val r = LinearLayout(this@AdminDetailActivity).apply{orientation = LinearLayout.HORIZONTAL; setPadding(4,8,4,8)}
                        fun c(t: String, w: Float): TextView { return TextView(this@AdminDetailActivity).apply{text = t; textSize = 11f; layoutParams = LinearLayout.LayoutParams(0,-2,w); gravity = Gravity.CENTER} }
                        r.addView(c(p[0],2f)); r.addView(c(p[1],1f)); r.addView(c("$${p[2]}",1f)); r.addView(c("$${p[3]}",1f)); r.addView(c(p[4],1f))
                        tableBody.addView(r); tq += p[4].toIntOrNull()?:0; tc += (p[2].toFloatOrNull()?:0f)*(p[4].toIntOrNull()?:0)
                    }
                    val totals = LinearLayout(this@AdminDetailActivity).apply{orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#DBEAFE")); setPadding(8,8,8,8)}
                    totals.addView(TextView(this@AdminDetailActivity).apply{text = "Total Products: ${grnProducts.size} Total Qty: $tq TOTAL COST: $${"%.2f".format(tc)}"; textSize = 11f; setTypeface(null, Typeface.BOLD)})
                    tableBody.addView(totals)
                }
            }
            refreshTable.invoke()

            card.addView(label("Add Product - YOUR INPUT"))
            val pName = input("Product Name"); val pCode = input("Code"); val pCost = numberInput("Cost", true); val pSell = numberInput("Selling", true); val pQty = numberInput("Quantity")
            card.addView(pName); card.addView(pCode); card.addView(pCost); card.addView(pSell); card.addView(pQty)

            card.addView(actionButton("+ ADD PRODUCT", "#2563EB") {
                if (pName.text.toString().trim().isEmpty()) { Toast.makeText(this, "Enter product name", Toast.LENGTH_SHORT).show(); return@actionButton }
                grnProducts.add(arrayOf(pName.text.toString().trim(), pCode.text.toString().trim().ifEmpty{"-"}, pCost.text.toString().trim().ifEmpty{"0"}, pSell.text.toString().trim().ifEmpty{"0"}, pQty.text.toString().trim().ifEmpty{"0"}))
                pName.text.clear(); pCode.text.clear(); pCost.text.clear(); pSell.text.clear(); pQty.text.clear()
                refreshTable.invoke()
            })

            val bottom = LinearLayout(this).apply{orientation = LinearLayout.HORIZONTAL; setPadding(0,10,0,0)}
            bottom.addView(Button(this).apply{
                text = "SAVE DRAFT"; setBackgroundColor(Color.parseColor("#E5E7EB")); setTextColor(Color.BLACK); layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(0,0,4,0)}
                setOnClickListener{
                    if (grnProducts.isEmpty()) { Toast.makeText(this@AdminDetailActivity, "Add products first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                    val draftData = StringBuilder(); draftData.append("${sup.text}|${date.text}|${inv.text}|")
                    grnProducts.forEach { p -> draftData.append("${p[0]},${p[1]},${p[2]},${p[3]},${p[4]};") }
                    getSharedPreferences("grn_drafts",0).edit().putString(grnNo, draftData.toString()).apply()
                    audit("GRN Draft $grnNo saved"); Toast.makeText(this@AdminDetailActivity, "DRAFT $grnNo SAVED!", Toast.LENGTH_LONG).show()
                }
            })
            bottom.addView(Button(this).apply{
                text = "PRINT GRN"; setBackgroundColor(Color.parseColor("#E5E7EB")); setTextColor(Color.BLACK); layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)}
                setOnClickListener{
                    if (grnProducts.isEmpty()) { Toast.makeText(this@AdminDetailActivity, "Add products first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                    var tq = 0; var tc = 0f; val sb = StringBuilder()
                    sb.append("===== GRN RECEIPT =====\nGRN: $grnNo\nSupplier: ${sup.text}\nDate: ${date.text}\nInvoice: ${inv.text}\n\nProducts:\n")
                    grnProducts.forEach { p -> sb.append("${p[0]} | ${p[1]} | Cost:${p[2]} | Sell:${p[3]} | Qty:${p[4]}\n"); tq += p[4].toIntOrNull()?:0; tc += (p[2].toFloatOrNull()?:0f)*(p[4].toIntOrNull()?:0) }
                    sb.append("\nTotal Products: ${grnProducts.size}\nTotal Qty: $tq\nTOTAL COST: $${"%.2f".format(tc)}\n=======================")
                    val i = Intent(Intent.ACTION_SEND); i.type = "text/plain"; i.putExtra(Intent.EXTRA_TEXT, sb.toString())
                    startActivity(Intent.createChooser(i, "Share GRN $grnNo"))
                }
            })
            bottom.addView(Button(this).apply{
                text = "COMPLETE"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0,-2,1.3f).apply{setMargins(4,0,0,0)}
                setOnClickListener{
                    if (grnProducts.isEmpty()) { Toast.makeText(this@AdminDetailActivity, "Add products first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                    val prodPrefs = getSharedPreferences("products_db",0)
                    grnProducts.forEach { p ->
                        var found = false
                        prodPrefs.all.forEach{ try{ val a = it.value.toString().split("|"); if(a.size>=5 && a[0].equals(p[0],true)){ val nq = (a[4].toIntOrNull()?:0)+(p[4].toIntOrNull()?:0); prodPrefs.edit().putString(it.key, "${a[0]}|${a[1]}|${p[2]}|${p[3]}|$nq").apply(); found = true } }catch(_: Exception){} }
                        if (!found){ prodPrefs.edit().putString("P${System.currentTimeMillis()}", "${p[0]}|General|${p[2]}|${p[3]}|${p[4]}").apply() }
                    }
                    getSharedPreferences("grn_db",0).edit().putString(grnNo, "${sup.text}|${inv.text}|${grnProducts.size}").apply()
                    getSharedPreferences("grn_drafts",0).edit().remove(grnNo).apply()
                    audit("GRN $grnNo COMPLETED"); Toast.makeText(this@AdminDetailActivity, "GRN $grnNo COMPLETED!", Toast.LENGTH_LONG).show()
                    grnProducts.clear(); refreshTable.invoke()
                }
            })
            card.addView(bottom); root.addView(card)
        } else { root.addView(TextView(this).apply{text = "$title - Ready"}) }
        setContentView(ScrollView(this).apply{isFillViewport = true; addView(root)})
    }
}
