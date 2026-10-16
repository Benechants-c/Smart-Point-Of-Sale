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

class StockListActivity : Activity() {

    data class Product(val code:String, val name:String, val qty:Int, val cost:Float, val price:Float, val branch:String)

    var all = mutableListOf<Product>()
    var filtered = mutableListOf<Product>()
    lateinit var table: LinearLayout
    lateinit var etSearch: EditText
    lateinit var tvCost: TextView
    lateinit var tvSell: TextView
    lateinit var tvCount: TextView
    lateinit var spinnerShop: Spinner
    var selectedShop = "Rukudzo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)

        // HEADER
        root.addView(LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#111827"))
            setPadding(16,14,16,14)
            addView(TextView(this@ViewStockActivity).apply{ text="SMART POS"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=16f })
        })

        // BACK
        val backRow = LinearLayout(this)
        backRow.setBackgroundColor(Color.parseColor("#1F2937"))
        backRow.setPadding(12,10,12,10)
        backRow.addView(Button(this).apply {
            text="<- Back View Stock"; textSize=12f; setBackgroundColor(Color.parseColor("#374151")); setTextColor(Color.WHITE)
            setOnClickListener{ finish() }
        })
        root.addView(backRow)

        // SHOP SELECTOR
        val shopRow = LinearLayout(this)
        shopRow.setPadding(8,8,8,8)
        shopRow.gravity = Gravity.CENTER_VERTICAL
        shopRow.addView(TextView(this).apply{ text="Shop:"; setTypeface(null, Typeface.BOLD); textSize=13f })
        spinnerShop = Spinner(this)
        val shops = mutableListOf("Rukudzo","Rukudzo Shop","Main Shop","All Shops")
        try{
            val p=getSharedPreferences("shops", Context.MODE_PRIVATE)
            for((_,v) in p.all){ try{ val o=JSONObject(v.toString()); val n=o.optString("name",""); if(n.isNotBlank() && !shops.contains(n)) shops.add(n) }catch(_:Exception){} }
        }catch(_:Exception){}
        spinnerShop.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shops)
        spinnerShop.layoutParams = LinearLayout.LayoutParams(-1,-2).apply{setMargins(12,0,0,0)}
        shopRow.addView(spinnerShop)
        root.addView(shopRow)

        // TOTALS CARDS - 3 CARDS
        val cards = LinearLayout(this)
        cards.orientation = LinearLayout.HORIZONTAL
        cards.setPadding(6,6,6,6)

        fun makeCard(title:String, color:String): LinearLayout {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.setBackgroundColor(Color.parseColor(color))
            card.setPadding(8,10,8,10)
            card.gravity = Gravity.CENTER
            card.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)}
            card.addView(TextView(this).apply{ text=title; textSize=9f; gravity=Gravity.CENTER; setTextColor(Color.parseColor("#6B7280")) })
            val tv = TextView(this)
            tv.text="$0.00"; tv.setTypeface(null, Typeface.BOLD); tv.gravity=Gravity.CENTER; tv.textSize=13f
            card.addView(tv)
            return card
        }

        val cardCost = makeCard("Total Value at COST","#FECACA")
        tvCost = (cardCost.getChildAt(1) as TextView)
        cards.addView(cardCost)

        val cardSell = makeCard("Total Value at SELLING","#BBF7D0")
        tvSell = (cardSell.getChildAt(1) as TextView)
        cards.addView(cardSell)

        val cardProd = LinearLayout(this)
        cardProd.orientation = LinearLayout.VERTICAL
        cardProd.setBackgroundColor(Color.parseColor("#BFDBFE"))
        cardProd.setPadding(8,10,8,10)
        cardProd.gravity = Gravity.CENTER
        cardProd.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(4,0,4,0)}
        cardProd.addView(TextView(this).apply{ text="Products"; textSize=9f; gravity=Gravity.CENTER })
        tvCount = TextView(this)
        tvCount.text="0"; tvCount.setTypeface(null, Typeface.BOLD); tvCount.gravity=Gravity.CENTER; tvCount.textSize=13f
        cardProd.addView(tvCount)
        cards.addView(cardProd)

        root.addView(cards)

        // SEARCH
        val searchBox = LinearLayout(this)
        searchBox.setBackgroundColor(Color.parseColor("#F3F4F6"))
        searchBox.setPadding(12,12,12,12)
        etSearch = EditText(this)
        etSearch.hint = "Search name or code..."
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.textSize = 13f
        searchBox.addView(etSearch)
        root.addView(searchBox)

        // TABLE HEADER
        val th = LinearLayout(this)
        th.setBackgroundColor(Color.parseColor("#E5E7EB"))
        th.setPadding(4,8,4,8)
        fun tht(t:String,w:Float):TextView{ val tv=TextView(this); tv.text=t; tv.textSize=9f; tv.setTypeface(null, Typeface.BOLD); tv.setTextColor(Color.GRAY); tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv }
        th.addView(tht("SKU / CODE",0.8f)); th.addView(tht("PRODUCT NAME",1.2f)); th.addView(tht("QTY",0.5f)); th.addView(tht("COST",0.6f)); th.addView(tht("PRICE",0.6f)); th.addView(tht("VALUE",0.7f))
        root.addView(th)

        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(-1,0,1f)
        table = LinearLayout(this)
        table.orientation = LinearLayout.VERTICAL
        scroll.addView(table)
        root.addView(scroll)

        setContentView(root)

        loadReceivedStock()
        filter()

        etSearch.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })

        spinnerShop.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a:AdapterView<*>?, v:View?, p:Int, id:Long){ selectedShop=shops[p]; filter() }
            override fun onNothingSelected(a:AdapterView<*>?){}
        }
    }

    fun loadReceivedStock(){
        all.clear()
        // FIX: READ FROM ALL POSSIBLE PREFS WHERE RECEIVED STOCK IS SAVED
        val allPrefs = listOf(
            "received_stock","stock_received","products","products_main","products_db",
            "inventory","stock","stock_list","products_list","items","received",
            "goods_received","grn","purchase_stock","receiving","product_list"
        )

        for(prefName in allPrefs){
            try{
                val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
                if(pref.all.isEmpty()) continue
                for((key,value) in pref.all){
                    try{
                        val s = value.toString()
                        if(!s.trim().startsWith("{")) continue
                        val o = JSONObject(s)

                        val code = o.optString("code", o.optString("sku", o.optString("product_code", o.optString("id", key))))
                        val name = o.optString("name", o.optString("product_name", o.optString("product", o.optString("item_name",""))))

                        var qty = 0
                        val qtyKeys = listOf("qnty","quantity","qty","on_hand","stock","current_stock","received_qty","received","available","balance","in_stock","stock_qty")
                        for(k in qtyKeys){
                            if(o.has(k)){
                                val raw = o.optString(k,"0")
                                qty = raw.toIntOrNull() ?: o.optInt(k,0)
                                if(qty==0){ qty = o.optDouble(k,0.0).toInt() }
                                if(qty!=0) break
                            }
                        }

                        var cost = 0f
                        for(k in listOf("cost","buying_price","buy_price","purchase_price","cost_price")){
                            if(o.has(k)){ cost = o.optString(k,"0").toFloatOrNull() ?: o.optDouble(k,0.0).toFloat(); if(cost!=0f) break }
                        }

                        var price = 0f
                        for(k in listOf("price","selling_price","sell_price","sale_price","retail_price","selling")){
                            if(o.has(k)){ price = o.optString(k,"0").toFloatOrNull() ?: o.optDouble(k,0.0).toFloat(); if(price!=0f) break }
                        }

                        val branch = o.optString("branch", o.optString("shop", o.optString("shop_name","Rukudzo")))

                        if((name.isNotBlank() || code.isNotBlank()) && all.none{ it.code==code }){
                            val finalName = if(name.isNotBlank()) name else code
                            all.add(Product(code, finalName, qty, cost, price, branch))
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
    }

    fun filter(){
        val q = etSearch.text.toString().lowercase().trim()
        filtered = all.filter{
            val shopOk = selectedShop=="All Shops" || it.branch.contains(selectedShop, true) || selectedShop.contains(it.branch, true)
            val searchOk = q.isBlank() || it.name.lowercase().contains(q) || it.code.lowercase().contains(q)
            shopOk && searchOk
        }.toMutableList()
        render()
    }

    fun render(){
        table.removeAllViews()
        var totalCost = 0f
        var totalSell = 0f

        for(p in filtered){
            totalCost += p.qty * p.cost
            totalSell += p.qty * p.price

            val row = LinearLayout(this)
            row.setPadding(4,10,4,10)
            row.setBackgroundColor(Color.WHITE)

            fun td(t:String,w:Float):TextView{
                val tv=TextView(this); tv.text=t; tv.textSize=11f; tv.gravity=Gravity.CENTER; tv.layoutParams=LinearLayout.LayoutParams(0,-2,w); return tv
            }

            row.addView(td(p.code,0.8f))
            row.addView(TextView(this).apply{ text=p.name; textSize=11f; layoutParams=LinearLayout.LayoutParams(0,-2,1.2f); setPadding(4,0,0,0) })
            row.addView(td("${p.qty}",0.5f).apply{ setTypeface(null, Typeface.BOLD) })
            row.addView(td("${p.cost}",0.6f))
            row.addView(td("${p.price}",0.6f))
            row.addView(td("${p.qty * p.price}",0.7f).apply{ setTypeface(null, Typeface.BOLD) })

            table.addView(row)
            table.addView(View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#F3F4F6")) })
        }

        tvCost.text = "$${String.format("%.2f", totalCost)}"
        tvSell.text = "$${String.format("%.2f", totalSell)}"
        tvCount.text = "${filtered.size}"

        if(filtered.isEmpty()){
            table.addView(TextView(this).apply{
                text = if(all.isEmpty()) "No stock found\n\nClick Refresh\nReceived stock not saved to products pref" else "No match"
                gravity=Gravity.CENTER; setPadding(0,60,0,0); setTextColor(Color.GRAY); textSize=12f
            })
        }
    }
}