package com.smartpos

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.json.JSONObject

data class RealProd(val name: String, val code: String, val cost: Double, val sell: Double, val qty: Int, val shopId: String)

class StockPurchasingActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: StockAdapter
    private lateinit var tvHeader: TextView
    private lateinit var spinnerShop: Spinner
    private lateinit var etSearch: EditText
    private var fullList: List<RealProd> = listOf()
    private var filtered: List<RealProd> = listOf()
    private val shops = mutableListOf<Pair<String,String>>()

    private fun loadShops(){
        shops.clear()
        try{
            val prefs = getSharedPreferences("shops_db", Context.MODE_PRIVATE)
            prefs.all.forEach { (keyId, value) ->
                try{
                    val str = value.toString()
                    if(str.startsWith("{")){
                        val json = JSONObject(str)
                        val id = json.optString("id", keyId)
                        val name = json.optString("name", id)
                        shops.add(Pair(id, "$name ($id)"))
                    } else { shops.add(Pair(keyId, keyId)) }
                }catch(_:Exception){ shops.add(Pair(keyId, keyId)) }
            }
        }catch(_:Exception){}
        if(shops.isEmpty()) shops.add(Pair("main","Main Shop"))
    }

    private fun getSelectedShopId(): String = if(spinnerShop.selectedItemPosition in shops.indices) shops[spinnerShop.selectedItemPosition].first else "main"

    private fun loadRealStock(shopId: String): List<RealProd>{
        val map = mutableMapOf<String, RealProd>() // code lower -> prod
        // 1. Load from inventory_db
        try{
            val inv = getSharedPreferences("inventory_db", Context.MODE_PRIVATE)
            inv.all.forEach { (k,v) ->
                try{
                    val key = k.lowercase()
                    if(!key.startsWith("${shopId.lowercase()}_")) return@forEach
                    val str = v.toString()
                    if(!str.contains("|")) return@forEach
                    val parts = str.split("|")
                    if(parts.size < 5) return@forEach
                    val name = parts[0]; val cost = parts[1].toDoubleOrNull()?:0.0
                    val sell = parts[2].toDoubleOrNull()?:0.0; val qty = parts[3].toIntOrNull()?:0
                    val code = k.substringAfter("_", "")
                    map[code.lowercase()] = RealProd(name, code, cost, sell, qty, shopId)
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        // 2. Also load from stock_shopId
        try{
            val pref = getSharedPreferences("stock_$shopId", Context.MODE_PRIVATE)
            pref.all.forEach { (code, v) ->
                try{
                    val str = v.toString(); if(!str.contains("|")) return@forEach
                    val parts = str.split("|")
                    val name = parts[0]; val cost = parts[1].toDoubleOrNull()?:0.0
                    val sell = parts[2].toDoubleOrNull()?:0.0; val qty = parts[3].toIntOrNull()?:0
                    val existing = map[code.lowercase()]
                    if(existing == null || qty > existing.qty){
                        map[code.lowercase()] = RealProd(name, code, cost, sell, qty, shopId)
                    }
                }catch(_:Exception){}
            }
        }catch(_:Exception){}
        // 3. Fallback: if still empty, try products_db grouped
        if(map.isEmpty()){
            try{
                val db = openOrCreateDatabase("products_db", Context.MODE_PRIVATE, null)
                db.execSQL("CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, code TEXT, cost REAL, price REAL, qty INTEGER)")
                val c = db.rawQuery("SELECT name, code, cost, price, qty, shop_id FROM products WHERE shop_id=? OR shop_id IS NULL", arrayOf(shopId))
                while(c.moveToNext()){
                    val n = c.getString(0)?:continue; val co = c.getString(1)?:continue
                    val cost = c.getDouble(2); val price = c.getDouble(3); val qty = c.getInt(4)
                    if(!map.containsKey(co.lowercase())) map[co.lowercase()] = RealProd(n, co, cost, price, qty, shopId)
                }
                c.close(); db.close()
            }catch(_:Exception){}
        }
        return map.values.sortedBy { it.name.lowercase() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_purchasing)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Stock & Purchasing"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { showTools() }

        tvHeader = findViewById(R.id.tvHeader)
        recyclerView = findViewById(R.id.recyclerView)
        spinnerShop = findViewById(R.id.spinnerShop)
        etSearch = findViewById(R.id.etSearch)
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadShops()
        spinnerShop.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, shops.map{it.second})
        spinnerShop.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long){ reload() }
            override fun onNothingSelected(p0: AdapterView<*>?){}
        }

        etSearch.addTextChangedListener(object: android.text.TextWatcher{
            override fun afterTextChanged(s: android.text.Editable?){ filter(s.toString()) }
            override fun beforeTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
            override fun onTextChanged(a: CharSequence?, b: Int, c: Int, d: Int) {}
        })

        reload()
    }

    private fun reload(){
        val shopId = getSelectedShopId()
        fullList = loadRealStock(shopId)
        filter(etSearch.text.toString())
    }

    private fun filter(q: String){
        filtered = if(q.trim().isEmpty()) fullList else fullList.filter { it.name.contains(q, true) || it.code.contains(q, true) }
        if(!::adapter.isInitialized){
            adapter = StockAdapter(filtered)
            recyclerView.adapter = adapter
        } else adapter.update(filtered)
        var total = 0.0; var qtySum = 0
        for(p in filtered){ total += p.qty * p.cost; qtySum += p.qty }
        tvHeader.text = "Products - ${filtered.size} / ${fullList.size} Items | Qty: $qtySum | Value: $${"%.2f".format(total)} | Shop: ${shops.find{it.first==getSelectedShopId()}?.second?: getSelectedShopId()}"
    }

    private fun showTools(){
        val v = layoutInflater.inflate(R.layout.bottom_sheet_stock_tools, null)
        val d = BottomSheetDialog(this)
        d.setContentView(v)
        v.findViewById<View>(R.id.btnAdjustments).setOnClickListener { d.dismiss(); Toast.makeText(this,"Adjust Stock: Long press item to edit",Toast.LENGTH_LONG).show() }
        v.findViewById<View>(R.id.btnHistory).setOnClickListener { d.dismiss() }
        v.findViewById<View>(R.id.btnPrint).setOnClickListener { d.dismiss()
            var txt = "STOCK REPORT - ${getSelectedShopId()}\n"; for(p in filtered) txt += "${p.name} | ${p.code} | Qty:${p.qty} | Cost:${p.cost} | Sell:${p.sell}\n"
            val i = android.content.Intent(android.content.Intent.ACTION_SEND); i.type="text/plain"; i.putExtra(android.content.Intent.EXTRA_TEXT, txt); startActivity(android.content.Intent.createChooser(i,"PRINT STOCK"))
        }
        v.findViewById<View>(R.id.btnHelp).setOnClickListener { d.dismiss(); Toast.makeText(this,"RED = Low Stock (qty <=5), GREEN Bold = Profit",Toast.LENGTH_LONG).show() }
        d.show()
    }

    inner class StockAdapter(private var list: List<RealProd>) : RecyclerView.Adapter<StockAdapter.VH>(){
        inner class VH(view: View): RecyclerView.ViewHolder(view){
            val root = view.findViewById<View>(R.id.rowRoot)
            val name = view.findViewById<TextView>(R.id.tvProductName)
            val qty = view.findViewById<TextView>(R.id.tvQty)
            val buy = view.findViewById<TextView>(R.id.tvBuy)
            val sell = view.findViewById<TextView>(R.id.tvSell)
            val profit = view.findViewById<TextView>(R.id.tvProfit)
        }
        override fun onCreateViewHolder(p: ViewGroup, t: Int) = VH(LayoutInflater.from(p.context).inflate(R.layout.item_stock_row, p, false))
        override fun getItemCount() = list.size
        override fun onBindViewHolder(h: VH, pos: Int) {
            val p = list[pos]
            h.name.text = "${p.name} (${p.code})"
            h.qty.text = "${p.qty}"
            h.buy.text = "$${p.cost}"
            h.sell.text = "$${p.sell}"
            val prof = p.sell - p.cost
            h.profit.text = "$${"%.2f".format(prof)}"
            h.root.setBackgroundColor(if(pos % 2 == 0) Color.parseColor("#FFFFFF") else Color.parseColor("#F1F5F9"))
            if(p.qty <= 5){ h.qty.setTextColor(Color.parseColor("#DC2626")); h.qty.setTypeface(null, android.graphics.Typeface.BOLD) } else { h.qty.setTextColor(Color.parseColor("#0F172A")); h.qty.setTypeface(null, android.graphics.Typeface.NORMAL) }
            h.profit.setTextColor(if(prof > 0) Color.parseColor("#16A34A") else Color.RED)
            h.profit.setTypeface(null, if(prof>0) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
        }
        fun update(newList: List<RealProd>){ list = newList; notifyDataSetChanged() }
    }
}
