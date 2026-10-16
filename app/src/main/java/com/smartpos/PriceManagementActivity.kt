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

class PriceManagementActivity : Activity() {

    data class Product(val code:String, val name:String, val qty:Int, val cost:Float, val sell:Float, val branch:String, val rawJson:String)

    var all = mutableListOf<Product>()
    var filtered = mutableListOf<Product>()
    lateinit var table: LinearLayout
    lateinit var etSearch: EditText
    lateinit var spinnerBranch: Spinner
    var selectedBranch = "All Branches"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)

        // HEADER
        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#111827"))
        header.setPadding(12,10,12,10)
        header.orientation = LinearLayout.HORIZONTAL
        header.addView(TextView(this).apply{
            text="💰 Price Management - TAX INCLUSIVE"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=13f
            layoutParams=LinearLayout.LayoutParams(0,-2,1f)
        })
        header.addView(Button(this).apply{
            text="BACK"; setBackgroundColor(Color.parseColor("#D1D5DB")); setTextColor(Color.BLACK)
            setPadding(20,8,20,8)
            setOnClickListener{ finish() }
        })
        root.addView(header)

        // SEARCH
        val searchBox = LinearLayout(this)
        searchBox.setBackgroundColor(Color.WHITE)
        searchBox.setPadding(12,12,12,12)
        searchBox.orientation = LinearLayout.HORIZONTAL
        searchBox.gravity = Gravity.CENTER_VERTICAL
        searchBox.addView(TextView(this).apply{ text="🔍"; textSize=16f })
        etSearch = EditText(this)
        etSearch.hint="Search by CODE, SKU or Name... e.g. STOVE or STOVE-01"
        etSearch.textSize=11f
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.layoutParams=LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(8,0,0,0)}
        searchBox.addView(etSearch)
        root.addView(searchBox)

        // BRANCH
        val branchRow = LinearLayout(this)
        branchRow.setPadding(12,4,12,8)
        branchRow.gravity=Gravity.CENTER_VERTICAL
        branchRow.addView(TextView(this).apply{ text="Branch:"; textSize=12f; setTextColor(Color.GRAY); setTypeface(null, Typeface.BOLD) })
        spinnerBranch = Spinner(this)
        val branches = mutableListOf("All Branches","Rukudzo","Main Shop")
        try{
            val p=getSharedPreferences("shops", Context.MODE_PRIVATE)
            for((_,v) in p.all){ try{ val o=JSONObject(v.toString()); val n=o.optString("name",""); if(n.isNotBlank() && !branches.contains(n)) branches.add(n) }catch(_:Exception){} }
        }catch(_:Exception){}
        spinnerBranch.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, branches)
        spinnerBranch.layoutParams=LinearLayout.LayoutParams(-1,-2).apply{setMargins(8,0,0,0)}
        branchRow.addView(spinnerBranch)
        root.addView(branchRow)

        spinnerBranch.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a:AdapterView<*>?, v:View?, p:Int, id:Long){ selectedBranch=branches[p]; filter() }
            override fun onNothingSelected(a:AdapterView<*>?){}
        }

        // TABLE HEADER
        val th = LinearLayout(this)
        th.setBackgroundColor(Color.parseColor("#1F2937"))
        th.setPadding(4,10,4,10)
        fun tht(t:String,w:Float):TextView{ val tv=TextView(this); tv.text=t; tv.textSize=10f; tv.setTypeface(null, Typeface.BOLD); tv.setTextColor(Color.WHITE); tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv }
        th.addView(tht("CODE",0.7f)); th.addView(tht("PRODUCT",1.0f)); th.addView(tht("COST",0.6f)); th.addView(tht("SELL",0.6f)); th.addView(tht("PROFIT%",0.7f)); th.addView(tht("EDIT",0.7f))
        root.addView(th)

        val scroll = ScrollView(this)
        scroll.layoutParams=LinearLayout.LayoutParams(-1,0,1f)
        table = LinearLayout(this)
        table.orientation=LinearLayout.VERTICAL
        scroll.addView(table)
        root.addView(scroll)

        setContentView(root)

        loadProductsFixed()
        filter()

        etSearch.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    fun loadProductsFixed(){
        all.clear()
        val prefs = listOf("received_stock","stock_received","products","products_main","products_db","inventory","stock","products_list","items","goods_received")

        for(pn in prefs){
            try{
                val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                for((key,v) in pref.all){
                    try{
                        val raw = v.toString()
                        if(!raw.trim().startsWith("{")) continue
                        val o = JSONObject(raw)

                        val code = o.optString("code", o.optString("sku", o.optString("product_code", key)))
                        val name = o.optString("name", o.optString("product_name", o.optString("product","")))

                        // ===== QTY - SEPARATE KEYS ONLY =====
                        var qty = 0
                        for(qk in listOf("qnty","quantity","qty","on_hand","current_stock","received_qty","in_stock")){
                            if(o.has(qk)){
                                qty = o.optString(qk,"0").toIntOrNull() ?: o.optInt(qk,0)
                                if(qty==0) qty = o.optDouble(qk,0.0).toInt()
                                if(qty!=0) break
                            }
                        }

                        // ===== COST - ONLY COST KEYS - NEVER QNTY =====
                        var cost = -1f
                        val costKeys = listOf("cost","buying_price","buy_price","cost_price","purchase_price","costPrice","buyingPrice")
                        for(ck in costKeys){
                            if(o.has(ck)){
                                val str = o.optString(ck,"").trim()
                                if(str.isNotEmpty()){
                                    cost = str.toFloatOrNull() ?: o.optDouble(ck,-1.0).toFloat()
                                    if(cost>=0) break
                                }
                            }
                        }
                        // If cost still -1, try price but log
                        if(cost==-1f) cost = 0f

                        // ===== SELL - ONLY SELL KEYS - NEVER QNTY =====
                        var sell = -1f
                        val sellKeys = listOf("selling_price","sell_price","price","sale_price","retail_price","sellingPrice","sellPrice","sp","unit_price")
                        for(sk in sellKeys){
                            if(o.has(sk)){
                                val str = o.optString(sk,"").trim()
                                if(str.isNotEmpty()){
                                    sell = str.toFloatOrNull() ?: o.optDouble(sk,-1.0).toFloat()
                                    if(sell>=0) break
                                }
                            }
                        }
                        if(sell==-1f) sell = 0f

                        val branch = o.optString("branch","All Branches")

                        // DEBUG: For 001 tv - show what we parsed
                        if((name.isNotBlank() || code.isNotBlank()) && all.none{ it.code==code }){
                            all.add(Product(code, if(name.isNotBlank()) name else code, qty, cost, sell, branch, raw))
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
            table.addView(TextView(this).apply{ text="No products"; gravity=Gravity.CENTER; setPadding(0,40,0,0); setTextColor(Color.GRAY) })
            return
        }

        for(p in filtered){
            val row = LinearLayout(this)
            row.setPadding(4,8,4,8)
            row.setBackgroundColor(Color.WHITE)
            row.gravity=Gravity.CENTER_VERTICAL

            fun td(t:String,w:Float,color:Int=Color.BLACK,bold:Boolean=false):TextView{
                val tv=TextView(this); tv.text=t; tv.textSize=11f; tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); tv.setTextColor(color)
                if(bold) tv.setTypeface(null, Typeface.BOLD)
                return tv
            }

            row.addView(td(p.code,0.7f, Color.parseColor("#2563EB"), true))
            row.addView(TextView(this).apply{ text=p.name; textSize=11f; layoutParams=LinearLayout.LayoutParams(0,-2,1.0f); setPadding(6,0,0,0) })

            // COST - NOW SHOWS REAL COST NOT QNTY
            row.addView(td("$${p.cost}",0.6f, Color.GRAY))
            row.addView(td("$${p.sell}",0.6f, Color.BLACK, true))

            val profit = if(p.cost>0) ((p.sell - p.cost)/p.cost*100) else 0f
            row.addView(td("${profit.toInt()}%",0.7f, if(profit>0) Color.parseColor("#16A34A") else Color.RED))

            val btnEdit = Button(this)
            btnEdit.text="EDIT"
            btnEdit.textSize=10f
            btnEdit.setBackgroundColor(Color.parseColor("#2563EB"))
            btnEdit.setTextColor(Color.WHITE)
            btnEdit.layoutParams=LinearLayout.LayoutParams(0,-2,0.7f).apply{setMargins(4,0,4,0)}
            btnEdit.setPadding(0,12,0,12)
            btnEdit.setOnClickListener{
                showEditDialog(p)
            }
            row.addView(btnEdit)

            table.addView(row)
            table.addView(View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#E5E7EB")) })
        }
    }

    fun showEditDialog(p:Product){
        val dialog = LinearLayout(this)
        dialog.orientation=LinearLayout.VERTICAL
        dialog.setPadding(20,20,20,20)

        val etCost = EditText(this)
        etCost.hint="Cost"
        etCost.setText("${p.cost}")
        dialog.addView(TextView(this).apply{ text="Cost for ${p.name} (${p.code}) - Current QTY is ${p.qty}" })
        dialog.addView(etCost)

        val etSell = EditText(this)
        etSell.hint="Selling Price"
        etSell.setText("${p.sell}")
        dialog.addView(etSell)

        val btnSave = Button(this)
        btnSave.text="SAVE"
        btnSave.setBackgroundColor(Color.parseColor("#16A34A"))
        btnSave.setTextColor(Color.WHITE)
        dialog.addView(btnSave)

        val popup = android.app.AlertDialog.Builder(this).setView(dialog).create()
        btnSave.setOnClickListener{
            try{
                val newCost = etCost.text.toString().toFloat()
                val newSell = etSell.text.toString().toFloat()

                // SAVE BACK TO PREFS - UPDATE COST AND SELL ONLY - KEEP QNTY SAME
                for(pn in listOf("received_stock","products","inventory","stock")){
                    try{
                        val pref = getSharedPreferences(pn, Context.MODE_PRIVATE)
                        for((k,v) in pref.all){
                            try{
                                val o = JSONObject(v.toString())
                                val code = o.optString("code", o.optString("sku", k))
                                if(code==p.code){
                                    o.put("cost", newCost)
                                    o.put("buying_price", newCost)
                                    o.put("price", newSell)
                                    o.put("selling_price", newSell)
                                    pref.edit().putString(k, o.toString()).apply()
                                }
                            }catch(_:Exception){}
                        }
                    }catch(_:Exception){}
                }

                Toast.makeText(this,"Price updated! Cost: $newCost Sell: $newSell", Toast.LENGTH_SHORT).show()
                popup.dismiss()
                loadProductsFixed()
                filter()
            }catch(e:Exception){
                Toast.makeText(this,"Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        popup.show()
    }
}