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

class StockListActivity : Activity() {

    data class Product(val sku:String, val name:String, var qty:Int, val cost:Float, val price:Float, val shop:String)

    var allProducts = mutableListOf<Product>()
    var filtered = mutableListOf<Product>()
    lateinit var tableList: LinearLayout
    lateinit var tvTotalCost: TextView
    lateinit var tvTotalSelling: TextView
    lateinit var tvProducts: TextView
    lateinit var etSearch: EditText
    var selectedShop = "Rukudzo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.WHITE)

        val header = LinearLayout(this)
        header.setBackgroundColor(Color.parseColor("#1E293B"))
        header.setPadding(16,16,16,16)
        header.orientation = LinearLayout.HORIZONTAL
        header.addView(TextView(this).apply{
            text="<- Back View Stock"; setTextColor(Color.WHITE); setTypeface(null, Typeface.BOLD); textSize=14f
            setOnClickListener{ finish() }
        })
        root.addView(header)

        val shopRow = LinearLayout(this)
        shopRow.orientation = LinearLayout.HORIZONTAL
        shopRow.setPadding(16,8,16,8)
        shopRow.gravity = Gravity.CENTER_VERTICAL
        shopRow.addView(TextView(this).apply{ text="Shop:"; setTypeface(null, Typeface.BOLD); textSize=12f })
        val spinnerShop = Spinner(this)
        val shops = loadShops()
        spinnerShop.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shops)
        spinnerShop.setSelection(shops.indexOf("Rukudzo").takeIf{ it>=0 }?: 0)
        spinnerShop.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(a:AdapterView<*>?, v:android.view.View?, p:Int, id:Long){
                selectedShop = shops[p]
                filter(etSearch.text.toString())
            }
            override fun onNothingSelected(a:AdapterView<*>?){}
        }
        shopRow.addView(spinnerShop, LinearLayout.LayoutParams(-1,-2,1f).apply{ setMargins(12,0,0,0) })
        root.addView(shopRow)

        val cardsRow = LinearLayout(this)
        cardsRow.orientation = LinearLayout.HORIZONTAL
        cardsRow.setPadding(8,4,8,4)

        fun card(title:String, valueView: TextView, colorBg:String): LinearLayout {
            val c = LinearLayout(this)
            c.orientation = LinearLayout.VERTICAL
            c.setBackgroundColor(Color.parseColor(colorBg))
            c.setPadding(12,8,12,8)
            c.layoutParams = LinearLayout.LayoutParams(0,-2,1f).apply{ setMargins(4,0,4,0) }
            c.addView(TextView(this).apply{ text=title; textSize=9f; gravity=Gravity.CENTER; setTextColor(Color.DKGRAY) })
            c.addView(valueView)
            return c
        }

        tvTotalCost = TextView(this).apply{ text="$0.00"; textSize=14f; setTypeface(null, Typeface.BOLD); gravity=Gravity.CENTER }
        tvTotalSelling = TextView(this).apply{ text="$0.00"; textSize=14f; setTypeface(null, Typeface.BOLD); gravity=Gravity.CENTER }
        tvProducts = TextView(this).apply{ text="0"; textSize=14f; setTypeface(null, Typeface.BOLD); gravity=Gravity.CENTER }

        cardsRow.addView(card("Total Value at COST", tvTotalCost, "#FFCDD2"))
        cardsRow.addView(card("Total Value at SELLING", tvTotalSelling, "#C8E6C9"))
        cardsRow.addView(card("Products", tvProducts, "#BBDEFB"))
        root.addView(cardsRow)

        val searchBox = LinearLayout(this)
        searchBox.setBackgroundColor(Color.parseColor("#F1F5F9"))
        searchBox.setPadding(12,8,12,8)
        searchBox.orientation = LinearLayout.HORIZONTAL
        etSearch = EditText(this)
        etSearch.hint = "Search name or code..."
        etSearch.setBackgroundColor(Color.TRANSPARENT)
        etSearch.layoutParams = LinearLayout.LayoutParams(-1,-2,1f)
        searchBox.addView(etSearch)
        root.addView(searchBox)

        val headerRow = LinearLayout(this)
        headerRow.setBackgroundColor(Color.parseColor("#E5E7EB"))
        headerRow.setPadding(4,8,4,8)
        fun th(t:String, w:Float): TextView {
            val tv = TextView(this)
            tv.text = t; tv.textSize = 9f; tv.setTypeface(null, Typeface.BOLD); tv.gravity = Gravity.CENTER
            tv.layoutParams = LinearLayout.LayoutParams(0,-2,w)
            return tv
        }
        headerRow.addView(th("SKU / CODE", 0.8f))
        headerRow.addView(th("PRODUCT NAME", 1.2f))
        headerRow.addView(th("QTY", 0.5f))
        headerRow.addView(th("COST", 0.6f))
        headerRow.addView(th("PRICE", 0.6f))
        headerRow.addView(th("VALUE", 0.6f))
        root.addView(headerRow)

        val scroll = ScrollView(this)
        scroll.layoutParams = LinearLayout.LayoutParams(-1,0,1f)
        tableList = LinearLayout(this)
        tableList.orientation = LinearLayout.VERTICAL
        scroll.addView(tableList)
        root.addView(scroll)

        setContentView(root)

        loadRealProducts()
        filter("")

        etSearch.addTextChangedListener(object: TextWatcher{
            override fun afterTextChanged(s:Editable?){ filter(s.toString()) }
            override fun beforeTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
            override fun onTextChanged(a:CharSequence?,b:Int,c:Int,d:Int){}
        })
    }

    fun loadShops(): List<String>{
        val list = mutableListOf("All Shops","Rukudzo","Main Shop")
        try{
            val pref = getSharedPreferences("shops", Context.MODE_PRIVATE)
            for((_,v) in pref.all){
                try{
                    val o = JSONObject(v.toString())
                    val n = o.optString("name","")
                    if(n.isNotBlank() &&!list.contains(n)) list.add(n)
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        return list
    }

    fun loadRealProducts(){
        allProducts.clear()
        val prefNames = listOf("products","products_main","products_db","inventory","stock","products_list","items")

        for(prefName in prefNames){
            try{
                val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
                for((_,v) in pref.all){
                    try{
                        val o = JSONObject(v.toString())

                        val qty = when{
                            o.has("qnty") -> o.optString("qnty","0").toIntOrNull()?: o.optInt("qnty",0)
                            o.has("quantity") -> o.optString("quantity","0").toIntOrNull()?: o.optInt("quantity",0)
                            o.has("qty") -> o.optString("qty","0").toIntOrNull()?: o.optInt("qty",0)
                            o.has("stock") -> o.optString("stock","0").toIntOrNull()?: o.optInt("stock",0)
                            o.has("current_stock") -> o.optInt("current_stock",0)
                            o.has("available") -> o.optInt("available",0)
                            else -> 0
                        }

                        val sku = o.optString("code", o.optString("sku", o.optString("id","")))
                        val name = o.optString("name", o.optString("product_name",""))

                        val cost = when{
                            o.has("cost") -> o.optString("cost","0").toFloatOrNull()?: o.optDouble("cost",0.0).toFloat()
                            o.has("buying_price") -> o.optDouble("buying_price",0.0).toFloat()
                            o.has("buy_price") -> o.optDouble("buy_price",0.0).toFloat()
                            else -> 0f
                        }

                        val price = when{
                            o.has("price") -> o.optString("price","0").toFloatOrNull()?: o.optDouble("price",0.0).toFloat()
                            o.has("selling_price") -> o.optDouble("selling_price",0.0).toFloat()
                            o.has("sell_price") -> o.optDouble("sell_price",0.0).toFloat()
                            else -> 0f
                        }

                        val shop = o.optString("shop", o.optString("store","Rukudzo"))

                        if(name.isNotBlank()){
                            val existing = allProducts.find{ it.sku.equals(sku, true) && sku.isNotBlank() }
                            if(existing!=null){
                                existing.qty = maxOf(existing.qty, qty)
                            } else {
                                allProducts.add(Product(sku, name, qty, cost, price, shop))
                            }
                        }
                    }catch(_:Exception){}
                }
            }catch(_:Exception){}
        }
    }

    fun filter(q:String){
        val query = q.lowercase().trim()
        filtered = allProducts.filter{
            val shopOk = selectedShop=="All Shops" || it.shop.contains(selectedShop, true)
            val searchOk = query.isBlank() || it.name.lowercase().contains(query) || it.sku.lowercase().contains(query)
            shopOk && searchOk
        }.toMutableList()
        renderTable()
    }

    fun renderTable(){
        tableList.removeAllViews()
        var totalCost = 0f
        var totalSelling = 0f

        for(p in filtered){
            totalCost += p.qty * p.cost
            totalSelling += p.qty * p.price

            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.setPadding(4,10,4,10)
            row.setBackgroundColor(Color.WHITE)

            fun td(text:String, w:Float, isQty:Boolean=false): TextView {
                val tv = TextView(this)
                tv.text = text; tv.textSize = 11f; tv.gravity = Gravity.CENTER
                tv.layoutParams = LinearLayout.LayoutParams(0,-2,w)
                if(isQty){
                    tv.setTextColor(if(p.qty<=0) Color.RED else Color.parseColor("#16A34A"))
                    tv.setTypeface(null, Typeface.BOLD)
                }
                return tv
            }

            row.addView(td(p.sku, 0.8f))
            row.addView(td(p.name, 1.2f).apply{ setTypeface(null, Typeface.BOLD) })
            row.addView(td("${p.qty}", 0.5f, true))
            row.addView(td("$${p.cost}", 0.6f))
            row.addView(td("$${p.price}", 0.6f))
            row.addView(td("$${p.qty * p.price}", 0.6f).apply{ setTypeface(null, Typeface.BOLD) })

            tableList.addView(row)
            tableList.addView(android.view.View(this).apply{ layoutParams=LinearLayout.LayoutParams(-1,1); setBackgroundColor(Color.parseColor("#E5E7EB")) })
        }

        tvTotalCost.text = "$${"%.2f".format(totalCost)}"
        tvTotalSelling.text = "$${"%.2f".format(totalSelling)}"
        tvProducts.text = "${filtered.size}"
    }
}