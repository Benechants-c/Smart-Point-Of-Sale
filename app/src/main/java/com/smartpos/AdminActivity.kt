package com.smartpos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.*

class AdminActivity : Activity() {
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
            val avatar = TextView(this).apply { text = "👤"; textSize = 28f; setBackgroundColor(Color.parseColor("#334155")); setPadding(16,8,16,8) }
            header.addView(headerText); header.addView(avatar)
            root.addView(header)

            val prefsSales = getSharedPreferences("sales_db", Context.MODE_PRIVATE)
            val prefsProducts = getSharedPreferences("products_db", Context.MODE_PRIVATE)
            val prefsCustomers = getSharedPreferences("customers", Context.MODE_PRIVATE)
            val prefsCash = getSharedPreferences("cash", Context.MODE_PRIVATE)

            val today = "29/09/2026"
            var todaySales = 0f; var profit = 0f
            try{
                prefsSales.all.forEach {
                    val parts = it.value.toString().split("|")
                    if(parts.size>=3 && parts[0]==today) { todaySales += parts[1].toFloatOrNull()?:0f; profit += (parts[1].toFloatOrNull()?:0f) - (parts[2].toFloatOrNull()?:0f) }
                }
            }catch(_:Exception){}
            var stockValue = 0f; var lowStock = 0; var prodCount = prefsProducts.all.size
            try{
                prefsProducts.all.forEach {
                    val p = it.value.toString().split("|")
                    val qty = p.getOrNull(3)?.toFloatOrNull()?:0f; val buy = p.getOrNull(1)?.toFloatOrNull()?:0f
                    stockValue += qty * buy; if(qty < 5) lowStock++
                }
            }catch(_:Exception){}
            val custCount = prefsCustomers.all.size
            val drawer = prefsCash.getFloat("drawer", 1245.6f)

            val dateRow = LinearLayout(this).apply { setPadding(24,16,24,8); orientation = LinearLayout.HORIZONTAL }
            dateRow.addView(TextView(this).apply { text = "Overview"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#0F172A")); layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
            dateRow.addView(TextView(this).apply { text = "📅 Today, $today"; textSize = 12f; setTextColor(Color.parseColor("#64748B")) })
            root.addView(dateRow)

            fun kpiCard(bg:String, icon:String, title:String, value:String, sub:String, subColor:String): LinearLayout {
                return LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.parseColor(bg))
                    setPadding(20,20,20,20)
                    layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(8,8,8,8) }
                    addView(TextView(this@AdminActivity).apply { text = icon; textSize = 22f; gravity = android.view.Gravity.CENTER })
                    addView(TextView(this@AdminActivity).apply { text = title; textSize = 11f; gravity = android.view.Gravity.CENTER; setTextColor(Color.parseColor("#334155")) })
                    addView(TextView(this@AdminActivity).apply { text = value; textSize = 16f; setTypeface(null, Typeface.BOLD); gravity = android.view.Gravity.CENTER; setTextColor(Color.parseColor("#0F172A")) })
                    addView(TextView(this@AdminActivity).apply { text = sub; textSize = 9f; gravity = android.view.Gravity.CENTER; setTextColor(Color.parseColor(subColor)) })
                }
            }
            val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,0,12,0) }
            row1.addView(kpiCard("#BBF7D0","📈","Today's Sales","$${String.format("%.2f",todaySales)}","+ REAL","#16A34A"))
            row1.addView(kpiCard("#BFDBFE","💰","Profit","$${String.format("%.2f",profit)}","REAL","#2563EB"))
            row1.addView(kpiCard("#DDD6FE","🏠","Stock Value","$${String.format("%.0f",stockValue)}","REAL","#7C3AED"))
            val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,0,12,12) }
            row2.addView(kpiCard(if(lowStock>0)"#FECACA" else "#BBF7D0", if(lowStock>0)"⚠️" else "✅","Low Stock","$lowStock Items", if(lowStock>0)"Needs restock" else "All OK", if(lowStock>0)"#DC2626" else "#16A34A"))
            row2.addView(kpiCard("#A5F3FC","📦","Products","$prodCount","Active SKUs","#0F172A"))
            row2.addView(kpiCard("#FED7AA","👥","Customers","$custCount","REAL","#EA580C"))
            root.addView(row1); root.addView(row2)

            root.addView(TextView(this).apply { text = "Quick Actions"; textSize = 16f; setTypeface(null, Typeface.BOLD); setPadding(24,8,24,8) })
            fun quickRow(b1:Button, b2:Button, b3:Button): LinearLayout {
                val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,0,12,0) }
                b1.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(6,6,6,6)}; b2.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(6,6,6,6)}; b3.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(6,6,6,6)}
                r.addView(b1); r.addView(b2); r.addView(b3); return r
            }
            fun qBtn(text:String, color:String, action:()->Unit): Button {
                return Button(this).apply { this.text = text; setBackgroundColor(Color.parseColor(color)); setTextColor(Color.WHITE); textSize = 11f; setOnClickListener{ action() } }
            }
            // KEEP YOUR 3 DONE FIELDS SAFE
            val qa1 = quickRow(
                qBtn("🚚 + Receive Stock","#2563EB"){ try{ startActivity(Intent(this, ReceiveStockActivity::class.java)) }catch(e:Exception){ Toast.makeText(this,"ReceiveStock: ${e.message}",1).show(); openSection("Purchasing") } },
                qBtn("📦 + Add Product","#22C55E"){ openSection("Add Product") },
                qBtn("🛒 + New Sale","#A855F7"){ try{ startActivity(Intent(this, SalesActivity::class.java)) }catch(_:Exception){ openSection("Sales Management") } }
            )
            val qa2 = quickRow(
                qBtn("🔄 Stock Transfer","#0D9488"){ try{ startActivity(Intent(this, StockTransferActivity::class.java)) }catch(e:Exception){ Toast.makeText(this,"Transfer: ${e.message}",1).show(); openSection("Purchasing") } },
                qBtn("🏢 Add Supplier","#FB923C"){ openSection("Purchasing") },
                qBtn("👤 + Add User","#1E293B"){ openSection("Users & Permissions") }
            )
            root.addView(qa1); root.addView(qa2)

            root.addView(TextView(this).apply { text = "Admin Menu"; textSize = 16f; setTypeface(null, Typeface.BOLD); setPadding(24,16,24,8) })
            fun menuGroup(title:String, icon:String, items:List<Pair<String,String>>): LinearLayout {
                val group = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16); layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,8,16,8)} }
                group.addView(TextView(this).apply { text = "$icon $title"; textSize = 15f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#0F172A")) })
                items.forEach { (name, target) ->
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,16,12,16) }
                    row.addView(TextView(this).apply { text = name; textSize = 13f; layoutParams = LinearLayout.LayoutParams(0,-2,1f); setTextColor(Color.parseColor("#334155")) })
                    row.addView(TextView(this).apply { text = ">"; setTextColor(Color.parseColor("#94A3B8")) })
                    row.setOnClickListener { openSection(target) }
                    group.addView(row)
                    group.addView(TextView(this).apply { text = ""; setBackgroundColor(Color.parseColor("#F1F5F9")); layoutParams = LinearLayout.LayoutParams(-1,2) })
                }
                return group
            }
            root.addView(menuGroup("Sales","📊", listOf("Sales Management" to "Sales Management","Customers" to "Customers","Cash Drawer $${String.format("%.2f",drawer)}" to "Cash Management")))
            root.addView(menuGroup("Management","⚙️", listOf("Users & Permissions" to "Users & Permissions","Branches / Shops" to "Branches / Shops","Stock & Purchasing" to "Purchasing")))
            root.addView(menuGroup("Analytics","📈", listOf("Reports (Sales/Profit/Stock)" to "Reports","Audit Log" to "Audit Log","Price Management" to "Price Management","System Settings" to "System Settings")))

            scroll.addView(root)
            setContentView(scroll)
        } catch (e: Exception) {
            val tv = TextView(this); tv.text = "Admin Error: ${e.message}"; setContentView(tv)
        }
    }
    private fun openSection(title:String){
        try{
            if(title.contains("Branch")){
                startActivity(Intent(this, BranchesActivity::class.java)); return
            }
            val i = Intent(this, AdminDetailActivity::class.java)
            val cleanTitle = when{
                title.contains("Sales") -> "Sales Management"
                title.contains("Customer") -> "Customers"
                title.contains("Cash") -> "Cash Management"
                title.contains("User") -> "Users & Permissions"
                title.contains("Purchasing") || title.contains("Stock") -> "Purchasing"
                title.contains("Report") -> "Reports"
                title.contains("Audit") -> "Audit Log"
                title.contains("Price") -> "Price Management"
                title.contains("Setting") -> "System Settings"
                else -> title
            }
            i.putExtra("TITLE", cleanTitle); startActivity(i)
        } catch (e:Exception){ Toast.makeText(this,"Open $title: ${e.message}",1).show() }
    }
}
