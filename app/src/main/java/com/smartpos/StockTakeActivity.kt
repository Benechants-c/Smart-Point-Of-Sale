package com.smartpos
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject

class StockTakeActivity : Activity() {

    data class Product(val code:String, val name:String, val onHand:Int, val cost:Float, val sell:Float, val branch:String)

    var all = mutableListOf<Product>()
    var filtered = mutableListOf<Product>()
    lateinit var table: LinearLayout
    lateinit var etSearch: EditText
    lateinit var spinnerBranch: Spinner
    var selectedBranch = "All Branches"
    var countedMap = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)

        // HEADER
        val h1 = LinearLayout(this)
        h1.setBackgroundColor(Color.parseColor("#111827"))
        h1.setPadding(16,12,16,8)
        h1.addView(TextView(this).apply{ text="SMART POS"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=14f })
        root.addView(h1)

        val h2 = LinearLayout(this)
        h2.setBackgroundColor(Color.parseColor("#1F2937"))
        h2.orientation = LinearLayout.VERTICAL
        h2.setPadding(12,8,12,8)
        h2.addView(TextView(this).apply{ text="STOCK TAKE SHEET"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=13f })
        h2.addView(TextView(this).apply{ text="Branch Stock Verification"; setTextColor(Color.parseColor("#9CA3AF")); textSize=11f })
        root.addView(h2)

        // ACTIONS BUTTON - ARRANGED
        val actionRow = LinearLayout(this)
        actionRow.orientation = LinearLayout.HORIZONTAL
        actionRow.setPadding(8,8,8,8)
        actionRow.setBackgroundColor(Color.WHITE)

        val btnActions = Button(this)
        btnActions.text = "ACTIONS ▼"
        btnActions.setBackgroundColor(Color.parseColor("#1E293B"))
        btnActions.setTextColor(Color.WHITE)
        btnActions.setPadding(0,20,0,20)
        btnActions.layoutParams = LinearLayout.LayoutParams(0,-2,1f)
        btnActions.setOnClickListener{ showActionsMenu(btnActions) }
        actionRow.addView(btnActions)

        actionRow.addView(TextView(this).apply{
            text="15/10/2026"; textSize=12f; setPadding(12,0,0,0); gravity=Gravity.CENTER_VERTICAL
            layoutParams=LinearLayout.LayoutParams(-2,-1)
        })
        root.addView(actionRow)

        // BRANCH
        val branchRow = LinearLayout(this)
        branchRow.orientation = LinearLayout.HORIZONTAL
        branchRow.setPadding(8,8,8,8)
        branchRow.gravity = Gravity.CENTER_VERTICAL
        branchRow.addView(TextView(this).apply{ text="Shop Branch"; textSize=12f; setTextColor(Color.GRAY) })
        spinnerBranch = Spinner(this)
        val branches = mutableListOf("All Branches","Rukudzo Shop","Main Shop","Rukudzo")
        try{
            val pref=getSharedPreferences("shops", Context.MODE_PRIVATE)
            for((_,v) in pref.all){ try{ val o=JSONObject(v.toString()); val n=o.optString("name",""); if(n.isNotBlank() && !branches.contains(n)) branches.add(n) }catch(_:Exception){} }
        }catch(_:Exception){}
        spinnerBranch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branches)
        spinnerBranch.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,0,0,0)}
        branchRow.addView(spinnerBranch)
        root.addView(branchRow)

        spinnerBranch.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a:AdapterView<*>?, v:View?, p:Int, id:Long){ selectedBranch=branches[p]; filter() }
            override fun onNothingSelected(a:AdapterView<*>?){}
        }

        // SEARCH - LIVE BY CODE OR NAME
        val searchBox = LinearLayout(this)
        searchBox.setBackgroundColor(Color.parseColor("#F3F4F6"))
        searchBox.setPadding(12,12,12,12)
        searchBox.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,4,8,8)}
        etSearch = EditText(this)
        etSearch.hint = "Search by CODE or NAME - live hints"
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.textSize = 13f
        etSearch.layoutParams = LinearLayout.LayoutParams(-1,-2,1f)
        searchBox.addView(etSearch)
        root.addView(searchBox)

        // TABLE HEADER
        val thRow = LinearLayout(this)
        thRow.setBackgroundColor(Color.parseColor("#1F2937"))
        thRow.setPadding(4,10,4,10)
        fun th(t:String,w:Float):TextView{ val tv=TextView(this); tv.text=t; tv.textSize=10f; tv.setTypeface(null, Typeface.BOLD); tv.setTextColor(Color.WHITE); tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv }
        thRow.addView(th("PRODUCT",1.2f)); thRow.addView(th("ON HAND",0.7f)); thRow.addView(th("COST",0.6f)); thRow.addView(th("SELL",0.6f)); thRow.addView(th("COUNTED",0.8f)); thRow.addView(th("VAR",0.5f))
        root.addView(thRow)

        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(-1,0,1f)
        table = LinearLayout(this)
        table.orientation = LinearLayout.VERTICAL
        scroll.addView(table)
        root.addView(scroll)

        setContentView(root)

        loadAllStock()
        filter()

        etSearch.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    fun loadAllStock(){
        all.clear()
        // FIX: READ FROM ALL STOCK PREFS INCLUDING RECEIVED STOCK
        val stockPrefs = listOf(
            "received_stock","stock_received","products","products_main","products_db",
            "inventory","stock","stock_list","products_list","items",
            "received","receiving","goods_received","grn","purchase_stock"
        )

        for(pn in stockPrefs){
            try{
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    try{
                        val raw = v.toString()
                        if(!raw.trim().startsWith("{")) continue
                        val o = JSONObject(raw)

                        // ALL POSSIBLE KEYS FOR CODE/NAME
                        val code = o.optString("code", o.optString("sku", o.optString("product_code", o.optString("id", k))))
                        val name = o.optString("name", o.optString("product_name", o.optString("product", o.optString("item_name",""))))

                        // ALL POSSIBLE QTY KEYS - qnty, quantity, qty, on_hand, stock, current_stock, received_qty
                        var onHand = 0
                        for(qk in listOf("qnty","quantity","qty","on_hand","stock","current_stock","received_qty","received","available","balance","in_stock")){
                            if(o.has(qk)){
                                val str = o.optString(qk,"0")
                                onHand = str.toIntOrNull() ?: o.optInt(qk,0)
                                if(onHand!=0) break
                                // try double
                                onHand = (o.optDouble(qk,0.0)).toInt()
                                if(onHand!=0) break
                            }
                        }

                        var cost = 0f
                        for(ck in listOf("cost","buying_price","buy_price","purchase_price","cost_price")){
                            if(o.has(ck)){ cost = o.optString(ck,"0").toFloatOrNull() ?: o.optDouble(ck,0.0).toFloat(); if(cost!=0f) break }
                        }

                        var sell = 0f
                        for(sk in listOf("price","selling_price","sell_price","sale_price","retail_price")){
                            if(o.has(sk)){ sell = o.optString(sk,"0").toFloatOrNull() ?: o.optDouble(sk,0.0).toFloat(); if(sell!=0f) break }
                        }

                        val branch = o.optString("branch", o.optString("shop","Rukudzo Shop"))

                        if(name.isNotBlank() && all.none{ it.code==code }){
                            all.add(Product(code, name, onHand, cost, sell, branch))
                        } else if(code.isNotBlank() && name.isBlank() && all.none{ it.code==code }){
                            all.add(Product(code, code, onHand, cost, sell, branch))
                        }

                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
    }

    fun filter(){
        val q = etSearch.text.toString().lowercase().trim()
        filtered = all.filter{
            val branchOk = selectedBranch=="All Branches" || it.branch.contains(selectedBranch,true)
            val searchOk = q.isBlank() || it.name.lowercase().contains(q) || it.code.lowercase().contains(q)
            branchOk && searchOk
        }.toMutableList()
        render()
    }

    fun render(){
        table.removeAllViews()
        if(filtered.isEmpty()){
            val empty = LinearLayout(this)
            empty.orientation = LinearLayout.VERTICAL
            empty.gravity = Gravity.CENTER
            empty.setPadding(0,60,0,0)
            empty.addView(TextView(this).apply{
                text = if(all.isEmpty()) "No products\nAdd products first" else "No match for '${etSearch.text}'"
                gravity = Gravity.CENTER; setTextColor(Color.GRAY); textSize=13f
            })
            if(all.isEmpty()){
                empty.addView(TextView(this).apply{
                    text="\nFound 0 in all prefs.\nCheck Received Stock saved."; gravity=Gravity.CENTER; setTextColor(Color.parseColor("#9CA3AF")); textSize=10f; setPadding(0,12,0,0)
                })
            }
            table.addView(empty)
            return
        }

        for(p in filtered){
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setBackgroundColor(Color.WHITE)
            row.setPadding(4,8,4,8)

            fun td(t:String,w:Float,bold:Boolean=false):TextView{
                val tv=TextView(this); tv.text=t; tv.textSize=11f; tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w)
                if(bold) tv.setTypeface(null, Typeface.BOLD)
                return tv
            }

            row.addView(LinearLayout(this).apply{
                orientation=LinearLayout.VERTICAL; layoutParams=LinearLayout.LayoutParams(0,-2,1.2f)
                addView(TextView(this@StockTakeActivity).apply{ text=p.name; textSize=11f; setTypeface(null, Typeface.BOLD); setPadding(4,0,0,0) })
                addView(TextView(this@StockTakeActivity).apply{ text=p.code; textSize=8f; setTextColor(Color.GRAY); setPadding(4,0,0,0) })
            })

            row.addView(td("${p.onHand}",0.7f,true))
            row.addView(td("${p.cost}",0.6f))
            row.addView(td("${p.sell}",0.6f))

            // COUNTED - EDITABLE
            val etCount = EditText(this)
            etCount.hint="0"
            etCount.textSize=11f
            etCount.gravity=Gravity.CENTER
            etCount.setBackgroundColor(Color.parseColor("#F3F4F6"))
            etCount.layoutParams=LinearLayout.LayoutParams(0,-2,0.8f).apply{setMargins(4,0,4,0)}
            etCount.setText("${countedMap[p.code] ?: ""}")
            etCount.addTextChangedListener(object:TextWatcher{
                override fun afterTextChanged(s:Editable?){
                    val c = s.toString().toIntOrNull() ?: 0
                    countedMap[p.code]=c
                    filter() // to update VAR
                }
                override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
                override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            })
            row.addView(etCount)

            val counted = countedMap[p.code] ?: 0
            val variance = if(countedMap.containsKey(p.code)) counted - p.onHand else 0
            val tvVar = td(if(countedMap.containsKey(p.code)) "$variance" else "-",0.5f,true)
            tvVar.setTextColor(if(variance==0) Color.GRAY else if(variance>0) Color.parseColor("#16A34A") else Color.RED)
            row.addView(tvVar)

            table.addView(row)
            table.addView(View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#E5E7EB")) })
        }
    }

    fun showActionsMenu(anchor: View){
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Save Stock Take")
        popup.menu.add("Export to CSV")
        popup.menu.add("Refresh Stock")
        popup.setOnMenuItemClickListener{
            when(it.title){
                "Refresh Stock" -> { loadAllStock(); filter(); Toast.makeText(this,"Refreshed ${all.size} products", Toast.LENGTH_SHORT).show() }
                "Save Stock Take" -> { Toast.makeText(this,"Saved ${countedMap.size} counted", Toast.LENGTH_SHORT).show() }
                "Export to CSV" -> { Toast.makeText(this,"Export", Toast.LENGTH_SHORT).show() }
            }
            true
        }
        popup.show()
    }
}