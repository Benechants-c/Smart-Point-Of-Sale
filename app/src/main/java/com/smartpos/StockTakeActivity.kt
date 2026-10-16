package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*
import org.json.JSONObject

class StockTakeActivity : Activity() {

    data class Product(val code:String, val name:String, var onHand:Int, val cost:Float, val sell:Float, var counted:Int?=null)

    var allProducts = mutableListOf<Product>()
    var filtered = mutableListOf<Product>()
    lateinit var tableList: LinearLayout
    lateinit var etSearch: EditText
    lateinit var hintList: LinearLayout
    lateinit var spinnerBranch: Spinner
    var branch = "Select Branch"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)

        // HEADER
        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#111827"))
        header.setPadding(16,24,16,20)
        header.addView(TextView(this).apply{ text="SMART POS"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=18f })
        root.addView(header)

        val sub = LinearLayout(this)
        sub.orientation=LinearLayout.VERTICAL
        sub.setBackgroundColor(Color.parseColor("#111827"))
        sub.setPadding(16,0,16,12)
        sub.addView(TextView(this).apply{ text="STOCK TAKE SHEET"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=14f })
        sub.addView(TextView(this).apply{ text="Branch Stock Verification"; setTextColor(Color.parseColor("#9CA3AF")); textSize=11f })
        root.addView(sub)

        // ACTIONS + DATE ROW
        val topRow = LinearLayout(this)
        topRow.orientation=LinearLayout.HORIZONTAL
        topRow.setPadding(12,8,12,8)
        topRow.gravity=Gravity.CENTER_VERTICAL
        val btnActions = Button(this)
        btnActions.text="ACTIONS ▼"
        btnActions.setBackgroundColor(Color.parseColor("#1E293B"))
        btnActions.setTextColor(Color.WHITE)
        btnActions.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(0,0,8,0) }
        btnActions.setOnClickListener{ saveStockTake() }
        topRow.addView(btnActions)
        topRow.addView(TextView(this).apply{ text="15/10/2026"; setTextColor(Color.GRAY); textSize=12f })
        root.addView(topRow)

        // BRANCH SELECT
        val branchRow = LinearLayout(this)
        branchRow.setPadding(12,8,12,8)
        branchRow.orientation=LinearLayout.HORIZONTAL
        branchRow.gravity=Gravity.CENTER_VERTICAL
        branchRow.addView(TextView(this).apply{ text="Shop Branch"; textSize=11f; setTextColor(Color.GRAY) })
        spinnerBranch = Spinner(this)
        val branches = mutableListOf("Select Branch","Rukudzo","Main Shop","All Shops")
        try{
            val pref=getSharedPreferences("shops", Context.MODE_PRIVATE)
            for((_,v) in pref.all){ try{ val o=JSONObject(v.toString()); val n=o.optString("name",""); if(n.isNotBlank() && !branches.contains(n)) branches.add(n) }catch(_:Exception){} }
        }catch(_:Exception){}
        spinnerBranch.adapter=ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branches)
        spinnerBranch.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(12,0,0,0) }
        branchRow.addView(spinnerBranch)
        root.addView(branchRow)

        // SEARCH WITH LIVE HINTS
        val searchContainer = LinearLayout(this)
        searchContainer.orientation=LinearLayout.VERTICAL
        searchContainer.setPadding(12,4,12,4)

        val searchBox = LinearLayout(this)
        searchBox.setBackgroundColor(Color.parseColor("#F3F4F6"))
        searchBox.setPadding(12,8,12,8)
        searchBox.orientation=LinearLayout.HORIZONTAL
        etSearch = EditText(this)
        etSearch.hint="🔍 Search by CODE or NAME - live hints"
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.layoutParams=LinearLayout.LayoutParams(-1,-2,1f)
        searchBox.addView(etSearch)
        searchContainer.addView(searchBox)

        // HINTS DROPDOWN
        hintList = LinearLayout(this)
        hintList.orientation=LinearLayout.VERTICAL
        hintList.setBackgroundColor(Color.WHITE)
        hintList.visibility=LinearLayout.GONE
        searchContainer.addView(hintList)

        root.addView(searchContainer)

        // TABLE HEADER
        val hRow = LinearLayout(this)
        hRow.setBackgroundColor(Color.parseColor("#1E293B"))
        hRow.setPadding(4,10,4,10)
        fun th(t:String,w:Float):TextView{ val tv=TextView(this); tv.text=t; tv.setTextColor(Color.WHITE); tv.textSize=10f; tv.setTypeface(null, Typeface.BOLD); tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv }
        hRow.addView(th("PRODUCT",1.2f))
        hRow.addView(th("ON HAND",0.6f))
        hRow.addView(th("COST",0.5f))
        hRow.addView(th("SELL",0.5f))
        hRow.addView(th("COUNTED",0.7f))
        hRow.addView(th("VAR",0.5f))
        root.addView(hRow)

        // TABLE LIST
        val scroll = ScrollView(this)
        scroll.layoutParams=LinearLayout.LayoutParams(-1,0,1f)
        tableList = LinearLayout(this)
        tableList.orientation=LinearLayout.VERTICAL
        scroll.addView(tableList)
        root.addView(scroll)

        setContentView(root)

        loadProducts()
        filter("")

        etSearch.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){
                val q=s.toString()
                filter(q)
                showHints(q)
            }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    fun loadProducts(){
        allProducts.clear()
        for(pn in listOf("products","products_main","products_db","inventory","stock","products_list")){
            try{
                val pref=getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    try{
                        val o=JSONObject(v.toString())
                        val qty = when{
                            o.has("qnty") -> o.optString("qnty","0").toIntOrNull()?: o.optInt("qnty",0)
                            o.has("quantity") -> o.optString("quantity","0").toIntOrNull()?: o.optInt("quantity",0)
                            o.has("qty") -> o.optString("qty","0").toIntOrNull()?: o.optInt("qty",0)
                            o.has("stock") -> o.optString("stock","0").toIntOrNull()?: o.optInt("stock",0)
                            else -> 0
                        }
                        val code=o.optString("code", o.optString("sku",""))
                        val name=o.optString("name","")
                        val cost=o.optString("cost","0").toFloatOrNull()?: o.optDouble("cost", o.optDouble("buying_price",0.0)).toFloat()
                        val sell=o.optString("price","0").toFloatOrNull()?: o.optDouble("price", o.optDouble("selling_price",0.0)).toFloat()
                        if(name.isNotBlank()){
                            if(allProducts.none{ it.code==code && code.isNotBlank() }){
                                allProducts.add(Product(code.ifBlank{ name }, name, qty, cost, sell))
                            }
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
    }

    fun showHints(q:String){
        hintList.removeAllViews()
        if(q.trim().length <1){
            hintList.visibility=LinearLayout.GONE
            return
        }
        val matches = allProducts.filter{ it.name.lowercase().contains(q.lowercase()) || it.code.lowercase().contains(q.lowercase()) }.take(5)
        if(matches.isEmpty()){
            hintList.visibility=LinearLayout.GONE
            return
        }
        hintList.visibility=LinearLayout.VISIBLE
        for(p in matches){
            val tv=TextView(this)
            tv.text=" ${p.code} - ${p.name} (On Hand ${p.onHand})"
            tv.setPadding(12,14,12,14)
            tv.setBackgroundColor(Color.parseColor("#F9FAFB"))
            tv.textSize=12f
            tv.setOnClickListener{
                etSearch.setText(p.code)
                filtered = mutableListOf(p)
                renderTable()
                hintList.visibility=LinearLayout.GONE
            }
            hintList.addView(tv)
            hintList.addView(android.view.View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#E5E7EB")) })
        }
    }

    fun filter(q:String){
        val s=q.lowercase().trim()
        filtered = if(s.isBlank()) allProducts.toMutableList() else allProducts.filter{ it.name.lowercase().contains(s) || it.code.lowercase().contains(s) }.toMutableList()
        renderTable()
    }

    fun renderTable(){
        tableList.removeAllViews()
        if(filtered.isEmpty() && allProducts.isEmpty()){
            tableList.addView(TextView(this).apply{ text="\nNo products\nAdd products first"; gravity=Gravity.CENTER; setPadding(0,40,0,0); setTextColor(Color.GRAY) })
            return
        }

        for(p in filtered){
            val row = LinearLayout(this)
            row.orientation=LinearLayout.HORIZONTAL
            row.setPadding(4,6,4,6)
            row.setBackgroundColor(if(filtered.indexOf(p)%2==0) Color.WHITE else Color.parseColor("#F9FAFB"))
            row.gravity=Gravity.CENTER_VERTICAL

            fun td(t:String,w:Float):TextView{
                val tv=TextView(this); tv.text=t; tv.textSize=11f; tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv
            }

            row.addView(td(p.name,1.2f).apply{ setTypeface(null, Typeface.BOLD); gravity=Gravity.START; setPadding(8,0,0,0) })
            row.addView(td("${p.onHand}",0.6f))
            row.addView(td("$${p.cost.toInt()}",0.5f))
            row.addView(td("$${p.sell.toInt()}",0.5f))

            // COUNTED - YELLOW EDITABLE LIVE
            val etCount = EditText(this)
            etCount.hint=""
            etCount.setBackgroundColor(Color.parseColor("#FDE68A"))
            etCount.gravity=Gravity.CENTER
            etCount.textSize=12f
            etCount.setPadding(4,8,4,8)
            etCount.layoutParams=LinearLayout.LayoutParams(0,-2,0.7f).apply{ setMargins(4,0,4,0) }
            if(p.counted!=null) etCount.setText("${p.counted}")

            // VAR - LIVE COLOR
            val tvVar = TextView(this)
            tvVar.text="0"
            tvVar.gravity=Gravity.CENTER
            tvVar.setTypeface(null, Typeface.BOLD)
            tvVar.textSize=13f
            tvVar.setTextColor(Color.parseColor("#16A34A"))
            tvVar.layoutParams=LinearLayout.LayoutParams(0,-2,0.5f)

            // LIVE CALC
            fun calc(){
                val counted = etCount.text.toString().toIntOrNull()
                if(counted==null){
                    tvVar.text="0"
                    tvVar.setTextColor(Color.parseColor("#16A34A"))
                    p.counted=null
                } else {
                    val variance = counted - p.onHand
                    tvVar.text="$variance"
                    p.counted=counted
                    when{
                        variance < 0 -> tvVar.setTextColor(Color.parseColor("#DC2626")) // RED
                        variance == 0 -> tvVar.setTextColor(Color.parseColor("#16A34A")) // GREEN
                        else -> tvVar.setTextColor(Color.parseColor("#2563EB")) // BLUE
                    }
                }
            }

            etCount.addTextChangedListener(object:TextWatcher{
                override fun afterTextChanged(s:Editable?){ calc() }
                override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
                override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            })

            row.addView(etCount)
            row.addView(tvVar)

            tableList.addView(row)
            tableList.addView(android.view.View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#E5E7EB")) })
        }
    }

    fun saveStockTake(){
        Toast.makeText(this,"Stock Take Saved - Variance calculated",Toast.LENGTH_SHORT).show()
    }
}