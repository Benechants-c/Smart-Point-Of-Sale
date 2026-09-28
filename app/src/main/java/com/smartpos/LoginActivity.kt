package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.view.View
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
        pref = getSharedPreferences("pos_v15", 0)
        load(); login()
    }
    fun load() {
        val s = pref.getString("stock", "")?: ""
        if (s.isNotEmpty()) for (row in s.split(";;")) { val p=row.split("|"); if(p.size==5) stock[p[1]]=Item(p[0],p[1],p[2].toDoubleOrNull()?:0.0,p[3].toDoubleOrNull()?:0.0,p[4].toIntOrNull()?:0) }
        if (stock.isEmpty()) { stock["1002"]=Item("milk","1002",0.7,1.0,50); stock["1003"]=Item("dovi","1003",2.0,3.0,20); stock["CC500"]=Item("Coca Cola","CC500",0.7,1.0,50) }
        val sl = pref.getString("sales", "")?: ""
        if (sl.isNotEmpty()) for (row in sl.split(";;")) { val p=row.split("||"); if(p.size==3) sales.add(Sale(p[0],p[1].toDoubleOrNull()?:0.0,p[2])) }
    }
    fun saveAll() {
        pref.edit().putString("stock", stock.values.joinToString(";;"){it.name+"|"+it.code+"|"+it.cost+"|"+it.sell+"|"+it.qty}).apply()
        pref.edit().putString("sales", sales.joinToString(";;"){it.date+"||"+it.total+"||"+it.items}).apply()
    }
    fun login() {
        val lay = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(60,200,60,60); gravity=Gravity.CENTER; setBackgroundColor(0xFF0A1931.toInt()) }
        val t = TextView(this).apply { text="SmartShop POS"; textSize=22f; setTextColor(0xFFFFFFFF.toInt()); gravity=Gravity.CENTER }
        val pin = EditText(this).apply { hint="PIN 0000 Cashier / 1234 Admin"; inputType=129; setBackgroundColor(0xFFFFFFFF.toInt()) }
        val btn = Button(this).apply { text="LOGIN"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
        btn.setOnClickListener {
            val p = pin.text.toString()
            if(p=="0000") dashboard(false)
            else if(p=="1234") dashboard(true)
            else Toast.makeText(applicationContext,"0000 Cashier 1234 Admin",Toast.LENGTH_SHORT).show()
        }
        lay.addView(t); lay.addView(pin); lay.addView(btn); setContentView(lay)
    }

    fun dashboard(isAdmin: Boolean) {
        val root = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
        val drawer = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFF0A1931.toInt()); layoutParams=LinearLayout.LayoutParams(200, LinearLayout.LayoutParams.MATCH_PARENT) }
        val scrollContent = ScrollView(this).apply { layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f); setBackgroundColor(0xFFF1F5F9.toInt()) }
        val content = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(6,6,6,6) }

        fun showHome() {
            drawer.visibility=View.VISIBLE
            content.removeAllViews()
            content.addView(TextView(this).apply { text="HOME\nToday sales: ${sales.size}\n\nUse 0000 for Cashier\n1234 for Admin"; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(15,15,15,15) })
        }
        fun showSimple(txt: String) {
            drawer.visibility=View.VISIBLE
            content.removeAllViews()
            content.addView(TextView(this).apply { text=txt; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(15,15,15,15) })
        }

        fun showSales() {
            // 1) HIDE DRAWER FOR CASHIER
            if(!isAdmin) drawer.visibility=View.GONE else drawer.visibility=View.GONE

            content.removeAllViews()

            val topBar = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
            val title = TextView(this).apply { text="SALES"; textSize=14f; layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f) }
            val btnBack = Button(this).apply { text="ADMIN"; textSize=10f }
            btnBack.setOnClickListener { showHome() }
            topBar.addView(title)
            if(isAdmin) topBar.addView(btnBack)
            content.addView(topBar)

            val top = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
            val eSearch = EditText(this).apply { hint="Scan barcode or enter product..."; setBackgroundColor(0xFFFFFFFF.toInt()) }
            val btnSearch = Button(this).apply { text="SEARCH"; setBackgroundColor(0xFF185ADB.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            top.addView(eSearch, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0,0,6,0) })
            top.addView(btnSearch)
            content.addView(top)

            val header = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; setBackgroundColor(0xFFE2E8F0.toInt()); setPadding(4,8,4,8) }
            fun mkHd(s: String, w: Float): TextView { val tv=TextView(this); tv.text=s; tv.textSize=10f; tv.layoutParams=LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w); return tv }
            header.addView(mkHd("#",0.3f)); header.addView(mkHd("Name",2f)); header.addView(mkHd("Code",1f)); header.addView(mkHd("Qty",1.6f)); header.addView(mkHd("Price",0.8f)); header.addView(mkHd("Total",0.8f)); header.addView(mkHd("X",0.4f))
            content.addView(header)

            val cartBox = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFFFFFFFF.toInt()) }
            content.addView(cartBox)

            val sumBox = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setBackgroundColor(0xFFFFFFFF.toInt()); setPadding(8,8,8,8) }
            val tvSub = TextView(this).apply { text="Subtotal 0.00"; gravity=Gravity.RIGHT }
            val tvTot = TextView(this).apply { text="Total 0.00"; textSize=18f; setBackgroundColor(0xFF1E3A5F.toInt()); setTextColor(0xFFFFFFFF.toInt()); setPadding(10,10,10,10); gravity=Gravity.RIGHT }
            val payRow = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL }
            val bCash = Button(this).apply { text="CASH"; setBackgroundColor(0xFF16A34A.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            val bEco = Button(this).apply { text="ECOCASH"; setBackgroundColor(0xFF7C3AED.toInt()); setTextColor(0xFFFFFFFF.toInt()) }
            payRow.addView(bCash, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(0,0,4,0) })
            payRow.addView(bEco, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val eRec = EditText(this).apply { hint="Amount Received"; inputType=8194 }
            val tvChange = TextView(this).apply { text="Change: 0.00"; setBackgroundColor(0xFFDCFCE7.toInt()); setPadding(10,10,10,10); gravity=Gravity.RIGHT; textSize=18f }
            val bComplete = Button(this).apply { text="COMPLETE SALE"; setBackgroundColor(0xFF16A34A.toInt()); setTextColor(0xFFFFFFFF.toInt()) }

            sumBox.addView(tvSub); sumBox.addView(tvTot); sumBox.addView(TextView(this).apply { text="Payment Method" }); sumBox.addView(payRow); sumBox.addView(eRec); sumBox.addView(tvChange); sumBox.addView(bComplete)
