package com.smartpos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.widget.*

class AdminDetailActivity : Activity() {

    fun lbl(t:String)=TextView(this).apply { text=t; textSize=12f; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor("#334155")); setPadding(0,18,0,4) }
    fun inp(h:String, type:Int=InputType.TYPE_CLASS_TEXT)=EditText(this).apply { hint=h; setText(""); inputType=type; setPadding(20,20,20,20); setBackgroundColor(Color.parseColor("#F8FAFC")) }
    fun tv(t:String,c:Int=Color.BLACK,s:Float=14f)=TextView(this).apply { text=t; setTextColor(c); textSize=s; setPadding(10,10,10,10) }
    fun btn(t:String,col:String,fn:()->Unit)=Button(this).apply { text=t; setBackgroundColor(Color.parseColor(col)); setTextColor(Color.WHITE); setPadding(0,28,0,28); setOnClickListener{ fn() } }
    fun toast(m:String)=Toast.makeText(this,m,Toast.LENGTH_SHORT).show()

    override fun onCreate(b:Bundle?){
        super.onCreate(b)
        val title=intent.getStringExtra("TITLE")?:"Admin"
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16) }
        val head=LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#1E293B")); setPadding(18,18,18,18); orientation=LinearLayout.HORIZONTAL }
        head.addView(TextView(this).apply { text=title.uppercase(); setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        head.addView(Button(this).apply { text="BACK"; setOnClickListener{ finish() } })
        root.addView(head)
        root.addView(TextView(this).apply { text=title; textSize=18f; setTypeface(null,Typeface.BOLD); setPadding(0,16,0,8) })

        // SHARED PREFS
        val usersPrefs=getSharedPreferences("users",0)
        val branchesPrefs=getSharedPreferences("branches",0)
        val productsPrefs=getSharedPreferences("products_db",0)
        val suppliersPrefs=getSharedPreferences("suppliers",0)
        val systemPrefs=getSharedPreferences("system",0)
        val salesPrefs=getSharedPreferences("sales_db",0)
        val auditPrefs=getSharedPreferences("audit_log",0)

        if(title=="Users & Permissions"){
            val fn=inp("e.g. John Doe"); val un=inp("e.g. john123"); val pw=inp("••••••••", InputType.TYPE_TEXT_VARIATION_PASSWORD)
            root.addView(lbl("Full Name")); root.addView(fn); root.addView(lbl("Username")); root.addView(un); root.addView(lbl("Password")); root.addView(pw)
            root.addView(lbl("Role - Tap to Pick")); val roles=arrayOf("CASHIER","MANAGER","ADMIN","STOCK KEEPER")
            val sp=Spinner(this).apply { adapter=ArrayAdapter(this@AdminDetailActivity, android.R.layout.simple_spinner_dropdown_item, roles) }; root.addView(sp)
            root.addView(lbl("Give Permissions - Tick duties:"))
            val cb1=CheckBox(this).apply { text="Make Sales"; isChecked=true }; val cb2=CheckBox(this).apply { text="Do Refunds"; isChecked=true }
            val cb3=CheckBox(this).apply { text="View Reports" }; val cb4=CheckBox(this).apply { text="Manage Stock" }
            val cb5=CheckBox(this).apply { text="Manage Prices" }; val cb6=CheckBox(this).apply { text="Manage Users" }
            val cb7=CheckBox(this).apply { text="Manage Branches" }; val cb8=CheckBox(this).apply { text="System Settings" }
            root.addView(cb1); root.addView(cb2); root.addView(cb3); root.addView(cb4); root.addView(cb5); root.addView(cb6); root.addView(cb7); root.addView(cb8)
            val usersList=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,20,0,0) }
            fun refreshUsers(){ usersList.removeAllViews(); usersPrefs.all.forEach{ val p=it.value.toString().split("|"); usersList.addView(tv("• ${it.key} [${p.getOrNull(2)?:""}] -> ${p.getOrNull(3)?:""}")) } }
            root.addView(btn("SAVE USER WITH PERMISSIONS","#16A34A"){
                if(un.text.toString().isEmpty()){ toast("Enter username"); return@btn }
                val role=roles[sp.selectedItemPosition]; if(role=="ADMIN"){ cb1.isChecked=true; cb2.isChecked=true; cb3.isChecked=true; cb4.isChecked=true; cb5.isChecked=true; cb6.isChecked=true; cb7.isChecked=true; cb8.isChecked=true }
                val perms=ArrayList<String>(); if(cb1.isChecked) perms.add("Sales"); if(cb2.isChecked) perms.add("Refunds"); if(cb3.isChecked) perms.add("Reports"); if(cb4.isChecked) perms.add("Stock"); if(cb5.isChecked) perms.add("Prices"); if(cb6.isChecked) perms.add("Users"); if(cb7.isChecked) perms.add("Branches"); if(cb8.isChecked) perms.add("Settings")
                usersPrefs.edit().putString(un.text.toString(),"${fn.text}|${pw.text}|$role|${perms.joinToString(",")}").apply()
                auditPrefs.edit().putString(System.currentTimeMillis().toString(),"Created user ${un.text} as $role").apply()
                toast("Saved ${un.text} as $role"); fn.setText(""); un.setText(""); pw.setText(""); refreshUsers()
            })
            root.addView(tv("Saved Users:",Color.BLACK,13f)); root.addView(usersList); refreshUsers()

        } else if(title=="Branches / Shops"){
            val sn=inp("e.g. Shop 4 - Bulawayo"); val loc=inp("e.g. Bulawayo"); root.addView(lbl("Shop Name")); root.addView(sn); root.addView(lbl("Location")); root.addView(loc)
            val branchesList=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            fun refreshBranches(){ branchesList.removeAllViews(); if(branchesPrefs.all.isEmpty()) branchesList.addView(tv("Main Shop - Harare\nShop 2 - Chitungwiza\nShop 3 - Norton",Color.GRAY)) else branchesPrefs.all.forEach{ branchesList.addView(tv("• ${it.key} - ${it.value}")) } }
            root.addView(btn("ADD SHOP","#0F766E"){ if(sn.text.toString().isEmpty()){ toast("Enter name"); return@btn } branchesPrefs.edit().putString(sn.text.toString(),loc.text.toString()).apply(); auditPrefs.edit().putString(System.currentTimeMillis().toString(),"Added branch ${sn.text}").apply(); toast("Shop Added"); sn.setText(""); loc.setText(""); refreshBranches() })
            root.addView(branchesList); refreshBranches()

        } else if(title=="Products & Categories"){
            val pname=inp("e.g. Coca Cola 500ml"); val pcat=inp("e.g. Drinks"); val buy=inp("e.g. 0.50", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL); val sell=inp("e.g. 1.00", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL); val qty=inp("e.g. 100", InputType.TYPE_CLASS_NUMBER)
            root.addView(lbl("Product Name")); root.addView(pname); root.addView(lbl("Category")); root.addView(pcat); root.addView(lbl("Buy Price")); root.addView(buy); root.addView(lbl("Sell Price")); root.addView(sell); root.addView(lbl("Quantity")); root.addView(qty)
            val prodList=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,16,0,0) }
            fun refreshProducts(){ prodList.removeAllViews(); if(productsPrefs.all.isEmpty()) prodList.addView(tv("No products yet",Color.GRAY)) else productsPrefs.all.forEach{ try{ val a=it.value.toString().split("|"); prodList.addView(tv("• ${a[0]} | ${a[1]} | Buy:$${a[2]} Sell:$${a[3]} Qty:${a[4]}")) }catch(_:Exception){} } }
            root.addView(btn("ADD PRODUCT","#2563EB"){ if(pname.text.toString().isEmpty()){ toast("Enter name"); return@btn } val id="P${System.currentTimeMillis()}"; productsPrefs.edit().putString(id,"${pname.text}|${pcat.text}|${buy.text}|${sell.text}|${qty.text}").apply(); toast("Product Added"); pname.setText(""); pcat.setText(""); buy.setText(""); sell.setText(""); qty.setText(""); refreshProducts() })
            root.addView(tv("Products:",Color.BLACK,13f)); root.addView(prodList); refreshProducts()

        } else if(title=="Price Management"){
            val cat=inp("e.g. Drinks or leave empty for ALL"); val pct=inp("e.g. 10 for +10% or -5", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED or InputType.TYPE_NUMBER_FLAG_DECIMAL)
            root.addView(lbl("Category Filter (empty = ALL)")); root.addView(cat); root.addView(lbl("Percent Change")); root.addView(pct)
            root.addView(btn("APPLY PRICE UPDATE","#DC2626"){
                val per=pct.text.toString().toFloatOrNull(); if(per==null){ toast("Enter %"); return@btn }
                if(productsPrefs.all.isEmpty()){ toast("No products"); return@btn }
                val ed=productsPrefs.edit(); var c=0; productsPrefs.all.forEach{ try{ val a=it.value.toString().split("|"); val prodCat=a[1]; if(cat.text.toString().isEmpty() || prodCat.equals(cat.text.toString(),true)){ val newSell=a[3].toFloat()*(1+per/100f); ed.putString(it.key,"${a[0]}|${a[1]}|${a[2]}|$newSell|${a[4]}"); c++ } }catch(_:Exception){} }; ed.apply()
                auditPrefs.edit().putString(System.currentTimeMillis().toString(),"Price update $per% on ${if(cat.text.isEmpty()) "ALL" else cat.text} ($c items)").apply()
                toast("Updated $c products by $per%")
            })

        } else if(title=="Suppliers"){
            val sname=inp("e.g. Delta Beverages"); val sphone=inp("e.g. 0771234567"); val semail=inp("e.g. delta@example.com")
            root.addView(lbl("Supplier Name")); root.addView(sname); root.addView(lbl("Phone")); root.addView(sphone); root.addView(lbl("Email")); root.addView(semail)
            val supList=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            fun refreshSup(){ supList.removeAllViews(); if(suppliersPrefs.all.isEmpty()) supList.addView(tv("No suppliers",Color.GRAY)) else suppliersPrefs.all.forEach{ supList.addView(tv("• ${it.key} - ${it.value}")) } }
            root.addView(btn("ADD SUPPLIER","#7C3AED"){ if(sname.text.toString().isEmpty()){ toast("Enter name"); return@btn } suppliersPrefs.edit().putString(sname.text.toString(),"${sphone.text}|${semail.text}").apply(); toast("Supplier Added"); sname.setText(""); sphone.setText(""); semail.setText(""); refreshSup() })
            root.addView(supList); refreshSup()

        } else if(title=="Stock Control"){
            val pList=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            fun refreshStock(){ pList.removeAllViews(); if(productsPrefs.all.isEmpty()) pList.addView(tv("No products",Color.GRAY)) else productsPrefs.all.forEach{ try{ val a=it.value.toString().split("|"); val qty=a[4].toIntOrNull()?:0; val col=if(qty<10) Color.RED else Color.BLACK; pList.addView(tv("• ${a[0]} - Qty: $qty ${if(qty<10) "⚠️ LOW!" else ""}",col)) }catch(_:Exception){} } }
            root.addView(tv("Current Stock Levels:",Color.BLACK,13f)); root.addView(pList)
            val adjName=inp("e.g. Coca Cola"); val adjQty=inp("e.g. 50 to add or -10 to reduce", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)
            root.addView(lbl("Product Name to Adjust")); root.addView(adjName); root.addView(lbl("Adjust Quantity")); root.addView(adjQty)
            root.addView(btn("UPDATE STOCK","#EA580C"){
                val name=adjName.text.toString(); val delta=adjQty.text.toString().toIntOrNull()?:0
                var found=false; val ed=productsPrefs.edit(); productsPrefs.all.forEach{ try{ val a=it.value.toString().split("|"); if(a[0].equals(name,true)){ val newQty=(a[4].toIntOrNull()?:0)+delta; ed.putString(it.key,"${a[0]}|${a[1]}|${a[2]}|${a[3]}|$newQty"); found=true } }catch(_:Exception){} }; ed.apply()
                if(found){ toast("Stock updated by $delta"); adjName.setText(""); adjQty.setText(""); refreshStock() } else toast("Product not found")
            })
            refreshStock()

        } else if(title=="Reports"){
            fun openReport(type:String){
                val rRoot=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,16,16,16); setBackgroundColor(Color.WHITE) }
                rRoot.addView(LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#1E293B")); setPadding(18,18,18,18); orientation=LinearLayout.HORIZONTAL; addView(TextView(this@AdminDetailActivity).apply { text="$type REPORT"; setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) }); addView(Button(this@AdminDetailActivity).apply { text="BACK"; setOnClickListener{ setContentView(ScrollView(this@AdminDetailActivity).apply { addView(root) }) } }) })
                when(type){
                    "SALES"->{ var t=0f; if(salesPrefs.all.isEmpty()) rRoot.addView(tv("No sales yet",Color.GRAY)) else salesPrefs.all.forEach{ try{ val a=it.value.toString().split("|"); t+=a[1].toFloat(); rRoot.addView(tv("${it.key}: $${a[1]} - ${a[0]}")) }catch(_:Exception){} }; rRoot.addView(tv("TOTAL SALES: $${String.format("%.2f",t)}",Color.BLUE,18f)) }
                    "PROFIT"->{ var s=0f; var c=0f; salesPrefs.all.forEach{ try{ val a=it.value.toString().split("|"); s+=a[1].toFloat(); c+=a[2].toFloat() }catch(_:Exception){} }; rRoot.addView(tv("Sales: $${String.format("%.2f",s)}")); rRoot.addView(tv("Cost: $${String.format("%.2f",c)}")); rRoot.addView(tv("PROFIT: $${String.format("%.2f",s-c)}",Color.parseColor("#16A34A"),18f)) }
                    "STOCK"->{ var v=0f; productsPrefs.all.forEach{ try{ val a=it.value.toString().split("|"); v+=a[4].toFloat()*a[2].toFloat() }catch(_:Exception){} }; rRoot.addView(tv("STOCK VALUE (cost): $${String.format("%.2f",v)}",Color.parseColor("#EA580C"),18f)); if(productsPrefs.all.isEmpty()) rRoot.addView(tv("No products")) }
                }
                setContentView(ScrollView(this).apply { addView(rRoot) })
            }
            root.addView(btn("SALES REPORT","#2563EB"){ openReport("SALES") })
            root.addView(btn("PROFIT REPORT","#16A34A"){ openReport("PROFIT") })
            root.addView(btn("STOCK REPORT","#EA580C"){ openReport("STOCK") })
            root.addView(tv("Total Products: ${productsPrefs.all.size} | Total Sales: ${salesPrefs.all.size}",Color.GRAY,12f))

        } else if(title=="Sales Settings"){
            val tax=inp(systemPrefs.getString("tax","15")!!, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL); val disc=inp(systemPrefs.getString("discount","0")!!, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL); val cur=inp(systemPrefs.getString("currency","USD")!!)
            root.addView(lbl("Tax %")); root.addView(tax); root.addView(lbl("Default Discount %")); root.addView(disc); root.addView(lbl("Currency")); root.addView(cur)
            root.addView(btn("SAVE SALES SETTINGS","#0F766E"){ systemPrefs.edit().putString("tax",tax.text.toString()).putString("discount",disc.text.toString()).putString("currency",cur.text.toString()).apply(); toast("Sales Settings Saved") })

        } else if(title=="Receipt Settings"){
            val header=inp(systemPrefs.getString("receiptHeader","SMART POS - Harare")!!); val footer=inp(systemPrefs.getString("receiptFooter","Thank you! Come again!")!!); val showVat=CheckBox(this).apply { text="Show VAT on Receipt"; isChecked=systemPrefs.getBoolean("showVat",true) }
            root.addView(lbl("Receipt Header")); root.addView(header); root.addView(lbl("Receipt Footer")); root.addView(footer); root.addView(showVat)
            root.addView(btn("SAVE RECEIPT SETTINGS","#1E293B"){ systemPrefs.edit().putString("receiptHeader",header.text.toString()).putString("receiptFooter",footer.text.toString()).putBoolean("showVat",showVat.isChecked).apply(); toast("Receipt Saved") })

        } else if(title=="System Settings"){
            val shop=inp(systemPrefs.getString("shopName","SMART POS")!!); val lang=inp(systemPrefs.getString("language","English")!!)
            root.addView(lbl("Shop Name")); root.addView(shop); root.addView(lbl("Language")); root.addView(lang)
            root.addView(btn("SAVE SYSTEM SETTINGS","#1E293B"){ systemPrefs.edit().putString("shopName",shop.text.toString()).putString("language",lang.text.toString()).apply(); toast("System Saved"); finish() })

        } else if(title=="Backup & Restore"){
            root.addView(tv("Backup saves all users, products, branches, sales to internal storage",Color.GRAY,12f))
            root.addView(btn("BACKUP NOW","#16A34A"){
                val backup=getSharedPreferences("backup",0); val ed=backup.edit()
                ed.putString("users",usersPrefs.all.toString()); ed.putString("products",productsPrefs.all.toString()); ed.putString("branches",branchesPrefs.all.toString()); ed.putString("sales",salesPrefs.all.toString()); ed.putLong("time",System.currentTimeMillis()); ed.apply()
                toast("Backup saved at ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date())}")
            })
            root.addView(btn("VIEW LAST BACKUP","#2563EB"){
                val backup=getSharedPreferences("backup",0); val t=backup.getLong("time",0L); if(t==0L) toast("No backup yet") else toast("Last backup: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(java.util.Date(t))} - ${backup.all.size} sets")
            })

        } else if(title=="Audit Log"){
            val logList=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            fun refreshLog(){ logList.removeAllViews(); if(auditPrefs.all.isEmpty()) logList.addView(tv("No activity yet",Color.GRAY)) else auditPrefs.all.toList().sortedByDescending { it.first }.take(50).forEach{ logList.addView(tv("• ${java.text.SimpleDateFormat("dd/MM HH:mm").format(java.util.Date(it.first.toLongOrNull()?:0L))} - ${it.second}")) } }
            root.addView(btn("CLEAR LOG","#DC2626"){ auditPrefs.edit().clear().apply(); refreshLog(); toast("Log cleared") })
            root.addView(logList); refreshLog()
        } else {
            root.addView(tv("$title - Ready (coming soon)"))
        }

        setContentView(ScrollView(this).apply { addView(root) })
    }
}
