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
            header.addView(headerText)
            root.addView(header)

            var todaySales = 0f; var profit = 0f; var stockValue = 0f; var lowStock = 0; var prodCount = 0; var custCount = 0

            try{
                val allSales = listOf("sales_db", "sales_main")
                for(name in allSales){
                    val pref = getSharedPreferences(name, Context.MODE_PRIVATE)
                    for((_,v) in pref.all){
                        try{
                            val parts = v.toString().split("|")
                            if(parts.size>=2){
                                val amount = parts[1].toFloatOrNull()?:0f
                                val cost = if(parts.size>2) parts[2].toFloatOrNull()?:0f else 0f
                                todaySales += amount
                                profit += if(cost>0) amount-cost else amount*0.2f
                            }
                        }catch(_:Exception){}
                    }
                }
            }catch(_:Exception){}

            try{
                val allProd = listOf("products_db", "stock_main", "products")
                val seen = HashSet<String>()
                for(name in allProd){
                    val pref = getSharedPreferences(name, Context.MODE_PRIVATE)
                    for((k,v) in pref.all){
                        try{
                            if(seen.contains(k)) continue
                            seen.add(k)
                            val p = v.toString().split("|")
                            val buy = if(p.size>1) p[1].toFloatOrNull()?:0f else 0f
                            var sell = if(p.size>2) p[2].toFloatOrNull()?:0f else 0f
                            if(sell==0f) sell = buy
                            var qty = 1f
                            if(p.size>3) qty = p[3].toFloatOrNull()?:1f
                            // SELLING PRICE NOW
                            stockValue += sell * qty
                            if(qty < 5) lowStock++
                            prodCount++
                        }catch(_:Exception){}
                    }
                }
            }catch(_:Exception){}

            try{
                val c1 = getSharedPreferences("customers", Context.MODE_PRIVATE).all.size
                val c2 = getSharedPreferences("customers_db", Context.MODE_PRIVATE).all.size
                custCount = c1 + c2
            }catch(_:Exception){}

            val prefsCash = getSharedPreferences("cash", Context.MODE_PRIVATE)
            val drawer = prefsCash.getFloat("drawer", 5.0f)

            val dateRow = LinearLayout(this).apply { setPadding(24,16,24,8); orientation = LinearLayout.HORIZONTAL }
            dateRow.addView(TextView(this).apply { text = "Overview"; textSize = 20f; setTypeface(null, Typeface.BOLD); setTextColor(Color.parseColor("#0F172A")); layoutParams = LinearLayout.LayoutParams(0,-2,1f) })
            dateRow.addView(TextView(this).apply { text = "📅 Today, 29/09/2026"; textSize = 12f; setTextColor(Color.parseColor("#64748B")) })
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
            row1.addView(kpiCard("#BBF7D0","📈","Today's Sales","$"+String.format("%.2f",todaySales),"+ REAL","#16A34A"))
            row1.addView(kpiCard("#BFDBFE","💰","Profit","$"+String.format("%.2f",profit),"REAL","#2563EB"))
            row1.addView(kpiCard("#DDD6FE","🏠","Stock Value","$"+String.format("%.0f",stockValue),"Selling Price","#7C3AED"))
            val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12,0,12,12) }
            row2.addView(kpiCard(if(lowStock>0)"#FECACA" else "#BBF7D0", if(lowStock>0)"⚠️" else "✅","Low Stock",lowStock.toString()+" Items", if(lowStock>0)"Needs restock" else "All OK", if(lowStock>0)"#DC2626" else "#16A34A"))
            row2.addView(kpiCard("#A5F3FC","📦","Products",prodCount.toString(),"Active SKUs","#0F172A"))
            row2.addView(kpiCard("#FED7AA","👥","Customers",custCount.toString(),"REAL","#EA580C"))
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
            val qa1 = quickRow(
                qBtn("🚚 + RECEIVE STOCK","#2563EB"){ try{ startActivity(Intent(this, ReceiveStockActivity::class.java)) }catch(e:Exception){ Toast.makeText(this,""+e.message,1).show() } },
                qBtn("📦 + ADD PRODUCT","#22C55E"){ openSection("Add Product") },
                qBtn("🛒 + NEW SALE","#A855F7"){ try{ startActivity(Intent(this, SalesActivity::class.java)) }catch(_:Exception){ openSection("Sales Management") } }
            )
            val qa2 = quickRow(
                qBtn("🔄 STOCK TRANSFER","#0D9488"){ try{ startActivity(Intent(this, StockTransferActivity::class.java)) }catch(e:Exception){ Toast.makeText(this,""+e.message,1).show() } },
                qBtn("🏢 ADD SUPPLIER","#FB923C"){ openSection("Purchasing") },
                qBtn("👤 + ADD USER","#1E293B"){ openSection("Users & Permissions") }
            )
            root.addView(qa1); root.addView(qa2)

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
                    group.addView(TextView(this).apply { text = ""; setBackgroundColor(Color.parseColor("#F1F5F9")); layoutParams = LinearLayout.LayoutParams(-1,2) })
                }
                return group
            }
            root.addView(menuGroup("Sales","📊", listOf(Pair("Sales Management","Sales Management"),Pair("Customers","Customers"),Pair("Cash Drawer $"+String.format("%.2f",drawer),"Cash Management"))))
            root.addView(menuGroup("Management","⚙️", listOf(Pair("Users & Permissions","Users & Permissions"),Pair("Branches / Shops","Branches / Shops"),Pair("Stock & Purchasing","Purchasing"))))
            root.addView(menuGroup("Analytics","📈", listOf(Pair("Reports (Sales/Profit/Stock)","Reports"),Pair("Audit Log","Audit Log"),Pair("Price Management","Price Management"),Pair("System Settings","System Settings"))))

            scroll.addView(root); setContentView(scroll)
        } catch (e: Exception) {
            val tv = TextView(this); tv.text = "Admin Error: "+e.message; setContentView(tv)
        }
    }
    private fun openSection(title:String){
        try{
            if(title.contains("Branch")){ startActivity(Intent(this, BranchesActivity::class.java)); return }
            val i = Intent(this, AdminDetailActivity::class.java)
            var cleanTitle = title
            if(title.contains("Sales")) cleanTitle = "Sales Management"
            else if(title.contains("Customer")) cleanTitle = "Customers"
            else if(title.contains("Cash")) cleanTitle = "Cash Management"
            else if(title.contains("User")) cleanTitle = "Users & Permissions"
            else if(title.contains("Purchasing") || title.contains("Stock")) cleanTitle = "Purchasing"
            else if(title.contains("Report")) cleanTitle = "Reports"
            else if(title.contains("Audit")) cleanTitle = "Audit Log"
            else if(title.contains("Price")) cleanTitle = "Price Management"
            else if(title.contains("Setting")) cleanTitle = "System Settings"
            i.putExtra("TITLE", cleanTitle); startActivity(i)
        } catch (e:Exception){ Toast.makeText(this,"Open "+title+": "+e.message,1).show() }
    }
}
