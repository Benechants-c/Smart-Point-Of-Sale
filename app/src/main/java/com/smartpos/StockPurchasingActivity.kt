package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.widget.*

class StockPurchasingActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }

        val topBar = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(30,60,30,30)
            orientation = LinearLayout.HORIZONTAL
        }
        val back = TextView(this).apply {
            text=" <- Back"; textSize=14f; setTextColor(Color.WHITE)
            setOnClickListener{ finish() }
        }
        val title = TextView(this).apply {
            text=" Stock & Purchasing"; textSize=18f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE)
        }
        topBar.addView(back); topBar.addView(title)
        root.addView(topBar)

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32,32,32,32)
        }

        fun addBtn(txt:String, clr:String, click:()->Unit){
            val b = Button(this)
            b.text = txt
            b.setBackgroundColor(Color.parseColor(clr))
            b.setTextColor(Color.WHITE)
            val p = LinearLayout.LayoutParams(-1,-2)
            p.setMargins(0,0,0,24)
            b.layoutParams = p
            b.setOnClickListener{ click() }
            container.addView(b)
        }

        addBtn("RECEIVE STOCK", "#2563EB"){
            try{ startActivity(Intent(this, ReceiveStockActivity::class.java)) } catch(e:Exception){ Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
        }
        addBtn("STOCK TRANSFER", "#0D9488"){
            try{ startActivity(Intent(this, StockTransferActivity::class.java)) } catch(e:Exception){ Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
        }
        addBtn("VIEW STOCK", "#334155"){
            try{ startActivity(Intent(this, StockListActivity::class.java)) } catch(e:Exception){ Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
        }

        val warning = TextView(this)
        warning.text = "CRITICAL - Password Required"
        warning.setTextColor(Color.parseColor("#DC2626"))
        warning.setTypeface(null, Typeface.BOLD)
        warning.setPadding(0,20,0,10)
        container.addView(warning)

        addBtn("🔐 STOCK TAKE", "#7C3AED"){
            askPassword()
        }

        val scroll = ScrollView(this)
        scroll.addView(container)
        root.addView(scroll)
        setContentView(root)
    }

    private fun askPassword(){
        val input = EditText(this)
        input.hint = "Enter Admin Password"
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        input.setPadding(40,40,40,40)

        AlertDialog.Builder(this)
            .setTitle("Stock Take Protected")
            .setView(input)
            .setPositiveButton("Unlock"){_,_->
                val v = input.text.toString().trim()
                if(v=="1234" || v=="admin123" || v=="2026" || v=="admin"){
                    startActivity(Intent(this, StockTakeActivity::class.java))
                } else {
                    Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}