package com.smartpos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class AdminDetailActivity : Activity() {

    private val grnProducts = mutableListOf<ProductLine>()
    private val keyCounter = AtomicLong(0)

    data class ProductLine(val name: String, val code: String, val cost: Double, val sell: Double, val qty: Int)

    private fun romLabel(t: String): TextView {
        return TextView(this).apply {
            text = t.uppercase(); textSize = 11f; setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE); setBackgroundColor(Color.parseColor("#1E293B")); setPadding(12,8,12,8)
        }
    }
    private fun inputBox(h: String = ""): EditText {
        return EditText(this).apply { hint = h; setPadding(20,14,20,14); setBackgroundColor(Color.WHITE); setTextColor(Color.BLACK) }
    }
    private fun numInputDec(h: String = ""): EditText {
        return EditText(this).apply { hint = h; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setPadding(20,14,20,14); setBackgroundColor(Color.WHITE); setTextColor(Color.BLACK) }
    }
    private fun numInputInt(h: String = ""): EditText {
        return EditText(this).apply { hint = h; inputType = InputType.TYPE_CLASS_NUMBER; setPadding(20,14,20,14); setBackgroundColor(Color.WHITE); setTextColor(Color.BLACK) }
    }
    private fun getToday(): String = SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date())
    private fun createGrnNumber(): String = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date()) + "-" + (System.currentTimeMillis() % 1000)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val title = intent.getStringExtra("TITLE")?: "Admin"
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(10,10,10,10) }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.parseColor("#1E293B")); setPadding(16,12,10,12) }
        head.addView(TextView(this).apply { text = title.uppercase(); setTextColor(Color.WHITE); textSize = 16f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
        head.addView(Button(this).apply { text = "BACK"; setBackgroundColor(Color.parseColor("#475569")); setTextColor(Color.WHITE); setOnClickListener { finish() } })
        root.addView(head)

        val isStock = title.equals("Stock Control",true) || title.contains("RECEIVE",true) || title.contains("STOCK",true) || title.contains("GRN",true)

        if (isStock) {
            val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC")); setPadding(8,8,8,8) }
            card.addView(romLabel("Supplier / Optional")); val sup = inputBox("Supplier"); card.addView(sup)
            card.addView(romLabel("Date")); val date = inputBox("").apply{ setText(getToday()) }; card.addView(date)
            card.addView(romLabel("Invoice No (Optional)")); val inv = inputBox("Invoice"); card.addView(inv)
            card.addView(romLabel("GRN No")); var grnNo = createGrnNumber(); val grn = inputBox("").apply{setText("GRN-$grnNo"); isEnabled = false}; card.addView(grn)

            val th = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#1E293B")); setPadding(6,8,6,8) }
            fun h(t: String): TextView { return TextView(this).apply{text = t; textSize = 10f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0,-2,1f); gravity = Gravity.CENTER} }
            th.addView(h("Product")); th.addView(h("Code")); th.addView(h("Cost")); th.addView(h("Sell")); th.addView(h("Qty")); card.addView(th)

            val tableBody = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }; card.addView(tableBody)

            val refresh = {
                tableBody.removeAllViews()
                if (grnProducts.isEmpty()) {
                    tableBody.addView(TextView(this).apply{text = "No products - Click ADD PRODUCT"; gravity = Gravity.CENTER; setPadding(0,20,0,20); setTextColor(Color.GRAY)})
                } else {
                    var tq = 0; var tc = 0.0
                    grnProducts.forEach { p ->
                        val row = LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;setPadding(4,4,4,4)}
                        fun cell(v:String):TextView=TextView(this).apply{text=v;textSize=11f;setTextColor(Color.BLACK);gravity=Gravity.CENTER;setPadding(2,8,2,8);layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
                        row.addView(cell(p.name));row.addView(cell(p.code));row.addView(cell("%.2f".format(p.cost)));row.addView(cell("%.2f".format(p.sell)));row.addView(cell(p.qty.toString()))
                        tableBody.addView(row); tq += p.qty; tc += p.cost * p.qty
                    }
                    tableBody.addView(TextView(this).apply{text = "Total: ${grnProducts.size} Products, Qty: $tq COST: $${"%.2f".format(tc)}"; setBackgroundColor(Color.parseColor("#DBEAFE")); setPadding(10,8,10,8); setTypeface(null, Typeface.BOLD)})
                }
            }; refresh.invoke()

            card.addView(Button(this).apply{
                text = "ADD PRODUCT"; setBackgroundColor(Color.parseColor("#E5E7EB"))
                setOnClickListener{
                    val dlg = LinearLayout(this@AdminDetailActivity).apply{orientation = LinearLayout.VERTICAL; setPadding(20,20,20,20)}
                    val n = inputBox("").apply{hint = "Product Name"}; dlg.addView(romLabel("Product Name")); dlg.addView(n)
                    val co = inputBox("").apply{hint = "Code"}; dlg.addView(romLabel("Code")); dlg.addView(co)
                    val c = numInputDec("").apply{hint = "Cost"}; dlg.addView(romLabel("Cost")); dlg.addView(c)
                    val s = numInputDec("").apply{hint = "Selling"}; dlg.addView(romLabel("Selling")); dlg.addView(s)
                    val q = numInputInt("").apply{hint = "Quantity"}; dlg.addView(romLabel("Quantity")); dlg.addView(q)
                    android.app.AlertDialog.Builder(this@AdminDetailActivity).setTitle("Add Product").setView(dlg)
                      .setPositiveButton("ADD"){_,_->
                            val name = n.text.toString().trim()
                            if(name.isEmpty()){Toast.makeText(this@AdminDetailActivity,"Name required",Toast.LENGTH_SHORT).show(); return@setPositiveButton}
                            val rawCode = co.text.toString().trim()
                            val code = if(rawCode.isEmpty()) "N/A" else rawCode
                            val cost = c.text.toString().trim().toDoubleOrNull()?: 0.0
                            val sell = s.text.toString().trim().toDoubleOrNull()?: 0.0
                            val qty = q.text.toString().trim().toIntOrNull()?: 0
                            if(qty <= 0){Toast.makeText(this@AdminDetailActivity,"Qty must be >0 integer",Toast.LENGTH_SHORT).show(); return@setPositiveButton}
                            if(cost<0||sell<0){Toast.makeText(this@AdminDetailActivity,"Price cannot be negative",Toast.LENGTH_SHORT).show(); return@setPositiveButton}
                            grnProducts.add(ProductLine(name, code, cost, sell, qty))
                            refresh.invoke(); Toast.makeText(this@AdminDetailActivity,"$name ADDED!",Toast.LENGTH_SHORT).show()
                        }.setNegativeButton("CANCEL",null).show()
                }
            })

            val bottom = LinearLayout(this).apply{orientation = LinearLayout.HORIZONTAL; setPadding(0,10,0,0)}

            bottom.addView(Button(this).apply{
                text = "SAVE DRAFT"; setBackgroundColor(Color.parseColor("#E5E7EB")); layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(0,0,4,0)}
                setOnClickListener{
                    if (grnProducts.isEmpty()) { Toast.makeText(this@AdminDetailActivity, "Add products first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                    try {
                        val json = JSONObject(); json.put("grnNo", grnNo); json.put("supplier", sup.text.toString().trim()); json.put("date", date.text.toString().trim()); json.put("invoice", inv.text.toString().trim())
                        val arr = JSONArray(); grnProducts.forEach { p -> val o = JSONObject(); o.put("name", p.name); o.put("code", p.code); o.put("cost", p.cost); o.put("sell", p.sell); o.put("qty", p.qty); arr.put(o) }; json.put("items", arr)
                        getSharedPreferences("grn_drafts",0).edit().putString(grnNo, json.toString()).apply()
                        Toast.makeText(this@AdminDetailActivity, "DRAFT GRN-$grnNo SAVED!", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) { Toast.makeText(this@AdminDetailActivity, "Draft error: ${e.message}", Toast.LENGTH_LONG).show() }
                }
            })

            bottom.addView(Button(this).apply{
                text = "PRINT GRN"; setBackgroundColor(Color.parseColor("#E5E7EB")); layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)}
                setOnClickListener{
                    if (grnProducts.isEmpty()) { Toast.makeText(this@AdminDetailActivity, "Add products first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                    var tq = 0; var tc = 0.0; val sb = StringBuilder()
                    sb.append("GRN RECEIPT\nGRN: GRN-$grnNo\nSupplier: ${sup.text}\nDate: ${date.text}\nInvoice: ${inv.text}\n\n")
                    sb.append("Product | Code | Cost | Sell | Qty\n--------------------------------\n")
                    grnProducts.forEach { p -> sb.append("${p.name} | ${p.code} | ${p.cost} | ${p.sell} | ${p.qty}\n"); tq+=p.qty; tc+=p.cost*p.qty }
                    sb.append("\nTotal Products: ${grnProducts.size}\nTotal Qty: $tq\nTOTAL COST: $${"%.2f".format(tc)}")
                    val i = Intent(Intent.ACTION_SEND); i.type = "text/plain"; i.putExtra(Intent.EXTRA_TEXT, sb.toString()); startActivity(Intent.createChooser(i, "Share GRN GRN-$grnNo"))
                }
            })

            bottom.addView(Button(this).apply{
                text = "COMPLETE RECEIVE"; setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(0,-2,1.3f).apply{setMargins(4,0,0,0)}
                setOnClickListener{
                    if (grnProducts.isEmpty()) { Toast.makeText(this@AdminDetailActivity, "Add products first", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                    val prodPrefs = getSharedPreferences("products_db",0)
                    val all = prodPrefs.all
                    val byCode = mutableMapOf<String, String>(); val byName = mutableMapOf<String, String>()
                    all.forEach { (k,v) ->
                        try {
                            val s = v.toString()
                            var code=""; var name=""
                            if(s.trim().startsWith("{")){ val j=JSONObject(s); name=j.optString("name"); code=j.optString("code") }
                            else { val p=s.split("|"); name=p[0]; code=p.getOrNull(1)?:"" }
                            if(code.isNotEmpty() && code.uppercase()!="N/A") byCode[code.lowercase()] = k
                            if(name.isNotEmpty()) byName[name.lowercase()] = k
                        } catch (_: Exception) {}
                    }
                    val editor = prodPrefs.edit()
                    grnProducts.forEachIndexed { idx, p ->
                        val isRealCode = p.code.uppercase()!="N/A" && p.code.isNotBlank()
                        val existingKey = if(isRealCode) byCode[p.code.lowercase()]?: byName[p.name.lowercase()] else byName[p.name.lowercase()]
                        if (existingKey!= null) {
                            try {
                                val oldStr = all[existingKey].toString()
                                var oldQty=0; var oldCost=0.0; var oldSell=0.0; var oldName=p.name; var oldCode=p.code
                                if(oldStr.trim().startsWith("{")){ val j=JSONObject(oldStr); oldName=j.optString("name",p.name); oldCode=j.optString("code",p.code); oldCost=j.optDouble("cost",0.0); oldSell=j.optDouble("sell",0.0); oldQty=j.optInt("qty",0) }
                                else { val parts=oldStr.split("|"); oldName=parts.getOrNull(0)?:p.name; oldCode=parts.getOrNull(1)?:p.code; oldCost=parts.getOrNull(2)?.toDoubleOrNull()?:0.0; oldSell=parts.getOrNull(3)?.toDoubleOrNull()?:0.0; oldQty=parts.getOrNull(4)?.toIntOrNull()?:0 }
                                val newQty = oldQty + p.qty
                                val avgCost = if(oldQty>0) ((oldCost*oldQty)+(p.cost*p.qty))/newQty else p.cost
                                val finalSell = if(p.sell>0) p.sell else oldSell
                                val nj = JSONObject(); nj.put("name", p.name); nj.put("code", p.code); nj.put("cost", avgCost); nj.put("sell", finalSell); nj.put("qty", newQty)
                                editor.putString(existingKey, nj.toString())
                            } catch (_: Exception) {}
                        } else {
                            val safeCode = p.code.replace(" ","_").replace("|","_").ifEmpty { "NA" }
                            val uniqueKey = "P_${safeCode}_${System.currentTimeMillis()}_${keyCounter.incrementAndGet()}_${idx}"
                            val nj = JSONObject(); nj.put("name", p.name); nj.put("code", p.code); nj.put("cost", p.cost); nj.put("sell", p.sell); nj.put("qty", p.qty)
                            editor.putString(uniqueKey, nj.toString())
                        }
                    }
                    editor.apply()
                    getSharedPreferences("grn_db",0).edit().putString(grnNo, "${sup.text}|${inv.text}|${grnProducts.size}").apply()
                    getSharedPreferences("grn_drafts",0).edit().remove(grnNo).apply()
                    Toast.makeText(this@AdminDetailActivity, "GRN GRN-$grnNo COMPLETED! Stock updated!", Toast.LENGTH_LONG).show()
                    grnProducts.clear(); refresh.invoke(); grnNo = createGrnNumber(); grn.setText("GRN-$grnNo")
                }
            })
            card.addView(bottom); root.addView(card)
        } else {
            root.addView(TextView(this).apply{text="Section: $title - Coming soon.\nUse Stock Control for GRN.";setPadding(30,30,30,30);setTextColor(Color.GRAY);gravity=Gravity.CENTER})
        }
        setContentView(ScrollView(this).apply{isFillViewport = true; addView(root)})
    }
}
