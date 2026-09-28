package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.view.View
import android.graphics.Color
import android.content.SharedPreferences
import android.text.Editable
import android.text.TextWatcher
import java.util.*

class LoginActivity : Activity() {
    lateinit var pref: SharedPreferences
    var stock = HashMap<String, Item>()
    var cart = ArrayList<Item>()
    data class Item(var name: String, var code: String, var cost: Double, var sell: Double, var qty: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pref = getSharedPreferences("pos", 0)
        stock["1002"] = Item("milk","1002",0.7,1.0,50)
        stock["1003"] = Item("dovi","1003",2.0,3.0,20)
        stock["CC500"] = Item("Coca Cola","CC500",0.7,1.0,50)
        stock["BRD001"] = Item("Bread","BRD001",1.2,1.5,20)
        login()
    }

    fun login(){
        val lay = LinearLayout(this)
        lay.orientation = LinearLayout.VERTICAL
        lay.setPadding(60,200,60,60)
        lay.gravity = Gravity.CENTER
        lay.setBackgroundColor(Color.parseColor("#0A1931"))
        val t = TextView(this)
        t.text = "SmartShop POS\n0000 Cashier / 1234 Admin"
        t.gravity = Gravity.CENTER
        t.setTextColor(Color.WHITE)
        t.textSize = 18f
        val pin = EditText(this)
        pin.hint = "Enter PIN"
        pin.setBackgroundColor(Color.WHITE)
        val btn = Button(this)
        btn.text = "LOGIN"
        btn.setBackgroundColor(Color.parseColor("#185ADB"))
        btn.setTextColor(Color.WHITE)
        btn.setOnClickListener{
            val v = pin.text.toString()
            if(v=="0000"){ dashboard(false) }
            else if(v=="1234"){ dashboard(true) }
        }
        lay.addView(t); lay.addView(pin); lay.addView(btn)
        setContentView(lay)
    }

    fun dashboard(isAdmin: Boolean){
        val root = LinearLayout(this)
        root.orientation = LinearLayout.HORIZONTAL

        val drawer = LinearLayout(this)
        drawer.orientation = LinearLayout.VERTICAL
        drawer.setBackgroundColor(Color.parseColor("#0A1931"))
        drawer.layoutParams = LinearLayout.LayoutParams(200, LinearLayout.LayoutParams.MATCH_PARENT)

        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        scroll.setBackgroundColor(Color.parseColor("#F1F5F9"))

        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(6,6,6,6)

        fun showHome(){
            drawer.visibility = View.VISIBLE
            content.removeAllViews()
            val tv = TextView(this)
            tv.text = "ADMIN HOME\n\nSales: "+cart.size+"\n\nGo to SALES"
            tv.setBackgroundColor(Color.WHITE)
            tv.setPadding(15,15,15,15)
            content.addView(tv)
        }

        fun showSales(){
            if(!isAdmin){ drawer.visibility = View.GONE } else { drawer.visibility = View.GONE }
            content.removeAllViews()

            val title = TextView(this)
            title.text = "SMART POS \$12/mo - SALES"
            title.textSize = 14f
            content.addView(title)

            val eSearch = EditText(this)
            eSearch.hint = "Enter code or name"
            eSearch.setBackgroundColor(Color.WHITE)

            val btnSearch = Button(this)
            btnSearch.text = "SEARCH ADD"
            btnSearch.setBackgroundColor(Color.parseColor("#185ADB"))
            btnSearch.setTextColor(Color.WHITE)

            val cartBox = LinearLayout(this)
            cartBox.orientation = LinearLayout.VERTICAL
            cartBox.setBackgroundColor(Color.WHITE)

            val tvTot = TextView(this)
            tvTot.text = "Total 0.00"
            tvTot.textSize = 20f
            tvTot.gravity = Gravity.RIGHT
            tvTot.setBackgroundColor(Color.parseColor("#1E3A5F"))
            tvTot.setTextColor(Color.WHITE)
            tvTot.setPadding(10,10,10,10)

            val payRow = LinearLayout(this)
            payRow.orientation = LinearLayout.HORIZONTAL
            val bCash = Button(this); bCash.text = "CASH"
            bCash.setBackgroundColor(Color.parseColor("#16A34A")); bCash.setTextColor(Color.WHITE)
            val bEco = Button(this); bEco.text = "ECOCASH"
            bEco.setBackgroundColor(Color.parseColor("#7C3AED")); bEco.setTextColor(Color.WHITE)
            payRow.addView(bCash, LinearLayout.LayoutParams(0, -2, 1f))
            payRow.addView(bEco, LinearLayout.LayoutParams(0, -2, 1f))

            val eRec = EditText(this)
            eRec.hint = "Amount Received"
            eRec.inputType = 8194
            eRec.setBackgroundColor(Color.WHITE)

            val tvChange = TextView(this)
            tvChange.text = "Change: 0.00"
            tvChange.textSize = 22f
            tvChange.gravity = Gravity.RIGHT
            tvChange.setBackgroundColor(Color.parseColor("#DCFCE7"))
            tvChange.setPadding(10,10,10,10)

            val bComplete = Button(this)
            bComplete.text = "COMPLETE SALE"
            bComplete.setBackgroundColor(Color.parseColor("#16A34A"))
            bComplete.setTextColor(Color.WHITE)

            val btnBack = Button(this)
            btnBack.text = "BACK TO ADMIN"
            btnBack.setOnClickListener{ showHome() }

            fun getTotal(): Double {
                var s = 0.0
                for(it in cart){ s = s + it.sell * it.qty }
                return s
            }

            fun refresh(){
                cartBox.removeAllViews()
                var sub = 0.0
                var idx = 1
                for(item in cart){
                    val tot = item.sell * item.qty
                    sub = sub + tot
                    val row = LinearLayout(this)
                    row.orientation = LinearLayout.HORIZONTAL
                    row.setPadding(2,8,2,8)

                    val tv1 = TextView(this); tv1.text = idx.toString()
                    tv1.layoutParams = LinearLayout.LayoutParams(0, -2, 0.3f)

                    val tv2 = TextView(this); tv2.text = item.name
                    tv2.layoutParams = LinearLayout.LayoutParams(0, -2, 1.8f)

                    // FIXED QTY BOX - 3 buttons same line with fixed width
                    val box = LinearLayout(this)
                    box.orientation = LinearLayout.HORIZONTAL
                    box.layoutParams = LinearLayout.LayoutParams(0, -2, 1.5f)

                    val bm = Button(this); bm.text = "-"
                    bm.layoutParams = LinearLayout.LayoutParams(60, -2)
                    val tq = TextView(this); tq.text = item.qty.toString(); tq.gravity = Gravity.CENTER
                    tq.setBackgroundColor(Color.parseColor("#E2E8F0"))
                    tq.layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                    val bp = Button(this); bp.text = "+"
                    bp.layoutParams = LinearLayout.LayoutParams(60, -2)

                    box.addView(bm); box.addView(tq); box.addView(bp)

                    val tv5 = TextView(this); tv5.text = String.format("%.2f", tot); tv5.gravity = Gravity.CENTER
                    tv5.layoutParams = LinearLayout.LayoutParams(0, -2, 0.6f)

                    val del = Button(this); del.text = "X"
                    del.setBackgroundColor(Color.parseColor("#FFC7C7"))
                    del.layoutParams = LinearLayout.LayoutParams(0, -2, 0.4f)

                    bm.setOnClickListener{
                        if(item.qty > 1){ item.qty = item.qty - 1; refresh() }
                    }
                    bp.setOnClickListener{
                        item.qty = item.qty + 1; refresh()
                    }
                    del.setOnClickListener{
                        cart.remove(item); refresh()
                    }
                    row.addView(tv1); row.addView(tv2); row.addView(box); row.addView(tv5); row.addView(del)
                    cartBox.addView(row)
                    idx = idx + 1
                }
                tvTot.text = "Total " + String.format("%.2f", sub)
                val recStr = eRec.text.toString()
                var r = 0.0
                val v = recStr.toDoubleOrNull()
                if(v!= null) r = v
                tvChange.text = "Change: " + String.format("%.2f", r - sub)
                if(cart.size == 0){
                    val em = TextView(this); em.text = "Cart empty - Search product"; em.gravity = Gravity.CENTER; em.setPadding(0,20,0,20)
                    cartBox.addView(em)
                }
            }

            btnSearch.setOnClickListener{
                val q = eSearch.text.toString().trim()
                if(q.length == 0) return@setOnClickListener
                for(k in stock.keys){
                    val it = stock[k]
                    if(it!= null){
                        if(it.code.toLowerCase().contains(q.toLowerCase()) || it.name.toLowerCase().contains(q.toLowerCase())){
                            var found = false
                            for(c in cart){ if(c.code == it.code){ c.qty = c.qty + 1; found = true } }
                            if(!found){ cart.add(Item(it.name, it.code, it.cost, it.sell, 1)) }
                            eSearch.setText("")
                            refresh()
                            break
                        }
                    }
                }
            }

            eRec.addTextChangedListener(object: TextWatcher{
                override fun afterTextChanged(s: Editable?){ refresh() }
                override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int){}
                override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int){}
            })

            bComplete.setOnClickListener{
                val tot = getTotal()
                if(cart.size == 0) return@setOnClickListener
                var rec = 0.0
                val vv = eRec.text.toString().toDoubleOrNull()
                if(vv!= null) rec = vv
                if(rec < tot){ Toast.makeText(this, "Less amount", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                cart.clear(); eRec.setText(""); refresh()
                Toast.makeText(this, "Sold Change " + String.format("%.2f", rec - tot), Toast.LENGTH_LONG).show()
            }

            content.addView(eSearch)
            content.addView(btnSearch)
            content.addView(cartBox)
            content.addView(tvTot)
            content.addView(payRow)
            content.addView(eRec)
            content.addView(tvChange)
            content.addView(bComplete)
            if(isAdmin){ content.addView(btnBack) }

            refresh()
        }

        for(m in arrayOf("HOME","SALES","STOCK","REPORTS")){
            val tv = TextView(this)
            tv.text = " "+m+" "
            tv.setTextColor(Color.WHITE)
            tv.setPadding(12,18,12,18)
            tv.setOnClickListener{
                if(m=="SALES") showSales() else showHome()
            }
            drawer.addView(tv)
        }

        scroll.addView(content)
        root.addView(drawer)
        root.addView(scroll)
        setContentView(root)

        if(isAdmin){ showHome() } else { showSales() }
    }
}
