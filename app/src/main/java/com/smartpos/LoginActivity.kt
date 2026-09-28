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

    data class Item(var name: String, var code: String, var cost: Double, var sell: Double, var qty: Int)
    data class Supplier(var name: String, var phone: String, var balance: Double)
    data class Sale(var date: String, var total: Double, var items: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("pos_full_v11", 0)
        load()
        login()
    }

    fun load() {
        val s = pref.getString("stock", "")?: ""
        if (s.isNotEmpty()) for (row in s.split(";;")) { val p=row.split("|"); if(p.size==5) stock[p[1]]=Item(p[0],p[1],p[2].toDoubleOrNull()?:0.0,p[3].toDoubleOrNull()?:0.0,p[4].toIntOrNull()?:0) }
        if (stock.isEmpty()) { stock["BRD001"]=Item("White Bread","BRD001",1.2,2.0,20); stock["OIL001"]=Item("Cooking Oil 1L","OIL001",3.0,4.5,15); stock["SUG001"]=Item("Sugar 2kg","SUG001",2.0,3.0,8) }

        val sup = pref.getString("sups", "")?: ""
        if (sup.isNotEmpty()) for (row in sup.split(";;")) { val p=row.split("|"); if(p.size==3) suppliers.add(Supplier(p[0],p[1],p[2].toDoubleOrNull()?:0.0) ) }
        if (suppliers.isEmpty()) suppliers.add(Supplier("Harare Wholesalers","0771234567",0.0))

        val sl = pref.getString("sales", "")?: ""
        if (sl.isNotEmpty()) for (row in sl.split(";;")) { val p=row.split("||"); if(p.size==3) sales.add(Sale(p[0],p[1].toDoubleOrNull()?:0.0,p[2])) }
    }

    fun saveAll() {
        val st=stock.values.joinToString(";;"){it.name+"|"+it.code+"|"+it.cost+"|"+it.sell+"|"+it.qty}
        pref.edit().putString("stock", st).apply()
        val sup=suppliers.joinToString(";;"){it.name+"|"+it.phone+"|"+it.balance}
        pref.edit().putString("sups", sup).apply()
        val sl=sales.joinToString(";;"){it.date+"||"+it.total+"||"+it.items}
        pref.edit().putString("sales", sl).apply()
    }

    fun login() {
        val lay = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(60,250,60,60); gravity=Gravity.CENTER; setBackgroundColor(0xFF0A1931.toInt()) }
        val t = TextView(this).apply { text="SmartShop POS\nSHOP001-HRE"; textSize=22f; setTextColor(0xFFFFFFFF.toInt()); gravity=Gravity.CENTER; setPadding(0,0,0,40) }
        val pin = EditText(this).apply { hint="PIN 1234"; inputType=129; setBackgroundColor(0xFFFFFFFF.toInt()) }
        val btn = Button(this).apply { text="LOGIN"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        btn.setOnClickListener { if(pin.text.toString()=="1234") dashboard() else Toast.makeText(this,"PIN 1234",Toast.LENGTH_SHORT).show() }
        lay.addView(t); lay.addView(pin); lay.addView(btn); setContentView(lay)
    }

    fun dashboard() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        val drawer = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFF0A1931.toInt()); setPadding(0,10,0,10) }
        val scrollContent = ScrollView(this)
        val content = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(12,12,12,12); setBackgroundColor(0xFFF8FAFC.toInt()) }

        fun showHome() {
            content.removeAllViews()
            val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val todaySales = sales.filter { it.date==today }.sumOf { it.total }
            val totalStockVal = stock.values.sumOf { it.cost * it.qty }
            val low = stock.values.filter { it.qty < 10 }

            content.addView(TextView(this).apply { text="HOME - Main Dashboard\n$today"; textSize=18f; setPadding(0,0,0,15) })
            content.addView(TextView(this).apply { text="Today's Sales: $${String.format("%.2f", todaySales)}\nTotal Sales: ${sales.size}\nStock Value (Cost): $${String.format("%.2f", totalStockVal)}\nProducts: ${stock.size}"; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(15,15,15,15) })
            content.addView(TextView(this).apply { text="Low Stock Alerts (<10):"; textSize=14f; setPadding(0,15,0,5) })
            var lowTxt = if(low.isEmpty()) "No low stock" else low.joinToString("\n"){ "${it.name} ${it.code} - Qty ${it.qty}" }
            content.addView(TextView(this).apply { text=lowTxt; setBackgroundColor(0xFFFFF3CD.toInt()); setPadding(10,10,10,10) })
        }

        fun showSales() {
            content.removeAllViews()
            content.addView(TextView(this).apply { text="SALES - Point of Sale"; textSize=18f; setPadding(0,0,0,10) })
            val eSearch = EditText(this).apply { hint="Scan or Search Code / Name" }
            val tvCart = TextView(this).apply { text="Cart: Empty"; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(10,10,10,10) }
            val tvTotal = TextView(this).apply { text="Total: $0.00"; textSize=18f; gravity=Gravity.RIGHT; setPadding(0,10,0,10) }
            val eQty = EditText(this).apply { hint="Qty"; inputType=2; setText("1") }
            val btnAdd = Button(this).apply { text="Add to Cart"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            val btnPay = Button(this).apply { text="PAY / COMPLETE SALE"; setBackgroundColor(0xFF16A34A.toInt()); setTextColor(0xFFFFFFFF.toInt()) }

            fun refreshCart() {
                if(cart.isEmpty()) { tvCart.text="Cart: Empty"; tvTotal.text="Total: $0.00"; return }
                var txt=""; var tot=0.0; var i=1
                for(it in cart) { val t=it.sell*it.qty; txt+="$i. ${it.name} ${it.code} x${it.qty} @ $${it.sell} = $${String.format("%.2f", t)}\n"; tot+=t; i++ }
                tvCart.text=txt; tvTotal.text="Total: $${String.format("%.2f", tot)}"
            }

            fun addToCartByCode(code: String) {
                val item=stock[code.uppercase()]?: return
                val q=eQty.text.toString().toIntOrNull()?:1
                if(item.qty < q) { Toast.makeText(this,"Stock only ${item.qty}",Toast.LENGTH_SHORT).show(); return }
                val ex=cart.find { it.code==code.uppercase() }
                if(ex!=null) ex.qty+=q else cart.add(Item(item.name,item.code,item.cost,item.sell,q))
                refreshCart()
            }

            eSearch.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    val txt=s.toString().trim()
                    if(txt.length<2) return
                    for(it in stock.values) if(it.code.equals(txt,true) || it.name.lowercase().contains(txt.lowercase())) { addToCartByCode(it.code); eSearch.text.clear(); break }
                }
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            })
            btnAdd.setOnClickListener { val code=eSearch.text.toString(); if(code.isNotEmpty()) addToCartByCode(code) }
            btnPay.setOnClickListener {
                if(cart.isEmpty()) return@setOnClickListener
                val total=cart.sumOf { it.sell*it.qty }
                val date=SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                val items=cart.joinToString(","){ "${it.name} x${it.qty}" }
                for(c in cart) { stock[c.code]?.qty = (stock[c.code]?.qty?:0) - c.qty }
                sales.add(Sale(date,total,items))
                saveAll()
                Toast.makeText(this,"Sale $${String.format("%.2f", total)} Complete",Toast.LENGTH_LONG).show()
                cart.clear(); refreshCart()
            }

            content.addView(eSearch); content.addView(eQty); content.addView(btnAdd); content.addView(tvCart); content.addView(tvTotal); content.addView(btnPay)
        }

        fun showReceiving() {
            content.removeAllViews()
            val autoDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            content.addView(TextView(this).apply { text="RECEIVING (USD)"; textSize=18f; setPadding(0,0,0,10) })
            content.addView(TextView(this).apply { text="Date (Auto): $autoDate"; textSize=14f; setPadding(0,0,0,10) })

            val sp = Spinner(this)
            val supNames = suppliers.map { it.name }.toMutableList()
            if(supNames.isEmpty()) supNames.add("Select Supplier")
            sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, supNames)

            val eName = EditText(this).apply { hint="Product Name" }
            val eCode = EditText(this).apply { hint="Code" }
            val eCost = EditText(this).apply { hint="Cost (USD)"; inputType=8194 }
            val eSell = EditText(this).apply { hint="Selling Price (USD)"; inputType=8194 }
            val eQty = EditText(this).apply { hint="Qty"; inputType=2 }

            eCode.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) { if(isAuto) return; val code=s.toString().trim().uppercase(); if(stock.containsKey(code)) { isAuto=true; val it=stock[code]!!; eName.setText(it.name); eCost.setText(it.cost.toString()); eSell.setText(it.sell.toString()); isAuto=false } }
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            })
            eName.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) { if(isAuto) return; val n=s.toString().trim().lowercase(); if(n.length<2) return; for(item in stock.values) if(item.name.lowercase().contains(n)) { isAuto=true; eCode.setText(item.code); eCost.setText(item.cost.toString()); eSell.setText(item.sell.toString()); isAuto=false; break } }
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            })

            val btnAdd = Button(this).apply { text="+ Add Item"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            val tvTable = TextView(this).apply { text="No items"; setPadding(0,10,0,10); setBackgroundColor(0xFFFFFFFF.toInt()) }
            val tvGrand = TextView(this).apply { text="Grand Total: $0.00"; gravity=Gravity.RIGHT }
            fun refresh() { if(receiveList.isEmpty()) { tvTable.text="No items"; tvGrand.text="Grand Total: $0.00"; return }; var txt=""; var g=0.0; var i=1; for(it in receiveList){ val t=it.cost*it.qty; txt+="$i. ${it.name} ${it.code} x${it.qty} = $${String.format("%.2f", t)}\n"; g+=t; i++ }; tvTable.text=txt; tvGrand.text="Grand Total: $${String.format("%.2f", g)}" }
            btnAdd.setOnClickListener { if(eName.text.isEmpty()||eCode.text.isEmpty()||eCost.text.isEmpty()||eSell.text.isEmpty()||eQty.text.isEmpty()) return@setOnClickListener; receiveList.add(Item(eName.text.toString(),eCode.text.toString().uppercase(),eCost.text.toString().toDouble(),eSell.text.toString().toDouble(),eQty.text.toString().toInt())); refresh(); eName.text.clear(); eCode.text.clear(); eCost.text.clear(); eSell.text.clear(); eQty.text.clear() }
            val bPost = Button(this).apply { text="Post Receiving"; setBackgroundColor(0xFF16A34A.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            bPost.setOnClickListener { if(receiveList.isEmpty()) return@setOnClickListener; for(it in receiveList){ val ex=stock[it.code]; if(ex!=null){ ex.qty+=it.qty; ex.cost=it.cost; ex.sell=it.sell } else stock[it.code]=it }; saveAll(); Toast.makeText(this,"Posted on $autoDate",Toast.LENGTH_LONG).show(); receiveList.clear(); refresh() }

            content.addView(TextView(this).apply { text="Supplier"; textSize=12f }); content.addView(sp)
            content.addView(TextView(this).apply { text="Product Name"; textSize=12f; setPadding(0,15,0,0) }); content.addView(eName)
            content.addView(TextView(this).apply { text="Code"; textSize=12f }); content.addView(eCode)
            content.addView(TextView(this).apply { text="Cost (USD)"; textSize=12f }); content.addView(eCost)
            content.addView(TextView(this).apply { text="Selling Price (USD)"; textSize=12f }); content.addView(eSell)
            content.addView(TextView(this).apply { text="Qty"; textSize=12f }); content.addView(eQty)
            content.addView(btnAdd); content.addView(tvTable); content.addView(tvGrand); content.addView(bPost)
        }

        fun showStock() {
            content.removeAllViews()
            content.addView(TextView(this).apply { text="STOCK - View All Products"; textSize=18f; setPadding(0,0,0,10) })
            var txt="CODE | NAME | COST | SELL | QTY | VALUE\n--------------------------------\n"
            var totalVal=0.0
            for(it in stock.values) { val v=it.qty*it.cost; txt+="${it.code} | ${it.name} | $${it.cost} | $${it.sell} | ${it.qty} = $${String.format("%.2f", v)}\n"; totalVal+=v }
            txt+="\nTOTAL VALUATION: $${String.format("%.2f", totalVal)}"
            content.addView(TextView(this).apply { text=txt; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(10,10,10,10) })
            content.addView(TextView(this).apply { text="Low Stock (<10):"; setPadding(0,15,0,0) })
            val low=stock.values.filter { it.qty<10 }.joinToString("\n"){ "${it.name} ${it.code} Qty ${it.qty}" }
            content.addView(TextView(this).apply { text=if(low.isEmpty()) "None" else low; setBackgroundColor(0xFFFFF3CD.toInt()); setPadding(10,10,10,10) })
        }

        fun showSuppliers() {
            content.removeAllViews()
            content.addView(TextView(this).apply { text="SUPPLIERS"; textSize=18f; setPadding(0,0,0,10) })
            val eName = EditText(this).apply { hint="Supplier Name" }
            val ePhone = EditText(this).apply { hint="Contact Phone" }
            val btnAdd = Button(this).apply { text="Add Supplier"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            val tvList = TextView(this).apply { setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(10,10,10,10) }
            fun refresh() { tvList.text = if(suppliers.isEmpty()) "No suppliers" else suppliers.joinToString("\n\n"){ "Name: ${it.name}\nPhone: ${it.phone}\nBalance: $${it.balance}" } }
            btnAdd.setOnClickListener { if(eName.text.isNotEmpty()){ suppliers.add(Supplier(eName.text.toString(),ePhone.text.toString(),0.0)); saveAll(); refresh(); eName.text.clear(); ePhone.text.clear() } }
            content.addView(eName); content.addView(ePhone); content.addView(btnAdd); content.addView(tvList)
            refresh()
        }

        fun showReports() {
            content.removeAllViews()
            val today=SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val totalSales=sales.sumOf { it.total }
            val profit=sales.sumOf { sale ->
                // approximate profit from stock cost
                0.0
            }
            var report="REPORTS\nDate: $today\n------------------------\n"
            report+="Total Sales Count: ${sales.size}\n"
            report+="Total Sales Value: $${String.format("%.2f", totalSales)}\n"
            report+="Today's Sales: ${sales.filter { it.date==today }.size} transactions\n"
            report+="Stock Items: ${stock.size}\n"
            report+="Stock Valuation (Cost): $${String.format("%.2f", stock.values.sumOf { it.cost*it.qty })}\n"
            report+="Stock Valuation (Sell): $${String.format("%.2f", stock.values.sumOf { it.sell*it.qty })}\n"
            report+="\nRecent Sales:\n"
            for(s in sales.takeLast(10).reversed()) report+="${s.date} - $${String.format("%.2f", s.total)} - ${s.items}\n"
            content.addView(TextView(this).apply { text=report; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(10,10,10,10) })
        }

        fun showSettings() {
            content.removeAllViews()
            content.addView(TextView(this).apply { text="SETTINGS"; textSize=18f; setPadding(0,0,0,10) })
            val eShop = EditText(this).apply { hint="Shop Name: SHOP001-HRE"; setText(pref.getString("shop_name","SHOP001-HRE")) }
            val eCurr = EditText(this).apply { hint="Currency: USD"; setText(pref.getString("currency","USD")) }
            val btnSave = Button(this).apply { text="Save Settings"; setBackgroundColor(0xFF16A34A.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            btnSave.setOnClickListener { pref.edit().putString("shop_name",eShop.text.toString()).putString("currency",eCurr.text.toString()).apply(); Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show() }
            content.addView(eShop); content.addView(eCurr); content.addView(btnSave)
            content.addView(TextView(this).apply { text="\nShop Info:\n${pref.getString("shop_name","SHOP001-HRE")}\nCurrency: ${pref.getString("currency","USD")}\nVersion: v11\nTax: 0%\nUsers: Admin (PIN 1234)"; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(10,10,10,10); setPadding(0,20,0,0) })
        }

        fun showAdmin() {
            content.removeAllViews()
            content.addView(TextView(this).apply { text="Admin Back Office"; textSize=18f })
            content.addView(TextView(this).apply { text="All Data Management"; setPadding(0,10,0,10) })
            val btnClearSales = Button(this).apply { text="Clear Sales History"; setBackgroundColor(0xFFDC2626.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            btnClearSales.setOnClickListener { sales.clear(); saveAll(); Toast.makeText(this,"Sales cleared",Toast.LENGTH_SHORT).show() }
            val btnClearStock = Button(this).apply { text="Clear All Stock"; setBackgroundColor(0xFFDC2626.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            btnClearStock.setOnClickListener { stock.clear(); saveAll(); Toast.makeText(this,"Stock cleared",Toast.LENGTH_SHORT).show() }
            content.addView(btnClearSales); content.addView(btnClearStock)
        }

        val menu = arrayOf("HOME","SALES","RECEIVING","STOCK","SUPPLIERS","REPORTS","SETTINGS","ADMIN")
        for(m in menu) {
            val btn = TextView(this).apply { text=" $m "; setTextColor(0xFFFFFFFF.toInt()); setPadding(15,18,15,18); textSize=12f; setBackgroundColor(if(m=="RECEIVING") 0xFF185ADB.toInt() else 0x00000000) }
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

        drawer.addView(TextView(this).apply { text="\n SMART POS\n \$12/mo"; setTextColor(0xFF94A3B8.toInt()); setPadding(10,30,10,10); textSize=10f })
        scrollContent.addView(content)
        root.addView(drawer); root.addView(scrollContent)
        setContentView(root)
        showHome()
    }
}
