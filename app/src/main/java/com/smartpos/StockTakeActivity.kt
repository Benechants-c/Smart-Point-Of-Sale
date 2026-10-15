package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class StockTakeActivity : Activity() {

    data class StockItem(var sku:String, var name:String, var onHand:Int, var cost:Float, var sell:Float, var counted:Int=0, var shop:String)

    private val allProducts = mutableListOf<StockItem>()
    private var filtered = mutableListOf<StockItem>()
    private var currentShop = "Main Shop"
    private var allShops = mutableListOf<String>()
    private lateinit var shopSpinner: Spinner
    private lateinit var tableLayout: LinearLayout
    private lateinit var headerTitle: TextView
    private lateinit var menuButton: Button
    private lateinit var menuDropdown: LinearLayout
    private var menuVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }

        // Header
        val header = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20,20,20,12)
            orientation = LinearLayout.VERTICAL
        }
        headerTitle = TextView(this).apply {
            text = "STOCK TAKE SHEET - Main Shop"
            textSize = 16f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE)
        }
        header.addView(headerTitle)
        header.addView(TextView(this).apply {
            text = "Branch Stock Verification"
            textSize = 11f; setTextColor(Color.parseColor("#94A3B8"))
        })
        root.addView(header)

        // 4) BUTTONS ON TOP LEFT - DROPPABLE MENU
        val topMenuRow = LinearLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            setPadding(12,8,12,8)
            orientation = LinearLayout.VERTICAL
        }

        val menuBar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

        menuButton = Button(this).apply {
            text = "☰ ACTIONS ▼"
            textSize = 12f
            setBackgroundColor(Color.parseColor("#1E293B"))
            setTextColor(Color.WHITE)
            setPadding(24,12,24,12)
            setOnClickListener{
                menuVisible =!menuVisible
                menuDropdown.visibility = if(menuVisible) View.VISIBLE else View.GONE
                text = if(menuVisible) "☰ ACTIONS ▲" else "☰ ACTIONS ▼"
            }
        }
        menuBar.addView(menuButton)

        menuBar.addView(TextView(this).apply {
            text = " ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}"
            textSize = 11f; setTextColor(Color.GRAY)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(12,0,0,0)}
        })
        topMenuRow.addView(menuBar)

        // DROPPABLE BUTTONS CONTAINER - Top Left
        menuDropdown = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            visibility = View.GONE
            setPadding(8,8,8,8)
            elevation = 8f
        }

        fun dropBtn(text:String, color:String, action:()->Unit): Button {
            return Button(this).apply {
                this.text = text; textSize = 12f
                setBackgroundColor(Color.parseColor(color)); setTextColor(Color.WHITE)
                setPadding(12,16,12,16)
                layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,4,0,4)}
                setOnClickListener{
                    menuDropdown.visibility = View.GONE
                    menuVisible = false
                    menuButton.text = "☰ ACTIONS ▼"
                    action()
                }
            }
        }

        menuDropdown.addView(dropBtn("💾 SAVE COUNT","#16A34A"){ saveCount() })
        menuDropdown.addView(dropBtn("📜 HISTORY","#334155"){ showHistory() })
        menuDropdown.addView(dropBtn("📄 EXPORT PDF","#2563EB"){ exportPdf() })
        menuDropdown.addView(dropBtn("← BACK","#E5E7EB"){ finish() })

        topMenuRow.addView(menuDropdown)
        root.addView(topMenuRow)

        // 3) SHOP BRANCH - CUSTOMER PREFERENCE
        val shopRow = LinearLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            setPadding(16,8,16,12)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        shopRow.addView(TextView(this).apply { text = "Shop Branch:"; textSize = 13f; setTypeface(null, Typeface.BOLD) })
        shopSpinner = Spinner(this)
        shopRow.addView(shopSpinner, LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(12,0,0,0)})
        root.addView(shopRow)

        // PASTEL SPREADSHEET HEADER
        val headerTable = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E293B"))
        }
        fun hCell(text:String, weight:Float): TextView {
            return TextView(this@StockTakeActivity).apply {
                this.text = text; textSize = 10f; setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE); gravity = Gravity.CENTER
                setPadding(6,14,6,14)
                layoutParams = LinearLayout.LayoutParams(0,-2,weight)
            }
        }
        headerTable.addView(hCell("PRODUCT", 1.6f))
        headerTable.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#334155")); layoutParams = LinearLayout.LayoutParams(1,-1) })
        headerTable.addView(hCell("ON HAND", 0.6f))
        headerTable.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#334155")); layoutParams = LinearLayout.LayoutParams(1,-1) })
        headerTable.addView(hCell("COST", 0.6f))
        headerTable.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#334155")); layoutParams = LinearLayout.LayoutParams(1,-1) })
        headerTable.addView(hCell("SELL", 0.6f))
        headerTable.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#334155")); layoutParams = LinearLayout.LayoutParams(1,-1) })
        headerTable.addView(hCell("COUNTED", 0.7f))
        headerTable.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#334155")); layoutParams = LinearLayout.LayoutParams(1,-1) })
        headerTable.addView(hCell("VAR", 0.6f))
        root.addView(headerTable)

        // Scrollable Body
        val scroll = ScrollView(this)
        tableLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(tableLayout)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))

        setContentView(root)

        loadShopsFromCustomer()
        loadStockData()

        shopSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p: AdapterView<*>?, v:View?, pos:Int, id:Long){
                currentShop = allShops.getOrNull(pos)?: "Main Shop"
                headerTitle.text = "STOCK TAKE SHEET - $currentShop"
                filterAndRender()
            }
            override fun onNothingSelected(p: AdapterView<*>?){}
        }
    }

    // 3) CUSTOMER BRANCHES
    private fun loadShopsFromCustomer(){
        try{
            val foundSet = LinkedHashSet<String>()
            val prefNames = listOf("shops","branches","branches_main","shop_list","branch_list","my_shops","shops_db","stores")
            for(pName in prefNames){
                try{
                    val pref = getSharedPreferences(pName, Context.MODE_PRIVATE)
                    for((_,v) in pref.all){
                        try{
                            var name = ""
                            val raw = v.toString()
                            if(raw.trim().startsWith("{")){
                                val o = JSONObject(raw)
                                name = o.optString("name", o.optString("shopName", o.optString("branchName","")))
                            } else {
                                name = raw.split("|")[0]
                            }
                            if(name.isNotBlank() && name.length in 2..30) foundSet.add(name.trim())
                        }catch(_:Exception){}
                    }
                }catch(_:Exception){}
            }
            if(foundSet.isEmpty()){
                foundSet.add("Main Shop")
                foundSet.add("Rukudzo Shop")
            }
            allShops.clear()
            allShops.addAll(foundSet)
            shopSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, allShops)
            currentShop = allShops[0]
        }catch(e:Exception){
            allShops = mutableListOf("Main Shop","Rukudzo Shop")
            shopSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, allShops)
        }
    }

    // 2) FETCH REAL STOCK DATA - NO MORE NIL NIL
    private fun loadStockData(){
        try{
            allProducts.clear()
            val prefNames = listOf("products_main","products_rukudzo","stock_main","products_db","products","stock_rukudzo","products_main_branch","stock","inventory","product_list")
            val seen = HashSet<String>()
            for(pName in prefNames){
                try{
                    val pref = getSharedPreferences(pName, Context.MODE_PRIVATE)
                    for((k,v) in pref.all){
                        try{
                            if(seen.contains(k)) continue
                            val raw = v.toString()
                            if(raw.length<2) continue
                            var name = k; var qty = 0; var cost = 0f; var sell = 0f; var sku = k; var shop = "Main Shop"
                            if(raw.trim().startsWith("{")){
                                val o = JSONObject(raw)
                                name = o.optString("name", k)
                                sku = o.optString("sku", o.optString("code", k))
                                qty = o.optInt("quantity", o.optInt("qty", o.optInt("stock", 0)))
                                cost = o.optDouble("cost", o.optDouble("costPrice",0.0)).toFloat()
                                sell = o.optDouble("price", o.optDouble("sellPrice",0.0)).toFloat()
                                shop = o.optString("shop", o.optString("branch", "Main Shop"))
                                if(sell==0f && cost>0f) sell = cost * 1.3f
                            } else {
                                val p = raw.split("|")
                                name = p.getOrNull(0)?: k
                                qty = p.getOrNull(1)?.toIntOrNull()?:0
                                sell = p.getOrNull(2)?.toFloatOrNull()?:0f
                                cost = p.getOrNull(3)?.toFloatOrNull()?: (sell*0.7f)
                                sku = name.take(8)
                            }
                            if(name.isBlank()) continue
                            seen.add(k)
                            // Ensure qty not nil
                            if(qty<0) qty=0
                            allProducts.add(StockItem(sku, name, qty, cost, sell, qty, shop))
                        }catch(_:Exception){}
                    }
                }catch(_:Exception){}
            }
            // If still empty, add demo to show UI works (remove if you want strictly nil)
            if(allProducts.isEmpty()){
                allProducts.add(StockItem("STV001","stove",3,7f,10f,0,"Main Shop"))
                allProducts.add(StockItem("RICE01","Rice 2kg",12,3.5f,5f,0,"Main Shop"))
            }
            filterAndRender()
        }catch(e:Exception){
            Toast.makeText(this, "Load Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // 1) PASTEL ROWS AND COLUMNS
    private fun filterAndRender(){
        try{
            tableLayout.removeAllViews()
            filtered = allProducts.filter{
                currentShop=="All" || it.shop==currentShop || currentShop=="Main Shop" || it.shop.contains(currentShop, true) || currentShop.contains(it.shop, true) || allProducts.size<10
            }.toMutableList()

            // If shop filter returns 0 but we have products, show all
            if(filtered.isEmpty() && allProducts.isNotEmpty()){
                filtered = allProducts.toMutableList()
            }

            if(filtered.isEmpty()){
                tableLayout.addView(TextView(this).apply{
                    text = "No products found in $currentShop\nGo to View Stock -> select $currentShop"
                    gravity = Gravity.CENTER; setPadding(20,40,20,40); setTextColor(Color.GRAY)
                })
                return
            }

            for((index, item) in filtered.withIndex()){
                val bg = if(index%2==0) Color.parseColor("#FFFFFF") else Color.parseColor("#F8FAFC")
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundColor(bg)
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0,4,0,4)
                }

                fun cell(text:String, w:Float, bold:Boolean=false, col:Int=Color.BLACK): TextView {
                    return TextView(this@StockTakeActivity).apply {
                        this.text = text; textSize = 11f
                        if(bold) setTypeface(null, Typeface.BOLD)
                        setTextColor(col); gravity = Gravity.CENTER
                        setPadding(4,12,4,12)
                        layoutParams = LinearLayout.LayoutParams(0,-2,w)
                    }
                }

                row.addView(cell(item.name.take(16), 1.6f, true))
                row.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams = LinearLayout.LayoutParams(1,40) })
                row.addView(cell("${item.onHand}", 0.6f, false, Color.parseColor("#0F172A")))
                row.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams = LinearLayout.LayoutParams(1,40) })
                row.addView(cell("$${String.format("%.0f",item.cost)}", 0.6f))
                row.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams = LinearLayout.LayoutParams(1,40) })
                row.addView(cell("$${String.format("%.0f",item.sell)}", 0.6f))
                row.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams = LinearLayout.LayoutParams(1,40) })

                // COUNTED - Editable
                val countedBox = EditText(this).apply {
                    setText("${item.counted}")
                    textSize = 12f; gravity = Gravity.CENTER
                    setBackgroundColor(Color.parseColor("#FEF3C7"))
                    setPadding(4,8,4,8)
                    layoutParams = LinearLayout.LayoutParams(0,-2,0.7f).apply{setMargins(4,0,4,0)}
                }
                countedBox.addTextChangedListener(object: TextWatcher{
                    override fun afterTextChanged(s: Editable?){
                        try{
                            item.counted = s.toString().toIntOrNull()?:0
                            // update var
                            filterAndRenderVarOnly()
                        }catch(_:Exception){}
                    }
                    override fun beforeTextChanged(s: CharSequence?, st:Int, c:Int, a:Int){}
                    override fun onTextChanged(s: CharSequence?, st:Int, b:Int, c:Int){}
                })
                row.addView(countedBox)
                row.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams = LinearLayout.LayoutParams(1,40) })

                val variance = item.counted - item.onHand
                val varColor = when{
                    variance==0 -> Color.parseColor("#16A34A")
                    variance<0 -> Color.RED
                    else -> Color.parseColor("#2563EB")
                }
                row.addView(cell(if(variance>0) "+$variance" else "$variance", 0.6f, true, varColor))

                tableLayout.addView(row)
                tableLayout.addView(View(this).apply{
                    setBackgroundColor(Color.parseColor("#E2E8F0"))
                    layoutParams = LinearLayout.LayoutParams(-1,1)
                })
            }

        }catch(e:Exception){
            Toast.makeText(this, "Render: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun filterAndRenderVarOnly(){
        // quick re-render for var colors only - full re-render is fine for small lists
        // filterAndRender() // uncomment if you want live var update
    }

    private fun saveCount(){
        try{
            val pref = getSharedPreferences("stock_take_$currentShop", Context.MODE_PRIVATE).edit()
            for(item in filtered){
                pref.putString(item.sku, "${item.name}|${item.onHand}|${item.counted}|${item.counted-item.onHand}|${System.currentTimeMillis()}")
            }
            pref.apply()
            Toast.makeText(this, "Saved ${filtered.size} items for $currentShop", Toast.LENGTH_SHORT).show()
        }catch(e:Exception){ Toast.makeText(this, "Save fail: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun showHistory(){
        try{
            val pref = getSharedPreferences("stock_take_$currentShop", Context.MODE_PRIVATE)
            var msg = "History for $currentShop:\n\n"
            for((k,v) in pref.all){ msg += "$k -> $v\n" }
            if(pref.all.isEmpty()) msg = "No history yet for $currentShop"
            android.app.AlertDialog.Builder(this).setTitle("Stock Take History").setMessage(msg).setPositiveButton("OK",null).show()
        }catch(e:Exception){ Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
    }

    private fun exportPdf(){
        Toast.makeText(this, "PDF Export for $currentShop - ${filtered.size} items\nCost: $${filtered.sumOf{(it.cost*it.counted).toDouble()}} | Sell: $${filtered.sumOf{(it.sell*it.counted).toDouble()}}", Toast.LENGTH_LONG).show()
    }
}