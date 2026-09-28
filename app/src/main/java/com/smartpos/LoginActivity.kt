package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import java.text.SimpleDateFormat
import java.util.*

class LoginActivity : Activity() {
    lateinit var pref: SharedPreferences
    var stock = mutableMapOf<String, Item>()
    var list = mutableListOf<Item>()
    var isAuto = false
    data class Item(var name: String, var code: String, var cost: Double, var sell: Double, var qty: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("pos_v92", 0)
        val s = pref.getString("stock", "")?: ""
        if (s.isNotEmpty()) for (row in s.split(";;")) { val p = row.split("|"); if (p.size==5) stock[p[1]]=Item(p[0],p[1],p[2].toDoubleOrNull()?:0.0,p[3].toDoubleOrNull()?:0.0,p[4].toIntOrNull()?:0) }
        if (stock.isEmpty()) { stock["BRD001"]=Item("White Bread","BRD001",1.2,2.0,10); stock["OIL001"]=Item("Cooking Oil","OIL001",3.0,4.5,10); saveAll() }
        login()
    }
    fun saveAll() { val st = stock.values.joinToString(";;") { it.name+"|"+it.code+"|"+it.cost+"|"+it.sell+"|"+it.qty }; pref.edit().putString("stock", st).apply() }
    fun login() {
        val lay = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(60,300,60,60); gravity=Gravity.CENTER; setBackgroundColor(0xFF0A1931.toInt()) }
        val t = TextView(this).apply { text="SMART POS"; textSize=26f; setTextColor(0xFFFFFFFF.toInt()); gravity=Gravity.CENTER }
        val pin = EditText(this).apply { hint="PIN"; inputType=129; setBackgroundColor(0xFFFFFFFF.toInt()) }
        val btn = Button(this).apply { text="LOGIN"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        btn.setOnClickListener { if(pin.text.toString()=="1234") main() else Toast.makeText(this,"1234",Toast.LENGTH_SHORT).show() }
        lay.addView(t); lay.addView(pin); lay.addView(btn); setContentView(lay)
    }
    fun main() {
        // FIXED LINE - AUTO TODAY FROM PHONE
        val autoDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val scroll = ScrollView(this)
        val main = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(14,14,14,14); setBackgroundColor(0xFFFFFFFF.toInt()) }
        val top = TextView(this).apply { text="SMART POS \$12/mo - Receiving"; textSize=16f; setPadding(0,0,0,20) }
        val tvDateLabel = TextView(this).apply { text="Date (Auto)"; textSize=12f }
        val tvDate = TextView(this).apply { text=autoDate; textSize=18f; setPadding(0,5,0,15); setBackgroundColor(0xFFF1F5F9.toInt()) }
        val tvScanLabel = TextView(this).apply { text="Scan or Enter Product Code / Name"; textSize=12f; setPadding(0,10,0,0) }
        val eSearch = EditText(this).apply { hint="Search by code or name..." }
        val tvNameLabel = TextView(this).apply { text="Product Name"; textSize=12f; setPadding(0,20,0,0) }
        val eName = EditText(this).apply { hint="Product Name" }
        val tvCodeLabel = TextView(this).apply { text="Code"; textSize=12f; setPadding(0,15,0,0) }
        val eCode = EditText(this).apply { hint="Code" }
        val tvCostLabel = TextView(this).apply { text="Cost (USD)"; textSize=12f; setPadding(0,15,0,0) }
        val eCost = EditText(this).apply { hint="Cost (USD)"; inputType=8194 }
        val tvSellLabel = TextView(this).apply { text="Selling Price (USD)"; textSize=12f; setPadding(0,15,0,0) }
        val eSell = EditText(this).apply { hint="Selling Price (USD)"; inputType=8194 }
        val tvQtyLabel = TextView(this).apply { text="Qty"; textSize=12f; setPadding(0,15,0,0) }
        val eQty = EditText(this).apply { hint="Qty"; inputType=2 }

        eCode.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) { if(isAuto) return; val code=s.toString().trim().uppercase(); if(code.length>=2 && stock.containsKey(code)) { isAuto=true; val it=stock[code]!!; eName.setText(it.name); eCost.setText(it.cost.toString()); eSell.setText(it.sell.toString()); isAuto=false } }
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
        })
        eName.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) { if(isAuto) return; val n=s.toString().trim().lowercase(); if(n.length<2) return; for(item in stock.values) if(item.name.lowercase().contains(n)) { isAuto=true; eCode.setText(item.code); eCost.setText(item.cost.toString()); eSell.setText(item.sell.toString()); isAuto=false; break } }
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
        })

        val btnAdd = Button(this).apply { text="+ Add Item"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        val tvTable = TextView(this).apply { text="No items yet"; setPadding(0,20,0,10) }
        val tvGrand = TextView(this).apply { text="Grand Total (USD): $0.00"; gravity=Gravity.RIGHT; textSize=16f; setPadding(0,10,0,10) }
        val rowBtn = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER }
        val bClear = Button(this).apply { text="Clear" }
        val bPost = Button(this).apply { text="Post Receiving"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        rowBtn.addView(bClear); rowBtn.addView(bPost)
        fun refresh() { if(list.isEmpty()) { tvTable.text="No items yet"; tvGrand.text="Grand Total (USD): $0.00"; return }; var txt=""; var grand=0.0; var i=1; for(it in list) { val t=it.cost*it.qty; txt+="$i. ${it.name} | ${it.code} | $${it.cost} | $${it.sell} | x${it.qty} = $${String.format("%.2f", t)}\n"; grand+=t; i++ }; tvTable.text=txt; tvGrand.text="Grand Total (USD): $${String.format("%.2f", grand)}" }
        btnAdd.setOnClickListener { if(eName.text.isEmpty()||eCode.text.isEmpty()||eCost.text.isEmpty()||eSell.text.isEmpty()||eQty.text.isEmpty()) { Toast.makeText(this,"Fill all",Toast.LENGTH_SHORT).show(); return@setOnClickListener }; try { val c=eCost.text.toString().toDouble(); val s=eSell.text.toString().toDouble(); if(s<c) { Toast.makeText(this,"Selling < Cost",Toast.LENGTH_LONG).show(); return@setOnClickListener }; list.add(Item(eName.text.toString(),eCode.text.toString().uppercase(),c,s,eQty.text.toString().toInt())); refresh(); eName.text.clear(); eCode.text.clear(); eCost.text.clear(); eSell.text.clear(); eQty.text.clear() } catch(e: Exception) { Toast.makeText(this,"Check numbers",Toast.LENGTH_SHORT).show() } }
        bClear.setOnClickListener { list.clear(); refresh() }
        bPost.setOnClickListener { if(list.isEmpty()) return@setOnClickListener; for(it in list) { val ex=stock[it.code]; if(ex!=null) { ex.qty+=it.qty; ex.cost=it.cost; ex.sell=it.sell } else stock[it.code]=it }; saveAll(); Toast.makeText(this,"Posted ${list.size} items on $autoDate",Toast.LENGTH_LONG).show(); list.clear(); refresh() }

        main.addView(top); main.addView(tvDateLabel); main.addView(tvDate); main.addView(tvScanLabel); main.addView(eSearch); main.addView(tvNameLabel); main.addView(eName); main.addView(tvCodeLabel); main.addView(eCode); main.addView(tvCostLabel); main.addView(eCost); main.addView(tvSellLabel); main.addView(eSell); main.addView(tvQtyLabel); main.addView(eQty); main.addView(btnAdd); main.addView(tvTable); main.addView(tvGrand); main.addView(rowBtn)
        scroll.addView(main); setContentView(scroll)
    }
}
