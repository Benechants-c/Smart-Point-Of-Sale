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
import java.text.SimpleDateFormat
import java.util.*

class AdminActivity : Activity() {
    data class StockItem(val sku:String, val name:String, val qty:Int, val sell:Float)

    private val allStockItems = mutableListOf<StockItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val settings = getSharedPreferences("settings", Context.MODE_PRIVATE)
            val shopName = settings.getString("shop_name", "") ?: ""
            val displayShopName = if (shopName.isBlank()) "POS" else shopName

            val scroll = ScrollView(this)
            val root = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.parseColor("#F8FAFC"))
            }

            // HEADER - No hardcoded shop name, comes from Settings
            val header = LinearLayout(this).apply {
                setBackgroundColor(Color.parseColor("#1E293B"))
                setPadding(30,40,30,30)
                orientation = LinearLayout.HORIZONTAL
            }
            val headerText = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0,-2,1f) }
            headerText.addView(TextView(this).apply { text = displayShopName; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.WHITE) })
            headerText.addView(TextView(this).apply { text = "Main Menu"; textSize = 12f; setTextColor(Color.parseColor("#94A3B8")) })
            header.addView(headerText)
            root.addView(header)

            var todaySales = 0f; var profit = 0f; var stockValue = 0f; var lowStock = 0; var prodCount = 0; var custCount = 0

            try{
                val salesPrefs = listOf("sales_main","sales_rukudzo","sales","sales_db","todays_sales")
                for(spName in salesPrefs){
                    try{
                        val pref = getSharedPreferences(spName, Context.MODE_PRIVATE)
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

            try{
                val possibleFiles = listOf(
                    "products_main","products_rukudzo","stock_main","products_db","products",
                    "stock_rukudzo","products_main_branch","stock","inventory",
                    "product_list","products_list","SmartPosPrefs","smartpos_products"
                )
                val seen = HashSet<String>()
                allStockItems.clear()
                for(prefName in possibleFiles){
                    try{
                        val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
                        if(pref.all.isEmpty()) continue
                        for((k,v) in pref.all){
                            try{
                                if(seen.contains(k)) continue
                                val raw = v.toString()
                                if(raw.length<2) continue
                                var pname = k
                                var qty = 1
                                var sell = 0f
                                var sku = k.take(8).uppercase()
                                if(raw.trim().startsWith("{")){
                                    try{
                                        val o = JSONObject(raw)
                                        pname = o.optString("name", o.optString("productName", k))
                                        sku = o.optString("sku", o.optString("barcode", k.take(8)))
                                        qty = o.optInt("quantity", o.optInt("qty", 1))
                                        sell = o.optDouble("price", o.optDouble("sellPrice", 0.0)).toFloat()
                                    }catch(_:Exception){ continue }
                                } else {
                                    val p = raw.split("|")
                                    if(p.isNotEmpty()) pname = p[0]
                                    if(p.size>=2) qty = p[1].toIntOrNull()?:1
                                    if(p.size>=3) sell = p[2].toFloatOrNull()?:0f
                                    if(p.size>=4) qty = p[3].toIntOrNull()?:qty
                                    sku = pname.take(8).uppercase()
                                }
                                if(pname.isBlank()) continue
                                if(!seen.contains(pname)){
                                    seen.add(pname)
                                    seen.add(k)
                                    stockValue += sell * qty
                                    if(qty < 5) lowStock++
                                    prodCount++
                                    allStockItems.add(StockItem(sku, pname, qty, sell))
                                }
                            }catch(_:Exception){}
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}

            try{
                for(cName in listOf("customers","customers_db","customer_list")){
                    try{ custCount += getSharedPreferences(cName, Context.MODE_PRIVATE).all.size }catch(_:Exception){}
                }
            }catch(_:Exception){}

            val prefsCash = getSharedPreferences("cash", Context.MODE_PRIVATE)
            val drawer = prefsCash.getFloat("drawer", 0f)
            val autoDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

            // OVERVIEW
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
            val txtDateAuto = TextView(this).apply { text = "📅 Today, $autoDate"; textSize = 12f; setTextColor(Color.parseColor("#64748B")); setTypeface(null, Typeface.BOLD) }
            val arrow = TextView(this).apply { text = " ▲"; textSize = 14f; setTextColor(Color.parseColor("#0F172A")) }
            dateRow.addView(txtOverview); dateRow.addView(txtDateAuto); dateRow.addView(arrow)
            root.addView(dateRow)

            val overviewContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.VISIBLE }
            var isOverviewVisible = true
            dateRow.setOnClickListener {
                isOverviewVisible =!isOverviewVisible
                overviewContainer.visibility = if(isOverviewVisible) View.VISIBLE else View.GONE
                arrow.text = if(isOverviewVisible) " ▲" else " ▼"
            }

            fun kpiCard(bg:String, icon:String, title:String, value:String, sub:String, subColor:String, onClick:()->Unit): LinearLayout {
                return LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.parseColor(bg))
                    setPadding(20,20,20,20)
                    layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(8,8,8,8) }
                    isClickable = true
                    setOnClickListener{ onClick() }
                    addView(TextView(this@AdminActivity).apply { text = icon; textSize = 22f; gravity = Gravity.CENTER })
                    addView(TextView(this@AdminActivity).apply { text = title; textSize = 11f; gravity = Gravity.CENTER; setTextColor(Color.parseColor("#334155")) })
                    addView(TextView(this@AdminActivity).apply { text = value; textSize = 16f; setTypeface(null, Typeface.BOLD); gravity = Gravity.CENTER; setTextColor(Color.parseColor("#0F172A")) })
                    addView(TextView(this@AdminActivity).apply { text = sub; textSize = 9f; gravity = Gravity.CENTER; setTextColor(Color.parseColor(subColor)) })
                }
            }

            val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,0,12,0) }
            row1.addView(kpiCard("#BBF7D0","📈","Today's Sales","$${String.format("%.2f",todaySales)}","","#16A34A"){ openSection("Transactions") })
            row1.addView(kpiCard("#BFDBFE","💰","Profit","$${String.format("%.2f",profit)}","","#2563EB"){ openSection("Reports") })
            row1.addView(kpiCard("#DDD6FE","🏠","Stock Value","$${String.format("%.0f",stockValue)}","Tap → Details","#7C3AED"){
                val msg = "Total Stock Value: $${String.format("%.2f",stockValue)}\n$prodCount products"
                android.app.AlertDialog.Builder(this).setTitle("Stock Value").setMessage(msg).setPositiveButton("View Stock"){_,_-> safeOpen(StockListActivity::class.java)}.setNegativeButton("Close",null).show()
            })

            val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,0,12,12) }
            row2.addView(kpiCard(if(lowStock>0)"#FECACA" else "#BBF7D0", if(lowStock>0)"⚠️" else "✅","Low Stock", if(lowStock>0) "$lowStock Items" else "0 Items", if(lowStock>0)"Needs restock" else "All OK", if(lowStock>0)"#DC2626" else "#16A34A"){
                var lowList = ""
                for(item in allStockItems.filter{it.qty<5}.take(10)){ lowList += "${item.name}: ${item.qty}\n" }
                if(lowList.isEmpty() && prodCount>0) lowList = "All $prodCount products OK (5+ qty)"
                if(prodCount==0) lowList = "No products found"
                android.app.AlertDialog.Builder(this).setTitle("Low Stock").setMessage(lowList).setPositiveButton("View Stock"){_,_-> safeOpen(StockListActivity::class.java)}.setNegativeButton("Close",null).show()
            })
            row2.addView(kpiCard("#A5F3FC","📦","Products","$prodCount","Tap → List","#0F172A"){
                safeOpen(StockListActivity::class.java)
            })
            row2.addView(kpiCard("#FED7AA","👥","Customers","$custCount","","#EA580C"){ safeOpen(CustomersActivity::class.java) })

            overviewContainer.addView(row1)
            overviewContainer.addView(row2)
            root.addView(overviewContainer)

            // ===== NEW MAIN MENU - 7 BUTTONS ONLY =====
            root.addView(TextView(this).apply { text = "Main Menu"; textSize = 16f; setTypeface(null, Typeface.BOLD); setPadding(24,16,24,8) })

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

            // 1. NEW SALE - Search by Name or Code, handled in SalesActivity
            root.addView(menuGroup("New Sale","🛒", listOf(
                Pair("New Sale","New Sale")
            )))
            // 2. MANAGE SALES - Transactions + Daily Sales Summary + Z
            root.addView(menuGroup("Manage Sales","🧾", listOf(
                Pair("Transactions","Transactions"),
                Pair("Daily Sales Summary","Daily Sales Summary")
            )))
            // 3. PRODUCTS - View Only
            root.addView(menuGroup("Products","📦", listOf(
                Pair("View Products","View Products"),
                Pair("Low Stock","Low Stock")
            )))
            // 4. INVENTORY - View Only
            root.addView(menuGroup("Inventory","🏠", listOf(
                Pair("Current Stock","Current Stock"),
                Pair("Low Stock","Low Stock")
            )))
            // 5. REPORTS
            root.addView(menuGroup("Reports","📈", listOf(
                Pair("Daily Sales Report","Reports"),
                Pair("Z Report","Z Report"),
                Pair("X Report","X Report")
            )))
            // 6. SETTINGS - Manager Only, all empty by default
            root.addView(menuGroup("Settings","⚙️", listOf(
                Pair("Printer Settings","Printer Settings"),
                Pair("Receipt Settings","Receipt Settings"),
                Pair("Device Settings","Device Settings"),
                Pair("Cash Drawer Settings","Cash Drawer Settings"),
                Pair("Sound ON/OFF","Sound Settings"),
                Pair("Change PIN","Change PIN")
            )))
            // 7. BACK OFFICE - Opens Web Admin URL from Settings
            root.addView(menuGroup("Back Office","🌐", listOf(
                Pair("Open Web Admin","Back Office"),
                Pair("Sync Status","Sync Status")
            )))

            scroll.addView(root); setContentView(scroll)
        } catch (e: Exception) {
            val tv = TextView(this); tv.text = "Error: "+e.message; setContentView(tv)
        }
    }

    private fun safeOpen(clazz: Class<*>){
        try{ startActivity(Intent(this, clazz)) }catch(e:Exception){ Toast.makeText(this,"Open ${clazz.simpleName}: ${e.message}",1).show() }
    }

    private fun openSection(title:String){
        try{
            when{
                title == "New Sale" -> { safeOpen(SalesActivity::class.java); return }
                title == "Transactions" || title == "Daily Sales Summary" -> { safeOpen(SalesActivity::class.java); return }
                title == "View Products" || title == "Current Stock" || title == "Low Stock" -> { safeOpen(StockListActivity::class.java); return }
                title == "Reports" || title == "Z Report" || title == "X Report" -> { safeOpen(ReportsActivity::class.java); return }
                title == "Printer Settings" || title == "Receipt Settings" || title == "Device Settings" || title == "Cash Drawer Settings" || title == "Sound Settings" || title == "Change PIN" -> { safeOpen(SystemSettingsActivity::class.java); return }
                title == "Back Office" -> {
                    val adminUrl = getSharedPreferences("settings", Context.MODE_PRIVATE).getString("admin_url", "") ?: ""
                    if(adminUrl.isNotBlank()) startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(adminUrl)))
                    else Toast.makeText(this, "Enter Admin URL in Settings", 1).show()
                    return
                }
                title == "Sync Status" -> { safeOpen(SystemSettingsActivity::class.java); return }
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