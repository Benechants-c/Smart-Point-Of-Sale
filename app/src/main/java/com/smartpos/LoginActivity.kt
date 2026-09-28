package com.smartpos

import android.app.Activity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.view.View
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
        val t = TextView(this)
        t.text = "SmartShop POS\n0000 Cashier"
        t.gravity = Gravity.CENTER
        val pin = EditText(this)
        pin.hint = "PIN 0000"
        val btn = Button(this)
        btn.text = "LOGIN"
        btn.setOnClickListener{
            if(pin.text.toString()=="0000" || pin.text.toString()=="1234"){
                dashboard()
            }
        }
        lay.addView(t)
        lay.addView(pin)
        lay.addView(btn)
        setContentView(lay)
    }

    fun dashboard(){
        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(8,8,8,8)

        val eSearch = EditText(this)
        eSearch.hint = "Enter code or name"

        val btnSearch = Button(this)
        btnSearch.text = "SEARCH ADD"

        val cartBox = LinearLayout(this)
        cartBox.orientation = LinearLayout.VERTICAL

        val tvTot = TextView(this)
        tvTot.text = "Total 0.00"
        tvTot.textSize = 20f
        tvTot.gravity = Gravity.RIGHT

        val eRec = EditText(this)
        eRec.hint = "Amount Received"
        eRec.inputType = 8194

        val tvChange = TextView(this)
        tvChange.text = "Change: 0.00"
        tvChange.textSize = 22f
        tvChange.gravity = Gravity.RIGHT

        val bComplete = Button(this)
        bComplete.text = "COMPLETE SALE"

        val payRow = LinearLayout(this)
        payRow.orientation = LinearLayout.HORIZONTAL
        val bCash = Button(this); bCash.text = "CASH"
        val bEco = Button(this); bEco.text = "ECOCASH"
        payRow.addView(bCash, LinearLayout.LayoutParams(0, -2, 1f))
        payRow.addView(bEco, LinearLayout.LayoutParams(0, -2, 1f))

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
                val tv1 = TextView(this); tv1.text = idx.toString()
                tv1.layoutParams = LinearLayout.LayoutParams(0, -2, 0.3f)
                val tv2 = TextView(this); tv2.text = item.name
                tv2.layoutParams = LinearLayout.LayoutParams(0, -2, 2f)
                val box = LinearLayout(this)
                box.orientation = LinearLayout.HORIZONTAL
                box.layoutParams = LinearLayout.LayoutParams(0, -2, 1.5f)
                val bm = Button(this); bm.text = "-"
                val tq = TextView(this); tq.text = item.qty.toString(); tq.gravity = Gravity.CENTER
                val bp = Button(this); bp.text = "+"
                box.addView(bm); box.addView(tq); box.addView(bp)
                val tv5 = TextView(this); tv5.text = String.format("%.2f", tot)
                tv5.layoutParams = LinearLayout.LayoutParams(0, -2, 0.8f)
                val del = Button(this); del.text = "X"
                del.layoutParams = LinearLayout.LayoutParams(0, -2, 0.4f)

                bm.setOnClickListener{
                    if(item.qty > 1){
                        item.qty = item.qty - 1
                        refresh()
                    }
                }
                bp.setOnClickListener{
                    item.qty = item.qty + 1
                    refresh()
                }
                del.setOnClickListener{
                    cart.remove(item)
                    refresh()
                }
                row.addView(tv1); row.addView(tv2); row.addView(box); row.addView(tv5); row.addView(del)
                cartBox.addView(row)
                idx = idx + 1
            }
            tvTot.text = "Total " + String.format("%.2f", sub)
            val rec = eRec.text.toString().toDoubleOrNull()
            var r = 0.0
            if(rec!= null){ r = rec }
            tvChange.text = "Change: " + String.format("%.2f", r - sub)
        }

        btnSearch.setOnClickListener{
            val q = eSearch.text.toString().trim()
            if(q.length == 0) return@setOnClickListener
            for(k in stock.keys){
                val it = stock[k]
                if(it!= null){
                    if(it.code.toLowerCase().contains(q.toLowerCase()) || it.name.toLowerCase().contains(q.toLowerCase())){
                        var found = false
                        for(c in cart){
                            if(c.code == it.code){
                                c.qty = c.qty + 1
                                found = true
                            }
                        }
                        if(!found){
                            cart.add(Item(it.name, it.code, it.cost, it.sell, 1))
                        }
                        eSearch.setText("")
                        refresh()
                        break
                    }
                }
            }
        }

        eRec.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s: Editable?){
                refresh()
            }
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int){}
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int){}
        })

        bComplete.setOnClickListener{
            val tot = getTotal()
            if(cart.size == 0) return@setOnClickListener
            val recStr = eRec.text.toString()
            var rec = 0.0
            if(recStr.length > 0){
                val v = recStr.toDoubleOrNull()
                if(v!= null) rec = v
            }
            if(rec < tot){
                Toast.makeText(this, "Less amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            cart.clear()
            eRec.setText("")
            refresh()
            Toast.makeText(this, "Change " + String.format("%.2f", rec - tot), Toast.LENGTH_LONG).show()
        }

        content.addView(eSearch)
        content.addView(btnSearch)
        content.addView(cartBox)
        content.addView(tvTot)
        content.addView(payRow)
        content.addView(eRec)
        content.addView(tvChange)
        content.addView(bComplete)

        val scroll = ScrollView(this)
        scroll.addView(content)
        setContentView(scroll)
        refresh()
    }
}
