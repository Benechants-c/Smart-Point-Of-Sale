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
    var receiveList = mutableListOf<Item>()
    var suppliers = mutableListOf<Supplier>()
    var sales = mutableListOf<Sale>()
    var isAuto = false
    var selectedPay = "Cash"
    data class Item(var name: String, var code: String, var cost: Double, var sell: Double, var qty: Int)
    data class Supplier(var name: String, var phone: String, var balance: Double)
    data class Sale(var date: String, var total: Double, var items: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("pos_v12_sale", 0)
        load(); login()
    }
    fun load() {
        val s = pref.getString("stock", "")?: ""
        if (s.isNotEmpty()) for (row in s.split(";;")) { val p=row.split("|"); if(p.size==5) stock[p[1]]=Item(p[0],p[1],p[2].toDoubleOrNull()?:0.0,p[3].toDoubleOrNull()?:0.0,p[4].toIntOrNull()?:0) }
        if (stock.isEmpty()) { stock["CC500"]=Item("Coca Cola 500ml","CC500",0.7,1.0,50); stock["BRD001"]=Item("White Bread","BRD001",1.2,1.5,20); stock["SGR2"]=Item("Sugar 2kg","SGR2",2.5,3.5,15); stock["MLK001"]=Item("Milk 1 Litre","MLK001",1.2,2.0,20); stock["OIL001"]=Item("Cooking Oil 1 Litre","OIL001",2.8,3.5,10) }
        val sl = pref.getString("sales", "")?: ""
        if (sl.isNotEmpty()) for (row in sl.split(";;")) { val p=row.split("||"); if(p.size==3) sales.add(Sale(p[0],p[1].toDoubleOrNull()?:0.0,p[2])) }
        val sup = pref.getString("sups", "")?: ""
        if (sup.isNotEmpty()) for (row in sup.split(";;")) { val p=row.split("|"); if(p.size==3) suppliers.add(Supplier(p[0],p[1],p[2].toDoubleOrNull()?:0.0)) }
        if (suppliers.isEmpty()) suppliers.add(Supplier("Harare Wholesalers","0771234567",0.0))
    }
    fun saveAll() {
        pref.edit().putString("stock", stock.values.joinToString(";;"){it.name+"|"+it.code+"|"+it.cost+"|"+it.sell+"|"+it.qty}).apply()
        pref.edit().putString("sales", sales.joinToString(";;"){it.date+"||"+it.total+"||"+it.items}).apply()
        pref.edit().putString("sups", suppliers.joinToString(";;"){it.name+"|"+it.phone+"|"+it.balance}).apply()
    }
    fun login() {
        val lay = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(60,250,60,60); gravity=Gravity.CENTER; setBackgroundColor(0xFF0A1931.toInt()) }
        val t = TextView(this).apply { text="SmartShop POS\nSHOP001-HRE"; textSize=22f; setTextColor(0xFFFFFFFF.toInt()); gravity=Gravity.CENTER }
        val pin = EditText(this).apply { hint="PIN 1234"; inputType=129; setBackgroundColor(0xFFFFFFFF.toInt()) }
        val btn = Button(this).apply { text="LOGIN"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        btn.setOnClickListener { if(pin.text.toString()=="1234") dashboard() else Toast.makeText(this,"PIN 1234",Toast.LENGTH_SHORT).show() }
        lay.addView(t); lay.addView(pin); lay.addView(btn); setContentView(lay)
    }

    fun dashboard() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        val drawer = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFF0A1931.toInt()); setPadding(0,10,0,10) }
        val scrollContent = ScrollView(this)
        val content = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(8,8,8,8); setBackgroundColor(0xFFF8FAFC.toInt()) }

        fun showHome() {
            content.removeAllViews()
            val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val todaySales = sales.filter { it.date==today }.sumOf { it.total }
            content.addView(TextView(this).apply { text="HOME\n$today\n\nToday Sales: $${String.format("%.2f", todaySales)}\nTotal Sales: ${sales.size}\nProducts: ${stock.size}\nStock Value: $${String.format("%.2f", stock.values.sumOf { it.cost*it.qty })}"; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(15,15,15,15) })
            val low=stock.values.filter { it.qty<10 }.joinToString("\n"){ "${it.name} ${it.code} Qty ${it.qty}" }
            content.addView(TextView(this).apply { text="Low Stock (<10):\n${if(low.isEmpty()) "None" else low}"; setBackgroundColor(0xFFFFF3CD.toInt()); setPadding(10,10,10,10) })
        }

        fun showSales() {
            content.removeAllViews()
            // TOP SEARCH BAR LIKE PHOTO
            val topRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
            val eSearch = EditText(this).apply { hint="Scan barcode or enter product name / code..."; setBackgroundColor(0xFFFFFFFF.toInt()) }
            val btnSearch = Button(this).apply { text="🔍 Search"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            topRow.addView(eSearch, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            topRow.addView(btnSearch)
            content.addView(topRow)

            // MAIN SPLIT: LEFT TABLE + RIGHT SUMMARY
            val mainRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
            val left = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFFFFFFFF.toInt()) }
            val right = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(8,8,8,8) }

            // TABLE HEADER
            val header = TextView(this).apply { text="# Product Name Code Qty Price Total"; setBackgroundColor(0xFFF1F5F9.toInt()); setPadding(8,10,8,10); textSize=12f }
            left.addView(header)
            val tvCart = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
            left.addView(tvCart)

            // RIGHT SUMMARY LIKE PHOTO
            right.addView(TextView(this).apply { text="Sale Summary"; textSize=16f; setBackgroundColor(0xFFE2E8F0.toInt()); setPadding(10,10,10,10) })
            val tvSubtotal = TextView(this).apply { text="Subtotal 0.00"; gravity=Gravity.RIGHT; setPadding(5,10,5,5) }
            val tvTax = TextView(this).apply { text="Tax 0.00"; gravity=Gravity.RIGHT; setPadding(5,5,5,5) }
            val tvTotal = TextView(this).apply { text="Total 0.00"; textSize=18f; setBackgroundColor(0xFF1E3A5F.toInt()); setTextColor(0xFFFFFFFF.toInt()); setPadding(10,12,10,12); gravity=Gravity.RIGHT }
            right.addView(tvSubtotal); right.addView(tvTax); right.addView(tvTotal)

            right.addView(TextView(this).apply { text="Payment Method"; textSize=14f; setPadding(0,15,0,5) })
            val payRow1 = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
            val btnCash = Button(this).apply { text="💲 Cash"; setBackgroundColor(0xFF16A34A.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            val btnEco = Button(this).apply { text="📱 EcoCash"; setBackgroundColor(0xFF7C3AED.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            payRow1.addView(btnCash, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            payRow1.addView(btnEco, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            right.addView(payRow1)

            right.addView(TextView(this).apply { text="Amount Received"; setPadding(0,15,0,0); textSize=12f })
            val eReceived = EditText(this).apply { hint="0.00"; inputType=8194; setBackgroundColor(0xFFF8FAFC.toInt()) }
            right.addView(eReceived)
            right.addView(TextView(this).apply { text="Change"; setPadding(0,10,0,0); textSize=12f })
            val tvChange = TextView(this).apply { text="0.00"; setBackgroundColor(0xFFDCFCE7.toInt()); setPadding(10,8,10,8); gravity=Gravity.RIGHT; setTextColor(0xFF15803D.toInt()) }
            right.addView(tvChange)

            val btnComplete = Button(this).apply { text="✔ Complete Sale"; setBackgroundColor(0xFF16A34A.toInt()); setTextColor(0xFFFFFFFF.toInt()); textSize=16f }
            right.addView(btnComplete)

            fun refresh() {
                tvCart.removeAllViews()
                if(cart.isEmpty()) { tvCart.addView(TextView(this).apply { text="No items"; setPadding(10,20,10,20) }); tvSubtotal.text="Subtotal 0.00"; tvTotal.text="Total 0.00"; tvChange.text="0.00"; return }
                var sub=0.0; var idx=1
                for(item in cart) {
                    val row = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setPadding(4,6,4,6) }
                    val t = item.sell * item.qty; sub+=t
                    val tv1 = TextView(this).apply { text="$idx ${item.name}\n ${item.code}"; textSize=11f; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.8f) }
                    val btnMinus = Button(this).apply { text="-"; textSize=10f }
                    val tvQty = TextView(this).apply { text="${item.qty}"; gravity=Gravity.CENTER; setPadding(8,0,8,0); setBackgroundColor(0xFFF1F5F9.toInt()) }
                    val btnPlus = Button(this).apply { text="+"; textSize=10f }
                    val tvPrice = TextView(this).apply { text="${String.format("%.2f", item.sell)}"; gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f) }
                    val tvT = TextView(this).apply { text="${String.format("%.2f", t)}"; gravity=Gravity.CENTER; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f) }
                    val btnDel = Button(this).apply { text="🗑️"; textSize=10f }

                    btnMinus.setOnClickListener { if(item.qty>1) { item.qty--; refresh() } }
                    btnPlus.setOnClickListener { if((stock[item.code]?.qty?:0) >= item.qty+1) { item.qty++; refresh() } }
                    btnDel.setOnClickListener { cart.remove(item); refresh() }

                    row.addView(tv1); row.addView(btnMinus); row.addView(tvQty); row.addView(btnPlus); row.addView(tvPrice); row.addView(tvT); row.addView(btnDel)
                    tvCart.addView(row); idx++
                }
                tvSubtotal.text="Subtotal ${String.format("%.2f", sub)}"
                tvTotal.text="Total ${String.format("%.2f", sub)}"
                val rec = eReceived.text.toString().toDoubleOrNull()?:0.0
                tvChange.text=String.format("%.2f", rec - sub)
            }

            fun addItem(code: String) {
                val st=stock[code.uppercase()]?: return
                val ex=cart.find { it.code==code.uppercase() }
                if(ex!=null) { if(st.qty >= ex.qty+1) ex.qty++ } else cart.add(Item(st.name,st.code,st.cost,st.sell,1))
                refresh()
            }

            eSearch.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val txt=s.toString().trim()
                    if(txt.length>=2) { for(it in stock.values) if(it.code.equals(txt,true) || it.name.lowercase().contains(txt.lowercase())) { addItem(it.code); eSearch.text.clear(); break } }
                }
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            })
            btnSearch.setOnClickListener { val txt=eSearch.text.toString().trim(); if(txt.isNotEmpty()) { for(it in stock.values) if(it.code.equals(txt,true) || it.name.lowercase().contains(txt.lowercase())) { addItem(it.code); eSearch.text.clear(); break } } }
            eReceived.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) { refresh() }
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            })
            btnCash.setOnClickListener { selectedPay="Cash"; btnCash.setBackgroundColor(0xFF15803D.toInt()); btnEco.setBackgroundColor(0xFF7C3AED.toInt()) }
            btnEco.setOnClickListener { selectedPay="EcoCash"; btnEco.setBackgroundColor(0xFF5B21B6.toInt()); btnCash.setBackgroundColor(0xFF16A34A.toInt()) }

            btnComplete.setOnClickListener {
                if(cart.isEmpty()) return@setOnClickListener
                val total=cart.sumOf { it.sell*it.qty }
                val received=eReceived.text.toString().toDoubleOrNull()?:0.0
                if(received < total) { Toast.makeText(this,"Amount received less than total",Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                val date=SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                sales.add(Sale(date,total,cart.joinToString(","){ "${it.name} x${it.qty}" }+" | $selectedPay"))
                for(c in cart) stock[c.code]?.qty = (stock[c.code]?.qty?:0) - c.qty
                saveAll()
                Toast.makeText(this,"Sale $${String.format("%.2f", total)} - $selectedPay - Change $${String.format("%.2f", received-total)}",Toast.LENGTH_LONG).show()
                cart.clear(); eReceived.text.clear(); refresh()
            }

            mainRow.addView(left, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f))
            mainRow.addView(right, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            content.addView(mainRow)
            refresh()
        }

        fun showReceiving() {
            content.removeAllViews()
            val autoDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            content.addView(TextView(this).apply { text="RECEIVING (USD) Date (Auto): $autoDate"; textSize=16f; setPadding(0,0,0,10) })
            val sp = Spinner(this)
            sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, suppliers.map { it.name })
            val eName = EditText(this).apply { hint="Product Name" }
            val eCode = EditText(this).apply { hint="Code" }
            val eCost = EditText(this).apply { hint="Cost (USD)"; inputType=8194 }
            val eSell = EditText(this).apply { hint="Selling Price (USD)"; inputType=8194 }
            val eQty = EditText(this).apply { hint="Qty"; inputType=2 }
            val btnAdd = Button(this).apply { text="+ ADD ITEM"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            val tvTable = TextView(this).apply { text="No items"; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(8,8,8,8) }
            val tvGrand = TextView(this).apply { text="Grand Total: $0.00"; gravity=Gravity.RIGHT }
            val bPost = Button(this).apply { text="POST RECEIVING"; setBackgroundColor(0xFF16A34A.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            fun refreshR() { if(receiveList.isEmpty()) { tvTable.text="No items"; tvGrand.text="Grand Total: $0.00"; return }; var txt=""; var g=0.0; var i=1; for(it in receiveList){ val t=it.cost*it.qty; txt+="$i. ${it.name} ${it.code} x${it.qty} = $${String.format("%.2f", t)}\n"; g+=t; i++ }; tvTable.text=txt; tvGrand.text="Grand Total: $${String.format("%.2f", g)}" }
            btnAdd.setOnClickListener { if(eName.text.isEmpty()||eCode.text.isEmpty()||eCost.text.isEmpty()||eSell.text.isEmpty()||eQty.text.isEmpty()) return@setOnClickListener; receiveList.add(Item(eName.text.toString(),eCode.text.toString().uppercase(),eCost.text.toString().toDouble(),eSell.text.toString().toDouble(),eQty.text.toString().toInt())); refreshR(); eName.text.clear(); eCode.text.clear(); eCost.text.clear(); eSell.text.clear(); eQty.text.clear() }
            bPost.setOnClickListener { if(receiveList.isEmpty()) return@setOnClickListener; for(it in receiveList){ val ex=stock[it.code]; if(ex!=null){ ex.qty+=it.qty; ex.cost=it.cost; ex.sell=it.sell } else stock[it.code]=it }; saveAll(); Toast.makeText(this,"Posted on $autoDate",Toast.LENGTH_LONG).show(); receiveList.clear(); refreshR() }
            content.addView(TextView(this).apply { text="Supplier"; textSize=12f }); content.addView(sp); content.addView(eName); content.addView(eCode); content.addView(eCost); content.addView(eSell); content.addView(eQty); content.addView(btnAdd); content.addView(tvTable); content.addView(tvGrand); content.addView(bPost)
        }

        fun showStock() { content.removeAllViews(); var txt="CODE | NAME | COST | SELL | QTY\n---------------------------\n"; for(it in stock.values) txt+="${it.code} | ${it.name} | $${it.cost} | $${it.sell} | ${it.qty}\n"; content.addView(TextView(this).apply { text=txt; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(10,10,10,10) }) }
        fun showSuppliers() { content.removeAllViews(); content.addView(TextView(this).apply { text=suppliers.joinToString("\n"){ "${it.name} - ${it.phone}" }; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(10,10,10,10) }) }
        fun showReports() { content.removeAllViews(); content.addView(TextView(this).apply { text="Total Sales: ${sales.size}\nValue: $${String.format("%.2f", sales.sumOf { it.total })}\n\n${sales.takeLast(10).joinToString("\n"){ "${it.date} $${it.total} ${it.items}" }}"; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(10,10,10,10) }) }
        fun showSettings() { content.removeAllViews(); content.addView(TextView(this).apply { text="SETTINGS\nSHOP001-HRE\nUSD\nPIN 1234"; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(10,10,10,10) }) }
        fun showAdmin() { content.removeAllViews(); content.addView(TextView(this).apply { text="Admin - Clear Sales / Stock"; setPadding(10,10,10,10) }) }

        val menu = arrayOf("HOME","SALES","RECEIVING","STOCK","SUPPLIERS","REPORTS","SETTINGS","ADMIN")
        for(m in menu) {
            val btn = TextView(this).apply { text=" $m "; setTextColor(0xFFFFFFFF.toInt()); setPadding(15,18,15,18); textSize=12f; setBackgroundColor(if(m=="SALES") 0xFF185ADB.toInt() else 0x00000000) }
            btn.setOnClickListener {
                for(i in 0 until drawer.childCount) { val v=drawer.getChildAt(i); if(v is TextView) v.setBackgroundColor(0x00000000) }
                btn.setBackgroundColor(0xFF185ADB.toInt())
                when(m) {
                    "HOME" -> showHome()
                    "SALES" -> showSales()
                    "RECEIVING" -> showReceiving()
                    "STOCK" -> showStock()
                    "SUPPLIERS" -> showSuppliers()
                    "REPORTS" -> showReports()
                    "SETTINGS" -> showSettings()
                    "ADMIN" -> showAdmin()
                }
            }
            drawer.addView(btn)
        }
        scrollContent.addView(content)
        root.addView(drawer); root.addView(scrollContent)
        setContentView(root)
        showSales()
    }
}
