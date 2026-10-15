package com.smartpos
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.*
import android.graphics.Color
import android.view.Gravity
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream

class StockTakeActivity : Activity() {
    private val names = mutableListOf<String>()
    private val inHands = mutableListOf<Int>()
    private val costs = mutableListOf<Double>()
    private val sells = mutableListOf<Double>()
    private val countedList = mutableListOf<Int>()
    private lateinit var listLayout: LinearLayout
    private lateinit var totalTxt: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try{
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(24,24,24,24); setBackgroundColor(Color.parseColor("#F5F5F5")) }
            root.addView(TextView(this).apply{ text="STOCK TAKE #437 FINAL+"; textSize=20f; setTextColor(Color.BLACK); gravity=Gravity.CENTER })
            totalTxt = TextView(this).apply{ text="Loading..."; setBackgroundColor(Color.WHITE); setPadding(20,20,20,20) }
            root.addView(totalTxt)

            // Row 1 SAVE + HISTORY
            val row1 = LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL }
            val saveBtn = Button(this).apply{ text="SAVE" }
            val histBtn = Button(this).apply{ text="HISTORY" }
            row1.addView(saveBtn, LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(0,0,6,0) })
            row1.addView(histBtn, LinearLayout.LayoutParams(0,-2,1f))
            root.addView(row1)

            // Row 2 EXPORT + CLEAR
            val row2 = LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL }
            val exportBtn = Button(this).apply{ text="EXPORT PDF" }
            val clearBtn = Button(this).apply{ text="CLEAR HIST" }
            row2.addView(exportBtn, LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(0,0,6,0) })
            row2.addView(clearBtn, LinearLayout.LayoutParams(0,-2,1f))
            root.addView(row2)

            listLayout = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL }
            root.addView(listLayout)
            root.addView(Button(this).apply{ text="BACK"; setOnClickListener{ finish() } })

            loadProducts()
            render()

            saveBtn.setOnClickListener{ save() }
            histBtn.setOnClickListener{ history() }
            exportBtn.setOnClickListener{ exportPdf() }
            clearBtn.setOnClickListener{ clearHistory() }

            scroll.addView(root)
            setContentView(scroll)
        }catch(e:Exception){ setContentView(TextView(this).apply{ text="Error: ${e.message}" }) }
    }

    private fun loadProducts(){
        names.clear(); inHands.clear(); costs.clear(); sells.clear(); countedList.clear()
        val seen = HashSet<String>()
        val prefs = listOf("products_main","products_rukudzo","products")
        for(pn in prefs){
            try{
                val sp = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in sp.all){
                    if(seen.contains(k)) continue
                    try{
                        val raw=v.toString()
                        var n=k; var q=0; var c=0.0; var s=0.0
                        if(raw.trim().startsWith("{")){
                            val o=JSONObject(raw)
                            n=o.optString("name", o.optString("productName",k))
                            q=o.optInt("quantity", o.optInt("qty", o.optInt("stock",0)))
                            c=o.optDouble("cost", o.optDouble("costPrice",0.0))
                            s=o.optDouble("price", o.optDouble("sellPrice", o.optDouble("sellingPrice", c*1.3)))
                        } else {
                            val parts=raw.split("|")
                            n=parts.getOrNull(0)?:k
                            q=parts.getOrNull(1)?.toIntOrNull()?:0
                            c=parts.getOrNull(2)?.toDoubleOrNull()?:0.0
                            s=parts.getOrNull(3)?.toDoubleOrNull()?: c
                        }
                        if(n.isNotBlank()){ names.add(n); inHands.add(q); costs.add(c); sells.add(s); countedList.add(q); seen.add(k) }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
        if(names.isEmpty()){ names.add("Sugar 2kg"); inHands.add(200); costs.add(2.5); sells.add(3.5); countedList.add(200) }
    }

    private fun render(){
        listLayout.removeAllViews()
        var totalVarCost=0.0; var totalVarSell=0.0
        for(i in names.indices){
            val variance = countedList[i] - inHands[i]
            totalVarCost += variance * costs[i]
            totalVarSell += variance * sells[i]
            val card = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(18,18,18,18); val lp=LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,12,0,0); layoutParams=lp }
            card.addView(TextView(this).apply{ text="${i+1}. ${names[i]}"; textSize=16f; setTextColor(Color.BLACK) })
            card.addView(TextView(this).apply{ text="InHand:${inHands[i]} Counted:${countedList[i]} Var:$variance"; textSize=14f })
            card.addView(TextView(this).apply{ text="Cost: $${costs[i]} Sell: $${sells[i]} | Var Cost:$${"%.2f".format(variance*costs[i])} Var Sell:$${"%.2f".format(variance*sells[i])}"; textSize=13f; setTextColor(if(variance<0) Color.RED else Color.parseColor("#0A7A00")) })
            val et = EditText(this).apply{ hint="Counted"; setText(countedList[i].toString()) }
            val idx=i
            val upBtn = Button(this).apply{ text="Update Counted" }
            upBtn.setOnClickListener{
                try{ val nc = et.text.toString().toIntOrNull()?: countedList[idx]; countedList[idx]=nc; render() }catch(_:Exception){}
            }
            card.addView(et); card.addView(upBtn)
            listLayout.addView(card)
        }
        totalTxt.text="Products:${names.size} | Var Cost: $${"%.2f".format(totalVarCost)} | Var Sell: $${"%.2f".format(totalVarSell)}"
    }

    private fun save(){
        try{
            val sp=getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
            val id=System.currentTimeMillis().toString()
            val o=JSONObject()
            o.put("date", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date()))
            o.put("count", names.size)
            var tvCost=0.0; var tvSell=0.0
            for(i in names.indices){ tvCost += (countedList[i]-inHands[i])*costs[i]; tvSell += (countedList[i]-inHands[i])*sells[i] }
            o.put("varCost", tvCost); o.put("varSell", tvSell)
            val arr=JSONArray()
            for(i in names.indices){ val jo=JSONObject(); jo.put("name",names[i]); jo.put("inHand",inHands[i]); jo.put("counted",countedList[i]); jo.put("cost",costs[i]); jo.put("sell",sells[i]); jo.put("var",countedList[i]-inHands[i]); arr.put(jo) }
            o.put("items",arr)
            sp.edit().putString("take_$id", o.toString()).apply()
            Toast.makeText(this,"Saved! Var Cost $${"%.2f".format(tvCost)} Var Sell $${"%.2f".format(tvSell)}",Toast.LENGTH_LONG).show()
        }catch(e:Exception){ Toast.makeText(this,"Save fail ${e.message}",Toast.LENGTH_LONG).show() }
    }

    private fun history(){
        try{
            val sp=getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
            if(sp.all.isEmpty()){ Toast.makeText(this,"No history",Toast.LENGTH_SHORT).show(); return }
            val lay=LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(20,20,20,20) }
            lay.addView(TextView(this).apply{ text="HISTORY (Last 15)\n"; textSize=18f })
            for((_,v) in sp.all.entries.sortedByDescending{it.key}.take(15)){
                try{ val o=JSONObject(v.toString()); lay.addView(TextView(this).apply{ text="${o.optString("date")} - ${o.optInt("count")} items\nVar Cost $${"%.2f".format(o.optDouble("varCost"))} Var Sell $${"%.2f".format(o.optDouble("varSell"))}\n"; setPadding(0,10,0,10) }) }catch(_:Exception){}
            }
            val sv=ScrollView(this); sv.addView(lay)
            android.app.AlertDialog.Builder(this).setView(sv).setPositiveButton("Close",null).show()
        }catch(e:Exception){ Toast.makeText(this,"Hist err ${e.message}",Toast.LENGTH_SHORT).show() }
    }

    private fun clearHistory(){
        try{
            android.app.AlertDialog.Builder(this).setTitle("Clear All History?").setMessage("Delete all saved stock takes?")
           .setPositiveButton("YES CLEAR"){_,_->
                getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE).edit().clear().apply()
                Toast.makeText(this,"History cleared",Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel",null).show()
        }catch(e:Exception){ Toast.makeText(this,"Clear fail ${e.message}",Toast.LENGTH_SHORT).show() }
    }

    private fun exportPdf(){
        try{
            val sp=getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
            if(sp.all.isEmpty()){ Toast.makeText(this,"No data to export",Toast.LENGTH_SHORT).show(); return }

            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            var page = pdfDoc.startPage(pageInfo)
            var canvas = page.canvas
            var paint = Paint()
            paint.textSize = 14f
            var y = 40f

            canvas.drawText("SMART POS - STOCK TAKE REPORT", 40f, y, paint); y+=25f
            paint.textSize = 10f
            canvas.drawText("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date())}", 40f, y, paint); y+=20f

            // Current take
            paint.textSize = 12f
            canvas.drawText("Current Stock (Live):", 40f, y, paint); y+=18f
            paint.textSize = 9f
            canvas.drawText("No | Name | InHand | Counted | Var | Cost | Sell", 40f, y, paint); y+=12f
            for(i in names.indices){
                if(y>800){ pdfDoc.finishPage(page); val pi=PdfDocument.PageInfo.Builder(595,842,pdfDoc.pages.size+1).create(); page=pdfDoc.startPage(pi); canvas=page.canvas; y=40f }
                val line = "${i+1} ${names[i].take(18)} ${inHands[i]} ${countedList[i]} ${countedList[i]-inHands[i]} ${costs[i]} ${sells[i]}"
                canvas.drawText(line, 40f, y, paint); y+=11f
            }
            y+=15f
            // History summary
            if(y>750){ pdfDoc.finishPage(page); val pi=PdfDocument.PageInfo.Builder(595,842,pdfDoc.pages.size+1).create(); page=pdfDoc.startPage(pi); canvas=page.canvas; y=40f }
            paint.textSize = 12f
            canvas.drawText("Saved Takes History:", 40f, y, paint); y+=18f
            paint.textSize = 9f
            for((_,v) in sp.all.entries.sortedByDescending{it.key}.take(20)){
                if(y>800){ pdfDoc.finishPage(page); val pi=PdfDocument.PageInfo.Builder(595,842,pdfDoc.pages.size+1).create(); page=pdfDoc.startPage(pi); canvas=page.canvas; y=40f }
                try{ val o=JSONObject(v.toString()); canvas.drawText("${o.optString("date")} - ${o.optInt("count")} items VarCost ${"%.2f".format(o.optDouble("varCost"))}", 40f, y, paint); y+=11f }catch(_:Exception){}
            }

            pdfDoc.finishPage(page)
            val dir = getExternalFilesDir(null)?: filesDir
            val file = File(dir, "StockTake_${System.currentTimeMillis()}.pdf")
            pdfDoc.writeTo(FileOutputStream(file))
            pdfDoc.close()
            Toast.makeText(this,"PDF Exported: ${file.absolutePath}",Toast.LENGTH_LONG).show()
            android.app.AlertDialog.Builder(this).setTitle("PDF Exported").setMessage("Saved to:\n${file.absolutePath}").setPositiveButton("OK",null).show()
        }catch(e:Exception){ Toast.makeText(this,"Export failed: ${e.message}",Toast.LENGTH_LONG).show() }
    }
}