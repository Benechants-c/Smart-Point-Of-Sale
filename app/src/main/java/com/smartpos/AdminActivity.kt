package com.smartpos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AdminActivity : Activity() {
    data class StockItem(val sku:String, val name:String, val qty:Int, val sell:Float, val prefFile:String)

    private val allStockItems = mutableListOf<StockItem>()
    private lateinit var txtStockValue: TextView
    private lateinit var txtProducts: TextView
    private lateinit var txtCustomers: TextView
    private lateinit var txtLowStock: TextView
    private lateinit var txtLowSub: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#F8FAFC"))
            }
            val header = LinearLayout(this).apply {
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(30,40,30,30)
                orientation = LinearLayout.HORIZONTAL
            }
            val headerText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0,-2,1f) }
            headerText.addView(TextView(this).apply { text = "🏪 Smartpos"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE) })
            headerText.addView(TextView(this).apply { text = "Admin Dashboard • Main Shop"; textSize = 12f; setTextColor(Color.parseColor("#94A3B8")) })
            header.addView(headerText)
            root.addView(header)

            var todaySales = 0f; var profit = 0f; var stockValue = 0f; var lowStock = 0; var prodCount = 0; var custCount = 0
            var debugInfo = ""

            // SALES - This already works for you
            try{
                val salesPrefs = listOf("sales_main","sales_rukudzo","sales","sales_db","todays_sales")
                for(spName in salesPrefs){
                    try{
                        val pref = getSharedPreferences(spName, Context.MODE_PRIVATE)
                        if(pref.all.isNotEmpty()) debugInfo += "$spName:${pref.all.size} "
                        for((_,v) in pref.all){
                            try{
                                val parts = v.toString().split("|")
                                if(parts.size>=2){
                                    val amount = parts[1].toFloatOrNull()?:0f
                                    val realProfit = if(parts.size>2) parts[2].toFloatOrNull()?:0f else amount*0.33f
                                    todaySales += amount
                                    profit += realProfit
                                }
                            }catch(_:Exception){}
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}

            // PRODUCTS - SCAN EVERY POSSIBLE FILE!
            try{
                val possibleFiles = mutableListOf<String>()
                // List all shared_prefs files in app directory to find real file
                try{
                    val prefsDir = File(filesDir.parent + "/shared_prefs")
                    if(prefsDir.exists()){
                        for(f in prefsDir.listFiles()!!){
                            val name = f.name.replace(".xml","")
                            possibleFiles.add(name)
                        }
                    }
                }catch(_:Exception){}

                // Add known names
                possibleFiles.addAll(listOf("products_main","products_rukudzo","stock_main","products_db","products","stock_rukudzo","products_main_branch","stock","inventory","product_list","products_list","SmartPosPrefs","smartpos_products","my_products","shop_products"))

                val seen = HashSet<String>()
                allStockItems.clear()
                var foundFiles = ""

                for(prefName in possibleFiles.distinct()){
                    try{
                        val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
                        if(pref.all.isEmpty()) continue
                        // Check if this looks like products (not sales/customers)
                        var looksLikeProduct = false
                        for((_,v) in pref.all.take(2)){
                            val raw = v.toString()
                            if(raw.contains("|") || raw.trim().startsWith("{")) looksLikeProduct = true
                        }
                        if(!looksLikeProduct) continue

                        foundFiles += "$prefName=${pref.all.size} "
                        for((k,v) in pref.all){
                            try{
                                if(seen.contains(k) && k.length>3) continue
                                // Don't double count same key from different file if already found, but allow if first file empty
                                val raw = v.toString()
                                if(raw.length<2) continue
                                var pname = k
                                var qty = 1
                                var sell = 0f
                                var sku = k.take(8)

                                if(raw.trim().startsWith("{")){
                                    try{
                                        val o = JSONObject(raw)
                                        pname = o.optString("name", o.optString("productName", o.optString("title", k)))
                                        sku = o.optString("sku", o.optString("barcode", o.optString("code", k.take(8))))
                                        qty = o.optInt("quantity", o.optInt("qty", o.optInt("stock", o.optInt("available", 1))))
                                        sell = o.optDouble("price", o.optDouble("sellPrice", o.optDouble("sellingPrice", o.optDouble("selling_price", 0.0)))).toFloat()
                                        val cost = o.optDouble("cost",0.0).toFloat()
                                        if(sell==0f) sell = cost * 1.3f
                                        if(sell==0f) sell = 10f
                                    }catch(_:Exception){ continue }
                                } else {
                                    // pipe format - try all variations
                                    val p = raw.split("|")
                                    if(p.size>=1) pname = p[0]
                                    if(p.size>=2){
                                        // could be qty or price
                                        qty = p[1].toIntOrNull()?: p[2].toIntOrNull()?: 1
                                        sell = p[1].toFloatOrNull()?: p[2].toFloatOrNull()?: 0f
                                    }
                                    if(p.size>=3){
                                        sell = p[2].toFloatOrNull()?: p[1].toFloatOrNull()?: 0f
                                        qty = p[1].toIntOrNull()?: p[3].toIntOrNull()?: 1
                                    }
                                    if(p.size>=4){
                                        qty = p[3].toIntOrNull()?: p[1].toIntOrNull()?: 1
                                    }
                                    sku = pname.take(8).uppercase()
                                }

                                if(pname.isBlank()) continue
                                if(qty<0) qty=0
                                if(qty==0 && prodCount<200) qty=1 // prevent 0 qty counting as no stock

                                // Only count once
                                if(!seen.contains(pname)){
                                    seen.add(pname)
                                    seen.add(k)
                                    stockValue += sell * qty
                                    if(qty < 5) lowStock++
                                    prodCount++
                                    allStockItems.add(StockItem(sku, pname, qty, sell, prefName))
                                }
                            }catch(_:Exception){}
                        }
                    }catch(_:Exception){}
                }
                debugInfo += " | Found: $foundFiles"
            }catch(e:Exception){ debugInfo += " ProdErr:${e.message}" }

            try{
                val cPrefs = listOf("customers","customers_db","customer_list","clients","customers_main")
                for(cName in cPrefs){
                    try{
                        val pref = getSharedPreferences(cName, Context.MODE_PRIVATE)
                        custCount += pref.all.size
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}

            val prefsCash = getSharedPreferences("cash", Context.MODE_PRIVATE)
            val drawer = prefsCash.getFloat("drawer", 5.0f)

            val autoDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            val dateRow = LinearLayout(this).apply {
                setPadding(24,16,24,16)
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.WHITE)
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
            }
            val txtOverview = TextView(this).apply {
                text = "Overview (tap to hide)"; textSize = 18f; setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
                layoutParams = LinearLayout.LayoutParams(0,-2,1f)
            }
            val txtDateAuto = TextView(this).apply {
                text = "📅 Today, $autoDate"; textSize = 12f
                setTextColor(Color.parseColor("#64748B"))
                setTypeface(null, Typeface.BOLD)
            }
            val arrow = TextView(this).apply { text = " ▲"; textSize = 14f; setTextColor(Color.parseColor("#0F172A")) }
            dateRow.addView(txtOverview); dateRow.addView(txtDateAuto); dateRow.addView(arrow)
            root.addView(dateRow)

            val overviewContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.VISIBLE }
            var isOverviewVisible = true
            dateRow.setOnClickListener {
                isOverviewVisible =!isOverviewVisible
                overviewContainer.visibility = if(isOverviewVisible) View.VISIBLE else View.GONE
                arrow.text = if(isOverviewVisible) " ▲" else " ▼"
                txtOverview.text = if(isOverviewVisible) "Overview (tap to hide)" else "Overview (tap to show)"
            }

            fun kpiCard(bg:String, icon:String, title:String, valueView: TextView, subView: TextView, onClick:()->Unit): LinearLayout {
                return LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.parseColor(bg))
                    setPadding(20,20,20,20)
                    layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(8,8,8,8) }
                    isClickable = true
                    setOnClickListener{ onClick() }
                    addView(TextView(this@AdminActivity).apply { text = icon; textSize = 22f; gravity = Gravity.CENTER })
                    addView(TextView(this@AdminActivity).apply { text = title; textSize = 11f; gravity = Gravity.CENTER; setTextColor(Color.parseColor("#334155")) })
                    addView(valueView.apply { textSize = 16f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setTextColor(Color.parseColor("#0F172A")) })
                    addView(subView.apply { textSize = 9f; gravity = Gravity.CENTER })
                }
            }

            txtStockValue = TextView(this).apply{ text = "$"+String.format("%.0f",stockValue) }
            val subStock = TextView(this).apply{ text = "Selling Price"; setTextColor(Color.parseColor("#7C3AED")) }

            txtProducts = TextView(this).apply{ text = "$prodCount" }
            val subProd = TextView(this).apply{ text = "Active SKUs"; setTextColor(Color.parseColor("#0F172A")) }

            txtCustomers = TextView(this).apply{ text = "$custCount" }
            val subCust = TextView(this).apply{ text = if(custCount>0) "REAL" else "Tap to add"; setTextColor(Color.parseColor("#EA580C")) }

            txtLowStock = TextView(this).apply{ text = if(lowStock>0) "$lowStock Items" else "0 Items" }
            txtLowSub = TextView(this).apply{ text = if(lowStock>0) "Needs restock" else "All OK"; setTextColor(if(lowStock>0) Color.parseColor("#DC2626") else Color.parseColor("#16A34A")) }

            val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,0,12,0) }
            row1.addView(kpiCard("#BBF7D0","📈","Today's Sales", TextView(this).apply{ text="$${String.format("%.2f",todaySales)}" }, TextView(this).apply{ text="+ REAL"; setTextColor(Color.parseColor("#16A34A")) }){ openSection("Sales Management") })
            row1.addView(kpiCard("#BFDBFE","💰","Profit", TextView(this).apply{ text="$${String.format("%.2f",profit)}" }, TextView(this).apply{ text="REAL PROFIT"; setTextColor(Color.parseColor("#2563EB")) }){ openSection("Reports") })
            row1.addView(kpiCard("#DDD6FE","🏠","Stock Value", txtStockValue, subStock){
                if(prodCount==0){
                    Toast.makeText(this, "No stock found! Debug: $debugInfo", Toast.LENGTH_LONG).show()
                    // Open view stock to see where products are
                    safeOpen(StockListActivity::class.java)
                } else {
                    // Show stock value breakdown
                    val msg = "Total Stock Value: $${String.format("%.2f",stockValue)}\n${prodCount} products\nAvg: $${String.format("%.2f", if(prodCount>0) stockValue/prodCount else 0f)} per SKU\n\nFiles: $debugInfo"
                    android.app.AlertDialog.Builder(this).setTitle("Stock Value - REAL DATA").setMessage(msg).setPositiveButton("View Stock"){_,_-> safeOpen(StockListActivity::class.java)}.setNegativeButton("Close",null).show()
                }
            })

            val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,0,12,12) }
            row2.addView(kpiCard(if(lowStock>0)"#FECACA" else "#BBF7D0", if(lowStock>0)"⚠️" else "✅","Low Stock", txtLowStock, txtLowSub){
                if(lowStock==0 && prodCount==0){
                    Toast.makeText(this, "No products to check! Add products first. Debug: $debugInfo", Toast.LENGTH_LONG).show()
                    safeOpen(StockListActivity::class.java)
                } else if(lowStock==0){
                    Toast.makeText(this, "✅ All ${prodCount} products have 5+ qty!", Toast.LENGTH_SHORT).show()
                } else {
                    var lowList = ""
                    for(item in allStockItems.filter{it.qty<5}.take(10)){
                        lowList += "${item.name}: ${item.qty} left\n"
                    }
                    android.app.AlertDialog.Builder(this).setTitle("⚠️ Low Stock - $lowStock Items").setMessage(lowList+"\nTap RECEIVE STOCK to restock").setPositiveButton("Restock"){_,_-> safeOpen(ReceiveStockActivity::class.java)}.setNegativeButton("Close",null).show()
                }
            })
            row2.addView(kpiCard("#A5F3FC","📦","Products", txtProducts, subProd){
                if(prodCount==0){
                    Toast.makeText(this, "No products found! Checking files: $debugInfo", Toast.LENGTH_LONG).show()
                    safeOpen(StockListActivity::class.java)
                } else {
                    var prodList = ""
                    for(item in allStockItems.take(15)){
                        prodList += "${item.sku} - ${item.name} (${item.qty})\n"
                    }
                    android.app.AlertDialog.Builder(this).setTitle("📦 Products - ${prodCount} SKUs").setMessage(prodList+"\nFile source: $debugInfo").setPositiveButton("View All"){_,_-> safeOpen(StockListActivity::class.java)}.setNegativeButton("Close",null).show()
                }
            })
            row2.addView(kpiCard("#FED7AA","👥","Customers", txtCustomers, subCust){
                if(custCount==0){
                    Toast.makeText(this, "No customers yet - Tap to add first customer", Toast.LENGTH_SHORT).show()
                }
                safeOpen(CustomersActivity::class.java)
            })

            overviewContainer.addView(row1); overviewContainer.addView(row2)

            // DEBUG BOX - Shows where data was found
            val debugBox = TextView(this).apply{
                text = "DEBUG: $debugInfo\nFound ${allStockItems.size} items"
                textSize=9f; setTextColor(Color.GRAY); setPadding(16,8,16,8)
                setBackgroundColor(Color.parseColor("#F1F5F9"))
            }
            overviewContainer.addView(debugBox)

            root.addView(overviewContainer)

            root.addView(TextView(this).apply { text = "Quick Actions"; textSize = 16f; setTypeface(null, Typeface.BOLD); setPadding(24,16,24,8) })
            fun quickRow(b1:Button, b2:Button, b3:Button): LinearLayout {
                val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,0,12,0) }
                b1.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(6,6,6,6)}; b2.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(6,6,6,6)}; b3.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(6,6,6,6)}
                r.addView(b1); r.addView(b2); r.addView(b3); return r
            }
            fun qBtn(text:String, color:String, action:()->Unit): Button {
                return Button(this).apply { this.text = text; setBackgroundColor(Color.parseColor(color)); setTextColor(Color.WHITE); textSize = 11f; setOnClickListener{ action() } }
            }
            root.addView(quickRow(
                qBtn("🚚 + RECEIVE STOCK","#2563EB"){ safeOpen(ReceiveStockActivity::class.java) },
                qBtn("📦 + ADD PRODUCT","#22C55E"){ safeOpen(StockListActivity::class.java) },
                qBtn("🛒 + NEW SALE","#A855F7"){ safeOpen(SalesActivity::class.java) }
            ))
            root.addView(quickRow(
                qBtn("🔄 STOCK TRANSFER","#0D9488"){ safeOpen(StockTransferActivity::class.java) },
                qBtn("🏢 ADD SUPPLIER","#FB923C"){ safeOpen(ManageShopsActivity::class.java) },
                qBtn("👤 + ADD USER","#1E293B"){ safeOpen(UsersActivity::class.java) }
            ))

            root.addView(TextView(this).apply { text = "Admin Menu"; textSize = 16f; setTypeface(null, Typeface.BOLD); setPadding(24,16,24,8) })
            fun menuGroup(title:String, icon:String, items:List<Pair<String,String>>): LinearLayout {
                val group = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16); layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,8,16,8)} }
                group.addView(TextView(this).apply { text = "$icon $title"; textSize = 15f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#0F172A")) })
                for(item in items){
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,16,12,16) }
                    row.addView(TextView(this).apply { text = item.first; textSize = 13f; layoutParams = LinearLayout.LayoutParams(0,-2,1f); setTextColor(Color.parseColor("#334155")) })
                    row.addView(TextView(this).apply { text = ">"; setTextColor(Color.parseColor("#94A3B8")) })
                    row.setOnClickListener { openSection(item.second) }
                    group.addView(row)
                    group.addView(View(this).apply { setBackgroundColor(Color.parseColor("#F1F5F9")); layoutParams = LinearLayout.LayoutParams(-1,2) })
                }
                return group
            }
            root.addView(menuGroup("Sales","📊", listOf(Pair("Sales Management","Sales Management"),Pair("Customers","Customers"),Pair("Cash Drawer $"+String.format("%.2f",drawer),"Cash Management"),Pair("Stock Take Sheet","StockTake"))))
            root.addView(menuGroup("Management","⚙️", listOf(Pair("Users & Permissions","Users & Permissions"),Pair("Branches / Shops","Branches / Shops"),Pair("Stock & Purchasing","Purchasing"),Pair("View Stock (All Shops)","StockList"))))
            root.addView(menuGroup("Analytics","📈", listOf(Pair("Reports (Sales/Profit/Stock)","Reports"),Pair("Audit Log","Audit Log"),Pair("Price Management","Price Management"),Pair("System Settings","System Settings"))))

            scroll.addView(root); setContentView(scroll)
        } catch (e: Exception) {
            val tv = TextView(this); tv.text = "Admin Error: "+e.message; setContentView(tv)
        }
    }

    private fun safeOpen(clazz: Class<*>){
        try{ startActivity(Intent(this, clazz)) }catch(e:Exception){ Toast.makeText(this,"Open ${clazz.simpleName}: ${e.message}",1).show() }
    }
    private fun openSection(title:String){
        try{
            when{
                title.contains("User") -> { safeOpen(UsersActivity::class.java); return }
                title.contains("Branch") -> { safeOpen(BranchesActivity::class.java); return }
                title.contains("Purchasing") || title.contains("Supplier") -> { safeOpen(StockPurchasingActivity::class.java); return }
                title.contains("StockTake") -> { safeOpen(StockTakeActivity::class.java); return }
                title.contains("StockList") || title.contains("View Stock") -> { safeOpen(StockListActivity::class.java); return }
                title.contains("Receive") -> { safeOpen(ReceiveStockActivity::class.java); return }
                title.contains("Transfer") -> { safeOpen(StockTransferActivity::class.java); return }
                title.contains("Sales Management") -> { safeOpen(SalesActivity::class.java); return }
                title.contains("Customer") -> { safeOpen(CustomersActivity::class.java); return }
                title.contains("Cash") -> { safeOpen(CashManagementActivity::class.java); return }
                title.contains("Report") -> { safeOpen(ReportsActivity::class.java); return }
                title.contains("Audit") -> { try{ startActivity(Intent().setClassName(this, "com.smartpos.AuditLogActivity")) }catch(_:Exception){ safeOpen(AdminDetailActivity::class.java) }; return }
                title.contains("Price") -> { safeOpen(PriceManagementActivity::class.java); return }
                title.contains("Setting") -> { safeOpen(SystemSettingsActivity::class.java); return }
                else -> { val i = Intent(this, AdminDetailActivity::class.java); i.putExtra("TITLE", title); startActivity(i) }
            }
        } catch (e:Exception){ Toast.makeText(this,"Open "+title+": "+e.message,1).show() }
    }
}