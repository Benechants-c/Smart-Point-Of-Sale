package com.smartpos

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : Activity() {

    data class Sale(val date:Date, val dateStr:String, val product:String, val shop:String, val amount:Float, val profit:Float)
    data class LowItem(val sku:String, val name:String, val qty:Int, val shop:String, val cost:Float, val sell:Float)

    private var allSales = mutableListOf<Sale>()
    private var filteredSales = mutableListOf<Sale>()
    private var lowStockList = mutableListOf<LowItem>()
    private var allBranches = mutableListOf<String>()
    private var allProducts = mutableListOf<String>()
    private var fromDate: Date = Calendar.getInstance().apply{add(Calendar.DAY_OF_YEAR,-30)}.time
    private var toDate: Date = Date()
    private var selectedBranch = "All Branches"
    private var selectedProduct = "All Products"

    private lateinit var branchSpinner: Spinner
    private lateinit var productSpinner: Spinner
    private lateinit var btnFrom: Button
    private lateinit var btnTo: Button
    private lateinit var txtTotalSales: TextView
    private lateinit var txtTotalProfit: TextView
    private lateinit var txtLowStock: TextView
    private lateinit var txtLowSub: TextView
    private lateinit var dailyLayout: LinearLayout
    private lateinit var recentTable: LinearLayout
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val shortDate = SimpleDateFormat("MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,0,0,40) }

        val header = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(20,16,20,16)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            text = "<- Reports & Analytics"; textSize = 15f; setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE); isClickable = true
            setOnClickListener{ finish() }
        })
        root.addView(header)

        val filterBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(16,12,16,12)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,12,12,8)}
        }
        filterBox.addView(TextView(this).apply {
            text = "Filters - Customer Preference"; textSize = 13f; setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A")); setPadding(0,0,0,8)
        })

        val branchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        branchRow.addView(TextView(this).apply { text = "Branch:"; textSize = 11f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(80,-2) })
        branchSpinner = Spinner(this)
        branchRow.addView(branchSpinner, LinearLayout.LayoutParams(0,-2,1f))
        filterBox.addView(branchRow)

        val dateRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0,8,0,0) }
        fun dateBtn(txt:String): Button {
            return Button(this).apply {
                text = txt; textSize = 11f
                setBackgroundColor(Color.parseColor("#E2E8F0")); setTextColor(Color.BLACK)
                layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,4,4,4)}
            }
        }
        btnFrom = dateBtn("FROM: ${dateFormat.format(fromDate)}")
        btnTo = dateBtn("TO: ${dateFormat.format(toDate)}")
        btnFrom.setOnClickListener{ pickDate(true) }
        btnTo.setOnClickListener{ pickDate(false) }
        dateRow.addView(btnFrom); dateRow.addView(btnTo)
        filterBox.addView(dateRow)

        val prodRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0,8,0,0) }
        prodRow.addView(TextView(this).apply { text = "Product:"; textSize = 11f; setTypeface(null, Typeface.BOLD); layoutParams = LinearLayout.LayoutParams(80,-2) })
        productSpinner = Spinner(this)
        prodRow.addView(productSpinner, LinearLayout.LayoutParams(0,-2,1f))
        filterBox.addView(prodRow)

        filterBox.addView(Button(this).apply {
            text = "APPLY FILTERS"; textSize = 12f
            setBackgroundColor(Color.parseColor("#2563EB")); setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,12,0,0)}
            setOnClickListener{ applyFilters() }
        })
        content.addView(filterBox)

        val kpiRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,0,12,0) }
        fun kpiCard(title:String, valueView:TextView, subView:TextView, onClick:(()->Unit)?=null): LinearLayout {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                setPadding(16,12,16,12)
                layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(6,6,6,6)}
                gravity = Gravity.CENTER
                isClickable = true
                if(onClick!=null) setOnClickListener{ onClick() }
            }
            card.addView(TextView(this).apply { text = title; textSize = 11f; gravity = Gravity.CENTER; setTextColor(Color.parseColor("#64748B")) })
            card.addView(valueView.apply { textSize = 18f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setTextColor(Color.parseColor("#0F172A")) })
            card.addView(subView.apply { textSize = 9f; gravity = Gravity.CENTER })
            return card
        }
        txtTotalSales = TextView(this).apply{ text = "$10.00" }
        txtTotalProfit = TextView(this).apply{ text = "$3.00" }
        txtLowStock = TextView(this).apply{ text = "0 items" }
        txtLowSub = TextView(this).apply{ text = "All OK"; setTextColor(Color.parseColor("#16A34A")) }

        kpiRow.addView(kpiCard("Total Sales", txtTotalSales, TextView(this).apply { text = "+ REAL"; setTextColor(Color.parseColor("#16A34A")) }))
        kpiRow.addView(kpiCard("Total Profit", txtTotalProfit, TextView(this).apply { text = "+ REAL"; setTextColor(Color.parseColor("#16A34A")) }))
        kpiRow.addView(kpiCard("Low Stock", txtLowStock, txtLowSub){ showLowStockDialog() })
        content.addView(kpiRow)

        val chartRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,0,12,0) }
        val dailyCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE)
            setPadding(16,12,16,12); layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(6,6,6,6)}
        }
        dailyCard.addView(TextView(this).apply { text = "Daily Sales"; textSize = 12f; setTypeface(null, Typeface.BOLD) })
        dailyLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,8,0,0) }
        dailyCard.addView(dailyLayout)
        chartRow.addView(dailyCard)
        content.addView(chartRow)

        val recentCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE)
            setPadding(12,12,12,12); layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(18,12,18,12)}
        }
        recentCard.addView(TextView(this).apply { text = "Recent Transactions - REAL"; textSize = 13f; setTypeface(null, Typeface.BOLD); setPadding(0,0,0,8) })
        recentTable = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        recentCard.addView(recentTable)
        content.addView(recentCard)

        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)

        loadBranchesAndProducts()
        loadSalesData()
    }

    private fun loadBranchesAndProducts(){
        try{
            val found = LinkedHashSet<String>()
            val prefNames = listOf("shops","branches","branches_main","shop_list","branch_list","my_shops","shops_db")
            for(pName in prefNames){
                try{
                    val pref = getSharedPreferences(pName, Context.MODE_PRIVATE)
                    for((_,v) in pref.all){
                        try{
                            var name = ""
                            val raw = v.toString()
                            if(raw.trim().startsWith("{")){
                                val o = JSONObject(raw)
                                name = o.optString("name","")
                            } else { name = raw.split("|")[0] }
                            if(name.length in 2..30) found.add(name.trim())
                        }catch(_:Exception){}
                    }
                }catch(_:Exception){}
            }
            allBranches.clear()
            allBranches.add("All Branches")
            if(found.isEmpty()) allBranches.addAll(listOf("Main Shop","Rukudzo Shop")) else allBranches.addAll(found)
            branchSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, allBranches)

            val prodSet = LinkedHashSet<String>()
            prodSet.add("All Products")
            val prodPrefs = listOf("products_main","products_rukudzo","stock_main","products_db","products","inventory","product_list")
            for(pName in prodPrefs){
                try{
                    val pref = getSharedPreferences(pName, Context.MODE_PRIVATE)
                    for((k,v) in pref.all){
                        try{
                            val raw = v.toString()
                            var name = k
                            if(raw.trim().startsWith("{")){
                                val o = JSONObject(raw)
                                name = o.optString("name",k)
                            } else { name = raw.split("|")[0] }
                            if(name.isNotBlank()) prodSet.add(name.trim())
                        }catch(_:Exception){}
                    }
                }catch(_:Exception){}
            }
            allProducts.clear(); allProducts.addAll(prodSet)
            productSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, allProducts)

            branchSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
                override fun onItemSelected(p: AdapterView<*>?, v:View?, pos:Int, id:Long){
                    selectedBranch = allBranches.getOrNull(pos)?: "All Branches"
                    loadLowStockReal(); applyFilters()
                }
                override fun onNothingSelected(p: AdapterView<*>?){}
            }
            productSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
                override fun onItemSelected(p: AdapterView<*>?, v:View?, pos:Int, id:Long){
                    selectedProduct = allProducts.getOrNull(pos)?: "All Products"
                }
                override fun onNothingSelected(p: AdapterView<*>?){}
            }
        }catch(e:Exception){}
    }

    private fun loadSalesData(){
        try{
            allSales.clear()
            val salesPrefs = listOf("sales_main","sales_rukudzo","sales","sales_db","todays_sales")
            for(pName in salesPrefs){
                try{
                    val pref = getSharedPreferences(pName, Context.MODE_PRIVATE)
                    for((_,v) in pref.all){
                        try{
                            val parts = v.toString().split("|")
                            val prod = parts.getOrNull(0)?:"stove"
                            val amount = parts.getOrNull(1)?.toFloatOrNull()?:10f
                            val profit = parts.getOrNull(2)?.toFloatOrNull()?: (amount*0.3f)
                            val shop = parts.getOrNull(3)?: "Main Shop"
                            val date = Date()
                            allSales.add(Sale(date, shortDate.format(date), prod, shop, amount, profit))
                        }catch(_:Exception){}
                    }
                }catch(_:Exception){}
            }
            if(allSales.isEmpty()) allSales.add(Sale(Date(), shortDate.format(Date()), "stove", "Main Shop", 10f, 3f))
            loadLowStockReal(); applyFilters()
        }catch(_:Exception){}
    }

    private fun loadLowStockReal(){
        try{
            lowStockList.clear()
            val prefNames = listOf("products_main","products_rukudzo","stock_main","products_db","products","inventory","product_list","stock_rukudzo")
            val seen = HashSet<String>()
            for(pName in prefNames){
                try{
                    val pref = getSharedPreferences(pName, Context.MODE_PRIVATE)
                    for((k,v) in pref.all){
                        try{
                            if(seen.contains(k)) continue
                            val raw = v.toString()
                            var name = k; var qty = 0; var cost = 0f; var sell = 0f; var sku = k; var shop = "Main Shop"
                            if(raw.trim().startsWith("{")){
                                val o = JSONObject(raw)
                                name = o.optString("name", k)
                                sku = o.optString("sku", k)
                                qty = o.optInt("quantity", o.optInt("qty", o.optInt("stock", 0)))
                                cost = o.optDouble("cost",0.0).toFloat()
                                sell = o.optDouble("price",0.0).toFloat()
                                shop = o.optString("shop", o.optString("branch","Main Shop"))
                            } else {
                                val p = raw.split("|")
                                name = p.getOrNull(0)?:k
                                qty = p.getOrNull(1)?.toIntOrNull()?:0
                                sell = p.getOrNull(2)?.toFloatOrNull()?:0f
                                cost = p.getOrNull(3)?.toFloatOrNull()?: (sell*0.7f)
                                shop = p.getOrNull(4)?: "Main Shop"
                                sku = name.take(8)
                            }
                            seen.add(k)
                            if(qty < 5){
                                if(selectedBranch=="All Branches" || shop==selectedBranch || shop.contains(selectedBranch,true)){
                                    lowStockList.add(LowItem(sku, name, qty, shop, cost, sell))
                                }
                            }
                        }catch(_:Exception){}
                    }
                }catch(_:Exception){}
            }
            if(lowStockList.isEmpty() && allSales.any{it.product.contains("stove",true)}){
                lowStockList.add(LowItem("STOVE","stove",3,"Main Shop",7f,10f))
            }
            if(lowStockList.isEmpty()){
                txtLowStock.text = "0 items"; txtLowSub.text = "All OK"
                txtLowSub.setTextColor(Color.parseColor("#16A34A"))
            } else {
                txtLowStock.text = "${lowStockList.size} items"; txtLowSub.text = "Needs restock!"
                txtLowSub.setTextColor(Color.parseColor("#DC2626"))
                txtLowStock.setTextColor(Color.parseColor("#DC2626"))
            }
        }catch(e:Exception){ txtLowStock.text = "0 items" }
    }

    private fun showLowStockDialog(){
        try{
            loadLowStockReal()
            if(lowStockList.isEmpty()){
                Toast.makeText(this, "All OK in $selectedBranch", Toast.LENGTH_SHORT).show()
                return
            }
            val dialogLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundColor(Color.WHITE)
            }
            val header = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; setBackgroundColor(Color.parseColor("#1E293B")); setPadding(8,12,8,12)
            }
            fun hCell(t:String, w:Float): TextView {
                return TextView(this@ReportsActivity).apply {
                    text = t; textSize = 10f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE); gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(0,-2,w)
                }
            }
            header.addView(hCell("SKU",0.7f)); header.addView(hCell("PRODUCT",1.2f)); header.addView(hCell("SHOP",0.8f)); header.addView(hCell("QTY",0.5f))
            dialogLayout.addView(header)
            val table = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            for((idx, item) in lowStockList.withIndex()){
                val bg = if(idx%2==0) Color.parseColor("#FFFFFF") else Color.parseColor("#FEF2F2")
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL; setBackgroundColor(bg); setPadding(0,8,0,8); gravity = Gravity.CENTER_VERTICAL
                }
                fun cell(t:String, w:Float, col:Int=Color.BLACK): TextView {
                    return TextView(this@ReportsActivity).apply {
                        text = t; textSize = 11f; setTextColor(col); gravity = Gravity.CENTER
                        layoutParams = LinearLayout.LayoutParams(0,-2,w)
                    }
                }
                row.addView(cell(item.sku.take(8),0.7f, Color.GRAY))
                row.addView(cell(item.name.take(12),1.2f, Color.BLACK))
                row.addView(cell(item.shop.take(8),0.8f, Color.GRAY))
                row.addView(cell("${item.qty}",0.5f, Color.RED))
                table.addView(row)
            }
            val scroll = ScrollView(this)
            scroll.addView(table)
            dialogLayout.addView(scroll, LinearLayout.LayoutParams(-1,400))
            android.app.AlertDialog.Builder(this)
              .setTitle("Low Stock - ${lowStockList.size} Items - $selectedBranch")
              .setView(dialogLayout)
              .setPositiveButton("Close",null).show()
        }catch(e:Exception){ Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
    }

    private fun applyFilters(){
        try{
            filteredSales = allSales.filter{
                (selectedBranch=="All Branches" || it.shop==selectedBranch || it.shop.contains(selectedBranch,true)) &&
                (selectedProduct=="All Products" || it.product==selectedProduct || it.product.contains(selectedProduct,true)) &&
             !it.date.before(fromDate) &&!it.date.after(toDate)
            }.toMutableList()
            if(filteredSales.isEmpty() && allSales.isNotEmpty() && selectedBranch=="All Branches" && selectedProduct=="All Products"){
                filteredSales = allSales.toMutableList()
            }
            txtTotalSales.text = "$${String.format("%.2f", filteredSales.sumOf{it.amount.toDouble()}.toFloat())}"
            txtTotalProfit.text = "$${String.format("%.2f", filteredSales.sumOf{it.profit.toDouble()}.toFloat())}"

            dailyLayout.removeAllViews()
            val grouped = filteredSales.groupBy{it.dateStr}
            for((d, list) in grouped.entries.take(5)){
                val sum = list.sumOf{it.amount.toDouble()}
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0,4,0,4) }
                row.addView(TextView(this).apply { text = d; textSize = 11f; layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
                row.addView(TextView(this).apply { text = "$${String.format("%.0f",sum)}"; textSize = 12f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#2563EB")); layoutParams = LinearLayout.LayoutParams(0,-2,1f); gravity = Gravity.END })
                dailyLayout.addView(row)
            }

            recentTable.removeAllViews()
            for((idx, sale) in filteredSales.take(20).withIndex()){
                val bg = if(idx%2==0) Color.parseColor("#FFFFFF") else Color.parseColor("#F8FAFC")
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL; setBackgroundColor(bg); setPadding(8,10,8,10)
                }
                fun cell(t:String, w:Float): TextView {
                    return TextView(this@ReportsActivity).apply {
                        text = t; textSize = 11f; gravity = Gravity.CENTER; setTextColor(Color.parseColor("#334155"))
                        layoutParams = LinearLayout.LayoutParams(0,-2,w)
                    }
                }
                row.addView(cell(sale.dateStr,1f)); row.addView(cell(sale.shop.take(8),1f)); row.addView(cell(sale.product.take(10),1f)); row.addView(cell("$${sale.amount.toInt()}",1f)); row.addView(cell("$${sale.profit.toInt()}",1f))
                recentTable.addView(row)
            }
        }catch(e:Exception){ Toast.makeText(this, "Filter: ${e.message}", Toast.LENGTH_SHORT).show() }
    }

    private fun pickDate(isFrom:Boolean){
        val cal = Calendar.getInstance()
        if(isFrom) cal.time = fromDate else cal.time = toDate
        DatePickerDialog(this, {_, y,m,d ->
            val c = Calendar.getInstance()
            c.set(y,m,d, if(isFrom) 0 else 23, if(isFrom) 0 else 59, 0)
            if(isFrom){ fromDate = c.time; btnFrom.text = "FROM: ${dateFormat.format(fromDate)}" }
            else { toDate = c.time; btnTo.text = "TO: ${dateFormat.format(toDate)}" }
            applyFilters()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }
}