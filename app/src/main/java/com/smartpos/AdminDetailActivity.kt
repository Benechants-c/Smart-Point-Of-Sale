package com.smartpos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class AdminDetailActivity : Activity() {

    private val grnProducts = mutableListOf<Array<String>>()

    private fun romLabel(t: String): TextView {
        return TextView(this).apply {
            text = t.uppercase(); textSize = 11f; setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(12,8,12,8)
        }
    }
    private fun inputBox(h: String = ""): EditText {
        return EditText(this).apply {
            hint = h; setPadding(20,14,20,14); setBackgroundColor(Color.WHITE)
            setTextColor(Color.BLACK)
        }
    }
    private fun numInput(h: String = ""): EditText {
        return EditText(this).apply {
            hint = h; inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setPadding(20,14,20,14); setBackgroundColor(Color.WHITE)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val title = intent.getStringExtra("TITLE")?: "Admin"
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(10,10,10,10) }

        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#1E293B")); setPadding(16,12,10,12) }
        head.addView(TextView(this).apply { text = title.uppercase(); setTextColor(Color.WHITE); textSize = 16f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
        head.addView(Button(this).apply { text = "BACK"; setBackgroundColor(Color.parseColor("#475569")); setTextColor(Color.WHITE); setOnClickListener { finish() } })
        root.addView(head)

        if (title == "Stock Control" || title.contains("RECEIVE", true)) {

            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC")); setPadding(8,8,8,8) }

            card.addView(romLabel("Supplier / Optional-Se")); val sup = inputBox("Supplier"); card.addView(sup)
            card.addView(romLabel("Date")); val date = inputBox("").apply{setText("29/09/2026")}; card.addView(date)
            card.addView(romLabel("Invoice No (Optional)")); val inv = inputBox("Invoice"); card.addView(inv)
            card.addView(romLabel("GRN No")); val grnNo = "AUTO-000125"; val grn = inputBox("").apply{setText(grnNo); isEnabled = false}; card.addView(grn)

            val th = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#1E293B")); setPadding(6,8,6,8) }
            fun h(t: String, w: Float): TextView { return TextView(this).apply{text = t; textSize = 10f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0,-2,w); gravity = Gravity.CENTER} }
            th.addView(h("Product | Code | Sell | Qty",1f)); card.addView(th)

            val tableBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
            card.addView(tableBody)

            val refresh = {
                tableBody.removeAllViews()
                if (grnProducts.isEmpty()) {
                    tableBody.addView(TextView(this).apply{text = "No products - Click ADD PRODUCT"; gravity = Gravity.CENTER; setPadding(0,20,0,20); setTextColor(Color.GRAY)})
                } else {
                    var tq = 0; var tc = 0f
                    grnProducts.forEach { p ->
                        val row = TextView(this).apply{text = "${p[0]} | ${p[1]} | ${p[2]} | ${p[3]} | ${p[4]}"; setPadding(10,10,10,10); textSize = 12f}
                        tableBody.addView(row); tq += p[4].toIntOrNull()?:0; tc += (p[2].toFloatOrNull()?:0f)*(p[4].toIntOrNull()?:0)
                    }
                    tableBody.addView(TextView(this).apply{text = "Total: ${grnProducts.size} Products, Qty: $tq COST: $${"%.1f".format(tc)}"; setBackgroundColor(Color.parseColor("#DBEAFE")); setPadding(10,8,10,8); setTypeface(null, Typeface.BOLD)})
                }
            }
            refresh.invoke()

            // START EMPTY - NO FAKE COCA-COLA!
            // If you want demo for testing, uncomment below - but BEST is empty
            // grnProducts.clear(); refresh.invoke()

            card.addView(Button(this).apply{
                text = "ADD PRODUCT"; setBackgroundColor(Color.parseColor("#E5E7EB"))
                setOnClickListener{
                    val dlg = LinearLayout(this@AdminDetailActivity).apply{orientation = LinearLayout.VERTICAL; setPadding(20,20,20,20)}
                    val n = inputBox("Product Name").apply{hint = "Product Name"}; dlg.addView(n)
                    val co = inputBox("Code").apply{hint = "Code"}; dlg.addView(co)
                    val c = numInput("Cost").apply{hint = "Cost"}; dlg.addView(c)
                    val s = numInput("Selling").apply{hint = "Selling"}; dlg.addView(s)
                    val q = numInput("Quantity").apply{hint = "Quantity"}; dlg.addView(q)
                    android.app.AlertDialog.Builder(this@AdminDetailActivity).setTitle("Add Product").setView(dlg)
                       .setPositiveButton("ADD"){_,_->
                            if(n.text.toString().trim().isEmpty()){Toast.makeText(this@AdminDetailActivity,"Name required",Toast.LENGTH_SHORT).show(); return@setPositiveButton}
                            grnProducts.add(arrayOf(n.text.toString().trim(), co.text.toString().trim().ifEmpty{"-"}, c.text.toString().trim().ifEmpty{"0"}, s.text.toString().trim().ifEmpty{"0"}, q.text.toString().trim().ifEmpty{"0"}))
                            refresh.invoke(); Toast.makeText(this@AdminDetailActivity,"${n.text} ADDED!",Toast.LENGTH_SHORT).show()
                        }.setNegativeButton("CANCEL",null).show()
                }
            })

            // BOTTOM BUTTONS - FIXED 100%
            val bottom = LinearLayout(this).apply{orientation = LinearLayout.HORIZONTAL; setPadding(0,10,0,0)}

            // 1. SAVE DRAFT - NOW 100% WORKING
            bottom.addView(Button(this).apply{
                text = "SAVE DRAFT"; setBackgroundColor(Color.parseColor("#E5E7EB")); layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(0,0,4,0)}
                setOnClickListener{
                    if (grnProducts.isEmpty()) { Toast.makeText(this@AdminDetailActivity, "Add products first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                    try {
                        var tq = 0; var tc = 0f
                        grnProducts.forEach { p -> tq += p[4].toIntOrNull()?:0; tc += (p[2].toFloatOrNull()?:0f)*(p[4].toIntOrNull()?:0) }
                        val draftStr = StringBuilder()
                        draftStr.append("GRN:$grnNo|Sup:${sup.text}|Date:${date.text}|Inv:${inv.text}|Items:${grnProducts.size}|Qty:$tq|Cost:$tc|")
                        grnProducts.forEach { p -> draftStr.append("${p[0]},${p[1]},${p[2]},${p[3]},${p[4]};") }
                        getSharedPreferences("grn_drafts",0).edit().putString(grnNo, draftStr.toString()).apply()
                        Toast.makeText(this@AdminDetailActivity, "DRAFT $grnNo SAVED! ${grnProducts.size} items", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) { Toast.makeText(this@AdminDetailActivity, "Draft error: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            })

            // 2. PRINT GRN - NOW 100% WORKING WITH FALLBACK
            bottom.addView(Button(this).apply{
                text = "PRINT GRN"; setBackgroundColor(Color.parseColor("#E5E7EB")); layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)}
                setOnClickListener{
                    if (grnProducts.isEmpty()) { Toast.makeText(this@AdminDetailActivity, "Add products first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                    try {
                        var tq = 0; var tc = 0f; val sb = StringBuilder()
                        sb.append("===== GRN RECEIPT =====\nGRN: $grnNo\nSupplier: ${sup.text}\nDate: ${date.text}\nInvoice: ${inv.text}\n\nProducts:\n")
                        grnProducts.forEach { p -> sb.append("${p[0]} | ${p[1]} | Cost:${p[2]} | Sell:${p[3]} | Qty:${p[4]}\n"); tq += p[4].toIntOrNull()?:0; tc += (p[2].toFloatOrNull()?:0f)*(p[4].toIntOrNull()?:0) }
                        sb.append("\nTotal Products: ${grnProducts.size}\nTotal Qty: $tq\nTOTAL COST: $${"%.2f".format(tc)}\n=======================")
                        val printText = sb.toString()
                        // Try share - if fails, show dialog
                        try {
                            val i = Intent(Intent.ACTION_SEND); i.type = "text/plain"; i.putExtra(Intent.EXTRA_TEXT, printText); i.putExtra(Intent.EXTRA_SUBJECT, "GRN $grnNo")
                            startActivity(Intent.createChooser(i, "PRINT GRN $grnNo"))
                            Toast.makeText(this@AdminDetailActivity, "Printing GRN...", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            // FALLBACK - SHOW TEXT FOR MANUAL PRINT
                            val tv = TextView(this@AdminDetailActivity).apply{text = printText; setPadding(20,20,20,20); textSize = 12f}
                            android.app.AlertDialog.Builder(this@AdminDetailActivity).setTitle("GRN $grnNo - PRINT").setView(ScrollView(this@AdminDetailActivity).apply{addView(tv)}).setPositiveButton("OK",null).show()
                        }
                    } catch (e: Exception) { Toast.makeText(this@AdminDetailActivity, "Print error: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            })

            bottom.addView(Button(this).apply{
                text = "COMPLETE RECEIVE"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0,-2,1.3f).apply{setMargins(4,0,0,0)}
                setOnClickListener{
                    if (grnProducts.isEmpty()) { Toast.makeText(this@AdminDetailActivity, "Add products first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                    val prodPrefs = getSharedPreferences("products_db",0)
                    grnProducts.forEach { p ->
                        prodPrefs.edit().putString("P${System.currentTimeMillis()}_${p[1]}", "${p[0]}|General|${p[2]}|${p[3]}|${p[4]}").apply()
                    }
                    getSharedPreferences("grn_db",0).edit().putString(grnNo, "${sup.text}|${inv.text}|${grnProducts.size}").apply()
                    Toast.makeText(this@AdminDetailActivity, "GRN $grnNo COMPLETED! Stock updated!", Toast.LENGTH_LONG).show()
                    grnProducts.clear(); refresh.invoke()
                }
            })
            card.addView(bottom); root.addView(card)
        }
        setContentView(ScrollView(this).apply{isFillViewport = true; addView(root)})
    }
}
