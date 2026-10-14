package com.smartpos

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.*

class PriceManagementActivity : Activity() {
    data class Product(val code: String, val name: String, var cost: Double, var sell: Double, var qty: Int)

    private val products = mutableListOf<Product>()
    private lateinit var listLayout: LinearLayout
    private lateinit var countView: TextView

    private fun getCurrency(): String = getSharedPreferences("system_settings",0).getString("currency","$")?:"$"

    private fun loadProducts(){
        products.clear()
        try{
            val pref = getSharedPreferences("stock_main", Context.MODE_PRIVATE)
            for((code,v) in pref.all){
                try{
                    val p = v.toString().split("|")
                    if(p.size>=4){
                        val name = p[0]
                        val cost = p[1].toDoubleOrNull()?:0.0
                        val sell = p[2].toDoubleOrNull()?:0.0
                        val qty = p[3].toIntOrNull()?:0
                        products.add(Product(code,name,cost,sell,qty))
                    }
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        refreshList()
    }

    private fun refreshList(){
        listLayout.removeAllViews()
        val cur = getCurrency()
        countView.text = "${products.size} products - REAL | Currency: $cur | Tax Inclusive"

        if(products.isEmpty()){
            listLayout.addView(TextView(this).apply{
                text="No products yet!\nGo to Receive Stock or Stock List to add."; gravity=Gravity.CENTER; setPadding(20,100,20,20)
            })
            return
        }

        for(prod in products){
            val row = LinearLayout(this).apply{ orientation=LinearLayout.HORIZONTAL; setPadding(12,14,12,14); setBackgroundColor(Color.WHITE); gravity=Gravity.CENTER_VERTICAL }
            val left = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; layoutParams=LinearLayout.LayoutParams(0,-2,2f) }
            left.addView(TextView(this).apply{text=prod.name; textSize=15f; setTypeface(null,Typeface.BOLD)})
            left.addView(TextView(this).apply{text="${prod.code} | Stock:${prod.qty} | Cost:$cur${prod.cost}"; textSize=11f; setTextColor(Color.GRAY)})

            val priceBtn = Button(this).apply{
                text="$cur${prod.sell}"; setBackgroundColor(Color.parseColor("#16A34A")); setTextColor(Color.WHITE)
                layoutParams=LinearLayout.LayoutParams(-2,-2).apply{setMargins(8,0,0,0)}
                setOnClickListener{ showEditDialog(prod) }
            }
            row.addView(left); row.addView(priceBtn)
            listLayout.addView(row)
            listLayout.addView(View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#E5E7EB")) })
        }
    }

    private fun showEditDialog(prod: Product){
        val cur = getCurrency()
        val lay = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setPadding(30,20,30,20) }
        lay.addView(TextView(this).apply{text="${prod.name}\n${prod.code}\nTax Inclusive Price"; textSize=14f; setTypeface(null,Typeface.BOLD); setPadding(0,0,0,12)})

        lay.addView(TextView(this).apply{text="Cost Price ($cur)"; textSize=11f; setTextColor(Color.GRAY)})
        val edCost = EditText(this).apply{ setText(prod.cost.toString()); inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setBackgroundColor(Color.parseColor("#F1F5F9")); setPadding(16,12,16,12) }
        lay.addView(edCost)

        lay.addView(TextView(this).apply{text="Sell Price - TAX INCLUSIVE ($cur)"; textSize=11f; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor("#16A34A")); setPadding(0,12,0,0)})
        val edSell = EditText(this).apply{ setText(prod.sell.toString()); inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL; setBackgroundColor(Color.parseColor("#FEF3C7")); setPadding(16,12,16,12) }
        lay.addView(edSell)

        AlertDialog.Builder(this).setTitle("Edit Price - Tax Incl").setView(lay)
           .setPositiveButton("💾 SAVE"){_,_->
                val newCost = edCost.text.toString().toDoubleOrNull()?:prod.cost
                val newSell = edSell.text.toString().toDoubleOrNull()?:prod.sell
                try{
                    val pref = getSharedPreferences("stock_main", Context.MODE_PRIVATE)
                    val old = pref.getString(prod.code,"")?:""
                    val parts = old.split("|").toMutableList()
                    if(parts.size>=4){ parts[1]=newCost.toString(); parts[2]=newSell.toString(); pref.edit().putString(prod.code, parts.joinToString("|")).apply() }
                    // Update all shops
                    val shopsPref = getSharedPreferences("shops_db",0)
                    for((shopId,_) in shopsPref.all){
                        val sPref = getSharedPreferences("stock_$shopId",0)
                        val o = sPref.getString(prod.code,"")
                        if(o!=null && o.contains("|")){
                            val pp = o.split("|").toMutableList()
                            if(pp.size>=4){ pp[1]=newCost.toString(); pp[2]=newSell.toString(); sPref.edit().putString(prod.code, pp.joinToString("|")).apply() }
                        }
                    }
                    Toast.makeText(this,"✅ Price updated to $cur$newSell Tax Incl",Toast.LENGTH_SHORT).show()
                    loadProducts()
                }catch(e:Exception){ Toast.makeText(this,"Error: ${e.message}",1).show() }
            }
           .setNegativeButton("Cancel",null).show()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F8FAFC")) }
        root.addView(TextView(this).apply{ text="Price Management"; textSize=18f; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.WHITE); setPadding(20,30,20,12) })
        root.addView(TextView(this).apply{ text="Auto Device Date: 14/10/2026 - Tax Inclusive"; textSize=12f; setBackgroundColor(Color.parseColor("#1E293B")); setTextColor(Color.parseColor("#94A3B8")); setPadding(20,0,20,12) })

        val back = Button(this).apply{ text="IJ BACK"; setBackgroundColor(Color.parseColor("#E2E8F0")); layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,8,8,8)} }
        back.setOnClickListener{ finish() }; root.addView(back)

        countView = TextView(this).apply{ text="0 products - REAL"; textSize=13f; setTypeface(null,Typeface.BOLD); setBackgroundColor(Color.parseColor("#FDE68A")); setPadding(16,12,16,12) }
        root.addView(countView)

        listLayout = LinearLayout(this).apply{ orientation=LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,0,1f); addView(listLayout) }
        root.addView(scroll)
        setContentView(root)
        loadProducts()
    }
}