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

        if(title=="Users & Permissions"){
            val fn=inp("e.g. John Doe"); val un=inp("e.g. john123")
            val pw=inp("••••••••", InputType.TYPE_TEXT_VARIATION_PASSWORD)
            root.addView(lbl("Full Name")); root.addView(fn)
            root.addView(lbl("Username")); root.addView(un)
            root.addView(lbl("Password")); root.addView(pw)

            root.addView(lbl("Role - Tap to Pick"))
            val roles=arrayOf("CASHIER","MANAGER","ADMIN","STOCK KEEPER")
            val sp=Spinner(this).apply { adapter=ArrayAdapter(this@AdminDetailActivity, android.R.layout.simple_spinner_dropdown_item, roles) }
            root.addView(sp)

            root.addView(lbl("Give Permissions - Tick duties:"))
            val cb1=CheckBox(this).apply { text="Make Sales"; isChecked=true }
            val cb2=CheckBox(this).apply { text="Do Refunds"; isChecked=true }
            val cb3=CheckBox(this).apply { text="View Reports" }
            val cb4=CheckBox(this).apply { text="Manage Stock" }
            val cb5=CheckBox(this).apply { text="Manage Prices" }
            val cb6=CheckBox(this).apply { text="Manage Users" }
            val cb7=CheckBox(this).apply { text="Manage Branches" }
            val cb8=CheckBox(this).apply { text="System Settings" }
            root.addView(cb1); root.addView(cb2); root.addView(cb3); root.addView(cb4)
            root.addView(cb5); root.addView(cb6); root.addView(cb7); root.addView(cb8)

            val usersList=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,20,0,0) }
            val prefsUsers=getSharedPreferences("users",0)
            fun refreshUsers(){ usersList.removeAllViews(); prefsUsers.all.forEach{ usersList.addView(tv("• ${it.key}")) } }

            root.addView(btn("SAVE USER WITH PERMISSIONS","#16A34A"){
                if(un.text.toString().isEmpty()){ toast("Enter username"); return@btn }
                val role=roles[sp.selectedItemPosition]
                if(role=="ADMIN"){ cb1.isChecked=true; cb2.isChecked=true; cb3.isChecked=true; cb4.isChecked=true; cb5.isChecked=true; cb6.isChecked=true; cb7.isChecked=true; cb8.isChecked=true }
                val perms=ArrayList<String>()
                if(cb1.isChecked) perms.add("Make Sales")
                if(cb2.isChecked) perms.add("Do Refunds")
                if(cb3.isChecked) perms.add("View Reports")
                if(cb4.isChecked) perms.add("Manage Stock")
                if(cb5.isChecked) perms.add("Manage Prices")
                if(cb6.isChecked) perms.add("Manage Users")
                if(cb7.isChecked) perms.add("Manage Branches")
                if(cb8.isChecked) perms.add("System Settings")
                if(perms.isEmpty()){ toast("Tick at least 1"); return@btn }
                prefsUsers.edit().putString(un.text.toString(),"${fn.text}|${pw.text}|$role|${perms.joinToString(",")}").apply()
                toast("Saved ${un.text} as $role"); fn.setText(""); un.setText(""); pw.setText(""); refreshUsers()
            })
            root.addView(tv("Saved Users:",Color.BLACK,13f)); root.addView(usersList); refreshUsers()
        } else if(title=="Branches / Shops"){
            val sn=inp("e.g. Shop 4 - Bulawayo"); val loc=inp("e.g. Bulawayo")
            root.addView(lbl("Shop Name")); root.addView(sn); root.addView(lbl("Location")); root.addView(loc)
            val branchesList=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            val prefsBranches=getSharedPreferences("branches",0)
            fun refreshBranches(){ branchesList.removeAllViews(); if(prefsBranches.all.isEmpty()) branchesList.addView(tv("Main Shop - Harare\nShop 2 - Chitungwiza",Color.GRAY)) else prefsBranches.all.forEach{ branchesList.addView(tv("• ${it.key} - ${it.value}")) } }
            root.addView(btn("ADD SHOP","#0F766E"){ if(sn.text.toString().isEmpty()){ toast("Enter name"); return@btn } prefsBranches.edit().putString(sn.text.toString(),loc.text.toString()).apply(); toast("Shop Added"); sn.setText(""); loc.setText(""); refreshBranches() })
            root.addView(branchesList); refreshBranches()
        } else if(title=="Price Management"){
            root.addView(lbl("Category")); val cat=inp("e.g. Drinks"); root.addView(cat)
            root.addView(lbl("Percent")); val pct=inp("10", InputType.TYPE_CLASS_NUMBER); root.addView(pct)
            root.addView(btn("APPLY","#DC2626"){ toast("Applied ${pct.text}%") })
        } else if(title=="System Settings"){
            val sys=getSharedPreferences("system",0)
            val shop=inp(sys.getString("shopName","SMART POS")!!); val curr=inp(sys.getString("currency","USD")!!)
            root.addView(lbl("Shop Name")); root.addView(shop); root.addView(lbl("Currency")); root.addView(curr)
            root.addView(btn("SAVE SETTINGS","#1E293B"){ sys.edit().putString("shopName",shop.text.toString()).putString("currency",curr.text.toString()).apply(); toast("Saved"); finish() })
        } else if(title=="Reports"){
            root.addView(btn("SALES REPORT","#2563EB"){ toast("Opening Sales") })
            root.addView(btn("PROFIT REPORT","#16A34A"){ toast("Opening Profit") })
            root.addView(btn("STOCK REPORT","#EA580C"){ toast("Opening Stock") })
        } else {
            root.addView(tv("$title - Ready"))
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }
}
