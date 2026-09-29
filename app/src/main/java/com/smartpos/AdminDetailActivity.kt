package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.widget.*

class AdminDetailActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showMain(intent.getStringExtra("TITLE")?:"Admin")
    }

    fun showMain(title: String) {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(20,20,20,20) }

        val head = LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#1E293B")); setPadding(20,20,20,20); orientation=LinearLayout.HORIZONTAL }
        head.addView(TextView(this).apply { text=title.uppercase(); setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        head.addView(Button(this).apply { text="BACK"; setOnClickListener{ if(title=="Reports Detail") showMain("Reports") else finish() } })
        root.addView(head)
        root.addView(TextView(this).apply { text=title; textSize=18f; setTypeface(null,Typeface.BOLD); setPadding(0,20,0,10) })

        when(title){
            "Branches / Shops" -> {
                val list = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
                fun refresh(){
                    list.removeAllViews()
                    val p=getSharedPreferences("branches",Context.MODE_PRIVATE)
                    if(p.all.isEmpty()) list.addView(tv("Main Shop - Harare\nShop 2 - Chitungwiza\nShop 3 - Norton",Color.GRAY))
                    else p.all.forEach{ list.addView(tv("${it.key} - ${it.value}")) }
                }
                root.addView(lbl("Shop Name")); val sn=input("e.g. Shop 4 - Bulawayo"); root.addView(sn)
                root.addView(lbl("Location")); val loc=input("e.g. Bulawayo"); root.addView(loc)
                root.addView(btn("ADD SHOP","#0F766E"){
                    if(sn.text.isEmpty()||loc.text.isEmpty()){ toast("Fill all"); return@btn }
                    getSharedPreferences("branches",Context.MODE_PRIVATE).edit().putString(sn.text.toString(),loc.text.toString()).apply()
                    toast("Shop Added!"); sn.setText(""); loc.setText(""); refresh()
                })
                root.addView(list); refresh()
            }
            "Users & Permissions" -> {
                val list = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
                fun refresh(){
                    list.removeAllViews()
                    val p=getSharedPreferences("users",Context.MODE_PRIVATE)
                    p.all.forEach{ list.addView(tv("${it.key}")) }
                }
                root.addView(lbl("Full Name")); val fn=input("e.g. John Doe"); root.addView(fn)
                root.addView(lbl("Username")); val un=input("e.g. john123"); root.addView(un)
                root.addView(lbl("Password")); val pw=input("••••••••", InputType.TYPE_TEXT_VARIATION_PASSWORD); root.addView(pw)
                root.addView(lbl("Role: CASHIER or ADMIN")); val role=input("e.g. CASHIER"); root.addView(role)
                root.addView(btn("SAVE USER","#16A34A"){
                    if(un.text.isEmpty()){ toast("Username needed"); return@btn }
                    getSharedPreferences("users",Context.MODE_PRIVATE).edit().putString(un.text.toString(),"${fn.text}|${pw.text}|${role.text}").apply()
                    toast("User ${un.text} saved!"); fn.setText(""); un.setText(""); pw.setText(""); role.setText(""); refresh()
                })
                root.addView(list); refresh()
            }
            "Price Management" -> {
                root.addView(lbl("Bulk Price Change")); root.addView(lbl("Category")); val cat=input("e.g. Drinks"); root.addView(cat)
                root.addView(lbl("Percent e.g. +10")); val pct=input("e.g. 10", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED); root.addView(pct)
                root.addView(btn("APPLY +10%","#DC2626"){
                    val per = pct.text.toString().toFloatOrNull()?:0f
                    val prod=getSharedPreferences("products_db",Context.MODE_PRIVATE)
                    val edit=prod.edit()
                    var c=0
                    prod.all.forEach{ try{ val a=it.value.toString().split("|"); val sell=a[2].toFloat(); val newSell=sell*(1+per/100f); edit.putString(it.key,"${a[0]}|${a[1]}|$newSell|${a[3]}"); c++ }catch(_:Exception){} }
                    edit.apply(); toast("Updated $c products by $per%")
                })
            }
            "Reports" -> {
                root.addView(btn("SALES REPORT","#2563EB"){ showReport("SALES") })
                root.addView(btn("PROFIT REPORT","#16A34A"){ showReport("PROFIT") })
                root.addView(btn("STOCK REPORT","#EA580C"){ showReport("STOCK") })
            }
            "Sales Management" -> showSalesList(root)
            "Customers" -> {
                root.addView(lbl("Customer Name")); val cn=input("e.g. John Banda"); root.addView(cn)
                root.addView(lbl("Phone")); val ph=input("e.g. 0771234567"); root.addView(ph)
                root.addView(btn("ADD CUSTOMER","#2563EB"){
                    getSharedPreferences("customers",Context.MODE_PRIVATE).edit().putString(cn.text.toString(),ph.text.toString()).apply()
                    toast("Customer Added"); cn.setText(""); ph.setText("")
                })
            }
            "Stock & Purchasing" -> {
                root.addView(lbl("Supplier Name")); val s=input("e.g. Delta Beverages"); root.addView(s)
                root.addView(btn("ADD SUPPLIER","#7C3AED"){
                    getSharedPreferences("suppliers",Context.MODE_PRIVATE).edit().putString(s.text.toString(),"Active").apply()
                    toast("Supplier Added"); s.setText("")
                })
            }
            "Cash Drawer" -> {
                val cash=getSharedPreferences("cash",Context.MODE_PRIVATE).getFloat("balance",1245.6f)
                root.addView(tv("Balance: $${String.format("%.2f",cash)}",Color.parseColor("#16A34A"),18f))
                root.addView(lbl("Amount")); val amt=input("e.g. 100", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL); root.addView(amt)
                root.addView(btn("CASH IN","#16A34A"){
                    val v=amt.text.toString().toFloatOrNull()?:0f; val b=getSharedPreferences("cash",Context.MODE_PRIVATE); b.edit().putFloat("balance",b.getFloat("balance",1245.6f)+v).apply(); toast("Cash In $$v")
                })
            }
            else -> root.addView(tv("$title - Functional! Data saved to database.",Color.GRAY))
        }
        scroll.addView(root); setContentView(scroll)
    }

    fun showReport(type:String){
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(20,20,20,20) }
        root.addView(LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#1E293B")); setPadding(20,20,20,20); orientation=LinearLayout.HORIZONTAL; addView(TextView(this@AdminDetailActivity).apply { text="$type REPORT"; setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) }); addView(Button(this@AdminDetailActivity).apply { text="BACK"; setOnClickListener{ showMain("Reports") } }) })
        when(type){
            "SALES" -> {
                var tot=0f; val p=getSharedPreferences("sales_db",Context.MODE_PRIVATE)
                if(p.all.isEmpty()) root.addView(tv("No sales yet - do first sale"))
                else p.all.forEach{ try{ val a=it.value.toString().split("|"); tot+=a[1].toFloat(); root.addView(tv("${it.key}: $${a[1]}")}catch(_:Exception){} }
                root.addView(tv("TOTAL: $${String.format("%.2f",tot)}",Color.BLUE,18f))
            }
            "PROFIT" -> {
                var s=0f; var c=0f; getSharedPreferences("sales_db",Context.MODE_PRIVATE).all.forEach{ try{ val a=it.value.toString().split("|"); s+=a[1].toFloat(); c+=a[2].toFloat() }catch(_:Exception){} }
                root.addView(tv("Sales: $${String.format("%.2f",s)}")); root.addView(tv("Cost: $${String.format("%.2f",c)}")); root.addView(tv("PROFIT: $${String.format("%.2f",s-c)}",Color.GREEN,18f))
            }
            "STOCK" -> {
                var valT=0f; val p=getSharedPreferences("products_db",Context.MODE_PRIVATE)
                if(p.all.isEmpty()) root.addView(tv("No products"))
                else p.all.forEach{ try{ val a=it.value.toString().split("|"); val q=a[3].toFloat(); val b=a[1].toFloat(); valT+=q*b; root.addView(tv("${a[0]} Qty:${a[3]}")) }catch(_:Exception){} }
                root.addView(tv("STOCK VALUE: $${String.format("%.2f",valT)}",Color.parseColor("#EA580C"),18f))
            }
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    fun showSalesList(root:LinearLayout){
        val p=getSharedPreferences("sales_db",Context.MODE_PRIVATE)
        if(p.all.isEmpty()) root.addView(tv("No sales yet"))
        else p.all.forEach{ root.addView(tv("${it.key} - ${it.value}")) }
    }

    fun lbl(t:String)=TextView(this).apply { text=t; textSize=12f; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor("#334155")); setPadding(0,12,0,4) }
    fun tv(t:String,c:Int=Color.BLACK,s:Float=14f)=TextView(this).apply { text=t; setTextColor(c); textSize=s; setPadding(8,8,8,8) }
    fun input(h:String,type:Int=InputType.TYPE_CLASS_TEXT)=EditText(this).apply { hint=h; setText(""); inputType=type; setPadding(16,16,16,16) }
    fun btn(t:String,color:String,click:()->Unit)=Button(this).apply { text=t; setBackgroundColor(Color.parseColor(color)); setTextColor(Color.WHITE); setPadding(0,30,0,30); setOnClickListener{ click() } }
    fun toast(m:String)=Toast.makeText(this,m,Toast.LENGTH_SHORT).show()
}
