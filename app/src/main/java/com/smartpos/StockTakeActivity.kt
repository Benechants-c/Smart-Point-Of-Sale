package com.smartpos
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.*
import android.graphics.Color
import android.view.Gravity
import android.graphics.Typeface
import org.json.JSONObject
import org.json.JSONArray
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream

class StockTakeActivity : Activity() {
    private val names = mutableListOf<String>()
    private val inHands = mutableListOf<Int>()
    private val costs = mutableListOf<Double>()
    private val sells = mutableListOf<Double>()
    private val countedList = mutableListOf<Int>()
    private lateinit var tableLayout: LinearLayout
    private lateinit var totalTxt: TextView
    private lateinit var titleTxt: TextView
    private lateinit var shopSpinner: Spinner
    private var currentShop = "Main Shop"
    private var shopList = mutableListOf<String>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        try{
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F2F4F8")) }

            // ===== TOP TITLE BAR =====
            val titleBar = LinearLayout(this).apply{ setBackgroundColor(Color.parseColor("#0F1F3C")); setPadding(20,20,20,20); orientation=LinearLayout.VERTICAL }
            titleTxt = TextView(this).apply{ text="STOCK TAKE SHEET"; textSize=18f; setTextColor(Color.WHITE); typeface=Typeface.DEFAULT_BOLD }
            val subTitle = TextView(this).apply{ text="Branch Stock Verification"; textSize=12f; setTextColor(Color.parseColor("#AAB8D0")) }
            titleBar.addView(titleTxt)
            titleBar.addView(subTitle)
            root.addView(titleBar)

            // ===== SHOP SELECTOR BAR =====
            val shopBar = LinearLayout(this).apply{ setBackgroundColor(Color.WHITE); setPadding(15,12,15,12); orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
            shopBar.addView(TextView(this).apply{ text="Shop Branch: "; textSize=13f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.BLACK) })
            shopSpinner = Spinner(this)
            loadShopList()
            shopSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shopList)
            shopSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
                override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos:Int, id:Long){
                    currentShop = shopList[pos]
                    titleTxt.text = "STOCK TAKE SHEET - $currentShop"
                    loadProductsForShop(currentShop)
                    renderTable()
                }
                override fun onNothingSelected(p: AdapterView<*>?){}
            }
            shopBar.addView(shopSpinner, LinearLayout.LayoutParams(-1,-2))
            root.addView(shopBar)

            // ===== SUMMARY BAR =====
            totalTxt = TextView(this).apply{ text="Loading..."; setBackgroundColor(Color.parseColor("#DBE7FF")); setPadding(15,10,15,10); textSize=12f; typeface=Typeface.DEFAULT_BOLD; setTextColor(Color.parseColor("#0F1F3C")) }
            root.addView(totalTxt)

            // ===== TABLE COLUMN TITLES =====
            val colHeader = LinearLayout(this).apply{ setBackgroundColor(Color.parseColor("#1B2A4A")); setPadding(8,12,8,12); orientation=LinearLayout.HORIZONTAL }
            fun addHeader(text:String, weight:Float){
                colHeader.addView(TextView(this).apply{ this.text=text; setTextColor(Color.WHITE); textSize=10f; typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER }, LinearLayout.LayoutParams(0,-2,weight))
            }
            addHeader("PRODUCT", 2.5f)
            addHeader("IN HAND", 0.8f)
            addHeader("COST", 0.7f)
            addHeader("SELL", 0.7f)
            addHeader("COUNTED", 1f)
            addHeader("VAR", 0.6f)
            root.addView(colHeader)

            // ===== TABLE ROWS CONTAINER =====
            tableLayout = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE) }
            root.addView(tableLayout)

            // ===== ACTION BUTTONS =====
            val btnBar = LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setPadding(10,10,10,10) }
            val saveBtn = Button(this).apply{ text="SAVE"; setBackgroundColor(Color.parseColor("#1B8A3D")); setTextColor(Color.WHITE); textSize=12f }
            val histBtn = Button(this).apply{ text="HISTORY"; setBackgroundColor(Color.parseColor("#2C3E50")); setTextColor(Color.WHITE); textSize=12f }
            val exportBtn = Button(this).apply{ text="PDF"; setBackgroundColor(Color.parseColor("#1565C0")); setTextColor(Color.WHITE); textSize=12f }
            btnBar.addView(saveBtn, LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(0,0,4,0) })
            btnBar.addView(histBtn, LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(0,0,4,0) })
            btnBar.addView(exportBtn, LinearLayout.LayoutParams(0,-2,1f))
            root.addView(btnBar)

            root.addView(Button(this).apply{ text="BACK"; setOnClickListener{ finish() } })

            loadProductsForShop(if(shopList.isNotEmpty()) shopList[0] else "Main Shop")
            renderTable()

            saveBtn.setOnClickListener{ save() }
            histBtn.setOnClickListener{ history() }
            exportBtn.setOnClickListener{ exportPdf() }

            scroll.addView(root)
            setContentView(scroll)
        }catch(e:Exception){ setContentView(TextView(this).apply{ text="Error: ${e.message}" }) }
    }

    private fun loadShopList(){
        shopList.clear()
        try{
            val possibleBranchPrefs = listOf("branches","shops","all_shops","branches_db")
            for(pn in possibleBranchPrefs){
                try{
                    val sp = getSharedPreferences(pn, Context.MODE_PRIVATE)
                    for((_,v) in sp.all){
                        try{
                            val raw=v.toString()
                            if(raw.trim().startsWith("{")){
                                val o=JSONObject(raw)
                                val name=o.optString("name", o.optString("shopName",""))
                                if(name.isNotBlank() &&!shopList.contains(name)) shopList.add(name)
                            }
                        }catch(_:Exception){}
                    }
                }catch(_:Exception){}
            }
            val known = listOf("products_main","products_rukudzo")
            for(pn in known){
                try{
                    val sp=getSharedPreferences(pn, Context.MODE_PRIVATE)
                    if(sp.all.isNotEmpty()){
                        val displayName = if(pn=="products_main") "Main Shop" else "Rukudzo Shop"
                        if(!shopList.contains(displayName)) shopList.add(displayName)
                    }
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        if(shopList.isEmpty()) shopList.addAll(listOf("Main Shop","Rukudzo Shop"))
    }

    private fun prefNameForShop(shopName:String): List<String>{
        return if(shopName.lowercase().contains("rukudzo")) listOf("products_rukudzo","products_main")
        else listOf("products_main","products_rukudzo")
    }

    private fun loadProductsForShop(shopName:String){
        names.clear(); inHands.clear(); costs.clear(); sells.clear(); countedList.clear()
        val seen=HashSet<String>()
        for(pn in prefNameForShop(shopName)){
            try{
                val sp=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in sp.all){
                    if(seen.contains(k)) continue
                    try{
                        val raw=v.toString()
                        var n=k; var q=0; var c=0.0; var s=0.0
                        if(raw.trim().startsWith("{")){
                            val o=JSONObject(raw)
                            n=o.optString("name",k); q=o.optInt("quantity",0); c=o.optDouble("cost",0.0); s=o.optDouble("price",c)
                        } else {
                            val parts=raw.split("|")
                            n=parts.getOrNull(0)?:k; q=parts.getOrNull(1)?.toIntOrNull()?:0
                        }
                        if(n.isNotBlank()){ names.add(n); inHands.add(q); costs.add(c); sells.add(s); countedList.add(q); seen.add(k) }
                    }catch(_:Exception){}
                }
                if(names.isNotEmpty()) break
            }catch(_:Exception){}
        }
    }

    private fun renderTable(){
        tableLayout.removeAllViews()
        var totalVar=0
        var totalValCost=0.0
        for(i in names.indices){
            val variance = countedList[i] - inHands[i]
            totalVar+=variance
            totalValCost+=variance*costs[i]

            val isEven = i%2==0
            val row = LinearLayout(this).apply{
                orientation=LinearLayout.HORIZONTAL
                setBackgroundColor(if(isEven) Color.parseColor("#FFFFFF") else Color.parseColor("#F7F9FC"))
                setPadding(4,8,4,8)
                gravity=Gravity.CENTER_VERTICAL
            }

            // PRODUCT
            row.addView(TextView(this).apply{ text="${i+1}. ${names[i]}"; textSize=11f; setTextColor(Color.BLACK); maxLines=2 }, LinearLayout.LayoutParams(0,-2,2.5f).apply{ setMargins(4,0,4,0) })
            // IN HAND
            row.addView(TextView(this).apply{ text="${inHands[i]}"; textSize=11f; gravity=Gravity.CENTER; setTextColor(Color.BLACK); typeface=Typeface.DEFAULT_BOLD }, LinearLayout.LayoutParams(0,-2,0.8f))
            // COST
            row.addView(TextView(this).apply{ text="$${costs[i]}"; textSize=10f; gravity=Gravity.CENTER; setTextColor(Color.GRAY) }, LinearLayout.LayoutParams(0,-2,0.7f))
            // SELL
            row.addView(TextView(this).apply{ text="$${sells[i]}"; textSize=10f; gravity=Gravity.CENTER; setTextColor(Color.GRAY) }, LinearLayout.LayoutParams(0,-2,0.7f))
            // COUNTED EDITABLE
            val et = EditText(this).apply{
                setText(countedList[i].toString())
                textSize=11f
                gravity=Gravity.CENTER
                setBackgroundColor(Color.parseColor("#E8EDF5"))
                setPadding(4,4,4,4)
                val idx=i
                setOnFocusChangeListener{ _, hasFocus ->
                    if(!hasFocus){
                        try{ countedList[idx]=text.toString().toIntOrNull()?:countedList[idx]; renderTable() }catch(_:Exception){}
                    }
                }
            }
            row.addView(et, LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(2,0,2,0) })
            // VAR
            row.addView(TextView(this).apply{
                text="$variance"
                gravity=Gravity.CENTER
                textSize=11f
                typeface=Typeface.DEFAULT_BOLD
                setTextColor(if(variance==0) Color.parseColor("#2E7D32") else if(variance<0) Color.RED else Color.parseColor("#1565C0"))
            }, LinearLayout.LayoutParams(0,-2,0.6f))

            tableLayout.addView(row)
            // divider
            tableLayout.addView(android.view.View(this).apply{ setBackgroundColor(Color.parseColor("#E0E0E0")); layoutParams=LinearLayout.LayoutParams(-1,1) })
        }
        totalTxt.text = "$currentShop | Products:${names.size} | Total Var: $totalVar | Var Value: $${"%.2f".format(totalValCost)}"
        if(names.isEmpty()){
            tableLayout.addView(TextView(this).apply{ text="No products found in $currentShop\nGo to View Stock -> select $currentShop"; gravity=Gravity.CENTER; setPadding(20,40,20,40); setTextColor(Color.GRAY) })
        }
    }

    private fun save(){
        try{
            val sp=getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
            val id=System.currentTimeMillis().toString()
            val o=JSONObject()
            o.put("date", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(java.util.Date()))
            o.put("shop", currentShop); o.put("count", names.size)
            val arr=JSONArray(); for(i in names.indices){ val jo=JSONObject(); jo.put("name",names[i]); jo.put("inHand",inHands[i]); jo.put("counted",countedList[i]); arr.put(jo) }
            o.put("items",arr)
            sp.edit().putString("take_${currentShop}_$id", o.toString()).apply()
            Toast.makeText(this,"Saved for $currentShop!",Toast.LENGTH_LONG).show()
        }catch(e:Exception){ Toast.makeText(this,"Save fail ${e.message}",Toast.LENGTH_LONG).show() }
    }

    private fun history(){
        try{
            val sp=getSharedPreferences("stock_takes_db", Context.MODE_PRIVATE)
            val filtered = sp.all.entries.filter{ it.key.contains(currentShop, true) }
            if(filtered.isEmpty()){ Toast.makeText(this,"No history for $currentShop",Toast.LENGTH_SHORT).show(); return }
            val lay=LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(20,20,20,20) }
            lay.addView(TextView(this).apply{ text="HISTORY - $currentShop\n"; textSize=18f; typeface=Typeface.DEFAULT_BOLD })
            for((_,v) in filtered.sortedByDescending{it.key}.take(10)){
                try{ val o=JSONObject(v.toString()); lay.addView(TextView(this).apply{ text="${o.optString("date")} - ${o.optInt("count")} items\n"; setPadding(0,10,0,10) }) }catch(_:Exception){}
            }
            val sv=ScrollView(this); sv.addView(lay)
            android.app.AlertDialog.Builder(this).setView(sv).setPositiveButton("Close",null).show()
        }catch(e:Exception){}
    }

    private fun exportPdf(){
        try{
            if(names.isEmpty()){ Toast.makeText(this,"No data",Toast.LENGTH_SHORT).show(); return }
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595,842,1).create()
            var page = pdfDoc.startPage(pageInfo)
            var canvas = page.canvas
            var paint = Paint(); paint.textSize=12f
            var y=40f
            canvas.drawText("STOCK TAKE SHEET - $currentShop",40f,y,paint); y+=20f
            paint.textSize=9f
            for(i in names.indices){
                if(y>800){ pdfDoc.finishPage(page); val pi=PdfDocument.PageInfo.Builder(595,842,pdfDoc.pages.size+1).create(); page=pdfDoc.startPage(pi); canvas=page.canvas; y=40f }
                canvas.drawText("${i+1}. ${names[i].take(20)} In:${inHands[i]} Count:${countedList[i]} Var:${countedList[i]-inHands[i]} Cost:${costs[i]} Sell:${sells[i]}",40f,y,paint); y+=11f
            }
            pdfDoc.finishPage(page)
            val file = File(getExternalFilesDir(null), "StockTake_${currentShop}_${System.currentTimeMillis()}.pdf")
            pdfDoc.writeTo(FileOutputStream(file)); pdfDoc.close()
            Toast.makeText(this,"PDF Exported: ${file.name}",Toast.LENGTH_LONG).show()
        }catch(e:Exception){ Toast.makeText(this,"Export failed: ${e.message}",Toast.LENGTH_LONG).show() }
    }
}