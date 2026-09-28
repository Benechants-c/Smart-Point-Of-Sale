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
    var cart = mutableListOf<Item>()
    var sales = mutableListOf<Sale>()
    var selectedPay = "Cash"
    data class Item(var name: String, var code: String, var cost: Double, var sell: Double, var qty: Int)
    data class Sale(var date: String, var total: Double, var items: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("pos_v14_cashier", 0)
        load(); login()
    }
    fun load() {
        val s = pref.getString("stock", "")?: ""
        if (s.isNotEmpty()) for (row in s.split(";;")) { val p=row.split("|"); if(p.size==5) stock[p[1]]=Item(p[0],p[1],p[2].toDoubleOrNull()?:0.0,p[3].toDoubleOrNull()?:0.0,p[4].toIntOrNull()?:0) }
        if (stock.isEmpty()) { stock["1002"]=Item("milk","1002",0.7,1.0,50); stock["1003"]=Item("dovi","1003",2.0,3.0,20); stock["CC500"]=Item("Coca Cola","CC500",0.7,1.0,50); stock["BRD001"]=Item("Bread","BRD001",1.2,1.5,20) }
        val sl = pref.getString("sales", "")?: ""
        if (sl.isNotEmpty()) for (row in sl.split(";;")) { val p=row.split("||"); if(p.size==3) sales.add(Sale(p[0],p[1].toDoubleOrNull()?:0.0,p[2])) }
    }
    fun saveAll() {
        pref.edit().putString("stock", stock.values.joinToString(";;"){it.name+"|"+it.code+"|"+it.cost+"|"+it.sell+"|"+it.qty}).apply()
        pref.edit().putString("sales", sales.joinToString(";;"){it.date+"||"+it.total+"||"+it.items}).apply()
    }
    fun login() {
        val lay = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(60,200,60,60); gravity=Gravity.CENTER; setBackgroundColor(0xFF0A1931.toInt()) }
        val t = TextView(this).apply { text="SmartShop POS\nCASHIER LOGIN"; textSize=20f; setTextColor(0xFFFFFFFF.toInt()); gravity=Gravity.CENTER }
        val pin = EditText(this).apply { hint="Cashier PIN 0000 / Admin 1234"; inputType=129; setBackgroundColor(0xFFFFFFFF.toInt()) }
        val btn = Button(this).apply { text="LOGIN"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        btn.setOnClickListener {
            if(pin.text.toString()=="0000") { dashboard(false) }
            else if(pin.text.toString()=="1234") { dashboard(true) }
            else Toast.makeText(this,"0000 Cashier / 1234 Admin",Toast.LENGTH_SHORT).show()
        }
        lay.addView(t); lay.addView(pin); lay.addView(btn); setContentView(lay)
    }

    fun dashboard(isAdmin: Boolean) {
        val root = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        val drawer = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFF0A1931.toInt()); layoutParams=LinearLayout.LayoutParams(220, LinearLayout.LayoutParams.MATCH_PARENT) }
        val scrollContent = ScrollView(this).apply { layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f); setBackgroundColor(0xFFF1F5F9.toInt()) }
        val content = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(6,6,6,6) }

        fun showSales() {
            drawer.visibility=8 // HIDE DRAWER - 1) YOUR REQUEST
            content.removeAllViews()

            // TOP BAR with Admin button if admin
            val topBar = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,0,0,4) }
            val title = TextView(this).apply { text="SMART POS \$12/mo - SALES"; textSize=12f; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            val btnAdmin = Button(this).apply { text="≡ ADMIN"; textSize=10f; setBackgroundColor(0xFF0A1931.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            btnAdmin.setOnClickListener { drawer.visibility=0; showHome() }
            topBar.addView(title); if(isAdmin) topBar.addView(btnAdmin)
            content.addView(topBar)

            // SEARCH
            val top = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
            val eSearch = EditText(this).apply { hint="Scan barcode or enter product name / code..."; setBackgroundColor(0xFFFFFFFF.toInt()); textSize=14f }
            val btnSearch = Button(this).apply { text="SEARCH"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            top.addView(eSearch, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0,0,6,0) })
            top.addView(btnSearch)
            content.addView(top)

            // HEADER
            val header = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setBackgroundColor(0xFFE2E8F0.toInt()); setPadding(4,8,4,8) }
            fun hd(t: String, w: Float)=TextView(this).apply { text=t; textSize=11f; setPadding(4,0,4,0); layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w) }
            header.addView(hd("#",0.3f)); header.addView(hd("Product Name",2f)); header.addView(hd("Code",1f)); header.addView(hd("Qty",1.5f)); header.addView(hd("Price",0.8f)); header.addView(hd("Total",0.8f)); header.addView(hd("X",0.4f))
            content.addView(header)

            val cartBox = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFFFFFFFF.toInt()) }
            content.addView(cartBox)

            // SUMMARY - FULL WIDTH NOW
            val sumBox = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(10,10,10,10) }
            val tvSub = TextView(this).apply { text="Subtotal 0.00"; gravity=Gravity.RIGHT; textSize=14f }
            val tvTax = TextView(this).apply { text="Tax 0.00"; gravity=Gravity.RIGHT }
            val tvTot = TextView(this).apply { text="Total 0.00"; textSize=20f; setBackgroundColor(0xFF1E3A5F.toInt()); setTextColor(0xFFFFFFFF.toInt()); setPadding(12,12,12,12); gravity=Gravity.RIGHT }
            val tvPay = TextView(this).apply { text="Payment Method"; setPadding(0,12,0,4); textSize=13f }
            val payRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
            val bCash = Button(this).apply { text="CASH"; setBackgroundColor(0xFF16A34A.toInt()); setTextColor(0xFFFFFFFF.toInt()); textSize=16f }
            val bEco = Button(this).apply { text="ECOCASH"; setBackgroundColor(0xFF7C3AED.toInt()); setTextColor(0xFFFFFFFF.toInt()); textSize=16f }
            payRow.addView(bCash, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0,0,6,0) })
            payRow.addView(bEco, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val tvRec = TextView(this).apply { text="Amount Received"; textSize=12f; setPadding(0,10,0,2) }
            val eRec = EditText(this).apply { hint="0.00"; inputType=8194; setBackgroundColor(0xFFF8FAFC.toInt()); textSize=18f }
            val tvChangeL = TextView(this).apply { text="Change"; textSize=12f; setPadding(0,6,0,2) }
            val tvChange = TextView(this).apply { text="0.00"; textSize=22f; setBackgroundColor(0xFFDCFCE7.toInt()); setTextColor(0xFF15803D.toInt()); setPadding(12,10,12,10); gravity=Gravity.RIGHT }
            val bComplete = Button(this).apply { text="✔ COMPLETE SALE"; setBackgroundColor(0xFF16A34A.toInt()); setTextColor(0xFFFFFFFF.toInt()); textSize=18f; setPadding(0,20,0,20) }

            sumBox.addView(tvSub); sumBox.addView(tvTax); sumBox.addView(tvTot); sumBox.addView(tvPay); sumBox.addView(payRow); sumBox.addView(tvRec); sumBox.addView(eRec); sumBox.addView(tvChangeL); sumBox.addView(tvChange); sumBox.addView(bComplete)
            content.addView(sumBox)

            fun refresh() {
                cartBox.removeAllViews()
                var sub=0.0
                for((idx, item) in cart.withIndex()) {
                    val tot=item.sell*item.qty; sub+=tot
                    val row = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(2,10,2,10); setBackgroundColor(if(idx%2==0) 0xFFFFFFFF.toInt() else 0xFFF8FAFC.toInt()) }
                    val tvIdx = TextView(this).apply { text="${idx+1}"; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f) }
                    val tvName = TextView(this).apply { text=item.name; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f) }
                    val tvCode = TextView(this).apply { text=item.code; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
                    // 3) QTY ON SAME LINE - and + NOW
                    val qtyBox = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f) }
                    val bm = Button(this).apply { text="-"; setPadding(12,4,12,4) }
                    val tq = TextView(this).apply { text="${item.qty}"; gravity=Gravity.CENTER; setPadding(12,0,12,0); setBackgroundColor(0xFFE2E8F0.toInt()); textSize=16f }
                    val bp = Button(this).apply { text="+"; setPadding(12,4,12,4) }
                    qtyBox.addView(bm); qtyBox.addView(tq); qtyBox.addView(bp)
                    val tvPrice = TextView(this).apply { text=String.format("%.2f", item.sell); gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f) }
                    val tvT = TextView(this).apply { text=String.format("%.2f", tot); gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f) }
                    val bd = Button(this).apply { text="X"; setBackgroundColor(0xFFFFC7C7.toInt()); layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.4f) }
                    bm.setOnClickListener { if(item.qty>1){item.qty--; refresh()} }
                    bp.setOnClickListener { item.qty++; refresh() }
                    bd.setOnClickListener { cart.remove(item); refresh() }
                    row.addView(tvIdx); row.addView(tvName); row.addView(tvCode); row.addView(qtyBox); row.addView(tvPrice); row.addView(tvT); row.addView(bd)
                    cartBox.addView(row)
                }
                tvSub.text="Subtotal ${String.format("%.2f", sub)}"
                tvTot.text="Total ${String.format("%.2f", sub)}"
                val rec=eRec.text.toString().toDoubleOrNull()?:0.0
                val ch=rec-sub
                tvChange.text=String.format("%.2f", ch)
                if(ch<0) { tvChange.setBackgroundColor(0xFFFFE2E2.toInt()); tvChange.setTextColor(0xFFDC2626.toInt()) } else { tvChange.setBackgroundColor(0xFFDCFCE7.toInt()); tvChange.setTextColor(0xFF15803D.toInt()) }
                if(sub==0.0) cartBox.addView(TextView(this).apply { text="Cart empty"; gravity=Gravity.CENTER; setPadding(0,40,0,40) })
            }

            fun doSearch() {
                val q=eSearch.text.toString().trim().lowercase()
                if(q.isEmpty()) return
                val found=stock.values.filter { it.code.lowercase().contains(q) || it.name.lowercase().contains(q) }
                if(found.isEmpty()) { Toast.makeText(this,"Not found $q",Toast.LENGTH_SHORT).show(); return }
                val st=found[0]
                val ex=cart.find { it.code==st.code }
                if(ex!=null) ex.qty++ else cart.add(Item(st.name,st.code,st.cost,st.sell,1))
                eSearch.text.clear(); refresh()
            }

            btnSearch.setOnClickListener { doSearch() }
            eSearch.setOnEditorActionListener { _, _, _ -> doSearch(); true }

            // 2) CHANGE LIVE CALCULATION FIX
            eRec.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) { refresh() }
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            })

            bCash.setOnClickListener { selectedPay="Cash"; bCash.setBackgroundColor(0xFF15803D.toInt()); bEco.setBackgroundColor(0xFF7C3AED.toInt()) }
            bEco.setOnClickListener { selectedPay="EcoCash"; bEco.setBackgroundColor(0xFF5B21B6.toInt()); bCash.setBackgroundColor(0xFF16A34A.toInt()) }

            bComplete.setOnClickListener {
                if(cart.isEmpty()) return@setOnClickListener
                val tot=cart.sumOf { it.sell*it.qty }
                val rec=eRec.text.toString().toDoubleOrNull()?:0.0
                if(rec < tot) { Toast.makeText(this,"Received $rec < Total $tot",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                val date=SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                sales.add(Sale(date,tot,cart.joinToString(","){ "${it.name}x${it.qty}" }))
                for(c in cart) stock[c.code]?.qty=(stock[c.code]?.qty?:0)-c.qty
                saveAll()
                Toast.makeText(this,"Change $${String.format("%.2f", rec-tot)}",Toast.LENGTH_LONG).show()
                cart.clear(); eRec.text.clear(); refresh()
            }
            refresh()
        }

        fun showHome() { drawer.visibility=0; content.removeAllViews(); content.addView(TextView(this).apply { text="HOME\nSales Today $${sales.filter { it.date==SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }.sumOf { it.total }}\n\nPIN 0000 = Cashier (Sales only)\nPIN 1234 = Admin (All menus)"; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(15,15,15,15) }) }
        fun showSimple(t: String) { drawer.visibility=0; content.removeAllViews(); content.addView(TextView(this).apply { text=t; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(10,10,10,10) }) }

        val menus = listOf("HOME","SALES","RECEIVING","STOCK","SUPPLIERS","REPORTS","SETTINGS","ADMIN")
        for(m in menus) {
            val btn = TextView(this).apply { text=" $m "; setTextColor(0xFFFFFFFF.toInt()); setPadding(15,18,15,18); textSize=12f }
            btn.setOnClickListener {
                when(m) {
                    "SALES" -> showSales()
                    "HOME" -> showHome()
                    else -> showSimple(m)
                }
            }
            drawer.addView(btn)
        }
        scrollContent.addView(content)
        root.addView(drawer); root.addView(scrollContent)
        setContentView(root)
        if(isAdmin) showHome() else showSales()
    }
}
