package com.smartpos

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.*

class StockPurchasingActivity : Activity() {

    data class Prod(val name:String, val qty:Float, val price:Float, val shop:String)

    private var allProducts = mutableListOf<Prod>()
    private lateinit var listContainer: LinearLayout
    private lateinit var headerTitle: TextView
    private lateinit var searchBox: EditText
    private lateinit var shopSpinner: Spinner
    private var currentShop = "Main Shop"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F1F5F9"))
        }

        // Top bar
        val topBar = LinearLayout(this).apply {
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(30,40,30,30)
            orientation = LinearLayout.HORIZONTAL
        }
        val back = TextView(this).apply { text="←"; textSize=22f; setTextColor(Color.WHITE); setPadding(0,0,20,0); setOnClickListener{ finish() } }
        val title = TextView(this).apply { text="Stock & Purchasing"; textSize=18f; setTypeface(null,Typeface.BOLD); setTextColor(Color.WHITE) }
        topBar.addView(back); topBar.addView(title)
        root.addView(topBar)

        // SHOP at top
        val shopWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16,12,16,8)
            setBackgroundColor(Color.WHITE)
        }
        shopWrap.addView(TextView(this).apply { text="SHOP"; textSize=10f; setTypeface(null,Typeface.BOLD); setTextColor(Color.parseColor("#64748B")) })
        shopSpinner = Spinner(this)
        val shops = arrayOf("Main Shop","Shop 2","Warehouse")
        shopSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shops)
        shopSpinner.setBackgroundColor(Color.WHITE)
        shopWrap.addView(shopSpinner)
        root.addView(shopWrap)

        // Search
        val searchWrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            setPadding(16,8,16,8)
        }
        val searchBoxWrap = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#FFFFFF"))
            setPadding(16,10,16,10)
            layoutParams = LinearLayout.LayoutParams(-1,-2)
        }
        searchBoxWrap.addView(TextView(this).apply { text="🔍"; textSize=14f })
        searchBox = EditText(this).apply {
            hint = "Search name or code..."
            setBackgroundColor(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(-1,-2).apply{ setMargins(10,0,0,0) }
            textSize = 13f
        }
        searchBoxWrap.addView(searchBox)
        searchWrap.addView(searchBoxWrap)
        root.addView(searchWrap)

        // Card
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(-1,-1).apply{ setMargins(16,12,16,16) }
            setPadding(0,0,0,0)
        }

        headerTitle = TextView(this).apply {
            text = "Products 0/0 | Qty:0 | Value:$0.00 | Main Shop"
            textSize = 12f
            setTypeface(null,Typeface.BOLD)
            setTextColor(Color.parseColor("#334155"))
            setBackgroundColor(Color.parseColor("#E2E8F0"))
            setPadding(12,10,12,10)
        }
        card.addView(headerTitle)

        // Table header - SIMPLE 3 COLS
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12,10,12,10)
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
        fun h(text:String, weight:Float): TextView {
            return TextView(this@StockPurchasingActivity).apply {
                this.text=text; textSize=11f; setTypeface(null,Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
                layoutParams = LinearLayout.LayoutParams(0,-2,weight)
                gravity = if(text=="PRODUCT") Gravity.LEFT else Gravity.CENTER
            }
        }
        headerRow.addView(h("PRODUCT",2.5f)) // BIGGER
        headerRow.addView(h("QTY",0.7f))
        headerRow.addView(h("PRICE",0.9f))
        card.addView(headerRow)

        val scroll = ScrollView(this)
        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(listContainer)
        card.addView(scroll)

        root.addView(card)
        setContentView(root)

        loadProducts()
        render()

        shopSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p:AdapterView<*>?,v:android.view.View?,pos:Int,id:Long){
                currentShop = shops[pos]
                render()
            }
            override fun onNothingSelected(p:AdapterView<*>?){}
        }

        searchBox.addTextChangedListener(object:TextWatcher{
            override fun afterTextChanged(s:Editable?){ render() }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    private fun loadProducts(){
        allProducts.clear()
        try{
            val seen = HashSet<String>()
            val names = listOf("stock_main","products_db","products","stock")
            for(n in names){
                val pref = getSharedPreferences(n, Context.MODE_PRIVATE)
                for((k,v) in pref.all){
                    if(seen.contains(k)) continue
                    seen.add(k)
                    try{
                        val p = v.toString().split("|")
                        val name = if(p.isNotEmpty()) p[0] else k
                        var buy = if(p.size>1) p[1].toFloatOrNull()?:0f else 0f
                        var sell = if(p.size>2) p[2].toFloatOrNull()?:0f else 0f
                        if(sell==0f) sell=buy
                        var qty = 1f
                        if(p.size>3) qty = p[3].toFloatOrNull()?:1f
                        if(name.isNotBlank()) allProducts.add(Prod(name,qty,sell,currentShop))
                    }catch(_:Exception){}
                }
            }
        }catch(_:Exception){}
    }

    private fun render(){
        val filter = searchBox.text.toString()
        listContainer.removeAllViews()

        val filtered = allProducts.filter{
            (filter.isBlank() || it.name.contains(filter,true))
        }

        var totalQty = 0f
        var totalValue = 0f
        filtered.forEach{ totalQty+=it.qty; totalValue+=it.qty*it.price }

        headerTitle.text = "Products ${filtered.size}/${allProducts.size} | Qty:${totalQty.toInt()} | Value:$${String.format("%.2f",totalValue)} | $currentShop"

        for((idx,prod) in filtered.withIndex()){
            val bg = if(idx%2==0) "#FFFFFF" else "#F8FAFC"
            val isLow = prod.qty <= 10 // RED = need restock

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(Color.parseColor(bg))
                setPadding(12,14,12,14)
            }

            val nameTv = TextView(this).apply {
                text = prod.name
                textSize = 13f
                setTextColor(if(isLow) Color.parseColor("#DC2626") else Color.parseColor("#0F172A"))
                layoutParams = LinearLayout.LayoutParams(0,-2,2.5f) // BIGGER
            }
            val qtyTv = TextView(this).apply {
                text = prod.qty.toInt().toString()
                textSize = 13f
                setTypeface(null,Typeface.BOLD)
                setTextColor(if(isLow) Color.parseColor("#DC2626") else Color.parseColor("#0F172A"))
                layoutParams = LinearLayout.LayoutParams(0,-2,0.7f)
                gravity = Gravity.CENTER
            }
            val priceTv = TextView(this).apply {
                text = "$${String.format("%.1f",prod.price)}"
                textSize = 13f
                setTextColor(if(isLow) Color.parseColor("#DC2626") else Color.parseColor("#0F172A"))
                layoutParams = LinearLayout.LayoutParams(0,-2,0.9f)
                gravity = Gravity.CENTER
            }

            row.addView(nameTv); row.addView(qtyTv); row.addView(priceTv)
            listContainer.addView(row)
            listContainer.addView(View(this).apply { setBackgroundColor(Color.parseColor("#F1F5F9")); layoutParams = LinearLayout.LayoutParams(-1,1) })
        }
    }
}
