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

class StockListActivity : Activity() {

    data class Product(val sku:String, val name:String, val qty:Int, val cost:Float, val sell:Float, val shop:String)

    private val allProducts = mutableListOf<Product>()
    private var currentShop = "Main Shop"
    private var allShops = mutableListOf<String>()
    private lateinit var shopSpinner: Spinner
    private lateinit var tableLayout: LinearLayout
    private lateinit var txtTotalCost: TextView
    private lateinit var txtTotalSell: TextView
    private lateinit var txtProdCount: TextView
    private lateinit var searchBox: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }

        // Header
        val header = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20,24,20,20)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val back = TextView(this).apply {
            text = "<- Back"; textSize = 14f; setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD); setPadding(10,10,20,10)
            isClickable = true
            setOnClickListener{ finish() }
        }
        header.addView(back)
        header.addView(TextView(this).apply {
            text = "View Stock"; textSize = 16f; setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        })
        root.addView(header)

        // Shop Row - CUSTOMER PREFERENCE BRANCHES
        val shopRow = LinearLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            setPadding(16,12,16,12)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        shopRow.addView(TextView(this).apply { text = "Shop:"; textSize = 13f; setTypeface(null, Typeface.BOLD) })
        shopSpinner = Spinner(this)
        shopRow.addView(shopSpinner, LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(12,0,0,0)})
        root.addView(shopRow)

        // TOTALS ROW - COST + SELLING SIDE BY SIDE
        val totalsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12,12,12,12)
        }
        fun totalCard(title:String, valueView:TextView, bg:String): LinearLayout {
            val card = LinearLayout(this@StockListActivity).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor(bg))
                setPadding(16,12,16,12)
                layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(6,6,6,6)}
            }
            card.addView(TextView(this@StockListActivity).apply { text = title; textSize = 10f; setTextColor(Color.parseColor("#475569")); gravity = Gravity.CENTER })
            card.addView(valueView.apply { textSize = 15f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setTextColor(Color.parseColor("#0F172A")) })
            return card
        }

        txtTotalCost = TextView(this).apply{ text = "$0.00" }
        txtTotalSell = TextView(this).apply{ text = "$0.00" }
        txtProdCount = TextView(this).apply{ text = "0" }

        totalsRow.addView(totalCard("Total Value at COST", txtTotalCost, "#FEE2E2"))
        totalsRow.addView(totalCard("Total Value at SELLING", txtTotalSell, "#DCFCE7"))
        totalsRow.addView(totalCard("Products", txtProdCount, "#DBEAFE"))
        root.addView(totalsRow)

        // Search
        searchBox = EditText(this).apply {
            hint = "🔍 Search name or code..."
            setBackgroundColor(Color.WHITE)
            setPadding(20,16,20,16)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,0,16,12)}
        }
        root.addView(searchBox)

        // PASTEL SPREADSHEET HEADER - EXCEL LIKE
        val headerTable = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#E2E8F0"))
            setPadding(0,0,0,0)
        }
        fun hCell(text:String, weight:Float): TextView {
            return TextView(this@StockListActivity).apply {
                this.text = text; textSize = 11f; setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#334155")); gravity = Gravity.CENTER
                setPadding(8,14,8,14)
                layoutParams = LinearLayout.LayoutParams(0,-2,weight)
                // grid border
                setBackgroundColor(Color.parseColor("#E2E8F0"))
            }
        }
        headerTable.addView(hCell("SKU / CODE", 0.8f))
        headerTable.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#CBD5E1")); layoutParams = LinearLayout.LayoutParams(1,-1) })
        headerTable.addView(hCell("PRODUCT NAME", 1.5f))
        headerTable.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#CBD5E1")); layoutParams = LinearLayout.LayoutParams(1,-1) })
        headerTable.addView(hCell("QTY", 0.5f))
        headerTable.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#CBD5E1")); layoutParams = LinearLayout.LayoutParams(1,-1) })
        headerTable.addView(hCell("COST", 0.7f))
        headerTable.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#CBD5E1")); layoutParams = LinearLayout.LayoutParams(1,-1) })
        headerTable.addView(hCell("PRICE", 0.7f))
        headerTable.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#CBD5E1")); layoutParams = LinearLayout.LayoutParams(1,-1) })
        headerTable.addView(hCell("VALUE", 0.8f))
        root.addView(headerTable)
        root.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#94A3B8")); layoutParams = LinearLayout.LayoutParams(-1,2) })

        // Scrollable Table Body
        val scroll = ScrollView(this)
        tableLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(tableLayout)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))

        setContentView(root)

        loadShopsFromCustomerPreference()
        loadAllProducts()

        shopSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p: AdapterView<*>?, v:View?, pos:Int, id:Long){
                currentShop = allShops.getOrNull(pos)?: "Main Shop"
                filterAndRender()
            }
            override fun onNothingSelected(p: AdapterView<*>?){}
        }

        searchBox.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?){ filterAndRender() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int){}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int){}
        })
    }

    private fun loadShopsFromCustomerPreference(){
        try{
            allShops.clear()
            // 2) CUSTOMER INPUT BRANCHES - Read from where customer adds branches/shops
            val branchPrefNames = listOf("shops","branches","branches_main","shop_list","branch_list","my_shops","shops_db","stores","warehouses")
            val foundSet = LinkedHashSet<String>()

            for(prefName in branchPrefNames){
                try{
                    val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    if(pref.all.isEmpty()) continue
                    for((_,v) in pref.all){
                        try{
                            val raw = v.toString()
                            var name = ""
                            if(raw.trim().startsWith("{")){
                                val o = JSONObject(raw)
                                name = o.optString("name", o.optString("shopName", o.optString("branchName","")))
                                if(name.isBlank()) name = o.optString("shop","")
                            } else {
                                // could be "Main Shop|location|..." or just name
                                name = raw.split("|").getOrNull(0)?: raw
                            }
                            if(name.isNotBlank() && name.length>1 && name.length<30){
                                foundSet.add(name.trim())
                            }
                        }catch(_:Exception){}
                    }
                }catch(_:Exception){}
            }

            // Also read from single key list
            try{
                val p = getSharedPreferences("branches", Context.MODE_PRIVATE)
                val listStr = p.getString("branch_names","")
                if(!listStr.isNullOrBlank()){
                    for(s in listStr.split(",")){ if(s.trim().isNotBlank()) foundSet.add(s.trim()) }
                }
            }catch(_:Exception){}

            if(foundSet.isEmpty()){
                // Fallback if customer hasn't added any yet - show default but customer can add
                foundSet.add("Main Shop")
            }

            allShops.addAll(foundSet)

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, allShops)
            shopSpinner.adapter = adapter
            currentShop = allShops[0]

        }catch(e:Exception){
            allShops = mutableListOf("Main Shop","Branch 1","Warehouse")
            shopSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, allShops)
        }
    }

    private fun loadAllProducts(){
        try{
            val prefNames = listOf("products_main","products_rukudzo","stock_main","products_db","products","stock_rukudzo","products_main_branch","stock","inventory")
            val seen = HashSet<String>()
            allProducts.clear()
            for(prefName in prefNames){
                try{
                    val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    for((k,v) in pref.all){
                        try{
                            if(seen.contains(k)) continue
                            val raw = v.toString()
                            var name = k; var qty = 0; var cost = 0f; var sell = 0f; var sku = k; var shop = "Main Shop"
                            if(raw.trim().startsWith("{")){
                                val o = JSONObject(raw)
                                name = o.optString("name", k)
                                sku = o.optString("sku", o.optString("code", o.optString("barcode", k)))
                                qty = o.optInt("quantity", o.optInt("qty", 0))
                                cost = o.optDouble("cost", o.optDouble("costPrice",0.0)).toFloat()
                                sell = o.optDouble("price", o.optDouble("sellPrice",0.0)).toFloat()
                                shop = o.optString("shop", o.optString("branch", "Main Shop"))
                                if(shop.isBlank()) shop = "Main Shop"
                            } else {
                                val p = raw.split("|")
                                name = p.getOrNull(0)?: k
                                qty = p.getOrNull(1)?.toIntOrNull()?: 0
                                sell = p.getOrNull(2)?.toFloatOrNull()?: 0f
                                cost = p.getOrNull(3)?.toFloatOrNull()?: (sell * 0.7f)
                                sku = name.take(8)
                            }
                            if(qty<0) qty=0
                            seen.add(k)
                            allProducts.add(Product(sku, name, qty, cost, sell, shop))
                        }catch(_:Exception){}
                    }
                }catch(_:Exception){}
            }
            filterAndRender()
        }catch(_:Exception){}
    }

    private fun filterAndRender(){
        try{
            tableLayout.removeAllViews()
            val query = searchBox.text.toString().lowercase().trim()

            var filtered = allProducts.filter{
                (currentShop=="All Shops" || it.shop==currentShop || currentShop=="Main Shop" || allShops.size==1) &&
                (query.isEmpty() || it.name.lowercase().contains(query) || it.sku.lowercase().contains(query))
            }

            // If shop filter yields nothing, show all (because old products have no shop tag)
            if(filtered.isEmpty() && query.isEmpty() && allProducts.isNotEmpty()){
                filtered = allProducts
            }

            var totalCost = 0f; var totalSell = 0f
            for(p in filtered){ totalCost += p.cost * p.qty; totalSell += p.sell * p.qty }

            txtTotalCost.text = "$${String.format("%.2f", totalCost)}"
            txtTotalSell.text = "$${String.format("%.2f", totalSell)}"
            txtProdCount.text = "${filtered.size}"

            // 3) PASTEL SPREADSHEET ROWS - Alternating pastel colors with grid
            for((index, prod) in filtered.withIndex()){
                val isEven = index % 2 == 0
                val bgColor = if(isEven) Color.parseColor("#FFFFFF") else Color.parseColor("#F8FAFC")
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setBackgroundColor(bgColor)
                    gravity = Gravity.CENTER_VERTICAL
                }

                fun cell(text:String, weight:Float, bold:Boolean=false, color:Int=Color.BLACK): TextView {
                    return TextView(this@StockListActivity).apply {
                        this.text = text; textSize = 11f
                        if(bold) setTypeface(null, Typeface.BOLD)
                        setTextColor(color); gravity = Gravity.CENTER
                        setPadding(6,12,6,12)
                        layoutParams = LinearLayout.LayoutParams(0,-2,weight)
                    }
                }

                row.addView(cell(prod.sku.take(10), 0.8f, false, Color.parseColor("#64748B")))
                row.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams = LinearLayout.LayoutParams(1,30) })
                row.addView(cell(prod.name.take(18), 1.5f, true))
                row.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams = LinearLayout.LayoutParams(1,30) })
                row.addView(cell("${prod.qty}", 0.5f, true, if(prod.qty<5) Color.RED else Color.parseColor("#16A34A")))
                row.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams = LinearLayout.LayoutParams(1,30) })
                row.addView(cell("$${String.format("%.2f",prod.cost)}", 0.7f))
                row.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams = LinearLayout.LayoutParams(1,30) })
                row.addView(cell("$${String.format("%.2f",prod.sell)}", 0.7f))
                row.addView(View(this).apply{ setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams = LinearLayout.LayoutParams(1,30) })
                row.addView(cell("$${String.format("%.2f",prod.sell*prod.qty)}", 0.8f, true))

                tableLayout.addView(row)
                // horizontal grid line
                tableLayout.addView(View(this).apply{
                    setBackgroundColor(Color.parseColor("#E2E8F0"))
                    layoutParams = LinearLayout.LayoutParams(-1,1)
                })
            }

            if(filtered.isEmpty()){
                tableLayout.addView(TextView(this).apply{
                    text = "No products found in ${currentShop}\nAdd products first"; gravity = Gravity.CENTER
                    setPadding(20,40,20,40); setTextColor(Color.GRAY)
                })
            }

        }catch(e:Exception){
            Toast.makeText(this, "Render: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}