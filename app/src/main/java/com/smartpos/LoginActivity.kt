package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

class LoginActivity : Activity() {
    lateinit var pref: SharedPreferences
    var stock = mutableMapOf<String, Item>()
    var cart = mutableListOf<Item>()
    var receiveList = mutableListOf<Item>()
    var suppliers = mutableListOf<Supplier>()
    var sales = mutableListOf<Sale>()
    var selectedPay = "Cash"
    data class Item(var name: String, var code: String, var cost: Double, var sell: Double, var qty: Int)
    data class Supplier(var name: String, var phone: String, var balance: Double)
    data class Sale(var date: String, var total: Double, var items: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("pos_v13", 0)
        load(); login()
    }
    fun load() {
        val s = pref.getString("stock", "")?: ""
        if (s.isNotEmpty()) for (row in s.split(";;")) { val p=row.split("|"); if(p.size==5) stock[p[1]]=Item(p[0],p[1],p[2].toDoubleOrNull()?:0.0,p[3].toDoubleOrNull()?:0.0,p[4].toIntOrNull()?:0) }
        if (stock.isEmpty()) { stock["CC500"]=Item("Coca Cola 500ml","CC500",0.7,1.0,50); stock["BRD001"]=Item("White Bread","BRD001",1.2,1.5,20); stock["SGR2"]=Item("Sugar 2kg","SGR2",2.5,3.5,15); stock["MLK001"]=Item("Milk 1 Litre","MLK001",1.2,2.0,20); stock["OIL001"]=Item("Cooking Oil 1 Litre","OIL001",2.8,3.5,10); stock["CBRY001"]=Item("Cereals","CBRY001",2.0,3.0,15) }
        val sl = pref.getString("sales", "")?: ""
        if (sl.isNotEmpty()) for (row in sl.split(";;")) { val p=row.split("||"); if(p.size==3) sales.add(Sale(p[0],p[1].toDoubleOrNull()?:0.0,p[2])) }
    }
    fun saveAll() {
        pref.edit().putString("stock", stock.values.joinToString(";;"){it.name+"|"+it.code+"|"+it.cost+"|"+it.sell+"|"+it.qty}).apply()
        pref.edit().putString("sales", sales.joinToString(";;"){it.date+"||"+it.total+"||"+it.items}).apply()
    }
    fun login() {
        val lay = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(60,200,60,60); gravity=Gravity.CENTER; setBackgroundColor(0xFF0A1931.toInt()) }
        val t = TextView(this).apply { text="SmartShop POS"; textSize=22f; setTextColor(0xFFFFFFFF.toInt()); gravity=Gravity.CENTER; setPadding(0,0,0,20) }
        val pin = EditText(this).apply { hint="PIN 1234"; inputType=129; setBackgroundColor(0xFFFFFFFF.toInt()) }
        val btn = Button(this).apply { text="LOGIN"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        btn.setOnClickListener { if(pin.text.toString()=="1234") dashboard() else Toast.makeText(this,"PIN 1234",Toast.LENGTH_SHORT).show() }
        lay.addView(t); lay.addView(pin); lay.addView(btn); setContentView(lay)
    }

    fun dashboard() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setBackgroundColor(0xFFF1F5F9.toInt()) }
        val drawer = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFF0A1931.toInt()); layoutParams=LinearLayout.LayoutParams(220, LinearLayout.LayoutParams.MATCH_PARENT) }
        val scrollContent = ScrollView(this).apply { layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f) }
        val content = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(6,6,6,6) }

        fun showSales() {
            content.removeAllViews()
            // TOP
            val top = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(0,0,0,6) }
            val eSearch = EditText(this).apply { hint="Scan barcode or enter product name / code..."; setBackgroundColor(0xFFFFFFFF.toInt()); textSize=14f }
            val btnSearch = Button(this).apply { text="SEARCH"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            top.addView(eSearch, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0,0,6,0) })
            top.addView(btnSearch)
            content.addView(top)

            // SUGGESTION BOX - shows matching products
            val tvSuggest = TextView(this).apply { text=""; setBackgroundColor(0xFFFFFF99.toInt()); setPadding(10,10,10,10); visibility=8 }
            content.addView(tvSuggest)

            // HEADER
            val header = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setBackgroundColor(0xFFE2E8F0.toInt()); setPadding(4,8,4,8) }
            fun hdr(t: String, w: Float): TextView { return TextView(this).apply { text=t; textSize=10f; setPadding(2,0,2,0); layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w) } }
            header.addView(hdr("#",0.3f)); header.addView(hdr("Product Name",2f)); header.addView(hdr("Code",1f)); header.addView(hdr("Qty",1.2f)); header.addView(hdr("Price",0.8f)); header.addView(hdr("Total",0.8f)); header.addView(hdr("X",0.4f))
            content.addView(header)

            val cartBox = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFFFFFFFF.toInt()) }
            val svCart = ScrollView(this).apply { layoutParams=LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 500) }
            svCart.addView(cartBox)
            content.addView(svCart)

            // SUMMARY
            val sumBox = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(8,8,8,8) }
            val tvSub = TextView(this).apply { text="Subtotal 0.00"; gravity=Gravity.RIGHT }
            val tvTax = TextView(this).apply { text="Tax 0.00"; gravity=Gravity.RIGHT }
            val tvTot = TextView(this).apply { text="Total 0.00"; textSize=18f; setBackgroundColor(0xFF1E3A5F.toInt()); setTextColor(0xFFFFFFFF.toInt()); setPadding(10,10,10,10); gravity=Gravity.RIGHT }
            val tvPayLabel = TextView(this).apply { text="Payment Method"; setPadding(0,8,0,4) }
            val payRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
            val bCash = Button(this).apply { text="Cash"; setBackgroundColor(0xFF16A34A.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            val bEco = Button(this).apply { text="EcoCash"; setBackgroundColor(0xFF7C3AED.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            payRow.addView(bCash, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0,0,4,0) })
            payRow.addView(bEco, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val tvRecLabel = TextView(this).apply { text="Amount Received"; textSize=12f; setPadding(0,8,0,0) }
            val eRec = EditText(this).apply { hint="20.00"; inputType=8194 }
            val tvChangeLabel = TextView(this).apply { text="Change"; textSize=12f }
            val tvChange = TextView(this).apply { text="0.00"; setBackgroundColor(0xFFDCFCE7.toInt()); setPadding(8,8,8,8); gravity=Gravity.RIGHT }
            val bComplete = Button(this).apply { text="Complete Sale"; setBackgroundColor(0xFF16A34A.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            sumBox.addView(tvSub); sumBox.addView(tvTax); sumBox.addView(tvTot); sumBox.addView(tvPayLabel); sumBox.addView(payRow); sumBox.addView(tvRecLabel); sumBox.addView(eRec); sumBox.addView(tvChangeLabel); sumBox.addView(tvChange); sumBox.addView(bComplete)
            content.addView(sumBox)

            fun refreshCart() {
                cartBox.removeAllViews()
                var sub=0.0
                for((idx, item) in cart.withIndex()) {
                    val total=item.sell*item.qty; sub+=total
                    val row = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(2,8,2,8) }
                    val tvIdx = TextView(this).apply { text="${idx+1}"; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f) }
                    val tvName = TextView(this).apply { text=item.name; textSize=11f; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f) }
                    val tvCode = TextView(this).apply { text=item.code; textSize=10f; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
                    val qtyBox = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f) }
                    val bm = Button(this).apply { text="-"; textSize=10f }
                    val tq = TextView(this).apply { text="${item.qty}"; gravity=Gravity.CENTER; setPadding(6,0,6,0) }
                    val bp = Button(this).apply { text="+"; textSize=10f }
                    qtyBox.addView(bm); qtyBox.addView(tq); qtyBox.addView(bp)
                    val tvPrice = TextView(this).apply { text="${item.sell}"; gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f) }
                    val tvT = TextView(this).apply { text="${String.format("%.2f", total)}"; gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f) }
                    val bd = Button(this).apply { text="X"; textSize=10f; setBackgroundColor(0xFFFFE2E2.toInt()) }
                    bm.setOnClickListener { if(item.qty>1) { item.qty--; refreshCart() } }
                    bp.setOnClickListener { item.qty++; refreshCart() }
                    bd.setOnClickListener { cart.remove(item); refreshCart() }
                    row.addView(tvIdx); row.addView(tvName); row.addView(tvCode); row.addView(qtyBox); row.addView(tvPrice); row.addView(tvT); row.addView(bd)
                    cartBox.addView(row)
                }
                tvSub.text="Subtotal ${String.format("%.2f", sub)}"
                tvTot.text="Total ${String.format("%.2f", sub)}"
                val rec=eRec.text.toString().toDoubleOrNull()?:0.0
                tvChange.text=String.format("%.2f", rec-sub)
                if(sub==0.0) cartBox.addView(TextView(this).apply { text="Cart empty - search product"; gravity=Gravity.CENTER; setPadding(0,30,0,30) })
            }

            fun doSearch() {
                val q=eSearch.text.toString().trim().lowercase()
                if(q.isEmpty()) return
                val found=stock.values.filter { it.code.lowercase().contains(q) || it.name.lowercase().contains(q) }
                if(found.isEmpty()) { Toast.makeText(this,"No product found: $q",Toast.LENGTH_SHORT).show(); return }
                if(found.size==1) {
                    val st=found[0]
                    val ex=cart.find { it.code==st.code }
                    if(ex!=null) ex.qty++ else cart.add(Item(st.name,st.code,st.cost,st.sell,1))
                    eSearch.text.clear(); refreshCart(); Toast.makeText(this,"Added ${st.name}",Toast.LENGTH_SHORT).show()
                } else {
                    // Show list to pick
                    var txt="Found ${found.size} - tap code to add:\n"
                    for(f in found.take(5)) txt+="${f.code} - ${f.name} $${f.sell} (Stock ${f.qty})\n"
                    tvSuggest.text=txt; tvSuggest.visibility=0
                    // Auto add first for speed
                    val st=found[0]
                    val ex=cart.find { it.code==st.code }
                    if(ex!=null) ex.qty++ else cart.add(Item(st.name,st.code,st.cost,st.sell,1))
                    eSearch.text.clear(); refreshCart()
                }
            }

            btnSearch.setOnClickListener { doSearch() }
            eSearch.setOnEditorActionListener { _, _, _ -> doSearch(); true }
            bCash.setOnClickListener { selectedPay="Cash"; bCash.setBackgroundColor(0xFF15803D.toInt()); bEco.setBackgroundColor(0xFF7C3AED.toInt()) }
            bEco.setOnClickListener { selectedPay="EcoCash"; bEco.setBackgroundColor(0xFF5B21B6.toInt()); bCash.setBackgroundColor(0xFF16A34A.toInt()) }
            bComplete.setOnClickListener {
                if(cart.isEmpty()) return@setOnClickListener
                val tot=cart.sumOf { it.sell*it.qty }
                val rec=eRec.text.toString().toDoubleOrNull()?:0.0
                if(rec < tot) { Toast.makeText(this,"Received $rec < Total $tot",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                val date=SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                sales.add(Sale(date,tot,cart.joinToString(","){ "${it.name}x${it.qty}" }))
                for(c in cart) stock[c.code]?.qty = (stock[c.code]?.qty?:0) - c.qty
                saveAll()
                Toast.makeText(this,"SOLD $${String.format("%.2f", tot)} Change $${String.format("%.2f", rec-tot)} $selectedPay",Toast.LENGTH_LONG).show()
                cart.clear(); eRec.text.clear(); refreshCart()
            }
            refreshCart()
        }

        fun showReceiving() {
            content.removeAllViews()
            content.addView(TextView(this).apply { text="RECEIVING - Date Auto ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}"; textSize=16f })
            val eN=EditText(this).apply { hint="Product Name" }
            val eC=EditText(this).apply { hint="Code" }
            val eCo=EditText(this).apply { hint="Cost"; inputType=8194 }
            val eSe=EditText(this).apply { hint="Sell"; inputType=8194 }
            val eQ=EditText(this).apply { hint="Qty"; inputType=2 }
            val b=Button(this).apply { text="Add + Post"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            b.setOnClickListener { if(eN.text.isNotEmpty()&&eC.text.isNotEmpty()){ val co=eCo.text.toString().toDoubleOrNull()?:0.0; val se=eSe.text.toString().toDoubleOrNull()?:0.0; val q=eQ.text.toString().toIntOrNull()?:1; val ex=stock[eC.text.toString().uppercase()]; if(ex!=null){ex.qty+=q; ex.cost=co; ex.sell=se}else stock[eC.text.toString().uppercase()]=Item(eN.text.toString(),eC.text.toString().uppercase(),co,se,q); saveAll(); Toast.makeText(this,"Posted",Toast.LENGTH_SHORT).show(); eN.text.clear(); eC.text.clear(); eCo.text.clear(); eSe.text.clear(); eQ.text.clear()} }
            content.addView(eN); content.addView(eC); content.addView(eCo); content.addView(eSe); content.addView(eQ); content.addView(b)
        }
        fun simple(title: String){ content.removeAllViews(); content.addView(TextView(this).apply { text=title; textSize=18f; setPadding(10,10,10,10) }) }

        val menus = listOf("HOME" to {simple("HOME\nToday Sales ${sales.filter { it.date==SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }.sumOf { it.total }}")}, "SALES" to {showSales()}, "RECEIVING" to {showReceiving()}, "STOCK" to {simple("STOCK\n"+stock.values.joinToString("\n"){ "${it.code} ${it.name} Qty ${it.qty} $${it.sell}" })}, "SUPPLIERS" to {simple("SUPPLIERS")}, "REPORTS" to {simple("REPORTS\nTotal ${sales.size} sales $${sales.sumOf { it.total }}")}, "SETTINGS" to {simple("SETTINGS")}, "ADMIN" to {simple("ADMIN")})
        for((name, fn) in menus){
            val btn = TextView(this).apply { text=" $name "; setTextColor(0xFFFFFFFF.toInt()); setPadding(12,18,12,18); textSize=12f; setBackgroundColor(if(name=="SALES") 0xFF185ADB.toInt() else 0x00000000) }
            btn.setOnClickListener { for(i in 0 until drawer.childCount) (drawer.getChildAt(i) as? TextView)?.setBackgroundColor(0x00000000); btn.setBackgroundColor(0xFF185ADB.toInt()); fn() }
            drawer.addView(btn)
        }
        scrollContent.addView(content)
        root.addView(drawer); root.addView(scrollContent)
        setContentView(root)
        showSales()
    }
}
