package com.smartpos

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class CashManagementActivity : Activity() {
    private lateinit var drawerView: TextView
    private lateinit var totalSalesView: TextView
    private fun getCurrency(): String = getSharedPreferences("system_settings",0).getString("currency","$")?:"$"

    private fun calcTotals(){
        val cur = getCurrency()
        var totalSales = 0.0
        try{
            val pref = getSharedPreferences("sales_main",0)
            for((_,v) in pref.all){ try{ val p=v.toString().split("|"); totalSales+=p.getOrNull(1)?.toDoubleOrNull()?:0.0 }catch(_:Exception){} }
        }catch(_:Exception){}
        val opening = getSharedPreferences("cash_drawer",0).getString("opening","5.00")?:"5.00"
        val openVal = opening.toDoubleOrNull()?:5.0
        val drawer = openVal + totalSales
        drawerView.text = "Current Drawer: $cur${String.format(Locale.getDefault(),"%.2f", drawer)} | Auto Date: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}"
        totalSalesView.text = "Total Sales: $cur${String.format(Locale.getDefault(),"%.2f", totalSales)} | Tax Inclusive"
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC"))}
        root.addView(TextView(this).apply{text="Cash Management"; textSize=18f; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(20,30,20,12)})
        val date = SimpleDateFormat("14/10/2026 HH:mm - Auto Device Date", Locale.getDefault()).format(Date())
        root.addView(TextView(this).apply{text="📅 $date"; textSize=12f; setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.parseColor("#94A3B8")); setPadding(20,0,20,12)})

        val back = Button(this).apply{text="IJ BACK"; setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,8,8,8)}}; back.setOnClickListener{finish()}; root.addView(back)

        val cur = getCurrency()
        drawerView = TextView(this).apply{text="Current Drawer: $cur 5.00"; textSize=16f; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#DDD6FE")); setPadding(16,16,16,12)}; root.addView(drawerView)
        totalSalesView = TextView(this).apply{text="Total Sales: $cur 0.00 | Tax Inclusive"; setBackgroundColor(Color.parseColor("#BBF7D0")); setPadding(16,12,16,12)}; root.addView(totalSalesView)

        root.addView(TextView(this).apply{text="Opening Float ($cur)"; textSize=12f; setPadding(16,16,16,2); setTextColor(Color.GRAY)})
        val edOpening = EditText(this).apply{setText(getSharedPreferences("cash_drawer",0).getString("opening","5.00")); inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setBackgroundColor(Color.WHITE); setPadding(16,12,16,12); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,4,16,8)}}
        root.addView(edOpening)

        val saveBtn = Button(this).apply{text="💾 SAVE OPENING FLOAT"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(16,8,16,8)}}
        saveBtn.setOnClickListener{ getSharedPreferences("cash_drawer",0).edit().putString("opening", edOpening.text.toString()).apply(); Toast.makeText(this,"✅ Saved $cur${edOpening.text}",0).show(); calcTotals() }; root.addView(saveBtn)

        setContentView(root); calcTotals()
    }
}