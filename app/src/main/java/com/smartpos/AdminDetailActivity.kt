package com.smartpos

import android.app.Activity
import android.content.Context
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

    override fun onCreate(b:Bundle?){ super.onCreate(b); build(intent.getStringExtra("TITLE")?:"Admin") }

    fun build(title:String){
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.WHITE); setPadding(16,16,16,16) }
        val head=LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#1E293B")); setPadding(18,18,18,18); orientation=LinearLayout.HORIZONTAL }
        head.addView(TextView(this).apply { text=title.uppercase(); setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) })
        head.addView(Button(this).apply { text="BACK"; setOnClickListener{ if(title.contains("REPORT")) build("Reports") else finish() } })
        root.addView(head)
        root.addView(TextView(this).apply { text=title; textSize=18f; setTypeface(null,Typeface.BOLD); setPadding(0,16,0,8) })

        when(title){
            "Users & Permissions" -> {
                val fn=inp("e.g. John Doe"); val un=inp("e.g. john123")
                val pw=inp("••••••••", InputType.TYPE_TEXT_VARIATION_PASSWORD)
                root.addView(lbl("Full Name")); root.addView(fn)
                root.addView(lbl("Username")); root.addView(un)
                root.addView(lbl("Password")); root.addView(pw)

                root.addView(lbl("Role - Tap to Pick"))
                val roles=arrayOf("CASHIER","MANAGER","ADMIN","STOCK KEEPER")
                val sp=Spinner(this).apply { adapter=ArrayAdapter(this@AdminDetailActivity, android.R.layout.simple_spinner_dropdown_item, roles) }
                root.addView(sp)

                root.addView(lbl("Give Permissions - Tick what they can do:"))
                val duties=arrayOf("Make Sales","Do Refunds","View Reports","Manage Stock","Manage Prices","Manage Users","Manage Branches","System Settings")
                val checks=duties.map { CheckBox(this).apply { text=it; setPadding(0,10,0,10) } }
                // Default for CASHIER
                checks[0].isChecked=true; checks[1].isChecked=true
                checks.forEach{ root.addView(it) }

                val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,20,0,0) }
                fun refresh(){ list.removeAllViews(); getSharedPreferences("users",0).all.forEach{ list.addView(tv("• ${it.key}")) } }

                root.addView(btn("SAVE USER WITH PERMISSIONS","#16A34A"){
                    if(un.text.isEmpty()){ toast("Enter username"); return@btn }
                    val role=roles[sp.selectedItemPosition]
                    // FIXED: Auto permissions on SAVE, no listener needed
                    if(role=="ADMIN") checks.forEach{ it.isChecked=true }
                    if(role=="CASHIER" && checks.none{ it.isChecked }) { checks[0].isChecked=true; checks[1].isChecked=true }
                    val perms=checks.filter{ it.isChecked }.joinToString(","){ it.text.toString() }
                    if(perms.isEmpty()){ toast("Tick at least 1 duty!"); return@btn }
                    getSharedPreferences("users",0).edit().putString(un.text.toString(),"${fn.text}|${pw.text}|$role|$perms").apply()
                    toast("Saved ${un.text} as $role"); fn.setText(""); un.setText(""); pw.setText(""); refresh()
                })
                root.addView(tv("Saved Users:",Color.BLACK,13f)); root.addView(list); refresh()
            }
            "Branches / Shops" -> {
                val sn=inp("e.g. Shop 4 - Bulawayo"); val loc=inp("e.g. Bulawayo")
                root.addView(lbl("Shop Name")); root.addView(sn); root.addView(lbl("Location")); root.addView(loc)
                val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
                fun refresh(){ list.removeAllViews(); val p=getSharedPreferences("branches",0); if(p.all.isEmpty()) list.addView(tv("Main Shop - Harare\nShop 2 - Chitungwiza\nShop 3 - Norton",Color.GRAY)) else p.all.forEach{ list.addView(tv("• ${it.key} - ${it.value}")) } }
                root.addView(btn("ADD SHOP","#0F766E"){ if(sn.text.isEmpty()){ toast("Enter name"); return@btn } getSharedPreferences("branches",0).edit().putString(sn.text.toString(),loc.text.toString()).apply(); toast("Shop Added"); sn.setText(""); loc.setText(""); refresh() })
                root.addView(list); refresh()
            }
            "Price Management" -> {
                root.addView(lbl("Category")); val cat=inp("e.g. Drinks"); root.addView(cat)
                root.addView(lbl("Percent")); val pct=inp("10", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED); root.addView(pct)
                root.addView(btn("APPLY","#DC2626"){ toast("Applied ${pct.text}% to ${cat.text}") })
            }
            "System Settings" -> {
                val sys=getSharedPreferences("system",0)
                val shop=inp(sys.getString("shopName","SMART POS")!!); val curr=inp(sys.getString("currency","USD")!!)
                root.addView(lbl("Shop Name")); root.addView(shop); root.addView(lbl("Currency")); root.addView(curr)
                root.addView(btn("SAVE SETTINGS","#1E293B"){ sys.edit().putString("shopName",shop.text.toString()).putString("currency",curr.text.toString()).apply(); toast("Saved"); finish() })
            }
            "Reports" -> {
                root.addView(btn("SALES REPORT","#2563EB"){ openReport("SALES") })
                root.addView(btn("PROFIT REPORT","#16A34A"){ openReport("PROFIT") })
                root.addView(btn("STOCK REPORT","#EA580C"){ openReport("STOCK") })
            }
            else -> root.addView(tv("$title ready"))
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    fun openReport(type:String){
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(16,16,16,16) }
        root.addView(LinearLayout(this).apply { setBackgroundColor(Color.parseColor("#1E293B")); setPadding(18,18,18,18); orientation=LinearLayout.HORIZONTAL; addView(TextView(this@AdminDetailActivity).apply { text="$type REPORT"; setTextColor(Color.WHITE); setTypeface(null,Typeface.BOLD); layoutParams=LinearLayout.LayoutParams(0,-2,1f) }); addView(Button(this@AdminDetailActivity).apply { text="BACK"; setOnClickListener{ build("Reports") } }) })
        var t=0f; val p=getSharedPreferences("sales_db",0)
        if(p.all.isEmpty()) root.addView(tv("No sales yet"))
        else p.all.forEach{ try{ val a=it.value.toString().split("|"); t+=a[1].toFloat(); root.addView(tv("${it.key}: $${a[1]}")) }catch(_:Exception){} }
        root.addView(tv("TOTAL: $${String.format("%.2f",t)}",Color.BLUE,18f))
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
