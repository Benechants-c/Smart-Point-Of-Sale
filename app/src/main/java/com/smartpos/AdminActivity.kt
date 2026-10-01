package com.smartpos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class AdminActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.WHITE);setPadding(12,12,12,12)}
        val head = LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setBackgroundColor(Color.parseColor("#1E293B"));setPadding(16,12,16,12)}
        head.addView(TextView(this).apply{text="ADMIN PANEL - BUILD 130 DIRECT REAL ONLY";setTextColor(Color.WHITE);textSize=14f;setTypeface(null,Typeface.BOLD);layoutParams=LinearLayout.LayoutParams(0,-2,1f)})
        head.addView(Button(this).apply{text="BACK";setBackgroundColor(Color.parseColor("#475569"));setTextColor(Color.WHITE);setOnClickListener{finish()}})
        root.addView(head)

        fun section(title:String): TextView = TextView(this).apply{text=title;setBackgroundColor(Color.parseColor("#1E293B"));setTextColor(Color.WHITE);setPadding(12,8,12,8);setTypeface(null,Typeface.BOLD);textSize=12f;setPadding(0,10,0,0)}
        fun btn(txt:String, col:String, action:()->Unit): Button = Button(this).apply{text=txt;setBackgroundColor(Color.parseColor(col));setTextColor(Color.WHITE);setOnClickListener{action()}}

        // QUICK ACTIONS
        root.addView(section("QUICK ACTIONS"))
        root.addView(btn("STOCK RECEIVE - GRN (REAL)","#2563EB"){
            val i = Intent(this@AdminActivity, AdminDetailActivity::class.java)
            i.putExtra("TITLE","STOCK RECEIVE - REAL")
            startActivity(i)
        })
        // ===== FIXED DIRECT — NO MORE SAME MENU! =====
        root.addView(btn("STOCK TRANSFER - REAL","#059669"){
            startActivity(Intent(this@AdminActivity, StockTransferActivity::class.java))
        })
        root.addView(btn("RECEIVE STOCK ACTIVITY","#7C3AED"){
            startActivity(Intent(this@AdminActivity, ReceiveStockActivity::class.java))
        })

        // MANAGEMENT
        root.addView(section("MANAGEMENT"))
        root.addView(btn("BRANCHES / SHOPS - REAL","#2563EB"){
            startActivity(Intent(this@AdminActivity, BranchesActivity::class.java))
        })
        root.addView(btn("USERS","#475569"){
            val i = Intent(this@AdminActivity, AdminDetailActivity::class.java)
            i.putExtra("TITLE","USERS")
            startActivity(i)
        })
        root.addView(btn("REPORTS","#475569"){
            val i = Intent(this@AdminActivity, AdminDetailActivity::class.java)
            i.putExtra("TITLE","REPORTS")
            startActivity(i)
        })

        root.addView(btn("BACK","#E5E7EB"){finish()}.apply{setTextColor(Color.BLACK)})
        setContentView(ScrollView(this).apply{isFillViewport=true;addView(root)})
    }
}
